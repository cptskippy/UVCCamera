import 'package:equatable/equatable.dart';
import 'package:flutter/cupertino.dart';

import 'uvccamera_frame_format.dart';

/// Represents a camera mode with resolution and frame format.
///
/// A mode defines the width, height, and pixel format used for preview or recording.
///
/// Properties:
///   frameWidth: Width in pixels.
///   frameHeight: Height in pixels.
///   frameFormat: Pixel format, e.g., YUYV or MJPEG.
///
/// Thread Safety:
///   Immutable. Safe to share.
@immutable
class UvcCameraMode extends Equatable {
  final int frameWidth;
  final int frameHeight;
  final UvcCameraFrameFormat frameFormat;

  /// Create a [UvcCameraMode] instance.
  ///
  /// Args:
  ///   frameWidth: Width in pixels.
  ///   frameHeight: Height in pixels.
  ///   frameFormat: Frame pixel format.
  const UvcCameraMode({required this.frameWidth, required this.frameHeight, required this.frameFormat});

  /// Get the aspect ratio of this mode.
  ///
  /// Returns:
  ///   Width divided by height as a double.
  double get aspectRatio => frameWidth / frameHeight;

  /// Create a [UvcCameraMode] from a platform map.
  ///
  /// Args:
  ///   map: Map with frameWidth, frameHeight, frameFormat.
  ///
  /// Returns:
  ///   A new [UvcCameraMode] instance.
  ///
  /// Throws:
  ///   TypeError: If map keys are missing or invalid.
  factory UvcCameraMode.fromMap(Map<dynamic, dynamic> map) {
    return UvcCameraMode(
      frameWidth: map['frameWidth'] as int,
      frameHeight: map['frameHeight'] as int,
      frameFormat: UvcCameraFrameFormat.values.byName(map['frameFormat'] as String),
    );
  }

  /// Convert this mode to a platform map.
  ///
  /// Returns:
  ///   Map with frameWidth, frameHeight, frameFormat.
  Map<String, dynamic> toMap() {
    return {'frameWidth': frameWidth, 'frameHeight': frameHeight, 'frameFormat': frameFormat.name};
  }

  @override
  List<Object?> get props => [frameWidth, frameHeight, frameFormat];
}
