package com.github.cptskippy.uvccamera.flutter;

import android.util.Log;

import io.flutter.plugin.common.EventChannel;

/**
 * Handle the per-camera "uvccamera/camera@<cameraId>/error_events" event channel.
 *
 * Stores the event sink while the Dart side is listening so the platform can cast error events. The sink
 * is guarded by a lock because casting happens off the platform thread.
 */
/* package-private */ class UvcCameraErrorEventStreamHandler implements EventChannel.StreamHandler {

/**
 * Log tag
 */
    private static final String TAG = UvcCameraErrorEventStreamHandler.class.getCanonicalName();

/**
 * The event sink
 */
    private EventChannel.EventSink eventSink;

/**
 * Lock for {@link #eventSink}
 */
    private final Object eventSinkLock = new Object();

/**
 * Get the current event sink.
 *
 * Returns:
 *     the event sink while the Dart side is listening, or null otherwise
 */
    public EventChannel.EventSink getEventSink() {
        synchronized (eventSinkLock) {
            return eventSink;
        }
    }

    @Override
    public void onListen(Object arguments, EventChannel.EventSink eventSink) {
        Log.v(TAG, "onListen: arguments=" + arguments + ", eventSink=" + eventSink);

        synchronized (eventSinkLock) {
            this.eventSink = eventSink;
        }
    }

    @Override
    public void onCancel(Object arguments) {
        Log.v(TAG, "onCancel: arguments=" + arguments);

        synchronized (eventSinkLock) {
            this.eventSink = null;
        }
    }

}
