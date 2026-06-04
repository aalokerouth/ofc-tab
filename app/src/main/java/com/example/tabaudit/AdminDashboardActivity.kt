package com.example.tabaudit

import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.tabaudit.api.AdminStats
import com.example.tabaudit.api.ForceReturnRequest
import com.example.tabaudit.api.RetrofitClient
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class AdminDashboardActivity : AppCompatActivity() {

    // UI Containers
    private lateinit var containerReturns: LinearLayout
    private lateinit var containerActiveLoans: LinearLayout
    private lateinit var containerStock: LinearLayout
    private lateinit var containerLogs: LinearLayout

    // Coroutine Job for the auto-refresh loop
    private var refreshJob: Job? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_admin_dashboard)

        // Initialize Views
        containerReturns = findViewById(R.id.containerReturns)
        containerActiveLoans = findViewById(R.id.containerActiveLoans)
        containerStock = findViewById(R.id.containerStock)
        containerLogs = findViewById(R.id.containerLogs)

        // Initial manual load
        loadStats()
    }

    override fun onResume() {
        super.onResume()
        startAutoRefresh()
    }

    override fun onPause() {
        super.onPause()
        stopAutoRefresh()
    }

    private fun startAutoRefresh() {
        // Cancel any existing job just in case
        refreshJob?.cancel()

        refreshJob = lifecycleScope.launch {
            while (isActive) {
                delay(3000) // Wait 3 seconds
                loadStats(isQuiet = true) // Pass true to avoid spamming Toast errors on every 3s tick
            }
        }
    }

    private fun stopAutoRefresh() {
        refreshJob?.cancel()
        refreshJob = null
    }

    private fun loadStats(isQuiet: Boolean = false) {
        val token = SessionManager.getToken(this) ?: return
        lifecycleScope.launch {
            try {
                val response = RetrofitClient.getApi(this@AdminDashboardActivity).getDashboardStats(token)

                if (response.isSuccessful && response.body() != null) {
                    val data = response.body()!!
                    populateUI(data)
                } else if (!isQuiet) {
                    Toast.makeText(
                        this@AdminDashboardActivity,
                        "Failed to load stats",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            } catch (e: Exception) {
                if (!isQuiet) {
                    Toast.makeText(
                        this@AdminDashboardActivity,
                        "Error: ${e.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }

    private fun populateUI(data: AdminStats) {
        // 1. Populate PENDING RETURNS
        containerReturns.removeAllViews()
        if (data.pending_returns.isEmpty()) {
            addTextToContainer(containerReturns, "No pending returns", isBold = false)
        } else {
            for (item in data.pending_returns) {
                val warningColor = androidx.core.content.ContextCompat.getColor(this, R.color.brand_secondary)
                val text = "OTP: ${item.otp_code}\nUser: ${item.username}\nDevice: ${item.serial}"
                addTextToContainer(containerReturns, text, isBold = true, color = warningColor)

                val divider = TextView(this)
                divider.layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 2)
                divider.setBackgroundColor(Color.LTGRAY)
                containerReturns.addView(divider)
            }
        }

        // 2. Populate ACTIVE LOANS (With Force Return Click Listener)
        containerActiveLoans.removeAllViews()
        if (data.active_loans.isEmpty()) {
            addTextToContainer(containerActiveLoans, "No active loans", isBold = false)
        } else {
            for (loan in data.active_loans) {
                val text = "${loan.username} (${loan.employee_id})\n${loan.tab_name}\nS/N: ${loan.serial}\n[Tap to Force Return]"

                val tv = TextView(this).apply {
                    this.text = text
                    setTextColor(Color.parseColor("#0D47A1"))
                    textSize = 15f
                    setPadding(0, 16, 0, 16)

                    // Click listener to trigger Force Return
                    setOnClickListener {
                        showForceReturnDialog(loan.serial, loan.username)
                    }
                }
                containerActiveLoans.addView(tv)

                val divider = TextView(this).apply {
                    layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 1)
                    setBackgroundColor(Color.LTGRAY)
                }
                containerActiveLoans.addView(divider)
            }
        }

        // 3. Populate STOCK
        containerStock.removeAllViews()
        for (item in data.stock) {
            val text = "${item.name}: ${item.stock_remaining} available"
            addTextToContainer(containerStock, text, isBold = true)
        }

        // 4. Populate LOGS
        containerLogs.removeAllViews()
        for (log in data.recent_activity) {
            val actionDisplay = when (log.action) {
                "active" -> "Checked Out"
                "returned" -> "Returned"
                "transferred_out" -> "Transferred Out"
                "transferred" -> "Received Transfer"
                else -> log.action ?: "Action"
            }

            var displayText = "${log.username} $actionDisplay ${log.tab_name}"
            if (!log.notes.isNullOrBlank()) {
                displayText += "\n(${log.notes})"
            }

            addTextToContainer(containerLogs, displayText, isBold = false)

            val divider = TextView(this)
            val params = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 1)
            params.setMargins(0, 8, 0, 8)
            divider.layoutParams = params
            divider.setBackgroundColor(Color.LTGRAY)
            containerLogs.addView(divider)
        }
    }

    // --- FORCE RETURN LOGIC ---
    private fun showForceReturnDialog(serial: String, username: String?) {
        AlertDialog.Builder(this)
            .setTitle("Force Return Tablet")
            .setMessage("Are you sure you want to force return tablet S/N: $serial from $username?\n\nThis will bypass OTP verification.")
            .setPositiveButton("Force Return") { _, _ -> forceReturnDevice(serial) }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun forceReturnDevice(serial: String) {
        val token = SessionManager.getToken(this) ?: return
        lifecycleScope.launch {
            try {
                val request = ForceReturnRequest(serial)
                val response = RetrofitClient.getApi(this@AdminDashboardActivity).adminForceReturn(token, request)

                if (response.isSuccessful) {
                    Toast.makeText(this@AdminDashboardActivity, "Device Force Returned!", Toast.LENGTH_SHORT).show()
                    loadStats(isQuiet = false) // Refresh immediately
                } else {
                    Toast.makeText(this@AdminDashboardActivity, "Error: ${response.errorBody()?.string()}", Toast.LENGTH_LONG).show()
                }
            } catch (e: Exception) {
                Toast.makeText(this@AdminDashboardActivity, "Network Error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // --- HELPER FUNCTION ---
    private fun addTextToContainer(
        container: LinearLayout,
        content: String,
        isBold: Boolean,
        color: Int = Color.BLACK
    ) {
        val tv = TextView(this)
        tv.text = content
        tv.setTextColor(color)
        tv.textSize = 15f
        tv.setPadding(0, 16, 0, 16)
        if (isBold) tv.setTypeface(null, Typeface.BOLD)
        container.addView(tv)
    }
}