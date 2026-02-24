package com.example.tabaudit

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.tabaudit.api.DevicePossession

class DevicesAdapter(
    private var devices: List<DevicePossession>,
    private val onReturnClick: (DevicePossession) -> Unit
) : RecyclerView.Adapter<DevicesAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvName: TextView = view.findViewById(R.id.tvDeviceName)
        val tvSerial: TextView = view.findViewById(R.id.tvSerial)
        val btnReturn: Button = view.findViewById(R.id.btnReturn)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_device, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val device = devices[position]
        holder.tvName.text = device.device__tab_type__name
        holder.tvSerial.text = "S/N: ${device.device__serial_number}"

        holder.btnReturn.setOnClickListener { onReturnClick(device) }
    }

    override fun getItemCount() = devices.size

    fun updateList(newList: List<DevicePossession>) {
        devices = newList
        notifyDataSetChanged()
    }
}