/*
 *  UVCCamera
 *  library and sample to access to UVC web camera on non-rooted Android device
 *
 * Copyright (c) 2014-2017 saki t_saki@serenegiant.com
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 *
 *  All files in the folder are under this Apache License, Version 2.0.
 *  Files in the libjpeg-turbo, libusb, libuvc, rapidjson folder
 *  may have a different license, see the respective files.
 */

package com.serenegiant.usbcameracommon;

import android.app.Activity;

import com.serenegiant.usb.UVCCamera;
import com.serenegiant.widget.CameraViewInterface;
/**
 * Manage a UVCCamera handler for preview, capture, and recording.
 */

public class UVCCameraHandler extends AbstractUVCCameraHandler {

	/**
	 * Create a UVCCameraHandler that uses MediaVideoEncoder with MJPEG frames and the default bandwidth.
	 *
	 * Args:
	 *     parent: the parent Activity.
	 *     cameraView: the camera view used to capture still images.
	 *     width: the preview width in pixels.
	 *     height: the preview height in pixels.
	 *
	 * Returns:
	 *     the created UVCCameraHandler.
	 */
	public static final UVCCameraHandler createHandler(
			final Activity parent, final CameraViewInterface cameraView,
			final int width, final int height) {

		return createHandler(parent, cameraView, 1, width, height, UVCCamera.FRAME_FORMAT_MJPEG, UVCCamera.DEFAULT_BANDWIDTH);
	}

	/**
	 * Create a UVCCameraHandler that uses MediaVideoEncoder with MJPEG frames.
	 *
	 * Args:
	 *     parent: the parent Activity.
	 *     cameraView: the camera view used to capture still images.
	 *     width: the preview width in pixels.
	 *     height: the preview height in pixels.
	 *     bandwidthFactor: the bandwidth factor passed to setPreviewSize.
	 *
	 * Returns:
	 *     the created UVCCameraHandler.
	 */
	public static final UVCCameraHandler createHandler(
			final Activity parent, final CameraViewInterface cameraView,
			final int width, final int height, final float bandwidthFactor) {

		return createHandler(parent, cameraView, 1, width, height, UVCCamera.FRAME_FORMAT_MJPEG, bandwidthFactor);
	}

	/**
	 * Create a UVCCameraHandler with MJPEG frames and the default bandwidth.
	 *
	 * Args:
	 *     parent: the parent Activity.
	 *     cameraView: the camera view used to capture still images.
	 *     encoderType: 0: use MediaSurfaceEncoder, 1: use MediaVideoEncoder, 2: use MediaVideoBufferEncoder.
	 *     width: the preview width in pixels.
	 *     height: the preview height in pixels.
	 *
	 * Returns:
	 *     the created UVCCameraHandler.
	 */
	public static final UVCCameraHandler createHandler(
			final Activity parent, final CameraViewInterface cameraView,
			final int encoderType, final int width, final int height) {

		return createHandler(parent, cameraView, encoderType, width, height, UVCCamera.FRAME_FORMAT_MJPEG, UVCCamera.DEFAULT_BANDWIDTH);
	}

	/**
	 * Create a UVCCameraHandler with the default bandwidth.
	 *
	 * Args:
	 *     parent: the parent Activity.
	 *     cameraView: the camera view used to capture still images.
	 *     encoderType: 0: use MediaSurfaceEncoder, 1: use MediaVideoEncoder, 2: use MediaVideoBufferEncoder.
	 *     width: the preview width in pixels.
	 *     height: the preview height in pixels.
	 *     format: either UVCCamera.FRAME_FORMAT_YUYV(0) or UVCCamera.FRAME_FORMAT_MJPEG(1).
	 *
	 * Returns:
	 *     the created UVCCameraHandler.
	 */
	public static final UVCCameraHandler createHandler(
			final Activity parent, final CameraViewInterface cameraView,
			final int encoderType, final int width, final int height, final int format) {

		return createHandler(parent, cameraView, encoderType, width, height, format, UVCCamera.DEFAULT_BANDWIDTH);
	}

	/**
	 * Create a UVCCameraHandler with the given encoder, format, and bandwidth.
	 *
	 * Args:
	 *     parent: the parent Activity.
	 *     cameraView: the camera view used to capture still images.
	 *     encoderType: 0: use MediaSurfaceEncoder, 1: use MediaVideoEncoder, 2: use MediaVideoBufferEncoder.
	 *     width: the preview width in pixels.
	 *     height: the preview height in pixels.
	 *     format: either UVCCamera.FRAME_FORMAT_YUYV(0) or UVCCamera.FRAME_FORMAT_MJPEG(1).
	 *     bandwidthFactor: the bandwidth factor passed to setPreviewSize.
	 *
	 * Returns:
	 *     the created UVCCameraHandler.
	 */
	public static final UVCCameraHandler createHandler(
			final Activity parent, final CameraViewInterface cameraView,
			final int encoderType, final int width, final int height, final int format, final float bandwidthFactor) {

		final CameraThread thread = new CameraThread(UVCCameraHandler.class, parent, cameraView, encoderType, width, height, format, bandwidthFactor);
		thread.start();
		return (UVCCameraHandler)thread.getHandler();
	}

	protected UVCCameraHandler(final CameraThread thread) {
		super(thread);
	}

	@Override
	/**
	 * Start the preview on the given surface.
	 *
	 * Args:
	 *     surface: a SurfaceHolder, Surface, or SurfaceTexture for the preview.
	 */
	public void startPreview(final Object surface) {
		super.startPreview(surface);
	}

	@Override
	/**
	 * Capture a still image and save it as a PNG file in the DCIM directory.
	 */
	public void captureStill() {
		super.captureStill();
	}

	@Override
	/**
	 * Capture a still image and save it as a PNG file to the given path.
	 *
	 * Args:
	 *     path: the output file path, or a generated path in the DCIM directory if empty.
	 */
	public void captureStill(final String path) {
		super.captureStill(path);
	}
}
