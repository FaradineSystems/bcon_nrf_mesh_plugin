import 'package:flutter/foundation.dart';
import 'package:flutter/material.dart';
import 'package:nordic_nrf_mesh_faradine/nordic_nrf_mesh_faradine.dart';
import 'package:nordic_nrf_mesh_example/src/views/control_module/provisioned_devices.dart';
import 'package:nordic_nrf_mesh_example/src/views/home/home.dart';
import 'package:nordic_nrf_mesh_example/src/views/scan_and_provisionning/scan_and_provisioning.dart';
import 'package:permission_handler/permission_handler.dart';
import 'package:nordic_nrf_mesh_example/src/views/custom/custom_screen.dart';
import 'package:provider/provider.dart';

const int homeTab = 0;
const int provisioningTab = 1;
const int controlTab = 2;
const int customTab = 3;
// used for app's theme
const Map<int, Color> primarySwatch = {
  50: Color.fromRGBO(0, 164, 153, .1),
  100: Color.fromRGBO(0, 164, 153, .2),
  200: Color.fromRGBO(0, 164, 153, .3),
  300: Color.fromRGBO(0, 164, 153, .4),
  400: Color.fromRGBO(0, 164, 153, .5),
  500: Color.fromRGBO(0, 164, 153, .6),
  600: Color.fromRGBO(0, 164, 153, .7),
  700: Color.fromRGBO(0, 164, 153, .8),
  800: Color.fromRGBO(0, 164, 153, .9),
  900: Color.fromRGBO(0, 164, 153, 1),
};

class NordicNrfMeshExampleApp extends StatefulWidget {
  const NordicNrfMeshExampleApp({Key? key}) : super(key: key);

  @override
  State<NordicNrfMeshExampleApp> createState() => _NordicNrfMeshExampleAppState();
}

class _NordicNrfMeshExampleAppState extends State<NordicNrfMeshExampleApp> {
  late final GlobalKey<ScaffoldMessengerState> _scaffoldKey =
      GlobalKey<ScaffoldMessengerState>(debugLabel: 'main_scaffold');
  late final NordicNrfMesh nordicNrfMesh = NordicNrfMesh();

  int _bottomNavigationBarIndex = homeTab;

  @override
  Widget build(BuildContext context) {
    Widget body = const SizedBox.shrink();

    if (_bottomNavigationBarIndex == homeTab) {
      //  home
      body = Home(nordicNrfMesh: nordicNrfMesh);
    } else if (_bottomNavigationBarIndex == provisioningTab) {
      //  scanning & provisionning
      body = ScanningAndProvisioning(
          nordicNrfMesh: nordicNrfMesh,
          onGoToControl: () {
            setState(() {
              _bottomNavigationBarIndex = controlTab;
            });
          });
    } else if (_bottomNavigationBarIndex == controlTab) {
      //  List provisioned devices and then can control/setup them
      body = ProvisionedDevices(
          nordicNrfMesh: nordicNrfMesh,
          onGoToProvisioning: () {
            setState(() {
              _bottomNavigationBarIndex = provisioningTab;
            });
          });
    } else if (_bottomNavigationBarIndex == customTab) {
      // Custom stuff JL implemented for demos
      body = CustomScreen(nordicNrfMesh: nordicNrfMesh);
    }

    // return ChangeNotifierProvider(
    //   create: (_) => BconDemoState(),
    //   builder: (newContext, child) => MaterialApp(
    return ChangeNotifierProvider(
      create: (context) => BconDemoState(),
      child: MaterialApp(
        theme: ThemeData(
          brightness: Brightness.light,
          primarySwatch: const MaterialColor(0xFF00A499, primarySwatch),
          primaryColor: const Color.fromRGBO(0, 164, 153, 1),
          primaryColorDark: const Color.fromRGBO(0, 114, 105, 1),
          scaffoldBackgroundColor: Colors.white,
        ),
        home: ScaffoldMessenger(
          key: _scaffoldKey,
          child: Scaffold(
            appBar: AppBar(),
            body: body,
            bottomNavigationBar: BottomNavigationBar(
              type: BottomNavigationBarType.fixed,
              currentIndex: _bottomNavigationBarIndex,
              onTap: (newBottomNavigationBarIndex) {
                if (nordicNrfMesh.meshManagerApi.meshNetwork != null) {
                  setState(() {
                    _bottomNavigationBarIndex = newBottomNavigationBarIndex;
                  });
                } else {
                  _scaffoldKey.currentState!.clearSnackBars();
                  _scaffoldKey.currentState!.showSnackBar(const SnackBar(content: Text('Please load a mesh network')));
                }
              },
              items: const [
                BottomNavigationBarItem(
                  icon: Icon(Icons.home),
                  label: 'Home',
                ),
                BottomNavigationBarItem(
                  icon: Icon(Icons.bluetooth_searching),
                  label: 'Provisioning',
                ),
                BottomNavigationBarItem(
                  icon: Icon(Icons.videogame_asset),
                  label: 'Control',
                ),
                BottomNavigationBarItem(
                  icon: Icon(Icons.settings),
                  label: 'Custom',
                ),
              ],
            ),
          ),
        ),
      ),
    );
  }
}

void log(Object? msg) => debugPrint('[$NordicNrfMeshExampleApp - ${DateTime.now().toIso8601String()}] $msg');

Future<void> checkAndAskPermissions() async {
  if (defaultTargetPlatform == TargetPlatform.android) {
    //COMMENTED OUT
    // final androidInfo = await DeviceInfoPlugin().androidInfo;
    // if (androidInfo.version.sdkInt! < 31) {
    //   // location
    //   await Permission.locationWhenInUse.request();
    //   await Permission.locationAlways.request();
    //   // bluetooth
    //   await Permission.bluetooth.request();
    // } else {
    // bluetooth for Android 12 and up
    await Permission.bluetoothScan.request();
    await Permission.bluetoothConnect.request();
    //COMMENTED OUT
    // }
    // } else {
    //   // bluetooth for iOS 13 and up
    //   await Permission.bluetooth.request();
  }
}
