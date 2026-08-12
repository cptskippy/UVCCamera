/*
 * UVCCamera
 * library and sample to access to UVC web camera on non-rooted Android device
 *
 * Copyright (c) 2014-2017 saki t_saki@serenegiant.com
 *
 * File name: libUVCCamera.h
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

/**
 * \brief Common includes for UVCCamera native layer.
 *
 * Provides the core JNI, libusb, libuvc and utility base headers required
 * by all UVCCamera native components. This header centralizes the native
 * dependencies so that UVCCamera, UVCPreview and pipeline modules share a
 * consistent build surface.
 *
 * Exports:
 *     None — inclusion-only header.
 *
 * Dependencies:
 *     - jni.h: JNI bridge types for Android.
 *     - libusb.h: USB device access.
 *     - libuvc.h: UVC protocol implementation.
 *     - utilbase.h: Internal utility base definitions.
 *
 * Architecture Note:
 *     All native source files include this header to guarantee a uniform
 *     native environment. Do not add platform-specific includes here.
 */

#ifndef LIBUVCCAMERA_H_
#define LIBUVCCAMERA_H_

#include <jni.h>
#include "libusb.h"
#include "libuvc.h"
#include "utilbase.h"

#endif /* LIBUVCCAMERA_H_ */
