/*
 * UVCCamera
 * library and sample to access to UVC web camera on non-rooted Android device
 *
 * Copyright (c) 2014-2017 saki t_saki@serenegiant.com
 *
 * File name: _onload.h
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 * All files in the folder are under this Apache License, Version 2.0.
 * Files in the jni/libjpeg, jni/libusb, jin/libuvc, jni/rapidjson folder may have a different license, see the respective files.
*/

#ifndef ONLOAD_H_
#define ONLOAD_H_

#pragma interface

#include <jni.h>

/**
 * \brief JNI entry point for the UVCCamera native library.
 *
 * Registers native methods when the Android runtime loads the library.
 */
#ifdef __cplusplus
extern "C" {
#endif

/**
 * \brief Load the UVCCamera native library and register native methods.
 *
 * \param[in] vm Java virtual machine instance.
 * \param[in] reserved Reserved by the JNI specification; must be passed
 *     through unchanged.
 * \return JNI_VERSION_1_6 on success, or JNI_ERR on failure.
 */
jint JNI_OnLoad(JavaVM *vm, void *reserved);

#ifdef __cplusplus
}
#endif

#endif /* ONLOAD_H_ */
