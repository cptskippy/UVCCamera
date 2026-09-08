//
// Created by saki on 15/11/25.
//

#include "utilbase.h"

// Implementation for DistributePipeline.h; see the header for the public pipeline interface.


#include "common_utils.h"
#include "libUVCCamera.h"

#include "pipeline_helper.h"
#include "IPipeline.h"
#include "DistributePipeline.h"

DistributePipeline::DistributePipeline(const int &_max_buffer_num, const int &init_pool_num,
		const size_t &default_frame_size, const bool &drop_frames_when_buffer_empty)
:	AbstractBufferedPipeline(_max_buffer_num, init_pool_num, default_frame_size, drop_frames_when_buffer_empty)
{
	ENTER();

	setState(PIPELINE_STATE_INITIALIZED);

	EXIT();
}

DistributePipeline::~DistributePipeline() {
	ENTER();

	Mutex::Autolock lock(pipeline_mutex);

	pipelines.clear();

	EXIT();
}

void DistributePipeline::on_start() {
	ENTER();
	EXIT();
}

void DistributePipeline::on_stop() {
	ENTER();
	EXIT();
}

/**
 * \brief Forward a frame to all registered downstream pipelines.
 *
 * Each downstream pipeline receives its own copy via queueFrame(); the
 * frame buffer ownership remains with the caller (AbstractBufferedPipeline
 * recycles it after this returns).
 *
 * \param[in] frame Frame to distribute, must not be null.
 * \return 0 on success.
 *
 * Code Paths:
 *   1. Acquire pipeline_mutex.
 *   2. Iterate all registered pipelines, calling queueFrame(frame) on each.
 *      Individual pipeline failures do not stop iteration.
 *   3. Return 0.
 */
int DistributePipeline::handle_frame(uvc_frame_t *frame) {
	ENTER();

	Mutex::Autolock lock(pipeline_mutex);

	for (auto iter = pipelines.begin(); iter != pipelines.end(); iter++) {
		(*iter)->queueFrame(frame);
	}

	RETURN(0, int);
}

/**
 * \brief Register a downstream pipeline to receive all frames.
 *
 * \param[in] pipeline Pipeline to add, or null (no-op).
 * \return 0 on success.
 *
 * Code Paths:
 *   1. pipeline is null → return 0 without modification.
 *   2. Acquire pipeline_mutex, push pipeline onto the list.
 */
int DistributePipeline::addPipeline(IPipeline *pipeline) {
	ENTER();

	if (pipeline) {
		Mutex::Autolock lock(pipeline_mutex);
		pipelines.push_back(pipeline);
	}

	RETURN(0, int);
}

/**
 * \brief Remove a downstream pipeline from the distribution list.
 *
 * \param[in] pipeline Pipeline to remove, or null (no-op).
 * \return 0 on success.
 *
 * Code Paths:
 *   1. pipeline is null → return 0 without modification.
 *   2. Acquire pipeline_mutex, linear search for the matching pointer,
 *      erase it if found. Multiple instances of the same pointer are all removed.
 */
int DistributePipeline::removePipeline(IPipeline *pipeline) {
	ENTER();

	if (pipeline) {
		Mutex::Autolock lock(pipeline_mutex);

		for (auto iter = pipelines.begin(); iter != pipelines.end(); ) {
			if (*iter == pipeline) {
				iter = pipelines.erase(iter);
			} else {
				iter++;
			}
		}
	}

	RETURN(0, int);
}

//**********************************************************************
//
//**********************************************************************
static ID_TYPE nativeCreate(JNIEnv *env, jobject thiz) {

	ENTER();
	DistributePipeline *pipeline = new DistributePipeline();
	setField_long(env, thiz, "mNativePtr", reinterpret_cast<ID_TYPE>(pipeline));
	RETURN(reinterpret_cast<ID_TYPE>(pipeline), ID_TYPE);
}

static void nativeDestroy(JNIEnv *env, jobject thiz,
	ID_TYPE id_pipeline) {

	ENTER();
	setField_long(env, thiz, "mNativePtr", 0);
	DistributePipeline *pipeline = reinterpret_cast<DistributePipeline *>(id_pipeline);
	if (LIKELY(pipeline)) {
		pipeline->release();
		SAFE_DELETE(pipeline);
	}
	EXIT();
}

static jint nativeGetState(JNIEnv *env, jobject thiz,
	ID_TYPE id_pipeline) {

	ENTER();
	jint result = 0;
	DistributePipeline *pipeline = reinterpret_cast<DistributePipeline *>(id_pipeline);
	if (pipeline) {
		result = pipeline->getState();
	}
	RETURN(result, jint);
}

static jint nativeSetPipeline(JNIEnv *env, jobject thiz,
	ID_TYPE id_pipeline, jobject pipeline_obj) {

	ENTER();
	jint result = JNI_ERR;
	DistributePipeline *pipeline = reinterpret_cast<DistributePipeline *>(id_pipeline);
	if (pipeline) {
		IPipeline *target_pipeline = getPipeline(env, pipeline_obj);
		result = pipeline->setPipeline(target_pipeline);
	}

	RETURN(result, jint);
}

static jint nativeAddPipeline(JNIEnv *env, jobject thiz,
	ID_TYPE id_pipeline, jobject pipeline_obj) {

	ENTER();
	jint result = JNI_ERR;
	DistributePipeline *pipeline = reinterpret_cast<DistributePipeline *>(id_pipeline);
	if (pipeline) {
		IPipeline *target_pipeline = getPipeline(env, pipeline_obj);
		result = pipeline->addPipeline(target_pipeline);
	}

	RETURN(result, jint);
}

static jint nativeRemovePipeline(JNIEnv *env, jobject thiz,
	ID_TYPE id_pipeline, jobject pipeline_obj) {

	ENTER();
	jint result = JNI_ERR;
	DistributePipeline *pipeline = reinterpret_cast<DistributePipeline *>(id_pipeline);
	if (pipeline) {
		IPipeline *target_pipeline = getPipeline(env, pipeline_obj);
		result = pipeline->removePipeline(target_pipeline);
	}

	RETURN(result, jint);
}

static jint nativeStart(JNIEnv *env, jobject thiz,
	ID_TYPE id_pipeline) {

	ENTER();

	int result = JNI_ERR;
	DistributePipeline *pipeline = reinterpret_cast<DistributePipeline *>(id_pipeline);
	if (LIKELY(pipeline)) {
		result = pipeline->start();
	}
	RETURN(result, jint);
}

static jint nativeStop(JNIEnv *env, jobject thiz,
	ID_TYPE id_pipeline) {

	jint result = JNI_ERR;
	ENTER();
	DistributePipeline *pipeline = reinterpret_cast<DistributePipeline *>(id_pipeline);
	if (LIKELY(pipeline)) {
		result = pipeline->stop();
	}
	RETURN(result, jint);
}

//**********************************************************************
//
//**********************************************************************
static JNINativeMethod methods[] = {
	{ "nativeCreate",					"()J", (void *) nativeCreate },
	{ "nativeDestroy",					"(J)V", (void *) nativeDestroy },

	{ "nativeGetState",					"(J)I", (void *) nativeGetState },
	{ "nativeSetPipeline",				"(JLcom/serenegiant/usb/IPipeline;)I", (void *) nativeSetPipeline },
	{ "nativeAddPipeline",				"(JLcom/serenegiant/usb/IPipeline;)I", (void *) nativeAddPipeline },
	{ "nativeRemovePipeline",			"(JLcom/serenegiant/usb/IPipeline;)I", (void *) nativeRemovePipeline },

	{ "nativeStart",					"(J)I", (void *) nativeStart },
	{ "nativeStop",						"(J)I", (void *) nativeStop },
};

int register_distribute_pipeline(JNIEnv *env) {
	LOGV("register_distribute_pipeline:");
	if (registerNativeMethods(env,
		"com/serenegiant/usb/DistributePipeline",
		methods, NUM_ARRAY_ELEMENTS(methods)) < 0) {
		return -1;
	}
    return 0;
}
