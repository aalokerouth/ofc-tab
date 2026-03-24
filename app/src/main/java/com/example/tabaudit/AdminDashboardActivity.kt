package com.example.tabaudit

import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.tabaudit.api.AdminStats
import com.example.tabaudit.api.RetrofitClient
import kotlinx.coroutines.launch

class AdminDashboardActivity : AppCompatActivity() {

    // UI Containers
    private lateinit var containerReturns: LinearLayout
    private lateinit var containerActiveLoans: LinearLayout
    private lateinit var containerStock: LinearLayout
    private lateinit var containerLogs: LinearLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_admin_dashboard)

        // Initialize Views
        containerReturns = findViewById(R.id.containerReturns)
        containerActiveLoans = findViewById(R.id.containerActiveLoans)
        containerStock = findViewById(R.id.containerStock)
        containerLogs = findViewById(R.id.containerLogs)

        loadStats()
    }

    private fun loadStats() {
        val token = SessionManager.getToken(this) ?: return
        lifecycleScope.launch {
            try {
                // --- FIX 1: Use getApi(this) ---
                val response =
                    RetrofitClient.getApi(this@AdminDashboardActivity).getDashboardStats(token)

                if (response.isSuccessful && response.body() != null) {
                    val data = response.body()!!
                    populateUI(data)
                } else {
                    Toast.makeText(
                        this@AdminDashboardActivity,
                        "Failed to load stats",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            } catch (e: Exception) {
                Toast.makeText(
                    this@AdminDashboardActivity,
                    "Error: ${e.message}",
                    Toast.LENGTH_SHORT
                ).show()
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
                val text = "OTP: ${item.otp_code}\nUser: ${item.username}\nDevice: ${item.serial}"
                addTextToContainer(containerReturns, text, isBold = true, color = Color.RED)

                val divider = TextView(this)
                divider.layoutParams =
                    LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 2)
                divider.setBackgroundColor(Color.LTGRAY)
                containerReturns.addView(divider)
            }
        }

        // 2. Populate ACTIVE LOANS
        containerActiveLoans.removeAllViews()
        if (data.active_loans.isEmpty()) {
            addTextToContainer(containerActiveLoans, "No active loans", isBold = false)
        } else {
            for (loan in data.active_loans) {
                val text =
                    "${loan.username} (${loan.employee_id})\n${loan.tab_name}\nS/N: ${loan.serial}"
                addTextToContainer(
                    containerActiveLoans,
                    text,
                    isBold = false,
                    color = Color.parseColor("#0D47A1")
                )

                val divider = TextView(this)
                divider.layoutParams =
                    LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 1)
                divider.setBackgroundColor(Color.LTGRAY)
                containerActiveLoans.addView(divider)
            }
        }

        // 3. Populate STOCK
        containerStock.removeAllViews()
        for (item in data.stock) {
            val text = "${item.name}: ${item.stock_remaining} available"
            addTextToContainer(containerStock, text, isBold = true)
        }

        // 4. Populate LOGS (Updated for Transfer Audit)
        containerLogs.removeAllViews()
        for (log in data.recent_activity) {
            // Map the backend status string to a readable label
            val actionDisplay = when (log.action) {
                "active" -> "Checked Out"
                "returned" -> "Returned"
                "transferred_out" -> "Transferred Out"
                "transferred" -> "Received Transfer"
                else -> log.action ?: "Action"
            }

            // Build the main line: "Username Action TabName"
            var displayText = "${log.username} $actionDisplay ${log.tab_name}"

            // Add the audit note on a new line if it exists (e.g., "Transferred to User B")
            if (!log.notes.isNullOrBlank()) {
                displayText += "\n(${log.notes})"
            }

            addTextToContainer(containerLogs, displayText, isBold = false)

            // Add a divider for readability
            val divider = TextView(this)
            val params = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 1)
            params.setMargins(0, 8, 0, 8)
            divider.layoutParams = params
            divider.setBackgroundColor(Color.LTGRAY)
            containerLogs.addView(divider)
        }
    }

    // Ensure this helper remains at the bottom of your Activity class
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