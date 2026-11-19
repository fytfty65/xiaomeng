package com.omniroute.app.core.map;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.LruCache;

import com.omniroute.app.core.network.ApiClient;
import com.omniroute.app.core.network.MapApi;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Locale;

import okhttp3.ResponseBody;
import retrofit2.Response;

/**
 * 真实底图仓库。
 *
 * <p>带内存 LRU 与磁盘两级缓存：同一路线在列表滚动、旋转或进程重建后都不必重复拉图。
 * 图片解码失败（网关降级返回 JSON）时抛 IOException，界面据此回退到矢量示意图。
 */
public final class StaticMapRepository {

    private static final long MAX_DISK_BYTES = 32L * 1024 * 1024;
    private static final String CACHE_DIR = "static_map_cache";

    private final MapApi api;
    private final File cacheDir;
    private final LruCache<String, Bitmap> memoryCache;

    public StaticMapRepository(ApiClient client, Context context) {
        this.api = client.getMapApi();
        this.cacheDir = new File(context.getApplicationContext().getCacheDir(), CACHE_DIR);
        int maxMemoryKb = (int) (Runtime.getRuntime().maxMemory() / 1024);
        int cacheSizeKb = Math.min(maxMemoryKb / 8, 8 * 1024);
        this.memoryCache = new LruCache<String, Bitmap>(cacheSizeKb) {
            @Override
            protected int sizeOf(String key, Bitmap value) {
                return value.getAllocationByteCount() / 1024;
            }
        };
    }

    /**
     * 拉取一张底图。
     *
     * @param center 地图中心点，格式 lng,lat
     * @param zoom   缩放级别
     * @param width  请求像素宽
     * @param height 请求像素高
     */
    public Bitmap load(String center, int zoom, int width, int height) throws IOException {
        String key = cacheKey(center, zoom, width, height);
        Bitmap cached = memoryCache.get(key);
        if (cached != null && !cached.isRecycled()) {
            return cached;
        }

        Bitmap fromDisk = readFromDisk(key);
        if (fromDisk != null) {
            memoryCache.put(key, fromDisk);
            return fromDisk;
        }

        Bitmap downloaded = download(center, zoom, width, height);
        memoryCache.put(key, downloaded);
        writeToDisk(key, downloaded);
        return downloaded;
    }

    private Bitmap download(String center, int zoom, int width, int height) throws IOException {
        Response<ResponseBody> response = api.staticMap(center, zoom, width + "*" + height).execute();
        if (!response.isSuccessful()) {
            throw new IOException("底图接口返回 " + response.code());
        }
        ResponseBody body = response.body();
        if (body == null) {
            throw new IOException("底图响应为空");
        }
        try {
            byte[] bytes = body.bytes();
            Bitmap bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
            if (bitmap == null) {
                throw new IOException("底图解码失败");
            }
            return bitmap;
        } finally {
            body.close();
        }
    }

    private Bitmap readFromDisk(String key) {
        File file = new File(cacheDir, key + ".img");
        if (!file.isFile() || file.length() <= 0) {
            return null;
        }
        try (InputStream input = new java.io.FileInputStream(file);
             java.io.ByteArrayOutputStream output = new java.io.ByteArrayOutputStream()) {
            byte[] buffer = new byte[8192];
            int read;
            while ((read = input.read(buffer)) != -1) {
                output.write(buffer, 0, read);
            }
            byte[] bytes = output.toByteArray();
            return BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
        } catch (IOException ignored) {
            // 缓存损坏时直接丢弃，下一次从网络重取。
            file.delete();
            return null;
        }
    }

    private void writeToDisk(String key, Bitmap bitmap) {
        if (!cacheDir.exists() && !cacheDir.mkdirs()) {
            return;
        }
        File file = new File(cacheDir, key + ".img");
        try (OutputStream output = new java.io.FileOutputStream(file)) {
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, output);
        } catch (IOException ignored) {
            file.delete();
        }
        pruneIfNeeded();
    }

    /** 控制磁盘占用：超过上限时按最后修改时间淘汰旧图。 */
    private void pruneIfNeeded() {
        File[] files = cacheDir.listFiles();
        if (files == null || files.length <= 1) {
            return;
        }
        long total = 0;
        for (File file : files) {
            total += file.length();
        }
        if (total <= MAX_DISK_BYTES) {
            return;
        }
        Arrays.sort(files, Comparator.comparingLong(File::lastModified));
        for (File file : files) {
            if (total <= MAX_DISK_BYTES) {
                break;
            }
            total -= file.length();
            file.delete();
        }
    }

    private String cacheKey(String center, int zoom, int width, int height) {
        String raw = String.format(Locale.US, "%s|%d|%d|%d", center, zoom, width, height);
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(raw.getBytes(StandardCharsets.UTF_8));
            StringBuilder builder = new StringBuilder(hash.length);
            for (byte value : hash) {
                builder.append(String.format(Locale.US, "%02x", value));
            }
            return builder.toString();
        } catch (Exception ignored) {
            return String.valueOf(raw.hashCode());
        }
    }
}
