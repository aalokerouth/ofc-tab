package com.example.tabaudit

import android.content.Context
import android.content.SharedPreferences

object SessionManager {
    private const val PREF_NAME = "tab_audit_pref"
    private const val KEY_TOKEN = "access_token"
    private const val KEY_ROLE = "user_role"
    private const val KEY_SERVER_IP = "server_ip"
    private const val KEY_SERVER_PORT = "server_port" // --- NEW ---

    // Default IP and Port (Fallback)
    private const val DEFAULT_IP = "172.31.0.203"
    private const val DEFAULT_PORT = "8000" // --- NEW ---

    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
    }

    // --- AUTH METHODS ---
    fun saveAuth(context: Context, token: String, role: String) {
        getPrefs(context).edit()
            .putString(KEY_TOKEN, "Bearer $token")
            .putString(KEY_ROLE, role)
            .apply()
    }

    fun getToken(context: Context): String? {
        return getPrefs(context).getString(KEY_TOKEN, null)
    }

    fun getRole(context: Context): String? {
        return getPrefs(context).getString(KEY_ROLE, "user")
    }

    fun clear(context: Context) {
        // Keep the IP address and Port even after logout!
        val ip = getServerIp(context)
        val port = getServerPort(context)

        getPrefs(context).edit().clear().apply()

        saveServerIp(context, ip)
        saveServerPort(context, port)
    }

    // --- IP & PORT ADDRESS METHODS ---
    fun saveServerIp(context: Context, ip: String) {
        getPrefs(context).edit().putString(KEY_SERVER_IP, ip).apply()
    }

    fun getServerIp(context: Context): String {
        return getPrefs(context).getString(KEY_SERVER_IP, DEFAULT_IP) ?: DEFAULT_IP
    }

    fun saveServerPort(context: Context, port: String) {
        getPrefs(context).edit().putString(KEY_SERVER_PORT, port).apply()
    }

    fun getServerPort(context: Context): String {
        return getPrefs(context).getString(KEY_SERVER_PORT, DEFAULT_PORT) ?: DEFAULT_PORT
    }
}