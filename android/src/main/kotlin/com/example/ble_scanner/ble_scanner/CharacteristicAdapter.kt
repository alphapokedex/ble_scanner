package com.example.ble_scanner.ble_scanner

import android.bluetooth.BluetoothGattCharacteristic
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.ble_scanner.ble_scanner.ble.printProperties
import org.jetbrains.anko.layoutInflater

class CharacteristicAdapter(
    private val items: List<BluetoothGattCharacteristic>,
    private val onClickListener: ((characteristic: BluetoothGattCharacteristic) -> Unit)
) : RecyclerView.Adapter<CharacteristicAdapter.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = parent.context.layoutInflater.inflate(
            R.layout.row_characteristic,
            parent,
            false
        )
        return ViewHolder(view, onClickListener)
    }

    override fun getItemCount() = items.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]
        holder.bind(item)
    }

    class ViewHolder(
        private val view: View,
        private val onClickListener: ((characteristic: BluetoothGattCharacteristic) -> Unit)
    ) : RecyclerView.ViewHolder(view) {

        fun bind(characteristic: BluetoothGattCharacteristic) {
            with(view) {
                findViewById<TextView>(R.id.characteristic_uuid).text = characteristic.uuid.toString()
                findViewById<TextView>(R.id.characteristic_properties).text = characteristic.printProperties()
                setOnClickListener { onClickListener.invoke(characteristic) }
            }
        }
    }
}
