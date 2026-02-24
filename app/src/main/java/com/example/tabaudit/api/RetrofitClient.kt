package com.example.tabaudit.api

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object RetrofitClient {
    // IMPORTANT: Use your PC's IP address, NOT localhost.
    // Ensure Django is running with: python manage.py runserver 0.0.0.0:8000
    private const val BASE_URL = "http://172.31.0.203:8000/"

    val api: TabApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(TabApiService::class.java)
    }
}