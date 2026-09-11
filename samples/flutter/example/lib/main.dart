import 'package:flutter/material.dart';

import 'uvccamera_demo_app.dart';

/// Application entry point for the UVC camera example.
///
/// Initializes the Flutter binding and launches [UvcCameraDemoApp].
void main() {
  WidgetsFlutterBinding.ensureInitialized();

  runApp(const UvcCameraDemoApp());
}
