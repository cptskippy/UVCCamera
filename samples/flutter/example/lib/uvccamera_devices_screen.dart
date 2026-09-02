import 'dart:async';

import 'package:flutter/material.dart';
import 'package:uvccamera/uvccamera.dart';

import 'uvccamera_device_screen.dart';

/// Screen listing the UVC cameras currently connected to the device.
///
/// Seeds the list from [UvcCamera.getDevices] on start and keeps it in sync by
/// listening to [UvcCamera.deviceEventStream] for attach/detach events. Tapping a
/// device opens [UvcCameraDeviceScreen].
///
/// State Machine:
///   Loading → Unsupported (UVC not supported)
///   Loading → Empty (supported, no devices)
///   Loading → List (one or more devices)
///   List ↔ Empty on attach/detach device events.
class UvcCameraDevicesScreen extends StatefulWidget {
  const UvcCameraDevicesScreen({super.key});

  /// Create the state object for [UvcCameraDevicesScreen].
  @override
  State<UvcCameraDevicesScreen> createState() => _UvcCameraDevicesScreenState();
}

/// State for [UvcCameraDevicesScreen].
///
/// Tracks the UVC support flag, the live device map, and the device-event
/// subscription.
class _UvcCameraDevicesScreenState extends State<UvcCameraDevicesScreen> {
  bool _isSupported = false;
  StreamSubscription<UvcCameraDeviceEvent>? _deviceEventSubscription;
  final Map<String, UvcCameraDevice> _devices = {};

  /// Initialize: query the support flag, subscribe to device events, and seed the
  /// device list.
  ///
  /// Side Effects:
  ///   - Subscribes to [UvcCamera.deviceEventStream] for attach/detach updates.
  ///   - Seeds [_devices] from [UvcCamera.getDevices].
  ///   - Sets [_isSupported] from [UvcCamera.isSupported].
  @override
  void initState() {
    super.initState();

    UvcCamera.isSupported().then((value) {
      setState(() {
        _isSupported = value;
      });
    });

    _deviceEventSubscription = UvcCamera.deviceEventStream.listen((event) {
      setState(() {
        if (event.type == UvcCameraDeviceEventType.attached) {
          _devices[event.device.name] = event.device;
        } else if (event.type == UvcCameraDeviceEventType.detached) {
          _devices.remove(event.device.name);
        }
      });
    });

    UvcCamera.getDevices().then((devices) {
      setState(() {
        _devices.addAll(devices);
      });
    });
  }

  /// Cancel the device-event subscription.
  ///
  /// Side Effects:
  ///   - Cancels [_deviceEventSubscription].
  @override
  void dispose() {
    _deviceEventSubscription?.cancel();
    _deviceEventSubscription = null;

    super.dispose();
  }

  /// Build the screen.
  ///
  /// Returns:
  ///   A message when UVC is unsupported or no devices are present, otherwise a
  ///   [ListView] of device tiles.
  ///
  /// Code Paths:
  ///   1. If not supported → "not supported" message.
  ///   2. If no devices → "no devices" message.
  ///   3. Otherwise → [ListView] of [ListTile]s; tapping one pushes
  ///      [UvcCameraDeviceScreen].
  @override
  Widget build(BuildContext context) {
    if (!_isSupported) {
      return const Center(child: Text('UVC Camera is not supported on this device.', style: TextStyle(fontSize: 18)));
    }

    if (_devices.isEmpty) {
      return const Center(child: Text('No UVC devices connected.', style: TextStyle(fontSize: 18)));
    }

    return ListView(
      children:
          _devices.values.map((device) {
            return ListTile(
              leading: const Icon(Icons.videocam),
              title: Text(device.name),
              subtitle: Text('Vendor ID: ${device.vendorId}, Product ID: ${device.productId}'),
              onTap: () {
                Navigator.push(context, MaterialPageRoute(builder: (context) => UvcCameraDeviceScreen(device: device)));
              },
            );
          }).toList(),
    );
  }
}
