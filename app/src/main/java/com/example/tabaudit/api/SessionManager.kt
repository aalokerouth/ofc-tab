package com.example.tabaudit

import android.content.Context

object SessionManager {
    private const val PREF_NAME = "tab_audit_pref"
    private const val KEY_TOKEN = "access_token"
    private const val KEY_ROLE = "user_role"

    fun saveAuth(context: Context, token: String, role: String) {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        prefs.edit()
            .putString(KEY_TOKEN, "Bearer $token")
            .putString(KEY_ROLE, role)
            .apply()
    }

    fun getToken(context: Context): String? {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        return prefs.getString(KEY_TOKEN, null)
    }

    fun getRole(context: Context): String? {
        return context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE).getString(KEY_ROLE, "user")
    }

    // NEW: Clear data for Logout
    fun clear(context: Context) {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        prefs.edit().clear().apply()
    }
}