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
import com.example.tabaudit.api.*
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton
import com.google.mlkit.vision.codescanner.GmsBarcodeScanning
import kotlinx.coroutines.launch

class UserDashboardActivity : AppCompatActivity() {

    private lateinit var adapter: DevicesAdapter
    private lateinit var swipeRefresh: SwipeRefreshLayout

    // Store current devices so the user can select which one to send
    private var myDevicesList: List<DevicePossession> = emptyList()

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

        // 6. Setup Transfer Buttons
        findViewById<Button>(R.id.btnSendTab).setOnClickListener {
            showSendDialog()
        }

        findViewById<Button>(R.id.btnReceiveTab).setOnClickListener {
            showReceiveDialog()
        }

        // Load initial data
        loadData()
    }

    private fun loadData() {
        // SessionManager already includes "Bearer " so we just use it directly!
        val token = SessionManager.getToken(this) ?: return
        lifecycleScope.launch {
            try {
                val response = RetrofitClient.getApi(this@UserDashboardActivity).getMyDevices(token)
                if (response.isSuccessful) {
                    myDevicesList = response.body() ?: emptyList()
                    adapter.updateList(myDevicesList)
                }
            } catch (e: Exception) {
                Toast.makeText(this@UserDashboardActivity, "Error loading list", Toast.LENGTH_SHORT).show()
            } finally {
                swipeRefresh.isRefreshing = false
            }
        }
    }

    // --- TRANSFER SEND LOGIC ---
    private fun showSendDialog() {
        if (myDevicesList.isEmpty()) {
            Toast.makeText(this, "You have no tabs to send.", Toast.LENGTH_SHORT).show()
            return
        }

        // Create a list of readable names for the dialog
        val options = myDevicesList.map { "${it.device__tab_type__name} (${it.device__serial_number})" }.toTypedArray()
        var selectedIndex = 0

        AlertDialog.Builder(this)
            .setTitle("Select Tab to Send")
            .setSingleChoiceItems(options, 0) { _, which ->
                selectedIndex = which
            }
            .setPositiveButton("Generate OTP") { _, _ ->
                val selectedDevice = myDevicesList[selectedIndex].device__serial_number
                initiateTransfer(selectedDevice)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun initiateTransfer(serial: String) {
        val token = SessionManager.getToken(this) ?: return
        lifecycleScope.launch {
            try {
                val response = RetrofitClient.getApi(this@UserDashboardActivity)
                    .initiateTransfer(token, TransferInitRequest(serial))

                if (response.isSuccessful) {
                    val otp = response.body()?.transfer_otp
                    AlertDialog.Builder(this@UserDashboardActivity)
                        .setTitle("Transfer Code Generated")
                        .setMessage("Your 6-digit code is:\n\n$otp\n\nShow this to the receiving user. It is valid for 10 minutes.")
                        .setPositiveButton("Done", null)
                        .setCancelable(false)
                        .show()
                } else {
                    Toast.makeText(this@UserDashboardActivity, "Failed to initiate transfer", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(this@UserDashboardActivity, "Network Error", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // --- TRANSFER RECEIVE LOGIC ---
    private fun showReceiveDialog() {
        val input = EditText(this)
        input.hint = "Enter 6-digit Transfer OTP"
        input.inputType = android.text.InputType.TYPE_CLASS_NUMBER

        val container = LinearLayout(this)
        container.orientation = LinearLayout.VERTICAL
        container.setPadding(50, 40, 50, 10)
        container.addView(input)

        AlertDialog.Builder(this)
            .setTitle("Receive a Tab")
            .setMessage("Enter the OTP provided by the sender.")
            .setView(container)
            .setPositiveButton("Claim") { _, _ ->
                val otp = input.text.toString()
                if (otp.length == 6) {
                    acceptTransfer(otp)
                } else {
                    Toast.makeText(this, "Invalid OTP format", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun acceptTransfer(otp: String) {
        val token = SessionManager.getToken(this) ?: return
        lifecycleScope.launch {
            try {
                val response = RetrofitClient.getApi(this@UserDashboardActivity)
                    .acceptTransfer(token, TransferAcceptRequest(otp))

                if (response.isSuccessful) {
                    try { SoundManager.play(this@UserDashboardActivity, R.raw.melody_assign) } catch (e: Exception) {}
                    Toast.makeText(this@UserDashboardActivity, "Tab successfully claimed!", Toast.LENGTH_LONG).show()
                    loadData() // Refresh list to show the new device
                } else {
                    Toast.makeText(this@UserDashboardActivity, "Invalid or expired OTP", Toast.LENGTH_LONG).show()
                }
            } catch (e: Exception) {
                Toast.makeText(this@UserDashboardActivity, "Network Error", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // --- EXISTING ASSIGN/RETURN LOGIC BELOW ---
    private fun assignTablet(serial: String) {
        val token = SessionManager.getToken(this) ?: return
        lifecycleScope.launch {
            try {
                val response = RetrofitClient.getApi(this@UserDashboardActivity).assignTablet(token, AssignRequest(device_id = serial))
                if (response.isSuccessful) {
                    try { SoundManager.play(this@UserDashboardActivity, R.raw.melody_assign) } catch (e: Exception) {}
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
                val response = RetrofitClient.getApi(this@UserDashboardActivity).initiateReturn(token, ReturnInitRequest(serial))
                if (response.isSuccessful) {
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
                val req = ReturnVerifyRequest(device_id = serial, otp_code = otp)
                val response = RetrofitClient.getApi(this@UserDashboardActivity).verifyReturn(token, req)

                if (response.isSuccessful) {
                    try { SoundManager.play(this@UserDashboardActivity, R.raw.melody_return) } catch (e: Exception) {}
                    Toast.makeText(this@UserDashboardActivity, "Success! Device Returned.", Toast.LENGTH_LONG).show()
                    loadData()
                } else {
                    Toast.makeText(this@UserDashboardActivity, "Wrong OTP. Try again.", Toast.LENGTH_LONG).show()
                }
            } catch (e: Exception) {
                Toast.makeText(this@UserDashboardActivity, "Network Error", Toast.LENGTH_SHORT).show()
            }
        }
    }
}