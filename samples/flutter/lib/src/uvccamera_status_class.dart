/// UVC status class corresponding to uvc_status_class from libuvc.
///
/// Values:
///   control: UVC_STATUS_CLASS_CONTROL.
///   controlCamera: UVC_STATUS_CLASS_CONTROL_CAMERA.
///   controlProcessing: UVC_STATUS_CLASS_CONTROL_PROCESSING.
enum UvcCameraStatusClass {
  /// Corresponds to the UVC_STATUS_CLASS_CONTROL
  control,

  /// Corresponds to the UVC_STATUS_CLASS_CONTROL_CAMERA
  controlCamera,

  /// Corresponds to the UVC_STATUS_CLASS_CONTROL_PROCESSING
  controlProcessing,
}
