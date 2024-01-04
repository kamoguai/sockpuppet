package com.xiang.ssg.main

import android.app.Application
import android.content.pm.PackageManager
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.xiang.ssg.getApplicationInfoCompat
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Time: 2023/6/7
 * Author: Xing
 * Descripton:
 *
 *
 */
@HiltViewModel
class MainViewModel @Inject constructor(
    application: Application
): ViewModel() {
    private val _uiState = MutableStateFlow(UiState())
    val uiState = _uiState.asStateFlow()

    init {

        viewModelScope.launch {
            val activityInfo = application.packageManager.getApplicationInfoCompat(application.packageName, PackageManager.GET_META_DATA)

            val url = activityInfo.metaData.getString("templateweb.hostname") ?:""
            var proxyHost = activityInfo.metaData.getString("templateweb.proxyHost") ?:""
            var proxyPort = activityInfo.metaData.getInt("templateweb.proxyPort") ?: 0
            var proxyUserName = activityInfo.metaData.getString("templateweb.proxyUserName") ?:""
            var proxyPassword = activityInfo.metaData.getString("templateweb.proxyPassword") ?:""
            val proxyList = activityInfo.metaData.getString("templateweb.proxyList") ?:""
            if(proxyList.isNotEmpty()) {
                val split = proxyList.split(";")
                val ramdomArr = split.random()
                val proxyStr = ramdomArr.split(",")
                proxyHost = proxyStr[0]
                proxyPort = proxyStr[1].toInt()
                proxyUserName = proxyStr[2]
                proxyPassword = proxyStr[3]

            }
            _uiState.update {
                it.copy(url = url, proxyHost = proxyHost,proxyPort = proxyPort,proxyUserName = proxyUserName,proxyPassword = proxyPassword)
            }

        }
    }

    fun setSplashLoading(s: Boolean) {
        viewModelScope.launch {
            _uiState.update {
                it.copy(splashState = s)
            }
        }
    }
}

data class UiState(
    val url: String = "",
    val splashState: Boolean = true,
    val proxyHost: String = "",
    val proxyPort: Int = 0,
    val proxyUserName: String = "",
    val proxyPassword: String = ""

)