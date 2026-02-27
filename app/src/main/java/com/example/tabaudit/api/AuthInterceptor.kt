package com.example.tabaudit.api

import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.Looper
import com.example.tabaudit.LoginActivity
import com.example.tabaudit.SessionManager
import okhttp3.Interceptor
import okhttp3.Response
import java.io.IOException

class AuthInterceptor(private val context: Context) : Interceptor {
    @Throws(IOException::class)
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val response = chain.proceed(request)

        // IF server says "Unauthorized" (401), it means Token Expired
        if (response.code == 401) {

            // 1. Wipe the bad token
            SessionManager.clear(context)

            // 2. Force open Login Screen (Run on UI Thread)
            Handler(Looper.getMainLooper()).post {
                val intent = Intent(context, LoginActivity::class.java)
                // Clear the back stack so they can't go back to Dashboard
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                context.startActivity(intent)
            }
        }
        return response
    }
}