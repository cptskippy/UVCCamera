/// UVC status attribute corresponding to uvc_status_attribute from libuvc.
///
/// Values:
///   valueChange: UVC_STATUS_ATTRIBUTE_VALUE_CHANGE.
///   infoChange: UVC_STATUS_ATTRIBUTE_INFO_CHANGE.
///   failureChange: UVC_STATUS_ATTRIBUTE_FAILURE_CHANGE.
///   unknown: UVC_STATUS_ATTRIBUTE_UNKNOWN.
enum UvcCameraStatusAttribute {
  /// Corresponds to the UVC_STATUS_ATTRIBUTE_VALUE_CHANGE
  valueChange,

  /// Corresponds to the UVC_STATUS_ATTRIBUTE_INFO_CHANGE
  infoChange,

  /// Corresponds to the UVC_STATUS_ATTRIBUTE_FAILURE_CHANGE
  failureChange,

  /// Corresponds to the UVC_STATUS_ATTRIBUTE_UNKNOWN
  unknown,
}
