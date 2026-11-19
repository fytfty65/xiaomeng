package com.omniroute.app.core.ui.components

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.LruCache
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import com.omniroute.app.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL

/** 服务端返回的是 /uploads/avatars/xxx.jpg 相对路径，展示前拼上网关地址。 */
fun avatarAbsoluteUrl(path: String?): String? {
    val value = path?.trim().orEmpty()
    if (value.isEmpty()) return null
    if (value.startsWith("http://") || value.startsWith("https://")) return value
    return BuildConfig.BASE_URL.trimEnd('/') + "/" + value.trimStart('/')
}

/**
 * 圆形远程头像：没有图片加载库时用 OkHttp 之外的 HttpURLConnection 解码，
 * 并按 URL 做内存 LRU 缓存，避免滚动时反复下载。
 */
@Composable
fun RemoteAvatar(
    path: String?,
    fallback: String,
    size: Dp,
    background: Color,
    foreground: Color,
    fontSize: TextUnit,
    contentDescription: String?,
    modifier: Modifier = Modifier,
) {
    val url = avatarAbsoluteUrl(path)
    val bitmap by produceState<Bitmap?>(initialValue = null, url) {
        value = if (url == null) null else AvatarBitmapCache.load(url)
    }
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(background),
        contentAlignment = Alignment.Center,
    ) {
        val image = bitmap
        if (image != null) {
            Image(
                bitmap = image.asImageBitmap(),
                contentDescription = contentDescription,
                contentScale = ContentScale.Crop,
                modifier = Modifier.size(size).clip(CircleShape),
            )
        } else {
            Text(
                text = fallback,
                fontSize = fontSize,
                fontWeight = FontWeight.ExtraBold,
                color = foreground,
            )
        }
    }
}

private object AvatarBitmapCache {

    private const val MAX_CACHE_BYTES = 8 * 1024 * 1024

    private val cache = object : LruCache<String, Bitmap>(MAX_CACHE_BYTES) {
        override fun sizeOf(key: String, value: Bitmap): Int = value.byteCount
    }

    suspend fun load(url: String): Bitmap? = withContext(Dispatchers.IO) {
        cache.get(url)?.let { return@withContext it }
        runCatching {
            val connection = (URL(url).openConnection() as HttpURLConnection).apply {
                connectTimeout = 8_000
                readTimeout = 8_000
                instanceFollowRedirects = true
            }
            try {
                connection.inputStream.use { BitmapFactory.decodeStream(it) }
            } finally {
                connection.disconnect()
            }
        }.getOrNull()?.also { bitmap ->
            if (bitmap.byteCount <= MAX_CACHE_BYTES / 2) {
                cache.put(url, bitmap)
            }
        }
    }
}
