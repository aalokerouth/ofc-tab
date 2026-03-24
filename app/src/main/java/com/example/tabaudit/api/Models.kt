package com.example.tabaudit.api

import com.google.gson.annotations.SerializedName

// Login & Auth Models
data class LoginRequest(
    @SerializedName("employee_id") val username: String,
    val password: String
)

data class AuthResponse(
    val access: String,
    val refresh: String,
    val user: UserDetails
)

data class UserDetails(
    val username: String,
    val role: String,
    val employee_id: String
)

// User Dashboard Models
data class DevicePossession(
    val device__serial_number: String,
    val device__tab_type__name: String,
    val issued_at: String
)

data class AssignRequest(val device_id: String? = null, val otp_code: String? = null)
data class ReturnInitRequest(val device_id: String)
data class ReturnVerifyRequest(
    val device_id: String,
    val otp_code: String,
    val condition: String = "Good"
)

// --- UPDATED ADMIN MODELS ---
data class AdminStats(
    val stock: List<StockItem>,
    val stats: StatsCount,
    // REMOVED: active_assignment_otps
    val active_loans: List<ActiveLoanItem>, // NEW: List of active users
    val recent_activity: List<LogItem>,
    val pending_returns: List<PendingReturnItem>
)

data class StockItem(val name: String, val stock_remaining: Int)
data class StatsCount(val total: Int, val available: Int, val assigned: Int)

data class ActiveLoanItem(
    @SerializedName("user__username") val username: String,
    @SerializedName("user__employee_id") val employee_id: String,
    @SerializedName("device__serial_number") val serial: String,
    @SerializedName("device__tab_type__name") val tab_name: String,
    val issued_at: String
)

data class AssignmentOTPItem(
    val otp_code: String,
    val tab_type__name: String
)


data class LogItem(
    @SerializedName("user__username") val username: String,
    @SerializedName("tab__name") val tab_name: String,
    val action: String?, // Changed from quantity to action
    val notes: String?,  // Nullable string so old production data doesn't crash the app
    val timestamp: String
)

// NEW MODEL FOR RETURN OTPS
data class PendingReturnItem(
    @SerializedName("device__serial_number") val serial: String,
    @SerializedName("device__assigned_to__username") val username: String,
    val otp_code: String
)

// --- TRANSFER MODELS ---
data class TransferInitRequest(val device_id: String)

data class TransferInitResponse(
    val message: String,
    val transfer_otp: String,
    val instructions: String
)

data class TransferAcceptRequest(val otp_code: String)

data class UserHistoryItem(
    @SerializedName("tab_name") val tabName: String,
    val action: String,
    val timestamp: String,
    val notes: String? = null // To show transfer details
)