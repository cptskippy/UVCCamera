//
// Created by saki on 15/10/06.
//

/**
 * \brief Pipeline that publishes frames via ZeroMQ.
 *
 * Sends frames over ZeroMQ PUB socket for external consumers.
 *
 * Exports:
 *     PublisherPipeline: ZeroMQ publishing pipeline.
 *
 * Dependencies:
 *     - zmq.hpp: ZeroMQ C++ binding.
 *     - AbstractBufferedPipeline.h: Buffered pipeline base.
 *     - pupilmobile_defs.h: Pupil Mobile definitions.
 *
 * Architecture Note:
 *     Used for streaming frames to external applications via ZeroMQ.
 */

#ifndef PUPILMOBILE_PUBLISHER_PIPELINE_H
#define PUPILMOBILE_PUBLISHER_PIPELINE_H

#pragma interface

#include <string>
#include "Mutex.h"
#include "Timers.h"
#include "zmq.hpp"

#include "pupilmobile_defs.h"
#include "AbstractBufferedPipeline.h"

using namespace android;

/**
 * \brief Publishes frames via ZeroMQ PUB socket.
 *
 * Sends each frame to ZeroMQ subscribers with subscription_id prefix.
 *
 * Lifecycle:
 *     Construction → start → running → stop → release
 *
 * Thread Safety:
 *     handle_frame runs on handler thread. publisher_mutex protects socket access.
 *
 * Properties:
 *     host: ZeroMQ endpoint address.
 *     subscription_id: Identifier for subscribers.
 *     data_bytes: Size of frame data.
 */
class PublisherPipeline : virtual public AbstractBufferedPipeline {
private:
protected:
	const std::string host;
	const std::string subscription_id;
	size_t data_bytes;
	zmq::context_t *context;
	zmq::socket_t *publisher;
	mutable Mutex publisher_mutex;
	/**
	 * \brief Called when pipeline starts.
	 *
	 * \pre None
	 * \post ZeroMQ context and socket created.
	 */
	virtual void on_start();
	/**
	 * \brief Called when pipeline stops.
	 *
	 * \pre Pipeline running.
	 * \post ZeroMQ socket closed.
	 */
	virtual void on_stop();
	/**
	 * \brief Handle frame and publish via ZeroMQ.
	 *
	 * \param[in] frame Frame to publish.
	 *
	 * \return 0 on success.
	 *
	 * Side Effects:
	 *     - Sends frame over ZeroMQ socket.
	 */
	virtual int handle_frame(uvc_frame_t *frame);
public:
	/**
	 * \brief Construct publisher pipeline with size and address.
	 *
	 * \param[in] _data_bytes Default frame size.
	 * \param[in] addr ZeroMQ endpoint address. NULL for default.
	 * \param[in] subscription_id Subscription identifier.
	 *
	 * \pre None
	 * \post ZeroMQ context not yet created.
	 */
	PublisherPipeline(const size_t &_data_bytes = DEFAULT_FRAME_SZ, const char *addr = NULL, const char *subscription_id = NULL);
	/**
	 * \brief Construct publisher pipeline with address.
	 *
	 * \param[in] addr ZeroMQ endpoint address.
	 * \param[in] subscription_id Subscription identifier.
	 */
	PublisherPipeline(const char *addr, const char *subscription_id);
	/**
	 * \brief Destroy publisher pipeline.
	 *
	 * Side Effects:
	 *     - Closes ZeroMQ socket and context.
	 */
	virtual ~PublisherPipeline();
	/**
	 * \brief Queue frame for publishing.
	 *
	 * \param[in] frame Frame to publish.
	 *
	 * \return 0 on success.
	 *
	 * \pre Frame must be valid.
	 * \post Frame enqueued for publishing.
	 *
	 * Side Effects:
	 *     - May block if buffer full.
	 */
	virtual int queueFrame(uvc_frame_t *frame);
};

#endif //PUPILMOBILE_PUBLISHER_PIPELINE_H
