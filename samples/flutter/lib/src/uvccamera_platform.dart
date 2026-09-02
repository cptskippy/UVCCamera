import 'package:flutter/services.dart';
import 'package:cross_file/cross_file.dart';

import 'uvccamera_button_event.dart';
import 'uvccamera_device.dart';
import 'uvccamera_device_event.dart';
import 'uvccamera_error_event.dart';
import 'uvccamera_mode.dart';
import 'uvccamera_platform_interface.dart';
import 'uvccamera_resolution_preset.dart';
import 'uvccamera_status_event.dart';

/// Default platform implementation for UVC camera using MethodChannel and EventChannel.
///
/// Handles communication with native Android implementation.
///
/// Thread Safety:
///   Methods may be called from main isolate. Channels handle threading internally.
class UvcCameraPlatform extends UvcCameraPlatformInterface {
  final _nativeMethodChannel = const MethodChannel('uvccamera/native');

  final EventChannel _deviceEventChannel = EventChannel('uvccamera/device_events');
  Stream<UvcCameraDeviceEvent>? _deviceEventStream;

  final Map<int, EventChannel> _errorEventChannels = {};
  final Map<int, Stream<UvcCameraErrorEvent>> _errorEventStreams = {};

  final Map<int, EventChannel> _statusEventChannels = {};
  final Map<int, Stream<UvcCameraStatusEvent>> _statusEventStreams = {};

  final Map<int, EventChannel> _buttonEventChannels = {};
  final Map<int, Stream<UvcCameraButtonEvent>> _buttonEventStreams = {};

  /// Query whether the platform supports UVC camera access.
  ///
  /// Returns:
  ///   true when the native platform reports UVC support.
  ///
  /// Throws:
  ///   [PlatformException] if the native call returns no result.
  @override
  Future<bool> isSupported() async {
    final result = await _nativeMethodChannel.invokeMethod<bool>('isSupported');
    if (result == null) {
      throw PlatformException(code: 'UNKNOWN', message: 'Unable to determine if UVC camera is supported');
    }
    return result;
  }

  /// Enumerate the UVC devices currently visible to the platform.
  ///
  /// Returns:
  ///   A map of device name to [UvcCameraDevice].
  ///
  /// Throws:
  ///   [PlatformException] if the native call returns no result.
  @override
  Future<Map<String, UvcCameraDevice>> getDevices() async {
    final result = _nativeMethodChannel.invokeMethod<Map>('getDevices');
    return result.then((value) {
      if (value == null) {
        throw PlatformException(code: 'UNKNOWN', message: 'Unable to get UVC camera devices');
      }
      return value.map((key, value) {
        return MapEntry(key, UvcCameraDevice.fromMap(value));
      });
    });
  }

  /// Request USB device permission for [device].
  ///
  /// Args:
  ///   device: The device whose USB permission should be requested.
  ///
  /// Returns:
  ///   true when permission is granted.
  ///
  /// Throws:
  ///   [PlatformException] if the native call returns no result.
  @override
  Future<bool> requestDevicePermission(UvcCameraDevice device) async {
    final result = await _nativeMethodChannel.invokeMethod<bool>('requestDevicePermission', {
      'deviceName': device.name,
    });
    if (result == null) {
      throw PlatformException(code: 'UNKNOWN', message: 'Unable to request device permission for device: $device');
    }
    return result;
  }

  /// Open [device] using [resolutionPreset].
  ///
  /// Args:
  ///   device: The UVC device to open.
  ///   resolutionPreset: The requested resolution preset.
  ///
  /// Returns:
  ///   The native camera identifier for the opened camera.
  ///
  /// Throws:
  ///   [PlatformException] if the native call returns no result.
  @override
  Future<int> openCamera(UvcCameraDevice device, UvcCameraResolutionPreset resolutionPreset) async {
    final result = await _nativeMethodChannel.invokeMethod<int>('openCamera', {
      'deviceName': device.name,
      'resolutionPreset': resolutionPreset.name,
    });
    if (result == null) {
      throw PlatformException(code: 'UNKNOWN', message: 'Unable to open camera for device: $device');
    }
    return result;
  }

  /// Close the camera identified by [cameraId].
  ///
  /// Args:
  ///   cameraId: Native camera identifier returned by [openCamera].
  ///
  /// Side Effects:
  ///   - Removes the cached status and button event channels for [cameraId].
  ///
  /// Throws:
  ///   [PlatformException] if the native close call fails.
  @override
  Future<void> closeCamera(int cameraId) async {
    _statusEventChannels.remove(cameraId);
    _statusEventStreams.remove(cameraId);

    _buttonEventChannels.remove(cameraId);
    _buttonEventStreams.remove(cameraId);

    await _nativeMethodChannel.invokeMethod<void>('closeCamera', {'cameraId': cameraId});
  }

  /// Get the platform texture id used to render the camera preview.
  ///
  /// Args:
  ///   cameraId: Native camera identifier returned by [openCamera].
  ///
  /// Returns:
  ///   The texture id for the camera preview surface.
  ///
  /// Throws:
  ///   [PlatformException] if the native call returns no result.
  @override
  Future<int> getCameraTextureId(int cameraId) async {
    final result = await _nativeMethodChannel.invokeMethod<int>('getCameraTextureId', {'cameraId': cameraId});
    if (result == null) {
      throw PlatformException(code: 'UNKNOWN', message: 'Unable to get camera texture id for camera: $cameraId');
    }
    return result;
  }

  /// Attach to the error-event stream for [cameraId].
  ///
  /// Args:
  ///   cameraId: Native camera identifier returned by [openCamera].
  ///
  /// Returns:
  ///   A broadcast stream of [UvcCameraErrorEvent] values.
  ///
  /// Throws:
  ///   [PlatformException] if the native attach call fails.
  @override
  Future<Stream<UvcCameraErrorEvent>> attachToCameraErrorCallback(int cameraId) async {
    final errorEventChannel = EventChannel('uvccamera/camera@$cameraId/error_events');
    final errorEventStream = errorEventChannel.receiveBroadcastStream().map((event) {
      return UvcCameraErrorEvent.fromMap(event);
    });

    await _nativeMethodChannel.invokeMethod<void>('attachToCameraErrorCallback', {'cameraId': cameraId});

    _errorEventChannels[cameraId] = errorEventChannel;
    _errorEventStreams[cameraId] = errorEventStream;

    return errorEventStream;
  }

  /// Detach from the error-event stream for [cameraId].
  ///
  /// Args:
  ///   cameraId: Native camera identifier returned by [openCamera].
  ///
  /// Side Effects:
  ///   - Removes the cached error event channel and stream for [cameraId].
  ///
  /// Throws:
  ///   [PlatformException] if the native detach call fails.
  @override
  Future<void> detachFromCameraErrorCallback(int cameraId) async {
    await _nativeMethodChannel.invokeMethod<void>('detachFromCameraErrorCallback', {'cameraId': cameraId});

    _errorEventChannels.remove(cameraId);
    _errorEventStreams.remove(cameraId);
  }

  /// Attach to the status-event stream for [cameraId].
  ///
  /// Args:
  ///   cameraId: Native camera identifier returned by [openCamera].
  ///
  /// Returns:
  ///   A broadcast stream of [UvcCameraStatusEvent] values.
  ///
  /// Throws:
  ///   [PlatformException] if the native attach call fails.
  @override
  Future<Stream<UvcCameraStatusEvent>> attachToCameraStatusCallback(int cameraId) async {
    final statusEventChannel = EventChannel('uvccamera/camera@$cameraId/status_events');
    final statusEventStream = statusEventChannel.receiveBroadcastStream().map((event) {
      return UvcCameraStatusEvent.fromMap(event);
    });

    await _nativeMethodChannel.invokeMethod<void>('attachToCameraStatusCallback', {'cameraId': cameraId});

    _statusEventChannels[cameraId] = statusEventChannel;
    _statusEventStreams[cameraId] = statusEventStream;

    return statusEventStream;
  }

  /// Detach from the status-event stream for [cameraId].
  ///
  /// Args:
  ///   cameraId: Native camera identifier returned by [openCamera].
  ///
  /// Side Effects:
  ///   - Removes the cached status event channel and stream for [cameraId].
  ///
  /// Throws:
  ///   [PlatformException] if the native detach call fails.
  @override
  Future<void> detachFromCameraStatusCallback(int cameraId) async {
    await _nativeMethodChannel.invokeMethod<void>('detachFromCameraStatusCallback', {'cameraId': cameraId});

    _statusEventChannels.remove(cameraId);
    _statusEventStreams.remove(cameraId);
  }

  /// Attach to the button-event stream for [cameraId].
  ///
  /// Args:
  ///   cameraId: Native camera identifier returned by [openCamera].
  ///
  /// Returns:
  ///   A broadcast stream of [UvcCameraButtonEvent] values.
  ///
  /// Throws:
  ///   [PlatformException] if the native attach call fails.
  @override
  Future<Stream<UvcCameraButtonEvent>> attachToCameraButtonCallback(int cameraId) async {
    final buttonEventChannel = EventChannel('uvccamera/camera@$cameraId/button_events');
    final buttonEventStream = buttonEventChannel.receiveBroadcastStream().map((event) {
      return UvcCameraButtonEvent.fromMap(event);
    });

    await _nativeMethodChannel.invokeMethod<void>('attachToCameraButtonCallback', {'cameraId': cameraId});

    _buttonEventChannels[cameraId] = buttonEventChannel;
    _buttonEventStreams[cameraId] = buttonEventStream;

    return buttonEventStream;
  }

  /// Detach from the button-event stream for [cameraId].
  ///
  /// Args:
  ///   cameraId: Native camera identifier returned by [openCamera].
  ///
  /// Side Effects:
  ///   - Removes the cached button event channel and stream for [cameraId].
  ///
  /// Throws:
  ///   [PlatformException] if the native detach call fails.
  @override
  Future<void> detachFromCameraButtonCallback(int cameraId) async {
    await _nativeMethodChannel.invokeMethod<void>('detachFromCameraButtonCallback', {'cameraId': cameraId});

    _buttonEventChannels.remove(cameraId);
    _buttonEventStreams.remove(cameraId);
  }

  /// Get the preview and recording modes supported by [cameraId].
  ///
  /// Args:
  ///   cameraId: Native camera identifier returned by [openCamera].
  ///
  /// Returns:
  ///   The list of supported [UvcCameraMode] values.
  ///
  /// Throws:
  ///   [PlatformException] if the native call returns no result.
  @override
  Future<List<UvcCameraMode>> getSupportedModes(int cameraId) async {
    final result = await _nativeMethodChannel.invokeMethod<List>('getSupportedModes', {'cameraId': cameraId});
    if (result == null) {
      throw PlatformException(code: 'UNKNOWN', message: 'Unable to get supported modes for camera: $cameraId');
    }
    return result.map((value) {
      return UvcCameraMode.fromMap(value);
    }).toList();
  }

  /// Get the currently active preview mode for [cameraId].
  ///
  /// Args:
  ///   cameraId: Native camera identifier returned by [openCamera].
  ///
  /// Returns:
  ///   The active [UvcCameraMode].
  ///
  /// Throws:
  ///   [PlatformException] if the native call returns no result.
  @override
  Future<UvcCameraMode> getPreviewMode(int cameraId) async {
    final result = await _nativeMethodChannel.invokeMethod<Map>('getPreviewMode', {'cameraId': cameraId});
    if (result == null) {
      throw PlatformException(code: 'UNKNOWN', message: 'Unable to get preview mode for camera: $cameraId');
    }
    return UvcCameraMode.fromMap(result);
  }

  /// Set the preview mode for [cameraId].
  ///
  /// Args:
  ///   cameraId: Native camera identifier returned by [openCamera].
  ///   previewMode: The mode to apply.
  ///
  /// Throws:
  ///   [PlatformException] if the native call fails.
  @override
  Future<void> setPreviewMode(int cameraId, UvcCameraMode previewMode) async {
    await _nativeMethodChannel.invokeMethod<void>('setPreviewMode', {
      'cameraId': cameraId,
      'previewMode': previewMode.toMap(),
    });
  }

  /// Capture a still image from [cameraId].
  ///
  /// Args:
  ///   cameraId: Native camera identifier returned by [openCamera].
  ///
  /// Returns:
  ///   An [XFile] pointing to the captured image.
  ///
  /// Throws:
  ///   [PlatformException] if capture fails or the native call returns no path.
  @override
  Future<XFile> takePicture(int cameraId) async {
    final result = await _nativeMethodChannel.invokeMethod<String>('takePicture', {'cameraId': cameraId});

    if (result == null) {
      throw PlatformException(code: 'UNKNOWN', message: 'Unable to take picture for camera: $cameraId');
    }

    return XFile(result);
  }

  /// Start recording video from [cameraId] using [videoRecordingMode].
  ///
  /// Args:
  ///   cameraId: Native camera identifier returned by [openCamera].
  ///   videoRecordingMode: The recording mode to use.
  ///
  /// Returns:
  ///   An [XFile] describing the video file being written.
  ///
  /// Throws:
  ///   [PlatformException] if recording fails or the native call returns no path.
  @override
  Future<XFile> startVideoRecording(int cameraId, UvcCameraMode videoRecordingMode) async {
    final result = await _nativeMethodChannel.invokeMethod<String>('startVideoRecording', {
      'cameraId': cameraId,
      'videoRecordingMode': videoRecordingMode.toMap(),
    });

    if (result == null) {
      throw PlatformException(code: 'UNKNOWN', message: 'Unable to start video recording for camera: $cameraId');
    }

    return XFile(result);
  }

  /// Stop the active video recording for [cameraId].
  ///
  /// Args:
  ///   cameraId: Native camera identifier returned by [openCamera].
  ///
  /// Throws:
  ///   [PlatformException] if the native stop call fails.
  @override
  Future<void> stopVideoRecording(int cameraId) async {
    await _nativeMethodChannel.invokeMethod<void>('stopVideoRecording', {'cameraId': cameraId});
  }

  /// Stream of device attach and detach events.
  ///
  /// Returns:
  ///   A lazily created broadcast stream of [UvcCameraDeviceEvent] values.
  @override
  Stream<UvcCameraDeviceEvent> get deviceEventStream {
    return _deviceEventStream ??= _deviceEventChannel.receiveBroadcastStream().map((event) {
      return UvcCameraDeviceEvent.fromMap(event);
    });
  }
}
