//
// Created by saki on 15/11/05.
//

/**
 * \brief Abstract buffered pipeline with frame pool and handler thread.
 *
 * Provides frame buffering, pooling, and threaded handling for pipeline stages.
 *
 * Exports:
 *     AbstractBufferedPipeline: Base class for buffered pipelines.
 *     DEFAULT_INIT_FRAME_POOL_SZ: Default initial pool size.
 *     DEFAULT_MAX_FRAME_NUM: Default max buffer count.
 *
 * Dependencies:
 *     - libUVCCamera.h: UVC frame types.
 *     - IPipeline.h: Base pipeline interface.
 *     - Mutex.h/Condition.h: Threading primitives.
 *
 * Architecture Note:
 *     Uses frame pool to reduce allocation overhead. Handler thread processes frames from buffer.
 */

#ifndef PUPILMOBILE_ABSTRACTBUFFEREDPIPELINE_H
#define PUPILMOBILE_ABSTRACTBUFFEREDPIPELINE_H

#include <stdlib.h>
#include <pthread.h>
#include <list>
#include "Mutex.h"
#include "Condition.h"

#include "libUVCCamera.h"
#include "IPipeline.h"

#pragma interface

#define DEFAULT_INIT_FRAME_POOL_SZ 2
#define DEFAULT_MAX_FRAME_NUM 8

using namespace android;

class AbstractBufferedPipeline;

/**
 * \brief Abstract base for buffered pipelines with frame pool.
 *
 * Manages frame pool, buffer, and handler thread. Subclasses implement on_start/on_stop/handle_frame.
 *
 * Lifecycle:
 *     Construction → start → handler thread running → stop → release
 *
 * State Machine:
 *     UNINITIALIZED → INITIALIZED → STARTING → RUNNING → STOPPING → UNINITIALIZED
 *
 * Thread Safety:
 *     Handler thread processes frames asynchronously. queueFrame may block.
 *
 * Properties:
 *     max_buffer_num: Maximum buffered frames.
 *     init_pool_num: Initial pool allocation.
 *     drop_frames: Drop frames when buffer full.
 */
class AbstractBufferedPipeline : virtual public IPipeline {
private:
	const uint32_t max_buffer_num;
	const uint32_t init_pool_num;
	const bool drop_frames;
	volatile uint32_t total_frame_num;

// frame buffer pool to improve performance by reducing memory allocation
	mutable Mutex pool_mutex;
	Condition pool_sync;
	std::list<uvc_frame_t *> frame_pool;
// frame buffers
	pthread_t handler_thread;
	mutable Mutex buffer_mutex;
	Condition buffer_sync;
	std::list<uvc_frame_t *> frame_buffers;
	static void *handler_thread_func(void *vptr_args);

protected:
	/**
	 * \brief Get frame from pool or allocate new.
	 *
	 * \param[in] data_bytes Size for frame data.
	 *
	 * \return Pointer to uvc_frame_t.
	 *
	 * \pre data_bytes > 0
	 * \post Frame allocated or reused.
	 *
	 * Side Effects:
	 *     - Allocates frame if pool empty.
	 */
	uvc_frame_t *get_frame(const size_t &data_bytes);
	/**
	 * \brief Return frame to pool.
	 *
	 * \param[in] frame Frame to recycle.
	 *
	 * \pre frame must be valid.
	 * \post Frame added to pool.
	 */
	void recycle_frame(uvc_frame_t *frame);
	/**
	 * \brief Initialize frame pool.
	 *
	 * \param[in] data_bytes Size for frames.
	 *
	 * \pre None
	 * \post Pool populated with init_pool_num frames.
	 */
	void init_pool(const size_t &data_bytes);
	/**
	 * \brief Clear frame pool.
	 *
	 * \pre None
	 * \post Pool empty.
	 */
	void clear_pool();
	/**
	 * \brief Clear buffered frames.
	 *
	 * \pre None
	 * \post Buffer empty.
	 */
	void clear_frames();
	/**
	 * \brief Add frame to buffer.
	 *
	 * \param[in] frame Frame to add.
	 *
	 * \return 0 on success.
	 *
	 * \pre frame valid.
	 * \post Frame queued.
	 */
	int add_frame(uvc_frame_t *frame);
	/**
	 * \brief Wait for frame from buffer.
	 *
	 * \return Pointer to uvc_frame_t or NULL.
	 *
	 * \pre Buffer may be empty.
	 * \post Frame removed from buffer.
	 *
	 * Code Paths:
	 *     1. Buffer has frame → returns it.
	 *     2. Buffer empty → waits on condition.
	 */
	uvc_frame_t *wait_frame();
	/**
	 * \brief Get current frame count.
	 *
	 * \return Number of buffered frames.
	 */
	uint32_t get_frame_count();
	/**
	 * \brief Main handler loop.
	 *
	 * \pre Handler thread running.
	 * \post Frames processed until stop.
	 */
	virtual void do_loop();
	/**
	 * \brief Called when pipeline starts.
	 *
	 * \pre State INITIALIZED.
	 * \post Resources prepared.
	 */
	virtual void on_start() = 0;
	/**
	 * \brief Called when pipeline stops.
	 *
	 * \pre State RUNNING.
	 * \post Resources released.
	 */
	virtual void on_stop() = 0;
	/**
	 * \brief Handle frame in subclass.
	 *
	 * \param[in] frame Frame to process.
	 *
	 * \return 0 on success.
	 *
	 * \pre frame valid.
	 * \post Frame processed.
	 */
	virtual int handle_frame(uvc_frame_t *frame) = 0;
public:
	/**
	 * \brief Construct buffered pipeline.
	 *
	 * \param[in] _max_buffer_num Maximum buffer size.
	 * \param[in] init_pool_num Initial pool size.
	 * \param[in] default_frame_size Default frame size.
	 * \param[in] drop_frames_when_buffer_empty Drop frames when buffer full.
	 *
	 * \pre None
	 * \post Pool initialized.
	 */
	AbstractBufferedPipeline(const int &_max_buffer_num = DEFAULT_MAX_FRAME_NUM, const int &init_pool_num = DEFAULT_INIT_FRAME_POOL_SZ,
		const size_t &default_frame_size = DEFAULT_FRAME_SZ, const bool &drop_frames_when_buffer_empty = true);
	/**
	 * \brief Destroy buffered pipeline.
	 *
	 * Side Effects:
	 *     - Stops handler thread, releases resources.
	 */
	virtual ~AbstractBufferedPipeline();
	/**
	 * \brief Release pipeline resources.
	 *
	 * \return 0 on success.
	 *
	 * Side Effects:
	 *     - Stops handler thread, clears pools.
	 */
	virtual int release();
	/**
	 * \brief Start handler thread.
	 *
	 * \return 0 on success.
	 *
	 * \pre State INITIALIZED.
	 * \post State RUNNING.
	 *
	 * Side Effects:
	 *     - Spawns handler_thread.
	 */
	virtual int start();
	/**
	 * \brief Stop handler thread.
	 *
	 * \return 0 on success.
	 *
	 * \pre State RUNNING.
	 * \post State STOPPING.
	 *
	 * Side Effects:
	 *     - Signals handler thread to exit.
	 */
	virtual int stop();
	/**
	 * \brief Queue frame for processing.
	 *
	 * \param[in] frame Frame to queue.
	 *
	 * \return 0 on success.
	 *
	 * \pre Pipeline running.
	 * \warning May block if buffer full.
	 *
	 * Side Effects:
	 *     - Adds frame to buffer.
	 */
	virtual int queueFrame(uvc_frame_t *frame);
};


#endif //PUPILMOBILE_ABSTRACTBUFFEREDPIPELINE_H
