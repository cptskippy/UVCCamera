//
// Created by saki on 15/11/06.
//

/**
 * \brief Pipeline for rendering UVC frames to an ANativeWindow.
 *
 * Captures frames from upstream and renders them to a native window using the capture thread.
 *
 * Exports:
 *     PreviewPipeline: Pipeline that displays preview frames.
 *
 * Dependencies:
 *     - android/native_window.h: Native window rendering.
 *     - libUVCCamera.h: UVC frame types.
 *     - CaptureBasePipeline.h: Base capture pipeline.
 *
 * Architecture Note:
 *     Used for live preview. Requires a valid ANativeWindow set via setCaptureDisplay before start.
 */

#ifndef PUPILMOBILE_PREVIEWPIPELINE_H
#define PUPILMOBILE_PREVIEWPIPELINE_H

#include <android/native_window.h>

#include "libUVCCamera.h"
#include "CaptureBasePipeline.h"

/**
 * \brief Renders frames to an ANativeWindow for preview.
 *
 * Captures latest frame and renders it to mCaptureWindow on capture thread.
 *
 * Lifecycle:
 *     Construction → setCaptureDisplay → start → running → stop → release
 *
 * Thread Safety:
 *     setCaptureDisplay must be called before start, preferably on main thread.
 *     do_capture runs on capture thread.
 *
 * Properties:
 *     mCaptureWindow: Target native window for rendering. Set via setCaptureDisplay.
 */
class PreviewPipeline : virtual public CaptureBasePipeline {
private:
	ANativeWindow *mCaptureWindow;
protected:
	/**
	 * \brief Perform frame capture and rendering.
	 *
	 * \param[in] env JNI environment for native window operations.
	 *
	 * \pre mCaptureWindow must be non-NULL.
	 * \post Frame rendered to window.
	 *
	 * Side Effects:
	 *     - Renders frame to ANativeWindow.
	 */
	virtual void do_capture(JNIEnv *env);
public:
	/**
	 * \brief Construct preview pipeline.
	 *
	 * \param[in] _data_bytes Default frame buffer size.
	 *
	 * \pre None
	 * \post mCaptureWindow is NULL.
	 */
	PreviewPipeline(const size_t &_data_bytes = DEFAULT_FRAME_SZ);
	/**
	 * \brief Destroy preview pipeline.
	 *
	 * Side Effects:
	 *     - Releases native window reference.
	 */
	virtual ~PreviewPipeline();
	/**
	 * \brief Set native window for preview rendering.
	 *
	 * \param[in] capture_window ANativeWindow to render frames to. May be NULL to clear.
	 *
	 * \return 0 on success.
	 *
	 * \pre capture_window must be valid or NULL.
	 * \post mCaptureWindow updated.
	 *
	 * \warning Must be called before start. Changing window while running is undefined.
	 *
	 * Side Effects:
	 *     - Updates mCaptureWindow pointer.
	 */
	int setCaptureDisplay(ANativeWindow *capture_window);
};


#endif //PUPILMOBILE_PREVIEWPIPELINE_H
