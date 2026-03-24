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

        // If the user was kicked out by AuthInterceptor, their ID will be waiting here.
        val prefs = getSharedPreferences("tab_audit_pref", Context.MODE_PRIVATE)
        val savedUser = prefs.getString("saved_username", "")
        if (!savedUser.isNullOrEmpty()) {
            etUser.setText(savedUser)
        }

        // --- SETTINGS BUTTON: Change IP & PORT ---
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
                    // IMPORTANT: Use .getApi(this) to ensure we use the dynamic IP/Port
                    val api = RetrofitClient.getApi(this@LoginActivity)

                    val response = api.login(LoginRequest(user, pass))

                    if (response.isSuccessful && response.body() != null) {
                        val auth = response.body()!!

                        // SAVE EMPLOYEE ID FOR NEXT TIME
                        getSharedPreferences("tab_audit_pref", Context.MODE_PRIVATE).edit()
                            .putString("saved_username", user)
                            .apply()

                        // Play Sound
                        try { SoundManager.play(this@LoginActivity, R.raw.melody_login) } catch(e: Exception) {}

                        SessionManager.saveAuth(this@LoginActivity, auth.access, auth.user.role)
                        startActivity(Intent(this@LoginActivity, UserDashboardActivity::class.java))
                        finish()
                    } else {
                        Toast.makeText(this@LoginActivity, "Login Failed: Invalid ID or Password", Toast.LENGTH_SHORT).show()
                    }
                } catch (e: Exception) {
                    Toast.makeText(this@LoginActivity, "Connection Error: Check IP, Port, or Wifi", Toast.LENGTH_LONG).show()
                    e.printStackTrace()
                }
            }
        }
    }

    private fun showIpDialog() {
        val currentIp = SessionManager.getServerIp(this)
        val currentPort = SessionManager.getServerPort(this)

        val ipInput = EditText(this)
        ipInput.setText(currentIp)
        ipInput.hint = "IP (e.g., 192.168.1.10)"

        val portInput = EditText(this)
        portInput.setText(currentPort)
        portInput.hint = "Port (e.g., 8000)"
        portInput.inputType = android.text.InputType.TYPE_CLASS_NUMBER

        val container = LinearLayout(this)
        container.orientation = LinearLayout.VERTICAL
        container.setPadding(50, 40, 50, 10)
        container.addView(ipInput)

        // Add a tiny bit of space between the two inputs
        val params = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
        params.topMargin = 20
        container.addView(portInput, params)

        AlertDialog.Builder(this)
            .setTitle("Set Server IP & Port")
            .setMessage("Enter the details for your Django PC.")
            .setView(container)
            .setPositiveButton("Save") { _, _ ->
                val newIp = ipInput.text.toString().trim()
                val newPort = portInput.text.toString().trim()

                if (newIp.isNotEmpty() && newPort.isNotEmpty()) {
                    SessionManager.saveServerIp(this, newIp)
                    SessionManager.saveServerPort(this, newPort)
                    updateIpDisplay()
                    Toast.makeText(this, "Server Settings Updated!", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this, "IP and Port cannot be empty!", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun updateIpDisplay() {
        val ip = SessionManager.getServerIp(this)
        val port = SessionManager.getServerPort(this)
        tvCurrentIp.text = "Server: $ip:$port"
    }
}