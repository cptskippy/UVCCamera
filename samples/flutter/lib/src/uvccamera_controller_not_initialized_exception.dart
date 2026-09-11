import 'uvccamera_exception.dart';

/// Thrown when an operation is attempted on an uninitialized [UvcCameraController].
///
/// Call [UvcCameraController.initialize] before using the controller.
///
/// Lifecycle:
///   Thrown before first initialize completes.
class UvcCameraControllerNotInitializedException extends UvcCameraException {
  /// Create a [UvcCameraControllerNotInitializedException] instance.
  const UvcCameraControllerNotInitializedException() : super('UvcCameraController is not initialized');
}
