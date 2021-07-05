import 'dart:async';

import 'package:flutter/services.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:ble_scanner/ble_scanner.dart';

void main() {
  const EventChannel channel = EventChannel('ble_scanner_event');
  late StreamSubscription streamSubscription;

  TestWidgetsFlutterBinding.ensureInitialized();

  setUp(() {
    streamSubscription = channel.receiveBroadcastStream().listen((event) {});
  });

  tearDown(() {
    streamSubscription.cancel();
  });

  test('Event stream test', () async {
    List elements = List.empty();
    BleScanner.eventStream.listen((event) {});
    expect(elements, []);
  });
}
