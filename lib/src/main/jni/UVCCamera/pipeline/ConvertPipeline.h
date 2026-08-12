//
// Created by saki on 15/11/05.
//

/**
 * \brief Pipeline that converts frames to target pixel format.
 *
 * Receives frames and converts them using a conversion function before forwarding.
 *
 * Exports:
 *     ConvertPipeline: Format conversion pipeline.
 *
 * Dependencies:
 *     - libUVCCamera.h: UVC frame types and conversion functions.
 *     - AbstractBufferedPipeline.h: Buffered pipeline base.
 *
 * Architecture Note:
 *     Used to adapt camera output format to application requirements.
 */

#ifndef PUPILMOBILE_CONVERTPIPELINE_H
#define PUPILMOBILE_CONVERTPIPELINE_H

#include "libUVCCamera.h"
#include "AbstractBufferedPipeline.h"

/**
 * \brief Converts frames to a target pixel format.
 *
 * Uses conversion function selected based on target_pixel_format.
 *
 * Lifecycle:
 *     Construction → start → running → stop → release
 *
 * Thread Safety:
 *     Conversion runs on handler thread. target_pixel_format is immutable.
 *
 * Properties:
 *     target_pixel_format: Desired output pixel format.
 */
class ConvertPipeline : virtual public AbstractBufferedPipeline {
private:
	const int target_pixel_format;
	convFunc_t mFrameConvFunc;
	/**
	 * \brief Update conversion function based on target format.
	 *
	 * \pre None
	 * \post mFrameConvFunc set appropriately.
	 */
	void updateConvFunc();
protected:
	/**
	 * \brief Called when pipeline starts.
	 *
	 * \pre None
	 * \post Conversion function initialized.
	 */
	virtual void on_start();
	/**
	 * \brief Called when pipeline stops.
	 *
	 * \pre Pipeline running.
	 * \post Conversion resources released.
	 */
	virtual void on_stop();
	/**
	 * \brief Handle frame and convert to target format.
	 *
	 * \param[in] frame Input frame.
	 *
	 * \return 0 on success.
	 *
	 * Side Effects:
	 *     - Converts frame pixels.
	 *     - Forwards converted frame downstream.
	 */
	virtual int handle_frame(uvc_frame_t *frame);
public:
	/**
	 * \brief Construct convert pipeline.
	 *
	 * \param[in] _data_bytes Default frame buffer size.
	 * \param[in] target_pixel_format Desired output pixel format.
	 *
	 * \pre None
	 * \post Conversion function not yet initialized.
	 */
	ConvertPipeline(const size_t &_data_bytes, const int &target_pixel_format = PIXEL_FORMAT_RAW);
	/**
	 * \brief Destroy convert pipeline.
	 */
	virtual ~ConvertPipeline();
};


#endif //PUPILMOBILE_CONVERTPIPELINE_H
