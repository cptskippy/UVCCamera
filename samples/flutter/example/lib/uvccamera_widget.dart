import 'dart:async';

import 'package:cross_file/cross_file.dart';
import 'package:flutter/material.dart';
import 'package:permission_handler/permission_handler.dart';
import 'package:uvccamera/uvccamera.dart';

/// Widget that manages the full lifecycle of a single UVC camera session.
///
/// Handles camera and device permission requests, tracks device attach/detach
/// and connection state, initializes a [UvcCameraController], subscribes to its
/// error/status/button event streams, and renders the live preview with picture
/// and video-recording controls.
///
/// Args:
///   device: The [UvcCameraDevice] this widget controls.
class UvcCameraWidget extends StatefulWidget {
  final UvcCameraDevice device;

  const UvcCameraWidget({super.key, required this.device});

  @override
  State<UvcCameraWidget> createState() => _UvcCameraWidgetState();
}

/// State for [UvcCameraWidget].
///
/// Tracks attach/permission/connection flags, the active [UvcCameraController],
/// and subscriptions to the controller's event streams plus the device-event
/// stream.
///
/// State Machine:
///   Detached → Attached (on app resume / first build via _attach)
///   Attached → Detached (on app pause / device detach / dispose)
///   Device Attached + Camera & Device Permissions + Connected
///     → Controller Initializing → Previewing
///
/// Thread Safety:
///   UI state; all mutations happen on the main isolate via [setState].
class _UvcCameraWidgetState extends State<UvcCameraWidget> with WidgetsBindingObserver {
  bool _isAttached = false;
  bool _hasDevicePermission = false;
  bool _hasCameraPermission = false;
  bool _isDeviceAttached = false;
  bool _isDeviceConnected = false;
  UvcCameraController? _cameraController;
  Future<void>? _cameraControllerInitializeFuture;
  StreamSubscription<UvcCameraErrorEvent>? _errorEventSubscription;
  StreamSubscription<UvcCameraStatusEvent>? _statusEventSubscription;
  StreamSubscription<UvcCameraButtonEvent>? _buttonEventSubscription;
  StreamSubscription<UvcCameraDeviceEvent>? _deviceEventSubscription;
  String _log = '';

  /// Register the lifecycle observer and start the attach flow.
  ///
  /// Side Effects:
  ///   - Registers this widget as a [WidgetsBindingObserver].
  ///   - Kicks off [_attach].
  @override
  void initState() {
    super.initState();

    WidgetsBinding.instance.addObserver(this);

    _attach();
  }

  /// Remove the lifecycle observer and tear down the session.
  ///
  /// Side Effects:
  ///   - Unregisters the [WidgetsBindingObserver].
  ///   - Forces a [_detach], releasing the controller and all subscriptions.
  @override
  void dispose() {
    WidgetsBinding.instance.removeObserver(this);

    _detach(force: true);

    super.dispose();
  }

  /// React to app lifecycle changes by attaching on resume and detaching on pause.
  ///
  /// Code Paths:
  ///   1. Resumed → [_attach].
  ///   2. Paused → [_detach].
  ///   3. Other states → no-op.
  @override
  void didChangeAppLifecycleState(AppLifecycleState state) {
    if (state == AppLifecycleState.resumed) {
      _attach();
    } else if (state == AppLifecycleState.paused) {
      _detach();
    }
  }

  /// Establish the session for the target device.
  ///
  /// Verifies the device is present, requests permissions, and — once the device
  /// reports connected — creates and initializes the [UvcCameraController],
  /// subscribing to its error, status, and button streams.
  ///
  /// Args:
  ///   force: When true, re-attach even if already attached (used by
  ///   [dispose] teardown and preview-interruption recovery).
  ///
  /// Side Effects:
  ///   - Subscribes to [UvcCamera.deviceEventStream].
  ///   - Requests camera and device permissions.
  ///   - Creates/initializes [_cameraController] on the connected event.
  ///
  /// Code Paths:
  ///   1. Already attached and not forced → returns immediately.
  ///   2. Device no longer present in [UvcCamera.getDevices] → no-op.
  ///   3. attached event → requests permissions (triggers connection).
  ///   4. connected event → creates controller, initializes, and subscribes to
  ///      error/status/button streams.
  ///   5. previewInterrupted error → detaches and re-attaches to recover.
  ///   6. detached/disconnected events → clears flags and disposes the
  ///      controller.
  void _attach({bool force = false}) {
    if (_isAttached && !force) {
      return;
    }

    UvcCamera.getDevices().then((devices) {
      if (!devices.containsKey(widget.device.name)) {
        return;
      }

      setState(() {
        _isDeviceAttached = true;
      });

      _requestPermissions();
    });

    _deviceEventSubscription = UvcCamera.deviceEventStream.listen((event) {
      if (event.device.name != widget.device.name) {
        return;
      }

      if (event.type == UvcCameraDeviceEventType.attached && !_isDeviceAttached) {
        // NOTE: Requesting UVC device permission will trigger connection request
        _requestPermissions();
      }

      setState(() {
        if (event.type == UvcCameraDeviceEventType.attached) {
          // _hasCameraPermission - maybe
          // _hasDevicePermission - maybe
          _isDeviceAttached = true;
          _isDeviceConnected = false;
        } else if (event.type == UvcCameraDeviceEventType.detached) {
          _hasCameraPermission = false;
          _hasDevicePermission = false;
          _isDeviceAttached = false;
          _isDeviceConnected = false;
        } else if (event.type == UvcCameraDeviceEventType.connected) {
          _hasCameraPermission = true;
          _hasDevicePermission = true;
          _isDeviceAttached = true;
          _isDeviceConnected = true;

          _log = '';

          _cameraController = UvcCameraController(device: widget.device);
          _cameraControllerInitializeFuture = _cameraController!.initialize().then((_) async {
            _errorEventSubscription = _cameraController!.cameraErrorEvents.listen((event) {
              setState(() {
                _log = 'error: ${event.error}\n$_log';
              });

              if (event.error.type == UvcCameraErrorType.previewInterrupted) {
                _detach();
                _attach();
              }
            });

            _statusEventSubscription = _cameraController!.cameraStatusEvents.listen((event) {
              setState(() {
                _log = 'status: ${event.payload}\n$_log';
              });
            });

            _buttonEventSubscription = _cameraController!.cameraButtonEvents.listen((event) {
              setState(() {
                _log = 'btn(${event.button}): ${event.state}\n$_log';
              });
            });
          });
        } else if (event.type == UvcCameraDeviceEventType.disconnected) {
          _hasCameraPermission = false;
          _hasDevicePermission = false;
          // _isDeviceAttached - maybe?
          _isDeviceConnected = false;

          _buttonEventSubscription?.cancel();
          _buttonEventSubscription = null;

          _statusEventSubscription?.cancel();
          _statusEventSubscription = null;

          _errorEventSubscription?.cancel();
          _errorEventSubscription = null;

          _cameraController?.dispose();
          _cameraController = null;
          _cameraControllerInitializeFuture = null;

          _log = '';
        }
      });
    });

    _isAttached = true;
  }

  /// Tear down the session and release all resources.
  ///
  /// Args:
  ///   force: When true, detach even if not currently attached (used by
  ///   [dispose]).
  ///
  /// Side Effects:
  ///   - Clears permission/connection flags.
  ///   - Cancels button/status/error and device-event subscriptions.
  ///   - Disposes [_cameraController] and clears the initialize future.
  void _detach({bool force = false}) {
    if (!_isAttached && !force) {
      return;
    }

    _hasDevicePermission = false;
    _hasCameraPermission = false;
    _isDeviceAttached = false;
    _isDeviceConnected = false;

    _buttonEventSubscription?.cancel();
    _buttonEventSubscription = null;

    _statusEventSubscription?.cancel();
    _statusEventSubscription = null;

    _cameraController?.dispose();
    _cameraController = null;
    _cameraControllerInitializeFuture = null;

    _deviceEventSubscription?.cancel();
    _deviceEventSubscription = null;

    _isAttached = false;
  }

  /// Request the camera permission, then the UVC device permission.
  ///
  /// Device permission can only be requested after camera permission is granted.
  ///
  /// Code Paths:
  ///   1. Camera permission granted → proceed to request device permission.
  ///   2. Camera permission denied → stop (device permission not requested).
  ///
  /// Side Effects:
  ///   - Updates [_hasCameraPermission] and [_hasDevicePermission] via [setState].
  Future<void> _requestPermissions() async {
    final hasCameraPermission = await _requestCameraPermission().then((value) {
      setState(() {
        _hasCameraPermission = value;
      });

      return value;
    });

    // NOTE: Requesting UVC device permission can be made only after camera permission is granted
    if (!hasCameraPermission) {
      return;
    }

    _requestDevicePermission().then((value) {
      setState(() {
        _hasDevicePermission = value;
      });

      return value;
    });
  }

  /// Request permission to access the target UVC device.
  ///
  /// Returns:
  ///   True if device permission is granted.
  Future<bool> _requestDevicePermission() async {
    final devicePermissionStatus = await UvcCamera.requestDevicePermission(widget.device);
    return devicePermissionStatus;
  }

  /// Request the Android camera permission via the permission handler.
  ///
  /// Returns:
  ///   True if the camera permission is granted.
  ///
  /// Code Paths:
  ///   1. Already granted → returns true.
  ///   2. Denied/restricted → prompts the user and returns the granted result.
  ///   3. Permanently denied → returns false.
  Future<bool> _requestCameraPermission() async {
    var cameraPermissionStatus = await Permission.camera.status;
    if (cameraPermissionStatus.isGranted) {
      return true;
    } else if (cameraPermissionStatus.isDenied || cameraPermissionStatus.isRestricted) {
      cameraPermissionStatus = await Permission.camera.request();
      return cameraPermissionStatus.isGranted;
    } else {
      // NOTE: Permission is permanently denied
      return false;
    }
  }

  /// Start video recording with the given mode via the active controller.
  ///
  /// Args:
  ///   videoRecordingMode: The [UvcCameraMode] to record in.
  Future<void> _startVideoRecording(UvcCameraMode videoRecordingMode) async {
    await _cameraController!.startVideoRecording(videoRecordingMode);
  }

  /// Capture a still image and log the resulting file path and size.
  ///
  /// Side Effects:
  ///   - Appends an "image file" line to [_log].
  Future<void> _takePicture() async {
    final XFile outputFile = await _cameraController!.takePicture();

    outputFile.length().then((length) {
      setState(() {
        _log = 'image file: ${outputFile.path} ($length bytes)\n$_log';
      });
    });
  }

  /// Stop video recording and log the resulting file path and size.
  ///
  /// Side Effects:
  ///   - Appends a "video file" line to [_log].
  Future<void> _stopVideoRecording() async {
    final XFile outputFile = await _cameraController!.stopVideoRecording();

    outputFile.length().then((length) {
      setState(() {
        _log = 'video file: ${outputFile.path} ($length bytes)\n$_log';
      });
    });
  }

  /// Build the screen for the target device.
  ///
  /// Returns:
  ///   A status message while a precondition is unmet, otherwise the preview
  ///   overlaid with the event log and the capture/recording controls.
  ///
  /// Code Paths:
  ///   1. Device not attached → "not attached" message.
  ///   2. Camera permission missing → "camera permission" message.
  ///   3. Device permission missing → "device permission" message.
  ///   4. Device not connected → "not connected" message.
  ///   5. Controller still initializing → progress indicator.
  ///   6. Ready → [UvcCameraPreview] with log overlay and picture/recording
  ///      buttons.
  @override
  Widget build(BuildContext context) {
    if (!_isDeviceAttached) {
      return Center(child: Text('Device is not attached', style: TextStyle(fontSize: 18)));
    }

    if (!_hasCameraPermission) {
      return Center(child: Text('Camera permission is not granted', style: TextStyle(fontSize: 18)));
    }

    if (!_hasDevicePermission) {
      return Center(child: Text('Device permission is not granted', style: TextStyle(fontSize: 18)));
    }

    if (!_isDeviceConnected) {
      return Center(child: Text('Device is not connected', style: TextStyle(fontSize: 18)));
    }

    return FutureBuilder<void>(
      future: _cameraControllerInitializeFuture,
      builder: (context, snapshot) {
        if (snapshot.connectionState == ConnectionState.done) {
          return Stack(
            children: [
              Align(
                alignment: Alignment.topCenter,
                child: UvcCameraPreview(
                  _cameraController!,
                  child: Padding(
                    padding: const EdgeInsets.all(8.0),
                    child: SingleChildScrollView(
                      child: SelectableText(
                        _log,
                        style: TextStyle(color: Colors.red, fontFamily: 'Courier', fontSize: 10.0),
                      ),
                    ),
                  ),
                ),
              ),
              Align(
                alignment: Alignment.bottomCenter,
                child: Padding(
                  padding: const EdgeInsets.only(bottom: 50.0),
                  child: ValueListenableBuilder<UvcCameraControllerState>(
                    valueListenable: _cameraController!,
                    builder: (context, value, child) {
                      return Row(
                        mainAxisAlignment: MainAxisAlignment.spaceEvenly,
                        children: [
                          FloatingActionButton(
                            backgroundColor: Colors.white,
                            onPressed:
                                value.isTakingPicture
                                    ? null
                                    : () async {
                                      await _takePicture();
                                    },
                            child: Icon(Icons.camera_alt, color: Colors.black),
                          ),
                          FloatingActionButton(
                            backgroundColor: value.isRecordingVideo ? Colors.red : Colors.white,
                            onPressed: () async {
                              if (value.isRecordingVideo) {
                                await _stopVideoRecording();
                              } else {
                                await _startVideoRecording(value.previewMode!);
                              }
                            },
                            child: Icon(
                              value.isRecordingVideo ? Icons.stop : Icons.videocam,
                              color: value.isRecordingVideo ? Colors.white : Colors.black,
                            ),
                          ),
                        ],
                      );
                    },
                  ),
                ),
              ),
            ],
          );
        } else {
          return const Center(child: CircularProgressIndicator());
        }
      },
    );
  }
}
