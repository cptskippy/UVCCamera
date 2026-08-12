import 'package:equatable/equatable.dart';
import 'package:flutter/foundation.dart';

import 'uvccamera_error_type.dart';

/// Represents an error from a UVC camera.
///
/// Contains error type and optional reason string.
///
/// Properties:
///   type: Error type.
///   reason: Optional reason description.
///
/// Thread Safety:
///   Immutable.
@immutable
class UvcCameraError extends Equatable {
  final UvcCameraErrorType type;
  final String? reason;

  /// Create a [UvcCameraError] instance.
  ///
  /// Args:
  ///   type: Error type.
  ///   reason: Optional reason.
  const UvcCameraError({required this.type, this.reason});

  /// Create a [UvcCameraError] from a platform map.
  ///
  /// Args:
  ///   map: Map with type and optional reason.
  ///
  /// Returns:
  ///   New error instance.
  factory UvcCameraError.fromMap(Map<dynamic, dynamic> map) {
    return UvcCameraError(
      type: UvcCameraErrorType.values.byName(map['type'] as String),
      reason: map['reason'] as String?,
    );
  }

  /// Convert error to a platform map.
  ///
  /// Returns:
  ///   Map with type.
  Map<String, dynamic> toMap() {
    return {'type': type.name};
  }

  @override
  List<Object?> get props => [type];
}
