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
                        Map data = snapshot.data;
                        int mapLength = data.length;
                        List keys = data.keys.toList();
                        widget = SizedBox(
                          height: 300,
                          child: ListView.builder(
                            itemCount: mapLength,
                            itemBuilder: (BuildContext context, int index) {
                              DeviceModel device = DeviceModel.fromMap(data[keys[index]]);
                              return ListTile(
                                leading: Text(device.rssi),
                                title: Text(device.name),
                                subtitle: Text(device.address),
                                trailing: IconButton(
                                  icon: Icon(Icons.double_arrow),
                                  onPressed: ()async{
                                    await BleScanner.attemptConnection(index);
                                  },
                                ),
                              );
                            },
                          ),
                        );
                      } else if (snapshot.connectionState ==
                          ConnectionState.done) {
                        widget = Text('DONE');
                      } else
                        widget = Text('ELSE BLOCK');
                      return Center(child: widget);
                    },
                  ),
                  Spacer(),
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
                    SizedBox(height: 15),
                    Text('Please wait until the bluetooth turns on before starting scan'),
                    SizedBox(height: 15),
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

class DeviceModel {
  final String address;
  final String name;
  final String rssi;

  DeviceModel({
    required this.address,
    required this.name,
    required this.rssi,
  });

  factory DeviceModel.fromMap(Map deviceInfo) {
    return DeviceModel(
      address: deviceInfo['DeviceAddress'] ?? '00:A0:00:00:0A:AA',
      name: deviceInfo['DeviceName'] ?? 'Unnamed',
      rssi: deviceInfo['DeviceRSSI'] ?? '0',
    );
  }
}
