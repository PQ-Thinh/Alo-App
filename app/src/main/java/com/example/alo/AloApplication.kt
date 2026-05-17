package com.example.alo

import android.app.Application
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import com.example.alo.core.crypto.CryptoHelper
import com.google.firebase.FirebaseApp
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class AloApplication : Application() {

    companion object {
        var isAppInForeground = false
            private set
    }

    override fun onCreate() {
        super.onCreate()
        FirebaseApp.initializeApp(this)
        CryptoHelper.initTink()

        ProcessLifecycleOwner.get().lifecycle.addObserver(object : DefaultLifecycleObserver {
            override fun onStart(owner: LifecycleOwner) {
                isAppInForeground = true
            }

            override fun onStop(owner: LifecycleOwner) {
                isAppInForeground = false
            }
        })
    }
}
