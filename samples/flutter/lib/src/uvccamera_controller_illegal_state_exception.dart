import 'uvccamera_exception.dart';

/// Thrown when [UvcCameraController] is used in an illegal state.
///
/// Indicates an operation was requested that conflicts with current state, e.g., taking picture while already taking picture.
///
/// Args:
///   message: Optional description of the illegal state.
class UvcCameraControllerIllegalStateException extends UvcCameraException {
  /// Create a [UvcCameraControllerIllegalStateException] instance.
  ///
  /// Args:
  ///   message: Optional description.
  const UvcCameraControllerIllegalStateException([super.message]);
}
