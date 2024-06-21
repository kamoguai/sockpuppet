package com.xiang.templateweb.main

import android.app.Application
import android.content.pm.PackageManager
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseUser
import com.xiang.templateweb.getApplicationInfoCompat
//import dagger.hilt.android.lifecycle.HiltViewModel
//import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

//import javax.inject.Inject

/**
 * Time: 2023/6/7
 * Author: Xing
 * Descripton:
 *
 *
 */
//@HiltViewModel
//class MainViewModel @Inject constructor(
//    application: Application
//): ViewModel() {
class MainViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(UiState())
    val uiState = _uiState.asStateFlow()
    private val _user = MutableStateFlow<FirebaseUser?>(null)
    val user: StateFlow<FirebaseUser?> = _user
    init {

//        viewModelScope.launch {
//            val activityInfo = application.packageManager.getApplicationInfoCompat(application.packageName, PackageManager.GET_META_DATA)
//
//            val url = activityInfo.metaData.getString("templateweb.hostname") ?:""
//
//            _uiState.update {
//                it.copy(url = url)
//            }
//
//        }
    }

    fun getMetaData(packageManager: PackageManager, applicationid: String) {
        viewModelScope.launch {
            val activityInfo =
                packageManager.getApplicationInfoCompat(applicationid, PackageManager.GET_META_DATA)
            val url = activityInfo.metaData.getString("templateweb.hostname") ?: ""
            _uiState.update {
                it.copy(url = url)
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
    fun onUserSignedIn(user: FirebaseUser?) {
        viewModelScope.launch {
            _user.value = user
        }
    }
}

data class UiState(
    val url: String = "",
    val splashState: Boolean = true
)