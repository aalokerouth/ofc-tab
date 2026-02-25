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

    // Create a shared OkHttp client for better performance
    private val client: OkHttpClient by lazy {
        val logging = HttpLoggingInterceptor()
        logging.setLevel(HttpLoggingInterceptor.Level.BODY)

        OkHttpClient.Builder()
            .addInterceptor(logging)
            .connectTimeout(15, TimeUnit.SECONDS) // Handle slow LAN connections
            .readTimeout(15, TimeUnit.SECONDS)
            .build()
    }

    // Now requires Context to get the saved IP
    fun getApi(context: Context): TabApiService {
        val ip = SessionManager.getServerIp(context)
        val newUrl = "http://$ip:8000/"

        // Rebuild Retrofit only if the URL has changed or it's null
        if (retrofit == null || newUrl != currentBaseUrl) {
            retrofit = Retrofit.Builder()
                .baseUrl(newUrl)
                .client(client)
                .addConverterFactory(GsonConverterFactory.create())
                .build()

            currentBaseUrl = newUrl
        }

        return retrofit!!.create(TabApiService::class.java)
    }
}