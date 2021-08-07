package com.cova.jsontocsv

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkInfo

const val channelId="2021"
const val notificationId=1

const val dateFormat = "yyyy-MM-dd"

object Utils {

    fun isConnected(context: Context): Boolean {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val activeNetwork: NetworkInfo? = cm.activeNetworkInfo
        return activeNetwork?.isConnectedOrConnecting == true
    }
}