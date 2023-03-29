import 'package:flutter/material.dart';
import 'package:flutter/services.dart';

void main() {
  WidgetsFlutterBinding.ensureInitialized();
  runApp(const MyApp());
}

class MyApp extends StatelessWidget {
  const MyApp({super.key});

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      title: 'Flutter Demo',
      theme: ThemeData(
        primarySwatch: Colors.blue,
      ),
      home: const MyHomePage(title: 'Flutter Demo Home Page'),
    );
  }
}

class MyHomePage extends StatefulWidget {
  const MyHomePage({super.key, required this.title});

  final String title;

  @override
  State<MyHomePage> createState() => _MyHomePageState();
}

class _MyHomePageState extends State<MyHomePage> {
  int _counter = 0;
  bool isLoading = false;
  final TextEditingController _controller = TextEditingController();
  String locationData = "location not got till";
  final MethodChannel _channel =
      const MethodChannel('samples.flutter.dev/battery');
  final EventChannel _stream = const EventChannel('locationStatusStream');

  String _batteryLevel = 'Unknown battery level.';

  Future<void> getBatteryLevel() async {
    String batteryLevel;

    setState(() {
      _batteryLevel = 'Unknown battery level......';
    });
    try {
      final int result = await _channel.invokeMethod('getBatteryLevel');
      batteryLevel = 'Battery level at $result % .';
    } on PlatformException catch (e) {
      batteryLevel = "Failed to get battery level: '${e.message}'.";
    } on MissingPluginException {
      batteryLevel = 'Failed to get battery level.';
    }

    setState(() {
      _batteryLevel = batteryLevel;
    });
  }

  Future<void> getLocation() async {
    setState(() {
      isLoading = true;
    });
    try {
      locationData = await _channel.invokeMethod('getUserLocation');
      _stream.receiveBroadcastStream().listen((event) {
        print("event $event");
      });
    } on PlatformException {
      locationData = "Failed to get location";
    }
    setState(() {
      isLoading = false;
    });
  }

  void _incrementCounter() {}

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: Text(widget.title),
      ),
      body: Center(
        child: Column(
          mainAxisAlignment: MainAxisAlignment.center,
          children: <Widget>[
            TextField(
              controller: _controller,
              decoration: InputDecoration(
                border: OutlineInputBorder(),
                labelText: 'Enter a search term',
              ),
            ),
            ElevatedButton(
                onPressed: sendDataToNative,
                child: Text("Pass to Android Native")),
            SizedBox(
              height: 10,
            ),
            IntrinsicHeight(
              child: Row(
                children: const [
                  Expanded(
                      child: Divider(
                    color: Colors.black,
                    endIndent: 10,
                  )),
                  Text("Battery Percentage"),
                  Expanded(
                      child: Divider(
                    color: Colors.black,
                    indent: 10,
                  )),
                ],
              ),
            ),
            SizedBox(
              height: 10,
            ),
            Text(_batteryLevel),
            ElevatedButton(
              onPressed: getBatteryLevel,
              child: const Text('Get Battery Level'),
            ),
            SizedBox(
              height: 10,
            ),
            IntrinsicHeight(
              child: Row(
                children: const [
                  Expanded(
                      child: Divider(
                    color: Colors.black,
                    endIndent: 10,
                  )),
                  Text("Method Channel with location data"),
                  Expanded(
                      child: Divider(
                    color: Colors.black,
                    indent: 10,
                  )),
                ],
              ),
            ),
            SizedBox(
              height: 10,
            ),
            isLoading
                ? const Center(
                    child: CircularProgressIndicator(),
                  )
                : Text(locationData),
            ElevatedButton(
              onPressed: getLocation,
              child: const Text('Get Location'),
            ),
            SizedBox(
              height: 10,
            ),
            IntrinsicHeight(
              child: Row(
                children: const [
                  Expanded(
                      child: Divider(
                    color: Colors.black,
                    endIndent: 10,
                  )),
                  Text("Event Channel with location data"),
                  Expanded(
                      child: Divider(
                    color: Colors.black,
                    indent: 10,
                  )),
                ],
              ),
            ),
            SizedBox(
              height: 10,
            ),
            StreamBuilder(
              stream: _stream.receiveBroadcastStream(),
              builder: (context, snapshot) {
                if (snapshot.hasData) {
                  return Text(snapshot.data.toString());
                } else {
                  return const Text(
                      "Make changes in location service will reflect here");
                }
              },
            ),
          ],
        ),
      ),
    );
  }

  Future<void> sendDataToNative() async {
    try {
      await _channel
          .invokeMethod('getDataFromFlutter', {'data': _controller.text});
    } on PlatformException catch (e) {
      print('"SendDataTONative"${e.message}');
    } on MissingPluginException {
      print('"SendDataToNative" not implemented');
    }
  }
}
