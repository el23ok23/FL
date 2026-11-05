package com.fletaba.app.data

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build

object Network {
    /** true si el teléfono tiene una red con internet (no garantiza que el servidor responda). */
    @Suppress("DEPRECATION")
    fun isOnline(context: Context): Boolean {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager ?: return true
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                val n = cm.activeNetwork ?: return false
                cm.getNetworkCapabilities(n)?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true
            } else {
                cm.activeNetworkInfo?.isConnected == true
            }
        } catch (e: Exception) { true }
    }
}
