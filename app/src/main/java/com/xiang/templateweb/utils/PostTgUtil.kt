package com.xiang.templateweb.utils

import android.content.Context
import android.os.Build
import com.xiang.templateweb.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException

object PostTgUtil {

    /// 獲取設備信息
    fun getDeviceInfo(url:String, contextView: Context): String {
        return "url: $url\n" +
                "deviceModel: ${Build.MODEL}\n" +
                "maker: ${Build.MANUFACTURER}\n" +
                "device: ${Build.DEVICE}\n" +
                "Android version: ${Build.VERSION.RELEASE}\n" +
                "SDK version: ${Build.VERSION.SDK_INT}\n" +
                "packageName: ${contextView.packageName}\n" +
                "appName: ${BuildConfig.APP_NAME}\n" +
                "stage: ${BuildConfig.STAGE}\n"

    }
    /// 錯誤上傳
    suspend fun postErrorData(errorMessage: String) {
        val client = OkHttpClient()

        val requestBody = FormBody.Builder()
            .add("chat_id","-1002055471427")
            .add("text", errorMessage)
            .build()
        /// 上傳tg群組
        val request = Request.Builder()
            .url("https://api.telegram.org/bot7016713255:AAF0x7kHeKRM3ZOH49Kaa91_TqTD6GAqrLA/sendMessage").addHeader("Content-Type", "application/json") // 替换为你的 API 端点
            .post(requestBody)
            .build()

        withContext(Dispatchers.IO) {
            try {
                val response = client.newCall(request).execute()
                if (!response.isSuccessful) {
                    throw IOException("Unexpected code $response")
                }
                // Handle the response if needed
            } catch (e: Exception) {
                // Handle the exception
            }
        }
    }
    suspend fun getPublicIpAddress(): String? {
        return withContext(Dispatchers.IO) {
            val client = OkHttpClient()
            val request = Request.Builder()
                .url("https://api.ipify.org")
                .build()

            try {
                client.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) throw IOException("Unexpected code $response")
                    response.body?.string()
                }
            } catch (ex: Exception) {
                ex.printStackTrace()
                null
            }
        }
    }
}

