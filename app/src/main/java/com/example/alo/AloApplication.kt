package com.example.alo

import android.app.Application
import android.util.Log
import com.example.alo.data.utils.CryptoHelper
import com.google.firebase.FirebaseApp
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class AloApplication : Application(){
    override fun onCreate() {
        super.onCreate()
            FirebaseApp.initializeApp(this)
            CryptoHelper.initTink()
    }
}