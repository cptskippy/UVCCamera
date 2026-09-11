/// UVC camera plugin for Flutter.
///
/// Provides a platform-agnostic API to discover, open, preview, and capture from
/// USB Video Class (UVC) cameras. The plugin wraps the native Android UVC stack
/// behind a small Dart API:
///
/// - [UvcCamera] — static helpers for device discovery and permissions.
/// - [UvcCameraController] — manages a single camera's lifecycle: open, preview,
///   capture, record, and close.
/// - [UvcCameraDevice] and the `*Event`/`*Error`/`*Status` types — immutable data
///   models for devices and attach/detach, error, status, and button events.
///
/// Typical usage:
///   ```dart
///   final devices = await UvcCamera.getDevices();
///   await UvcCamera.requestDevicePermission(devices['device name']);
///
///   final controller = UvcCameraController(device: devices['device name']);
///   await controller.initialize();
///   final preview = controller.buildPreview();
///   ```
library;

export 'src/uvccamera.dart' show UvcCamera;
export 'src/uvccamera_button_event.dart' show UvcCameraButtonEvent;
export 'src/uvccamera_controller.dart' show UvcCameraController;
export 'src/uvccamera_controller_disposed_exception.dart' show UvcCameraControllerDisposedException;
export 'src/uvccamera_controller_illegal_state_exception.dart' show UvcCameraControllerIllegalStateException;
export 'src/uvccamera_controller_initialized_exception.dart' show UvcCameraControllerInitializedException;
export 'src/uvccamera_controller_not_initialized_exception.dart' show UvcCameraControllerNotInitializedException;
export 'src/uvccamera_controller_state.dart' show UvcCameraControllerState;
export 'src/uvccamera_device.dart' show UvcCameraDevice;
export 'src/uvccamera_device_event.dart' show UvcCameraDeviceEvent;
export 'src/uvccamera_device_event_type.dart' show UvcCameraDeviceEventType;
export 'src/uvccamera_error.dart' show UvcCameraError;
export 'src/uvccamera_error_event.dart' show UvcCameraErrorEvent;
export 'src/uvccamera_error_type.dart' show UvcCameraErrorType;
export 'src/uvccamera_exception.dart' show UvcCameraException;
export 'src/uvccamera_frame_format.dart' show UvcCameraFrameFormat;
export 'src/uvccamera_mode.dart' show UvcCameraMode;
export 'src/uvccamera_preview.dart' show UvcCameraPreview;
export 'src/uvccamera_resolution_preset.dart' show UvcCameraResolutionPreset;
export 'src/uvccamera_status.dart' show UvcCameraStatus;
export 'src/uvccamera_status_attribute.dart' show UvcCameraStatusAttribute;
export 'src/uvccamera_status_class.dart' show UvcCameraStatusClass;
export 'src/uvccamera_status_event.dart' show UvcCameraStatusEvent;
