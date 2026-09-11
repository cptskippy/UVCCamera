//
// Created by saki on 15/11/25.
//

/**
 * \brief Pipeline that distributes frames to multiple downstream pipelines.
 *
 * Receives frames and forwards copies to all registered child pipelines.
 *
 * Exports:
 *     DistributePipeline: Multi-cast pipeline node.
 *
 * Dependencies:
 *     - AbstractBufferedPipeline.h: Buffered pipeline base.
 *
 * Architecture Note:
 *     Used for fan-out scenarios where one camera feed needs multiple consumers.
 */

#ifndef PUPILMOBILE_DISTRIBUTEPIPELINE_H
#define PUPILMOBILE_DISTRIBUTEPIPELINE_H

#include "AbstractBufferedPipeline.h"

#pragma interface

/**
 * \brief Distributes incoming frames to multiple child pipelines.
 *
 * Maintains a list of child pipelines and forwards each frame to all of them.
 *
 * Lifecycle:
 *     Construction → addPipeline/removePipeline → start → running → stop → release
 *
 * Thread Safety:
 *     addPipeline/removePipeline must be called before start. Not safe to modify list while running.
 *
 * Properties:
 *     pipelines: List of child IPipeline instances to receive frames.
 */
class DistributePipeline : virtual public AbstractBufferedPipeline {
private:
	std::list<IPipeline *> pipelines;
protected:
	/**
	 * \brief Called when pipeline starts.
	 *
	 * \pre None
	 * \post Child pipelines prepared for receiving frames.
	 */
	virtual void on_start();
	/**
	 * \brief Called when pipeline stops.
	 *
	 * \pre Pipeline is running.
	 * \post Child pipelines paused.
	 */
	virtual void on_stop();
	/**
	 * \brief Handle incoming frame and distribute to children.
	 *
	 * \param[in] frame Frame to distribute.
	 *
	 * \return 0 on success.
	 *
	 * Side Effects:
	 *     - Forwards frame to each child pipeline.
	 *
	 * Code Paths:
	 *     1. If pipelines empty → drops frame.
	 *     2. For each child → calls queueFrame.
	 */
	virtual int handle_frame(uvc_frame_t *frame);
public:
	/**
	 * \brief Construct distribute pipeline.
	 *
	 * \param[in] _max_buffer_num Maximum buffer size.
	 * \param[in] init_pool_num Initial frame pool size.
	 * \param[in] default_frame_size Default frame size.
	 * \param[in] drop_frames_when_buffer_empty Drop frames when buffer full.
	 *
	 * \pre None
	 * \post pipelines list empty.
	 */
	DistributePipeline(const int &_max_buffer_num = DEFAULT_MAX_FRAME_NUM, const int &init_pool_num = DEFAULT_INIT_FRAME_POOL_SZ,
			const size_t &default_frame_size = DEFAULT_FRAME_SZ, const bool &drop_frames_when_buffer_empty = true);
	/**
	 * \brief Destroy distribute pipeline.
	 *
	 * Side Effects:
	 *     - Clears pipelines list.
	 */
	virtual ~DistributePipeline();
	/**
	 * \brief Add child pipeline to distribution list.
	 *
	 * \param[in] pipeline Pipeline to add. Must not be NULL.
	 *
	 * \return 0 on success.
	 *
	 * \pre pipeline must be valid.
	 * \post pipeline added to list.
	 *
	 * \warning Must be called before start.
	 */
	virtual int addPipeline(IPipeline *pipeline);
	/**
	 * \brief Remove child pipeline from distribution list.
	 *
	 * \param[in] pipeline Pipeline to remove.
	 *
	 * \return 0 on success.
	 *
	 * \pre pipeline must be in list.
	 * \post pipeline removed.
	 *
	 * \warning Must be called before start.
	 */
	virtual int removePipeline(IPipeline *pipeline);
};

#endif //PUPILMOBILE_DISTRIBUTEPIPELINE_H
