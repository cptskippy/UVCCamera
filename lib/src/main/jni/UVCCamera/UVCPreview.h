/*
 * UVCCamera
 * library and sample to access to UVC web camera on non-rooted Android device
 *
 * Copyright (c) 2014-2017 saki t_saki@serenegiant.com
 *
 * File name: UVCPreview.h
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

/**
 * \brief Preview and capture management for UVC devices.
 *
 * Handles preview rendering to ANativeWindow, frame conversion, and
 * optional capture callbacks. Manages preview and capture threads,
 * frame pooling, and synchronization.
 *
 * Exports:
 *     UVCPreview: Preview/capture controller class.
 *
 * Dependencies:
 *     - libUVCCamera.h
 *     - pthread
 *     - android/native_window.h
 *     - objectarray.h
 *
 * Architecture Note:
 *     Preview runs on a dedicated pthread; capture runs on a separate
 *     thread when enabled. Frame pool reduces allocation churn.
 */

#ifndef UVCPREVIEW_H_
#define UVCPREVIEW_H_

#include "libUVCCamera.h"
#include <pthread.h>
#include <android/native_window.h>
#include "objectarray.h"

#pragma interface

#define DEFAULT_PREVIEW_WIDTH 640
#define DEFAULT_PREVIEW_HEIGHT 480
#define DEFAULT_PREVIEW_FPS_MIN 1
#define DEFAULT_PREVIEW_FPS_MAX 30
#define DEFAULT_PREVIEW_MODE 0
#define DEFAULT_BANDWIDTH 1.0f

typedef uvc_error_t (*convFunc_t)(uvc_frame_t *in, uvc_frame_t *out);

#define PIXEL_FORMAT_RAW 0		// same as PIXEL_FORMAT_YUV
#define PIXEL_FORMAT_YUV 1
#define PIXEL_FORMAT_RGB565 2
#define PIXEL_FORMAT_RGBX 3
#define PIXEL_FORMAT_YUV20SP 4
#define PIXEL_FORMAT_NV21 5		// YVU420SemiPlanar

// for callback to Java object
typedef struct {
	jmethodID onFrame;
} Fields_iframecallback;

/**
 * \brief Manage UVC preview rendering and frame capture.
 *
 * Controls preview streaming to a native window and optional capture
 * via surface or Java callback. Handles thread lifecycle, frame pooling,
 * and pixel format conversion.
 *
 * Lifecycle:
 *     Construct with device handle → setPreviewSize/Display → startPreview
 *     → stopPreview → destroy.
 *
 * State Machine:
 *     Idle → Previewing → Idle
 *     Idle → Capturing → Idle
 *
 * Thread Safety:
 *     Not thread-safe. All public methods must be called from the same thread.
 */
class UVCPreview {
private:
	uvc_device_handle_t *mDeviceHandle;
	ANativeWindow *mPreviewWindow;
	volatile bool mIsRunning;
	int requestWidth, requestHeight, requestMode;
	int requestMinFps, requestMaxFps;
	float requestBandwidth;
	int frameWidth, frameHeight;
	int frameMode;
	size_t frameBytes;
	pthread_t preview_thread;
	pthread_mutex_t preview_mutex;
	pthread_cond_t preview_sync;
	ObjectArray<uvc_frame_t *> previewFrames;
	int previewFormat;
	size_t previewBytes;
//
	volatile bool mIsCapturing;
	ANativeWindow *mCaptureWindow;
	pthread_t capture_thread;
	pthread_mutex_t capture_mutex;
	pthread_cond_t capture_sync;
	uvc_frame_t *captureQueu;			// keep latest frame
	jobject mFrameCallbackObj;
	convFunc_t mFrameCallbackFunc;
	Fields_iframecallback iframecallback_fields;
	int mPixelFormat;
	size_t callbackPixelBytes;
// improve performance by reducing memory allocation
	pthread_mutex_t pool_mutex;
	ObjectArray<uvc_frame_t *> mFramePool;
	uvc_frame_t *get_frame(size_t data_bytes);
	void recycle_frame(uvc_frame_t *frame);
	void init_pool(size_t data_bytes);
	void clear_pool();
//
	void clearDisplay();
	static void uvc_preview_frame_callback(uvc_frame_t *frame, void *vptr_args);
	void addPreviewFrame(uvc_frame_t *frame);
	uvc_frame_t *waitPreviewFrame();
	void clearPreviewFrame();
	static void *preview_thread_func(void *vptr_args);
	int prepare_preview(uvc_stream_ctrl_t *ctrl);
	void do_preview(uvc_stream_ctrl_t *ctrl);
	uvc_frame_t *draw_preview_one(uvc_frame_t *frame, ANativeWindow **window, convFunc_t func, int pixelBytes);
//
	void addCaptureFrame(uvc_frame_t *frame);
	uvc_frame_t *waitCaptureFrame();
	void clearCaptureFrame();
	static void *capture_thread_func(void *vptr_args);
	void do_capture(JNIEnv *env);
	void do_capture_surface(JNIEnv *env);
	void do_capture_idle_loop(JNIEnv *env);
	void do_capture_callback(JNIEnv *env, uvc_frame_t *frame);
	void callbackPixelFormatChanged();
public:
	/**
	 * \brief Construct preview controller for a UVC device.
	 *
	 * Initializes internal state and frame pool. Device handle must remain
	 * valid for the lifetime of the preview object.
	 *
	 * \param[in] devh Valid UVC device handle.
	 *
	 * \pre devh must be a valid, open UVC device handle.
	 * \post Preview object is constructed but not running.
	 */
	UVCPreview(uvc_device_handle_t *devh);
	/**
	 * \brief Destroy preview controller.
	 *
	 * Stops preview and capture threads, releases resources, and frees
	 * frame pool.
	 */
	~UVCPreview();

	/**
	 * \brief Query if preview is running.
	 *
	 * \return true if preview thread is active.
	 */
	inline const bool isRunning() const;
	/**
	 * \brief Configure preview size and frame rate.
	 *
	 * Sets requested width, height, fps range, mode and bandwidth for
	 * the next preview start. Must be called before startPreview.
	 *
	 * \param[in] width Requested preview width in pixels.
	 * \param[in] height Requested preview height in pixels.
	 * \param[in] min_fps Minimum acceptable fps.
	 * \param[in] max_fps Maximum acceptable fps.
	 * \param[in] mode Stream mode selector.
	 * \param[in] bandwidth Bandwidth multiplier.
	 * \return 0 on success, negative error code on failure.
	 *
	 * \pre Device must be connected.
	 *
	 * Side Effects:
	 *     - Updates internal request parameters.
	 *
	 * Code Paths:
	 *     1. Valid parameters → stores request, returns 0.
	 *     2. Invalid parameters → returns error.
	 */
	int setPreviewSize(int width, int height, int min_fps, int max_fps, int mode, float bandwidth = 1.0f);
	/**
	 * \brief Set native window for preview rendering.
	 *
	 * \param[in] preview_window Target ANativeWindow for preview.
	 * \return 0 on success, negative on failure.
	 *
	 * \pre preview_window must be valid if non-NULL.
	 */
	int setPreviewDisplay(ANativeWindow *preview_window);
	/**
	 * \brief Set Java frame callback for capture.
	 *
	 * Registers a Java object to receive frames in the specified pixel format.
	 *
	 * \param[in] env JNI environment.
	 * \param[in] frame_callback_obj Java callback object.
	 * \param[in] pixel_format Desired pixel format.
	 * \return 0 on success, negative on failure.
	 *
	 * \pre env must be valid.
	 *
	 * Side Effects:
	 *     - Stores global reference to callback object.
	 */
	int setFrameCallback(JNIEnv *env, jobject frame_callback_obj, int pixel_format);
	/**
	 * \brief Start preview streaming.
	 *
	 * Spawns preview thread and begins delivering frames to the configured
	 * window. Returns immediately.
	 *
	 * \return 0 on success, negative if already running or error.
	 *
	 * \pre Preview size and display must be configured.
	 *
	 * Side Effects:
	 *     - Starts preview_thread.
	 *     - Changes state to running.
	 *
	 * Code Paths:
	 *     1. Not running → starts thread, returns 0.
	 *     2. Already running → returns error.
	 */
	int startPreview();
	/**
	 * \brief Stop preview streaming.
	 *
	 * Signals preview thread to exit and waits for termination.
	 *
	 * \return 0 on success.
	 *
	 * Side Effects:
	 *     - Stops preview_thread.
	 *     - Clears display.
	 */
	int stopPreview();
	/**
	 * \brief Query if capture is active.
	 *
	 * \return true if capture thread is active.
	 */
	inline const bool isCapturing() const;
	/**
	 * \brief Set native window for capture output.
	 *
	 * \param[in] capture_window Target ANativeWindow for capture.
	 * \return 0 on success.
	 */
	int setCaptureDisplay(ANativeWindow *capture_window);
};

#endif /* UVCPREVIEW_H_ */
