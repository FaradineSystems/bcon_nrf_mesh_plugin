import 'dart:async';
import 'dart:convert';

import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:nordic_nrf_mesh_faradine/nordic_nrf_mesh_faradine.dart';

class SendVendorModelMessage extends StatefulWidget {
  final MeshManagerApi meshManagerApi;

  const SendVendorModelMessage(this.meshManagerApi, {Key? key}) : super(key: key);

  @override
  State<SendVendorModelMessage> createState() => _SendVendorModelMessageState();
}

class _SendVendorModelMessageState extends State<SendVendorModelMessage> {
  late int selectedElementAddress = 3;
  late int selectedModelId = 0x59000B;
  late int companyIdentifier = 0x59;
  late int selectedOpCode = 0x03;
  late Uint8List parameters = Uint8List.fromList([0]);

  bool parametersAsHex = true;

  // Controllers for testing purposes:
  final TextEditingController _addressController = TextEditingController(text: "3");
  final TextEditingController _modelController = TextEditingController(text: "59000B");
  final TextEditingController _opCodeController = TextEditingController(text: "03");
  final TextEditingController _parameterController = TextEditingController(text: "");

  @override
  Widget build(BuildContext context) {
    return ExpansionTile(
      title: const Text('Send a vendor model message'),
      initiallyExpanded: true,
      children: <Widget>[
        Padding(
          padding: const EdgeInsets.fromLTRB(16.0, 0, 16.0, 0),
          child: Column(
            children: [
              TextField(
                decoration: const InputDecoration(labelText: 'Element Address (dec)'),
                controller: _addressController,
                onChanged: (text) {
                  try {
                    selectedElementAddress = int.parse(text);
                  } on Exception catch (_) {
                    // TODO
                  }
                },
              ),
              TextField(
                decoration: const InputDecoration(
                  labelText: 'Model id (hex)',
                  prefixText: "0x",
                ),
                controller: _modelController,
                onChanged: (text) {
                  // selectedModelId = int.parse(text);
                  selectedModelId = int.parse(text, radix: 16);
                },
              ),
              TextField(
                decoration: const InputDecoration(
                  labelText: 'Opcode without company id (hex)',
                  prefixText: "0x",
                ),
                controller: _opCodeController,
                onChanged: (text) {
                  try {
                    selectedOpCode = int.parse(text, radix: 16);
                  } on Exception catch (e) {
                    // TODO
                  }
                },
              ),
              Row(
                children: [
                  Expanded(
                    child: TextField(
                      decoration: InputDecoration(
                        labelText: 'Parameters ${parametersAsHex ? 'Hex' : 'String'}',
                        prefixText: parametersAsHex ? '0x' : '',
                      ),
                      controller: _parameterController,
                      onChanged: (text) => updateParameters(text),
                    ),
                  ),
                  Column(
                    children: [
                      Text(parametersAsHex ? 'Hex' : 'String', style: Theme.of(context).textTheme.bodySmall),
                      Switch(
                        onChanged: (value) {
                          setState(() {
                            parametersAsHex = value;
                          });
                        },
                        value: parametersAsHex,
                      ),
                    ],
                  )
                ],
              ),
            ],
          ),
        ),
        TextButton(
          onPressed: () async {
            final scaffoldMessenger = ScaffoldMessenger.of(context);
            try {
              var status = await widget.meshManagerApi
                  .sendVendorModelMessage(selectedElementAddress, selectedModelId, companyIdentifier, selectedOpCode,
                      parameters) // fix company identifier
                  .timeout(const Duration(seconds: 3)); // set time out to something better.
              String hexMessage = uint8ListToHex(status.message);
              String asciiMessage = uint8ListToAscii(status.message);

              scaffoldMessenger.clearSnackBars();
              scaffoldMessenger.showSnackBar(SnackBar(content: Text('Hex: $hexMessage \nASCII: $asciiMessage')));
            } on TimeoutException catch (_) {
              scaffoldMessenger.clearSnackBars();

              scaffoldMessenger.showSnackBar(const SnackBar(content: Text('Board didn\'t respond')));
            } on PlatformException catch (e) {
              scaffoldMessenger.showSnackBar(SnackBar(content: Text('${e.message}')));
            } catch (e) {
              scaffoldMessenger.showSnackBar(SnackBar(content: Text(e.toString())));
            }
          },
          child: const Text('Send vendor message'),
        )
      ],
    );
  }

  updateParameters(String text) {
    if (parametersAsHex) {
      String spacelessText = text.replaceAll(" ", "");
      if (spacelessText.length % 2 == 0) {
        parameters = hexToUint8List(spacelessText);
      } else {
        parameters = hexToUint8List(spacelessText.substring(0, spacelessText.length - 1));
      }
    } else {
      parameters = Uint8List.fromList(utf8.encode('$text\u0000'));
    }
  }
}

String uint8ListToHex(Uint8List bytes) {
  return bytes.map((byte) => byte.toRadixString(16).padLeft(2, '0')).join().toUpperCase();
}

String uint8ListToAscii(Uint8List bytes) {
  return String.fromCharCodes(bytes);
}

Uint8List hexToUint8List(String hex) {
  // Remove any spaces or extra characters in the hex string if necessary
  hex = hex.replaceAll(' ', '');

  // Ensure the hex string has an even length
  if (hex.length % 2 != 0) {
    throw ArgumentError('Hex string must have an even length');
  }

  // Create a Uint8List with half the length of the hex string
  final bytes = Uint8List(hex.length ~/ 2);

  // Convert each pair of hex characters to a byte
  for (int i = 0; i < hex.length; i += 2) {
    final byteString = hex.substring(i, i + 2);
    final byteValue = int.parse(byteString, radix: 16);
    bytes[i ~/ 2] = byteValue;
  }

  // bytes.last = 0;

  return bytes;
}
