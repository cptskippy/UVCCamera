package com.github.cptskippy.uvccamera.flutter;

import android.util.Log;

import com.serenegiant.usb.IStatusCallback;

import java.nio.ByteBuffer;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Forward UVC camera status events to the status event stream.
 *
 * Implements {@link IStatusCallback} for {@link com.serenegiant.usb.UVCCamera}. Status events are cast to
 * the platform only while event casting is enabled for this camera.
 */
/* package-private */ class UvcCameraStatusCallback implements IStatusCallback {

/**
 * Log tag
 */
    private static final String TAG = UvcCameraStatusCallback.class.getCanonicalName();

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
 * Create a new {@link UvcCameraStatusCallback} instance.
 *
 * Args:
 *     uvcCameraPlatform: the UVC camera platform
 *     cameraId: the camera ID
 */
    public UvcCameraStatusCallback(final UvcCameraPlatform uvcCameraPlatform, final int cameraId) {
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
 * Forward the status event to the platform.
 *
 * Args:
 *     statusClass: the UVC status class
 *     event: the UVC status event
 *     selector: the UVC status selector
 *     statusAttribute: the UVC status attribute
 *     data: the status data
 *
 * Code Paths:
 *     1. If event casting is disabled → log and return without casting
 *     2. Otherwise → cast the status event to the platform's status event sink
 */
    @Override
    public void onStatus(int statusClass, int event, int selector, int statusAttribute, ByteBuffer data) {
        Log.v(TAG, "onStatus"
                + ": cameraId=" + cameraId
                + ", statusClass=" + statusClass
                + ", event=" + event
                + ", selector=" + selector
                + ", statusAttribute=" + statusAttribute
                + ", data=" + data
        );

        if (castEvents.get()) {
            uvcCameraPlatform.castCameraStatusEvent(cameraId, statusClass, event, selector, statusAttribute, data);
        }
    }

}
