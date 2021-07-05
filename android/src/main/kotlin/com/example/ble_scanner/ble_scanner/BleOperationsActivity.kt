package com.example.ble_scanner.ble_scanner

import android.annotation.SuppressLint
import android.app.Activity
import android.app.AlertDialog
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGattCharacteristic
import android.content.Context
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.EditText
import android.widget.ScrollView
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.SimpleItemAnimator
import com.example.ble_scanner.ble_scanner.ble.*
import io.flutter.embedding.android.FlutterActivity
import org.jetbrains.anko.alert
import org.jetbrains.anko.selector
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

class BleOperationsActivity : FlutterActivity() {

    private lateinit var device: BluetoothDevice
    private val dateFormatter = SimpleDateFormat("MMM d, HH:mm:ss", Locale.US)
    private val characteristics by lazy {
        ConnectionManager.servicesOnDevice(device)?.flatMap { service ->
            service.characteristics ?: listOf()
        } ?: listOf()
    }
    private val characteristicProperties by lazy {
        characteristics.map { characteristic ->
            characteristic to mutableListOf<CharacteristicProperty>().apply {
                if (characteristic.isNotifiable()) add(CharacteristicProperty.Notifiable)
                if (characteristic.isIndicatable()) add(CharacteristicProperty.Indicatable)
                if (characteristic.isReadable()) add(CharacteristicProperty.Readable)
                if (characteristic.isWritable()) add(CharacteristicProperty.Writable)
                if (characteristic.isWritableWithoutResponse()) {
                    add(CharacteristicProperty.WritableWithoutResponse)
                }
            }.toList()
        }.toMap()
    }
    private val characteristicAdapter: CharacteristicAdapter by lazy {
        CharacteristicAdapter(characteristics) { characteristic ->
            showCharacteristicOptions(characteristic)
        }
    }
    private var notifyingCharacteristics = mutableListOf<UUID>()

    override fun onCreate(savedInstanceState: Bundle?) {
        ConnectionManager.registerListener(connectionEventListener)
        super.onCreate(savedInstanceState)
        device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
            ?: error("Missing BluetoothDevice from MainActivity!")

        setContentView(R.layout.activity_ble_operations)
        activity.actionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            setDisplayShowTitleEnabled(true)
            title = getString(R.string.ble_playground)
        }
        setupRecyclerView()
        val requestMtuButton: Button = findViewById(R.id.request_mtu_button)
        val mtuField: EditText = findViewById(R.id.mtu_field)
        requestMtuButton.setOnClickListener {
            if (mtuField.text.isNotEmpty() && mtuField.text.isNotBlank()) {
                mtuField.text.toString().toIntOrNull().let { mtu ->
                    log("Requesting for MTU value of $mtu")
                    if (mtu != null) {
                        ConnectionManager.requestMtu(device, mtu)
                    }
                } ?: log("Invalid MTU value: ${mtuField.text}")
            } else {
                log("Please specify a numeric value for desired ATT MTU (23-517)")
            }
            hideKeyboard()
        }
    }

    override fun onDestroy() {
        ConnectionManager.unregisterListener(connectionEventListener)
        ConnectionManager.teardownConnection(device)
        super.onDestroy()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> {
                onBackPressed()
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }

    private fun setupRecyclerView() {
        findViewById<RecyclerView>(R.id.characteristics_recycler_view).apply {
            adapter = characteristicAdapter
            layoutManager = LinearLayoutManager(
                this@BleOperationsActivity,
                RecyclerView.VERTICAL,
                false
            )
            isNestedScrollingEnabled = false
        }

        val animator = findViewById<RecyclerView>(R.id.characteristics_recycler_view).itemAnimator
        if (animator is SimpleItemAnimator) {
            animator.supportsChangeAnimations = false
        }
    }

    @SuppressLint("SetTextI18n")
    private fun log(message: String) {
        val formattedMessage = String.format("%s: %s", dateFormatter.format(Date()), message)
        runOnUiThread {
            val logTextView: TextView = findViewById(R.id.log_text_view)
            val logScrollView:ScrollView = findViewById(R.id.log_scroll_view)
            val currentLogText = if (logTextView.text.isEmpty()) {
                "Beginning of log."
            } else {
                logTextView.text
            }
            logTextView.text = "$currentLogText\n$formattedMessage"
            logScrollView.post { logScrollView.fullScroll(View.FOCUS_DOWN) }
        }
    }

    private fun showCharacteristicOptions(characteristic: BluetoothGattCharacteristic) {
        characteristicProperties[characteristic]?.let { properties ->
            selector("Select an action to perform", properties.map { it.action }) { _, i ->
                when (properties[i]) {
                    CharacteristicProperty.Readable -> {
                        log("Reading from ${characteristic.uuid}")
                        ConnectionManager.readCharacteristic(device, characteristic)
                    }
                    CharacteristicProperty.Writable, CharacteristicProperty.WritableWithoutResponse -> {
                        showWritePayloadDialog(characteristic)
                    }
                    CharacteristicProperty.Notifiable, CharacteristicProperty.Indicatable -> {
                        if (notifyingCharacteristics.contains(characteristic.uuid)) {
                            log("Disabling notifications on ${characteristic.uuid}")
                            ConnectionManager.disableNotifications(device, characteristic)
                        } else {
                            log("Enabling notifications on ${characteristic.uuid}")
                            ConnectionManager.enableNotifications(device, characteristic)
                        }
                    }
                }
            }
        }
    }

    @SuppressLint("InflateParams")
    private fun showWritePayloadDialog(characteristic: BluetoothGattCharacteristic) {
        val hexField = layoutInflater.inflate(R.layout.edittext_hex_payload, null) as EditText
        val alert = AlertDialog.Builder(applicationContext)
        with(alert) {
            setCustomTitle(hexField)
            setCancelable(false)
            setPositiveButton("YES") { _, _ ->
                with(hexField.text.toString()) {
                    if (isNotBlank() && isNotEmpty()) {
                        val bytes = hexToBytes()
                        log("Writing to ${characteristic.uuid}: ${bytes.toHexString()}")
                        ConnectionManager.writeCharacteristic(device, characteristic, bytes)
                    } else {
                        log("Please enter a hex payload to write to ${characteristic.uuid}")
                    }
                }
            }
            setPositiveButton("NO") { _, _ -> }
            show()
        }
        hexField.showKeyboard()
    }

    private val connectionEventListener by lazy {
        ConnectionEventListener().apply {
            onDisconnect = {
                runOnUiThread {
                    alert {
                        title = "Disconnected"
                        message = "Disconnected from device."
                        positiveButton("OK") { onBackPressed() }
                    }.show()
                }
            }

            onCharacteristicRead = { _, characteristic ->
                log("Read from ${characteristic.uuid}: ${characteristic.value.toHexString()}")
            }

            onCharacteristicWrite = { _, characteristic ->
                log("Wrote to ${characteristic.uuid}")
            }

            onMtuChanged = { _, mtu ->
                log("MTU updated to $mtu")
            }

            onCharacteristicChanged = { _, characteristic ->
                log("Value changed on ${characteristic.uuid}: ${characteristic.value.toHexString()}")
            }

            onNotificationsEnabled = { _, characteristic ->
                log("Enabled notifications on ${characteristic.uuid}")
                notifyingCharacteristics.add(characteristic.uuid)
            }

            onNotificationsDisabled = { _, characteristic ->
                log("Disabled notifications on ${characteristic.uuid}")
                notifyingCharacteristics.remove(characteristic.uuid)
            }
        }
    }

    private enum class CharacteristicProperty {
        Readable,
        Writable,
        WritableWithoutResponse,
        Notifiable,
        Indicatable;

        val action
            get() = when (this) {
                Readable -> "Read"
                Writable -> "Write"
                WritableWithoutResponse -> "Write Without Response"
                Notifiable -> "Toggle Notifications"
                Indicatable -> "Toggle Indications"
            }
    }

    private fun Activity.hideKeyboard() {
        hideKeyboard(currentFocus ?: View(this))
    }

    private fun Context.hideKeyboard(view: View) {
        val inputMethodManager =
            getSystemService(Activity.INPUT_METHOD_SERVICE) as InputMethodManager
        inputMethodManager.hideSoftInputFromWindow(view.windowToken, 0)
    }

    private fun EditText.showKeyboard() {
        val inputMethodManager =
            getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        requestFocus()
        inputMethodManager.showSoftInput(this, InputMethodManager.SHOW_IMPLICIT)
    }

    private fun String.hexToBytes() =
        this.chunked(2).map { it.toUpperCase(Locale.US).toInt(16).toByte() }.toByteArray()
}
