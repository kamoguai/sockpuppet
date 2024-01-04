package com.xiang.ssg.webview

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity.RESULT_OK
import android.content.ActivityNotFoundException
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.net.http.SslError
import android.os.Build
import android.os.Message
import android.text.TextUtils
import android.util.Log
import android.view.View
import android.webkit.HttpAuthHandler
import android.webkit.PermissionRequest
import android.webkit.SslErrorHandler
import android.webkit.ValueCallback
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.webkit.ProxyConfig
import androidx.webkit.ProxyController
import androidx.webkit.WebViewFeature
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.web.AccompanistWebChromeClient
import com.google.accompanist.web.AccompanistWebViewClient
import com.google.accompanist.web.WebView
import com.google.accompanist.web.rememberWebViewNavigator
import com.google.accompanist.web.rememberWebViewState
import com.xiang.ssg.BuildConfig
import com.xiang.ssg.utils.FileUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import okhttp3.Dispatcher
import timber.log.Timber
import java.io.File
import java.lang.Exception
import java.lang.reflect.Method
import java.net.Inet4Address
import java.net.InetAddress
import java.net.UnknownHostException
import java.util.concurrent.Executor

/**
 * Time: 2023/6/8
 * Author: Xing
 * Descripton:
 *  自訂網路視圖
 *
 */
@OptIn(ExperimentalPermissionsApi::class)
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun CustomWebViewScreen(
    url: String,
    myWebView: WebView? = null,
    onCreateWindow:(WebView) -> Unit,
    onBack:(() -> Unit)? = null,
    proxyHost: String?,
    proxyPort: Int?,
    proxyUserName: String?,
    proxyPassword: String?
) {

    val contextView = LocalContext.current

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

    val navigator = rememberWebViewNavigator()
    
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
                if (adIntentUri.shouldOverrideUrlLoadingByApp(view, request?.url.toString())) {
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
                Timber.e("onReceivedSslError驗證錯誤？ $error")
            }

            override fun onReceivedHttpError(
                view: WebView?,
                request: WebResourceRequest?,
                errorResponse: WebResourceResponse?
            ) {
                super.onReceivedHttpError(view, request, errorResponse)
                Timber.e("onReceivedHttpError驗證錯誤？ $errorResponse")
            }

            override fun onReceivedError(
                view: WebView?,
                request: WebResourceRequest?,
                error: WebResourceError?
            ) {
                super.onReceivedError(view, request, error)
                Timber.e("onReceivedError驗證錯誤？ $error")
            }

            override fun onReceivedHttpAuthRequest(
                view: WebView?,
                handler: HttpAuthHandler?,
                host: String?,
                realm: String?
            ) {
                handler?.proceed(proxyUserName,proxyPassword)
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
            val acceptTypes = fileChooserParams?.acceptTypes

            if (!acceptTypes.isNullOrEmpty()) {
                Timber.d("onShowFileChooser acceptTypes is not empty.")
                val intent = Intent(Intent.ACTION_OPEN_DOCUMENT)

                intent.addCategory(Intent.CATEGORY_OPENABLE)
                intent.type = "*/*"

                intent.putExtra(Intent.EXTRA_MIME_TYPES, acceptTypes)

                type = acceptTypes.toString()

                Timber.d("onShowFileChooser type: $type")

                return try {
                    fileChooserLauncher.launch(intent)
                    true
                } catch (e: ActivityNotFoundException) {
                    mFilePathCallback = null
                    Toast.makeText(webView?.context, "FileChooser error.fileChooserParams:${type}", Toast.LENGTH_SHORT).show()
                    // TODO: 尚未實作

            //                    showErrorDialog(
            //                        webView?.context!!,
            //                        "FileChooser Error",
            //                        e.message ?:"not message"
            //                    )
                    false
                }
            } else {
                Timber.d("onShowFileChooser acceptTypes is empty.")

                val intent = fileChooserParams?.createIntent() ?:return false

                type = intent.type ?:"not type"

                Timber.d("onShowFileChooser type: $type")

                return try {
                    fileChooserLauncher.launch(intent)
                    true
                } catch (e: ActivityNotFoundException) {
                    mFilePathCallback = null
                    Toast.makeText(webView?.context, "FileChooser error.fileChooserParams:${type}", Toast.LENGTH_SHORT).show()
                    // TODO: 尚未實作

            //                    showErrorDialog(
            //                        webView?.context!!,
            //                        "FileChooser Error",
            //                        e.message ?:"not message"
            //                    )
                    false
                }
            }
        }

        // 新開分頁
        override fun onCreateWindow(
            oldView: WebView,
            isDialog: Boolean,
            isUserGesture: Boolean,
            resultMsg: Message?
        ): Boolean {
            Timber.i("另開新分頁")

            val newWebView = WebView(oldView.context)

            val transport = resultMsg?.obj as WebView.WebViewTransport
            transport.webView = newWebView
            resultMsg.sendToTarget()
            onCreateWindow.invoke(newWebView)
            return true
        }


    } }

    BackHandler(navigator.canGoBack) {
        onBack?.invoke()
    }

    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        WebView(
            state = webViewState,
            modifier = Modifier.fillMaxSize(),
            client = client,
            chromeClient = chromeClient,
            navigator = navigator,
//            onCreated = {webView ->  initProxy(webView) },
            factory = { context ->
                val childView = myWebView ?: WebView(context)
                main()
                setProxy(proxyHost,proxyPort)
                childView.apply {
                    settings.javaScriptEnabled = true

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
                }
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

private fun handleResultAboveLollipop(result: Uri?) {

}
fun setProxy(proxyHost:String? , proxyPort: Int?) {

    if(WebViewFeature.isFeatureSupported(WebViewFeature.PROXY_OVERRIDE)){

        val proxyConfig: ProxyConfig = ProxyConfig.Builder()
            .addProxyRule("$proxyHost:$proxyPort")
            .addDirect()
            .build()

        ProxyController.getInstance().setProxyOverride(
            proxyConfig,
            Executor { it.run()},
            Runnable { Timber.d("webview代理改變") }
        )

    }
    else {
        Timber.i("WTF why cannot connect")
    }
}
fun main() {

    runBlocking {
        launch(Dispatchers.IO) {
            val domain = "ssapp0.com"
            val ip = getIpv4Address(domain)
            println("Resolved IP for $domain: $ip")

//            withContext(Dispatchers.Main) {
//                println("Resolved IP for $domain: $ip")
//            }
        }
    }

}
fun getIpv4Address(domain:String): String? {
    return try {
        val addresses = InetAddress.getAllByName(domain)
        // 優先尋找 IPv4 地址
        val ipv4Address = addresses.firstOrNull { it is Inet4Address }
        ipv4Address?.hostAddress ?: addresses.firstOrNull()?.hostAddress
    }catch (e: UnknownHostException) {
        println("无法解析域名: $domain")
        null
    }
}