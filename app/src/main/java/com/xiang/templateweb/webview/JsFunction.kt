package com.xiang.templateweb.webview

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.webkit.JavascriptInterface
import androidx.activity.result.ActivityResultLauncher
import com.xiang.templateweb.utils.AuthHelper

class JsFunction constructor(private val context: Context, private val activity: Activity, private val launcher: ActivityResultLauncher<Intent>) {
    /**
     * 簡單測試window open function
     *
     */
    @JavascriptInterface
    fun openDefaultBrowser(url:String) {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
        context.startActivity(intent)
    }
    /**
     * google login function
     *
     */
    @JavascriptInterface
    fun googleLogin(){
        activity.runOnUiThread {
            AuthHelper.startSignIn(launcher)
        }
    }
}