import 'uvccamera_device.dart';
import 'uvccamera_device_event.dart';
import 'uvccamera_platform_interface.dart';

/// UVC Camera plugin entry point.
///
/// Provides static helpers for device discovery and permission.
///
/// Thread Safety:
///   Static methods are safe to call from main isolate.
class UvcCamera {
  /// Check if UVC camera is supported on the current device.
  ///
  /// Returns:
  ///   True if UVC cameras are supported.
  static Future<bool> isSupported() {
    return UvcCameraPlatformInterface.instance.isSupported();
  }

  /// Get all UVC camera devices connected to the device.
  ///
  /// Returns:
  ///   Map of device name to [UvcCameraDevice].
  static Future<Map<String, UvcCameraDevice>> getDevices() {
    return UvcCameraPlatformInterface.instance.getDevices();
  }

  /// Request permission to access a UVC camera device.
  ///
  /// Args:
  ///   uvcCameraDevice: Device to request permission for.
  ///
  /// Returns:
  ///   True if permission granted.
  static Future<bool> requestDevicePermission(UvcCameraDevice uvcCameraDevice) {
    return UvcCameraPlatformInterface.instance.requestDevicePermission(uvcCameraDevice);
  }

  /// Get device event stream.
  ///
  /// Returns:
  ///   Stream of [UvcCameraDeviceEvent] for attach/detach events.
  static Stream<UvcCameraDeviceEvent> get deviceEventStream {
    return UvcCameraPlatformInterface.instance.deviceEventStream;
  }
}
