/// Base exception for UVC camera errors.
///
/// Represents errors thrown by the plugin API. Subclasses provide specific error conditions.
///
/// Properties:
///   message: Optional error message describing the failure.
///
/// Thread Safety:
///   Immutable.
class UvcCameraException implements Exception {
  final dynamic message;

  /// Create a [UvcCameraException] instance.
  ///
  /// Args:
  ///   message: Optional error message.
  const UvcCameraException([this.message]);

  /// Get a string representation of this exception.
  ///
  /// Returns:
  ///   Type name if message is null, otherwise type and message.
  @override
  String toString() {
    if (message == null) return runtimeType.toString();
    return "$runtimeType: $message";
  }
}
