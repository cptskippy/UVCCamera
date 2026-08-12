import 'package:plugin_platform_interface/plugin_platform_interface.dart';
import 'package:cross_file/cross_file.dart';

import 'uvccamera_button_event.dart';
import 'uvccamera_device.dart';
import 'uvccamera_device_event.dart';
import 'uvccamera_error_event.dart';
import 'uvccamera_platform.dart';
import 'uvccamera_mode.dart';
import 'uvccamera_resolution_preset.dart';
import 'uvccamera_status_event.dart';

/// Platform interface for UVC camera operations.
///
/// Defines the contract for platform-specific implementations. Use [instance] to access the default implementation.
///
/// Thread Safety:
///   Implementations must be thread-safe for concurrent calls from Dart isolate.
abstract class UvcCameraPlatformInterface extends PlatformInterface {
  /// Create a platform interface instance.
  UvcCameraPlatformInterface() : super(token: _token);

  static final Object _token = Object();

  static UvcCameraPlatformInterface _instance = UvcCameraPlatform();

  /// Get the current platform instance.
  ///
  /// Returns:
  ///   The active [UvcCameraPlatformInterface] implementation.
  static UvcCameraPlatformInterface get instance => _instance;

  /// Set the platform instance for testing.
  ///
  /// Args:
  ///   instance: New platform implementation.
  static set instance(UvcCameraPlatformInterface instance) {
    PlatformInterface.verifyToken(instance, _token);
    _instance = instance;
  }

  /// Check if UVC camera is supported on this platform.
  ///
  /// Returns:
  ///   True if UVC cameras are supported.
  Future<bool> isSupported() {
    throw UnimplementedError('isSupported() has not been implemented.');
  }

  /// Get all connected UVC camera devices.
  ///
  /// Returns:
  ///   Map of device name to [UvcCameraDevice].
  Future<Map<String, UvcCameraDevice>> getDevices() {
    throw UnimplementedError('getDevices() has not been implemented.');
  }

  /// Request permission to access a UVC device.
  ///
  /// Args:
  ///   device: Device to request permission for.
  ///
  /// Returns:
  ///   True if permission granted.
  Future<bool> requestDevicePermission(UvcCameraDevice device) {
    throw UnimplementedError('requestDevicePermission() has not been implemented.');
  }

  /// Open a camera device.
  ///
  /// Args:
  ///   device: Device to open.
  ///   resolutionPreset: Desired resolution preset.
  ///
  /// Returns:
  ///   Platform camera identifier.
  Future<int> openCamera(UvcCameraDevice device, UvcCameraResolutionPreset resolutionPreset) {
    throw UnimplementedError('openCamera() has not been implemented.');
  }

  /// Close a camera.
  ///
  /// Args:
  ///   cameraId: Platform camera identifier.
  Future<void> closeCamera(int cameraId) {
    throw UnimplementedError('closeCamera() has not been implemented.');
  }

  /// Get texture ID for camera preview.
  ///
  /// Args:
  ///   cameraId: Platform camera identifier.
  ///
  /// Returns:
  ///   OpenGL texture identifier.
  Future<int> getCameraTextureId(int cameraId) {
    throw UnimplementedError('getCameraTextureId() has not been implemented.');
  }

  /// Attach to camera error callback stream.
  ///
  /// Args:
  ///   cameraId: Platform camera identifier.
  ///
  /// Returns:
  ///   Stream of [UvcCameraErrorEvent].
  Future<Stream<UvcCameraErrorEvent>> attachToCameraErrorCallback(int cameraId) {
    throw UnimplementedError('attachToCameraErrorCallback() has not been implemented.');
  }

  /// Detach from camera error callback.
  ///
  /// Args:
  ///   cameraId: Platform camera identifier.
  Future<void> detachFromCameraErrorCallback(int cameraId) {
    throw UnimplementedError('detachFromCameraErrorCallback() has not been implemented.');
  }

  /// Attach to camera status callback stream.
  ///
  /// Args:
  ///   cameraId: Platform camera identifier.
  ///
  /// Returns:
  ///   Stream of [UvcCameraStatusEvent].
  Future<Stream<UvcCameraStatusEvent>> attachToCameraStatusCallback(int cameraId) {
    throw UnimplementedError('attachToCameraStatusCallback() has not been implemented.');
  }

  /// Detach from camera status callback.
  ///
  /// Args:
  ///   cameraId: Platform camera identifier.
  Future<void> detachFromCameraStatusCallback(int cameraId) {
    throw UnimplementedError('detachFromCameraStatusCallback() has not been implemented.');
  }

  /// Attach to camera button callback stream.
  ///
  /// Args:
  ///   cameraId: Platform camera identifier.
  ///
  /// Returns:
  ///   Stream of [UvcCameraButtonEvent].
  Future<Stream<UvcCameraButtonEvent>> attachToCameraButtonCallback(int cameraId) {
    throw UnimplementedError('attachToCameraButtonCallback() has not been implemented.');
  }

  /// Detach from camera button callback.
  ///
  /// Args:
  ///   cameraId: Platform camera identifier.
  Future<void> detachFromCameraButtonCallback(int cameraId) {
    throw UnimplementedError('detachFromCameraButtonCallback() has not been implemented.');
  }

  /// Get supported modes for camera.
  ///
  /// Args:
  ///   cameraId: Platform camera identifier.
  ///
  /// Returns:
  ///   List of supported [UvcCameraMode].
  Future<List<UvcCameraMode>> getSupportedModes(int cameraId) {
    throw UnimplementedError('getSupportedModes() has not been implemented.');
  }

  /// Get current preview mode.
  ///
  /// Args:
  ///   cameraId: Platform camera identifier.
  ///
  /// Returns:
  ///   Current preview [UvcCameraMode].
  Future<UvcCameraMode> getPreviewMode(int cameraId) {
    throw UnimplementedError('getPreviewMode() has not been implemented.');
  }

  /// Set preview mode.
  ///
  /// Args:
  ///   cameraId: Platform camera identifier.
  ///   previewMode: Mode to set.
  Future<void> setPreviewMode(int cameraId, UvcCameraMode previewMode) {
    throw UnimplementedError('setPreviewMode() has not been implemented.');
  }

  /// Take picture.
  ///
  /// Args:
  ///   cameraId: Platform camera identifier.
  ///
  /// Returns:
  ///   Captured image file.
  Future<XFile> takePicture(int cameraId) {
    throw UnimplementedError('takePicture() has not been implemented.');
  }

  /// Start video recording.
  ///
  /// Args:
  ///   cameraId: Platform camera identifier.
  ///   videoRecordingMode: Recording mode.
  ///
  /// Returns:
  ///   Video file reference.
  Future<XFile> startVideoRecording(int cameraId, UvcCameraMode videoRecordingMode) {
    throw UnimplementedError('startVideoRecording() has not been implemented.');
  }

  /// Stop video recording.
  ///
  /// Args:
  ///   cameraId: Platform camera identifier.
  Future<void> stopVideoRecording(int cameraId) {
    throw UnimplementedError('stopVideoRecording() has not been implemented.');
  }

  /// Get device event stream.
  ///
  /// Returns:
  ///   Stream of [UvcCameraDeviceEvent].
  Stream<UvcCameraDeviceEvent> get deviceEventStream {
    throw UnimplementedError('deviceEventStream has not been implemented.');
  }
}
