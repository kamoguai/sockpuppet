package com.xiang.templateweb.main

import android.annotation.SuppressLint
import android.app.Activity
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.core.view.WindowCompat
//import androidx.hilt.navigation.compose.hiltViewModel
import com.xiang.templateweb.BuildConfig
import com.xiang.templateweb.R
import com.xiang.templateweb.ui.theme.TemplateWebTheme
import com.xiang.templateweb.webview.CustomWebViewScreen
import timber.log.Timber

/**
 * Time: 2023/6/7
 * Author: Xing
 * Descripton:
 * 呈現網路畫面與動畫
 *
 */

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun MainScreen(viewModel: MainViewModel) {
//fun MainScreen(viewModel: MainViewModel = viewModel()) {
//fun MainScreen() {//use di(hilt / dagger / koin) get viewModel
//    val viewModel: MainViewModel = hiltViewModel()
    val view = LocalView.current
    val uiState = viewModel.uiState.collectAsState()

    val imageResource = painterResource(R.drawable.startup_logo)

    if (!view.isInEditMode) {
        //TODO: 不太優雅 缺少顯示狀態值 目前是全黑
        // 更改狀態攔
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = Color.Black.toArgb()
            WindowCompat.getInsetsController(window, view)
        }
    }

    viewModel.getMetaData(view.context.packageManager, BuildConfig.APPLICATION_ID)

    val startupLogo = remember { mutableStateOf(true) }

    if (!uiState.value.splashState) {
        LaunchedEffect(Unit) {
            Timber.i("副作用觸發 startupLogo")
            startupLogo.value = false
        }
    }

    Box(
        modifier = Modifier.fillMaxSize()
    ) {

        Box {
            CustomWebViewScreen(
                url = uiState.value.url,
            )
        }

        AnimatedVisibility(
            visible = startupLogo.value,
            exit = fadeOut(
                animationSpec = tween(durationMillis = 1500)
            ),
        ) {
            Box(
                modifier = Modifier.clickable {
                    startupLogo.value = false
                }
            ) {
                Image(
                    modifier = Modifier.fillMaxSize(),
                    painter = imageResource,
                    contentScale = ContentScale.Crop,
                    contentDescription = "startup logo"
                )
            }
        }

    }

}

@Preview(showBackground = true)
@Composable
fun MainScreenPreview() {
    TemplateWebTheme {
        MainScreen(MainViewModel())
    }
}