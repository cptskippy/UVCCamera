//
// Created by saki on 15/11/07.
//

/**
 * \brief Pipeline that delivers frames via Java callback.
 *
 * Captures frames and invokes a Java callback object with converted pixel data.
 *
 * Exports:
 *     CallbackPipeline: Callback delivery pipeline.
 *
 * Dependencies:
 *     - libUVCCamera.h: UVC frame types and callback definitions.
 *     - CaptureBasePipeline.h: Base capture pipeline.
 *
 * Architecture Note:
 *     Used for delivering frames to Java/Kotlin code. Callback must be set before start.
 */

#ifndef PUPILMOBILE_CALLBACKPIPELINE_H
#define PUPILMOBILE_CALLBACKPIPELINE_H

#include "libUVCCamera.h"
#include "CaptureBasePipeline.h"

/**
 * \brief Delivers frames to Java callback object.
 *
 * Captures frames and invokes Java callback with pixel data in requested format.
 *
 * Lifecycle:
 *     Construction → setFrameCallback → start → running → stop → release
 *
 * Thread Safety:
 *     setFrameCallback must be called on JNI thread before start.
 *     do_capture runs on capture thread and calls Java via JNI.
 *
 * Properties:
 *     mFrameCallbackObj: Java object receiving frames.
 *     mPixelFormat: Target pixel format for callback.
 */
class CallbackPipeline : virtual public CaptureBasePipeline {
private:
	jobject mFrameCallbackObj;
	convFunc_t mFrameCallbackFunc;
	Fields_iframecallback iframecallback_fields;
	int mPixelFormat;
	size_t callbackPixelBytes;
	/**
	 * \brief Update callback pixel byte size on format change.
	 *
	 * \param[in] width Frame width.
	 * \param[in] height Frame height.
	 *
	 * \pre None
	 * \post callbackPixelBytes updated.
	 */
	void callbackPixelFormatChanged(const uint32_t &width, const uint32_t &height);
protected:
	/**
	 * \brief Perform capture and invoke Java callback.
	 *
	 * \param[in] env JNI environment.
	 *
	 * \pre mFrameCallbackObj must be set.
	 * \post Callback invoked with latest frame.
	 *
	 * Side Effects:
	 *     - Calls Java method via JNI.
	 */
	virtual void do_capture(JNIEnv *env);
public:
	/**
	 * \brief Construct callback pipeline.
	 *
	 * \param[in] _data_bytes Default frame buffer size.
	 *
	 * \pre None
	 * \post mFrameCallbackObj is NULL.
	 */
	CallbackPipeline(const size_t &_data_bytes = DEFAULT_FRAME_SZ);
	/**
	 * \brief Destroy callback pipeline.
	 *
	 * Side Effects:
	 *     - Releases global reference to callback object.
	 */
	virtual ~CallbackPipeline();
	/**
	 * \brief Set Java callback object and pixel format.
	 *
	 * \param[in] env JNI environment.
	 * \param[in] frame_callback_obj Java object implementing callback interface.
	 * \param[in] pixel_format Target pixel format for frames.
	 *
	 * \return 0 on success.
	 *
	 * \pre env must be valid, frame_callback_obj must be non-NULL.
	 * \post Callback object stored globally.
	 *
	 * \warning Must be called before start. Changing callback while running is unsafe.
	 *
	 * Side Effects:
	 *     - Creates global JNI reference to frame_callback_obj.
	 */
	int setFrameCallback(JNIEnv *env, jobject frame_callback_obj, int pixel_format);
};

#endif //PUPILMOBILE_CALLBACKPIPELINE_H
