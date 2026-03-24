package com.example.tabaudit.api

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST

interface TabApiService {

    @POST("api/token/")
    suspend fun login(@Body request: LoginRequest): Response<AuthResponse>

    // --- User Endpoints ---
    @GET("api/possession/")
    suspend fun getMyDevices(@Header("Authorization") token: String): Response<List<DevicePossession>>

    @POST("api/assign/")
    suspend fun assignTablet(
        @Header("Authorization") token: String,
        @Body request: AssignRequest
    ): Response<Map<String, String>>

    @POST("api/return/initiate/")
    suspend fun initiateReturn(
        @Header("Authorization") token: String,
        @Body request: ReturnInitRequest
    ): Response<Map<String, String>>

    // --- Admin Endpoints ---
    @GET("api/admin/dashboard/")
    suspend fun getDashboardStats(@Header("Authorization") token: String): Response<AdminStats>

    @POST("api/return/verify/")
    suspend fun verifyReturn(
        @Header("Authorization") token: String,
        @Body request: ReturnVerifyRequest
    ): Response<Map<String, String>>

    @POST("api/transfer/initiate/")
    suspend fun initiateTransfer(
        @Header("Authorization") token: String,
        @Body request: TransferInitRequest
    ): Response<TransferInitResponse>

    @POST("api/transfer/accept/")
    suspend fun acceptTransfer(
        @Header("Authorization") token: String,
        @Body request: TransferAcceptRequest
    ): Response<Map<String, String>>

    @GET("api/user/history/")
    suspend fun getUserHistory(
        @Header("Authorization") token: String
    ): Response<List<UserHistoryItem>>
}