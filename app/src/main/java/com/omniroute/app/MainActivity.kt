package com.omniroute.app

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import com.omniroute.app.core.ui.theme.JourneySeason
import com.omniroute.app.core.ui.theme.OmniRouteTheme
import com.omniroute.app.ui.AppNavHost

class MainActivity : ComponentActivity() {

    private val container by lazy { AppContainer(applicationContext) }

    private val localNetworkPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestLocalNetworkPermission()
        enableEdgeToEdge()
        setContent {
            // 季节来自登录页的选择并持久化；未选过则按当前月份。
            var season by remember {
                mutableStateOf(JourneySeason.fromKey(container.settingsStore.journeySeason))
            }
            // 深浅色跟随系统；页面颜色统一由 AppPalette 语义色提供，不再固定浅色。
            OmniRouteTheme(
                season = season,
                dynamicColor = container.settingsStore.isDynamicColorEnabled(),
            ) {
                AppNavHost(
                    container = container,
                    season = season,
                    onSeasonChange = { picked ->
                        season = picked
                        container.settingsStore.journeySeason = picked.key
                    },
                )
            }
        }
    }

    /** Android 16+ 默认拦截应用访问局域网（含模拟器 10.0.2.2），需显式申请才能连开发机 */
    private fun requestLocalNetworkPermission() {
        if (Build.VERSION.SDK_INT < 36) return
        val granted = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_LOCAL_NETWORK,
        ) == PackageManager.PERMISSION_GRANTED
        if (!granted) {
            localNetworkPermission.launch(Manifest.permission.ACCESS_LOCAL_NETWORK)
        }
    }
}
