import 'package:equatable/equatable.dart';
import 'package:flutter/foundation.dart';

import 'uvccamera_device.dart';
import 'uvccamera_device_event_type.dart';

/// Represents a device event for UVC cameras.
///
/// Indicates attach, detach, connect, or disconnect events.
///
/// Properties:
///   device: The [UvcCameraDevice] involved.
///   type: Event type.
///
/// Thread Safety:
///   Immutable.
@immutable
class UvcCameraDeviceEvent extends Equatable {
  final UvcCameraDevice device;
  final UvcCameraDeviceEventType type;

  /// Create a [UvcCameraDeviceEvent] instance.
  ///
  /// Args:
  ///   device: Device involved.
  ///   type: Event type.
  const UvcCameraDeviceEvent({required this.device, required this.type});

  /// Create a [UvcCameraDeviceEvent] from a platform map.
  ///
  /// Args:
  ///   map: Map with device and type.
  ///
  /// Returns:
  ///   New event instance.
  factory UvcCameraDeviceEvent.fromMap(Map<dynamic, dynamic> map) {
    return UvcCameraDeviceEvent(
      device: UvcCameraDevice.fromMap(map['device'] as Map<dynamic, dynamic>),
      type: UvcCameraDeviceEventType.values.byName(map['type'] as String),
    );
  }

  /// Convert event to a platform map.
  ///
  /// Returns:
  ///   Map with device and type.
  Map<String, dynamic> toMap() {
    return {'device': device.toMap(), 'type': type.name};
  }

  @override
  List<Object?> get props => [device, type];
}
