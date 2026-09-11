import 'package:equatable/equatable.dart';
import 'package:flutter/foundation.dart';

import 'uvccamera_error.dart';

/// Represents an error event from a UVC camera.
///
/// Contains camera identifier and error details.
///
/// Properties:
///   cameraId: Platform camera identifier.
///   error: Error details.
///
/// Thread Safety:
///   Immutable.
@immutable
class UvcCameraErrorEvent extends Equatable {
  final int cameraId;
  final UvcCameraError error;

  /// Create a [UvcCameraErrorEvent] instance.
  ///
  /// Args:
  ///   cameraId: Platform camera identifier.
  ///   error: Error details.
  const UvcCameraErrorEvent({required this.cameraId, required this.error});

  /// Create an error event from a platform map.
  ///
  /// Args:
  ///   map: Map with cameraId and error.
  ///
  /// Returns:
  ///   New event instance.
  factory UvcCameraErrorEvent.fromMap(Map<dynamic, dynamic> map) {
    return UvcCameraErrorEvent(
      cameraId: map['cameraId'] as int,
      error: UvcCameraError.fromMap(map['error'] as Map<dynamic, dynamic>),
    );
  }

  /// Convert event to a platform map.
  ///
  /// Returns:
  ///   Map with cameraId and error.
  Map<String, dynamic> toMap() {
    return {'cameraId': cameraId, 'error': error.toMap()};
  }

  @override
  List<Object?> get props => [cameraId, error];
}
