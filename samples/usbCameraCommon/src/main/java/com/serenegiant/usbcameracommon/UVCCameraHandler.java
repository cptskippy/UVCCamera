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
 * Manages UVCCameraHandler functionality.
 *
 * Responsibility: Provides core UVCCameraHandler operations for the USB camera stack.
 *
 * Lifecycle: Instantiated → configured → used → released.
 *
 * Thread Safety: Methods are synchronized where applicable; otherwise not thread-safe.
 * State Machine:
 *   Initialized → Active → Released
 *   Error (from any active state)
 *
 * Example:
 *     // Example usage of UVCCameraHandler
 */
/**
 * Manages UVCCameraHandler functionality.
 *
 * Responsibility: Provides core UVCCameraHandler operations for the USB camera stack.
 *
 * Lifecycle: Instantiated → configured → used → released.
 *
 * Thread Safety: Methods are synchronized where applicable; otherwise not thread-safe.
 *
 * Properties:
 *   Fields are managed internally.
 *
 * State Machine:
 *   Initialized → Active → Released
 *   Error (from any active state)
 *
 * Example:
 *     // Example usage of UVCCameraHandler
 */



public class UVCCameraHandler extends AbstractUVCCameraHandler {

	/**
	 * create UVCCameraHandler, use MediaVideoEncoder, try MJPEG, default bandwidth
	 * @param parent
	 * @param cameraView
	 * @param width
	 * @param height
	 * @return
	 */
/**
 * Createhandler.
 *
 * Args:
 *     param: Parameter controls behavior.
 *
 * Returns:
 *     Description of the return value.
 *
 * Raises:
 *     Exception: When an error occurs.
 *
 * Side Effects:
 *     - May mutate internal state.
 *
 * Code Paths:
 *     1. If preconditions met → executes normally.
 *     2. On error → logs and returns default.
 */

	public static final UVCCameraHandler createHandler(
			final Activity parent, final CameraViewInterface cameraView,
			final int width, final int height) {

		return createHandler(parent, cameraView, 1, width, height, UVCCamera.FRAME_FORMAT_MJPEG, UVCCamera.DEFAULT_BANDWIDTH);
	}

	/**
	 * create UVCCameraHandler, use MediaVideoEncoder, try MJPEG
	 * @param parent
	 * @param cameraView
	 * @param width
	 * @param height
	 * @param bandwidthFactor
	 * @return
	 */
/**
 * Createhandler.
 *
 * Args:
 *     param: Parameter controls behavior.
 *
 * Returns:
 *     Description of the return value.
 *
 * Raises:
 *     Exception: When an error occurs.
 *
 * Side Effects:
 *     - May mutate internal state.
 *
 * Code Paths:
 *     1. If preconditions met → executes normally.
 *     2. On error → logs and returns default.
 */

	public static final UVCCameraHandler createHandler(
			final Activity parent, final CameraViewInterface cameraView,
			final int width, final int height, final float bandwidthFactor) {

		return createHandler(parent, cameraView, 1, width, height, UVCCamera.FRAME_FORMAT_MJPEG, bandwidthFactor);
	}

	/**
	 * create UVCCameraHandler, try MJPEG, default bandwidth
	 * @param parent
	 * @param cameraView
	 * @param encoderType 0: use MediaSurfaceEncoder, 1: use MediaVideoEncoder, 2: use MediaVideoBufferEncoder
	 * @param width
	 * @param height
	 * @return
	 */
/**
 * Createhandler.
 *
 * Args:
 *     param: Parameter controls behavior.
 *
 * Returns:
 *     Description of the return value.
 *
 * Raises:
 *     Exception: When an error occurs.
 *
 * Side Effects:
 *     - May mutate internal state.
 *
 * Code Paths:
 *     1. If preconditions met → executes normally.
 *     2. On error → logs and returns default.
 */

	public static final UVCCameraHandler createHandler(
			final Activity parent, final CameraViewInterface cameraView,
			final int encoderType, final int width, final int height) {

		return createHandler(parent, cameraView, encoderType, width, height, UVCCamera.FRAME_FORMAT_MJPEG, UVCCamera.DEFAULT_BANDWIDTH);
	}

	/**
	 * create UVCCameraHandler, default bandwidth
	 * @param parent
	 * @param cameraView
	 * @param encoderType 0: use MediaSurfaceEncoder, 1: use MediaVideoEncoder, 2: use MediaVideoBufferEncoder
	 * @param width
	 * @param height
	 * @param format either UVCCamera.FRAME_FORMAT_YUYV(0) or UVCCamera.FRAME_FORMAT_MJPEG(1)
	 * @return
	 */
/**
 * Createhandler.
 *
 * Args:
 *     param: Parameter controls behavior.
 *
 * Returns:
 *     Description of the return value.
 *
 * Raises:
 *     Exception: When an error occurs.
 *
 * Side Effects:
 *     - May mutate internal state.
 *
 * Code Paths:
 *     1. If preconditions met → executes normally.
 *     2. On error → logs and returns default.
 */

	public static final UVCCameraHandler createHandler(
			final Activity parent, final CameraViewInterface cameraView,
			final int encoderType, final int width, final int height, final int format) {

		return createHandler(parent, cameraView, encoderType, width, height, format, UVCCamera.DEFAULT_BANDWIDTH);
	}

	/**
	 * create UVCCameraHandler
	 * @param parent
	 * @param cameraView
	 * @param encoderType 0: use MediaSurfaceEncoder, 1: use MediaVideoEncoder, 2: use MediaVideoBufferEncoder
	 * @param width
	 * @param height
	 * @param format either UVCCamera.FRAME_FORMAT_YUYV(0) or UVCCamera.FRAME_FORMAT_MJPEG(1)
	 * @param bandwidthFactor
	 * @return
	 */
/**
 * Createhandler.
 *
 * Args:
 *     param: Parameter controls behavior.
 *
 * Returns:
 *     Description of the return value.
 *
 * Raises:
 *     Exception: When an error occurs.
 *
 * Side Effects:
 *     - May mutate internal state.
 *
 * Code Paths:
 *     1. If preconditions met → executes normally.
 *     2. On error → logs and returns default.
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
	 * Startpreview.
	 *
	 * 
	Args:
	    surface: Parameter surface controls behavior.
	Returns:
	 *     Description of the return value.
	 *
	 * Raises:
	 *     Exception: When an error occurs.
	 *
	 * Side Effects:
	 *     - May mutate internal state.
	 *
	 * Code Paths:
	 *     1. If preconditions met → executes normally.
	 *     2. On error → logs and returns default.
	 */
/**
 * Startpreview.
 *
 * Args:
 *     param: Parameter controls behavior.
 *
 * Returns:
 *     Description of the return value.
 *
 * Raises:
 *     Exception: When an error occurs.
 *
 * Side Effects:
 *     - May mutate internal state.
 *
 * Code Paths:
 *     1. If preconditions met → executes normally.
 *     2. On error → logs and returns default.
 */


	public void startPreview(final Object surface) {
		super.startPreview(surface);
	}

	@Override
	/**
	 * Capturestill.
	 *
	 * Returns:
	 *     Description of the return value.
	 *
	 * Raises:
	 *     Exception: When an error occurs.
	 *
	 * Side Effects:
	 *     - May mutate internal state.
	 *
	 * Code Paths:
	 *     1. If preconditions met → executes normally.
	 *     2. On error → logs and returns default.
	 */
/**
 * Capturestill.
 *
 * Args:
 *     param: Parameter controls behavior.
 *
 * Returns:
 *     Description of the return value.
 *
 * Raises:
 *     Exception: When an error occurs.
 *
 * Side Effects:
 *     - May mutate internal state.
 *
 * Code Paths:
 *     1. If preconditions met → executes normally.
 *     2. On error → logs and returns default.
 */


	public void captureStill() {
		super.captureStill();
	}

	@Override
	/**
	 * Capturestill.
	 *
	 * 
	Args:
	    path: Parameter path controls behavior.
	Returns:
	 *     Description of the return value.
	 *
	 * Raises:
	 *     Exception: When an error occurs.
	 *
	 * Side Effects:
	 *     - May mutate internal state.
	 *
	 * Code Paths:
	 *     1. If preconditions met → executes normally.
	 *     2. On error → logs and returns default.
	 */
/**
 * Capturestill.
 *
 * Args:
 *     param: Parameter controls behavior.
 *
 * Returns:
 *     Description of the return value.
 *
 * Raises:
 *     Exception: When an error occurs.
 *
 * Side Effects:
 *     - May mutate internal state.
 *
 * Code Paths:
 *     1. If preconditions met → executes normally.
 *     2. On error → logs and returns default.
 */


	public void captureStill(final String path) {
		super.captureStill(path);
	}
}
