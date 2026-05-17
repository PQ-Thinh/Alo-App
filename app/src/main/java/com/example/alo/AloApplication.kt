package com.example.alo

import android.app.Application
import com.example.alo.core.crypto.CryptoHelper
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
