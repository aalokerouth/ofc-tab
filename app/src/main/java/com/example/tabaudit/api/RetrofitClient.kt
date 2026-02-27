package com.example.tabaudit.api

import android.content.Context
import com.example.tabaudit.SessionManager
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object RetrofitClient {

    private var retrofit: Retrofit? = null
    private var currentBaseUrl: String? = null

    // Helper to build the client with the AuthInterceptor
    private fun buildClient(context: Context): OkHttpClient {
        val logging = HttpLoggingInterceptor()
        logging.setLevel(HttpLoggingInterceptor.Level.BODY)

        return OkHttpClient.Builder()
            .addInterceptor(logging)
            // ADD THIS LINE TO ATTACH THE KILL SWITCH:
            .addInterceptor(AuthInterceptor(context))
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .build()
    }

    fun getApi(context: Context): TabApiService {
        val ip = SessionManager.getServerIp(context)
        val newUrl = "http://$ip:8000/"

        // Rebuild Retrofit if URL changes OR if it's null
        if (retrofit == null || newUrl != currentBaseUrl) {
            retrofit = Retrofit.Builder()
                .baseUrl(newUrl)
                .client(buildClient(context)) // Pass context to build the client
                .addConverterFactory(GsonConverterFactory.create())
                .build()

            currentBaseUrl = newUrl
        }

        return retrofit!!.create(TabApiService::class.java)
    }
}