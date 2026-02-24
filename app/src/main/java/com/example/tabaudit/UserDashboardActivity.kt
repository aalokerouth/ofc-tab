package com.example.tabaudit

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.example.tabaudit.api.AssignRequest
import com.example.tabaudit.api.ReturnInitRequest
import com.example.tabaudit.api.ReturnVerifyRequest
import com.example.tabaudit.api.RetrofitClient
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton
import com.google.mlkit.vision.codescanner.GmsBarcodeScanning
import kotlinx.coroutines.launch

class UserDashboardActivity : AppCompatActivity() {

    private lateinit var adapter: DevicesAdapter
    private lateinit var swipeRefresh: SwipeRefreshLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_user_dashboard)

        // 1. Setup Admin Button Visibility
        val btnAdmin = findViewById<Button>(R.id.btnGoToAdmin)
        val role = SessionManager.getRole(this)

        if (role == "admin") {
            btnAdmin.visibility = View.VISIBLE
            btnAdmin.setOnClickListener {
                startActivity(Intent(this, AdminDashboardActivity::class.java))
            }
        } else {
            btnAdmin.visibility = View.GONE
        }

        // 2. Setup Logout Button
        findViewById<View>(R.id.btnLogout).setOnClickListener {
            SessionManager.clear(this)
            val intent = Intent(this, LoginActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
        }

        // 3. Setup RecyclerView
        val rv = findViewById<RecyclerView>(R.id.rvDevices)
        rv.layoutManager = LinearLayoutManager(this)
        adapter = DevicesAdapter(emptyList()) { device ->
            confirmReturn(device.device__serial_number)
        }
        rv.adapter = adapter

        // 4. Setup Swipe Refresh
        swipeRefresh = findViewById(R.id.swipeRefresh)
        swipeRefresh.setOnRefreshListener {
            loadData()
        }

        // 5. Setup Scan Button
        findViewById<ExtendedFloatingActionButton>(R.id.fabScan).setOnClickListener {
            val scanner = GmsBarcodeScanning.getClient(this)
            scanner.startScan()
                .addOnSuccessListener { barcode ->
                    barcode.rawValue?.let { serial -> assignTablet(serial) }
                }
                .addOnFailureListener {
                    Toast.makeText(this, "Scan Cancelled", Toast.LENGTH_SHORT).show()
                }
        }

        // Load initial data
        loadData()
    }

    private fun loadData() {
        val token = SessionManager.getToken(this) ?: return
        lifecycleScope.launch {
            try {
                val response = RetrofitClient.api.getMyDevices(token)
                if (response.isSuccessful) {
                    adapter.updateList(response.body() ?: emptyList())
                }
            } catch (e: Exception) {
                Toast.makeText(this@UserDashboardActivity, "Error loading list", Toast.LENGTH_SHORT).show()
            } finally {
                swipeRefresh.isRefreshing = false // Stop animation
            }
        }
    }

    private fun assignTablet(serial: String) {
        val token = SessionManager.getToken(this) ?: return
        lifecycleScope.launch {
            try {
                val response = RetrofitClient.api.assignTablet(token, AssignRequest(device_id = serial))
                if (response.isSuccessful) {
                    Toast.makeText(this@UserDashboardActivity, "Assigned Successfully!", Toast.LENGTH_LONG).show()
                    loadData()
                } else {
                    Toast.makeText(this@UserDashboardActivity, "Failed: Device might be taken or Invalid", Toast.LENGTH_LONG).show()
                }
            } catch (e: Exception) {
                Toast.makeText(this@UserDashboardActivity, "Network Error", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun confirmReturn(serial: String) {
        AlertDialog.Builder(this)
            .setTitle("Return Device")
            .setMessage("Are you ready to return? You will need to ask the Admin for the Return OTP.")
            .setPositiveButton("Yes") { _, _ -> initiateReturn(serial) }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun initiateReturn(serial: String) {
        val token = SessionManager.getToken(this) ?: return
        lifecycleScope.launch {
            try {
                // Step 1: Tell Backend we want to return
                val response = RetrofitClient.api.initiateReturn(token, ReturnInitRequest(serial))

                if (response.isSuccessful) {
                    // Step 2: Backend generated OTP. Now ask User to enter it.
                    showOtpInputDialog(serial)
                } else {
                    Toast.makeText(this@UserDashboardActivity, "Failed to initiate return", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(this@UserDashboardActivity, "Network Error", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun showOtpInputDialog(serial: String) {
        val input = EditText(this)
        input.hint = "Enter OTP from Admin"
        input.inputType = android.text.InputType.TYPE_CLASS_NUMBER

        val container = LinearLayout(this)
        container.orientation = LinearLayout.VERTICAL
        container.setPadding(50, 40, 50, 10)
        container.addView(input)

        AlertDialog.Builder(this)
            .setTitle("Enter Return OTP")
            .setMessage("Please ask the Admin for the code displayed on their dashboard.")
            .setView(container)
            .setPositiveButton("Verify Return") { _, _ ->
                val otp = input.text.toString()
                if (otp.length == 6) {
                    verifyReturn(serial, otp)
                } else {
                    Toast.makeText(this, "Invalid OTP format", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun verifyReturn(serial: String, otp: String) {
        val token = SessionManager.getToken(this) ?: return
        lifecycleScope.launch {
            try {
                // Step 3: Send OTP to backend to finalize
                val req = ReturnVerifyRequest(device_id = serial, otp_code = otp)
                val response = RetrofitClient.api.verifyReturn(token, req)

                if (response.isSuccessful) {
                    Toast.makeText(this@UserDashboardActivity, "Success! Device Returned.", Toast.LENGTH_LONG).show()
                    loadData() // Refresh list to remove the device
                } else {
                    Toast.makeText(this@UserDashboardActivity, "Wrong OTP. Try again.", Toast.LENGTH_LONG).show()
                }
            } catch (e: Exception) {
                Toast.makeText(this@UserDashboardActivity, "Network Error", Toast.LENGTH_SHORT).show()
            }
        }
    }
}