package com.example.tabaudit

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.tabaudit.api.LoginRequest
import com.example.tabaudit.api.RetrofitClient
import kotlinx.coroutines.launch

class LoginActivity : AppCompatActivity() {

    private lateinit var tvCurrentIp: TextView
    private lateinit var etUser: EditText // Declare here to access in onCreate
    private lateinit var etPass: EditText

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 1. Auto-login check (Only if token exists)
        if (SessionManager.getToken(this) != null) {
            startActivity(Intent(this, UserDashboardActivity::class.java))
            finish()
            return
        }

        setContentView(R.layout.activity_login)

        tvCurrentIp = findViewById(R.id.tvCurrentIp)
        etUser = findViewById(R.id.etUsername)
        etPass = findViewById(R.id.etPassword)

        updateIpDisplay()

        // --- NEW: AUTO-FILL EMPLOYEE ID ---
        // If the user was kicked out by AuthInterceptor, their ID will be waiting here.
        val prefs = getSharedPreferences("tab_audit_pref", Context.MODE_PRIVATE)
        val savedUser = prefs.getString("saved_username", "")
        if (!savedUser.isNullOrEmpty()) {
            etUser.setText(savedUser)
        }
        // ----------------------------------

        // --- SETTINGS BUTTON: Change IP ---
        findViewById<ImageButton>(R.id.btnSettings).setOnClickListener {
            showIpDialog()
        }

        findViewById<Button>(R.id.btnLogin).setOnClickListener {
            val user = etUser.text.toString().trim()
            val pass = etPass.text.toString().trim()

            if (user.isEmpty() || pass.isEmpty()) {
                Toast.makeText(this, "Please enter credentials", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            lifecycleScope.launch {
                try {
                    // IMPORTANT: Use .getApi(this) to ensure we use the dynamic IP
                    val api = RetrofitClient.getApi(this@LoginActivity)

                    val response = api.login(LoginRequest(user, pass))

                    if (response.isSuccessful && response.body() != null) {
                        val auth = response.body()!!

                        // --- NEW: SAVE EMPLOYEE ID FOR NEXT TIME ---
                        getSharedPreferences("tab_audit_pref", Context.MODE_PRIVATE).edit()
                            .putString("saved_username", user)
                            .apply()
                        // -------------------------------------------

                        // Play Sound
                        SoundManager.play(this@LoginActivity, R.raw.melody_login)

                        SessionManager.saveAuth(this@LoginActivity, auth.access, auth.user.role)
                        startActivity(Intent(this@LoginActivity, UserDashboardActivity::class.java))
                        finish()
                    } else {
                        Toast.makeText(this@LoginActivity, "Login Failed: Invalid ID or Password", Toast.LENGTH_SHORT).show()
                    }
                } catch (e: Exception) {
                    Toast.makeText(this@LoginActivity, "Connection Error: Check IP or Wifi", Toast.LENGTH_LONG).show()
                    e.printStackTrace()
                }
            }
        }
    }

    private fun showIpDialog() {
        val currentIp = SessionManager.getServerIp(this)

        val input = EditText(this)
        input.setText(currentIp)
        input.hint = "e.g., 192.168.1.10"

        val container = LinearLayout(this)
        container.orientation = LinearLayout.VERTICAL
        container.setPadding(50, 40, 50, 10)
        container.addView(input)

        AlertDialog.Builder(this)
            .setTitle("Set Server IP")
            .setMessage("Enter the IP address of your Django PC.\nPort is fixed at :8000")
            .setView(container)
            .setPositiveButton("Save") { _, _ ->
                val newIp = input.text.toString().trim()
                if (newIp.isNotEmpty()) {
                    SessionManager.saveServerIp(this, newIp)
                    updateIpDisplay()
                    Toast.makeText(this, "IP Updated!", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun updateIpDisplay() {
        val ip = SessionManager.getServerIp(this)
        tvCurrentIp.text = "Server: $ip:8000"
    }
}