//
// Created by saki on 15/11/05.
//

#if 1	// set 1 if you don't need debug message
	#ifndef LOG_NDEBUG
		#define	LOG_NDEBUG		// ignore LOGV/LOGD/MARK
	#endif
	#undef USE_LOGALL
#else
	#define USE_LOGALL
	#undef LOG_NDEBUG
	#undef NDEBUG		// depends on definition in Android.mk and Application.mk
#endif

#include "utilbase.h"
#include "AbstractBufferedPipeline.h"

/*public*/
AbstractBufferedPipeline::AbstractBufferedPipeline(const int &_max_buffer_num, const int &_init_pool_num,
	const size_t &_default_frame_size, const bool &drop_frames_when_buffer_empty)
:	IPipeline(_default_frame_size),
	max_buffer_num(_max_buffer_num),
	init_pool_num(_init_pool_num),
	drop_frames(drop_frames_when_buffer_empty),
	total_frame_num(0)
{
	ENTER();

	EXIT();
}

/*public*/
AbstractBufferedPipeline::~AbstractBufferedPipeline() {
	ENTER();

	release();
	setState(PIPELINE_STATE_UNINITIALIZED);

	EXIT();
}

/**
 * \brief Release all pipeline resources and transition to UNINITIALIZED.
 *
 * \return 0 on success.
 *
 * Code Paths:
 *   1. Transition to RELEASING state.
 *   2. Call stop() to join the handler thread (no-op if already stopped).
 *   3. Recycle all buffered frames back to the pool via clear_frames().
 *   4. Free all pooled frames via clear_pool().
 *   5. Transition to UNINITIALIZED.
 */
/*public*/
int AbstractBufferedPipeline::release() {
	ENTER();

	setState(PIPELINE_STATE_RELEASING);
	stop();
	clear_frames();
	clear_pool();
	setState(PIPELINE_STATE_UNINITIALIZED);

	RETURN(0, int);
}

/**
 * \brief Start the handler thread that processes buffered frames.
 *
 * \return EXIT_SUCCESS if the thread was started or is already running,
 *         EXIT_FAILURE if thread creation failed.
 *
 * Code Paths:
 *   1. Already running (isRunning()) → return EXIT_FAILURE without action.
 *   2. Set mIsRunning, transition to STARTING, create the handler thread
 *      under buffer_mutex.
 *   3. Thread creation failed → revert to INITIALIZED, clear mIsRunning,
 *      broadcast both syncs to wake any waiting consumers, return EXIT_FAILURE.
 *   4. Thread created → do_loop() begins in the new thread.
 */
/*public*/
int AbstractBufferedPipeline::start() {
	ENTER();

	int result = EXIT_FAILURE;
	if (!isRunning()) {
		mIsRunning = true;
		setState(PIPELINE_STATE_STARTING);
		buffer_mutex.lock();
		{
			result = pthread_create(&handler_thread, NULL, handler_thread_func, (void *) this);
		}
		buffer_mutex.unlock();
		if (UNLIKELY(result != EXIT_SUCCESS)) {
			LOGW("AbstractBufferedPipeline::already running/could not create thread etc.");
			setState(PIPELINE_STATE_INITIALIZED);
			mIsRunning = false;
			buffer_mutex.lock();
			{
				pool_sync.broadcast();
				buffer_sync.broadcast();
			}
			buffer_mutex.unlock();
		}
	}
	RETURN(result, int);
}

/**
 * \brief Stop the handler thread and drain buffered frames.
 *
 * \return 0 on success.
 *
 * Code Paths:
 *   1. Not running → skip thread join, fall through to clear_frames().
 *   2. Running → transition to STOPPING, clear mIsRunning, broadcast syncs
 *      to wake the handler thread, pthread_join, transition to INITIALIZED.
 *   3. Always: clear_frames() recycles any remaining buffered frames.
 */
/*public*/
int AbstractBufferedPipeline::stop() {
	ENTER();

	bool b = isRunning();
	if (LIKELY(b)) {
		setState(PIPELINE_STATE_STOPPING);
		mIsRunning = false;
		pool_sync.broadcast();
		buffer_sync.broadcast();
		LOGD("pthread_join:handler_thread");
		if (pthread_join(handler_thread, NULL) != EXIT_SUCCESS) {
			LOGW("PublisherPipeline::terminate publisher thread: pthread_join failed");
		}
		setState(PIPELINE_STATE_INITIALIZED);
		LOGD("handler_thread finished");
	}
	clear_frames();

	RETURN(0, int);
}

/**
 * \brief Duplicate an incoming frame into the pool and enqueue it for processing.
 *
 * \param[in] frame Source frame to duplicate, or null.
 * \return 0 on success, UVC_ERROR_NO_MEM if the pool is exhausted,
 *         UVC_ERROR_OTHER if the frame is null.
 *
 * Code Paths:
 *   1. frame is null → return UVC_ERROR_OTHER.
 *   2. get_frame() returns null (pool empty and at max_buffer_num) →
 *      return UVC_ERROR_NO_MEM (frame dropped).
 *   3. uvc_duplicate_frame fails → recycle the empty copy, return error.
 *   4. Success → add_frame() enqueues the copy and signals the handler thread.
 */
/*public*/
int AbstractBufferedPipeline::queueFrame(uvc_frame_t *frame) {
	ENTER();

	int ret = UVC_ERROR_OTHER;
	if (LIKELY(frame)) {
		// get empty frame from frame pool
		uvc_frame_t *copy = get_frame(frame->data_bytes);
		if (UNLIKELY(!copy)) {
			LOGD("buffer pool is empty and exceeds the limit, drop frame");
			RETURN(UVC_ERROR_NO_MEM, int);
		}
		// duplicate frame buffer and pass copy to publisher
		ret = uvc_duplicate_frame(frame, copy);
		if (LIKELY(!ret)) {
			ret = add_frame(copy);
		} else {
			LOGW("uvc_duplicate_frame failed:%d", ret);
			recycle_frame(copy);
		}
	}

	RETURN(ret, int);
}

//********************************************************************************
//
//********************************************************************************
/**
 * \brief Acquire a reusable frame buffer from the pool.
 *
 * Grows the pool by doubling (capped at max_buffer_num) when empty and under
 * the limit. If drop_frames is false, blocks on pool_sync until a frame is
 * recycled or the pipeline stops.
 *
 * \param[in] data_bytes Required frame size in bytes.
 * \return A pooled frame, or NULL if the pool is exhausted and drop_frames is true.
 *
 * Code Paths:
 *   1. Pool empty and total_frame_num < max_buffer_num → allocate up to
 *      double the current pool size (capped), adding new frames.
 *   2. Pool still empty and !drop_frames → wait on pool_sync until a frame
 *      is recycled or mIsRunning becomes false.
 *   3. Pool non-empty → pop and return the front frame.
 *   4. Pool empty and drop_frames → return NULL.
 */
uvc_frame_t *AbstractBufferedPipeline::get_frame(const size_t &data_bytes) {
	uvc_frame_t *frame = NULL;
	Mutex::Autolock lock(pool_mutex);

	if (UNLIKELY(frame_pool.empty() && (total_frame_num < max_buffer_num))) {
		uint32_t n = total_frame_num * 2;
		if (n > max_buffer_num) {
			n = max_buffer_num;
		}
		n -= total_frame_num;
		if (LIKELY(n > 0)) {
			for (int i = 0; i < n; i++) {
				frame = uvc_allocate_frame(data_bytes);
				total_frame_num++;
			}
			LOGW("allocate new frame:%d", total_frame_num);
		} else {
			LOGW("number of allocated frame exceeds limit");
		}
	}
	if (UNLIKELY(frame_pool.empty() && !drop_frames)) {
		// if pool is empty and need to block(avoid dropping frames), wait frame recycling.
		for (; mIsRunning && frame_pool.empty() ; ) {
			pool_sync.wait(pool_mutex);
		}
	}
	if (!frame_pool.empty()) {
		frame = frame_pool.front();
		frame_pool.pop_front();
	}

	return frame;
}

/**
 * \brief Return a frame buffer to the pool, or free it if the pool is full.
 *
 * \param[in] frame Frame to recycle, or null (no-op).
 *
 * Code Paths:
 *   1. frame is null → no-op.
 *   2. Pool size < max_buffer_num → push frame onto pool, set local to NULL.
 *   3. Pool at capacity → decrement total_frame_num, uvc_free_frame.
 *   4. Always: pool_sync.signal() wakes a waiting get_frame() caller.
 */
void AbstractBufferedPipeline::recycle_frame(uvc_frame_t *frame) {
	ENTER();

	if (LIKELY(frame)) {
		Mutex::Autolock lock(pool_mutex);
		if (LIKELY(frame_pool.size() < max_buffer_num)) {
			frame_pool.push_back(frame);
			frame = NULL;
		}
		if (UNLIKELY(frame)) {
			// if pool overflowed
			total_frame_num--;
			uvc_free_frame(frame);
		}
		pool_sync.signal();
	}

	EXIT();
}

void AbstractBufferedPipeline::init_pool(const size_t &data_bytes) {
	ENTER();

	uvc_frame_t *frame = NULL;

	clear_pool();
	pool_mutex.lock();
	{

		size_t frame_sz = data_bytes / 4;	// expects 25%, this will be able to much lower
		if (!frame_sz) {
			frame_sz = DEFAULT_FRAME_SZ;
		}
		for (uint32_t i = 0; i < init_pool_num; i++) {
			frame = uvc_allocate_frame(frame_sz);
			if (LIKELY(frame)) {
				frame_pool.push_back(frame);
				total_frame_num++;
			} else {
				LOGW("failed to allocate new frame:%d", total_frame_num);
				break;
			}
		}
	}
	pool_mutex.unlock();

	EXIT();
}

void AbstractBufferedPipeline::clear_pool() {
	ENTER();

	Mutex::Autolock lock(pool_mutex);

	for (auto iter = frame_pool.begin(); iter != frame_pool.end(); iter++) {
		total_frame_num--;
		uvc_free_frame(*iter);
	}
	frame_pool.clear();
	EXIT();
}

//********************************************************************************
//
//********************************************************************************

void AbstractBufferedPipeline::clear_frames() {
	Mutex::Autolock lock(buffer_mutex);

	for (auto iter = frame_buffers.begin(); iter != frame_buffers.end(); iter++) {
		recycle_frame(*iter);
	}
	frame_buffers.clear();
}

/**
 * \brief Enqueue a frame for processing by the handler thread.
 *
 * When the buffer exceeds max_buffer_num, up to 5 oldest frames are dropped.
 * If the pipeline is not running or the buffer is at capacity, the frame is
 * recycled immediately.
 *
 * \param[in] frame Frame to enqueue (ownership transferred on success).
 * \return 0 on success.
 *
 * Code Paths:
 *   1. frame_buffers.size() > max_buffer_num → recycle up to 5 oldest frames.
 *   2. isRunning() and buffer not at capacity → push frame, signal buffer_sync.
 *   3. Not running or buffer at capacity → recycle the frame immediately.
 */
int AbstractBufferedPipeline::add_frame(uvc_frame_t *frame) {
	ENTER();

	buffer_mutex.lock();
	{
		// FIXME as current implementation, transferring frame data on my device is slower than that coming from UVC camera... just drop them now
		if (frame_buffers.size() > max_buffer_num) {
			// erase old frames
			int cnt = 0;
			for (auto iter = frame_buffers.begin();
				 (iter != frame_buffers.end()) && (cnt < 5); iter++, cnt++) {
				recycle_frame(*iter);
				iter = frame_buffers.erase(iter);
			}
			LOGW("droped frame data");
		}
		if (isRunning() && (frame_buffers.size() < max_buffer_num)) {
			frame_buffers.push_back(frame);
			frame = NULL;
		}
		buffer_sync.signal();
	}
	buffer_mutex.unlock();
	if (frame) {
		recycle_frame(frame);
	}

	RETURN(0, int);
}

uvc_frame_t *AbstractBufferedPipeline::wait_frame() {
	uvc_frame_t *frame = NULL;

	Mutex::Autolock lock(buffer_mutex);

	if (!frame_buffers.size()) {
		buffer_sync.wait(buffer_mutex);
	}
	if (LIKELY(isRunning() && frame_buffers.size() > 0)) {
		frame = frame_buffers.front();
		frame_buffers.pop_front();
	}
	return frame;
}

uint32_t AbstractBufferedPipeline::get_frame_count() {
	ENTER();

	Mutex::Autolock lock(buffer_mutex);
	uint32_t result = frame_buffers.size();

	RETURN(result, uint32_t);
}

//********************************************************************************
//
//********************************************************************************
void *AbstractBufferedPipeline::handler_thread_func(void *vptr_args) {

	ENTER();
	AbstractBufferedPipeline *pipeline = reinterpret_cast<AbstractBufferedPipeline *>(vptr_args);
	if (LIKELY(pipeline)) {
		pipeline->do_loop();
	}
	PRE_EXIT();
	pthread_exit(NULL);
}

/**
 * \brief Main processing loop executed on the handler thread.
 *
 * Initializes the frame pool, calls on_start(), then repeatedly waits for
 * frames, dispatches them to handle_frame() or the downstream pipeline via
 * chain_frame(), and recycles each frame after processing.
 *
 * Code Paths:
 *   1. Entry: clear any stale frames, init_pool(), call on_start(),
 *      transition to RUNNING.
 *   2. Loop while isRunning(): wait_frame() blocks until a frame arrives or
 *      the pipeline stops.
 *   3. handle_frame() returns false (not consumed) → chain_frame() forwards
 *      the frame downstream.
 *   4. handle_frame() throws → catch, log, continue (frame still recycled).
 *   5. After processing → recycle_frame() returns the buffer to the pool.
 *   6. Exit loop: transition to STOPPING, clear mIsRunning, call on_stop(),
 *      transition to INITIALIZED.
 */
void AbstractBufferedPipeline::do_loop() {
	ENTER();

	clear_frames();
	init_pool(default_frame_size);

	on_start();
	setState(PIPELINE_STATE_RUNNING);
	for ( ; LIKELY(isRunning()) ; ) {
		uvc_frame_t *frame = wait_frame();
		if ((LIKELY(frame))) {
			try {
				if (!handle_frame(frame)) {
					chain_frame(frame);
				}
			} catch (...) {
				LOGE("exception");
			}
			recycle_frame(frame);
		}
	}
	setState(PIPELINE_STATE_STOPPING);
	mIsRunning = false;
	on_stop();
	setState(PIPELINE_STATE_INITIALIZED);

	EXIT();
}
