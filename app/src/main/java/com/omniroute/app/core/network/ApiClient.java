package com.omniroute.app.core.network;

import com.omniroute.app.core.auth.TokenStore;

import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.WebSocket;
import okhttp3.WebSocketListener;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

/**
 * 网络入口。分两个 OkHttp 实例：
 * - plainClient 用于登录/注册/刷新，避免刷新自身触发 401 刷新造成递归；
 * - authorizedClient 额外挂 Authenticator，用于需要 Bearer 的业务接口。
 */
public final class ApiClient {

    /** 由 AuthRepository 注入，刷新逻辑统一放在仓库层（单飞）。 */
    public interface TokenRefresher {
        String refresh(String staleAccessToken);
    }

    private final String normalizedBaseUrl;
    private final TokenStore tokenStore;
    private volatile TokenRefresher refresher;

    private final OkHttpClient plainClient;
    private final OkHttpClient authorizedClient;
    private final OkHttpClient webSocketClient;
    private final AuthApi authApi;
    private final AuthApi api;
    private final PlanningApi planningApi;
    private final CommunityApi communityApi;
    private final PhotoApi photoApi;
    private final BudgetApi budgetApi;
    private final MapApi mapApi;
    private final VariantApi variantApi;

    public ApiClient(String baseUrl, TokenStore tokenStore) {
        this.tokenStore = tokenStore;
        String trimmed = baseUrl.trim();
        this.normalizedBaseUrl = (trimmed.endsWith("/") ? trimmed : trimmed + "/");

        OkHttpClient.Builder builder = new OkHttpClient.Builder()
                .connectTimeout(15, TimeUnit.SECONDS)
                .readTimeout(20, TimeUnit.SECONDS)
                .writeTimeout(20, TimeUnit.SECONDS)
                .addInterceptor(new AuthInterceptor(tokenStore))
                .addInterceptor(new SafeLoggingInterceptor(com.omniroute.app.BuildConfig.DEBUG));
        // 模拟器调试时可能会给系统设置全局代理（例如为输入法下载语言包），
        // 本地网关必须直连，否则会被代理拦截导致登录/规划失败。
        if (isLocalHost(normalizedBaseUrl)) {
            builder.proxy(java.net.Proxy.NO_PROXY);
        }
        this.plainClient = builder.build();

        this.authApi = retrofit(plainClient).create(AuthApi.class);

        OkHttpClient authorizedClient = plainClient.newBuilder()
                .authenticator(new TokenAuthenticator(stale -> {
                    TokenRefresher current = refresher;
                    return current == null ? null : current.refresh(stale);
                }))
                .build();
        this.authorizedClient = authorizedClient;

        // 实时推演通道单独一套超时：一次推演要 3~8 分钟，中途常有几十秒没有任何流式片段
        // （模型思考、高德批量取数）。沿用 HTTP 的 20s 读超时会把它误判成「通道中断」，
        // 用户看到的就是推演做到一半莫名失败。
        //
        // 这里只放开读超时，不启用客户端 pingInterval：网关在推演期间跑在同一条读协程里
        // 调 AI，控制帧要等推演结束才被处理，客户端 ping 会一直等不到 pong，
        // 20s 后 OkHttp 主动判定失败（实测 2026-11-02）。保活交给服务端 ping 即可。
        this.webSocketClient = authorizedClient.newBuilder()
                .readTimeout(0, TimeUnit.MILLISECONDS)
                .build();
        this.api = retrofit(authorizedClient).create(AuthApi.class);
        this.planningApi = retrofit(authorizedClient).create(PlanningApi.class);
        this.communityApi = retrofit(authorizedClient).create(CommunityApi.class);
        this.photoApi = retrofit(authorizedClient).create(PhotoApi.class);
        this.budgetApi = retrofit(authorizedClient).create(BudgetApi.class);
        this.mapApi = retrofit(authorizedClient).create(MapApi.class);
        this.variantApi = retrofit(authorizedClient).create(VariantApi.class);
    }

    /** 判断 baseUrl 是否指向本机/模拟器宿主机，这类地址在调试时应绕过系统代理。 */
    private static boolean isLocalHost(String url) {
        return url.contains("10.0.2.2") || url.contains("127.0.0.1") || url.contains("localhost");
    }

    private Retrofit retrofit(OkHttpClient client) {
        return new Retrofit.Builder()
                .baseUrl(normalizedBaseUrl)
                .client(client)
                .addConverterFactory(GsonConverterFactory.create(AppJson.gson()))
                .build();
    }

    public void setRefresher(TokenRefresher refresher) {
        this.refresher = refresher;
    }

    public AuthApi getAuthApi() {
        return authApi;
    }

    /** 需要 Authorization 的接口实例，401 时自动刷新并重试一次。 */
    public AuthApi getApi() {
        return api;
    }

    /** 规划主链路与行程列表，同样挂在带 Authenticator 的实例上。 */
    public PlanningApi getPlanningApi() {
        return planningApi;
    }

    /** 社区与话题接口。 */
    public CommunityApi getCommunityApi() {
        return communityApi;
    }

    /** 用户相册：上传 / 我的列表 / 取图 / 删除。 */
    public PhotoApi getPhotoApi() {
        return photoApi;
    }

    /** 预算管家：预算设置 / 记账 / 汇总。 */
    public BudgetApi getBudgetApi() {
        return budgetApi;
    }

    /** 真实底图接口（返回 PNG 二进制）。 */
    public MapApi getMapApi() {
        return mapApi;
    }

    /** 房间多方案：候选保存、方案池、投票与采纳。 */
    public VariantApi getVariantApi() {
        return variantApi;
    }

    /** 供实时通道拼接 ws:// 地址（把 http(s) 换成 ws(s)）。 */
    public String getBaseUrl() {
        return normalizedBaseUrl;
    }

    /**
     * 打开一条带 Bearer 的 WebSocket。
     * WebSocket 的握手同样经过 Interceptor，因此 AuthInterceptor 会自动注入 Authorization。
     * 401 时不会走 Authenticator 重试（升级请求不可重放），调用方应处理关闭原因。
     */
    public WebSocket openWebSocket(Request request, WebSocketListener listener) {
        return webSocketClient.newWebSocket(request, listener);
    }
}
