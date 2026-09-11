/// Resolution preset for camera initialization.
///
/// Defines approximate target resolution ranges used when opening a camera.
/// The actual mode selected is chosen by the platform from supported modes.
///
/// Values:
///   min: Minimum resolution supported by camera.
///   low: ~480p.
///   medium: ~720p.
///   high: ~1080p.
///   max: Maximum resolution supported by camera.
enum UvcCameraResolutionPreset {
  /// Minimum resolution supported by the camera.
  min,

  /// ~480p
  low,

  /// ~720p
  medium,

  /// ~1080p
  high,

  /// Maximum resolution supported by the camera.
  max,
}
