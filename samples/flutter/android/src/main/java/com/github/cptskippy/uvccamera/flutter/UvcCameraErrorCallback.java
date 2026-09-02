package com.github.cptskippy.uvccamera.flutter;

import android.util.Log;

import java.util.concurrent.atomic.AtomicBoolean;

import io.flutter.view.TextureRegistry;

/**
 * Report UVC camera error events to the error event stream.
 *
 * Watches the preview surface with a {@link TextureRegistry.SurfaceProducer.Callback}. When the surface is
 * destroyed, a "previewInterrupted" error event is cast to the platform while event casting is enabled.
 */
/* package-private */ class UvcCameraErrorCallback {

/**
 * Log tag
 */
    private static final String TAG = UvcCameraErrorCallback.class.getCanonicalName();

/**
 * The UVC camera platform
 */
    private final UvcCameraPlatform uvcCameraPlatform;

/**
 * The camera ID
 */
    private final int cameraId;

/**
 * Flag that controls whether or not the events are casted to the sink
 */
    private final AtomicBoolean castEvents = new AtomicBoolean(false);

/**
 * A callback for the {@link TextureRegistry.SurfaceProducer#setCallback(TextureRegistry.SurfaceProducer.Callback)}
 */
    public final TextureRegistry.SurfaceProducer.Callback textureRegistrySurfaceProducerCallback =
            new TextureRegistrySurfaceProducerCallback();

/**
 * Create a new {@link UvcCameraErrorCallback} instance.
 *
 * Args:
 *     uvcCameraPlatform: the UVC camera platform
 *     cameraId: the camera ID
 */
    public UvcCameraErrorCallback(final UvcCameraPlatform uvcCameraPlatform, final int cameraId) {
        this.uvcCameraPlatform = uvcCameraPlatform;
        this.cameraId = cameraId;
    }

/**
 * Enables casting of events to the sink
 */
    public void enableEventsCasting() {
        castEvents.set(true);
    }

/**
 * Disables casting of events to the sink
 */
    public void disableEventsCasting() {
        castEvents.set(false);
    }

/**
 * Preview surface callback that reports surface destruction as a camera error.
 */
    private class TextureRegistrySurfaceProducerCallback implements TextureRegistry.SurfaceProducer.Callback {
/**
 * Log tag
 */
        private static final String TAG = TextureRegistrySurfaceProducerCallback.class.getCanonicalName();

/**
 * Cast a "previewInterrupted" error event when the preview surface is destroyed.
 *
 * Code Paths:
 *     1. If event casting is disabled → log and return
 *     2. Otherwise → cast the error event to the platform's error event sink
 */
        @Override
        public void onSurfaceDestroyed() {
            Log.v(TAG, "onSurfaceDestroyed");

            if (UvcCameraErrorCallback.this.castEvents.get()) {
                UvcCameraErrorCallback.this.uvcCameraPlatform.castCameraErrorEvent(
                        cameraId,
                        "previewInterrupted",
                        "The surface was destroyed"
                );
            }
        }
    }

}
