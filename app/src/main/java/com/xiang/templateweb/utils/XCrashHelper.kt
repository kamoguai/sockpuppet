package com.xiang.templateweb.utils

import android.content.Context
import android.util.Log
import androidx.compose.material3.TimeInput
import com.xiang.templateweb.BuildConfig
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import timber.log.Timber
import xcrash.ICrashCallback
import xcrash.TombstoneManager
import xcrash.TombstoneParser
import xcrash.XCrash

import java.io.File
import java.io.FileWriter
import java.util.UUID
import kotlin.concurrent.thread
import kotlin.math.log
import kotlin.time.ExperimentalTime
import kotlin.time.measureTime

object XCrashHelper {
    private const val TAG = "crash_report"

    @OptIn(ExperimentalTime::class)
    @JvmStatic
    fun init(context: Context) {
        // TODO  Ref : https://github.com/iqiyi/xCrash/blob/master/xcrash_sample/src/main/java/xcrash/sample/MyCustomApplication.java
        // callback for java crash, native crash and ANR
        val callback = ICrashCallback { logPath, emergency ->
            if (emergency != null) {
                debug(logPath, emergency)

                // Disk is exhausted, send crash report immediately.
                sendThenDeleteCrashLog(logPath, emergency)
            } else {
//                TombstoneManager.appendSection(logPath, "Backend Env", "${BuildConfig.BUILD_TYPE} (ip: ${BuildConfig.APP_ENV_IP_VALUE})")
                debug(logPath, null)
            }
        }

        val anrFastCallback = ICrashCallback { logPath, emergency ->
            Log.d(TAG, "anrFastCallback is called logPath: $logPath")
            Log.d(TAG, "anrFastCallback is called emergency: $emergency")
        }

        val costTime = measureTime {
            XCrash.init(
                context,
                XCrash.InitParameters()
                    .setAppVersion(BuildConfig.VERSION_NAME)
                    .setJavaRethrow(true)
                    .setJavaLogCountMax(10)
                    .setJavaDumpAllThreadsWhiteList(
                        arrayOf(
                            "^main$",
                            "^Binder:.*",
                            ".*Finalizer.*"
                        )
                    )
                    .setJavaDumpAllThreadsCountMax(10)
                    .setJavaCallback(callback)
                    .setNativeRethrow(true)
                    .setNativeLogCountMax(10)
                    .setNativeDumpAllThreadsWhiteList(
                        arrayOf(
                            "^xcrash\\.sample$",
                            "^Signal Catcher$",
                            "^Jit thread pool$",
                            ".*(R|r)ender.*",
                            ".*Chrome.*"
                        )
                    )
                    .setNativeDumpAllThreadsCountMax(10)
                    .setNativeCallback(callback)
                    //.setAnrCheckProcessState(false)
                    .setAnrRethrow(true)
                    .setAnrLogCountMax(10)
                    .setAnrCallback(callback)
                    .setAnrFastCallback(anrFastCallback)
                    .setPlaceholderCountMax(3)
                    .setPlaceholderSizeKb(512)
                    //.setLogDir(getExternalFilesDir("xcrash").toString())
                    .setLogFileMaintainDelayMs(1000)
            )
        }
        Log.d(TAG, "xCrash SDK init: ${costTime.inWholeMilliseconds} ms.")

        // Send all pending crash log files.
        thread {
            TombstoneManager.getAllTombstones().forEach { file ->
                sendThenDeleteCrashLog(file.absolutePath, null)
            }
        }
    }

    private  fun sendThenDeleteCrashLog(logPath: String, emergency: String?) {
        try {
            Timber.e("crash logpath -> ${logPath}")
            val map = TombstoneParser.parse(logPath, emergency)
            val crashReport = """
                Tombstone maker: '${map["Tombstone maker"]}'
                Crash type: '${map["Crash type"]}'
                Start time: '${map["Start time"]}'
                Crash time: '${map["Crash time"]}'
                App ID: '${map["App ID"]}'
                App version: '${map["App version"]}'
                Rooted: '${map["Rooted"]}'
                API level: '${map["API level"]}'
                OS version: '${map["OS version"]}'
                Backend Env: '${map["Backend Env"]?.trim()}'
                Kernel version: '${map["Kernel version"]}'
                ABI list: '${map["ABI list"]}'
                Manufacturer: '${map["Manufacturer"]}'
                Brand: '${map["Brand"]}'
                Model: '${map["Model"]}'
                Build fingerprint: '${map["Build fingerprint"]}'
                ABI: '${map["ABI"]}'
                pid: ${map["pid"]}  >>> ${map["pname"]} <<< 
            """.trimIndent()
             //FIXME
            Timber.e("crash crashReport -> ${crashReport}")
            val file = File(logPath)

            // 檢查檔案是否存在
            if (!file.exists()) {
                println("File does not exist: $logPath")
                return
            }
            var appendStr = ""
            // 讀取檔案內容並打印

            file.forEachLine { line ->
                appendStr += line
                Timber.d("append line $appendStr")
                if (line.contains("--------- tail end of log main")) {
                    CoroutineScope(Dispatchers.IO).launch {
                        PostTgUtil.postErrorData("crash report : \n $appendStr")
                    }
                    throw Exception("line found:")

                }
            }



            appendStr = ""

            // remove local crash file
            TombstoneManager.deleteTombstone(logPath)
        } catch (e: Exception) {
            Log.e(TAG, "post lark error:", e)
        }
    }

    private fun debug(logPath: String, emergency: String?) {
        // Parse and save the crash info to a JSON file for debugging.
        var writer: FileWriter? = null
        try {
            val debug = File(XCrash.getLogDir() + "/debug.json")
            debug.createNewFile()

            val map = TombstoneParser.parse(logPath, emergency)

            writer = FileWriter(debug, false)
            writer.write(JSONObject(map.toMap()).toString())
        } catch (e: Exception) {
            Log.d(TAG, "debug failed", e)
        } finally {
            if (writer != null) {
                try {
                    writer.close()
                } catch (ignored: Exception) {
                }
            }
        }
    }
}