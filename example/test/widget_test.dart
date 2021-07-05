import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';

import 'package:ble_scanner_example/main.dart';

void main() {
  testWidgets('Verify Bluetooth available', (WidgetTester tester) async {
    await tester.pumpWidget(MyApp());

    expect(
      find.byWidgetPredicate(
        (Widget widget) => widget is Text &&
                           widget.data!.startsWith('Bluetooth available: false'),
      ),
      findsOneWidget,
    );
  });
}
