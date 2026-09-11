//
// Created by saki on 15/11/12.
//

#include "utilbase.h"

// Pipeline helper implementation; see pipeline_helper.h for the public helper declarations.


#include "Timers.h"
#include "SimpleBufferedPipeline.h"
#include "SQLiteBufferedPipeline.h"
#include "UVCCameraControl.h"
#include "CallbackPipeline.h"
#include "ConvertPipeline.h"
#include "PreviewPipeline.h"
#include "PublisherPipeline.h"
#include "DistributePipeline.h"
#include "pipeline_helper.h"

/**
 * \brief Resolve a Java IPipeline object to its native pipeline pointer.
 *
 * Reads the `mNativePtr` and `mType` fields from the Java object and casts
 * the stored pointer to the concrete pipeline type.
 *
 * \param[in] env JNI environment.
 * \param[in] pipeline_obj Java object implementing IPipeline, or null.
 *
 * \return The native pipeline pointer, or NULL if the object is null or the
 *         type is unrecognised.
 *
 * Code Paths:
 *   1. `pipeline_obj` is null → return NULL immediately.
 *   2. Read `mNativePtr` and `mType` from the Java object; clear any pending JNI exception.
 *   3. Switch on `mType` and cast `mNativePtr` to the matching concrete type
 *      (SimpleBuffered, SQLiteBuffered, UVCCameraControl, Callback, Convert,
 *      Preview, Publisher, or Distribute).
 *   4. Unrecognised type → return NULL.
 */
IPipeline *getPipeline(JNIEnv *env, jobject pipeline_obj) {
	ENTER();

	if (!pipeline_obj) return NULL;
	ID_TYPE id_pipeline = getField_long(env, pipeline_obj, "mNativePtr");
	jint type = getField_int(env, pipeline_obj, "mType");
	env->ExceptionClear();
	IPipeline *result = NULL;
	switch (type) {
		case PIPELINE_TYPE_SIMPLE_BUFFERED:
			result = reinterpret_cast<SimpleBufferedPipeline *>(id_pipeline);
			break;
		case PIPELINE_TYPE_SQLITE_BUFFERED:
			result = reinterpret_cast<SQLiteBufferedPipeline *>(id_pipeline);
			break;
		case PIPELINE_TYPE_UVC_CONTROL:
			result = reinterpret_cast<UVCCameraControl *>(id_pipeline);
			break;
		case PIPELINE_TYPE_CALLBACK:
			result = reinterpret_cast<CallbackPipeline *>(id_pipeline);
			break;
		case PIPELINE_TYPE_CONVERT:
			result = reinterpret_cast<ConvertPipeline *>(id_pipeline);
			break;
		case PIPELINE_TYPE_PREVIEW:
			result = reinterpret_cast<PreviewPipeline *>(id_pipeline);
			break;
		case PIPELINE_TYPE_PUBLISHER:
			result = reinterpret_cast<PublisherPipeline *>(id_pipeline);
			break;
		case PIPELINE_TYPE_DISTRIBUTE:
			result = reinterpret_cast<DistributePipeline *>(id_pipeline);
			break;
		default:
			result = NULL;
			break;
	}

	RETURN(result, IPipeline *);
}
