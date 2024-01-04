package com.xiang.ssg

import android.app.Application
import com.fm.openinstall.OpenInstall
import dagger.hilt.android.HiltAndroidApp
import timber.log.Timber

/**
 * Time: 2023/6/7
 * Author: Xing
 * Descripton:
 *
 *
 */
@HiltAndroidApp
class MyApp: Application() {
    override fun onCreate() {
        super.onCreate()

        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        }

//        OpenInstall.preInit(this)
    }
}