package com.example.ble_scanner.ble_scanner

import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.le.*
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.os.Build
import android.os.Bundle
//import android.os.ParcelUuid
import androidx.core.app.ActivityCompat.requestPermissions
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleEventObserver
import com.example.ble_scanner.ble_scanner.ble.ConnectionEventListener
import com.example.ble_scanner.ble_scanner.ble.ConnectionManager
import io.flutter.Log
import io.flutter.app.FlutterActivityEvents
import io.flutter.embedding.engine.plugins.FlutterPlugin
import io.flutter.embedding.engine.plugins.activity.ActivityAware
import io.flutter.embedding.engine.plugins.activity.ActivityPluginBinding
import io.flutter.embedding.engine.plugins.lifecycle.HiddenLifecycleReference
import io.flutter.plugin.common.EventChannel
import io.flutter.plugin.common.EventChannel.EventSink
import io.flutter.plugin.common.EventChannel.StreamHandler
import io.flutter.plugin.common.MethodCall
import io.flutter.plugin.common.MethodChannel
import io.flutter.plugin.common.MethodChannel.MethodCallHandler
import io.flutter.plugin.common.MethodChannel.Result


/** BleScannerPlugin */
class BleScannerPlugin : FlutterPlugin, MethodCallHandler, StreamHandler, ActivityAware,
    FlutterActivityEvents {

    private lateinit var methodChannel: MethodChannel
    private lateinit var eventChannel: EventChannel
    private lateinit var activity: Activity
    private var bleScanner: BluetoothLeScanner? = null

    private val bleMethods = BleRelatedMethods
    private val bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()

    private val connectionEventListener by lazy {
        ConnectionEventListener().apply {
            onConnectionSetupComplete = { gatt ->
                Intent(activity.applicationContext, BleOperationsActivity::class.java).also {
                    it.putExtra(BluetoothDevice.EXTRA_DEVICE, gatt.device)
                    activity.startActivity(it)
                }
                ConnectionManager.unregisterListener(this)
            }
            onDisconnect = {
                activity.runOnUiThread {
                    val alert = AlertDialog.Builder(activity.applicationContext)
                    alert.setTitle("Disconnected")
                    alert.setMessage("Disconnected or unable to connect to device.")
                    alert.setPositiveButton("OK") { _, _ -> }
                    alert.show()
                }
            }
        }
    }

    /**********************
     * FlutterPlugin Open *
     **********************/
    override fun onAttachedToEngine(flutterPluginBinding: FlutterPlugin.FlutterPluginBinding) {
        val binaryMessenger = flutterPluginBinding.binaryMessenger
        methodChannel = MethodChannel(binaryMessenger, "ble_scanner_method")
        methodChannel.setMethodCallHandler(this)
        eventChannel = EventChannel(binaryMessenger, "ble_scanner_event")
        eventChannel.setStreamHandler(this)
    }

    /** MethodCallHandler Open */
    override fun onMethodCall(call: MethodCall, result: Result) {
        with(bleMethods) {
            when (call.method) {
                enableBtTAG -> {
                    enableBt(activity, result)
                }
                requestLocationPermissionTAG -> {
                    requestLocationPermission(activity, result)
                }
                startBleScanTAG -> {
                    bleScanner = bluetoothAdapter.bluetoothLeScanner
                    startBleScan(bleScanner, activity, result)
                }
                isScanningTAG -> {
                    isScanning(result)
                }
                attemptConnectionTAG -> {
                    val index = call.argument<Int>("ScannedDeviceIndex")!!
                    attemptConnection(index, activity, result)
                }
                stopBleTAG -> {
                    stopBleScan(bleScanner, activity, result) {
                        bleScanner = null
                        Log.w(stopBleTAG, "BLE scanner dismissed")
                    }
                }
                else -> {
                    result.notImplemented()
                }
            }
        }
    }
    /** MethodCallHandler Close */


    /** StreamHandler Open */
    override fun onListen(arguments: Any?, events: EventSink) {
        bleMethods.eventSink = events
    }

    override fun onCancel(arguments: Any?) {}
    /** StreamHandler Close */

    /** ActivityAware Open */
    override fun onAttachedToActivity(binding: ActivityPluginBinding) {
        activity = binding.activity
        (binding.lifecycle as HiddenLifecycleReference)
            .lifecycle
            .addObserver(LifecycleEventObserver { _, event ->
                Log.e("Activity state: ", "$event onAttachedToActivity")
                if (event.toString() == "ON_RESUME") {
                    ConnectionManager.registerListener(connectionEventListener)
                }
            })
    }

    override fun onDetachedFromActivityForConfigChanges() {
        Log.e("Activity aware: ", "onDetachedFromActivityForConfigChanges")
    }

    override fun onReattachedToActivityForConfigChanges(binding: ActivityPluginBinding) {
        activity = binding.activity
        Log.e("Activity aware: ", "onReattachedToActivityForConfigChanges")
        (binding.lifecycle as HiddenLifecycleReference)
            .lifecycle
            .addObserver(LifecycleEventObserver { _, event ->
                Log.e(
                    "Activity state: ",
                    "$event onReattachedToActivityForConfigChanges"
                )
                if (event.toString() == "ON_RESUME") {
                    ConnectionManager.registerListener(connectionEventListener)
                }
            })
    }

    override fun onDetachedFromActivity() {
        Log.e("Activity aware: ", "onDetachedFromActivity")
        ConnectionManager.unregisterListener(connectionEventListener)
        //ConnectionManager.teardownConnection(device)
    }
    /** ActivityAware Close */

    /** FlutterActivityEvents Open */
    override fun onConfigurationChanged(newConfig: Configuration) {}

    override fun onLowMemory() {}

    override fun onTrimMemory(level: Int) {}

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?): Boolean {
        return true
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>?,
        grantResults: IntArray?
    ): Boolean {
        return true
    }

    override fun onCreate(savedInstanceState: Bundle?) {}

    override fun onNewIntent(intent: Intent?) {}

    override fun onPause() {}

    override fun onStart() {}

    override fun onResume() {
        Log.w("onResume", "[ onResume called ]")
        ConnectionManager.registerListener(connectionEventListener)
    }

    override fun onPostResume() {}

    override fun onDestroy() {}

    override fun onStop() {}

    override fun onBackPressed(): Boolean {
        return true
    }

    override fun onUserLeaveHint() {}

    /** FlutterActivityEvents Close */

    override fun onDetachedFromEngine(binding: FlutterPlugin.FlutterPluginBinding) {
        methodChannel.setMethodCallHandler(null)
        eventChannel.setStreamHandler(null)
    }
    /***********************
     * FlutterPlugin Close *
     ***********************/
}

class BleRelatedMethods {
    companion object {
        const val enableBtTAG = "enableBt"
        const val requestLocationPermissionTAG = "requestLocationPermission"
        const val startBleScanTAG = "startBleScan"
        const val isScanningTAG = "isScanning"
        const val attemptConnectionTAG = "attemptConnection"
        const val stopBleTAG = "stopBleScan"

        //private const val ENVIRONMENTAL_SERVICE_UUID = 0x181A

        private val scanResults = mutableListOf<ScanResult>()
        private val mScanResultInnerMap = mutableMapOf<String, String>()
        private val mScanResultDataMap = mutableMapOf<String, Map<String, String>>()

        private var isScanning = false

        private var isLocationPermissionGranted: Boolean? = null
        var eventSink: EventSink? = null

        fun enableBt(activity: Activity, result: Result) {
            Utils.methodLog(enableBtTAG)
            val enableBT = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
            activity.startActivityForResult(enableBT, 1)
            result.success(enableBtTAG)
        }

        private val scanCallback = object : ScanCallback() {
            override fun onScanResult(callbackType: Int, result: ScanResult) {
                val indexQuery =
                    scanResults.indexOfFirst { it.device.address == result.device.address }
                if (indexQuery != -1) { // A scan result already exists with the same address
                    scanResults[indexQuery] = result
                } else {
                    with(result.device) {
                        Log.w(
                            "scanCallBack",
                            """
                        ----------
                        Found BLE device!
                        Name: ${name ?: "Unnamed"}
                        address: $address
                        ----------
                        """
                        )
                    }
                    scanResults.add(result)
                    mScanResultDataMap.clear()
                    val bleDevice = result.device
                    mScanResultInnerMap["DeviceAddress"] = bleDevice.address.toString()
                    mScanResultInnerMap["DeviceName"] = bleDevice.name ?: "Unnamed"
                    mScanResultInnerMap["DeviceRSSI"] = result.rssi.toString()
                    mScanResultDataMap[bleDevice.address] = mScanResultInnerMap
                    eventSink?.success(mScanResultDataMap)
                }
            }

            override fun onScanFailed(errorCode: Int) {
                Log.e("", "onScanFailed: code $errorCode")
                mScanResultInnerMap.clear()
                mScanResultDataMap.clear()
            }
        }

        private fun hasPermissions(context: Context): Boolean {
            return ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        }

        fun requestLocationPermission(activity: Activity, result: Result) {
            Utils.methodLog(requestLocationPermissionTAG)
            isLocationPermissionGranted = hasPermissions(activity.applicationContext)
            if (isLocationPermissionGranted == true) {
                result.success("Success")
                return
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                activity.runOnUiThread {
                    val alert = AlertDialog.Builder(activity.applicationContext)
                    alert.setTitle("Required")
                    alert.setMessage("Location permission is needed to scan & get information from BLE devices.")
                    alert.setPositiveButton("OK") { _, _ -> }
                    alert.show()
                }
                requestPermissions(
                    activity,
                    arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                    2
                )
                result.success("Success")
            }
        }

        fun startBleScan(bleScanner: BluetoothLeScanner?, activity: Activity, result: Result) {
            Utils.methodLog(startBleScanTAG)
            isLocationPermissionGranted = hasPermissions(activity.applicationContext)
            val scanSettings = ScanSettings.Builder().build()
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !isLocationPermissionGranted!!) {
                requestLocationPermission(activity, result)
            }
            if (bleScanner == null) {
                activity.runOnUiThread {
                    val alert = AlertDialog.Builder(activity.applicationContext)
                    alert.setTitle("Adaptor not found")
                    alert.setMessage("Please turn on Bluetooth before starting scan")
                    alert.setPositiveButton("OK") { _, _ -> }
                    alert.show()
                }
            }
            if (bleScanner != null) {
                scanResults.clear()
                mScanResultDataMap.clear()
                mScanResultInnerMap.clear()
                bleScanner.startScan(null, scanSettings, scanCallback)
                isScanning = true
                result.success("Success")
            }
        }

        fun isScanning(result: Result) {
            Utils.methodLog(isScanningTAG)
            result.success(isScanning)
        }

        fun attemptConnection(index: Int, activity: Activity, result: Result) {
            val device = scanResults[index].device
            Utils.methodLog(attemptConnectionTAG)
            Log.w(
                attemptConnectionTAG,
                "Attempting connection with device ${device.name ?: "Unnamed"}|${device.address}"
            )
            ConnectionManager.connect(device, activity)
            result.success("Success")
        }

        fun stopBleScan(
            bleScanner: BluetoothLeScanner?,
            activity: Activity,
            result: Result,
            callBack: () -> Unit
        ) {
            Utils.methodLog("stopBleScan")
            if (bleScanner == null) {
                activity.runOnUiThread {
                    val alert = AlertDialog.Builder(activity.applicationContext)
                    alert.setTitle("Adaptor not found")
                    alert.setMessage("Please turn on Bluetooth before stopping scan")
                    alert.setPositiveButton("OK") { _, _ -> }
                    alert.show()
                }
            }
            if (bleScanner != null) {
                bleScanner.stopScan(scanCallback)
                scanResults.clear()
                isScanning = false
            }
            callBack.invoke()
            result.success("Success")
        }
    }
}

class Utils {
    companion object {
        private const val TAG = "BleRelatedMethods"

        fun methodLog(methodName: String) {
            Log.w(TAG, "[ $methodName called ]")
        }
    }
}