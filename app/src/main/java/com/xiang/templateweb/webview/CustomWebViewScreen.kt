package com.xiang.templateweb.webview

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.app.Activity.RESULT_OK

import android.content.pm.PackageManager
import android.net.Uri
import android.net.http.SslError
import android.os.Build
import android.os.Message
import android.text.TextUtils
import android.view.View
import android.view.ViewGroup
import android.webkit.PermissionRequest
import android.webkit.SslErrorHandler
import android.webkit.ValueCallback
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.firebase.ui.auth.FirebaseAuthUIActivityResultContract
import com.google.accompanist.web.AccompanistWebChromeClient
import com.google.accompanist.web.AccompanistWebViewClient
import com.google.accompanist.web.WebView
import com.google.accompanist.web.rememberWebViewState
import com.xiang.templateweb.BuildConfig
import com.xiang.templateweb.utils.AuthHelper
import com.xiang.templateweb.utils.FileUtils
import com.xiang.templateweb.utils.PostTgUtil
import kotlinx.coroutines.launch
import timber.log.Timber
import java.io.File


/**
 * Time: 2023/6/8
 * Author: Xing
 * Descripton:
 *  自訂網路視圖
 *
 */

@SuppressLint("SetJavaScriptEnabled", "UnusedMaterial3ScaffoldPaddingParameter")
@Composable
fun CustomWebViewScreen(
    url: String
) {
    val contextView = LocalContext.current
    var userWebview by remember {
        mutableStateOf<WebView?>(null)
    }
    val adIntentUri = remember {
        ADIntentUtils(contextView)
    }

    var showCustomView by remember {
        mutableStateOf<View?>(null)
    }

    var mFilePathCallback by remember {
        mutableStateOf<ValueCallback<Array<Uri>>?>(null)
    }

    var webkitPermissionRequest by remember {
        mutableStateOf<PermissionRequest?>(null)
    }

    val webViewState = rememberWebViewState(url = url)

    var errorMessage by remember { mutableStateOf<String?>(null) }

    val coroutineScope = rememberCoroutineScope()

    var showOnReceivedErrorSnackbar by remember { mutableStateOf(false) }

    val snackbarHostState = remember { SnackbarHostState() }

    var googleUserInfo by remember { mutableStateOf<String?>(null) }

    val launcher = rememberLauncherForActivityResult(FirebaseAuthUIActivityResultContract()) { result ->
     val res = AuthHelper.onSignInResult(result)
        if (res.isNotEmpty()) {
            userWebview?.evaluateJavascript("javascript:window._mtm.googleLoginCallback($res)",null)
        }

    }
    
    val fileChooserLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (mFilePathCallback == null) return@rememberLauncherForActivityResult

        val intent = result.data
        val data = intent?.data
        if (result.resultCode == RESULT_OK && data != null) {
            val path = FileUtils.getPath(contextView, data)
            if (TextUtils.isEmpty(path)) {
                mFilePathCallback?.onReceiveValue(null)
            } else {
                val uri = Uri.fromFile(path?.let { File(it) })
                mFilePathCallback?.onReceiveValue(arrayOf(uri))
            }
        } else {
            mFilePathCallback?.onReceiveValue(null)
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
        if (isGranted) {
            webkitPermissionRequest?.grant(webkitPermissionRequest?.resources)
        } else {
            webkitPermissionRequest?.deny()
        }

    }

    val client = remember {
        object : AccompanistWebViewClient() {
            override fun shouldOverrideUrlLoading(
                view: WebView?,
                request: WebResourceRequest?
            ): Boolean {
//                AuthHelper.googleLogout(contextView)
                val reqUrl = request?.url.toString()
                if (adIntentUri.shouldOverrideUrlLoadingByApp(view, reqUrl)) {
                    return true
                }
                return super.shouldOverrideUrlLoading(view, request)
            }

            @SuppressLint("WebViewClientOnReceivedSslError")
            override fun onReceivedSslError(
                view: WebView?,
                handler: SslErrorHandler?,
                error: SslError?
            ) {
                handler?.proceed()
            }

            override fun onReceivedError(
                view: WebView,
                request: WebResourceRequest?,
                error: WebResourceError?
            ) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    super.onReceivedError(view, request, error)
                    if(error?.errorCode == WebViewClient.ERROR_HOST_LOOKUP || error?.errorCode == WebViewClient.ERROR_CONNECT){
                        coroutineScope.launch {
                            // 获取设备信息
                            val deviceInfo = PostTgUtil.getDeviceInfo(url,contextView)
                            val deviceIP = PostTgUtil.getPublicIpAddress()
                            // 设置错误信息
                            errorMessage = "error msg: ${error.description}\ndeviceInfo: \n$deviceInfo"
                            errorMessage += "deviceIP: $deviceIP\n"
                            Timber.e(errorMessage)
                            if(error.errorCode == ERROR_CONNECT){
                                showOnReceivedErrorSnackbar = true
                            }
                            PostTgUtil.postErrorData(errorMessage!!)
                        }
                    }
                }


            }

            // For SDK below 23
            @Deprecated("Deprecated in Java")
            override fun onReceivedError(view: WebView?, errorCode: Int, description: String?, failingUrl: String?) {
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
                    super.onReceivedError(view, errorCode, description, failingUrl)
                    if (errorCode == ERROR_HOST_LOOKUP || errorCode == ERROR_CONNECT) {
                        coroutineScope.launch {
                            // 获取设备信息
                            val deviceInfo = PostTgUtil.getDeviceInfo(url,contextView)
                            val deviceIP = PostTgUtil.getPublicIpAddress()
                            // 设置错误信息
                            errorMessage = "error msg: $description\ndeviceInfo: \n$deviceInfo"
                            errorMessage += "deviceIP: $deviceIP\n"
                            Timber.e(errorMessage)
                            if(errorCode == ERROR_CONNECT){
                                showOnReceivedErrorSnackbar = true
                            }
                            PostTgUtil.postErrorData(errorMessage!!)
                        }
                    }
                }

            }

        }
    }

    val chromeClient = remember { object : AccompanistWebChromeClient() {

        // 顯示自定義螢幕
        // 目前用來顯示全螢幕
        override fun onShowCustomView(view: View?, callback: CustomViewCallback?) {
            showCustomView = view
        }

        // 關閉自定義螢幕
        override fun onHideCustomView() {
            showCustomView = null
        }

        // 請求權限
        override fun onPermissionRequest(request: PermissionRequest?) {
            webkitPermissionRequest = request

            request?.resources?.forEach {
                when(it) {
                    PermissionRequest.RESOURCE_AUDIO_CAPTURE -> {
                        if (ContextCompat.checkSelfPermission(contextView, PermissionRequest.RESOURCE_AUDIO_CAPTURE) == PackageManager.PERMISSION_GRANTED) {
                            request.grant(arrayOf(it))
                        } else {
                            permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                        }
                    }
                    PermissionRequest.RESOURCE_VIDEO_CAPTURE -> {
                        if (ContextCompat.checkSelfPermission(contextView, PermissionRequest.RESOURCE_VIDEO_CAPTURE) == PackageManager.PERMISSION_GRANTED) {
                            request.grant(arrayOf(it))
                        } else {
                            permissionLauncher.launch(Manifest.permission.CAMERA)
                        }
                    }
                }
            }

        }

        // 檔案選擇
        override fun onShowFileChooser(
            webView: WebView?,
            filePathCallback: ValueCallback<Array<Uri>>?,
            fileChooserParams: FileChooserParams?
        ): Boolean {
            Timber.i("開啟檔案選擇")
            mFilePathCallback = filePathCallback
            var type = ""
            if (fileChooserParams != null && fileChooserParams.acceptTypes != null && fileChooserParams.acceptTypes.isNotEmpty()) {
                type = if(!TextUtils.isEmpty(fileChooserParams.acceptTypes[0])) fileChooserParams.acceptTypes[0] else "*/*"
            }

            val intent = fileChooserParams?.createIntent() ?:return false
            Timber.d("onShowFileChooser type: $type")

            return try {
                fileChooserLauncher.launch(intent)
                true
            } catch (e: Exception) {
                mFilePathCallback = null
                Toast.makeText(webView?.context, "FileChooser error.fileChooserParams:${type}", Toast.LENGTH_SHORT).show()
                false
            }
        }

        // 新開分頁
        override fun onCreateWindow(
            view: WebView?,
            isDialog: Boolean,
            isUserGesture: Boolean,
            resultMsg: Message?
        ): Boolean {
            Timber.i("另開新分頁")
            // TODO: 尚未實作
            val transport = resultMsg?.obj as WebView.WebViewTransport
            transport.webView = view
            resultMsg.sendToTarget()
            return super.onCreateWindow(view, isDialog, isUserGesture, resultMsg)
        }
    } }
    LaunchedEffect(showOnReceivedErrorSnackbar) {
        if (showOnReceivedErrorSnackbar) {
            snackbarHostState.showSnackbar(
                message = "Unable to connect to the server, please check your network connection.",
                actionLabel = "Dismiss"
            )
            showOnReceivedErrorSnackbar = false
        }
    }
    Scaffold(snackbarHost = { SnackbarHost(snackbarHostState) }) {
        Box(
            modifier = Modifier.fillMaxSize()
        ) {
            WebView(
                state = webViewState,
                modifier = Modifier.fillMaxSize(),
                client = client,
                chromeClient = chromeClient,
                factory = { context ->
                    userWebview =  WebView(context)
                    userWebview?.apply {
                        /// 添加javascriptInterface function
                        addJavascriptInterface(JsFunction(context, context as Activity, launcher),"skywinApp")
                        layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)

                        settings.cacheMode = WebSettings.LOAD_DEFAULT
                        settings.javaScriptEnabled = true
                        settings.setSupportZoom(false)

                        // 允許新開分頁 需要額外實作 WebChromeClient.onCreateWindow
                        settings.setSupportMultipleWindows(false)

                        settings.domStorageEnabled = true
                        settings.javaScriptCanOpenWindowsAutomatically = true
                        settings.mediaPlaybackRequiresUserGesture = false

                        // 地理位置請求 尚未實作 onGeolocationPermissionsShowPrompt
                        settings.setGeolocationEnabled(true)

                        // 設定可以訪問檔案
                        settings.allowFileAccess = true

                        //自適應 手機窗口
                        settings.useWideViewPort = true

                        settings.loadWithOverviewMode = true
                        //自適應 彈出窗口 控制 默認正常顯示 不渲染
                        settings.layoutAlgorithm = WebSettings.LayoutAlgorithm.NORMAL

                        // 5.1以上默认禁止了https和http混用，以下方式是开启
                        settings.mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW

                        /**
                         * 調適模式
                         */
                        WebView.setWebContentsDebuggingEnabled(BuildConfig.DEBUG)

                        setDownloadListener(WebViewDownloadListener(context))
                    }!!
                },
            )

            if (showCustomView != null) {
                // 自定義畫面
                AndroidView(factory = {
                    showCustomView!!
                }) {

                }
            }
        }
    }
}

private fun handleResultAboveLollipop(result: Uri?) {

}