/*
 * UVCCamera
 * library and sample to access to UVC web camera on non-rooted Android device
 *
 * Copyright (c) 2015-2017 saki t_saki@serenegiant.com
 *
 * File name: Parameters.h
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
 * \brief Diagnostic helpers for UVC device inspection.
 *
 * Provides string- and JSON-based diagnostics for UVC devices, formats, and
 * stream controls. The JSON output is consumed by the Java layer to expose
 * camera capabilities and the current stream configuration.
 *
 * Exports:
 *     UVCDiags: Diagnostic helper class.
 *
 * Dependencies:
 *     - libUVCCamera.h: Core native includes.
 *     - rapidjson: JSON writer used by descriptor serialization.
 *     - libuvc: UVC descriptor structures.
 *
 * Architecture Note:
 *     Stateless helper; instances are cheap to create and destroy.
 */

#ifndef PARAMETERS_H_
#define PARAMETERS_H_

#pragma interface

#include "libUVCCamera.h"

/**
 * \brief Provide diagnostic string representations for UVC devices and streams.
 *
 * Collects human-readable descriptions of device capabilities, current
 * stream configuration, and supported resolutions. Intended for debugging
 * and logging; does not modify device state.
 *
 * Lifecycle:
 *     Construct → call diagnostic methods → destroy.
 *
 * Thread Safety:
 *     Not thread-safe. Create one instance per thread or external synchronize.
 */
class UVCDiags {
private:
public:
	/**
	 * \brief Construct a diagnostic helper.
	 *
	 * Initializes internal state for diagnostic queries.
	 *
	 * \post Instance is ready for diagnostic calls.
	 */
	UVCDiags();
	/**
	 * \brief Destroy diagnostic helper.
	 *
	 * Releases any internal resources allocated during construction.
	 */
	~UVCDiags();
	/**
	 * \brief Get human-readable device description.
	 *
	 * Builds a multi-line string describing device capabilities, formats,
	 * and supported controls. Caller must free the returned string.
	 *
	 * \param[in] deviceHandle Valid UVC device handle.
	 * \return Pointer to null-terminated description string, or NULL on error.
	 *
	 * \pre deviceHandle must be a valid, open UVC device handle.
	 * \warning Caller owns returned pointer and must free it with free().
	 *
	 * Side Effects:
	 *     - Allocates memory for the description string.
	 *
	 * Code Paths:
	 *     1. Valid handle → enumerates formats and controls → returns description.
	 *     2. Invalid handle → returns NULL.
	 */
	char *getDescriptions(const uvc_device_handle_t *deviceHandle);
	/**
	 * \brief Get current stream configuration as string.
	 *
	 * Formats width, height, fps, and pixel format from stream control.
	 *
	 * \param[in] ctrl Valid stream control structure.
	 * \return Pointer to description string, or NULL on error.
	 *
	 * \pre ctrl must be initialized.
	 * \warning Caller must free returned pointer.
	 */
	char *getCurrentStream(const uvc_stream_ctrl_t *ctrl);
	/**
	 * \brief Get supported preview sizes as string.
	 *
	 * Enumerates supported resolutions and frame rates for the device.
	 *
	 * \param[in] deviceHandle Valid UVC device handle.
	 * \return Pointer to sizes string, or NULL on error.
	 *
	 * \pre deviceHandle must be a valid, open UVC device handle.
	 * \warning Caller must free returned pointer.
	 */
	char *getSupportedSize(const uvc_device_handle_t *deviceHandle);
};

#endif /* PARAMETERS_H_ */
