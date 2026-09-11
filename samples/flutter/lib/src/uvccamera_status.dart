import 'package:equatable/equatable.dart';
import 'package:flutter/foundation.dart';

import 'uvccamera_status_attribute.dart';
import 'uvccamera_status_class.dart';

/// Represents UVC status callback data.
///
/// Corresponds to uvc_status_callback_t arguments from libuvc.
///
/// Properties:
///   statusClass: Status class identifier.
///   event: Event code.
///   selector: Selector code.
///   statusAttribute: Status attribute.
///
/// Thread Safety:
///   Immutable.
@immutable
class UvcCameraStatus extends Equatable {
  final UvcCameraStatusClass statusClass;
  final int event;
  final int selector;
  final UvcCameraStatusAttribute statusAttribute;

  /// Create a [UvcCameraStatus] instance.
  ///
  /// Args:
  ///   statusClass: Status class.
  ///   event: Event code.
  ///   selector: Selector code.
  ///   statusAttribute: Status attribute.
  const UvcCameraStatus({
    required this.statusClass,
    required this.event,
    required this.selector,
    required this.statusAttribute,
  });

  /// Create status from a platform map.
  ///
  /// Args:
  ///   map: Map with statusClass, event, selector, statusAttribute.
  ///
  /// Returns:
  ///   New status instance.
  factory UvcCameraStatus.fromMap(Map<dynamic, dynamic> map) {
    return UvcCameraStatus(
      statusClass: UvcCameraStatusClass.values.byName(map['statusClass'] as String),
      event: map['event'] as int,
      selector: map['selector'] as int,
      statusAttribute: UvcCameraStatusAttribute.values.byName(map['statusAttribute'] as String),
    );
  }

  /// Convert status to a platform map.
  ///
  /// Returns:
  ///   Map with status fields.
  Map<String, dynamic> toMap() {
    return {
      'statusClass': statusClass.name,
      'event': event,
      'selector': selector,
      'statusAttribute': statusAttribute.name,
    };
  }

  @override
  List<Object?> get props => [statusClass, event, selector, statusAttribute];
}
