import 'package:equatable/equatable.dart';
import 'package:flutter/foundation.dart';

/// Represents a UVC camera device discovered on the system.
///
/// Contains immutable metadata identifying the device for permission requests and opening.
///
/// Properties:
///   name: Human-readable device name.
///   deviceClass: USB device class code.
///   deviceSubclass: USB device subclass code.
///   vendorId: USB vendor identifier.
///   productId: USB product identifier.
///
/// Thread Safety:
///   Immutable after construction. Safe to share across isolates.
@immutable
class UvcCameraDevice extends Equatable {
  final String name;
  final int deviceClass;
  final int deviceSubclass;
  final int vendorId;
  final int productId;

  /// Create a [UvcCameraDevice] instance.
  ///
  /// Args:
  ///   name: Human-readable device name.
  ///   deviceClass: USB device class code.
  ///   deviceSubclass: USB device subclass code.
  ///   vendorId: USB vendor identifier.
  ///   productId: USB product identifier.
  const UvcCameraDevice({
    required this.name,
    required this.deviceClass,
    required this.deviceSubclass,
    required this.vendorId,
    required this.productId,
  });

  /// Create a [UvcCameraDevice] from a platform map.
  ///
  /// Args:
  ///   map: Platform map containing name, deviceClass, deviceSubclass, vendorId, productId.
  ///
  /// Returns:
  ///   A new [UvcCameraDevice] instance.
  ///
  /// Throws:
  ///   TypeError: If required keys are missing or have incorrect types.
  factory UvcCameraDevice.fromMap(Map<dynamic, dynamic> map) {
    return UvcCameraDevice(
      name: map['name'] as String,
      deviceClass: map['deviceClass'] as int,
      deviceSubclass: map['deviceSubclass'] as int,
      vendorId: map['vendorId'] as int,
      productId: map['productId'] as int,
    );
  }

  /// Convert this device to a platform map.
  ///
  /// Returns:
  ///   A map with keys name, deviceClass, deviceSubclass, vendorId, productId.
  Map<String, dynamic> toMap() {
    return {
      'name': name,
      'deviceClass': deviceClass,
      'deviceSubclass': deviceSubclass,
      'vendorId': vendorId,
      'productId': productId,
    };
  }

  @override
  List<Object?> get props => [name, deviceClass, deviceSubclass, vendorId, productId];
}
