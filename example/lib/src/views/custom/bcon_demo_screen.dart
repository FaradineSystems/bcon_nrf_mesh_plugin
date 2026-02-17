// Right now to be used with generic_level_set

import 'dart:async';

import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:flutter_reactive_ble/flutter_reactive_ble.dart';
import 'package:nordic_nrf_mesh_faradine/nordic_nrf_mesh_faradine.dart';
import 'package:nordic_nrf_mesh_example/src/views/custom/bcon_demo/bcon_group_card.dart';
import 'package:nordic_nrf_mesh_example/src/views/custom/custom_screen.dart';
import 'package:provider/provider.dart';

class BconDemoScreen extends StatefulWidget {
  final NordicNrfMesh nordicNrfMesh;
  final MeshManagerApi meshManagerApi;
  // final VoidCallback onDisconnect;

  const BconDemoScreen({
    Key? key,
    required this.nordicNrfMesh,
    required this.meshManagerApi,
  }) : super(key: key);

  @override
  State<BconDemoScreen> createState() => _BconDemoScreenState();
}

class _BconDemoScreenState extends State<BconDemoScreen> {
  final bleMeshManager = BleMeshManager();

  bool isLoading = true;
  late List<ProvisionedMeshNode> nodes;
  bool _isDisposed = false;
  List<int> nodeAddresses = [];

// Constants that should be refactored to come from a higher source later
  final List<String> bconGroups = ['BCON Group'];
  final List<int> subscriptionAddresses = [
    0xC000,
    0xC001,
    0xC002,
    0xC003,
    0xC004,
    0xC005,
    0xC006,
    0xC007,
    0xC008,
    0xC009,
    0xC00A,
    0xC00B
  ];

  @override
  void initState() {
    super.initState();
    _init();
  }

  @override
  void dispose() {
    _isDisposed = true;
    _deinit();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    final bconState = context.read<BconDemoState>();

    // bconTheme built by ChatGPT (I am not an artist)
    final ThemeData bconTheme = ThemeData(
      primaryColor: Colors.black,
      appBarTheme: AppBarTheme(
        backgroundColor: Colors.grey[900],
        titleTextStyle: const TextStyle(
          color: Colors.white,
          fontSize: 20,
          fontWeight: FontWeight.bold,
        ),
        iconTheme: const IconThemeData(
          color: Colors.white,
        ),
      ),
      textTheme: TextTheme(
        bodyLarge: TextStyle(
          color: Colors.grey[800],
          fontSize: 18,
        ),
        bodyMedium: TextStyle(
          color: Colors.grey[800],
          fontSize: 16,
        ),
        bodySmall: TextStyle(
          color: Colors.grey[700],
          fontSize: 14,
        ),
        titleMedium: const TextStyle(
          color: Colors.black,
          fontSize: 20,
          fontWeight: FontWeight.bold,
        ),
        labelMedium: const TextStyle(color: Colors.white, fontSize: 16),
      ),
      cardTheme: CardThemeData(
        color: Colors.grey[200],
        elevation: 4,
        shape: RoundedRectangleBorder(
          borderRadius: BorderRadius.circular(8),
          side: BorderSide(
            color: Colors.grey[500]!,
            width: 1,
          ),
        ),
      ),
      buttonTheme: ButtonThemeData(
        buttonColor: Colors.grey[800],
        textTheme: ButtonTextTheme.primary,
      ),
      elevatedButtonTheme: ElevatedButtonThemeData(
        style: ElevatedButton.styleFrom(
          backgroundColor: Colors.grey[800], // Set your preferred background color
          foregroundColor: Colors.white, // Set your preferred text color
          textStyle: const TextStyle(fontSize: 16), // Optional: customize text style
        ),
      ),
      switchTheme: SwitchThemeData(
        thumbColor: WidgetStateProperty.resolveWith<Color>((states) {
          if (states.contains(WidgetState.selected)) {
            return Colors.grey[300]!; // Color when the switch is on
          }
          return Colors.grey[800]!; // Color when the switch is off
        }),
        trackColor: WidgetStateProperty.resolveWith<Color>((states) {
          if (states.contains(WidgetState.selected)) {
            return Colors.black; // Track color when the switch is on
          }
          return Colors.grey[300]!; // Track color when the switch is off
        }),
        trackOutlineColor: WidgetStateProperty.resolveWith<Color>((states) {
          if (states.contains(WidgetState.selected)) {
            return Colors.black;
          }
          return Colors.grey[800]!;
        }),
        splashRadius: 24.0, // Size of the splash effect when interacting with the switch
      ),
      sliderTheme: const SliderThemeData(inactiveTrackColor: Colors.grey),
      iconTheme: const IconThemeData(
        color: Colors.black,
      ),
      scaffoldBackgroundColor: Colors.grey[800],
      dividerColor: Colors.grey[400],
      hintColor: Colors.grey[600],
      canvasColor: Colors.white,
      colorScheme: ColorScheme(
        primary: Colors.black,
        onPrimary: Colors.white,
        secondary: Colors.grey[700]!,
        onSecondary: Colors.grey[200]!,
        surface: Colors.grey[900]!,
        onSurface: Colors.grey[200]!,
        error: Colors.red[700]!,
        onError: Colors.white,
        brightness: Brightness.dark,
      ),
    );

    Widget layout = Center(
      child: Column(
        mainAxisSize: MainAxisSize.min,
        children: [
          CircularProgressIndicator(
            color: bconTheme.indicatorColor,
            // backgroundColor: Colors.green,
          ),
          Text('Connecting ...', style: bconTheme.textTheme.labelMedium),
        ],
      ),
    );
    if (!isLoading) {
      if (bconGroups.isNotEmpty) {
        layout = Column(
          children: [
            Expanded(
              child: ListView.builder(
                itemCount: bconGroups.length,
                itemBuilder: (context, index) {
                  return BconGroupCard(
                    onDelete: () {},
                    groupName: bconGroups[index],
                    nodes: nodes,
                    subscriptionAddresses: subscriptionAddresses,
                    meshManagerApi: widget.meshManagerApi,
                    nordicNrfMesh: widget.nordicNrfMesh,
                  );
                },
              ),
            ),
          ],
        );
      } else {
        layout = const Text("No BCON groups to display");
      }
    }
    return Theme(
      data: bconTheme,
      child: Scaffold(
        appBar: AppBar(title: const Text('Bcon Demo')),
        body: RefreshIndicator(onRefresh: () => refreshBcons(bconState), child: layout),
      ),
    );
  }

  Future<void> _init() async {
    bleMeshManager.callbacks = DoozProvisionedBleMeshManagerCallbacks(widget.meshManagerApi, bleMeshManager);
    DiscoveredDevice? device;
    final completer = Completer<void>();
    widget.nordicNrfMesh.scanForProxy().listen((proxyDevice) async {
      device = proxyDevice;
      if (!completer.isCompleted) {
        completer.complete();
      }
    });

    // Wait until the completer is completed, meaning the device is assigned
    await completer.future;
    await bleMeshManager.connect(device!);
    // get nodes (ignore first node which is the default provisioner)
    nodes = (await widget.meshManagerApi.meshNetwork!.nodes).skip(1).toList();
    // will bind app keys (needed to be able to configure node)
    for (final node in nodes) {
      final elements = await node.elements;
      nodeAddresses.add(elements[0].address); // initialize addresses of each connected node
      for (final element in elements) {
        for (final model in element.models) {
          if (model.boundAppKey.isEmpty) {
            if (element == elements.first && model == element.models.first) {
              continue;
            }
            final unicast = await node.unicastAddress;
            debugPrint('need to bind app key');
            await widget.meshManagerApi.sendConfigModelAppBind(
              unicast,
              element.address,
              model.modelId,
            );
            debugPrint('send config done');
          }
        }
      }
    }

    addNodesToGroup();

    // set isLoading and start subscription to bcon data
    if (mounted) {
      final bconState = context.read<BconDemoState>();

      refreshBcons(bconState);

      setState(() {
        isLoading = false;
      });
    }
  }

  void addNodesToGroup() async {
    // Count how many generic level servers there are
    const int genericLevelServer = 0x1002;

    // Loop over each node's elements and subscribe those elements to the group addresses
    for (final node in nodes) {
      final elements = await node.elements;
      List<int> genericLevelAddresses = [];
      int genericLevelServerCount = 0;

      // Count addresses of generic level servers
      for (final element in elements) {
        for (var model in element.models) {
          if (model.key == genericLevelServer) {
            genericLevelServerCount++;
            genericLevelAddresses.add(element.address);
          }
        }
      }

      for (var i = 0; i < genericLevelServerCount; i++) {
        try {
          // Probably don't want to await here in case its not connecting
          await widget.meshManagerApi
              .sendConfigModelSubscriptionAdd(genericLevelAddresses[i], subscriptionAddresses[i], genericLevelServer)
              .timeout(const Duration(seconds: 5));
        } on TimeoutException catch (_) {
          // scaffoldMessenger.showSnackBar(const SnackBar(content: Text('Board didn\'t respond')));
          debugPrint('Board didn\'t respond');
        } on PlatformException catch (e) {
          // scaffoldMessenger.showSnackBar(SnackBar(content: Text('${e.message}')));
          debugPrint('${e.message}');
        } catch (e) {
          // scaffoldMessenger.showSnackBar(SnackBar(content: Text(e.toString())));
          debugPrint(e.toString());
        }
      }
    }
  }

  void _deinit() async {
    await bleMeshManager.disconnect();
    await bleMeshManager.callbacks!.dispose();
  }

  /// Function to handle requesting data from each bcon in the group and updating state accordingly.
  ///
  /// Requires: Instance of BconDemoState for storing results
  Future<void> refreshBcons(BconDemoState bconState) async {
    // Set all bcons to 'connecting' state
    bconState.updateAllConnections(BconConnectionState.connecting);

    // tasks list stores tasks containing the getLevels function so that they
    // don't have to wait on one another when they are executed below
    List<Future<void>> tasks = [];

    // For each element of each node
    for (int nodeIndex = 0; nodeIndex < nodes.length; nodeIndex++) {
      var node = nodes[nodeIndex];

      tasks.add(() async {
        List<int> elementAddresses = [];

        // Just get the high brightness RGB
        var elements = await node.elements;
        elementAddresses.add(elements[1].address);
        elementAddresses.add(elements[2].address);
        elementAddresses.add(elements[3].address);

        ColorResponse? newColor = await getLevels(elementAddresses);

        if (newColor == null) {
          debugPrint('[BCON Refresh] Could not get response from board');
          bconState.updateColor(nodeIndex, Colors.black);
          bconState.updateConnection(nodeIndex, BconConnectionState.disconnected);
        } else {
          int r = to8BitInt(newColor.red);
          int g = to8BitInt(newColor.green);
          int b = to8BitInt(newColor.blue);
          debugPrint('[BCON Refresh] $r, $g, $b');

          // Update color for the specific node index
          bconState.updateColor(nodeIndex, Color.fromRGBO(r, g, b, 1));
          bconState.updateConnection(nodeIndex, BconConnectionState.connected);
        }
      }());
    }

    // Await all tasks to complete
    await Future.wait(tasks);
  }

  /// Get levels from device
  /// Takes a list of three addresses and sends a Generic Level Get message to each of them
  /// for R, G, & B levels. Returns a ColorResponse which has a red, green, and blue component from 0-255
  Future<ColorResponse?> getLevels(List<int> addresses) async {
    final scaffoldMessenger = ScaffoldMessenger.of(context); // for snackbars (bottom of screen pop ups)

    try {
      final redStatus = await widget.meshManagerApi
          .sendGenericLevelGet(addresses[0])
          .timeout(const Duration(seconds: 5)); // originally set to 40 which is probably more reasonable
      final greenStatus =
          await widget.meshManagerApi.sendGenericLevelGet(addresses[1]).timeout(const Duration(seconds: 5));
      final blueStatus =
          await widget.meshManagerApi.sendGenericLevelGet(addresses[2]).timeout(const Duration(seconds: 5));

      debugPrint('[Color Status] ${redStatus.level} ${greenStatus.level} ${blueStatus.level}');
      return ColorResponse(redStatus.level, greenStatus.level, blueStatus.level);

      // Error handling
    } on TimeoutException catch (_) {
      scaffoldMessenger.clearSnackBars();
      // prevents unhandled error of snackbar trying to show up on another screen that doesn't have a scaffold
      if (!_isDisposed) {
        scaffoldMessenger.showSnackBar(SnackBar(content: Text('Address ${addresses[0]} didn\'t respond')));
      }
    } on PlatformException catch (e) {
      if (!_isDisposed) {
        scaffoldMessenger.showSnackBar(SnackBar(content: Text('${e.message}')));
      }
    } catch (e) {
      if (!_isDisposed) {
        scaffoldMessenger.showSnackBar(SnackBar(content: Text(e.toString())));
      }
    }

    // return null for errors
    return null;
  }
}

// Callbacks for bleMeshManager
class DoozProvisionedBleMeshManagerCallbacks extends BleMeshManagerCallbacks {
  final MeshManagerApi meshManagerApi;
  final BleMeshManager bleMeshManager;

  late StreamSubscription<ConnectionStateUpdate> onDeviceConnectingSubscription;
  late StreamSubscription<ConnectionStateUpdate> onDeviceConnectedSubscription;
  late StreamSubscription<BleManagerCallbacksDiscoveredServices> onServicesDiscoveredSubscription;
  late StreamSubscription<DiscoveredDevice> onDeviceReadySubscription;
  late StreamSubscription<BleMeshManagerCallbacksDataReceived> onDataReceivedSubscription;
  late StreamSubscription<BleMeshManagerCallbacksDataSent> onDataSentSubscription;
  late StreamSubscription<ConnectionStateUpdate> onDeviceDisconnectingSubscription;
  late StreamSubscription<ConnectionStateUpdate> onDeviceDisconnectedSubscription;
  late StreamSubscription<List<int>> onMeshPduCreatedSubscription;

  DoozProvisionedBleMeshManagerCallbacks(this.meshManagerApi, this.bleMeshManager) {
    onDeviceConnectingSubscription = onDeviceConnecting.listen((event) {
      debugPrint('onDeviceConnecting $event');
    });
    onDeviceConnectedSubscription = onDeviceConnected.listen((event) {
      debugPrint('onDeviceConnected $event');
    });

    onServicesDiscoveredSubscription = onServicesDiscovered.listen((event) {
      debugPrint('onServicesDiscovered');
    });

    onDeviceReadySubscription = onDeviceReady.listen((event) async {
      debugPrint('onDeviceReady ${event.id}');
    });

    onDataReceivedSubscription = onDataReceived.listen((event) async {
      debugPrint('onDataReceived ${event.device.id} ${event.pdu} ${event.mtu}');
      await meshManagerApi.handleNotifications(event.mtu, event.pdu);
    });
    onDataSentSubscription = onDataSent.listen((event) async {
      debugPrint('onDataSent ${event.device.id} ${event.pdu} ${event.mtu}');
      await meshManagerApi.handleWriteCallbacks(event.mtu, event.pdu);
    });

    onDeviceDisconnectingSubscription = onDeviceDisconnecting.listen((event) {
      debugPrint('onDeviceDisconnecting $event');
    });
    onDeviceDisconnectedSubscription = onDeviceDisconnected.listen((event) {
      debugPrint('onDeviceDisconnected $event');
    });

    onMeshPduCreatedSubscription = meshManagerApi.onMeshPduCreated.listen((event) async {
      // debugPrint('onMeshPduCreated $event');
      await bleMeshManager.sendPdu(event);
    });
  }

  @override
  Future<void> dispose() => Future.wait([
        onDeviceConnectingSubscription.cancel(),
        onDeviceConnectedSubscription.cancel(),
        onServicesDiscoveredSubscription.cancel(),
        onDeviceReadySubscription.cancel(),
        onDataReceivedSubscription.cancel(),
        onDataSentSubscription.cancel(),
        onDeviceDisconnectingSubscription.cancel(),
        onDeviceDisconnectedSubscription.cancel(),
        onMeshPduCreatedSubscription.cancel(),
        super.dispose(),
      ]);

  @override
  Future<void> sendMtuToMeshManagerApi(int mtu) => meshManagerApi.setMtu(mtu);
}

class ColorResponse {
  final int red;
  final int green;
  final int blue;

  ColorResponse(this.red, this.green, this.blue);
}

/// Takes a 16 bit signed integer and returns a value from 0 - 255
int to8BitInt(int sixteenBitInt) {
  return (((sixteenBitInt + 32768) / 65355) * 255).round();
}
