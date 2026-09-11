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

package com.serenegiant.usb;

import java.nio.ByteBuffer;
/**
 * Receive raw video frames as ByteBuffer from UVCCamera native capture pipeline.
 *
 * Implementations are invoked from the native capture thread via JNI for each
 * captured frame when frame callback mode is active. Register an implementation
 * with UVCCamera#setFrameCallback to receive frames.
 *
 * Usage:
 *     Prerequisites:
 *         - UVCCamera instance must be initialized and native pointer non-zero
 *     Call sequence:
 *         1. UVCCamera#setFrameCallback(callback, pixelFormat) — registers callback and selects pixel format
 *         2. Callback onFrame is invoked asynchronously on native capture thread for each frame
 *         3. Process frame quickly to avoid drops; avoid blocking work in callback
 *
 * Thread safety:
 *     - onFrame is called on the native capture thread. Do not perform blocking work.
 *       Copy frame data if processing must occur on another thread.
 *
 * Performance note:
 *     - Long processing in onFrame causes frame drops. Prefer SurfaceTexture/GL pipeline for efficient rendering.
 */
public interface IFrameCallback {
	/**
	 * Process a raw video frame delivered from native capture.
	 *
	 * Args:
	 *     frame: Direct ByteBuffer containing raw frame data in the pixel format selected via setFrameCallback.
	 *         Byte order and buffer limits must be handled by the caller. The buffer is valid only during callback execution.
	 *
	 * Side Effects:
	 *     - Execution blocks the native capture pipeline. Long processing causes frame drops.
	 *     - Caller is responsible for copying frame data if processing must continue after callback returns.
	 *
	 * Code Paths:
	 *     1. Frame arrives from native capture → callback invoked with ByteBuffer containing frame data.
	 *        Implementation should process frame promptly and return.
	 *
	 * Performance note:
	 *     - For color formats like NV21, library performs pixel format conversion only, no colorspace conversion.
	 *     - Prefer SurfaceTexture/GL pipeline for efficient on-screen rendering instead of CPU processing via callback.
	 */
	public void onFrame(ByteBuffer frame);
}
