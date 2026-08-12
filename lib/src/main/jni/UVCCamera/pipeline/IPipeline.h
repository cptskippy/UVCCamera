//
// Created by saki on 15/11/05.
//

/**
 * \brief Pipeline abstraction for UVC frame processing chain.
 *
 * Defines the base interface and state machine for all pipeline stages in the UVCCamera frame processing architecture.
 *
 * Exports:
 *     pipeline_type_t: Enumeration of pipeline implementation types.
 *     pipeline_state_t: Enumeration of pipeline lifecycle states.
 *     IPipeline: Abstract base class for all pipeline nodes.
 *
 * Dependencies:
 *     - libUVCCamera.h: UVC frame structures and types.
 *     - Mutex.h: Android threading primitives.
 *     - pthread.h: Native threading support.
 *
 * Architecture Note:
 *     Pipelines form a chain where each node processes frames and optionally forwards them to next_pipeline.
 *     Buffered pipelines run handler threads; capture pipelines run capture threads.
 *     State transitions are guarded by pipeline_mutex.
 */

#ifndef PUPILMOBILE_IPIPELINE_H
#define PUPILMOBILE_IPIPELINE_H

#include <stdlib.h>
#include <pthread.h>
#include "Mutex.h"

#include "libUVCCamera.h"

#pragma interface

using namespace android;

#define DEFAULT_FRAME_SZ 1024

typedef enum pipeline_type {
	PIPELINE_TYPE_SIMPLE_BUFFERED = 0,
	PIPELINE_TYPE_SQLITE_BUFFERED = 10,
	PIPELINE_TYPE_UVC_CONTROL = 100,
	PIPELINE_TYPE_CALLBACK = 200,
	PIPELINE_TYPE_CONVERT = 300,
	PIPELINE_TYPE_PREVIEW = 400,
	PIPELINE_TYPE_PUBLISHER = 500,
	PIPELINE_TYPE_DISTRIBUTE = 600,
} pipeline_type_t;

typedef enum _pipeline_state {
	PIPELINE_STATE_UNINITIALIZED = 0,
	PIPELINE_STATE_RELEASING = 10,
	PIPELINE_STATE_INITIALIZED = 20,
	PIPELINE_STATE_STARTING = 30,
	PIPELINE_STATE_RUNNING = 40,
	PIPELINE_STATE_STOPPING = 50,
} pipeline_state_t;

class IPipeline;

/**
 * \brief Abstract base for all UVC frame pipelines.
 *
 * Defines the lifecycle and threading contract for frame processing nodes.
 * Subclasses implement queueFrame to receive frames and optionally chain them.
 *
 * Lifecycle:
 *     Uninitialized → Initialized → Starting → Running → Stopping → Uninitialized
 *
 * State Machine:
 *     PIPELINE_STATE_UNINITIALIZED → PIPELINE_STATE_INITIALIZED → PIPELINE_STATE_STARTING → PIPELINE_STATE_RUNNING
 *     PIPELINE_STATE_RUNNING → PIPELINE_STATE_STOPPING → PIPELINE_STATE_RELEASING → PIPELINE_STATE_UNINITIALIZED
 *
 * Thread Safety:
 *     State access is protected by pipeline_mutex. Subclasses must ensure handler threads do not race with start/stop/release.
 *
 * Properties:
 *     state: Current pipeline lifecycle state.
 *     mIsRunning: Indicates handler thread is active.
 *     default_frame_size: Default size for allocated frame buffers.
 *     next_pipeline: Optional downstream pipeline for chaining.
 */
class IPipeline {
private:
	volatile pipeline_state_t state;
	// force inhibiting copy/assignment
	IPipeline(const IPipeline &src);
	void operator =(const IPipeline &src);
protected:
	volatile bool mIsRunning;
	const size_t default_frame_size;
	mutable Mutex pipeline_mutex;
	IPipeline *next_pipeline;
	/**
	 * \brief Update pipeline state atomically.
	 *
	 * \param[in] new_state Target pipeline state.
	 *
	 * \pre new_state must be a valid pipeline_state_t value.
	 * \post state is updated.
	 *
	 * Side Effects:
	 *     - Locks pipeline_mutex during update.
	 */
	void setState(const pipeline_state_t &new_state);
	/**
	 * \brief Chain frame to next pipeline.
	 *
	 * \param[in] frame Frame to forward. Ownership transfers to next pipeline.
	 *
	 * \return 0 on success, negative on failure.
	 *
	 * \pre next_pipeline must be set and frame must be valid.
	 * \warning May block caller if downstream pipeline buffer is full.
	 *
	 * Side Effects:
	 *     - Calls queueFrame on next_pipeline.
	 *
	 * Code Paths:
	 *     1. If next_pipeline is NULL → returns error.
	 *     2. If queueFrame succeeds → returns 0.
	 *     3. If queueFrame fails → returns error code.
	 */
	virtual int chain_frame(uvc_frame_t *frame);
public:
	/**
	 * \brief Construct pipeline with default frame size.
	 *
	 * Initializes state to UNINITIALIZED and stores default_frame_size for buffer allocation.
	 *
	 * \param[in] default_frame_size Size in bytes for default frame buffers.
	 *
	 * \pre None
	 * \post state == PIPELINE_STATE_UNINITIALIZED
	 *
	 * Side Effects:
	 *     - Initializes pipeline_mutex.
	 */
	IPipeline(const size_t &default_frame_size = DEFAULT_FRAME_SZ);
	/**
	 * \brief Destroy pipeline and release resources.
	 *
	 * Side Effects:
	 *     - Releases pipeline resources if still running.
	 */
	virtual ~IPipeline();
	/**
	 * \brief Get current pipeline state.
	 *
	 * \return Current pipeline_state_t value.
	 *
	 * Thread Safety:
	 *     Reads volatile state without lock; safe for observation.
	 */
	const pipeline_state_t getState() const;
	/**
	 * \brief Check if pipeline is running.
	 *
	 * \return true if handler thread is active.
	 */
	const bool isRunning() const;
	/**
	 * \brief Set downstream pipeline for frame chaining.
	 *
	 * \param[in] pipeline Target pipeline to receive frames. May be NULL to disconnect.
	 *
	 * \return 0 on success, negative on error.
	 *
	 * \pre pipeline must be a valid IPipeline instance or NULL.
	 * \post next_pipeline points to pipeline.
	 *
	 * Side Effects:
	 *     - Updates next_pipeline reference.
	 *
	 * Code Paths:
	 *     1. If pipeline is NULL → disconnects chain, returns 0.
	 *     2. If pipeline is valid → stores reference, returns 0.
	 */
	virtual int setPipeline(IPipeline *pipeline);
	/**
	 * \brief Release pipeline resources.
	 *
	 * \return 0 on success.
	 *
	 * Side Effects:
	 *     - Transitions state to RELEASING then UNINITIALIZED.
	 *     - Stops handler thread if running.
	 *
	 * Code Paths:
	 *     1. If already uninitialized → returns 0.
	 *     2. Otherwise → stops processing, cleans up resources.
	 */
	virtual int release() { return 0; };
	/**
	 * \brief Start pipeline processing.
	 *
	 * \return 0 on success, negative on error.
	 *
	 * \pre State must be INITIALIZED or UNINITIALIZED.
	 * \post State becomes RUNNING.
	 *
	 * Side Effects:
	 *     - Starts handler thread.
	 */
	virtual int start() { return 0; };
	/**
	 * \brief Stop pipeline processing.
	 *
	 * \return 0 on success.
	 *
	 * \pre State must be RUNNING.
	 * \post State becomes STOPPING then INITIALIZED.
	 *
	 * Side Effects:
	 *     - Signals handler thread to exit.
	 */
	virtual int stop() { return 0; };
	/**
	 * \brief Queue a frame for processing.
	 *
	 * \param[in] frame Pointer to uvc_frame_t. Ownership transfers to pipeline.
	 *
	 * \return 0 on success, negative on error.
	 *
	 * \pre frame must be valid and pipeline must be RUNNING.
	 * \warning Caller must not use frame after queuing.
	 *
	 * Side Effects:
	 *     - Enqueues frame for handler thread processing.
	 *     - May block if internal buffer is full.
	 *
	 * Code Paths:
	 *     1. If pipeline not running → returns error.
	 *     2. If buffer full and drop_frames → drops frame.
	 *     3. Otherwise → queues frame.
	 */
	virtual int queueFrame(uvc_frame_t *frame) = 0;
};


#endif //PUPILMOBILE_IPIPELINE_H
