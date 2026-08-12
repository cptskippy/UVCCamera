import 'package:equatable/equatable.dart';
import 'package:flutter/foundation.dart';

/// Represents a button event from a UVC camera.
///
/// Contains camera identifier, button identifier, and button state.
///
/// Properties:
///   cameraId: Platform camera identifier.
///   button: Button identifier.
///   state: Button state value.
///
/// Thread Safety:
///   Immutable.
@immutable
class UvcCameraButtonEvent extends Equatable {
  final int cameraId;
  final int button;
  final int state;

  /// Create a [UvcCameraButtonEvent] instance.
  ///
  /// Args:
  ///   cameraId: Platform camera identifier.
  ///   button: Button identifier.
  ///   state: Button state.
  const UvcCameraButtonEvent({required this.cameraId, required this.button, required this.state});

  /// Create a [UvcCameraButtonEvent] from a platform map.
  ///
  /// Args:
  ///   map: Map with cameraId, button, state.
  ///
  /// Returns:
  ///   New event instance.
  factory UvcCameraButtonEvent.fromMap(Map<dynamic, dynamic> map) {
    return UvcCameraButtonEvent(
      cameraId: map['cameraId'] as int,
      button: map['button'] as int,
      state: map['state'] as int,
    );
  }

  /// Convert event to a platform map.
  ///
  /// Returns:
  ///   Map with cameraId, button, state.
  Map<String, dynamic> toMap() {
    return {'cameraId': cameraId, 'button': button, 'state': state};
  }

  @override
  List<Object?> get props => [cameraId, button, state];
}
