/*
 * UVCCamera
 * library and sample to access to UVC web camera on non-rooted Android device
 *
 * Copyright (c) 2014-2017 saki t_saki@serenegiant.com
 *
 * File name: _onload.cpp
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

// JNI_OnLoad bootstrap for the UVCCamera native library; see libUVCCamera.h for shared includes.



#include "_onload.h"
#include "utilbase.h"

#define LOCAL_DEBUG 0

extern int register_uvccamera(JNIEnv *env);

/**
 * \brief JNI library entry point for the UVCCamera native library.
 *
 * \pre The Android runtime loads the native library and provides a valid `JavaVM`.
 *
 * \param[in] vm (JavaVM *) Java virtual machine used to acquire the JNI environment.
 * \param[in] reserved (void *) Reserved JNI argument; unused.
 *
 * \return `JNI_VERSION_1_6` when the environment is acquired, otherwise `JNI_ERR`. Native-method registration is attempted after environment acquisition, but its failure is not propagated through the return value.
 *
 * Code Paths:
 * 1. `vm->GetEnv(...)` fails → return `JNI_ERR`.
 * 2. Environment acquired → call `register_uvccamera(env)`, store the VM with `setVM(vm)`, and return `JNI_VERSION_1_6`.
 */
jint JNI_OnLoad(JavaVM *vm, void *reserved) {
#if LOCAL_DEBUG
    LOGD("JNI_OnLoad");
#endif

    JNIEnv *env;
    if (vm->GetEnv(reinterpret_cast<void **>(&env), JNI_VERSION_1_6) != JNI_OK) {
        return JNI_ERR;
    }
    // register native methods
    int result = register_uvccamera(env);
	setVM(vm);
#if LOCAL_DEBUG
    LOGD("JNI_OnLoad:finshed:result=%d", result);
#endif
    return JNI_VERSION_1_6;
}
