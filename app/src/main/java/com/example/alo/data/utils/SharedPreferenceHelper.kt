package com.example.alo.data.utils

import android.content.Context

class SharedPreferenceHelper(private val context: Context)
{
    companion object{
        private const val PREF_NAME = "MyPrefs"
    }
    fun saveStringData(key: String, data: String) {
        val sharedPreferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        sharedPreferences.edit().putString(key,data).apply()
    }

    fun getStringData(key: String): String? {
        val sharedPreferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        return sharedPreferences.getString(key, null)
    }
}