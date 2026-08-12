import 'package:equatable/equatable.dart';
import 'package:flutter/foundation.dart';

import 'uvccamera_status.dart';

/// Represents a status event from a UVC camera.
///
/// Contains camera identifier and status payload.
///
/// Properties:
///   cameraId: Platform camera identifier.
///   payload: Status data.
///
/// Thread Safety:
///   Immutable.
@immutable
class UvcCameraStatusEvent extends Equatable {
  final int cameraId;
  final UvcCameraStatus payload;

  /// Create a [UvcCameraStatusEvent] instance.
  ///
  /// Args:
  ///   cameraId: Platform camera identifier.
  ///   payload: Status payload.
  const UvcCameraStatusEvent({required this.cameraId, required this.payload});

  /// Create status event from a platform map.
  ///
  /// Args:
  ///   map: Map with cameraId and payload.
  ///
  /// Returns:
  ///   New event instance.
  factory UvcCameraStatusEvent.fromMap(Map<dynamic, dynamic> map) {
    return UvcCameraStatusEvent(
      cameraId: map['cameraId'] as int,
      payload: UvcCameraStatus.fromMap(map['payload'] as Map<dynamic, dynamic>),
    );
  }

  /// Convert event to a platform map.
  ///
  /// Returns:
  ///   Map with cameraId and payload.
  Map<String, dynamic> toMap() {
    return {'cameraId': cameraId, 'payload': payload.toMap()};
  }

  @override
  List<Object?> get props => [cameraId, payload];
}
