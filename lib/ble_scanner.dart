import 'dart:async';
import 'dart:developer';

import 'package:flutter/services.dart';

class BleScanner {
  static const MethodChannel _methodChannel =
      const MethodChannel('ble_scanner_method');

  static const EventChannel _eventChannel =
      const EventChannel('ble_scanner_event');

  static const String enableBtTAG = 'enableBt';
  static const String isScanningTAG = 'isScanning';
  static const String requestLocationPermissionTAG =
      'requestLocationPermission';
  static const String startBleScanTAG = 'startBleScan';
  static const String stopBleScanTAG = 'stopBleScan';

  static Stream<dynamic> get eventStream =>
      _eventChannel.receiveBroadcastStream();

  static Future<void> enableBt() async {
    try {
      await _methodChannel.invokeMethod(enableBtTAG);
    } on PlatformException {
      log(enableBtTAG);
    }
  }

  static Future<void> requestLocationPermission() async {
    try {
      await _methodChannel.invokeMethod(requestLocationPermissionTAG);
    } on PlatformException {
      log(requestLocationPermissionTAG);
    }
  }

  static Future<bool?> get isScanning async {
    final bool? isScanning =
        await _methodChannel.invokeMethod(isScanningTAG) ?? false;
    return isScanning;
  }

  static Future<void> startBleScan() async {
    try {
      await _methodChannel.invokeMethod(startBleScanTAG);
    } on PlatformException {
      log(startBleScanTAG);
    }
  }

  static Future<void> stopBleScan() async {
    try {
      await _methodChannel.invokeMethod(stopBleScanTAG);
    } on PlatformException {
      log(stopBleScanTAG);
    }
  }
}