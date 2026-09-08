/*
 * UVCCamera
 * library and sample to access to UVC web camera on non-rooted Android device
 *
 * Copyright (c) 2014-2017 saki t_saki@serenegiant.com
 *
 * File name: UVCPreview.cpp
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 * All files in the folder are under this Apache License, Version 2.0.
 * Files in the jni/libjpeg, jni/libusb, jin/libuvc, jni/rapidjson folder may have a different license, see the respective files.
*/

// Implementation for UVCPreview.h; see the header for the preview and capture API.



#include <stdlib.h>
#include <linux/time.h>
#include <unistd.h>

#if 1	// set 1 if you don't need debug log
	#ifndef LOG_NDEBUG
		#define	LOG_NDEBUG		// w/o LOGV/LOGD/MARK
	#endif
	#undef USE_LOGALL
#else
	#define USE_LOGALL
	#undef LOG_NDEBUG
//	#undef NDEBUG
#endif

#include "utilbase.h"
#include "UVCPreview.h"
#include "libuvc_internal.h"

#define	LOCAL_DEBUG 0
#define MAX_FRAME 4
#define PREVIEW_PIXEL_BYTES 4	// RGBA/RGBX
#define FRAME_POOL_SZ MAX_FRAME + 2

UVCPreview::UVCPreview(uvc_device_handle_t *devh)
:	mPreviewWindow(NULL),
	mCaptureWindow(NULL),
	mDeviceHandle(devh),
	requestWidth(DEFAULT_PREVIEW_WIDTH),
	requestHeight(DEFAULT_PREVIEW_HEIGHT),
	requestMinFps(DEFAULT_PREVIEW_FPS_MIN),
	requestMaxFps(DEFAULT_PREVIEW_FPS_MAX),
	requestMode(DEFAULT_PREVIEW_MODE),
	requestBandwidth(DEFAULT_BANDWIDTH),
	frameWidth(DEFAULT_PREVIEW_WIDTH),
	frameHeight(DEFAULT_PREVIEW_HEIGHT),
	frameBytes(DEFAULT_PREVIEW_WIDTH * DEFAULT_PREVIEW_HEIGHT * 2),	// YUYV
	frameMode(0),
	previewBytes(DEFAULT_PREVIEW_WIDTH * DEFAULT_PREVIEW_HEIGHT * PREVIEW_PIXEL_BYTES),
	previewFormat(WINDOW_FORMAT_RGBA_8888),
	mIsRunning(false),
	mIsCapturing(false),
	captureQueu(NULL),
	mFrameCallbackObj(NULL),
	mFrameCallbackFunc(NULL),
	callbackPixelBytes(2) {

	ENTER();
	pthread_cond_init(&preview_sync, NULL);
	pthread_mutex_init(&preview_mutex, NULL);
//
	pthread_cond_init(&capture_sync, NULL);
	pthread_mutex_init(&capture_mutex, NULL);
//	
	pthread_mutex_init(&pool_mutex, NULL);
	EXIT();
}

UVCPreview::~UVCPreview() {

	ENTER();
	if (mPreviewWindow)
		ANativeWindow_release(mPreviewWindow);
	mPreviewWindow = NULL;
	if (mCaptureWindow)
		ANativeWindow_release(mCaptureWindow);
	mCaptureWindow = NULL;
	clearPreviewFrame();
	clearCaptureFrame();
	clear_pool();
	pthread_mutex_destroy(&preview_mutex);
	pthread_cond_destroy(&preview_sync);
	pthread_mutex_destroy(&capture_mutex);
	pthread_cond_destroy(&capture_sync);
	pthread_mutex_destroy(&pool_mutex);
	EXIT();
}

/**
 * get uvc_frame_t from frame pool
 * if pool is empty, create new frame
 * this function does not confirm the frame size
 * and you may need to confirm the size
 */
uvc_frame_t *UVCPreview::get_frame(size_t data_bytes) {
	uvc_frame_t *frame = NULL;
	pthread_mutex_lock(&pool_mutex);
	{
		if (!mFramePool.isEmpty()) {
			frame = mFramePool.last();
		}
	}
	pthread_mutex_unlock(&pool_mutex);
	if UNLIKELY(!frame) {
		LOGW("allocate new frame");
		frame = uvc_allocate_frame(data_bytes);
	}
	return frame;
}

void UVCPreview::recycle_frame(uvc_frame_t *frame) {
	pthread_mutex_lock(&pool_mutex);
	if (LIKELY(mFramePool.size() < FRAME_POOL_SZ)) {
		mFramePool.put(frame);
		frame = NULL;
	}
	pthread_mutex_unlock(&pool_mutex);
	if (UNLIKELY(frame)) {
		uvc_free_frame(frame);
	}
}


void UVCPreview::init_pool(size_t data_bytes) {
	ENTER();

	clear_pool();
	pthread_mutex_lock(&pool_mutex);
	{
		for (int i = 0; i < FRAME_POOL_SZ; i++) {
			mFramePool.put(uvc_allocate_frame(data_bytes));
		}
	}
	pthread_mutex_unlock(&pool_mutex);

	EXIT();
}

void UVCPreview::clear_pool() {
	ENTER();

	pthread_mutex_lock(&pool_mutex);
	{
		const int n = mFramePool.size();
		for (int i = 0; i < n; i++) {
			uvc_free_frame(mFramePool[i]);
		}
		mFramePool.clear();
	}
	pthread_mutex_unlock(&pool_mutex);
	EXIT();
}

inline const bool UVCPreview::isRunning() const {return mIsRunning; }

/**
 * \brief Update requested preview parameters and validate them against the device.
 *
 * Re-queries the device stream control only when the requested width,
 * height, or mode changes.
 *
 * \param[in] width Requested preview width.
 * \param[in] height Requested preview height.
 * \param[in] min_fps Minimum acceptable frame rate.
 * \param[in] max_fps Maximum acceptable frame rate.
 * \param[in] mode Frame format selector: 0 for YUYV, non-zero for MJPEG.
 * \param[in] bandwidth Requested bandwidth multiplier.
 * \return 0 when no re-query is needed, otherwise the libuvc result from stream control negotiation.
 *
 * Code Paths:
 *   1. Width, height, and mode are unchanged → return 0 without updating fps/bandwidth or querying the device.
 *   2. Width, height, or mode changed → store all request fields and query a matching stream control.
 *   3. Device query returns an error → propagate the libuvc error.
 */
int UVCPreview::setPreviewSize(int width, int height, int min_fps, int max_fps, int mode, float bandwidth) {
	ENTER();
	
	int result = 0;
	if ((requestWidth != width) || (requestHeight != height) || (requestMode != mode)) {
		requestWidth = width;
		requestHeight = height;
		requestMinFps = min_fps;
		requestMaxFps = max_fps;
		requestMode = mode;
		requestBandwidth = bandwidth;

		uvc_stream_ctrl_t ctrl;
		result = uvc_get_stream_ctrl_format_size_fps(mDeviceHandle, &ctrl,
			!requestMode ? UVC_FRAME_FORMAT_YUYV : UVC_FRAME_FORMAT_MJPEG,
			requestWidth, requestHeight, requestMinFps, requestMaxFps);
	}
	
	RETURN(result, int);
}

/**
 * \brief Set or clear the preview output window.
 *
 * Holds `preview_mutex` while replacing the preview surface so the preview
 * thread sees a consistent window pointer.
 *
 * \param[in] preview_window Target preview `ANativeWindow`, or NULL to clear preview output.
 * \return 0.
 *
 * Code Paths:
 *   1. Window pointer unchanged → return without releasing or configuring a surface.
 *   2. Window pointer changed → release the previous window if present and assign the new pointer.
 *   3. New window present → set buffer geometry using the current frame size and `previewFormat`.
 */
int UVCPreview::setPreviewDisplay(ANativeWindow *preview_window) {
	ENTER();
	pthread_mutex_lock(&preview_mutex);
	{
		if (mPreviewWindow != preview_window) {
			if (mPreviewWindow)
				ANativeWindow_release(mPreviewWindow);
			mPreviewWindow = preview_window;
			if (LIKELY(mPreviewWindow)) {
				ANativeWindow_setBuffersGeometry(mPreviewWindow,
					frameWidth, frameHeight, previewFormat);
			}
		}
	}
	pthread_mutex_unlock(&preview_mutex);
	RETURN(0, int);
}

/**
 * \brief Register or replace the Java frame callback.
 *
 * Holds `capture_mutex` while updating callback state so the capture thread
 * cannot observe a half-updated callback object, method ID, or pixel format.
 *
 * \param[in] env JNI environment.
 * \param[in] frame_callback_obj Global Java callback reference, or NULL to clear the callback.
 * \param[in] pixel_format Callback pixel format used to select conversion state.
 * \return 0.
 *
 * \pre `frame_callback_obj` is a global reference when non-NULL.
 *
 * Code Paths:
 *   1. Preview is running, capture is active, and a callback was registered → pause capture and wait for the capture thread to leave the callback path.
 *   2. Callback object unchanged → skip reference and method ID updates.
 *   3. Callback object changed → clear the cached `onFrame` method ID, delete the previous global reference if present, and store the new pointer.
 *   4. New object present → resolve the `IFrameCallback#onFrame(Ljava/nio/ByteBuffer;)V` method ID.
 *   5. Class or method resolution fails → clear any pending JNI exception, delete the new global reference, and reset the callback state.
 *   6. New object successfully resolved → update `mPixelFormat` and refresh the conversion function.
 */
int UVCPreview::setFrameCallback(JNIEnv *env, jobject frame_callback_obj, int pixel_format) {
	
	ENTER();
	pthread_mutex_lock(&capture_mutex);
	{
		if (isRunning() && isCapturing()) {
			mIsCapturing = false;
			if (mFrameCallbackObj) {
				pthread_cond_signal(&capture_sync);
				pthread_cond_wait(&capture_sync, &capture_mutex);	// wait finishing capturing
			}
		}
		if (!env->IsSameObject(mFrameCallbackObj, frame_callback_obj))	{
			iframecallback_fields.onFrame = NULL;
			if (mFrameCallbackObj) {
				env->DeleteGlobalRef(mFrameCallbackObj);
			}
			mFrameCallbackObj = frame_callback_obj;
			if (frame_callback_obj) {
				// get method IDs of Java object for callback
				jclass clazz = env->GetObjectClass(frame_callback_obj);
				if (LIKELY(clazz)) {
					iframecallback_fields.onFrame = env->GetMethodID(clazz,
						"onFrame",	"(Ljava/nio/ByteBuffer;)V");
				} else {
					LOGW("failed to get object class");
				}
				env->ExceptionClear();
				if (!iframecallback_fields.onFrame) {
					LOGE("Can't find IFrameCallback#onFrame");
					env->DeleteGlobalRef(frame_callback_obj);
					mFrameCallbackObj = frame_callback_obj = NULL;
				}
			}
		}
		if (frame_callback_obj) {
			mPixelFormat = pixel_format;
			callbackPixelFormatChanged();
		}
	}
	pthread_mutex_unlock(&capture_mutex);
	RETURN(0, int);
}

/**
 * \brief Select the callback conversion function and output frame size.
 *
 * Called after the callback pixel format changes. Determines whether the
 * capture callback receives raw YUYV data or a converted frame.
 *
 * Code Paths:
 *   1. `PIXEL_FORMAT_RAW` or `PIXEL_FORMAT_YUV` → no conversion, callback size is `width * height * 2`.
 *   2. `PIXEL_FORMAT_RGB565` → use `uvc_any2rgb565`, callback size is `width * height * 2`.
 *   3. `PIXEL_FORMAT_RGBX` → use `uvc_any2rgbx`, callback size is `width * height * 4`.
 *   4. `PIXEL_FORMAT_YUV20SP` or `PIXEL_FORMAT_NV21` → use the matching YUYV-to-semi-planar converter, callback size is `width * height * 3 / 2`.
 *   5. Unrecognized format → clear the conversion function and leave `callbackPixelBytes` unchanged.
 */
void UVCPreview::callbackPixelFormatChanged() {
	mFrameCallbackFunc = NULL;
	const size_t sz = requestWidth * requestHeight;
	switch (mPixelFormat) {
	  case PIXEL_FORMAT_RAW:
		LOGI("PIXEL_FORMAT_RAW:");
		callbackPixelBytes = sz * 2;
		break;
	  case PIXEL_FORMAT_YUV:
		LOGI("PIXEL_FORMAT_YUV:");
		callbackPixelBytes = sz * 2;
		break;
	  case PIXEL_FORMAT_RGB565:
		LOGI("PIXEL_FORMAT_RGB565:");
		mFrameCallbackFunc = uvc_any2rgb565;
		callbackPixelBytes = sz * 2;
		break;
	  case PIXEL_FORMAT_RGBX:
		LOGI("PIXEL_FORMAT_RGBX:");
		mFrameCallbackFunc = uvc_any2rgbx;
		callbackPixelBytes = sz * 4;
		break;
	  case PIXEL_FORMAT_YUV20SP:
		LOGI("PIXEL_FORMAT_YUV20SP:");
		mFrameCallbackFunc = uvc_yuyv2iyuv420SP;
		callbackPixelBytes = (sz * 3) / 2;
		break;
	  case PIXEL_FORMAT_NV21:
		LOGI("PIXEL_FORMAT_NV21:");
		mFrameCallbackFunc = uvc_yuyv2yuv420SP;
		callbackPixelBytes = (sz * 3) / 2;
		break;
	}
}

/**
 * \brief Clear the preview and capture surfaces.
 *
 * Zeroes the visible contents of each installed native window using
 * stride-aware row writes.
 *
 * Code Paths:
 *   1. Acquire `capture_mutex`; if `mCaptureWindow` exists and can be locked, zero each row and post the buffer.
 *   2. Acquire `preview_mutex`; if `mPreviewWindow` exists and can be locked, zero each row and post the buffer.
 *   3. Missing window or failed `ANativeWindow_lock` → skip that surface.
 */
void UVCPreview::clearDisplay() {
	ENTER();

	ANativeWindow_Buffer buffer;
	pthread_mutex_lock(&capture_mutex);
	{
		if (LIKELY(mCaptureWindow)) {
			if (LIKELY(ANativeWindow_lock(mCaptureWindow, &buffer, NULL) == 0)) {
				uint8_t *dest = (uint8_t *)buffer.bits;
				const size_t bytes = buffer.width * PREVIEW_PIXEL_BYTES;
				const int stride = buffer.stride * PREVIEW_PIXEL_BYTES;
				for (int i = 0; i < buffer.height; i++) {
					memset(dest, 0, bytes);
					dest += stride;
				}
				ANativeWindow_unlockAndPost(mCaptureWindow);
			}
		}
	}
	pthread_mutex_unlock(&capture_mutex);
	pthread_mutex_lock(&preview_mutex);
	{
		if (LIKELY(mPreviewWindow)) {
			if (LIKELY(ANativeWindow_lock(mPreviewWindow, &buffer, NULL) == 0)) {
				uint8_t *dest = (uint8_t *)buffer.bits;
				const size_t bytes = buffer.width * PREVIEW_PIXEL_BYTES;
				const int stride = buffer.stride * PREVIEW_PIXEL_BYTES;
				for (int i = 0; i < buffer.height; i++) {
					memset(dest, 0, bytes);
					dest += stride;
				}
				ANativeWindow_unlockAndPost(mPreviewWindow);
			}
		}
	}
	pthread_mutex_unlock(&preview_mutex);

	EXIT();
}

/**
 * \brief Start the preview thread.
 *
 * Starts `preview_thread_func` only when a preview window is installed and
 * preview is not already running.
 *
 * \return `EXIT_SUCCESS` when the preview thread starts, otherwise `EXIT_FAILURE`.
 *
 * Code Paths:
 *   1. Preview already running → return `EXIT_FAILURE` without changing state.
 *   2. Preview not running → set `mIsRunning` and inspect `mPreviewWindow` under `preview_mutex`.
 *   3. Preview window present → create `preview_thread`.
 *   4. No preview window or thread creation fails → clear `mIsRunning`, signal `preview_sync`, and return `EXIT_FAILURE`.
 */
int UVCPreview::startPreview() {
	ENTER();

	int result = EXIT_FAILURE;
	if (!isRunning()) {
		mIsRunning = true;
		pthread_mutex_lock(&preview_mutex);
		{
			if (LIKELY(mPreviewWindow)) {
				result = pthread_create(&preview_thread, NULL, preview_thread_func, (void *)this);
			}
		}
		pthread_mutex_unlock(&preview_mutex);
		if (UNLIKELY(result != EXIT_SUCCESS)) {
			LOGW("UVCCamera::window does not exist/already running/could not create thread etc.");
			mIsRunning = false;
			pthread_mutex_lock(&preview_mutex);
			{
				pthread_cond_signal(&preview_sync);
			}
			pthread_mutex_unlock(&preview_mutex);
		}
	}
	RETURN(result, int);
}

/**
 * \brief Stop preview streaming and release preview/capture windows.
 *
 * Signals both preview and capture threads to exit, joins them, and clears
 * queued frames and native window references.
 *
 * \return 0.
 *
 * Code Paths:
 *   1. Preview is not running → skip thread joins and display clearing, but still clear frames and release windows.
 *   2. Preview is running → clear `mIsRunning` and signal `preview_sync` and `capture_sync`.
 *   3. Join `capture_thread`, then `preview_thread`; join failures are logged as warnings.
 *   4. Clear any queued preview and capture frames.
 *   5. Release `mPreviewWindow` under `preview_mutex` and `mCaptureWindow` under `capture_mutex` if present.
 */
int UVCPreview::stopPreview() {
	ENTER();
	bool b = isRunning();
	if (LIKELY(b)) {
		mIsRunning = false;
		pthread_cond_signal(&preview_sync);
		pthread_cond_signal(&capture_sync);
		if (pthread_join(capture_thread, NULL) != EXIT_SUCCESS) {
			LOGW("UVCPreview::terminate capture thread: pthread_join failed");
		}
		if (pthread_join(preview_thread, NULL) != EXIT_SUCCESS) {
			LOGW("UVCPreview::terminate preview thread: pthread_join failed");
		}
		clearDisplay();
	}
	clearPreviewFrame();
	clearCaptureFrame();
	pthread_mutex_lock(&preview_mutex);
	if (mPreviewWindow) {
		ANativeWindow_release(mPreviewWindow);
		mPreviewWindow = NULL;
	}
	pthread_mutex_unlock(&preview_mutex);
	pthread_mutex_lock(&capture_mutex);
	if (mCaptureWindow) {
		ANativeWindow_release(mCaptureWindow);
		mCaptureWindow = NULL;
	}
	pthread_mutex_unlock(&capture_mutex);
	RETURN(0, int);
}

//**********************************************************************
//
//**********************************************************************
/**
 * \brief libuvc callback that queues valid incoming preview frames.
 *
 * Runs on the libuvc streaming thread. The callback copies the incoming
 * frame into the preview frame pool before returning so libuvc can reuse
 * its original frame safely.
 *
 * \param[in] frame Incoming libuvc frame.
 * \param[in] vptr_args Pointer to the owning `UVCPreview` instance.
 *
 * Code Paths:
 *   1. Preview pointer missing, preview not running, or frame data is invalid → return without queueing.
 *   2. Frame format or size does not match the negotiated preview format → discard the frame.
 *   3. Preview is running → allocate a pooled copy large enough for `frame->data_bytes`.
 *   4. Copy allocation or `uvc_duplicate_frame` fails → recycle any allocated copy and return.
 *   5. Duplicate succeeds → transfer ownership of the copy to `addPreviewFrame`.
 */
void UVCPreview::uvc_preview_frame_callback(uvc_frame_t *frame, void *vptr_args) {
	UVCPreview *preview = reinterpret_cast<UVCPreview *>(vptr_args);
	if UNLIKELY(!preview->isRunning() || !frame || !frame->frame_format || !frame->data || !frame->data_bytes) return;
	if (UNLIKELY(
		((frame->frame_format != UVC_FRAME_FORMAT_MJPEG) && (frame->actual_bytes < preview->frameBytes))
		|| (frame->width != preview->frameWidth) || (frame->height != preview->frameHeight) )) {

#if LOCAL_DEBUG
		LOGD("broken frame!:format=%d,actual_bytes=%d/%d(%d,%d/%d,%d)",
			frame->frame_format, frame->actual_bytes, preview->frameBytes,
			frame->width, frame->height, preview->frameWidth, preview->frameHeight);
#endif
		return;
	}
	if (LIKELY(preview->isRunning())) {
		uvc_frame_t *copy = preview->get_frame(frame->data_bytes);
		if (UNLIKELY(!copy)) {
#if LOCAL_DEBUG
			LOGE("uvc_callback:unable to allocate duplicate frame!");
#endif
			return;
		}
		uvc_error_t ret = uvc_duplicate_frame(frame, copy);
		if (UNLIKELY(ret)) {
			preview->recycle_frame(copy);
			return;
		}
		preview->addPreviewFrame(copy);
	}
}

void UVCPreview::addPreviewFrame(uvc_frame_t *frame) {

	pthread_mutex_lock(&preview_mutex);
	if (isRunning() && (previewFrames.size() < MAX_FRAME)) {
		previewFrames.put(frame);
		frame = NULL;
		pthread_cond_signal(&preview_sync);
	}
	pthread_mutex_unlock(&preview_mutex);
	if (frame) {
		recycle_frame(frame);
	}
}

uvc_frame_t *UVCPreview::waitPreviewFrame() {
	uvc_frame_t *frame = NULL;
	pthread_mutex_lock(&preview_mutex);
	{
		if (!previewFrames.size()) {
			pthread_cond_wait(&preview_sync, &preview_mutex);
		}
		if (LIKELY(isRunning() && previewFrames.size() > 0)) {
			frame = previewFrames.remove(0);
		}
	}
	pthread_mutex_unlock(&preview_mutex);
	return frame;
}

void UVCPreview::clearPreviewFrame() {
	pthread_mutex_lock(&preview_mutex);
	{
		for (int i = 0; i < previewFrames.size(); i++)
			recycle_frame(previewFrames[i]);
		previewFrames.clear();
	}
	pthread_mutex_unlock(&preview_mutex);
}

void *UVCPreview::preview_thread_func(void *vptr_args) {
	int result;

	ENTER();
	UVCPreview *preview = reinterpret_cast<UVCPreview *>(vptr_args);
	if (LIKELY(preview)) {
		uvc_stream_ctrl_t ctrl;
		result = preview->prepare_preview(&ctrl);
		if (LIKELY(!result)) {
			preview->do_preview(&ctrl);
		}
	}
	PRE_EXIT();
	pthread_exit(NULL);
}

/**
 * \brief Negotiate the preview stream format and update frame state.
 *
 * Queries the device for a stream control matching the requested preview
 * parameters and updates the frame dimensions, byte estimates, and preview
 * surface geometry before `do_preview` starts streaming.
 *
 * \param[out] ctrl Stream control populated by libuvc on success.
 * \return 0 on success, libuvc error code on negotiation failure.
 *
 * Code Paths:
 *   1. `uvc_get_stream_ctrl_format_size_fps` fails → log the negotiation failure and return the error.
 *   2. Stream control succeeds → query the frame descriptor for the negotiated control.
 *   3. Frame descriptor succeeds → update `frameWidth`/`frameHeight` and the preview window geometry under `preview_mutex`.
 *   4. Frame descriptor fails → fall back to the requested width and height.
 *   5. Success path → update `frameMode`, `frameBytes`, and `previewBytes` for the preview loop.
 */
int UVCPreview::prepare_preview(uvc_stream_ctrl_t *ctrl) {
	uvc_error_t result;

	ENTER();
	result = uvc_get_stream_ctrl_format_size_fps(mDeviceHandle, ctrl,
		!requestMode ? UVC_FRAME_FORMAT_YUYV : UVC_FRAME_FORMAT_MJPEG,
		requestWidth, requestHeight, requestMinFps, requestMaxFps
	);
	if (LIKELY(!result)) {
#if LOCAL_DEBUG
		uvc_print_stream_ctrl(ctrl, stderr);
#endif
		uvc_frame_desc_t *frame_desc;
		result = uvc_get_frame_desc(mDeviceHandle, ctrl, &frame_desc);
		if (LIKELY(!result)) {
			frameWidth = frame_desc->wWidth;
			frameHeight = frame_desc->wHeight;
			LOGI("frameSize=(%d,%d)@%s", frameWidth, frameHeight, (!requestMode ? "YUYV" : "MJPEG"));
			pthread_mutex_lock(&preview_mutex);
			if (LIKELY(mPreviewWindow)) {
				ANativeWindow_setBuffersGeometry(mPreviewWindow,
					frameWidth, frameHeight, previewFormat);
			}
			pthread_mutex_unlock(&preview_mutex);
		} else {
			frameWidth = requestWidth;
			frameHeight = requestHeight;
		}
		frameMode = requestMode;
		frameBytes = frameWidth * frameHeight * (!requestMode ? 2 : 4);
		previewBytes = frameWidth * frameHeight * PREVIEW_PIXEL_BYTES;
	} else {
		LOGE("could not negotiate with camera:err=%d", result);
	}
	RETURN(result, int);
}

/**
 * \brief Run the preview loop for an active UVC stream.
 *
 * Called from the preview thread after `prepare_preview` succeeds. Consumes
 * frames queued by `uvc_preview_frame_callback`, renders them to the preview
 * window when available, and forwards the latest YUYV frame to the capture
 * path.
 *
 * \param[in] ctrl Stream control negotiated by `prepare_preview`.
 *
 * Code Paths:
 *   1. `uvc_start_streaming_bandwidth` failure → log the libuvc error and return without starting the capture thread.
 *   2. Start success → clear any queued preview frames and start the capture thread.
 *   3. MJPEG mode (`frameMode != 0`) → for each queued MJPEG frame while `mIsRunning`:
 *      allocate a YUYV frame, decode MJPEG to YUYV, recycle the MJPEG frame,
 *      render the YUYV frame to `mPreviewWindow`, and enqueue it via `addCaptureFrame`.
 *   4. YUYV mode (`frameMode == 0`) → for each queued YUYV frame while `mIsRunning`:
 *      render the frame to `mPreviewWindow` and enqueue it via `addCaptureFrame`.
 *   5. MJPEG decode failure → recycle the converted YUYV frame and continue with the next queued frame.
 *   6. `mIsRunning` becomes false → exit the loop, signal the capture thread, and stop streaming.
 *
 * Frame Ownership:
 *   - Queued preview frames are consumed by this method.
 *   - Frames passed to `addCaptureFrame` are transferred to the capture queue.
 *   - `draw_preview_one` uses temporary converted frames internally and returns the original input frame.
 */
void UVCPreview::do_preview(uvc_stream_ctrl_t *ctrl) {
	ENTER();

	uvc_frame_t *frame = NULL;
	uvc_frame_t *frame_mjpeg = NULL;
	uvc_error_t result = uvc_start_streaming_bandwidth(
		mDeviceHandle, ctrl, uvc_preview_frame_callback, (void *)this, requestBandwidth, 0);

	if (LIKELY(!result)) {
		clearPreviewFrame();
		pthread_create(&capture_thread, NULL, capture_thread_func, (void *)this);

#if LOCAL_DEBUG
		LOGI("Streaming...");
#endif
		if (frameMode) {
			// MJPEG mode
			for ( ; LIKELY(isRunning()) ; ) {
				frame_mjpeg = waitPreviewFrame();
				if (LIKELY(frame_mjpeg)) {
					frame = get_frame(frame_mjpeg->width * frame_mjpeg->height * 2);
					result = uvc_mjpeg2yuyv(frame_mjpeg, frame);   // MJPEG => yuyv
					recycle_frame(frame_mjpeg);
					if (LIKELY(!result)) {
						frame = draw_preview_one(frame, &mPreviewWindow, uvc_any2rgbx, 4);
						addCaptureFrame(frame);
					} else {
						recycle_frame(frame);
					}
				}
			}
		} else {
			// yuvyv mode
			for ( ; LIKELY(isRunning()) ; ) {
				frame = waitPreviewFrame();
				if (LIKELY(frame)) {
					frame = draw_preview_one(frame, &mPreviewWindow, uvc_any2rgbx, 4);
					addCaptureFrame(frame);
				}
			}
		}
		pthread_cond_signal(&capture_sync);
#if LOCAL_DEBUG
		LOGI("preview_thread_func:wait for all callbacks complete");
#endif
		uvc_stop_streaming(mDeviceHandle);
#if LOCAL_DEBUG
		LOGI("Streaming finished");
#endif
	} else {
		uvc_perror(result, "failed start_streaming");
	}

	EXIT();
}

static void copyFrame(const uint8_t *src, uint8_t *dest, const int width, int height, const int stride_src, const int stride_dest) {
	const int h8 = height % 8;
	for (int i = 0; i < h8; i++) {
		memcpy(dest, src, width);
		dest += stride_dest; src += stride_src;
	}
	for (int i = 0; i < height; i += 8) {
		memcpy(dest, src, width);
		dest += stride_dest; src += stride_src;
		memcpy(dest, src, width);
		dest += stride_dest; src += stride_src;
		memcpy(dest, src, width);
		dest += stride_dest; src += stride_src;
		memcpy(dest, src, width);
		dest += stride_dest; src += stride_src;
		memcpy(dest, src, width);
		dest += stride_dest; src += stride_src;
		memcpy(dest, src, width);
		dest += stride_dest; src += stride_src;
		memcpy(dest, src, width);
		dest += stride_dest; src += stride_src;
		memcpy(dest, src, width);
		dest += stride_dest; src += stride_src;
	}
}


// transfer specific frame data to the Surface(ANativeWindow)
/**
 * \brief Copy frame pixels into an `ANativeWindow` buffer.
 *
 * Uses the smaller of the frame and surface dimensions and preserves source
 * and destination strides.
 *
 * \param[in] frame Source frame containing RGBA/RGBX pixel data.
 * \param[in] window Pointer to the target `ANativeWindow`, or NULL.
 * \return 0 on success, -1 if the window is missing or cannot be locked.
 *
 * Code Paths:
 *   1. `*window` is NULL → return -1.
 *   2. `ANativeWindow_lock` fails → return -1.
 *   3. Lock succeeds → copy the overlapping region row by row, then unlock and post the surface.
 */
int copyToSurface(uvc_frame_t *frame, ANativeWindow **window) {
	// ENTER();
	int result = 0;
	if (LIKELY(*window)) {
		ANativeWindow_Buffer buffer;
		if (LIKELY(ANativeWindow_lock(*window, &buffer, NULL) == 0)) {
			// source = frame data
			const uint8_t *src = (uint8_t *)frame->data;
			const int src_w = frame->width * PREVIEW_PIXEL_BYTES;
			const int src_step = frame->width * PREVIEW_PIXEL_BYTES;
			// destination = Surface(ANativeWindow)
			uint8_t *dest = (uint8_t *)buffer.bits;
			const int dest_w = buffer.width * PREVIEW_PIXEL_BYTES;
			const int dest_step = buffer.stride * PREVIEW_PIXEL_BYTES;
			// use lower transfer bytes
			const int w = src_w < dest_w ? src_w : dest_w;
			// use lower height
			const int h = frame->height < buffer.height ? frame->height : buffer.height;
			// transfer from frame data to the Surface
			copyFrame(src, dest, w, h, src_step, dest_step);
			ANativeWindow_unlockAndPost(*window);
		} else {
			result = -1;
		}
	} else {
		result = -1;
	}
	return result; //RETURN(result, int);
}

// changed to return original frame instead of returning converted frame even if convert_func is not null.
/**
 * \brief Render one preview frame to a native window.
 *
 * Converts the frame when required, copies it to the target surface, and
 * always returns the original input frame so ownership remains with the
 * caller.
 *
 * \param[in] frame Input preview frame.
 * \param[in] window Pointer to the target `ANativeWindow`, or NULL.
 * \param[in] convert_func Optional conversion function, or NULL for direct copy.
 * \param[in] pixcelBytes Output pixel size in bytes used to allocate a converted frame.
 * \return The original `frame` pointer.
 *
 * Code Paths:
 *   1. Acquire `preview_mutex` and check whether `*window` is present.
 *   2. `*window` is NULL → return the input frame without rendering.
 *   3. `convert_func` is non-null → allocate a conversion frame from the pool.
 *   4. Conversion allocation or conversion fails → recycle the conversion frame and return the input frame.
 *   5. Conversion succeeds → copy the converted frame to the surface and recycle the conversion frame.
 *   6. `convert_func` is NULL → copy the input frame directly to the surface.
 */
uvc_frame_t *UVCPreview::draw_preview_one(uvc_frame_t *frame, ANativeWindow **window, convFunc_t convert_func, int pixcelBytes) {
	// ENTER();

	int b = 0;
	pthread_mutex_lock(&preview_mutex);
	{
		b = *window != NULL;
	}
	pthread_mutex_unlock(&preview_mutex);
	if (LIKELY(b)) {
		uvc_frame_t *converted;
		if (convert_func) {
			converted = get_frame(frame->width * frame->height * pixcelBytes);
			if LIKELY(converted) {
				b = convert_func(frame, converted);
				if (!b) {
					pthread_mutex_lock(&preview_mutex);
					copyToSurface(converted, window);
					pthread_mutex_unlock(&preview_mutex);
				} else {
					LOGE("failed converting");
				}
				recycle_frame(converted);
			}
		} else {
			pthread_mutex_lock(&preview_mutex);
			copyToSurface(frame, window);
			pthread_mutex_unlock(&preview_mutex);
		}
	}
	return frame; //RETURN(frame, uvc_frame_t *);
}

//======================================================================
//
//======================================================================
inline const bool UVCPreview::isCapturing() const { return mIsCapturing; }

/**
 * \brief Set or clear the capture output window.
 *
 * Holds `capture_mutex` while changing the capture window so the capture
 * thread observes either the old or the new window, never a torn update.
 *
 * \param[in] capture_window Target capture `ANativeWindow`, or NULL to clear capture output.
 * \return 0.
 *
 * Code Paths:
 *   1. Preview is running, capture is active, and a capture window exists → pause capture and wait for the capture loop to finish.
 *   2. Window pointer unchanged → leave the current window in place.
 *   3. Window pointer changed → release the previous window if present and assign the new pointer.
 *   4. New window present → set buffer geometry and query the actual window format.
 *   5. Actual window format is not RGB565 while `previewFormat` is RGB565 → release the new window and clear `mCaptureWindow`.
 */
int UVCPreview::setCaptureDisplay(ANativeWindow *capture_window) {
	ENTER();
	pthread_mutex_lock(&capture_mutex);
	{
		if (isRunning() && isCapturing()) {
			mIsCapturing = false;
			if (mCaptureWindow) {
				pthread_cond_signal(&capture_sync);
				pthread_cond_wait(&capture_sync, &capture_mutex);	// wait finishing capturing
			}
		}
		if (mCaptureWindow != capture_window) {
			// release current Surface if already assigned.
			if (UNLIKELY(mCaptureWindow))
				ANativeWindow_release(mCaptureWindow);
			mCaptureWindow = capture_window;
			// if you use Surface came from MediaCodec#createInputSurface
			// you could not change window format at least when you use
			// ANativeWindow_lock / ANativeWindow_unlockAndPost
			// to write frame data to the Surface...
			// So we need check here.
			if (mCaptureWindow) {
				ANativeWindow_setBuffersGeometry(mCaptureWindow,
					frameWidth, frameHeight, previewFormat);
				int32_t window_format = ANativeWindow_getFormat(mCaptureWindow);
				if ((window_format != WINDOW_FORMAT_RGB_565)
					&& (previewFormat == WINDOW_FORMAT_RGB_565)) {
					LOGE("window format mismatch, cancelled movie capturing.");
					ANativeWindow_release(mCaptureWindow);
					mCaptureWindow = NULL;
				}
			}
		}
	}
	pthread_mutex_unlock(&capture_mutex);
	RETURN(0, int);
}

void UVCPreview::addCaptureFrame(uvc_frame_t *frame) {
	pthread_mutex_lock(&capture_mutex);
	if (LIKELY(isRunning())) {
		// keep only latest one
		if (captureQueu) {
			recycle_frame(captureQueu);
		}
		captureQueu = frame;
		pthread_cond_broadcast(&capture_sync);
	}
	pthread_mutex_unlock(&capture_mutex);
}

/**
 * get frame data for capturing, if not exist, block and wait
 */
uvc_frame_t *UVCPreview::waitCaptureFrame() {
	uvc_frame_t *frame = NULL;
	pthread_mutex_lock(&capture_mutex);
	{
		if (!captureQueu) {
			pthread_cond_wait(&capture_sync, &capture_mutex);
		}
		if (LIKELY(isRunning() && captureQueu)) {
			frame = captureQueu;
			captureQueu = NULL;
		}
	}
	pthread_mutex_unlock(&capture_mutex);
	return frame;
}

/**
 * clear drame data for capturing
 */
void UVCPreview::clearCaptureFrame() {
	pthread_mutex_lock(&capture_mutex);
	{
		if (captureQueu)
			recycle_frame(captureQueu);
		captureQueu = NULL;
	}
	pthread_mutex_unlock(&capture_mutex);
}

//======================================================================
/**
 * \brief Capture thread entry point.
 *
 * Attaches the capture thread to the Java VM, runs the capture loop until
 * `mIsRunning` is cleared, then detaches the thread.
 *
 * \param[in] vptr_args Pointer to the owning `UVCPreview` instance.
 *
 * Code Paths:
 *   1. `vptr_args` is null → skip the capture loop and exit the thread.
 *   2. `vptr_args` is valid → attach to `JavaVM`, call `do_capture`, detach, and exit.
 */
// static
void *UVCPreview::capture_thread_func(void *vptr_args) {
	int result;

	ENTER();
	UVCPreview *preview = reinterpret_cast<UVCPreview *>(vptr_args);
	if (LIKELY(preview)) {
		JavaVM *vm = getVM();
		JNIEnv *env;
		// attach to JavaVM
		vm->AttachCurrentThread(&env, NULL);
		preview->do_capture(env);	// never return until finish previewing
		// detach from JavaVM
		vm->DetachCurrentThread();
		MARK("DetachCurrentThread");
	}
	PRE_EXIT();
	pthread_exit(NULL);
}

/**
 * \brief Run the capture thread state loop.
 *
 * Called from `capture_thread_func` after the thread is attached to the
 * Java VM. Selects the capture path based on the current capture window and
 * keeps capture synchronized with `mIsRunning` and `mIsCapturing`.
 *
 * \param[in] env JNI environment for the attached capture thread.
 *
 * Code Paths:
 *   1. Entry → clear any queued capture frame and re-derive callback conversion state.
 *   2. While `isRunning()` → mark capture active and choose the active capture path.
 *   3. `mCaptureWindow` exists → run `do_capture_surface`.
 *   4. No capture window → run `do_capture_idle_loop`.
 *   5. After each sub-loop returns → broadcast `capture_sync` and re-evaluate running/capture state.
 */
void UVCPreview::do_capture(JNIEnv *env) {

	ENTER();

	clearCaptureFrame();
	callbackPixelFormatChanged();
	for (; isRunning() ;) {
		mIsCapturing = true;
		if (mCaptureWindow) {
			do_capture_surface(env);
		} else {
			do_capture_idle_loop(env);
		}
		pthread_cond_broadcast(&capture_sync);
	}	// end of for (; isRunning() ;)
	EXIT();
}

void UVCPreview::do_capture_idle_loop(JNIEnv *env) {
	ENTER();
	
	for (; isRunning() && isCapturing() ;) {
		do_capture_callback(env, waitCaptureFrame());
	}
	
	EXIT();
}

/**
 * \brief Capture loop that writes converted frames to the capture surface.
 *
 * Called from `do_capture` when a capture window is active. Consumes YUYV
 * frames from the capture queue, converts them to RGBX for surface output,
 * and forwards every frame to `do_capture_callback`.
 *
 * \param[in] env JNI environment for the attached capture thread.
 *
 * Code Paths:
 *   1. While `isRunning() && isCapturing()` → wait for a capture frame.
 *   2. Lazy conversion frame → allocate one RGBX-sized frame from the pool on first use.
 *   3. Conversion frame unavailable → skip surface copy but still deliver the original frame to the callback path.
 *   4. YUYV→RGBX conversion succeeds and `mCaptureWindow` exists → copy the converted frame to the capture surface.
 *   5. Each queued frame is passed to `do_capture_callback`, which owns recycling the callback frame.
 *   6. Loop exit → recycle the reused conversion frame and release `mCaptureWindow`.
 */
void UVCPreview::do_capture_surface(JNIEnv *env) {
	ENTER();

	uvc_frame_t *frame = NULL;
	uvc_frame_t *converted = NULL;
	char *local_picture_path;

	for (; isRunning() && isCapturing() ;) {
		frame = waitCaptureFrame();
		if (LIKELY(frame)) {
			// frame data is always YUYV format.
			if LIKELY(isCapturing()) {
				if (UNLIKELY(!converted)) {
					converted = get_frame(previewBytes);
				}
				if (LIKELY(converted)) {
					int b = uvc_any2rgbx(frame, converted);
					if (!b) {
						if (LIKELY(mCaptureWindow)) {
							copyToSurface(converted, &mCaptureWindow);
						}
					}
				}
			}
			do_capture_callback(env, frame);
		}
	}
	if (converted) {
		recycle_frame(converted);
	}
	if (mCaptureWindow) {
		ANativeWindow_release(mCaptureWindow);
		mCaptureWindow = NULL;
	}

	EXIT();
}

/**
 * \brief Deliver a captured frame to the registered Java `IFrameCallback`.
 *
 * Called from the capture thread with a YUYV frame taken from the capture
 * queue. The method holds `capture_mutex` for the entire JNI callback so that
 * `setFrameCallback` and `setCaptureDisplay` observe a stable callback state.
 *
 * \param[in] env JNI environment for the attached capture thread.
 * \param[in] frame YUYV frame from the capture queue, or NULL to do nothing.
 *
 * Code Paths:
 *   1. `frame == NULL` → release `capture_mutex` and return.
 *   2. No registered Java callback → recycle the frame and return.
 *   3. Pixel-format conversion is required (`mFrameCallbackFunc != NULL`):
 *      allocate a callback-sized frame, convert from YUYV, recycle the input
 *      frame, and pass the converted frame to Java.
 *   4. Conversion allocation or conversion fails → recycle available frame data
 *      and skip the JNI call.
 *   5. No conversion required → pass the original YUYV frame to Java through a
 *      direct `ByteBuffer`.
 *   6. After a successful or failed JNI call → clear any pending JNI exception,
 *      delete the local `ByteBuffer` reference, and recycle the callback frame.
 */
void UVCPreview::do_capture_callback(JNIEnv *env, uvc_frame_t *frame) {
	ENTER();
	pthread_mutex_lock(&capture_mutex);
	if (LIKELY(frame)) {
		uvc_frame_t *callback_frame = frame;
		if (mFrameCallbackObj) {
			if (mFrameCallbackFunc) {
				callback_frame = get_frame(callbackPixelBytes);
				if (LIKELY(callback_frame)) {
					int b = mFrameCallbackFunc(frame, callback_frame);
					recycle_frame(frame);
					if (UNLIKELY(b)) {
						LOGW("failed to convert for callback frame");
						goto SKIP;
					}
				} else {
					LOGW("failed to allocate for callback frame");
					callback_frame = frame;
					goto SKIP;
				}
			}
			jobject buf = env->NewDirectByteBuffer(callback_frame->data, callbackPixelBytes);
			env->CallVoidMethod(mFrameCallbackObj, iframecallback_fields.onFrame, buf);
			env->ExceptionClear();
			env->DeleteLocalRef(buf);
		}
 SKIP:
		recycle_frame(callback_frame);
	}
	pthread_mutex_unlock(&capture_mutex);
	EXIT();
}
