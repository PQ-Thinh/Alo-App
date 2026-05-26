package com.example.alo.core.utils

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

enum class NetworkStatus {
    Available, Unavailable, Losing, Lost
}

class NetworkConnectivityObserver(private val context: Context) {
    private val connectivityManager =
        context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

    val status: Flow<NetworkStatus> = callbackFlow {
        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                super.onAvailable(network)
                trySend(NetworkStatus.Available)
            }

            override fun onLosing(network: Network, maxMsToLive: Int) {
                super.onLosing(network, maxMsToLive)
                trySend(NetworkStatus.Losing)
            }

            override fun onLost(network: Network) {
                super.onLost(network)
                trySend(NetworkStatus.Lost)
            }

            override fun onUnavailable() {
                super.onUnavailable()
                trySend(NetworkStatus.Unavailable)
            }
        }

        val request = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()
        connectivityManager.registerNetworkCallback(request, callback)

        // Báo cáo trạng thái hiện tại ngay khi bắt đầu
        val currentNetwork = connectivityManager.activeNetwork
        if (currentNetwork == null) {
            trySend(NetworkStatus.Unavailable)
        } else {
            val actNw = connectivityManager.getNetworkCapabilities(currentNetwork)
            if (actNw != null && (
                actNw.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
                actNw.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) ||
                actNw.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) ||
                actNw.hasTransport(NetworkCapabilities.TRANSPORT_VPN)
            )) {
                trySend(NetworkStatus.Available)
            } else {
                trySend(NetworkStatus.Unavailable)
            }
        }

        awaitClose {
            connectivityManager.unregisterNetworkCallback(callback)
        }
    }
}
