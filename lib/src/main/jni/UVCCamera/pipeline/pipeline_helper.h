//
// Created by saki on 15/11/12.
//

#ifndef PUPILMOBILE_PIPELINE_HELPER_H_H
#define PUPILMOBILE_PIPELINE_HELPER_H_H

#include "common_utils.h"

#include "IPipeline.h"

/**
 * \brief Resolve a Java pipeline wrapper to its native pipeline object.
 *
 * \param[in] env JNI environment.
 * \param[in] pipeline_obj Java pipeline object containing native pointer and
 *     type fields; ownership remains with the caller.
 * \return The native pipeline instance, or NULL if the object is invalid.
 */
IPipeline *getPipeline(JNIEnv *env, jobject pipeline_obj);

#endif //PUPILMOBILE_PIPELINE_HELPER_H_H
