//
// Created by saki on 15/11/23.
//

/**
 * \brief Simple buffered pipeline with no additional processing.
 *
 * Buffers frames and forwards them downstream without modification.
 *
 * Exports:
 *     SimpleBufferedPipeline: Basic buffered pipeline.
 *
 * Dependencies:
 *     - AbstractBufferedPipeline.h: Buffered pipeline base.
 *
 * Architecture Note:
 *     Used as a pass-through buffer stage.
 */

#ifndef PUPILMOBILE_SIMPLEBUFFEREDPIPELINE_H
#define PUPILMOBILE_SIMPLEBUFFEREDPIPELINE_H

#include "AbstractBufferedPipeline.h"

/**
 * \brief Buffered pipeline that forwards frames unchanged.
 *
 * Implements handle_frame to chain frames to next pipeline.
 *
 * Lifecycle:
 *     Construction → start → running → stop → release
 *
 * Thread Safety:
 *     Inherits from AbstractBufferedPipeline.
 */
class SimpleBufferedPipeline : virtual public AbstractBufferedPipeline {
protected:
	/**
	 * \brief Called on start.
	 */
	virtual void on_start();
	/**
	 * \brief Called on stop.
	 */
	virtual void on_stop();
	/**
	 * \brief Handle frame by chaining downstream.
	 *
	 * \param[in] frame Frame to forward.
	 *
	 * \return 0 on success.
	 */
	virtual int handle_frame(uvc_frame_t *frame);
public:
	/**
	 * \brief Construct simple buffered pipeline.
	 *
	 * \param[in] _max_buffer_num Max buffer size.
	 * \param[in] init_pool_num Initial pool size.
	 * \param[in] default_frame_size Default frame size.
	 * \param[in] drop_frames_when_buffer_empty Drop frames when full.
	 */
	SimpleBufferedPipeline(const int &_max_buffer_num = DEFAULT_MAX_FRAME_NUM, const int &init_pool_num = DEFAULT_INIT_FRAME_POOL_SZ,
			const size_t &default_frame_size = DEFAULT_FRAME_SZ, const bool &drop_frames_when_buffer_empty = true);
	/**
	 * \brief Destroy simple buffered pipeline.
	 */
	virtual ~SimpleBufferedPipeline();
};


#endif //PUPILMOBILE_SIMPLEBUFFEREDPIPELINE_H
