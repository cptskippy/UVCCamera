import 'dart:async';

import 'package:cross_file/cross_file.dart';
import 'package:flutter/widgets.dart';

import 'uvccamera_button_event.dart';
import 'uvccamera_controller_disposed_exception.dart';
import 'uvccamera_controller_illegal_state_exception.dart';
import 'uvccamera_controller_initialized_exception.dart';
import 'uvccamera_controller_not_initialized_exception.dart';
import 'uvccamera_controller_state.dart';
import 'uvccamera_device.dart';
import 'uvccamera_error_event.dart';
import 'uvccamera_mode.dart';
import 'uvccamera_platform_interface.dart';
import 'uvccamera_resolution_preset.dart';
import 'uvccamera_status_event.dart';

/// Controls a connected [UvcCameraDevice] and manages its lifecycle from initialization to disposal.
///
/// The controller wraps platform interactions for opening the camera, attaching event streams,
/// capturing pictures, and recording video. It exposes state via [UvcCameraControllerState] and
/// notifies listeners when state changes.
///
/// Lifecycle:
///   Uninitialized → Initializing → Initialized → Disposed
///   Initialized may enter Recording or TakingPicture sub-states.
///
/// State Machine:
///   Uninitialized → Initializing (on initialize) → Initialized
///   Initialized → Disposed (on dispose)
///   Initialized → Recording → Initialized
///   Initialized → TakingPicture → Initialized
///   Error transitions from any state on platform failures.
///
/// Thread Safety:
///   Must be used on the main isolate. Platform calls are forwarded to the native thread.
///   State mutations happen on the main isolate via [ValueNotifier]. Not safe for concurrent initialize/dispose.
///
/// Properties:
///   device: The UVC device controlled by this controller. Immutable after construction.
///   resolutionPreset: Requested resolution preset for camera open. Immutable after construction.
///   cameraId: Platform camera identifier assigned after successful initialization.
///   textureId: OpenGL texture identifier for preview rendering.
///
/// Usage:
///   ```dart
///   final controller = UvcCameraController(device: device);
///   await controller.initialize();
///   final preview = controller.buildPreview();
///   ```
class UvcCameraController extends ValueNotifier<UvcCameraControllerState> {
  /// The camera device controlled by this controller.
  final UvcCameraDevice device;

  /// The resolution preset requested for the camera.
  final UvcCameraResolutionPreset resolutionPreset;

  bool _isDisposed = false;
  Future<void>? _initializeFuture;

  /// Camera ID
  int? _cameraId;

  /// Texture ID
  int? _textureId;

  /// Stream of camera error events.
  Stream<UvcCameraErrorEvent>? _cameraErrorEventStream;

  /// Stream of camera status events.
  Stream<UvcCameraStatusEvent>? _cameraStatusEventStream;

  /// Stream of camera button events.
  Stream<UvcCameraButtonEvent>? _cameraButtonEventStream;

  /// Create a new [UvcCameraController] instance for the given device.
  ///
  /// Args:
  ///   device: The [UvcCameraDevice] to control. Must be a valid UVC device discovered via [UvcCamera.getDevices].
  ///   resolutionPreset: Desired resolution preset for camera open. Defaults to [UvcCameraResolutionPreset.max].
  ///
  /// The controller starts in the Uninitialized state. Call [initialize] to open the device.
  UvcCameraController({required this.device, this.resolutionPreset = UvcCameraResolutionPreset.max})
    : super(UvcCameraControllerState.uninitialized(device));

  /// Initialize the controller on the device.
  ///
  /// Opens the camera, obtains texture ID, attaches event streams, and transitions state to Initialized.
  ///
  /// Throws:
  ///   [UvcCameraControllerInitializedException]: If initialize has already been called.
  ///   [UvcCameraControllerDisposedException]: If the controller has been disposed.
  ///   PlatformException: If the native platform fails to open the camera.
  ///
  /// Side Effects:
  ///   - Opens the camera via platform interface.
  ///   - Attaches error, status, and button event streams.
  ///   - Updates [value] to reflect initialized state and preview mode.
  ///
  /// Code Paths:
  ///   1. If already initializing → throws [UvcCameraControllerInitializedException].
  ///   2. If disposed → throws [UvcCameraControllerDisposedException].
  ///   3. On success → sets cameraId, textureId, attaches streams, updates state to Initialized.
  ///   4. On error → completes future with error, state remains Uninitialized.
  Future<void> initialize() => _initialize(device);

  /// Initializes the controller on the specified device.
  Future<void> _initialize(UvcCameraDevice device) async {
    if (_initializeFuture != null) {
      throw UvcCameraControllerInitializedException();
    }
    if (_isDisposed) {
      throw UvcCameraControllerDisposedException();
    }

    final Completer<void> initializeCompleter = Completer<void>();
    _initializeFuture = initializeCompleter.future;

    try {
      _cameraId = await UvcCameraPlatformInterface.instance.openCamera(device, resolutionPreset);

      _textureId = await UvcCameraPlatformInterface.instance.getCameraTextureId(_cameraId!);
      final previewMode = await UvcCameraPlatformInterface.instance.getPreviewMode(_cameraId!);

      _cameraErrorEventStream = await UvcCameraPlatformInterface.instance.attachToCameraErrorCallback(_cameraId!);
      _cameraStatusEventStream = await UvcCameraPlatformInterface.instance.attachToCameraStatusCallback(_cameraId!);
      _cameraButtonEventStream = await UvcCameraPlatformInterface.instance.attachToCameraButtonCallback(_cameraId!);

      value = value.copyWith(isInitialized: true, device: device, previewMode: previewMode);

      initializeCompleter.complete();
    } catch (e) {
      initializeCompleter.completeError(e);
    }
  }

  /// Dispose the controller and release all platform resources.
  ///
  /// Closes the camera, detaches event streams, and marks the controller as disposed.
  ///
  /// Side Effects:
  ///   - Closes the camera via platform interface.
  ///   - Detaches error, status, and button callbacks.
  ///   - Clears cameraId and textureId.
  ///
  /// Code Paths:
  ///   1. If already disposed → returns immediately.
  ///   2. If initializing → awaits initialization completion before cleanup.
  ///   3. On success → clears streams, closes camera, notifies listeners.
  ///
  /// Throws:
  ///   PlatformException: If native close fails.
  @override
  Future<void> dispose() async {
    if (_isDisposed) {
      return;
    }
    super.dispose();

    _isDisposed = true;

    if (_initializeFuture != null) {
      await _initializeFuture;
      _initializeFuture = null;
    }

    if (_cameraButtonEventStream != null) {
      if (_cameraId != null) {
        await UvcCameraPlatformInterface.instance.detachFromCameraButtonCallback(_cameraId!);
      }
      _cameraButtonEventStream = null;
    }

    if (_cameraStatusEventStream != null) {
      if (_cameraId != null) {
        await UvcCameraPlatformInterface.instance.detachFromCameraStatusCallback(_cameraId!);
      }
      _cameraStatusEventStream = null;
    }

    if (_cameraErrorEventStream != null) {
      if (_cameraId != null) {
        await UvcCameraPlatformInterface.instance.detachFromCameraErrorCallback(_cameraId!);
      }
      _cameraErrorEventStream = null;
    }

    _textureId = null;

    if (_cameraId != null) {
      await UvcCameraPlatformInterface.instance.closeCamera(_cameraId!);
      _cameraId = null;
    }
  }

  /// Get the platform camera identifier.
  ///
  /// Returns:
  ///   The integer camera ID assigned by the platform after successful initialization.
  ///
  /// Throws:
  ///   [UvcCameraControllerDisposedException]: If the controller is disposed.
  ///   [UvcCameraControllerNotInitializedException]: If the controller is not initialized.
  int get cameraId {
    _ensureInitializedNotDisposed();
    return _cameraId!;
  }

  /// Get the OpenGL texture identifier for preview rendering.
  ///
  /// Returns:
  ///   The texture ID used by [buildPreview] to render the camera feed.
  ///
  /// Throws:
  ///   [UvcCameraControllerDisposedException]: If the controller is disposed.
  ///   [UvcCameraControllerNotInitializedException]: If the controller is not initialized.
  int get textureId {
    _ensureInitializedNotDisposed();
    return _textureId!;
  }

  /// Get a stream of camera error events.
  ///
  /// Returns:
  ///   A broadcast stream of [UvcCameraErrorEvent] emitted by the platform when errors occur.
  ///
  /// Throws:
  ///   [UvcCameraControllerDisposedException]: If the controller is disposed.
  ///   [UvcCameraControllerNotInitializedException]: If the controller is not initialized.
  Stream<UvcCameraErrorEvent> get cameraErrorEvents {
    _ensureInitializedNotDisposed();
    return _cameraErrorEventStream!;
  }

  /// Get a stream of camera status events.
  ///
  /// Returns:
  ///   A broadcast stream of [UvcCameraStatusEvent] emitted by the platform for status changes.
  ///
  /// Throws:
  ///   [UvcCameraControllerDisposedException]: If the controller is disposed.
  ///   [UvcCameraControllerNotInitializedException]: If the controller is not initialized.
  Stream<UvcCameraStatusEvent> get cameraStatusEvents {
    _ensureInitializedNotDisposed();
    return _cameraStatusEventStream!;
  }

  /// Get a stream of camera button events.
  ///
  /// Returns:
  ///   A broadcast stream of [UvcCameraButtonEvent] emitted when physical buttons on the device change state.
  ///
  /// Throws:
  ///   [UvcCameraControllerDisposedException]: If the controller is disposed.
  ///   [UvcCameraControllerNotInitializedException]: If the controller is not initialized.
  Stream<UvcCameraButtonEvent> get cameraButtonEvents {
    _ensureInitializedNotDisposed();
    return _cameraButtonEventStream!;
  }

  /// Take a picture using the current camera configuration.
  ///
  /// Captures a single image from the camera and returns the file.
  ///
  /// Returns:
  ///   An [XFile] referencing the captured image file on the device.
  ///
  /// Throws:
  ///   [UvcCameraControllerDisposedException]: If the controller is disposed.
  ///   [UvcCameraControllerNotInitializedException]: If the controller is not initialized.
  ///   [UvcCameraControllerIllegalStateException]: If a picture is already being taken.
  ///   PlatformException: If the native capture fails.
  ///
  /// Side Effects:
  ///   - Sets [UvcCameraControllerState.isTakingPicture] to true during capture.
  ///   - Updates state back to false after completion.
  ///
  /// Code Paths:
  ///   1. If already taking picture → throws IllegalStateException.
  ///   2. On success → returns XFile and resets isTakingPicture.
  ///   3. On error → resets isTakingPicture and rethrows.
  Future<XFile> takePicture() async {
    _ensureInitializedNotDisposed();

    if (value.isTakingPicture) {
      throw UvcCameraControllerIllegalStateException('UvcCameraController is already taking a picture');
    }

    value = value.copyWith(isTakingPicture: true);
    try {
      final XFile pictureFile = await UvcCameraPlatformInterface.instance.takePicture(_cameraId!);
      return pictureFile;
    } catch (e) {
      rethrow;
    } finally {
      value = value.copyWith(isTakingPicture: false);
    }
  }

  /// Start video recording with the specified mode.
  ///
  /// Args:
  ///   videoRecordingMode: The [UvcCameraMode] to use for recording. Must be supported by the device.
  ///
  /// Throws:
  ///   [UvcCameraControllerDisposedException]: If the controller is disposed.
  ///   [UvcCameraControllerNotInitializedException]: If the controller is not initialized.
  ///   [UvcCameraControllerIllegalStateException]: If video recording is already in progress.
  ///   PlatformException: If the native start fails.
  ///
  /// Side Effects:
  ///   - Sets [UvcCameraControllerState.isRecordingVideo] to true.
  ///   - Stores videoRecordingMode and videoRecordingFile in state.
  ///
  /// Code Paths:
  ///   1. If already recording → throws IllegalStateException.
  ///   2. On success → updates state with file path from platform.
  ///   3. On error → resets recording state and rethrows.
  Future<void> startVideoRecording(UvcCameraMode videoRecordingMode) async {
    _ensureInitializedNotDisposed();

    if (value.isRecordingVideo) {
      throw UvcCameraControllerIllegalStateException('UvcCameraController is already recording video');
    }

    value = value.copyWith(isRecordingVideo: true, videoRecordingMode: videoRecordingMode, videoRecordingFile: null);
    try {
      final XFile videoRecordingFile = await UvcCameraPlatformInterface.instance.startVideoRecording(
        _cameraId!,
        videoRecordingMode,
      );
      value = value.copyWith(videoRecordingFile: videoRecordingFile);
    } catch (e) {
      value = value.copyWith(isRecordingVideo: false, videoRecordingMode: null, videoRecordingFile: null);
      rethrow;
    }
  }

  /// Stop video recording and return the recorded file.
  ///
  /// Returns:
  ///   The [XFile] referencing the video file created during recording.
  ///
  /// Throws:
  ///   [UvcCameraControllerDisposedException]: If the controller is disposed.
  ///   [UvcCameraControllerNotInitializedException]: If the controller is not initialized.
  ///   [UvcCameraControllerIllegalStateException]: If video recording is not in progress.
  ///   PlatformException: If the native stop fails.
  ///
  /// Side Effects:
  ///   - Resets recording state flags.
  ///   - Clears videoRecordingMode and videoRecordingFile.
  ///
  /// Code Paths:
  ///   1. If not recording → throws IllegalStateException.
  ///   2. On success → returns file and resets state.
  ///   3. On error → resets state and rethrows.
  Future<XFile> stopVideoRecording() async {
    _ensureInitializedNotDisposed();

    if (!value.isRecordingVideo) {
      throw UvcCameraControllerIllegalStateException('UvcCameraController is not recording video');
    }

    try {
      await UvcCameraPlatformInterface.instance.stopVideoRecording(_cameraId!);

      final XFile videoRecordingFile = value.videoRecordingFile!;

      return videoRecordingFile;
    } catch (e) {
      rethrow;
    } finally {
      value = value.copyWith(isRecordingVideo: false, videoRecordingMode: null, videoRecordingFile: null);
    }
  }

  /// Build a widget showing a live camera preview.
  ///
  /// Returns:
  ///   A [Texture] widget rendering the camera feed using the platform texture ID.
  ///
  /// Throws:
  ///   [UvcCameraControllerDisposedException]: If the controller is disposed.
  ///   [UvcCameraControllerNotInitializedException]: If the controller is not initialized.
  Widget buildPreview() {
    _ensureInitializedNotDisposed();

    return Texture(textureId: _textureId!);
  }

  /// Ensures that the controller is initialized and not disposed.
  void _ensureInitializedNotDisposed() {
    if (_isDisposed) {
      throw UvcCameraControllerDisposedException();
    }
    if (_initializeFuture == null) {
      throw UvcCameraControllerNotInitializedException();
    }
  }
}
