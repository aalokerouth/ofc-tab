package com.example.tabaudit

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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Auto-login check
        if (SessionManager.getToken(this) != null) {
            startActivity(Intent(this, UserDashboardActivity::class.java))
            finish()
            return
        }

        setContentView(R.layout.activity_login)

        tvCurrentIp = findViewById(R.id.tvCurrentIp)
        updateIpDisplay()

        val etUser = findViewById<EditText>(R.id.etUsername)
        val etPass = findViewById<EditText>(R.id.etPassword)

        // --- SETTINGS BUTTON: Change IP ---
        findViewById<ImageButton>(R.id.btnSettings).setOnClickListener {
            showIpDialog()
        }

        findViewById<Button>(R.id.btnLogin).setOnClickListener {
            val user = etUser.text.toString()
            val pass = etPass.text.toString()

            if (user.isEmpty() || pass.isEmpty()) {
                Toast.makeText(this, "Please enter creds", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            lifecycleScope.launch {
                try {
                    // IMPORTANT: Use .getApi(this) to ensure we use the dynamic IP
                    val api = RetrofitClient.getApi(this@LoginActivity)

                    val response = api.login(LoginRequest(user, pass))

                    if (response.isSuccessful && response.body() != null) {
                        val auth = response.body()!!

                        // Play Sound
                        SoundManager.play(this@LoginActivity, R.raw.melody_login)

                        SessionManager.saveAuth(this@LoginActivity, auth.access, auth.user.role)
                        startActivity(Intent(this@LoginActivity, UserDashboardActivity::class.java))
                        finish()
                    } else {
                        Toast.makeText(this@LoginActivity, "Login Failed", Toast.LENGTH_SHORT).show()
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