import 'package:flutter/material.dart';

import 'uvccamera_devices_screen.dart';

/// Root widget for the UVC camera example application.
///
/// Hosts a [MaterialApp] whose home is [UvcCameraDevicesScreen], the entry point
/// for discovering and opening connected UVC cameras.
class UvcCameraDemoApp extends StatefulWidget {
  const UvcCameraDemoApp({super.key});

  @override
  State<UvcCameraDemoApp> createState() => _UvcCameraDemoAppState();
}

/// State for [UvcCameraDemoApp]. Builds the [MaterialApp] shell and routes to
/// [UvcCameraDevicesScreen].
class _UvcCameraDemoAppState extends State<UvcCameraDemoApp> {
  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      title: 'UVC Camera Example',
      home: Scaffold(appBar: AppBar(title: const Text('UVC Camera Example')), body: UvcCameraDevicesScreen()),
    );
  }
}
