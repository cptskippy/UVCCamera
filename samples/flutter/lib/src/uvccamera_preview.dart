import 'package:flutter/material.dart';
import 'package:flutter/widgets.dart';

import 'uvccamera_controller.dart';
import 'uvccamera_controller_state.dart';

/// Displays a live camera preview from a [UvcCameraController].
///
/// The widget listens to controller state changes and renders the preview with correct aspect ratio.
/// An optional child widget can be overlaid on top of the preview.
///
/// Parameters:
///   controller: The [UvcCameraController] providing the preview texture.
///   child: Optional overlay widget drawn on top of preview. Defaults to empty.
///
/// State Machine:
///   Uninitialized → Initialized → Previewing
///
/// Thread Safety:
///   Must be used on the main UI thread.
class UvcCameraPreview extends StatelessWidget {
  final UvcCameraController controller;
  final Widget? child;

  /// Create a [UvcCameraPreview] widget.
  ///
  /// Args:
  ///   controller: Controller with initialized camera.
  ///   child: Optional overlay widget.
  const UvcCameraPreview(this.controller, {super.key, this.child});

  /// Build the preview widget.
  ///
  /// Returns:
  ///   Empty container if controller is not initialized, otherwise an [AspectRatio] with preview texture.
  ///
  /// Code Paths:
  ///   1. If not initialized → returns empty [Container].
  ///   2. If initialized → returns [AspectRatio] wrapping [Texture] and optional child.
  @override
  Widget build(BuildContext context) {
    if (!controller.value.isInitialized) {
      return Container();
    }

    return ValueListenableBuilder<UvcCameraControllerState>(
      valueListenable: controller,
      builder: (BuildContext context, Object? value, Widget? child) {
        return AspectRatio(
          aspectRatio: controller.value.previewMode!.aspectRatio,
          child: Stack(fit: StackFit.expand, children: <Widget>[controller.buildPreview(), child ?? Container()]),
        );
      },
      child: child,
    );
  }
}
