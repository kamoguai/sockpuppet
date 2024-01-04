package com.xiang.ssg.webview

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.webkit.WebView
import android.widget.Toast
import timber.log.Timber
import java.net.URISyntaxException
import java.net.URL
import java.util.Locale
import java.util.regex.Pattern

/**
 * Time: 2023/6/13
 * Author: Xing
 * Descripton:
 *
 *
 */
class ADIntentUtils constructor(private val context: Context) {

    companion object {
        private val ACCEPTED_URI_SCHEME = Pattern.compile("(?i)"
                + '('
                + "(?:http|https|ftp|file)://"
                + "|(?:inline|data|about|javascript):"
                + "|(?:.*:.*@)" + ')' + "(.*)")
        private fun isUrlValid(url: String): Boolean {
//            val regex = Regex(
//                "^((http|https|ftp|file)://([a-zA-Z0-9-]+\\.)+[a-zA-Z]{2,6}(/[a-zA-Z0-9-]*)*([/?#][^\\s]*)?)$"
//            )
//            return regex.matches(url)

            return try {
                val testUrl = URL(url)
                testUrl.toURI()
                true
            } catch (e: Exception) {
                false
            }
        }
    }

    fun shouldOverrideUrlLoadingByApp(view: WebView?, url: String?) : Boolean {
        return shouldOverrideUrlLoadingByInternal(view, url, true)
    }

    private fun shouldOverrideUrlLoadingByInternal(view: WebView?, url: String?, interceptExternalProtocol: Boolean) : Boolean {
        if (isAcceptedScheme(url)) return false
        val intent : Intent

        try {
            Timber.e("特殊跳轉 Intent URL")
            intent = Intent.parseUri(url, Intent.URI_INTENT_SCHEME)
        } catch (e: URISyntaxException) {
            Timber.e("URISyntaxException: %s", e.localizedMessage)
            return false
        }
        intent.component = null

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH_MR1) {
            intent.selector = null
        }

//        if (mActivity.packageManager.resolveActivity(intent, 0) == null) {
//            Timber.e("特殊跳轉 startActivity")
//            return tryHandleByMarket(intent) || interceptExternalProtocol
//        }

        try {
            Timber.e("特殊跳轉 startActivity")
            context.startActivity(intent)
        } catch (e: ActivityNotFoundException) {

            if (intent.resolveActivityInfo(context.packageManager, 0) == null) {
                Timber.e("特殊跳轉 resolveActivityInfo")
                return tryHandleByMarket(intent) || interceptExternalProtocol
            }

            Timber.e("ActivityNotFoundException: %s", e.localizedMessage)
            return false
        }

        return true
    }

    private fun tryHandleByMarket(intent: Intent): Boolean {
        val packageName = intent.`package`
        if (packageName != null) {
            Timber.e("tryHandleByMarket != null")
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=$packageName"))
            try {
                context.startActivity(intent)
            } catch (e: ActivityNotFoundException) {
                Timber.e("tryHandleByMarket Exception")
                Timber.e("ActivityNotFoundException: %s", e.localizedMessage)
                return false
            }
            return true
        } else {
            Timber.e("tryHandleByMarket is null")
            Toast.makeText(context, "App is not installed", Toast.LENGTH_LONG).show()
            return false
        }
    }

    private fun isAcceptedScheme(url: String?) : Boolean {
        val lowerCaseUrl = url?.lowercase(Locale.ENGLISH) ?:return false
        val acceptedUrlSchemeMatcher = isUrlValid(lowerCaseUrl)

        Timber.i("acceptedUrlSchemeMatcher $acceptedUrlSchemeMatcher")

        return acceptedUrlSchemeMatcher
    }
}