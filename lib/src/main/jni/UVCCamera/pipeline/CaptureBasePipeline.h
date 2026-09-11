//
// Created by saki on 15/11/07.
//

/**
 * \brief Base pipeline for capture operations with capture thread.
 *
 * Provides capture thread management and latest-frame queueing.
 *
 * Exports:
 *     CaptureBasePipeline: Base for capture pipelines.
 *
 * Dependencies:
 *     - libUVCCamera.h: UVC frame types.
 *     - AbstractBufferedPipeline.h: Buffered pipeline base.
 *     - Mutex.h/Condition.h: Threading primitives.
 *
 * Architecture Note:
 *     Used by PreviewPipeline and CallbackPipeline for JNI callbacks.
 */

#ifndef PUPILMOBILE_CAPTUREBASEPIPELINE_H
#define PUPILMOBILE_CAPTUREBASEPIPELINE_H

#include "Mutex.h"
#include "Condition.h"

#include "libUVCCamera.h"
#include "AbstractBufferedPipeline.h"

using namespace android;

/**
 * \brief Base for pipelines requiring capture thread.
 *
 * Manages capture thread and latest frame queue.
 *
 * Lifecycle:
 *     Construction → start → capturing → stop → release
 *
 * Thread Safety:
 *     Capture thread runs do_capture. capture_mutex protects captureQueue.
 *
 * Properties:
 *     mIsCapturing: Capture thread active flag.
 *     captureQueue: Latest frame for capture.
 */
class CaptureBasePipeline : virtual public AbstractBufferedPipeline {
private:
	/**
	 * \brief Capture thread entry point.
	 *
	 * \param[in] vptr_args Pointer to pipeline instance.
	 *
	 * \return NULL.
	 */
	static void *capture_thread_func(void *vptr_args);
	/**
	 * \brief Internal capture invocation.
	 *
	 * \param[in] env JNI environment.
	 */
	void internal_do_capture(JNIEnv *env);
protected:
	volatile bool mIsCapturing;
	mutable Mutex capture_mutex;
	Condition capture_sync;
	pthread_t capture_thread;
	uvc_frame_t *captureQueue;			// keep latest one frame only
	uint32_t frameWidth;
	uint32_t frameHeight;
	/**
	 * \brief Clear capture frame.
	 */
	void clearCaptureFrame();
	/**
	 * \brief Add frame to capture queue.
	 *
	 * \param[in] frame Frame to store.
	 */
	void addCaptureFrame(uvc_frame_t *frame);
	/**
	 * \brief Wait for capture frame.
	 *
	 * \return Frame pointer.
	 */
	uvc_frame_t *waitCaptureFrame();

	/**
	 * \brief Called on start.
	 */
	virtual void on_start();
	/**
	 * \brief Called on stop.
	 */
	virtual void on_stop();
	/**
	 * \brief Handle frame and update capture queue.
	 *
	 * \param[in] frame Frame to process.
	 *
	 * \return 0 on success.
	 */
	virtual int handle_frame(uvc_frame_t *frame);
	/**
	 * \brief Perform capture operation.
	 *
	 * \param[in] env JNI environment.
	 *
	 * \pre mIsCapturing true.
	 */
	virtual void do_capture(JNIEnv *env) = 0;
public:
	/**
	 * \brief Construct capture base pipeline with default size.
	 *
	 * \param[in] _data_bytes Default frame size.
	 */
	CaptureBasePipeline(const size_t &_data_bytes = DEFAULT_FRAME_SZ);
	/**
	 * \brief Construct capture base pipeline with buffer params.
	 *
	 * \param[in] _max_buffer_num Max buffer size.
	 * \param[in] init_pool_num Initial pool size.
	 * \param[in] default_frame_size Default frame size.
	 */
	CaptureBasePipeline(const int &_max_buffer_num = DEFAULT_MAX_FRAME_NUM, const int &init_pool_num = DEFAULT_INIT_FRAME_POOL_SZ, const size_t &default_frame_size = DEFAULT_FRAME_SZ);
	/**
	 * \brief Destroy capture base pipeline.
	 */
	virtual ~CaptureBasePipeline();
	/**
	 * \brief Check if capturing.
	 *
	 * \return true if capture thread active.
	 */
	const bool isCapturing() const;
};


#endif //PUPILMOBILE_CAPTUREBASEPIPELINE_H
