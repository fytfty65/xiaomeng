# OmniRoute Android release 混淆规则。
# 网络层 DTO 依赖 Gson 字段名反射，Retrofit 依赖接口注解，均不能被裁剪或改名。

-keepattributes Signature,InnerClasses,EnclosingMethod,*Annotation*,Exceptions

# Gson 按字段名映射 JSON，DTO 的类名与字段名都要保留。
-keep class com.omniroute.app.core.network.dto.** { *; }
-keepclassmembers class com.omniroute.app.core.network.dto.** { <fields>; }

# Retrofit 通过反射解析接口方法上的注解。
-keep interface com.omniroute.app.core.network.** { *; }
-keepclasseswithmembers class * {
    @retrofit2.http.* <methods>;
}

# WorkManager 通过反射实例化 Worker，构造器不能被裁剪。
-keep class * extends androidx.work.ListenableWorker {
    <init>(android.content.Context, androidx.work.WorkerParameters);
}

# Retrofit / OkHttp 的可选依赖，未引入时避免告警。
-dontwarn okhttp3.**
-dontwarn okio.**
-dontwarn javax.annotation.**
-dontwarn kotlin.Unit
-dontwarn retrofit2.KotlinExtensions*
