package com.xiang.templateweb.utils

import android.app.Activity
import android.app.Activity.RESULT_OK
import android.content.Context
import android.content.Intent
import androidx.activity.result.ActivityResultLauncher
import com.firebase.ui.auth.AuthUI
import com.firebase.ui.auth.IdpResponse
import com.firebase.ui.auth.data.model.FirebaseAuthUIAuthenticationResult
import com.google.firebase.auth.FirebaseAuth
import com.google.gson.Gson
import com.xiang.templateweb.BuildConfig


object AuthHelper {

    fun startSignIn(launcher: ActivityResultLauncher<Intent>) {
        val providers = arrayListOf(
            AuthUI.IdpConfig.GoogleBuilder().build(),
        )

        val signInIntent = AuthUI.getInstance()
            .createSignInIntentBuilder()
            .setAvailableProviders(providers)
            .build()

        launcher.launch(signInIntent)
    }

    fun onSignInResult(result: FirebaseAuthUIAuthenticationResult):String {
        val map = HashMap<String, String>()
        var res = ""

        if (result.resultCode == RESULT_OK) {
            val idpToken = result.idpResponse?.idpToken
            val user = FirebaseAuth.getInstance().currentUser
            map["idToken"] = idpToken.toString()
            map["email"] = user?.email.toString()
            map["clientId"] = BuildConfig.GOOGLE_AUTH_CLIENT_ID
            val gson = Gson()
            res = gson.toJson(map)?:""
            // Successfully signed in
            return res
            // ...
        }
        return res
    }

    fun handleSignInResult(resultCode: Int, data: Intent?, onSuccess: () -> Unit, onFailure: (Exception?) -> Unit) {
        if (resultCode == Activity.RESULT_OK) {
            // 登入成功
            onSuccess()
        } else {
            val response = IdpResponse.fromResultIntent(data)
            // 登入失敗
            onFailure(response?.error)
        }
    }

    fun isLoggedIn(): Boolean {
        return FirebaseAuth.getInstance().currentUser != null
    }

    fun googleLogout(context: Context) {
        AuthUI.getInstance()
            .signOut(context)
            .addOnCompleteListener {
                // ...
            }
    }
}