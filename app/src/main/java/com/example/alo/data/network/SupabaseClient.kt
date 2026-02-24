package com.example.alo.data.network

import io.github.jan.supabase.BuildConfig
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.gotrue.GoTrue



object SupabaseClient {
    val = createSupabaseClient(
        supabaseUrl = BuildConfig.supabaseUrl,
        supabaseKey = BuildConfig.supabaseKey
    ){
        install(GoTrue)
    }
}