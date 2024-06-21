package com.xiang.templateweb

import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen/**/
import com.xiang.templateweb.main.MainScreen
import com.xiang.templateweb.main.MainViewModel
import com.xiang.templateweb.ui.theme.TemplateWebTheme
//import dagger.hilt.android.AndroidEntryPoint
import timber.log.Timber
import java.security.MessageDigest
import java.util.UUID

//@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val viewModel : MainViewModel by viewModels()
    private var keep = true
    private val DELAY = 1000L



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val splashScreen = installSplashScreen()

        splashScreen.setKeepOnScreenCondition {
            if (!keep) {
                Timber.i("結束啟動圖")
                viewModel.setSplashLoading(false)
            }
            keep
        }

        Handler(Looper.getMainLooper()).postDelayed({ keep = false }, DELAY)
        setContent {
            TemplateWebTheme {
                // A surface container using the 'background' color from the theme
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    MainScreen(viewModel)
                }
            }
        }
    }

}
fun PackageManager.getApplicationInfoCompat(packageName: String, flags: Int): ApplicationInfo =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        getApplicationInfo(packageName, PackageManager.ApplicationInfoFlags.of(flags.toLong()))
    } else {
        getApplicationInfo(packageName, flags)
    }