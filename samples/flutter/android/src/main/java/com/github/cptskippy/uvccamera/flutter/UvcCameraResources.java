package com.github.cptskippy.uvccamera.flutter;

import android.media.MediaRecorder;
import android.view.Surface;

import com.serenegiant.usb.UVCCamera;

import io.flutter.plugin.common.EventChannel;
import io.flutter.view.TextureRegistry;

/**
 * Bundle the resources opened for a single camera.
 *
 * Groups the camera, its preview surface, its per-camera event channels and stream handlers, and its
 * callbacks under one immutable handle so the platform can look up and release everything for a camera
 * in a single pass on close.
 *
 * Properties:
 *     cameraId: Stable identifier for the opened camera, derived from the USB connection file descriptor and used as the key in {@link UvcCameraPlatform}'s resource map.
 *     surfaceSurfaceProducer: Flutter {@link TextureRegistry.SurfaceProducer} that produces the preview texture and provides the platform texture ID.
 *     surface: The Android {@link Surface} backed by {@link #surfaceSurfaceProducer}; it is passed to {@link UVCCamera#setPreviewDisplay(Surface)} and released on close.
 *     camera: The opened {@link UVCCamera} instance used for preview, status and button callbacks, frame capture, and video recording.
 *     errorEventChannel: The Flutter {@link EventChannel} used to deliver camera error events to Dart.
 *     errorEventStreamHandler: The stream handler attached to {@link #errorEventChannel}; it stores the active Dart event sink while the Dart side is listening.
 *     errorCallback: The {@link UvcCameraErrorCallback} that reports preview-surface destruction as an error and controls whether error events are cast; it is the camera-side event source, not the Dart-side sink holder.
 *     statusEventChannel: The Flutter {@link EventChannel} used to deliver UVC status events to Dart.
 *     statusEventStreamHandler: The stream handler attached to {@link #statusEventChannel}; it stores the active Dart event sink while the Dart side is listening.
 *     statusCallback: The {@link UvcCameraStatusCallback} registered with the camera; it gates forwarding of UVC status events to the platform.
 *     buttonEventChannel: The Flutter {@link EventChannel} used to deliver UVC button events to Dart.
 *     buttonEventStreamHandler: The stream handler attached to {@link #buttonEventChannel}; it stores the active Dart event sink while the Dart side is listening.
 *     buttonCallback: The {@link UvcCameraButtonCallback} registered with the camera; it gates forwarding of UVC button events to the platform.
 *     mediaRecorder: The per-camera {@link MediaRecorder} used for video recording; it is reset and released when the camera is closed.
 *
 * The error-event members are intentionally separate: {@link #errorEventChannel} is the Dart transport,
 * {@link #errorEventStreamHandler} holds the current Dart event sink, and {@link #errorCallback} is the
 * camera-side source and enable/disable gate.
 *
 * Thread Safety:
 *     The record itself is immutable, but the contained resources remain live objects with their own thread-safety requirements. Safe publication and atomic map access are provided by the containing concurrent map; the record does not synchronize access to the camera, surfaces, recorder, event channels, or callbacks.
 */
/* package-private */ record UvcCameraResources(
        int cameraId,
        TextureRegistry.SurfaceProducer surfaceSurfaceProducer,
        Surface surface,
        UVCCamera camera,
        EventChannel errorEventChannel,
        UvcCameraErrorEventStreamHandler errorEventStreamHandler,
        UvcCameraErrorCallback errorCallback,
        EventChannel statusEventChannel,
        UvcCameraStatusEventStreamHandler statusEventStreamHandler,
        UvcCameraStatusCallback statusCallback,
        EventChannel buttonEventChannel,
        UvcCameraButtonEventStreamHandler buttonEventStreamHandler,
        UvcCameraButtonCallback buttonCallback,
        MediaRecorder mediaRecorder
) {
}
