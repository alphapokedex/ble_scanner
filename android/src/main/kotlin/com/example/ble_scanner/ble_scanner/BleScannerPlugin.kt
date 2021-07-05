package com.example.ble_scanner.ble_scanner

import android.Manifest
import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.bluetooth.le.*
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.ParcelUuid
import androidx.core.app.ActivityCompat.requestPermissions
import androidx.core.content.ContextCompat
import io.flutter.Log
import io.flutter.embedding.engine.plugins.FlutterPlugin
import io.flutter.embedding.engine.plugins.activity.ActivityAware
import io.flutter.embedding.engine.plugins.activity.ActivityPluginBinding
import io.flutter.plugin.common.EventChannel
import io.flutter.plugin.common.EventChannel.EventSink
import io.flutter.plugin.common.EventChannel.StreamHandler
import io.flutter.plugin.common.MethodCall
import io.flutter.plugin.common.MethodChannel
import io.flutter.plugin.common.MethodChannel.MethodCallHandler
import io.flutter.plugin.common.MethodChannel.Result


/** BleScannerPlugin */
class BleScannerPlugin : FlutterPlugin, MethodCallHandler, StreamHandler, ActivityAware {

    private lateinit var methodChannel: MethodChannel
    private lateinit var eventChannel: EventChannel
    private lateinit var eventSink: EventSink
    private lateinit var activity: Activity

    private val bleMethods = BleRelatedMethods
    private val bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
    private val bleScanner = bluetoothAdapter.bluetoothLeScanner

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
        when (call.method) {
            bleMethods.enableBtTAG -> {
                bleMethods.enableBt(activity, result)
            }
            bleMethods.requestLocationPermissionTAG -> {
                bleMethods.requestLocationPermission(activity, result)
            }
            bleMethods.startBleScanTAG -> {
                bleMethods.startBleScan(bleScanner, activity, result)
            }
            bleMethods.isScanningTAG -> {
                bleMethods.isScanning(result)
            }
            bleMethods.stopBleTAG -> {
                bleMethods.stopBleScan(bleScanner, result)
            }
            else -> {
                result.notImplemented()
            }
        }
    }
    /** MethodCallHandler Close */


    /** StreamHandler Open */
    override fun onListen(arguments: Any?, events: EventSink) {
        eventSink = events
    }

    override fun onCancel(arguments: Any?) {}
    /** StreamHandler Close */

    /** ActivityAware Open */
    override fun onAttachedToActivity(binding: ActivityPluginBinding) {
        activity = binding.activity
    }

    override fun onDetachedFromActivityForConfigChanges() {}

    override fun onReattachedToActivityForConfigChanges(binding: ActivityPluginBinding) {
        activity = binding.activity
    }

    override fun onDetachedFromActivity() {}

    /** ActivityAware Close */

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
        const val stopBleTAG = "stopBleScan"

        private const val ENVIRONMENTAL_SERVICE_UUID = 0x181A

        private val scanResults = mutableListOf<ScanResult>()
        private val mScanResultInnerMap = mutableMapOf<String, String>()
        private val mScanResultDataMap = mutableMapOf<String, Map<String, String>>()

        private var isScanning = false

        private var isLocationPermissionGranted: Boolean? = null


        fun enableBt(activity: Activity, result: Result) {
            Utils.methodLog(enableBtTAG)
            val enableBT = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
            activity.startActivity(enableBT)
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
                    mScanResultInnerMap["DeviceName"] = bleDevice.name.toString()
                    mScanResultInnerMap["DeviceRSSI"] = result.rssi.toString()
                    mScanResultDataMap[bleDevice.address] = mScanResultInnerMap
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
                requestPermissions(
                    activity,
                    arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                    2
                )
                result.success("Success")
            }
        }

        fun startBleScan(bleScanner: BluetoothLeScanner, activity: Activity, result: Result) {
            Utils.methodLog("startBleScan")
            isLocationPermissionGranted = hasPermissions(activity.applicationContext)
            val scanSettings = ScanSettings.Builder().build()
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !isLocationPermissionGranted!!) {
                requestLocationPermission(activity, result)
            } else {
                scanResults.clear()
                mScanResultDataMap.clear()
                mScanResultInnerMap.clear()
                bleScanner.startScan(null, scanSettings, scanCallback)
                isScanning = true
                result.success("Success")
            }
        }

        fun isScanning(result: Result) {
            Utils.methodLog("isScanning")
            result.success(isScanning)
        }

        fun stopBleScan(bleScanner: BluetoothLeScanner, result: Result) {
            Utils.methodLog("stopBleScan")
            bleScanner.stopScan(scanCallback)
            isScanning = false
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