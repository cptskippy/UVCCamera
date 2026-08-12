import 'uvccamera_exception.dart';

/// Thrown when an operation is attempted on a disposed [UvcCameraController].
///
/// The controller has been disposed and can no longer be used. Create a new controller instance.
///
/// Lifecycle:
///   Thrown after dispose completes.
class UvcCameraControllerDisposedException extends UvcCameraException {
  /// Create a [UvcCameraControllerDisposedException] instance.
  const UvcCameraControllerDisposedException() : super('UvcCameraController is disposed');
}
