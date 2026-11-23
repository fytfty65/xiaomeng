import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import java.io.FileInputStream
import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
}

// 发布签名从 keystore.properties（不入库）或环境变量读取。
// 两者都没有时 release 仍可编译（产出未签名包），不会阻塞日常构建。
val keystoreProperties = Properties().apply {
    val file = rootProject.file("keystore.properties")
    if (file.exists()) {
        FileInputStream(file).use { load(it) }
    }
}
val releaseStoreFile = keystoreProperties.getProperty("storeFile")
    ?: System.getenv("OMNIROUTE_KEYSTORE_FILE")
val releaseStorePassword = keystoreProperties.getProperty("storePassword")
    ?: System.getenv("OMNIROUTE_KEYSTORE_PASSWORD")
val releaseKeyAlias = keystoreProperties.getProperty("keyAlias")
    ?: System.getenv("OMNIROUTE_KEY_ALIAS")
val releaseKeyPassword = keystoreProperties.getProperty("keyPassword")
    ?: System.getenv("OMNIROUTE_KEY_PASSWORD")
val releaseSigningReady = !releaseStoreFile.isNullOrBlank() &&
    !releaseStorePassword.isNullOrBlank() &&
    !releaseKeyAlias.isNullOrBlank() &&
    !releaseKeyPassword.isNullOrBlank()

// 正式环境网关地址：本地 keystore.properties 的 baseUrl → -POMNIROUTE_BASE_URL → 环境变量，缺省占位。
val releaseBaseUrl = keystoreProperties.getProperty("baseUrl")?.takeIf { it.isNotBlank() }
    ?: (project.findProperty("OMNIROUTE_BASE_URL") as String?)?.takeIf { it.isNotBlank() }
    ?: System.getenv("OMNIROUTE_BASE_URL")?.takeIf { it.isNotBlank() }
    ?: "https://api.example.com"

// 调试环境网关地址：keystore.properties 的 debugBaseUrl → -POMNIROUTE_DEBUG_BASE_URL → 环境变量 → 模拟器宿主机。
// 想直连生产网关或真机局域网 IP 时改 keystore.properties 即可，不必改代码。
val debugBaseUrl = keystoreProperties.getProperty("debugBaseUrl")?.takeIf { it.isNotBlank() }
    ?: (project.findProperty("OMNIROUTE_DEBUG_BASE_URL") as String?)?.takeIf { it.isNotBlank() }
    ?: System.getenv("OMNIROUTE_DEBUG_BASE_URL")?.takeIf { it.isNotBlank() }
    ?: "http://10.0.2.2:8080"

// 运行 debug 包前自动拉起本地联调环境（WSL Redis + ai-service + 网关），脚本本身幂等：
// 服务都在线时只做端口/健康探测，几秒内返回。用 -POMNIROUTE_DEV_UP_SCRIPT=... 可换脚本路径；
// 脚本不存在（例如只装了移动端仓库的机器）时静默跳过，不影响构建。
val devUpScript = (project.findProperty("OMNIROUTE_DEV_UP_SCRIPT") as String?)
    ?: "d:\\OminRoute_2\\OminRoute\\OminRoute\\scripts\\dev-up.ps1"
if (System.getProperty("os.name").startsWith("Windows", ignoreCase = true) && File(devUpScript).isFile) {
    val devUp = tasks.register<Exec>("omniRouteDevUp") {
        group = "omniroute"
        description = "启动本地开发服务（Redis / ai-service / 网关），已在运行则跳过"
        // 显式把 PowerShell 输出切成 UTF-8，否则中文在 Build 窗口里会变乱码。
        commandLine(
            "powershell.exe", "-NoProfile", "-ExecutionPolicy", "Bypass",
            "-Command", "[Console]::OutputEncoding=[Text.Encoding]::UTF8; & '$devUpScript'",
        )
        // 服务起不来不应该让 App 编不出来，错误在 build 日志里能看到。
        isIgnoreExitValue = true
    }
    tasks.matching { it.name == "preDebugBuild" }.configureEach { dependsOn(devUp) }
}

android {
    namespace = "com.omniroute.app"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        applicationId = "com.omniroute.app"
        minSdk = 26
        targetSdk = 37
        versionCode = 1
        versionName = "1.0"
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    signingConfigs {
        if (releaseSigningReady) {
            create("release") {
                storeFile = file(releaseStoreFile!!)
                storePassword = releaseStorePassword
                keyAlias = releaseKeyAlias
                keyPassword = releaseKeyPassword
            }
        }
    }

    buildTypes {
        // 默认走模拟器宿主机 10.0.2.2；可用 debugBaseUrl 覆盖为生产网关或真机局域网 IP。
        debug {
            buildConfigField("String", "BASE_URL", "\"$debugBaseUrl\"")
        }
        release {
            buildConfigField("String", "BASE_URL", "\"$releaseBaseUrl\"")
            // AGP 9 的 optimization 同时开启 R8 代码压缩与资源裁剪。
            optimization {
                enable = true
            }
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
            signingConfig = signingConfigs.findByName("release")
            // ML Kit 小票识别是 bundled 模型，每个 ABI 带一份原生库（约 10MB）。
            // 发布包只保留真机 ABI，去掉仅模拟器用得到的 x86/x86_64，包体从 ~44MB 降到 ~22MB。
            ndk {
                abiFilters += listOf("arm64-v8a", "armeabi-v7a")
            }
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_11)
    }
}

dependencies {
    implementation("com.google.zxing:core:3.5.3")
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.viewmodel.savedstate)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation("androidx.compose.material:material-icons-core")
    implementation(libs.androidx.navigation.compose)
    implementation(libs.retrofit)
    implementation(libs.retrofit.gson)
    implementation(libs.okhttp)
    implementation(libs.kotlinx.coroutines.android)
    // 小票 OCR：中文识别模型随 APK 打包，离线可用，不依赖 Google Play 服务。
    implementation(libs.mlkit.text.recognition.chinese)
    // 风险提醒：后台定时巡检 + 系统通知，不依赖 Firebase 配置。
    implementation(libs.androidx.work.runtime.ktx)

    testImplementation("junit:junit:4.13.2")

    debugImplementation(libs.androidx.compose.ui.tooling)
}
