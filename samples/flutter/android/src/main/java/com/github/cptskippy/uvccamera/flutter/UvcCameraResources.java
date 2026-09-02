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
