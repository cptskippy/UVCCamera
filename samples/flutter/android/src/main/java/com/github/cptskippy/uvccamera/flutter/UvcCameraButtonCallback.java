package com.github.cptskippy.uvccamera.flutter;

import android.util.Log;

import com.serenegiant.usb.IButtonCallback;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Forward UVC camera button events to the button event stream.
 *
 * Implements {@link IButtonCallback} for {@link com.serenegiant.usb.UVCCamera}. Button events are cast to
 * the platform only while event casting is enabled for this camera.
 */
/* package-private */ class UvcCameraButtonCallback implements IButtonCallback {

/**
 * Log tag
 */
    private static final String TAG = UvcCameraButtonCallback.class.getCanonicalName();

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
 * Create a new {@link UvcCameraButtonCallback} instance.
 *
 * Args:
 *     uvcCameraPlatform: the UVC camera platform
 *     cameraId: the camera ID
 */
    public UvcCameraButtonCallback(final UvcCameraPlatform uvcCameraPlatform, final int cameraId) {
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
 * Forward the button event to the platform.
 *
 * Args:
 *     button: the UVC button identifier
 *     state: the UVC button state
 *
 * Code Paths:
 *     1. If event casting is disabled → log and return without casting
 *     2. Otherwise → cast the button event to the platform's button event sink
 */
    @Override
    public void onButton(int button, int state) {
        Log.v(TAG, "onButton"
                + ": cameraId=" + cameraId
                + ", button=" + button
                + ", state=" + state
        );

        if (castEvents.get()) {
            uvcCameraPlatform.castCameraButtonEvent(cameraId, button, state);
        }
    }

}
