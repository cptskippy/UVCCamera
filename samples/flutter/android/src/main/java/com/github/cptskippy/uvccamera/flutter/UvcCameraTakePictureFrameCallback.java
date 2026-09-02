package com.github.cptskippy.uvccamera.flutter;

import android.util.Log;

import com.serenegiant.usb.IFrameCallback;

import java.io.File;
import java.nio.ByteBuffer;

/**
 * Capture a single preview frame for a take-picture request.
 *
 * Implements {@link IFrameCallback}. Only the first delivered frame is used; subsequent frames are
 * ignored. The captured frame is handed to the platform for encoding to the output file.
 */
/* package-private */ class UvcCameraTakePictureFrameCallback implements IFrameCallback {

/**
 * Log tag
 */
    private static final String TAG = UvcCameraTakePictureFrameCallback.class.getCanonicalName();

/**
 * The UVC camera platform
 */
    private final UvcCameraPlatform uvcCameraPlatform;

/**
 * The camera ID
 */
    private final int cameraId;

/**
 * Output file to which the picture is saved.
 */
    private final File outputFile;

/**
 * The result handler
 */
    private final UvcCameraTakePictureResultHandler resultHandler;

/**
 * Whether the frame has been captured
 */
    private boolean frameCaptured;

/**
 * Create a new {@link UvcCameraTakePictureFrameCallback}.
 *
 * Args:
 *     uvcCameraPlatform: the UVC camera platform
 *     cameraId: the camera ID
 *     outputFile: the output file
 *     resultHandler: the result handler
 */
    public UvcCameraTakePictureFrameCallback(
            final UvcCameraPlatform uvcCameraPlatform,
            final int cameraId,
            final File outputFile,
            final UvcCameraTakePictureResultHandler resultHandler
    ) {
        this.uvcCameraPlatform = uvcCameraPlatform;
        this.cameraId = cameraId;
        this.outputFile = outputFile;
        this.resultHandler = resultHandler;
    }

/**
 * Capture the first preview frame and hand it to the platform for encoding.
 *
 * Args:
 *     frame: the preview frame delivered by the camera
 *
 * Code Paths:
 *     1. If a frame was already captured → log a warning and ignore the frame
 *     2. Otherwise → mark the frame as captured and pass it to the platform's taken-picture handler
 */
    @Override
    public void onFrame(ByteBuffer frame) {
        Log.v(TAG, "onFrame"
                + ": frame=" + frame
        );

        if (frameCaptured) {
            Log.w(TAG, "onFrame: frame already captured");
            return;
        }

        frameCaptured = true;

        uvcCameraPlatform.handleTakenPicture(cameraId, outputFile, frame, resultHandler);
    }

}
