import 'package:flutter/material.dart';
import 'package:uvccamera/uvccamera.dart';

import 'uvccamera_widget.dart';

/// Screen showing the live preview and controls for a single UVC camera.
///
/// Wraps [UvcCameraWidget] in a [Scaffold] whose app bar shows the device name.
///
/// Parameters:
///   device: The [UvcCameraDevice] to display and control.
class UvcCameraDeviceScreen extends StatelessWidget {
  final UvcCameraDevice device;

  const UvcCameraDeviceScreen({super.key, required this.device});

  /// Build the device preview screen.
  @override
  Widget build(BuildContext context) {
    return Scaffold(appBar: AppBar(title: Text(device.name)), body: Center(child: UvcCameraWidget(device: device)));
  }
}
