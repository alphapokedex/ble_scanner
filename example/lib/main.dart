import 'package:ble_scanner/ble_scanner.dart';
import 'package:flutter/material.dart';

void main() {
  runApp(MyApp());
}

class MyApp extends StatefulWidget {
  @override
  _MyAppState createState() => _MyAppState();
}

class _MyAppState extends State<MyApp> {
  var isStreamed = false;

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      home: Scaffold(
        appBar: AppBar(
          title: const Text('Ble Plugin example app'),
        ),
        body: isStreamed
            ? Column(
                children: [
                  StreamBuilder(
                    stream: BleScanner.eventStream,
                    builder: (BuildContext context, AsyncSnapshot snapshot) {
                      Widget widget = Container();
                      if (snapshot.connectionState == ConnectionState.none) {
                        widget = Text('NONE');
                      } else if (snapshot.connectionState ==
                          ConnectionState.waiting) {
                        widget = Text('WAITING');
                      } else if (snapshot.connectionState ==
                              ConnectionState.active &&
                          snapshot.hasData) {
                        widget = Text(snapshot.data);
                      } else if (snapshot.connectionState ==
                          ConnectionState.done) {
                        widget = Text('DONE');
                      } else
                        widget = Text('ELSE BLOCK');
                      return Center(child: widget);
                    },
                  ),
                  TextButton(
                    child: Text('Stop Scanning'),
                    onPressed: () async {
                      await BleScanner.stopBleScan();
                      setState(() {
                        isStreamed = false;
                      });
                    },
                  ),
                ],
              )
            : Center(
                child: Column(
                  children: [
                    TextButton(
                      child: Text('Enable Bluetooth'),
                      onPressed: () async {
                        await BleScanner.enableBt();
                      },
                    ),
                    TextButton(
                      child: Text('Req location permission'),
                      onPressed: () async {
                        await BleScanner.requestLocationPermission();
                      },
                    ),
                    TextButton(
                      child: Text('Start Scanning'),
                      onPressed: () async {
                        await BleScanner.startBleScan();
                        setState(() {
                          isStreamed = true;
                        });
                      },
                    ),
                  ],
                ),
              ),
      ),
    );
  }
}
