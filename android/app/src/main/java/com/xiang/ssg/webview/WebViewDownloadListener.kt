package com.xiang.ssg.webview

import android.Manifest
import android.app.DownloadManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Environment
import android.webkit.DownloadListener
import android.webkit.URLUtil
import android.widget.Toast
import timber.log.Timber

/**
 * Time: 2023/6/13
 * Author: Xing
 * Descripton:
 * 下载监听器
 *
 */
class WebViewDownloadListener(private val context: Context) : DownloadListener {

    override fun onDownloadStart(
        url: String?,
        userAgent: String?,
        contentDisposition: String?,
        mimetype: String?,
        contentLength: Long
    ) {

        if (context.checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
            downloadBySystem(url, contentDisposition, mimetype)
        } else {
            // TODO: 沒辦法請求
//            ActivityCompat.requestPermissions(context, arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE), 2)
        }

    }

    /**
     * 跳转浏览器下载
     */
    private fun downloadByBrowser(url : String?) {
        val intent = Intent(Intent.ACTION_VIEW).apply {
            addCategory(Intent.CATEGORY_BROWSABLE)
            data = Uri.parse(url)
        }
        context.startActivity(intent)
    }

    /**
     * 系统的下载服务
     */
    private fun downloadBySystem(url: String?, contentDisposition: String?, mimeType: String?) {
        val request = DownloadManager.Request(Uri.parse(url))

        request.allowScanningByMediaScanner()
        request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
        request.setAllowedOverMetered(true)
        request.setVisibleInDownloadsUi(true)

        request.setAllowedOverRoaming(true)

        request.setAllowedNetworkTypes(DownloadManager.Request.NETWORK_WIFI)

        val fileName = URLUtil.guessFileName(url, contentDisposition, mimeType)
        Timber.d("fileName: $fileName")

        request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, fileName)
        val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager

        val downloadId = downloadManager.enqueue(request)
        Timber.d("downloadId: $downloadId")
        Toast.makeText(context, "Start Download", Toast.LENGTH_LONG).show()
    }
}