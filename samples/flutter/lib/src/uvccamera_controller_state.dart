import 'package:flutter/foundation.dart';
import 'package:cross_file/cross_file.dart';

import 'uvccamera_device.dart';
import 'uvccamera_mode.dart';

/// Represents the immutable state of a [UvcCameraController].
///
/// Holds initialization status, device reference, preview mode, and recording/picture flags.
/// The controller updates this state via [ValueNotifier] notifications.
///
/// Properties:
///   isInitialized: True after [UvcCameraController.initialize] completes.
///   device: The UVC device controlled by the controller.
///   previewMode: Current preview mode, null until initialized.
///   isRecordingVideo: True while video recording is active.
///   videoRecordingMode: Mode used for current recording, null if not recording.
///   videoRecordingFile: File reference for current recording, null if not recording.
///   isTakingPicture: True while a picture capture is in progress.
///
/// Thread Safety:
///   Immutable. Safe to read from any isolate.
@immutable
class UvcCameraControllerState {
  /// True after [UvcCameraController.initialize] has completed successfully.
  final bool isInitialized;

  /// The UVC device controlled by the controller.
  final UvcCameraDevice device;

  /// The current preview mode of the camera.
  ///
  /// Is `null` until [isInitialized] is `true`.
  final UvcCameraMode? previewMode;

  /// True if the camera is currently recording video.
  final bool isRecordingVideo;

  /// Camera video recording mode.
  final UvcCameraMode? videoRecordingMode;

  /// Camera video recording file.
  final XFile? videoRecordingFile;

  /// True if the camera is currently taking a picture.
  final bool isTakingPicture;

  /// Create a new [UvcCameraControllerState] instance.
  ///
  /// Args:
  ///   isInitialized: True if controller has been initialized.
  ///   device: The UVC device associated with this state.
  ///   previewMode: Current preview mode, optional.
  ///   isRecordingVideo: True if recording video.
  ///   videoRecordingMode: Mode for video recording, optional.
  ///   videoRecordingFile: File for current recording, optional.
  ///   isTakingPicture: True if taking picture.
  const UvcCameraControllerState({
    required this.isInitialized,
    required this.device,
    this.previewMode,
    required this.isRecordingVideo,
    this.videoRecordingMode,
    this.videoRecordingFile,
    required this.isTakingPicture,
  });

  /// Create an uninitialized [UvcCameraControllerState] for a new controller.
  ///
  /// Args:
  ///   device: The UVC device to associate with the initial state.
  ///
  /// Returns:
  ///   State with isInitialized false and all recording flags cleared.
  const UvcCameraControllerState.uninitialized(UvcCameraDevice device)
    : this(
        isInitialized: false,
        device: device,
        previewMode: null,
        isRecordingVideo: false,
        videoRecordingMode: null,
        videoRecordingFile: null,
        isTakingPicture: false,
      );

  /// Create a modified copy of this state with updated fields.
  ///
  /// Args:
  ///   isInitialized: Override initialization flag, optional.
  ///   device: Override device, optional.
  ///   previewMode: Override preview mode, optional.
  ///   isRecordingVideo: Override recording flag, optional.
  ///   videoRecordingMode: Override recording mode, optional.
  ///   videoRecordingFile: Override recording file, optional.
  ///   isTakingPicture: Override picture flag, optional.
  ///
  /// Returns:
  ///   A new [UvcCameraControllerState] with specified fields updated.
  UvcCameraControllerState copyWith({
    bool? isInitialized,
    UvcCameraDevice? device,
    UvcCameraMode? previewMode,
    bool? isRecordingVideo,
    UvcCameraMode? videoRecordingMode,
    XFile? videoRecordingFile,
    bool? isTakingPicture,
  }) {
    return UvcCameraControllerState(
      isInitialized: isInitialized ?? this.isInitialized,
      device: device ?? this.device,
      previewMode: previewMode ?? this.previewMode,
      isRecordingVideo: isRecordingVideo ?? this.isRecordingVideo,
      videoRecordingMode: videoRecordingMode ?? this.videoRecordingMode,
      videoRecordingFile: videoRecordingFile ?? this.videoRecordingFile,
      isTakingPicture: isTakingPicture ?? this.isTakingPicture,
    );
  }

  @override
  String toString() {
    return '${objectRuntimeType(this, 'UvcCameraControllerState')}('
        'isInitialized: $isInitialized, '
        'device: $device, '
        'previewMode: $previewMode, '
        'isRecordingVideo: $isRecordingVideo, '
        'videoRecordingMode: $videoRecordingMode, '
        'videoRecordingFile: $videoRecordingFile, '
        'isTakingPicture: $isTakingPicture'
        ')';
  }
}
