package com.example.tabaudit

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.tabaudit.api.LoginRequest
import com.example.tabaudit.api.RetrofitClient
import kotlinx.coroutines.launch

class LoginActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // --- 1. AUTO LOGIN CHECK ---
        // If a token exists, skip login and go straight to Dashboard
        if (SessionManager.getToken(this) != null) {
            startActivity(Intent(this, UserDashboardActivity::class.java))
            finish()
            return
        }

        setContentView(R.layout.activity_login)

        val etUser = findViewById<EditText>(R.id.etUsername)
        val etPass = findViewById<EditText>(R.id.etPassword)

        findViewById<Button>(R.id.btnLogin).setOnClickListener {
            val user = etUser.text.toString()
            val pass = etPass.text.toString()

            if (user.isEmpty() || pass.isEmpty()) {
                Toast.makeText(this, "Please enter creds", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            lifecycleScope.launch {
                try {
                    // Send login request
                    val response = RetrofitClient.api.login(LoginRequest(user, pass))

                    if (response.isSuccessful && response.body() != null) {
                        val auth = response.body()!!

                        // Save Token & Role
                        SessionManager.saveAuth(this@LoginActivity, auth.access, auth.user.role)

                        // Navigate to Dashboard
                        startActivity(Intent(this@LoginActivity, UserDashboardActivity::class.java))
                        finish()
                    } else {
                        Toast.makeText(this@LoginActivity, "Login Failed", Toast.LENGTH_SHORT).show()
                    }
                } catch (e: Exception) {
                    Toast.makeText(this@LoginActivity, "Error: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }
}