import 'uvccamera_exception.dart';

/// Thrown when [UvcCameraController.initialize] is called multiple times.
///
/// The controller is already initialized or initializing. Dispose and recreate to reinitialize.
///
/// Lifecycle:
///   Thrown on second initialize call.
class UvcCameraControllerInitializedException extends UvcCameraException {
  /// Create a [UvcCameraControllerInitializedException] instance.
  const UvcCameraControllerInitializedException() : super('UvcCameraController is already initialized');
}
