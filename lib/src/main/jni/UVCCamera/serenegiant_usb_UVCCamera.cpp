/*
 * UVCCamera
 * library and sample to access to UVC web camera on non-rooted Android device
 *
 * Copyright (c) 2014-2017 saki t_saki@serenegiant.com
 *
 * File name: serenegiant_usb_UVCCamera.cpp
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

// JNI trampoline layer for the UVCCamera native library; see UVCCamera.h and libUVCCamera.h for the native API surface.



#if 1	// Disable debug logging
	#ifndef LOG_NDEBUG
		#define	LOG_NDEBUG		// Suppress LOGV/LOGD/MARK output
		#endif
	#undef USE_LOGALL			// Output only the selected LOGx macros
#else
	#define USE_LOGALL
	#undef LOG_NDEBUG
	#undef NDEBUG
#endif

#include <jni.h>
#include <android/native_window_jni.h>

#include "libUVCCamera.h"
#include "UVCCamera.h"

/**
 * \brief Set a long field on a Java object by resolving its class.
 *
 * \param[in] env (JNIEnv *) JNI environment.
 * \param[in] java_obj (jobject) Java object containing the field.
 * \param[in] field_name (char *) Java field name to resolve.
 * \param[in] val (jlong) Value to write.
 *
 * \return The value written. The set is ignored if the field cannot be resolved.
 */
static jlong setField_long(JNIEnv *env, jobject java_obj, const char *field_name, jlong val) {
#if LOCAL_DEBUG
	LOGV("setField_long:");
#endif

	jclass clazz = env->GetObjectClass(java_obj);
	jfieldID field = env->GetFieldID(clazz, field_name, "J");
	if (LIKELY(field))
		env->SetLongField(java_obj, field, val);
	else {
		LOGE("__setField_long:field '%s' not found", field_name);
	}
#ifdef ANDROID_NDK
	env->DeleteLocalRef(clazz);
#endif
	return val;
}

/**
 * \brief Set a long field on a Java object using an already resolved class.
 *
 * \param[in] env (JNIEnv *) JNI environment.
 * \param[in] java_obj (jobject) Java object containing the field.
 * \param[in] clazz (jclass) Java class used to resolve the field.
 * \param[in] field_name (char *) Java field name to resolve.
 * \param[in] val (jlong) Value to write.
 *
 * \return The value written. The set is ignored if the field cannot be resolved.
 */
static jlong __setField_long(JNIEnv *env, jobject java_obj, jclass clazz, const char *field_name, jlong val) {
#if LOCAL_DEBUG
	LOGV("__setField_long:");
#endif

	jfieldID field = env->GetFieldID(clazz, field_name, "J");
	if (LIKELY(field))
		env->SetLongField(java_obj, field, val);
	else {
		LOGE("__setField_long:field '%s' not found", field_name);
	}
	return val;
}

/**
 * \brief Set an int field on a Java object using an already resolved class.
 *
 * \param[in] env (JNIEnv *) JNI environment.
 * \param[in] java_obj (jobject) Java object containing the field.
 * \param[in] clazz (jclass) Java class used to resolve the field.
 * \param[in] field_name (char *) Java field name to resolve.
 * \param[in] val (jint) Value to write.
 *
 * \return The value written. A pending `NoSuchFieldError` is cleared if the field cannot be resolved.
 */
jint __setField_int(JNIEnv *env, jobject java_obj, jclass clazz, const char *field_name, jint val) {
	LOGV("__setField_int:");

	jfieldID id = env->GetFieldID(clazz, field_name, "I");
	if (LIKELY(id))
		env->SetIntField(java_obj, id, val);
	else {
		LOGE("__setField_int:field '%s' not found", field_name);
		env->ExceptionClear();	// clear java.lang.NoSuchFieldError exception
	}
	return val;
}

/**
 * \brief Set an int field on a Java object by resolving its class.
 *
 * \param[in] env (JNIEnv *) JNI environment.
 * \param[in] java_obj (jobject) Java object containing the field.
 * \param[in] field_name (char *) Java field name to resolve.
 * \param[in] val (jint) Value to write.
 *
 * \return The value written. The set is ignored if the field cannot be resolved.
 */
jint setField_int(JNIEnv *env, jobject java_obj, const char *field_name, jint val) {
	LOGV("setField_int:");

	jclass clazz = env->GetObjectClass(java_obj);
	__setField_int(env, java_obj, clazz, field_name, val);
#ifdef ANDROID_NDK
	env->DeleteLocalRef(clazz);
#endif
	return val;
}

/**
 * \brief Allocate the native `UVCCamera` object and store its pointer in the Java instance.
 *
 * \post A new native `UVCCamera` object is allocated and Java `mNativePtr` is set to its pointer.
 *
 * \param[in] env (JNIEnv *) JNI environment.
 * \param[in] thiz (jobject) Java `UVCCamera` instance.
 *
 * \return Native `UVCCamera` pointer handle stored in Java `mNativePtr`.
 */
static ID_TYPE nativeCreate(JNIEnv *env, jobject thiz) {

	ENTER();
	UVCCamera *camera = new UVCCamera();
	setField_long(env, thiz, "mNativePtr", reinterpret_cast<ID_TYPE>(camera));
	RETURN(reinterpret_cast<ID_TYPE>(camera), ID_TYPE);
}

/**
 * \brief Clear `mNativePtr` and delete the native `UVCCamera` object.
 *
 * \pre `id_camera` is null or a live native handle created by `nativeCreate`.
 *
 * \post Java `mNativePtr` is cleared, and the native object is deleted when `id_camera` is non-null.
 *
 * \param[in] env (JNIEnv *) JNI environment.
 * \param[in] thiz (jobject) Java `UVCCamera` instance.
 * \param[in] id_camera (ID_TYPE) Native `UVCCamera` pointer handle stored in Java `mNativePtr`. The trampoline deletes the referenced object when non-null.
 */
static void nativeDestroy(JNIEnv *env, jobject thiz,
	ID_TYPE id_camera) {

	ENTER();
	setField_long(env, thiz, "mNativePtr", 0);
	UVCCamera *camera = reinterpret_cast<UVCCamera *>(id_camera);
	if (LIKELY(camera)) {
		SAFE_DELETE(camera);
	}
	EXIT();
}
//======================================================================
/**
 * \brief Connect to the UVC device using the USB file descriptor and device identity.
 *
 * \pre `id_camera` is non-null and `fd > 0` for an actual connection attempt.
 *
 * \post The UTF-8 buffer obtained from `usbfs_str` is released before return.
 *
 * \param[in] env (JNIEnv *) JNI environment.
 * \param[in] thiz (jobject) Java `UVCCamera` instance.
 * \param[in] id_camera (ID_TYPE) Native `UVCCamera` pointer handle stored in Java `mNativePtr`. The trampoline does not allocate or free this handle.
 * \param[in] vid (jint) USB vendor ID.
 * \param[in] pid (jint) USB product ID.
 * \param[in] fd (jint) USB file descriptor. Must be greater than 0 for `nativeConnect`.
 * \param[in] busNum (jint) USB bus number.
 * \param[in] devAddr (jint) USB device address.
 * \param[in] usbfs_str (jstring) USBFS path as a Java string.
 *
 * \return Value returned by `UVCCamera::connect` when `id_camera` is non-null and `fd > 0`; `JNI_ERR` otherwise.
 */
static jint nativeConnect(JNIEnv *env, jobject thiz,
	ID_TYPE id_camera,
	jint vid, jint pid, jint fd,
	jint busNum, jint devAddr, jstring usbfs_str) {

	ENTER();
	int result = JNI_ERR;
	UVCCamera *camera = reinterpret_cast<UVCCamera *>(id_camera);
	const char *c_usbfs = env->GetStringUTFChars(usbfs_str, JNI_FALSE);
	if (LIKELY(camera && (fd > 0))) {
//		libusb_set_debug(NULL, LIBUSB_LOG_LEVEL_DEBUG);
		result =  camera->connect(vid, pid, fd, busNum, devAddr, c_usbfs);
	}
	env->ReleaseStringUTFChars(usbfs_str, c_usbfs);
	RETURN(result, jint);
}

/**
 * \brief Release the native camera and associated native resources.
 *
 * \pre `id_camera` is null or a live native handle created by `nativeCreate`.
 *
 * \post `UVCCamera::release` is called when `id_camera` is non-null.
 *
 * \param[in] env (JNIEnv *) JNI environment.
 * \param[in] thiz (jobject) Java `UVCCamera` instance.
 * \param[in] id_camera (ID_TYPE) Native `UVCCamera` pointer handle stored in Java `mNativePtr`. The trampoline does not allocate or free this handle.
 *
 * \return Value returned by `UVCCamera::release` when `id_camera` is non-null; `JNI_ERR` otherwise.
 */
static jint nativeRelease(JNIEnv *env, jobject thiz,
	ID_TYPE id_camera) {

	ENTER();
	int result = JNI_ERR;
	UVCCamera *camera = reinterpret_cast<UVCCamera *>(id_camera);
	if (LIKELY(camera)) {
		result = camera->release();
	}
	RETURN(result, jint);
}
//======================================================================
/**
 * \brief Install the Java status callback on the native camera.
 *
 * \pre `id_camera` is non-null for a native callback installation.
 *
 * \post A global reference to the Java callback object is created before the native call.
 *
 * \param[in] env (JNIEnv *) JNI environment.
 * \param[in] thiz (jobject) Java `UVCCamera` instance.
 * \param[in] id_camera (ID_TYPE) Native `UVCCamera` pointer handle stored in Java `mNativePtr`. The trampoline does not allocate or free this handle.
 * \param[in] jIStatusCallback (jobject) Java `IStatusCallback` object.
 *
 * \return Value returned by `UVCCamera::setStatusCallback` when `id_camera` is non-null; `JNI_ERR` otherwise.
 */
static jint nativeSetStatusCallback(JNIEnv *env, jobject thiz,
	ID_TYPE id_camera, jobject jIStatusCallback) {

	jint result = JNI_ERR;
	ENTER();
	UVCCamera *camera = reinterpret_cast<UVCCamera *>(id_camera);
	if (LIKELY(camera)) {
		jobject status_callback_obj = env->NewGlobalRef(jIStatusCallback);
		result = camera->setStatusCallback(env, status_callback_obj);
	}
	RETURN(result, jint);
}

/**
 * \brief Install the Java button callback on the native camera.
 *
 * \pre `id_camera` is non-null for a native callback installation.
 *
 * \post A global reference to the Java callback object is created before the native call.
 *
 * \param[in] env (JNIEnv *) JNI environment.
 * \param[in] thiz (jobject) Java `UVCCamera` instance.
 * \param[in] id_camera (ID_TYPE) Native `UVCCamera` pointer handle stored in Java `mNativePtr`. The trampoline does not allocate or free this handle.
 * \param[in] jIButtonCallback (jobject) Java `IButtonCallback` object.
 *
 * \return Value returned by `UVCCamera::setButtonCallback` when `id_camera` is non-null; `JNI_ERR` otherwise.
 */
static jint nativeSetButtonCallback(JNIEnv *env, jobject thiz,
	ID_TYPE id_camera, jobject jIButtonCallback) {

	jint result = JNI_ERR;
	ENTER();
	UVCCamera *camera = reinterpret_cast<UVCCamera *>(id_camera);
	if (LIKELY(camera)) {
		jobject button_callback_obj = env->NewGlobalRef(jIButtonCallback);
		result = camera->setButtonCallback(env, button_callback_obj);
	}
	RETURN(result, jint);
}

/**
 * \brief Query the supported frame sizes as a Java string.
 *
 * \pre `id_camera` is non-null for the native call; otherwise `JNI_ERR` is returned.
 *
 * \param[in] env (JNIEnv *) JNI environment.
 * \param[in] thiz (jobject) Java `UVCCamera` instance.
 * \param[in] id_camera (ID_TYPE) Native `UVCCamera` pointer handle stored in Java `mNativePtr`. The trampoline does not allocate or free this handle.
 *
 * \return Java `String` containing the supported sizes, or `NULL` when unavailable or `id_camera` is null.
 */
static jobject nativeGetSupportedSize(JNIEnv *env, jobject thiz,
	ID_TYPE id_camera) {

	ENTER();
	jstring result = NULL;
	UVCCamera *camera = reinterpret_cast<UVCCamera *>(id_camera);
	if (LIKELY(camera)) {
		char *c_str = camera->getSupportedSize();
		if (LIKELY(c_str)) {
			result = env->NewStringUTF(c_str);
			free(c_str);
		}
	}
	RETURN(result, jobject);
}
//======================================================================
/**
 * \brief Set the preview size, fps range, mode, and bandwidth.
 *
 * \pre `id_camera` is non-null for the native call; otherwise `JNI_ERR` is returned.
 *
 * \param[in] env (JNIEnv *) JNI environment.
 * \param[in] thiz (jobject) Java `UVCCamera` instance.
 * \param[in] id_camera (ID_TYPE) Native `UVCCamera` pointer handle stored in Java `mNativePtr`. The trampoline does not allocate or free this handle.
 * \param[in] width (jint) Preview width.
 * \param[in] height (jint) Preview height.
 * \param[in] min_fps (jint) Minimum preview fps.
 * \param[in] max_fps (jint) Maximum preview fps.
 * \param[in] mode (jint) Preview size mode.
 * \param[in] bandwidth (jfloat) Bandwidth value.
 *
 * \return Value returned by `UVCCamera::setPreviewSize` when `id_camera` is non-null; `JNI_ERR` otherwise.
 */
static jint nativeSetPreviewSize(JNIEnv *env, jobject thiz,
	ID_TYPE id_camera, jint width, jint height, jint min_fps, jint max_fps, jint mode, jfloat bandwidth) {

	ENTER();
	UVCCamera *camera = reinterpret_cast<UVCCamera *>(id_camera);
	if (LIKELY(camera)) {
		return camera->setPreviewSize(width, height, min_fps, max_fps, mode, bandwidth);
	}
	RETURN(JNI_ERR, jint);
}

/**
 * \brief Start preview on the native camera.
 *
 * \pre `id_camera` is non-null for the native call; otherwise `JNI_ERR` is returned.
 *
 * \param[in] env (JNIEnv *) JNI environment.
 * \param[in] thiz (jobject) Java `UVCCamera` instance.
 * \param[in] id_camera (ID_TYPE) Native `UVCCamera` pointer handle stored in Java `mNativePtr`. The trampoline does not allocate or free this handle.
 *
 * \return Value returned by `UVCCamera::startPreview` when `id_camera` is non-null; `JNI_ERR` otherwise.
 */
static jint nativeStartPreview(JNIEnv *env, jobject thiz,
	ID_TYPE id_camera) {

	ENTER();
	UVCCamera *camera = reinterpret_cast<UVCCamera *>(id_camera);
	if (LIKELY(camera)) {
		return camera->startPreview();
	}
	RETURN(JNI_ERR, jint);
}

/**
 * \brief Stop preview on the native camera.
 *
 * \pre `id_camera` is non-null for the native call; otherwise `JNI_ERR` is returned.
 *
 * \param[in] env (JNIEnv *) JNI environment.
 * \param[in] thiz (jobject) Java `UVCCamera` instance.
 * \param[in] id_camera (ID_TYPE) Native `UVCCamera` pointer handle stored in Java `mNativePtr`. The trampoline does not allocate or free this handle.
 *
 * \return Value returned by `UVCCamera::stopPreview` when `id_camera` is non-null; `JNI_ERR` otherwise.
 */
static jint nativeStopPreview(JNIEnv *env, jobject thiz,
	ID_TYPE id_camera) {

	jint result = JNI_ERR;
	ENTER();
	UVCCamera *camera = reinterpret_cast<UVCCamera *>(id_camera);
	if (LIKELY(camera)) {
		result = camera->stopPreview();
	}
	RETURN(result, jint);
}

/**
 * \brief Set the Android surface used for preview display.
 *
 * \pre `id_camera` is non-null for a native display update.
 *
 * \post A null Java surface is converted to a null `ANativeWindow*`.
 *
 * \param[in] env (JNIEnv *) JNI environment.
 * \param[in] thiz (jobject) Java `UVCCamera` instance.
 * \param[in] id_camera (ID_TYPE) Native `UVCCamera` pointer handle stored in Java `mNativePtr`. The trampoline does not allocate or free this handle.
 * \param[in] jSurface (jobject) Android surface. May be null.
 *
 * \return Value returned by `UVCCamera::setPreviewDisplay` when `id_camera` is non-null; `JNI_ERR` otherwise.
 */
static jint nativeSetPreviewDisplay(JNIEnv *env, jobject thiz,
	ID_TYPE id_camera, jobject jSurface) {

	jint result = JNI_ERR;
	ENTER();
	UVCCamera *camera = reinterpret_cast<UVCCamera *>(id_camera);
	if (LIKELY(camera)) {
		ANativeWindow *preview_window = jSurface ? ANativeWindow_fromSurface(env, jSurface) : NULL;
		result = camera->setPreviewDisplay(preview_window);
	}
	RETURN(result, jint);
}

/**
 * \brief Install the Java frame callback and pixel format on the native camera.
 *
 * \pre `id_camera` is non-null for a native callback installation.
 *
 * \post A global reference to the Java callback object is created before the native call.
 *
 * \param[in] env (JNIEnv *) JNI environment.
 * \param[in] thiz (jobject) Java `UVCCamera` instance.
 * \param[in] id_camera (ID_TYPE) Native `UVCCamera` pointer handle stored in Java `mNativePtr`. The trampoline does not allocate or free this handle.
 * \param[in] jIFrameCallback (jobject) Java `IFrameCallback` object.
 * \param[in] pixel_format (jint) Pixel format requested for frame callbacks.
 *
 * \return Value returned by `UVCCamera::setFrameCallback` when `id_camera` is non-null; `JNI_ERR` otherwise.
 */
static jint nativeSetFrameCallback(JNIEnv *env, jobject thiz,
	ID_TYPE id_camera, jobject jIFrameCallback, jint pixel_format) {

	jint result = JNI_ERR;
	ENTER();
	UVCCamera *camera = reinterpret_cast<UVCCamera *>(id_camera);
	if (LIKELY(camera)) {
		jobject frame_callback_obj = env->NewGlobalRef(jIFrameCallback);
		result = camera->setFrameCallback(env, frame_callback_obj, pixel_format);
	}
	RETURN(result, jint);
}

/**
 * \brief Set the Android surface used for capture display.
 *
 * \pre `id_camera` is non-null for a native display update.
 *
 * \post A null Java surface is converted to a null `ANativeWindow*`.
 *
 * \param[in] env (JNIEnv *) JNI environment.
 * \param[in] thiz (jobject) Java `UVCCamera` instance.
 * \param[in] id_camera (ID_TYPE) Native `UVCCamera` pointer handle stored in Java `mNativePtr`. The trampoline does not allocate or free this handle.
 * \param[in] jSurface (jobject) Android surface. May be null.
 *
 * \return Value returned by `UVCCamera::setCaptureDisplay` when `id_camera` is non-null; `JNI_ERR` otherwise.
 */
static jint nativeSetCaptureDisplay(JNIEnv *env, jobject thiz,
	ID_TYPE id_camera, jobject jSurface) {

	jint result = JNI_ERR;
	ENTER();
	UVCCamera *camera = reinterpret_cast<UVCCamera *>(id_camera);
	if (LIKELY(camera)) {
		ANativeWindow *capture_window = jSurface ? ANativeWindow_fromSurface(env, jSurface) : NULL;
		result = camera->setCaptureDisplay(capture_window);
	}
	RETURN(result, jint);
}
//======================================================================
/**
 * \brief Query the UVC control support bit mask.
 *
 * \pre `id_camera` is non-null for the native call; otherwise `JNI_ERR` is returned.
 *
 * \param[in] env (JNIEnv *) JNI environment.
 * \param[in] thiz (jobject) Java `UVCCamera` instance.
 * \param[in] id_camera (ID_TYPE) Native `UVCCamera` pointer handle stored in Java `mNativePtr`. The trampoline does not allocate or free this handle.
 *
 * \return Support bit mask, or 0 when the native query fails or `id_camera` is null.
 */
static jlong nativeGetCtrlSupports(JNIEnv *env, jobject thiz,
	ID_TYPE id_camera) {

	jlong result = 0;
	ENTER();
	UVCCamera *camera = reinterpret_cast<UVCCamera *>(id_camera);
	if (LIKELY(camera)) {
		uint64_t supports;
		int r = camera->getCtrlSupports(&supports);
		if (!r)
			result = supports;
	}
	RETURN(result, jlong);
}

/**
 * \brief Query the processing-unit control support bit mask.
 *
 * \pre `id_camera` is non-null for the native call; otherwise `JNI_ERR` is returned.
 *
 * \param[in] env (JNIEnv *) JNI environment.
 * \param[in] thiz (jobject) Java `UVCCamera` instance.
 * \param[in] id_camera (ID_TYPE) Native `UVCCamera` pointer handle stored in Java `mNativePtr`. The trampoline does not allocate or free this handle.
 *
 * \return Support bit mask, or 0 when the native query fails or `id_camera` is null.
 */
static jlong nativeGetProcSupports(JNIEnv *env, jobject thiz,
	ID_TYPE id_camera) {

	jlong result = 0;
	ENTER();
	UVCCamera *camera = reinterpret_cast<UVCCamera *>(id_camera);
	if (LIKELY(camera)) {
		uint64_t supports;
		int r = camera->getProcSupports(&supports);
		if (!r)
			result = supports;
	}
	RETURN(result, jlong);
}
//======================================================================
// The corresponding Java method must not be static
/**
 * \brief Update the ScanningMode limit values exposed to the Java instance.
 *
 * \pre `id_camera` is non-null for a limit update.
 *
 * \post On success, writes `mScanningModeMin`, `mScanningModeMax`, `mScanningModeDef` on the Java instance.
 *
 * \param[in] env (JNIEnv *) JNI environment.
 * \param[in] thiz (jobject) Java `UVCCamera` instance.
 * \param[in] id_camera (ID_TYPE) Native `UVCCamera` pointer handle stored in Java `mNativePtr`. The trampoline does not allocate or free this handle.
 *
 * \return 0 on success, the `UVCCamera::updateScanningModeLimit` error otherwise, or `JNI_ERR` when `id_camera` is null.
 */
static jint nativeUpdateScanningModeLimit(JNIEnv *env, jobject thiz,
	ID_TYPE id_camera) {
	jint result = JNI_ERR;
	ENTER();
	UVCCamera *camera = reinterpret_cast<UVCCamera *>(id_camera);
	if (LIKELY(camera)) {
		int min, max, def;
		result = camera->updateScanningModeLimit(min, max, def);
		if (!result) {
			// Write the value back to Java
			setField_int(env, thiz, "mScanningModeMin", min);
			setField_int(env, thiz, "mScanningModeMax", max);
			setField_int(env, thiz, "mScanningModeDef", def);
		}
	}
	RETURN(result, jint);
}

/**
 * \brief Call `UVCCamera::setScanningMode` to set the ScanningMode value.
 *
 * \pre `id_camera` is non-null for the native call; otherwise `JNI_ERR` is returned.
 *
 * \param[in] env (JNIEnv *) JNI environment.
 * \param[in] thiz (jobject) Java `UVCCamera` instance.
 * \param[in] id_camera (ID_TYPE) Native `UVCCamera` pointer handle stored in Java `mNativePtr`. The trampoline does not allocate or free this handle.
 * \param[in] scanningMode (jint) Value passed to the native setter.
 *
 * \return Value returned by `UVCCamera::setScanningMode` when `id_camera` is non-null; `JNI_ERR` otherwise.
 */
static jint nativeSetScanningMode(JNIEnv *env, jobject thiz,
	ID_TYPE id_camera, jint scanningMode) {

	jint result = JNI_ERR;
	ENTER();
	UVCCamera *camera = reinterpret_cast<UVCCamera *>(id_camera);
	if (LIKELY(camera)) {
		result = camera->setScanningMode(scanningMode);
	}
	RETURN(result, jint);
}

/**
 * \brief Call `UVCCamera::getScanningMode` to read the ScanningMode value.
 *
 * \pre `id_camera` is non-null for the native call; otherwise `JNI_ERR` is returned.
 *
 * \param[in] env (JNIEnv *) JNI environment.
 * \param[in] thiz (jobject) Java `UVCCamera` instance.
 * \param[in] id_camera (ID_TYPE) Native `UVCCamera` pointer handle stored in Java `mNativePtr`. The trampoline does not allocate or free this handle.
 *
 * \return Value returned by `UVCCamera::getScanningMode` when `id_camera` is non-null; `JNI_ERR` otherwise.
 */
static jint nativeGetScanningMode(JNIEnv *env, jobject thiz,
	ID_TYPE id_camera) {

	jint result = JNI_ERR;
	ENTER();
	UVCCamera *camera = reinterpret_cast<UVCCamera *>(id_camera);
	if (LIKELY(camera)) {
		result = camera->getScanningMode();
	}
	RETURN(result, jint);
}
//======================================================================
// The corresponding Java method must not be static
/**
 * \brief Update the ExposureMode limit values exposed to the Java instance.
 *
 * \pre `id_camera` is non-null for a limit update.
 *
 * \post On success, writes `mExposureModeMin`, `mExposureModeMax`, `mExposureModeDef` on the Java instance.
 *
 * \param[in] env (JNIEnv *) JNI environment.
 * \param[in] thiz (jobject) Java `UVCCamera` instance.
 * \param[in] id_camera (ID_TYPE) Native `UVCCamera` pointer handle stored in Java `mNativePtr`. The trampoline does not allocate or free this handle.
 *
 * \return 0 on success, the `UVCCamera::updateExposureModeLimit` error otherwise, or `JNI_ERR` when `id_camera` is null.
 */
static jint nativeUpdateExposureModeLimit(JNIEnv *env, jobject thiz,
	ID_TYPE id_camera) {
	jint result = JNI_ERR;
	ENTER();
	UVCCamera *camera = reinterpret_cast<UVCCamera *>(id_camera);
	if (LIKELY(camera)) {
		int min, max, def;
		result = camera->updateExposureModeLimit(min, max, def);
		if (!result) {
			// Write the value back to Java
			setField_int(env, thiz, "mExposureModeMin", min);
			setField_int(env, thiz, "mExposureModeMax", max);
			setField_int(env, thiz, "mExposureModeDef", def);
		}
	}
	RETURN(result, jint);
}

/**
 * \brief Call `UVCCamera::setExposureMode` to set the ExposureMode value.
 *
 * \pre `id_camera` is non-null for the native call; otherwise `JNI_ERR` is returned.
 *
 * \param[in] env (JNIEnv *) JNI environment.
 * \param[in] thiz (jobject) Java `UVCCamera` instance.
 * \param[in] id_camera (ID_TYPE) Native `UVCCamera` pointer handle stored in Java `mNativePtr`. The trampoline does not allocate or free this handle.
 * \param[in] exposureMode (int) Value passed to the native setter.
 *
 * \return Value returned by `UVCCamera::setExposureMode` when `id_camera` is non-null; `JNI_ERR` otherwise.
 */
static jint nativeSetExposureMode(JNIEnv *env, jobject thiz,
	ID_TYPE id_camera, int exposureMode) {

	jint result = JNI_ERR;
	ENTER();
	UVCCamera *camera = reinterpret_cast<UVCCamera *>(id_camera);
	if (LIKELY(camera)) {
		result = camera->setExposureMode(exposureMode);
	}
	RETURN(result, jint);
}

/**
 * \brief Call `UVCCamera::getExposureMode` to read the ExposureMode value.
 *
 * \pre `id_camera` is non-null for the native call; otherwise `JNI_ERR` is returned.
 *
 * \param[in] env (JNIEnv *) JNI environment.
 * \param[in] thiz (jobject) Java `UVCCamera` instance.
 * \param[in] id_camera (ID_TYPE) Native `UVCCamera` pointer handle stored in Java `mNativePtr`. The trampoline does not allocate or free this handle.
 *
 * \return Value returned by `UVCCamera::getExposureMode` when `id_camera` is non-null; `JNI_ERR` otherwise.
 */
static jint nativeGetExposureMode(JNIEnv *env, jobject thiz,
	ID_TYPE id_camera) {

	jint result = JNI_ERR;
	ENTER();
	UVCCamera *camera = reinterpret_cast<UVCCamera *>(id_camera);
	if (LIKELY(camera)) {
		result = camera->getExposureMode();
	}
	RETURN(result, jint);
}
//======================================================================
// The corresponding Java method must not be static
/**
 * \brief Update the ExposurePriority limit values exposed to the Java instance.
 *
 * \pre `id_camera` is non-null for a limit update.
 *
 * \post On success, writes `mExposurePriorityMin`, `mExposurePriorityMax`, `mExposurePriorityDef` on the Java instance.
 *
 * \param[in] env (JNIEnv *) JNI environment.
 * \param[in] thiz (jobject) Java `UVCCamera` instance.
 * \param[in] id_camera (ID_TYPE) Native `UVCCamera` pointer handle stored in Java `mNativePtr`. The trampoline does not allocate or free this handle.
 *
 * \return 0 on success, the `UVCCamera::updateExposurePriorityLimit` error otherwise, or `JNI_ERR` when `id_camera` is null.
 */
static jint nativeUpdateExposurePriorityLimit(JNIEnv *env, jobject thiz,
	ID_TYPE id_camera) {
	jint result = JNI_ERR;
	ENTER();
	UVCCamera *camera = reinterpret_cast<UVCCamera *>(id_camera);
	if (LIKELY(camera)) {
		int min, max, def;
		result = camera->updateExposurePriorityLimit(min, max, def);
		if (!result) {
			// Write the value back to Java
			setField_int(env, thiz, "mExposurePriorityMin", min);
			setField_int(env, thiz, "mExposurePriorityMax", max);
			setField_int(env, thiz, "mExposurePriorityDef", def);
		}
	}
	RETURN(result, jint);
}

/**
 * \brief Call `UVCCamera::setExposurePriority` to set the ExposurePriority value.
 *
 * \pre `id_camera` is non-null for the native call; otherwise `JNI_ERR` is returned.
 *
 * \param[in] env (JNIEnv *) JNI environment.
 * \param[in] thiz (jobject) Java `UVCCamera` instance.
 * \param[in] id_camera (ID_TYPE) Native `UVCCamera` pointer handle stored in Java `mNativePtr`. The trampoline does not allocate or free this handle.
 * \param[in] priority (int) Value passed to the native setter.
 *
 * \return Value returned by `UVCCamera::setExposurePriority` when `id_camera` is non-null; `JNI_ERR` otherwise.
 */
static jint nativeSetExposurePriority(JNIEnv *env, jobject thiz,
	ID_TYPE id_camera, int priority) {

	jint result = JNI_ERR;
	ENTER();
	UVCCamera *camera = reinterpret_cast<UVCCamera *>(id_camera);
	if (LIKELY(camera)) {
		result = camera->setExposurePriority(priority);
	}
	RETURN(result, jint);
}

/**
 * \brief Call `UVCCamera::getExposurePriority` to read the ExposurePriority value.
 *
 * \pre `id_camera` is non-null for the native call; otherwise `JNI_ERR` is returned.
 *
 * \param[in] env (JNIEnv *) JNI environment.
 * \param[in] thiz (jobject) Java `UVCCamera` instance.
 * \param[in] id_camera (ID_TYPE) Native `UVCCamera` pointer handle stored in Java `mNativePtr`. The trampoline does not allocate or free this handle.
 *
 * \return Value returned by `UVCCamera::getExposurePriority` when `id_camera` is non-null; `JNI_ERR` otherwise.
 */
static jint nativeGetExposurePriority(JNIEnv *env, jobject thiz,
	ID_TYPE id_camera) {

	jint result = JNI_ERR;
	ENTER();
	UVCCamera *camera = reinterpret_cast<UVCCamera *>(id_camera);
	if (LIKELY(camera)) {
		result = camera->getExposurePriority();
	}
	RETURN(result, jint);
}
//======================================================================
// The corresponding Java method must not be static
/**
 * \brief Update the Exposure limit values exposed to the Java instance.
 *
 * \pre `id_camera` is non-null for a limit update.
 *
 * \post On success, writes `mExposureMin`, `mExposureMax`, `mExposureDef` on the Java instance.
 *
 * \param[in] env (JNIEnv *) JNI environment.
 * \param[in] thiz (jobject) Java `UVCCamera` instance.
 * \param[in] id_camera (ID_TYPE) Native `UVCCamera` pointer handle stored in Java `mNativePtr`. The trampoline does not allocate or free this handle.
 *
 * \return 0 on success, the `UVCCamera::updateExposureLimit` error otherwise, or `JNI_ERR` when `id_camera` is null.
 */
static jint nativeUpdateExposureLimit(JNIEnv *env, jobject thiz,
	ID_TYPE id_camera) {
	jint result = JNI_ERR;
	ENTER();
	UVCCamera *camera = reinterpret_cast<UVCCamera *>(id_camera);
	if (LIKELY(camera)) {
		int min, max, def;
		result = camera->updateExposureLimit(min, max, def);
		if (!result) {
			// Write the value back to Java
			setField_int(env, thiz, "mExposureMin", min);
			setField_int(env, thiz, "mExposureMax", max);
			setField_int(env, thiz, "mExposureDef", def);
		}
	}
	RETURN(result, jint);
}

/**
 * \brief Call `UVCCamera::setExposure` to set the Exposure value.
 *
 * \pre `id_camera` is non-null for the native call; otherwise `JNI_ERR` is returned.
 *
 * \param[in] env (JNIEnv *) JNI environment.
 * \param[in] thiz (jobject) Java `UVCCamera` instance.
 * \param[in] id_camera (ID_TYPE) Native `UVCCamera` pointer handle stored in Java `mNativePtr`. The trampoline does not allocate or free this handle.
 * \param[in] exposure (int) Value passed to the native setter.
 *
 * \return Value returned by `UVCCamera::setExposure` when `id_camera` is non-null; `JNI_ERR` otherwise.
 */
static jint nativeSetExposure(JNIEnv *env, jobject thiz,
	ID_TYPE id_camera, int exposure) {

	jint result = JNI_ERR;
	ENTER();
	UVCCamera *camera = reinterpret_cast<UVCCamera *>(id_camera);
	if (LIKELY(camera)) {
		result = camera->setExposure(exposure);
	}
	RETURN(result, jint);
}

/**
 * \brief Call `UVCCamera::getExposure` to read the Exposure value.
 *
 * \pre `id_camera` is non-null for the native call; otherwise `JNI_ERR` is returned.
 *
 * \param[in] env (JNIEnv *) JNI environment.
 * \param[in] thiz (jobject) Java `UVCCamera` instance.
 * \param[in] id_camera (ID_TYPE) Native `UVCCamera` pointer handle stored in Java `mNativePtr`. The trampoline does not allocate or free this handle.
 *
 * \return Value returned by `UVCCamera::getExposure` when `id_camera` is non-null; `JNI_ERR` otherwise.
 */
static jint nativeGetExposure(JNIEnv *env, jobject thiz,
	ID_TYPE id_camera) {

	jint result = JNI_ERR;
	ENTER();
	UVCCamera *camera = reinterpret_cast<UVCCamera *>(id_camera);
	if (LIKELY(camera)) {
		result = camera->getExposure();
	}
	RETURN(result, jint);
}
//======================================================================
// The corresponding Java method must not be static
/**
 * \brief Update the ExposureRel limit values exposed to the Java instance.
 *
 * \pre `id_camera` is non-null for a limit update.
 *
 * \post On success, writes `mExposureRelMin`, `mExposureRelMax`, `mExposureRelDef` on the Java instance.
 *
 * \param[in] env (JNIEnv *) JNI environment.
 * \param[in] thiz (jobject) Java `UVCCamera` instance.
 * \param[in] id_camera (ID_TYPE) Native `UVCCamera` pointer handle stored in Java `mNativePtr`. The trampoline does not allocate or free this handle.
 *
 * \return 0 on success, the `UVCCamera::updateExposureRelLimit` error otherwise, or `JNI_ERR` when `id_camera` is null.
 */
static jint nativeUpdateExposureRelLimit(JNIEnv *env, jobject thiz,
	ID_TYPE id_camera) {
	jint result = JNI_ERR;
	ENTER();
	UVCCamera *camera = reinterpret_cast<UVCCamera *>(id_camera);
	if (LIKELY(camera)) {
		int min, max, def;
		result = camera->updateExposureRelLimit(min, max, def);
		if (!result) {
			// Write the value back to Java
			setField_int(env, thiz, "mExposureRelMin", min);
			setField_int(env, thiz, "mExposureRelMax", max);
			setField_int(env, thiz, "mExposureRelDef", def);
		}
	}
	RETURN(result, jint);
}

/**
 * \brief Call `UVCCamera::setExposureRel` to set the ExposureRel value.
 *
 * \pre `id_camera` is non-null for the native call; otherwise `JNI_ERR` is returned.
 *
 * \param[in] env (JNIEnv *) JNI environment.
 * \param[in] thiz (jobject) Java `UVCCamera` instance.
 * \param[in] id_camera (ID_TYPE) Native `UVCCamera` pointer handle stored in Java `mNativePtr`. The trampoline does not allocate or free this handle.
 * \param[in] exposure_rel (jint) Value passed to the native setter.
 *
 * \return Value returned by `UVCCamera::setExposureRel` when `id_camera` is non-null; `JNI_ERR` otherwise.
 */
static jint nativeSetExposureRel(JNIEnv *env, jobject thiz,
	ID_TYPE id_camera, jint exposure_rel) {

	jint result = JNI_ERR;
	ENTER();
	UVCCamera *camera = reinterpret_cast<UVCCamera *>(id_camera);
	if (LIKELY(camera)) {
		result = camera->setExposureRel(exposure_rel);
	}
	RETURN(result, jint);
}

/**
 * \brief Call `UVCCamera::getExposureRel` to read the ExposureRel value.
 *
 * \pre `id_camera` is non-null for the native call; otherwise `JNI_ERR` is returned.
 *
 * \param[in] env (JNIEnv *) JNI environment.
 * \param[in] thiz (jobject) Java `UVCCamera` instance.
 * \param[in] id_camera (ID_TYPE) Native `UVCCamera` pointer handle stored in Java `mNativePtr`. The trampoline does not allocate or free this handle.
 *
 * \return Value returned by `UVCCamera::getExposureRel` when `id_camera` is non-null; `JNI_ERR` otherwise.
 */
static jint nativeGetExposureRel(JNIEnv *env, jobject thiz,
	ID_TYPE id_camera) {

	jint result = JNI_ERR;
	ENTER();
	UVCCamera *camera = reinterpret_cast<UVCCamera *>(id_camera);
	if (LIKELY(camera)) {
		result = camera->getExposureRel();
	}
	RETURN(result, jint);
}
//======================================================================
// The corresponding Java method must not be static
/**
 * \brief Update the AutoFocus limit values exposed to the Java instance.
 *
 * \pre `id_camera` is non-null for a limit update.
 *
 * \post On success, writes `mAutoFocusMin`, `mAutoFocusMax`, `mAutoFocusDef` on the Java instance.
 *
 * \param[in] env (JNIEnv *) JNI environment.
 * \param[in] thiz (jobject) Java `UVCCamera` instance.
 * \param[in] id_camera (ID_TYPE) Native `UVCCamera` pointer handle stored in Java `mNativePtr`. The trampoline does not allocate or free this handle.
 *
 * \return 0 on success, the `UVCCamera::updateAutoFocusLimit` error otherwise, or `JNI_ERR` when `id_camera` is null.
 */
static jint nativeUpdateAutoFocusLimit(JNIEnv *env, jobject thiz,
	ID_TYPE id_camera) {
	jint result = JNI_ERR;
	ENTER();
	UVCCamera *camera = reinterpret_cast<UVCCamera *>(id_camera);
	if (LIKELY(camera)) {
		int min, max, def;
		result = camera->updateAutoFocusLimit(min, max, def);
		if (!result) {
			// Write the value back to Java
			setField_int(env, thiz, "mAutoFocusMin", min);
			setField_int(env, thiz, "mAutoFocusMax", max);
			setField_int(env, thiz, "mAutoFocusDef", def);
		}
	}
	RETURN(result, jint);
}

/**
 * \brief Call `UVCCamera::setAutoFocus` to set the AutoFocus value.
 *
 * \pre `id_camera` is non-null for the native call; otherwise `JNI_ERR` is returned.
 *
 * \param[in] env (JNIEnv *) JNI environment.
 * \param[in] thiz (jobject) Java `UVCCamera` instance.
 * \param[in] id_camera (ID_TYPE) Native `UVCCamera` pointer handle stored in Java `mNativePtr`. The trampoline does not allocate or free this handle.
 * \param[in] autofocus (jboolean) Value passed to the native setter.
 *
 * \return Value returned by `UVCCamera::setAutoFocus` when `id_camera` is non-null; `JNI_ERR` otherwise.
 */
static jint nativeSetAutoFocus(JNIEnv *env, jobject thiz,
	ID_TYPE id_camera, jboolean autofocus) {

	jint result = JNI_ERR;
	ENTER();
	UVCCamera *camera = reinterpret_cast<UVCCamera *>(id_camera);
	if (LIKELY(camera)) {
		result = camera->setAutoFocus(autofocus);
	}
	RETURN(result, jint);
}

/**
 * \brief Call `UVCCamera::getAutoFocus` to read the AutoFocus value.
 *
 * \pre `id_camera` is non-null for the native call; otherwise `JNI_ERR` is returned.
 *
 * \param[in] env (JNIEnv *) JNI environment.
 * \param[in] thiz (jobject) Java `UVCCamera` instance.
 * \param[in] id_camera (ID_TYPE) Native `UVCCamera` pointer handle stored in Java `mNativePtr`. The trampoline does not allocate or free this handle.
 *
 * \return Value returned by `UVCCamera::getAutoFocus` when `id_camera` is non-null; `JNI_ERR` otherwise.
 */
static jint nativeGetAutoFocus(JNIEnv *env, jobject thiz,
	ID_TYPE id_camera) {

	jint result = JNI_ERR;
	ENTER();
	UVCCamera *camera = reinterpret_cast<UVCCamera *>(id_camera);
	if (LIKELY(camera)) {
		result = camera->getAutoFocus();
	}
	RETURN(result, jint);
}
//======================================================================
// The corresponding Java method must not be static
/**
 * \brief Update the AutoWhiteBlance limit values exposed to the Java instance.
 *
 * \pre `id_camera` is non-null for a limit update.
 *
 * \post On success, writes `mAutoWhiteBlanceMin`, `mAutoWhiteBlanceMax`, `mAutoWhiteBlanceDef` on the Java instance.
 *
 * \param[in] env (JNIEnv *) JNI environment.
 * \param[in] thiz (jobject) Java `UVCCamera` instance.
 * \param[in] id_camera (ID_TYPE) Native `UVCCamera` pointer handle stored in Java `mNativePtr`. The trampoline does not allocate or free this handle.
 *
 * \return 0 on success, the `UVCCamera::updateAutoWhiteBlanceLimit` error otherwise, or `JNI_ERR` when `id_camera` is null.
 */
static jint nativeUpdateAutoWhiteBlanceLimit(JNIEnv *env, jobject thiz,
	ID_TYPE id_camera) {
	jint result = JNI_ERR;
	ENTER();
	UVCCamera *camera = reinterpret_cast<UVCCamera *>(id_camera);
	if (LIKELY(camera)) {
		int min, max, def;
		result = camera->updateAutoWhiteBlanceLimit(min, max, def);
		if (!result) {
			// Write the value back to Java
			setField_int(env, thiz, "mAutoWhiteBlanceMin", min);
			setField_int(env, thiz, "mAutoWhiteBlanceMax", max);
			setField_int(env, thiz, "mAutoWhiteBlanceDef", def);
		}
	}
	RETURN(result, jint);
}

/**
 * \brief Call `UVCCamera::setAutoWhiteBlance` to set the AutoWhiteBlance value.
 *
 * \pre `id_camera` is non-null for the native call; otherwise `JNI_ERR` is returned.
 *
 * \param[in] env (JNIEnv *) JNI environment.
 * \param[in] thiz (jobject) Java `UVCCamera` instance.
 * \param[in] id_camera (ID_TYPE) Native `UVCCamera` pointer handle stored in Java `mNativePtr`. The trampoline does not allocate or free this handle.
 * \param[in] autofocus (jboolean) Value passed to the native setter.
 *
 * \return Value returned by `UVCCamera::setAutoWhiteBlance` when `id_camera` is non-null; `JNI_ERR` otherwise.
 */
static jint nativeSetAutoWhiteBlance(JNIEnv *env, jobject thiz,
	ID_TYPE id_camera, jboolean autofocus) {

	jint result = JNI_ERR;
	ENTER();
	UVCCamera *camera = reinterpret_cast<UVCCamera *>(id_camera);
	if (LIKELY(camera)) {
		result = camera->setAutoWhiteBlance(autofocus);
	}
	RETURN(result, jint);
}

/**
 * \brief Call `UVCCamera::getAutoWhiteBlance` to read the AutoWhiteBlance value.
 *
 * \pre `id_camera` is non-null for the native call; otherwise `JNI_ERR` is returned.
 *
 * \param[in] env (JNIEnv *) JNI environment.
 * \param[in] thiz (jobject) Java `UVCCamera` instance.
 * \param[in] id_camera (ID_TYPE) Native `UVCCamera` pointer handle stored in Java `mNativePtr`. The trampoline does not allocate or free this handle.
 *
 * \return Value returned by `UVCCamera::getAutoWhiteBlance` when `id_camera` is non-null; `JNI_ERR` otherwise.
 */
static jint nativeGetAutoWhiteBlance(JNIEnv *env, jobject thiz,
	ID_TYPE id_camera) {

	jint result = JNI_ERR;
	ENTER();
	UVCCamera *camera = reinterpret_cast<UVCCamera *>(id_camera);
	if (LIKELY(camera)) {
		result = camera->getAutoWhiteBlance();
	}
	RETURN(result, jint);
}
//======================================================================
// The corresponding Java method must not be static
/**
 * \brief Update the AutoWhiteBlanceCompo limit values exposed to the Java instance.
 *
 * \pre `id_camera` is non-null for a limit update.
 *
 * \post On success, writes `mAutoWhiteBlanceCompoMin`, `mAutoWhiteBlanceCompoMax`, `mAutoWhiteBlanceCompoDef` on the Java instance.
 *
 * \param[in] env (JNIEnv *) JNI environment.
 * \param[in] thiz (jobject) Java `UVCCamera` instance.
 * \param[in] id_camera (ID_TYPE) Native `UVCCamera` pointer handle stored in Java `mNativePtr`. The trampoline does not allocate or free this handle.
 *
 * \return 0 on success, the `UVCCamera::updateAutoWhiteBlanceCompoLimit` error otherwise, or `JNI_ERR` when `id_camera` is null.
 */
static jint nativeUpdateAutoWhiteBlanceCompoLimit(JNIEnv *env, jobject thiz,
	ID_TYPE id_camera) {
	jint result = JNI_ERR;
	ENTER();
	UVCCamera *camera = reinterpret_cast<UVCCamera *>(id_camera);
	if (LIKELY(camera)) {
		int min, max, def;
		result = camera->updateAutoWhiteBlanceCompoLimit(min, max, def);
		if (!result) {
			// Write the value back to Java
			setField_int(env, thiz, "mAutoWhiteBlanceCompoMin", min);
			setField_int(env, thiz, "mAutoWhiteBlanceCompoMax", max);
			setField_int(env, thiz, "mAutoWhiteBlanceCompoDef", def);
		}
	}
	RETURN(result, jint);
}

/**
 * \brief Call `UVCCamera::setAutoWhiteBlanceCompo` to set the AutoWhiteBlanceCompo value.
 *
 * \pre `id_camera` is non-null for the native call; otherwise `JNI_ERR` is returned.
 *
 * \param[in] env (JNIEnv *) JNI environment.
 * \param[in] thiz (jobject) Java `UVCCamera` instance.
 * \param[in] id_camera (ID_TYPE) Native `UVCCamera` pointer handle stored in Java `mNativePtr`. The trampoline does not allocate or free this handle.
 * \param[in] autofocus_compo (jboolean) `autofocus_compo` argument.
 *
 * \return Value returned by `UVCCamera::setAutoWhiteBlanceCompo` when `id_camera` is non-null; `JNI_ERR` otherwise.
 */
static jint nativeSetAutoWhiteBlanceCompo(JNIEnv *env, jobject thiz,
	ID_TYPE id_camera, jboolean autofocus_compo) {

	jint result = JNI_ERR;
	ENTER();
	UVCCamera *camera = reinterpret_cast<UVCCamera *>(id_camera);
	if (LIKELY(camera)) {
		result = camera->setAutoWhiteBlanceCompo(autofocus_compo);
	}
	RETURN(result, jint);
}

/**
 * \brief Call `UVCCamera::getAutoWhiteBlanceCompo` to read the AutoWhiteBlanceCompo value.
 *
 * \pre `id_camera` is non-null for the native call; otherwise `JNI_ERR` is returned.
 *
 * \param[in] env (JNIEnv *) JNI environment.
 * \param[in] thiz (jobject) Java `UVCCamera` instance.
 * \param[in] id_camera (ID_TYPE) Native `UVCCamera` pointer handle stored in Java `mNativePtr`. The trampoline does not allocate or free this handle.
 *
 * \return Value returned by `UVCCamera::getAutoWhiteBlanceCompo` when `id_camera` is non-null; `JNI_ERR` otherwise.
 */
static jint nativeGetAutoWhiteBlanceCompo(JNIEnv *env, jobject thiz,
	ID_TYPE id_camera) {

	jint result = JNI_ERR;
	ENTER();
	UVCCamera *camera = reinterpret_cast<UVCCamera *>(id_camera);
	if (LIKELY(camera)) {
		result = camera->getAutoWhiteBlanceCompo();
	}
	RETURN(result, jint);
}
//======================================================================
// The corresponding Java method must not be static
/**
 * \brief Update the Brightness limit values exposed to the Java instance.
 *
 * \pre `id_camera` is non-null for a limit update.
 *
 * \post On success, writes `mBrightnessMin`, `mBrightnessMax`, `mBrightnessDef` on the Java instance.
 *
 * \param[in] env (JNIEnv *) JNI environment.
 * \param[in] thiz (jobject) Java `UVCCamera` instance.
 * \param[in] id_camera (ID_TYPE) Native `UVCCamera` pointer handle stored in Java `mNativePtr`. The trampoline does not allocate or free this handle.
 *
 * \return 0 on success, the `UVCCamera::updateBrightnessLimit` error otherwise, or `JNI_ERR` when `id_camera` is null.
 */
static jint nativeUpdateBrightnessLimit(JNIEnv *env, jobject thiz,
	ID_TYPE id_camera) {
	jint result = JNI_ERR;
	ENTER();
	UVCCamera *camera = reinterpret_cast<UVCCamera *>(id_camera);
	if (LIKELY(camera)) {
		int min, max, def;
		result = camera->updateBrightnessLimit(min, max, def);
		if (!result) {
			// Write the value back to Java
			setField_int(env, thiz, "mBrightnessMin", min);
			setField_int(env, thiz, "mBrightnessMax", max);
			setField_int(env, thiz, "mBrightnessDef", def);
		}
	}
	RETURN(result, jint);
}

/**
 * \brief Call `UVCCamera::setBrightness` to set the Brightness value.
 *
 * \pre `id_camera` is non-null for the native call; otherwise `JNI_ERR` is returned.
 *
 * \param[in] env (JNIEnv *) JNI environment.
 * \param[in] thiz (jobject) Java `UVCCamera` instance.
 * \param[in] id_camera (ID_TYPE) Native `UVCCamera` pointer handle stored in Java `mNativePtr`. The trampoline does not allocate or free this handle.
 * \param[in] brightness (jint) Value passed to the native setter.
 *
 * \return Value returned by `UVCCamera::setBrightness` when `id_camera` is non-null; `JNI_ERR` otherwise.
 */
static jint nativeSetBrightness(JNIEnv *env, jobject thiz,
	ID_TYPE id_camera, jint brightness) {

	jint result = JNI_ERR;
	ENTER();
	UVCCamera *camera = reinterpret_cast<UVCCamera *>(id_camera);
	if (LIKELY(camera)) {
		result = camera->setBrightness(brightness);
	}
	RETURN(result, jint);
}

/**
 * \brief Call `UVCCamera::getBrightness` to read the Brightness value.
 *
 * \pre `id_camera` is non-null for the native call; otherwise `JNI_ERR` is returned.
 *
 * \param[in] env (JNIEnv *) JNI environment.
 * \param[in] thiz (jobject) Java `UVCCamera` instance.
 * \param[in] id_camera (ID_TYPE) Native `UVCCamera` pointer handle stored in Java `mNativePtr`. The trampoline does not allocate or free this handle.
 *
 * \return Value returned by `UVCCamera::getBrightness` when `id_camera` is non-null; `JNI_ERR` otherwise.
 */
static jint nativeGetBrightness(JNIEnv *env, jobject thiz,
	ID_TYPE id_camera) {

	jint result = 0;
	ENTER();
	UVCCamera *camera = reinterpret_cast<UVCCamera *>(id_camera);
	if (LIKELY(camera)) {
		result = camera->getBrightness();
	}
	RETURN(result, jint);
}
//======================================================================
// The corresponding Java method must not be static
/**
 * \brief Update the Focus limit values exposed to the Java instance.
 *
 * \pre `id_camera` is non-null for a limit update.
 *
 * \post On success, writes `mFocusMin`, `mFocusMax`, `mFocusDef` on the Java instance.
 *
 * \param[in] env (JNIEnv *) JNI environment.
 * \param[in] thiz (jobject) Java `UVCCamera` instance.
 * \param[in] id_camera (ID_TYPE) Native `UVCCamera` pointer handle stored in Java `mNativePtr`. The trampoline does not allocate or free this handle.
 *
 * \return 0 on success, the `UVCCamera::updateFocusLimit` error otherwise, or `JNI_ERR` when `id_camera` is null.
 */
static jint nativeUpdateFocusLimit(JNIEnv *env, jobject thiz,
	ID_TYPE id_camera) {
	jint result = JNI_ERR;
	ENTER();
	UVCCamera *camera = reinterpret_cast<UVCCamera *>(id_camera);
	if (LIKELY(camera)) {
		int min, max, def;
		result = camera->updateFocusLimit(min, max, def);
		if (!result) {
			// Write the value back to Java
			setField_int(env, thiz, "mFocusMin", min);
			setField_int(env, thiz, "mFocusMax", max);
			setField_int(env, thiz, "mFocusDef", def);
		}
	}
	RETURN(result, jint);
}

/**
 * \brief Call `UVCCamera::setFocus` to set the Focus value.
 *
 * \pre `id_camera` is non-null for the native call; otherwise `JNI_ERR` is returned.
 *
 * \param[in] env (JNIEnv *) JNI environment.
 * \param[in] thiz (jobject) Java `UVCCamera` instance.
 * \param[in] id_camera (ID_TYPE) Native `UVCCamera` pointer handle stored in Java `mNativePtr`. The trampoline does not allocate or free this handle.
 * \param[in] focus (jint) Value passed to the native setter.
 *
 * \return Value returned by `UVCCamera::setFocus` when `id_camera` is non-null; `JNI_ERR` otherwise.
 */
static jint nativeSetFocus(JNIEnv *env, jobject thiz,
	ID_TYPE id_camera, jint focus) {

	jint result = JNI_ERR;
	ENTER();
	UVCCamera *camera = reinterpret_cast<UVCCamera *>(id_camera);
	if (LIKELY(camera)) {
		result = camera->setFocus(focus);
	}
	RETURN(result, jint);
}

/**
 * \brief Call `UVCCamera::getFocus` to read the Focus value.
 *
 * \pre `id_camera` is non-null for the native call; otherwise `JNI_ERR` is returned.
 *
 * \param[in] env (JNIEnv *) JNI environment.
 * \param[in] thiz (jobject) Java `UVCCamera` instance.
 * \param[in] id_camera (ID_TYPE) Native `UVCCamera` pointer handle stored in Java `mNativePtr`. The trampoline does not allocate or free this handle.
 *
 * \return Value returned by `UVCCamera::getFocus` when `id_camera` is non-null; `JNI_ERR` otherwise.
 */
static jint nativeGetFocus(JNIEnv *env, jobject thiz,
	ID_TYPE id_camera) {

	jint result = 0;
	ENTER();
	UVCCamera *camera = reinterpret_cast<UVCCamera *>(id_camera);
	if (LIKELY(camera)) {
		result = camera->getFocus();
	}
	RETURN(result, jint);
}
//======================================================================
// The corresponding Java method must not be static
/**
 * \brief Update the FocusRel limit values exposed to the Java instance.
 *
 * \pre `id_camera` is non-null for a limit update.
 *
 * \post On success, writes `mFocusRelMin`, `mFocusRelMax`, `mFocusRelDef` on the Java instance.
 *
 * \param[in] env (JNIEnv *) JNI environment.
 * \param[in] thiz (jobject) Java `UVCCamera` instance.
 * \param[in] id_camera (ID_TYPE) Native `UVCCamera` pointer handle stored in Java `mNativePtr`. The trampoline does not allocate or free this handle.
 *
 * \return 0 on success, the `UVCCamera::updateFocusRelLimit` error otherwise, or `JNI_ERR` when `id_camera` is null.
 */
static jint nativeUpdateFocusRelLimit(JNIEnv *env, jobject thiz,
	ID_TYPE id_camera) {
	jint result = JNI_ERR;
	ENTER();
	UVCCamera *camera = reinterpret_cast<UVCCamera *>(id_camera);
	if (LIKELY(camera)) {
		int min, max, def;
		result = camera->updateFocusRelLimit(min, max, def);
		if (!result) {
			// Write the value back to Java
			setField_int(env, thiz, "mFocusRelMin", min);
			setField_int(env, thiz, "mFocusRelMax", max);
			setField_int(env, thiz, "mFocusRelDef", def);
		}
	}
	RETURN(result, jint);
}

/**
 * \brief Call `UVCCamera::setFocusRel` to set the FocusRel value.
 *
 * \pre `id_camera` is non-null for the native call; otherwise `JNI_ERR` is returned.
 *
 * \param[in] env (JNIEnv *) JNI environment.
 * \param[in] thiz (jobject) Java `UVCCamera` instance.
 * \param[in] id_camera (ID_TYPE) Native `UVCCamera` pointer handle stored in Java `mNativePtr`. The trampoline does not allocate or free this handle.
 * \param[in] focus_rel (jint) Value passed to the native setter.
 *
 * \return Value returned by `UVCCamera::setFocusRel` when `id_camera` is non-null; `JNI_ERR` otherwise.
 */
static jint nativeSetFocusRel(JNIEnv *env, jobject thiz,
	ID_TYPE id_camera, jint focus_rel) {

	jint result = JNI_ERR;
	ENTER();
	UVCCamera *camera = reinterpret_cast<UVCCamera *>(id_camera);
	if (LIKELY(camera)) {
		result = camera->setFocusRel(focus_rel);
	}
	RETURN(result, jint);
}

/**
 * \brief Call `UVCCamera::getFocusRel` to read the FocusRel value.
 *
 * \pre `id_camera` is non-null for the native call; otherwise `JNI_ERR` is returned.
 *
 * \param[in] env (JNIEnv *) JNI environment.
 * \param[in] thiz (jobject) Java `UVCCamera` instance.
 * \param[in] id_camera (ID_TYPE) Native `UVCCamera` pointer handle stored in Java `mNativePtr`. The trampoline does not allocate or free this handle.
 *
 * \return Value returned by `UVCCamera::getFocusRel` when `id_camera` is non-null; `JNI_ERR` otherwise.
 */
static jint nativeGetFocusRel(JNIEnv *env, jobject thiz,
	ID_TYPE id_camera) {

	jint result = 0;
	ENTER();
	UVCCamera *camera = reinterpret_cast<UVCCamera *>(id_camera);
	if (LIKELY(camera)) {
		result = camera->getFocusRel();
	}
	RETURN(result, jint);
}
//======================================================================
// The corresponding Java method must not be static
/**
 * \brief Update the Iris limit values exposed to the Java instance.
 *
 * \pre `id_camera` is non-null for a limit update.
 *
 * \post On success, writes `mIrisMin`, `mIrisMax`, `mIrisDef` on the Java instance.
 *
 * \param[in] env (JNIEnv *) JNI environment.
 * \param[in] thiz (jobject) Java `UVCCamera` instance.
 * \param[in] id_camera (ID_TYPE) Native `UVCCamera` pointer handle stored in Java `mNativePtr`. The trampoline does not allocate or free this handle.
 *
 * \return 0 on success, the `UVCCamera::updateIrisLimit` error otherwise, or `JNI_ERR` when `id_camera` is null.
 */
static jint nativeUpdateIrisLimit(JNIEnv *env, jobject thiz,
	ID_TYPE id_camera) {
	jint result = JNI_ERR;
	ENTER();
	UVCCamera *camera = reinterpret_cast<UVCCamera *>(id_camera);
	if (LIKELY(camera)) {
		int min, max, def;
		result = camera->updateIrisLimit(min, max, def);
		if (!result) {
			// Write the value back to Java
			setField_int(env, thiz, "mIrisMin", min);
			setField_int(env, thiz, "mIrisMax", max);
			setField_int(env, thiz, "mIrisDef", def);
		}
	}
	RETURN(result, jint);
}

/**
 * \brief Call `UVCCamera::setIris` to set the Iris value.
 *
 * \pre `id_camera` is non-null for the native call; otherwise `JNI_ERR` is returned.
 *
 * \param[in] env (JNIEnv *) JNI environment.
 * \param[in] thiz (jobject) Java `UVCCamera` instance.
 * \param[in] id_camera (ID_TYPE) Native `UVCCamera` pointer handle stored in Java `mNativePtr`. The trampoline does not allocate or free this handle.
 * \param[in] iris (jint) Value passed to the native setter.
 *
 * \return Value returned by `UVCCamera::setIris` when `id_camera` is non-null; `JNI_ERR` otherwise.
 */
static jint nativeSetIris(JNIEnv *env, jobject thiz,
	ID_TYPE id_camera, jint iris) {

	jint result = JNI_ERR;
	ENTER();
	UVCCamera *camera = reinterpret_cast<UVCCamera *>(id_camera);
	if (LIKELY(camera)) {
		result = camera->setIris(iris);
	}
	RETURN(result, jint);
}

/**
 * \brief Call `UVCCamera::getIris` to read the Iris value.
 *
 * \pre `id_camera` is non-null for the native call; otherwise `JNI_ERR` is returned.
 *
 * \param[in] env (JNIEnv *) JNI environment.
 * \param[in] thiz (jobject) Java `UVCCamera` instance.
 * \param[in] id_camera (ID_TYPE) Native `UVCCamera` pointer handle stored in Java `mNativePtr`. The trampoline does not allocate or free this handle.
 *
 * \return Value returned by `UVCCamera::getIris` when `id_camera` is non-null; `JNI_ERR` otherwise.
 */
static jint nativeGetIris(JNIEnv *env, jobject thiz,
	ID_TYPE id_camera) {

	jint result = 0;
	ENTER();
	UVCCamera *camera = reinterpret_cast<UVCCamera *>(id_camera);
	if (LIKELY(camera)) {
		result = camera->getIris();
	}
	RETURN(result, jint);
}
//======================================================================
// The corresponding Java method must not be static
/**
 * \brief Update the IrisRel limit values exposed to the Java instance.
 *
 * \pre `id_camera` is non-null for a limit update.
 *
 * \post On success, writes `mIrisRelMin`, `mIrisRelMax`, `mIrisRelDef` on the Java instance.
 *
 * \param[in] env (JNIEnv *) JNI environment.
 * \param[in] thiz (jobject) Java `UVCCamera` instance.
 * \param[in] id_camera (ID_TYPE) Native `UVCCamera` pointer handle stored in Java `mNativePtr`. The trampoline does not allocate or free this handle.
 *
 * \return 0 on success, the `UVCCamera::updateIrisRelLimit` error otherwise, or `JNI_ERR` when `id_camera` is null.
 */
static jint nativeUpdateIrisRelLimit(JNIEnv *env, jobject thiz,
	ID_TYPE id_camera) {
	jint result = JNI_ERR;
	ENTER();
	UVCCamera *camera = reinterpret_cast<UVCCamera *>(id_camera);
	if (LIKELY(camera)) {
		int min, max, def;
		result = camera->updateIrisRelLimit(min, max, def);
		if (!result) {
			// Write the value back to Java
			setField_int(env, thiz, "mIrisRelMin", min);
			setField_int(env, thiz, "mIrisRelMax", max);
			setField_int(env, thiz, "mIrisRelDef", def);
		}
	}
	RETURN(result, jint);
}

/**
 * \brief Call `UVCCamera::setIrisRel` to set the IrisRel value.
 *
 * \pre `id_camera` is non-null for the native call; otherwise `JNI_ERR` is returned.
 *
 * \param[in] env (JNIEnv *) JNI environment.
 * \param[in] thiz (jobject) Java `UVCCamera` instance.
 * \param[in] id_camera (ID_TYPE) Native `UVCCamera` pointer handle stored in Java `mNativePtr`. The trampoline does not allocate or free this handle.
 * \param[in] iris_rel (jint) Value passed to the native setter.
 *
 * \return Value returned by `UVCCamera::setIrisRel` when `id_camera` is non-null; `JNI_ERR` otherwise.
 */
static jint nativeSetIrisRel(JNIEnv *env, jobject thiz,
	ID_TYPE id_camera, jint iris_rel) {

	jint result = JNI_ERR;
	ENTER();
	UVCCamera *camera = reinterpret_cast<UVCCamera *>(id_camera);
	if (LIKELY(camera)) {
		result = camera->setIrisRel(iris_rel);
	}
	RETURN(result, jint);
}

/**
 * \brief Call `UVCCamera::getIrisRel` to read the IrisRel value.
 *
 * \pre `id_camera` is non-null for the native call; otherwise `JNI_ERR` is returned.
 *
 * \param[in] env (JNIEnv *) JNI environment.
 * \param[in] thiz (jobject) Java `UVCCamera` instance.
 * \param[in] id_camera (ID_TYPE) Native `UVCCamera` pointer handle stored in Java `mNativePtr`. The trampoline does not allocate or free this handle.
 *
 * \return Value returned by `UVCCamera::getIrisRel` when `id_camera` is non-null; `JNI_ERR` otherwise.
 */
static jint nativeGetIrisRel(JNIEnv *env, jobject thiz,
	ID_TYPE id_camera) {

	jint result = 0;
	ENTER();
	UVCCamera *camera = reinterpret_cast<UVCCamera *>(id_camera);
	if (LIKELY(camera)) {
		result = camera->getIrisRel();
	}
	RETURN(result, jint);
}
//======================================================================
// The corresponding Java method must not be static
/**
 * \brief Update the Pan limit values exposed to the Java instance.
 *
 * \pre `id_camera` is non-null for a limit update.
 *
 * \post On success, writes `mPanMin`, `mPanMax`, `mPanDef` on the Java instance.
 *
 * \param[in] env (JNIEnv *) JNI environment.
 * \param[in] thiz (jobject) Java `UVCCamera` instance.
 * \param[in] id_camera (ID_TYPE) Native `UVCCamera` pointer handle stored in Java `mNativePtr`. The trampoline does not allocate or free this handle.
 *
 * \return 0 on success, the `UVCCamera::updatePanLimit` error otherwise, or `JNI_ERR` when `id_camera` is null.
 */
static jint nativeUpdatePanLimit(JNIEnv *env, jobject thiz,
	ID_TYPE id_camera) {
	jint result = JNI_ERR;
	ENTER();
	UVCCamera *camera = reinterpret_cast<UVCCamera *>(id_camera);
	if (LIKELY(camera)) {
		int min, max, def;
		result = camera->updatePanLimit(min, max, def);
		if (!result) {
			// Write the value back to Java
			setField_int(env, thiz, "mPanMin", min);
			setField_int(env, thiz, "mPanMax", max);
			setField_int(env, thiz, "mPanDef", def);
		}
	}
	RETURN(result, jint);
}

/**
 * \brief Call `UVCCamera::setPan` to set the Pan value.
 *
 * \pre `id_camera` is non-null for the native call; otherwise `JNI_ERR` is returned.
 *
 * \param[in] env (JNIEnv *) JNI environment.
 * \param[in] thiz (jobject) Java `UVCCamera` instance.
 * \param[in] id_camera (ID_TYPE) Native `UVCCamera` pointer handle stored in Java `mNativePtr`. The trampoline does not allocate or free this handle.
 * \param[in] pan (jint) Value passed to the native setter.
 *
 * \return Value returned by `UVCCamera::setPan` when `id_camera` is non-null; `JNI_ERR` otherwise.
 */
static jint nativeSetPan(JNIEnv *env, jobject thiz,
	ID_TYPE id_camera, jint pan) {

	jint result = JNI_ERR;
	ENTER();
	UVCCamera *camera = reinterpret_cast<UVCCamera *>(id_camera);
	if (LIKELY(camera)) {
		result = camera->setPan(pan);
	}
	RETURN(result, jint);
}

/**
 * \brief Call `UVCCamera::getPan` to read the Pan value.
 *
 * \pre `id_camera` is non-null for the native call; otherwise `JNI_ERR` is returned.
 *
 * \param[in] env (JNIEnv *) JNI environment.
 * \param[in] thiz (jobject) Java `UVCCamera` instance.
 * \param[in] id_camera (ID_TYPE) Native `UVCCamera` pointer handle stored in Java `mNativePtr`. The trampoline does not allocate or free this handle.
 *
 * \return Value returned by `UVCCamera::getPan` when `id_camera` is non-null; `JNI_ERR` otherwise.
 */
static jint nativeGetPan(JNIEnv *env, jobject thiz,
	ID_TYPE id_camera) {

	jint result = 0;
	ENTER();
	UVCCamera *camera = reinterpret_cast<UVCCamera *>(id_camera);
	if (LIKELY(camera)) {
		result = camera->getPan();
	}
	RETURN(result, jint);
}
//======================================================================
// The corresponding Java method must not be static
/**
 * \brief Update the Tilt limit values exposed to the Java instance.
 *
 * \pre `id_camera` is non-null for a limit update.
 *
 * \post On success, writes `mTiltMin`, `mTiltMax`, `mTiltDef` on the Java instance.
 *
 * \param[in] env (JNIEnv *) JNI environment.
 * \param[in] thiz (jobject) Java `UVCCamera` instance.
 * \param[in] id_camera (ID_TYPE) Native `UVCCamera` pointer handle stored in Java `mNativePtr`. The trampoline does not allocate or free this handle.
 *
 * \return 0 on success, the `UVCCamera::updateTiltLimit` error otherwise, or `JNI_ERR` when `id_camera` is null.
 */
static jint nativeUpdateTiltLimit(JNIEnv *env, jobject thiz,
	ID_TYPE id_camera) {
	jint result = JNI_ERR;
	ENTER();
	UVCCamera *camera = reinterpret_cast<UVCCamera *>(id_camera);
	if (LIKELY(camera)) {
		int min, max, def;
		result = camera->updateTiltLimit(min, max, def);
		if (!result) {
			// Write the value back to Java
			setField_int(env, thiz, "mTiltMin", min);
			setField_int(env, thiz, "mTiltMax", max);
			setField_int(env, thiz, "mTiltDef", def);
		}
	}
	RETURN(result, jint);
}

/**
 * \brief Call `UVCCamera::setTilt` to set the Tilt value.
 *
 * \pre `id_camera` is non-null for the native call; otherwise `JNI_ERR` is returned.
 *
 * \param[in] env (JNIEnv *) JNI environment.
 * \param[in] thiz (jobject) Java `UVCCamera` instance.
 * \param[in] id_camera (ID_TYPE) Native `UVCCamera` pointer handle stored in Java `mNativePtr`. The trampoline does not allocate or free this handle.
 * \param[in] tilt (jint) Value passed to the native setter.
 *
 * \return Value returned by `UVCCamera::setTilt` when `id_camera` is non-null; `JNI_ERR` otherwise.
 */
static jint nativeSetTilt(JNIEnv *env, jobject thiz,
	ID_TYPE id_camera, jint tilt) {

	jint result = JNI_ERR;
	ENTER();
	UVCCamera *camera = reinterpret_cast<UVCCamera *>(id_camera);
	if (LIKELY(camera)) {
		result = camera->setTilt(tilt);
	}
	RETURN(result, jint);
}

/**
 * \brief Call `UVCCamera::getTilt` to read the Tilt value.
 *
 * \pre `id_camera` is non-null for the native call; otherwise `JNI_ERR` is returned.
 *
 * \param[in] env (JNIEnv *) JNI environment.
 * \param[in] thiz (jobject) Java `UVCCamera` instance.
 * \param[in] id_camera (ID_TYPE) Native `UVCCamera` pointer handle stored in Java `mNativePtr`. The trampoline does not allocate or free this handle.
 *
 * \return Value returned by `UVCCamera::getTilt` when `id_camera` is non-null; `JNI_ERR` otherwise.
 */
static jint nativeGetTilt(JNIEnv *env, jobject thiz,
	ID_TYPE id_camera) {

	jint result = 0;
	ENTER();
	UVCCamera *camera = reinterpret_cast<UVCCamera *>(id_camera);
	if (LIKELY(camera)) {
		result = camera->getTilt();
	}
	RETURN(result, jint);
}
//======================================================================
// The corresponding Java method must not be static
/**
 * \brief Update the Roll limit values exposed to the Java instance.
 *
 * \pre `id_camera` is non-null for a limit update.
 *
 * \post On success, writes `mRollMin`, `mRollMax`, `mRollDef` on the Java instance.
 *
 * \param[in] env (JNIEnv *) JNI environment.
 * \param[in] thiz (jobject) Java `UVCCamera` instance.
 * \param[in] id_camera (ID_TYPE) Native `UVCCamera` pointer handle stored in Java `mNativePtr`. The trampoline does not allocate or free this handle.
 *
 * \return 0 on success, the `UVCCamera::updateRollLimit` error otherwise, or `JNI_ERR` when `id_camera` is null.
 */
static jint nativeUpdateRollLimit(JNIEnv *env, jobject thiz,
	ID_TYPE id_camera) {
	jint result = JNI_ERR;
	ENTER();
	UVCCamera *camera = reinterpret_cast<UVCCamera *>(id_camera);
	if (LIKELY(camera)) {
		int min, max, def;
		result = camera->updateRollLimit(min, max, def);
		if (!result) {
			// Write the value back to Java
			setField_int(env, thiz, "mRollMin", min);
			setField_int(env, thiz, "mRollMax", max);
			setField_int(env, thiz, "mRollDef", def);
		}
	}
	RETURN(result, jint);
}

/**
 * \brief Call `UVCCamera::setRoll` to set the Roll value.
 *
 * \pre `id_camera` is non-null for the native call; otherwise `JNI_ERR` is returned.
 *
 * \param[in] env (JNIEnv *) JNI environment.
 * \param[in] thiz (jobject) Java `UVCCamera` instance.
 * \param[in] id_camera (ID_TYPE) Native `UVCCamera` pointer handle stored in Java `mNativePtr`. The trampoline does not allocate or free this handle.
 * \param[in] roll (jint) Value passed to the native setter.
 *
 * \return Value returned by `UVCCamera::setRoll` when `id_camera` is non-null; `JNI_ERR` otherwise.
 */
static jint nativeSetRoll(JNIEnv *env, jobject thiz,
	ID_TYPE id_camera, jint roll) {

	jint result = JNI_ERR;
	ENTER();
	UVCCamera *camera = reinterpret_cast<UVCCamera *>(id_camera);
	if (LIKELY(camera)) {
		result = camera->setRoll(roll);
	}
	RETURN(result, jint);
}

/**
 * \brief Call `UVCCamera::getRoll` to read the Roll value.
 *
 * \pre `id_camera` is non-null for the native call; otherwise `JNI_ERR` is returned.
 *
 * \param[in] env (JNIEnv *) JNI environment.
 * \param[in] thiz (jobject) Java `UVCCamera` instance.
 * \param[in] id_camera (ID_TYPE) Native `UVCCamera` pointer handle stored in Java `mNativePtr`. The trampoline does not allocate or free this handle.
 *
 * \return Value returned by `UVCCamera::getRoll` when `id_camera` is non-null; `JNI_ERR` otherwise.
 */
static jint nativeGetRoll(JNIEnv *env, jobject thiz,
	ID_TYPE id_camera) {

	jint result = 0;
	ENTER();
	UVCCamera *camera = reinterpret_cast<UVCCamera *>(id_camera);
	if (LIKELY(camera)) {
		result = camera->getRoll();
	}
	RETURN(result, jint);
}
//======================================================================
// The corresponding Java method must not be static
/**
 * \brief Update the PanRel limit values exposed to the Java instance.
 *
 * \pre `id_camera` is non-null for a limit update.
 *
 * \post On success, writes `mPanRelMin`, `mPanRelMax`, `mPanRelDef` on the Java instance.
 *
 * \param[in] env (JNIEnv *) JNI environment.
 * \param[in] thiz (jobject) Java `UVCCamera` instance.
 * \param[in] id_camera (ID_TYPE) Native `UVCCamera` pointer handle stored in Java `mNativePtr`. The trampoline does not allocate or free this handle.
 *
 * \return 0 on success, the `UVCCamera::updatePanRelLimit` error otherwise, or `JNI_ERR` when `id_camera` is null.
 */
static jint nativeUpdatePanRelLimit(JNIEnv *env, jobject thiz,
	ID_TYPE id_camera) {
	jint result = JNI_ERR;
	ENTER();
	UVCCamera *camera = reinterpret_cast<UVCCamera *>(id_camera);
	if (LIKELY(camera)) {
		int min, max, def;
		result = camera->updatePanRelLimit(min, max, def);
		if (!result) {
			// Write the value back to Java
			setField_int(env, thiz, "mPanRelMin", min);
			setField_int(env, thiz, "mPanRelMax", max);
			setField_int(env, thiz, "mPanRelDef", def);
		}
	}
	RETURN(result, jint);
}

/**
 * \brief Call `UVCCamera::setPanRel` to set the PanRel value.
 *
 * \pre `id_camera` is non-null for the native call; otherwise `JNI_ERR` is returned.
 *
 * \param[in] env (JNIEnv *) JNI environment.
 * \param[in] thiz (jobject) Java `UVCCamera` instance.
 * \param[in] id_camera (ID_TYPE) Native `UVCCamera` pointer handle stored in Java `mNativePtr`. The trampoline does not allocate or free this handle.
 * \param[in] pan_rel (jint) Value passed to the native setter.
 *
 * \return Value returned by `UVCCamera::setPanRel` when `id_camera` is non-null; `JNI_ERR` otherwise.
 */
static jint nativeSetPanRel(JNIEnv *env, jobject thiz,
	ID_TYPE id_camera, jint pan_rel) {

	jint result = JNI_ERR;
	ENTER();
	UVCCamera *camera = reinterpret_cast<UVCCamera *>(id_camera);
	if (LIKELY(camera)) {
		result = camera->setPanRel(pan_rel);
	}
	RETURN(result, jint);
}

/**
 * \brief Call `UVCCamera::getPanRel` to read the PanRel value.
 *
 * \pre `id_camera` is non-null for the native call; otherwise `JNI_ERR` is returned.
 *
 * \param[in] env (JNIEnv *) JNI environment.
 * \param[in] thiz (jobject) Java `UVCCamera` instance.
 * \param[in] id_camera (ID_TYPE) Native `UVCCamera` pointer handle stored in Java `mNativePtr`. The trampoline does not allocate or free this handle.
 *
 * \return Value returned by `UVCCamera::getPanRel` when `id_camera` is non-null; `JNI_ERR` otherwise.
 */
static jint nativeGetPanRel(JNIEnv *env, jobject thiz,
	ID_TYPE id_camera) {

	jint result = 0;
	ENTER();
	UVCCamera *camera = reinterpret_cast<UVCCamera *>(id_camera);
	if (LIKELY(camera)) {
		result = camera->getPanRel();
	}
	RETURN(result, jint);
}
//======================================================================
// The corresponding Java method must not be static
/**
 * \brief Update the TiltRel limit values exposed to the Java instance.
 *
 * \pre `id_camera` is non-null for a limit update.
 *
 * \post On success, writes `mTiltRelMin`, `mTiltRelMax`, `mTiltRelDef` on the Java instance.
 *
 * \param[in] env (JNIEnv *) JNI environment.
 * \param[in] thiz (jobject) Java `UVCCamera` instance.
 * \param[in] id_camera (ID_TYPE) Native `UVCCamera` pointer handle stored in Java `mNativePtr`. The trampoline does not allocate or free this handle.
 *
 * \return 0 on success, the `UVCCamera::updateTiltRelLimit` error otherwise, or `JNI_ERR` when `id_camera` is null.
 */
static jint nativeUpdateTiltRelLimit(JNIEnv *env, jobject thiz,
	ID_TYPE id_camera) {
	jint result = JNI_ERR;
	ENTER();
	UVCCamera *camera = reinterpret_cast<UVCCamera *>(id_camera);
	if (LIKELY(camera)) {
		int min, max, def;
		result = camera->updateTiltRelLimit(min, max, def);
		if (!result) {
			// Write the value back to Java
			setField_int(env, thiz, "mTiltRelMin", min);
			setField_int(env, thiz, "mTiltRelMax", max);
			setField_int(env, thiz, "mTiltRelDef", def);
		}
	}
	RETURN(result, jint);
}

/**
 * \brief Call `UVCCamera::setTiltRel` to set the TiltRel value.
 *
 * \pre `id_camera` is non-null for the native call; otherwise `JNI_ERR` is returned.
 *
 * \param[in] env (JNIEnv *) JNI environment.
 * \param[in] thiz (jobject) Java `UVCCamera` instance.
 * \param[in] id_camera (ID_TYPE) Native `UVCCamera` pointer handle stored in Java `mNativePtr`. The trampoline does not allocate or free this handle.
 * \param[in] tilt_rel (jint) Value passed to the native setter.
 *
 * \return Value returned by `UVCCamera::setTiltRel` when `id_camera` is non-null; `JNI_ERR` otherwise.
 */
static jint nativeSetTiltRel(JNIEnv *env, jobject thiz,
	ID_TYPE id_camera, jint tilt_rel) {

	jint result = JNI_ERR;
	ENTER();
	UVCCamera *camera = reinterpret_cast<UVCCamera *>(id_camera);
	if (LIKELY(camera)) {
		result = camera->setTiltRel(tilt_rel);
	}
	RETURN(result, jint);
}

/**
 * \brief Call `UVCCamera::getTiltRel` to read the TiltRel value.
 *
 * \pre `id_camera` is non-null for the native call; otherwise `JNI_ERR` is returned.
 *
 * \param[in] env (JNIEnv *) JNI environment.
 * \param[in] thiz (jobject) Java `UVCCamera` instance.
 * \param[in] id_camera (ID_TYPE) Native `UVCCamera` pointer handle stored in Java `mNativePtr`. The trampoline does not allocate or free this handle.
 *
 * \return Value returned by `UVCCamera::getTiltRel` when `id_camera` is non-null; `JNI_ERR` otherwise.
 */
static jint nativeGetTiltRel(JNIEnv *env, jobject thiz,
	ID_TYPE id_camera) {

	jint result = 0;
	ENTER();
	UVCCamera *camera = reinterpret_cast<UVCCamera *>(id_camera);
	if (LIKELY(camera)) {
		result = camera->getTiltRel();
	}
	RETURN(result, jint);
}
//======================================================================
// The corresponding Java method must not be static
/**
 * \brief Update the RollRel limit values exposed to the Java instance.
 *
 * \pre `id_camera` is non-null for a limit update.
 *
 * \post On success, writes `mRollRelMin`, `mRollRelMax`, `mRollRelDef` on the Java instance.
 *
 * \param[in] env (JNIEnv *) JNI environment.
 * \param[in] thiz (jobject) Java `UVCCamera` instance.
 * \param[in] id_camera (ID_TYPE) Native `UVCCamera` pointer handle stored in Java `mNativePtr`. The trampoline does not allocate or free this handle.
 *
 * \return 0 on success, the `UVCCamera::updateRollRelLimit` error otherwise, or `JNI_ERR` when `id_camera` is null.
 */
static jint nativeUpdateRollRelLimit(JNIEnv *env, jobject thiz,
	ID_TYPE id_camera) {
	jint result = JNI_ERR;
	ENTER();
	UVCCamera *camera = reinterpret_cast<UVCCamera *>(id_camera);
	if (LIKELY(camera)) {
		int min, max, def;
		result = camera->updateRollRelLimit(min, max, def);
		if (!result) {
			// Write the value back to Java
			setField_int(env, thiz, "mRollRelMin", min);
			setField_int(env, thiz, "mRollRelMax", max);
			setField_int(env, thiz, "mRollRelDef", def);
		}
	}
	RETURN(result, jint);
}

/**
 * \brief Call `UVCCamera::setRollRel` to set the RollRel value.
 *
 * \pre `id_camera` is non-null for the native call; otherwise `JNI_ERR` is returned.
 *
 * \param[in] env (JNIEnv *) JNI environment.
 * \param[in] thiz (jobject) Java `UVCCamera` instance.
 * \param[in] id_camera (ID_TYPE) Native `UVCCamera` pointer handle stored in Java `mNativePtr`. The trampoline does not allocate or free this handle.
 * \param[in] roll_rel (jint) Value passed to the native setter.
 *
 * \return Value returned by `UVCCamera::setRollRel` when `id_camera` is non-null; `JNI_ERR` otherwise.
 */
static jint nativeSetRollRel(JNIEnv *env, jobject thiz,
	ID_TYPE id_camera, jint roll_rel) {

	jint result = JNI_ERR;
	ENTER();
	UVCCamera *camera = reinterpret_cast<UVCCamera *>(id_camera);
	if (LIKELY(camera)) {
		result = camera->setRollRel(roll_rel);
	}
	RETURN(result, jint);
}

/**
 * \brief Call `UVCCamera::getRollRel` to read the RollRel value.
 *
 * \pre `id_camera` is non-null for the native call; otherwise `JNI_ERR` is returned.
 *
 * \param[in] env (JNIEnv *) JNI environment.
 * \param[in] thiz (jobject) Java `UVCCamera` instance.
 * \param[in] id_camera (ID_TYPE) Native `UVCCamera` pointer handle stored in Java `mNativePtr`. The trampoline does not allocate or free this handle.
 *
 * \return Value returned by `UVCCamera::getRollRel` when `id_camera` is non-null; `JNI_ERR` otherwise.
 */
static jint nativeGetRollRel(JNIEnv *env, jobject thiz,
	ID_TYPE id_camera) {

	jint result = 0;
	ENTER();
	UVCCamera *camera = reinterpret_cast<UVCCamera *>(id_camera);
	if (LIKELY(camera)) {
		result = camera->getRollRel();
	}
	RETURN(result, jint);
}
//======================================================================
// The corresponding Java method must not be static
/**
 * \brief Update the Contrast limit values exposed to the Java instance.
 *
 * \pre `id_camera` is non-null for a limit update.
 *
 * \post On success, writes `mContrastMin`, `mContrastMax`, `mContrastDef` on the Java instance.
 *
 * \param[in] env (JNIEnv *) JNI environment.
 * \param[in] thiz (jobject) Java `UVCCamera` instance.
 * \param[in] id_camera (ID_TYPE) Native `UVCCamera` pointer handle stored in Java `mNativePtr`. The trampoline does not allocate or free this handle.
 *
 * \return 0 on success, the `UVCCamera::updateContrastLimit` error otherwise, or `JNI_ERR` when `id_camera` is null.
 */
static jint nativeUpdateContrastLimit(JNIEnv *env, jobject thiz,
	ID_TYPE id_camera) {
	jint result = JNI_ERR;
	ENTER();
	UVCCamera *camera = reinterpret_cast<UVCCamera *>(id_camera);
	if (LIKELY(camera)) {
		int min, max, def;
		result = camera->updateContrastLimit(min, max, def);
		if (!result) {
			// Write the value back to Java
			setField_int(env, thiz, "mContrastMin", min);
			setField_int(env, thiz, "mContrastMax", max);
			setField_int(env, thiz, "mContrastDef", def);
		}
	}
	RETURN(result, jint);
}

/**
 * \brief Call `UVCCamera::setContrast` to set the Contrast value.
 *
 * \pre `id_camera` is non-null for the native call; otherwise `JNI_ERR` is returned.
 *
 * \param[in] env (JNIEnv *) JNI environment.
 * \param[in] thiz (jobject) Java `UVCCamera` instance.
 * \param[in] id_camera (ID_TYPE) Native `UVCCamera` pointer handle stored in Java `mNativePtr`. The trampoline does not allocate or free this handle.
 * \param[in] contrast (jint) Value passed to the native setter.
 *
 * \return Value returned by `UVCCamera::setContrast` when `id_camera` is non-null; `JNI_ERR` otherwise.
 */
static jint nativeSetContrast(JNIEnv *env, jobject thiz,
	ID_TYPE id_camera, jint contrast) {

	jint result = JNI_ERR;
	ENTER();
	UVCCamera *camera = reinterpret_cast<UVCCamera *>(id_camera);
	if (LIKELY(camera)) {
		result = camera->setContrast(contrast);
	}
	RETURN(result, jint);
}

/**
 * \brief Call `UVCCamera::getContrast` to read the Contrast value.
 *
 * \pre `id_camera` is non-null for the native call; otherwise `JNI_ERR` is returned.
 *
 * \param[in] env (JNIEnv *) JNI environment.
 * \param[in] thiz (jobject) Java `UVCCamera` instance.
 * \param[in] id_camera (ID_TYPE) Native `UVCCamera` pointer handle stored in Java `mNativePtr`. The trampoline does not allocate or free this handle.
 *
 * \return Value returned by `UVCCamera::getContrast` when `id_camera` is non-null; `JNI_ERR` otherwise.
 */
static jint nativeGetContrast(JNIEnv *env, jobject thiz,
	ID_TYPE id_camera) {

	jint result = 0;
	ENTER();
	UVCCamera *camera = reinterpret_cast<UVCCamera *>(id_camera);
	if (LIKELY(camera)) {
		result = camera->getContrast();
	}
	RETURN(result, jint);
}
//======================================================================
// The corresponding Java method must not be static
/**
 * \brief Update the AutoContrast limit values exposed to the Java instance.
 *
 * \pre `id_camera` is non-null for a limit update.
 *
 * \post On success, writes `mAutoContrastMin`, `mAutoContrastMax`, `mAutoContrastDef` on the Java instance.
 *
 * \param[in] env (JNIEnv *) JNI environment.
 * \param[in] thiz (jobject) Java `UVCCamera` instance.
 * \param[in] id_camera (ID_TYPE) Native `UVCCamera` pointer handle stored in Java `mNativePtr`. The trampoline does not allocate or free this handle.
 *
 * \return 0 on success, the `UVCCamera::updateAutoContrastLimit` error otherwise, or `JNI_ERR` when `id_camera` is null.
 */
static jint nativeUpdateAutoContrastLimit(JNIEnv *env, jobject thiz,
	ID_TYPE id_camera) {
	jint result = JNI_ERR;
	ENTER();
	UVCCamera *camera = reinterpret_cast<UVCCamera *>(id_camera);
	if (LIKELY(camera)) {
		int min, max, def;
		result = camera->updateAutoContrastLimit(min, max, def);
		if (!result) {
			// Write the value back to Java
			setField_int(env, thiz, "mAutoContrastMin", min);
			setField_int(env, thiz, "mAutoContrastMax", max);
			setField_int(env, thiz, "mAutoContrastDef", def);
		}
	}
	RETURN(result, jint);
}

/**
 * \brief Call `UVCCamera::setAutoContrast` to set the AutoContrast value.
 *
 * \pre `id_camera` is non-null for the native call; otherwise `JNI_ERR` is returned.
 *
 * \param[in] env (JNIEnv *) JNI environment.
 * \param[in] thiz (jobject) Java `UVCCamera` instance.
 * \param[in] id_camera (ID_TYPE) Native `UVCCamera` pointer handle stored in Java `mNativePtr`. The trampoline does not allocate or free this handle.
 * \param[in] autocontrast (jboolean) `autocontrast` argument.
 *
 * \return Value returned by `UVCCamera::setAutoContrast` when `id_camera` is non-null; `JNI_ERR` otherwise.
 */
static jint nativeSetAutoContrast(JNIEnv *env, jobject thiz,
	ID_TYPE id_camera, jboolean autocontrast) {

	jint result = JNI_ERR;
	ENTER();
	UVCCamera *camera = reinterpret_cast<UVCCamera *>(id_camera);
	if (LIKELY(camera)) {
		result = camera->setAutoContrast(autocontrast);
	}
	RETURN(result, jint);
}

/**
 * \brief Call `UVCCamera::getAutoContrast` to read the AutoContrast value.
 *
 * \pre `id_camera` is non-null for the native call; otherwise `JNI_ERR` is returned.
 *
 * \param[in] env (JNIEnv *) JNI environment.
 * \param[in] thiz (jobject) Java `UVCCamera` instance.
 * \param[in] id_camera (ID_TYPE) Native `UVCCamera` pointer handle stored in Java `mNativePtr`. The trampoline does not allocate or free this handle.
 *
 * \return Value returned by `UVCCamera::getAutoContrast` when `id_camera` is non-null; `JNI_ERR` otherwise.
 */
static jint nativeGetAutoContrast(JNIEnv *env, jobject thiz,
	ID_TYPE id_camera) {

	jint result = JNI_ERR;
	ENTER();
	UVCCamera *camera = reinterpret_cast<UVCCamera *>(id_camera);
	if (LIKELY(camera)) {
		result = camera->getAutoContrast();
	}
	RETURN(result, jint);
}
//======================================================================
// The corresponding Java method must not be static
/**
 * \brief Update the Sharpness limit values exposed to the Java instance.
 *
 * \pre `id_camera` is non-null for a limit update.
 *
 * \post On success, writes `mSharpnessMin`, `mSharpnessMax`, `mSharpnessDef` on the Java instance.
 *
 * \param[in] env (JNIEnv *) JNI environment.
 * \param[in] thiz (jobject) Java `UVCCamera` instance.
 * \param[in] id_camera (ID_TYPE) Native `UVCCamera` pointer handle stored in Java `mNativePtr`. The trampoline does not allocate or free this handle.
 *
 * \return 0 on success, the `UVCCamera::updateSharpnessLimit` error otherwise, or `JNI_ERR` when `id_camera` is null.
 */
static jint nativeUpdateSharpnessLimit(JNIEnv *env, jobject thiz,
	ID_TYPE id_camera) {
	jint result = JNI_ERR;
	ENTER();
	UVCCamera *camera = reinterpret_cast<UVCCamera *>(id_camera);
	if (LIKELY(camera)) {
		int min, max, def;
		result = camera->updateSharpnessLimit(min, max, def);
		if (!result) {
			// Write the value back to Java
			setField_int(env, thiz, "mSharpnessMin", min);
			setField_int(env, thiz, "mSharpnessMax", max);
			setField_int(env, thiz, "mSharpnessDef", def);
		}
	}
	RETURN(result, jint);
}

/**
 * \brief Call `UVCCamera::setSharpness` to set the Sharpness value.
 *
 * \pre `id_camera` is non-null for the native call; otherwise `JNI_ERR` is returned.
 *
 * \param[in] env (JNIEnv *) JNI environment.
 * \param[in] thiz (jobject) Java `UVCCamera` instance.
 * \param[in] id_camera (ID_TYPE) Native `UVCCamera` pointer handle stored in Java `mNativePtr`. The trampoline does not allocate or free this handle.
 * \param[in] sharpness (jint) Value passed to the native setter.
 *
 * \return Value returned by `UVCCamera::setSharpness` when `id_camera` is non-null; `JNI_ERR` otherwise.
 */
static jint nativeSetSharpness(JNIEnv *env, jobject thiz,
	ID_TYPE id_camera, jint sharpness) {

	jint result = JNI_ERR;
	ENTER();
	UVCCamera *camera = reinterpret_cast<UVCCamera *>(id_camera);
	if (LIKELY(camera)) {
		result = camera->setSharpness(sharpness);
	}
	RETURN(result, jint);
}

/**
 * \brief Call `UVCCamera::getSharpness` to read the Sharpness value.
 *
 * \pre `id_camera` is non-null for the native call; otherwise `JNI_ERR` is returned.
 *
 * \param[in] env (JNIEnv *) JNI environment.
 * \param[in] thiz (jobject) Java `UVCCamera` instance.
 * \param[in] id_camera (ID_TYPE) Native `UVCCamera` pointer handle stored in Java `mNativePtr`. The trampoline does not allocate or free this handle.
 *
 * \return Value returned by `UVCCamera::getSharpness` when `id_camera` is non-null; `JNI_ERR` otherwise.
 */
static jint nativeGetSharpness(JNIEnv *env, jobject thiz,
	ID_TYPE id_camera) {

	jint result = 0;
	ENTER();
	UVCCamera *camera = reinterpret_cast<UVCCamera *>(id_camera);
	if (LIKELY(camera)) {
		result = camera->getSharpness();
	}
	RETURN(result, jint);
}
//======================================================================
// The corresponding Java method must not be static
/**
 * \brief Update the Gain limit values exposed to the Java instance.
 *
 * \pre `id_camera` is non-null for a limit update.
 *
 * \post On success, writes `mGainMin`, `mGainMax`, `mGainDef` on the Java instance.
 *
 * \param[in] env (JNIEnv *) JNI environment.
 * \param[in] thiz (jobject) Java `UVCCamera` instance.
 * \param[in] id_camera (ID_TYPE) Native `UVCCamera` pointer handle stored in Java `mNativePtr`. The trampoline does not allocate or free this handle.
 *
 * \return 0 on success, the `UVCCamera::updateGainLimit` error otherwise, or `JNI_ERR` when `id_camera` is null.
 */
static jint nativeUpdateGainLimit(JNIEnv *env, jobject thiz,
	ID_TYPE id_camera) {
	jint result = JNI_ERR;
	ENTER();
	UVCCamera *camera = reinterpret_cast<UVCCamera *>(id_camera);
	if (LIKELY(camera)) {
		int min, max, def;
		result = camera->updateGainLimit(min, max, def);
		if (!result) {
			// Write the value back to Java
			setField_int(env, thiz, "mGainMin", min);
			setField_int(env, thiz, "mGainMax", max);
			setField_int(env, thiz, "mGainDef", def);
		}
	}
	RETURN(result, jint);
}

/**
 * \brief Call `UVCCamera::setGain` to set the Gain value.
 *
 * \pre `id_camera` is non-null for the native call; otherwise `JNI_ERR` is returned.
 *
 * \param[in] env (JNIEnv *) JNI environment.
 * \param[in] thiz (jobject) Java `UVCCamera` instance.
 * \param[in] id_camera (ID_TYPE) Native `UVCCamera` pointer handle stored in Java `mNativePtr`. The trampoline does not allocate or free this handle.
 * \param[in] gain (jint) Value passed to the native setter.
 *
 * \return Value returned by `UVCCamera::setGain` when `id_camera` is non-null; `JNI_ERR` otherwise.
 */
static jint nativeSetGain(JNIEnv *env, jobject thiz,
	ID_TYPE id_camera, jint gain) {

	jint result = JNI_ERR;
	ENTER();
	UVCCamera *camera = reinterpret_cast<UVCCamera *>(id_camera);
	if (LIKELY(camera)) {
		result = camera->setGain(gain);
	}
	RETURN(result, jint);
}

/**
 * \brief Call `UVCCamera::getGain` to read the Gain value.
 *
 * \pre `id_camera` is non-null for the native call; otherwise `JNI_ERR` is returned.
 *
 * \param[in] env (JNIEnv *) JNI environment.
 * \param[in] thiz (jobject) Java `UVCCamera` instance.
 * \param[in] id_camera (ID_TYPE) Native `UVCCamera` pointer handle stored in Java `mNativePtr`. The trampoline does not allocate or free this handle.
 *
 * \return Value returned by `UVCCamera::getGain` when `id_camera` is non-null; `JNI_ERR` otherwise.
 */
static jint nativeGetGain(JNIEnv *env, jobject thiz,
	ID_TYPE id_camera) {

	jint result = 0;
	ENTER();
	UVCCamera *camera = reinterpret_cast<UVCCamera *>(id_camera);
	if (LIKELY(camera)) {
		result = camera->getGain();
	}
	RETURN(result, jint);
}
//======================================================================
// The corresponding Java method must not be static
/**
 * \brief Update the Gamma limit values exposed to the Java instance.
 *
 * \pre `id_camera` is non-null for a limit update.
 *
 * \post On success, writes `mGammaMin`, `mGammaMax`, `mGammaDef` on the Java instance.
 *
 * \param[in] env (JNIEnv *) JNI environment.
 * \param[in] thiz (jobject) Java `UVCCamera` instance.
 * \param[in] id_camera (ID_TYPE) Native `UVCCamera` pointer handle stored in Java `mNativePtr`. The trampoline does not allocate or free this handle.
 *
 * \return 0 on success, the `UVCCamera::updateGammaLimit` error otherwise, or `JNI_ERR` when `id_camera` is null.
 */
static jint nativeUpdateGammaLimit(JNIEnv *env, jobject thiz,
	ID_TYPE id_camera) {
	jint result = JNI_ERR;
	ENTER();
	UVCCamera *camera = reinterpret_cast<UVCCamera *>(id_camera);
	if (LIKELY(camera)) {
		int min, max, def;
		result = camera->updateGammaLimit(min, max, def);
		if (!result) {
			// Write the value back to Java
			setField_int(env, thiz, "mGammaMin", min);
			setField_int(env, thiz, "mGammaMax", max);
			setField_int(env, thiz, "mGammaDef", def);
		}
	}
	RETURN(result, jint);
}

/**
 * \brief Call `UVCCamera::setGamma` to set the Gamma value.
 *
 * \pre `id_camera` is non-null for the native call; otherwise `JNI_ERR` is returned.
 *
 * \param[in] env (JNIEnv *) JNI environment.
 * \param[in] thiz (jobject) Java `UVCCamera` instance.
 * \param[in] id_camera (ID_TYPE) Native `UVCCamera` pointer handle stored in Java `mNativePtr`. The trampoline does not allocate or free this handle.
 * \param[in] gamma (jint) Value passed to the native setter.
 *
 * \return Value returned by `UVCCamera::setGamma` when `id_camera` is non-null; `JNI_ERR` otherwise.
 */
static jint nativeSetGamma(JNIEnv *env, jobject thiz,
	ID_TYPE id_camera, jint gamma) {

	jint result = JNI_ERR;
	ENTER();
	UVCCamera *camera = reinterpret_cast<UVCCamera *>(id_camera);
	if (LIKELY(camera)) {
		result = camera->setGamma(gamma);
	}
	RETURN(result, jint);
}

/**
 * \brief Call `UVCCamera::getGamma` to read the Gamma value.
 *
 * \pre `id_camera` is non-null for the native call; otherwise `JNI_ERR` is returned.
 *
 * \param[in] env (JNIEnv *) JNI environment.
 * \param[in] thiz (jobject) Java `UVCCamera` instance.
 * \param[in] id_camera (ID_TYPE) Native `UVCCamera` pointer handle stored in Java `mNativePtr`. The trampoline does not allocate or free this handle.
 *
 * \return Value returned by `UVCCamera::getGamma` when `id_camera` is non-null; `JNI_ERR` otherwise.
 */
static jint nativeGetGamma(JNIEnv *env, jobject thiz,
	ID_TYPE id_camera) {

	jint result = 0;
	ENTER();
	UVCCamera *camera = reinterpret_cast<UVCCamera *>(id_camera);
	if (LIKELY(camera)) {
		result = camera->getGamma();
	}
	RETURN(result, jint);
}
//======================================================================
// The corresponding Java method must not be static
/**
 * \brief Update the WhiteBlance limit values exposed to the Java instance.
 *
 * \pre `id_camera` is non-null for a limit update.
 *
 * \post On success, writes `mWhiteBlanceMin`, `mWhiteBlanceMax`, `mWhiteBlanceDef` on the Java instance.
 *
 * \param[in] env (JNIEnv *) JNI environment.
 * \param[in] thiz (jobject) Java `UVCCamera` instance.
 * \param[in] id_camera (ID_TYPE) Native `UVCCamera` pointer handle stored in Java `mNativePtr`. The trampoline does not allocate or free this handle.
 *
 * \return 0 on success, the `UVCCamera::updateWhiteBlanceLimit` error otherwise, or `JNI_ERR` when `id_camera` is null.
 */
static jint nativeUpdateWhiteBlanceLimit(JNIEnv *env, jobject thiz,
	ID_TYPE id_camera) {
	jint result = JNI_ERR;
	ENTER();
	UVCCamera *camera = reinterpret_cast<UVCCamera *>(id_camera);
	if (LIKELY(camera)) {
		int min, max, def;
		result = camera->updateWhiteBlanceLimit(min, max, def);
		if (!result) {
			// Write the value back to Java
			setField_int(env, thiz, "mWhiteBlanceMin", min);
			setField_int(env, thiz, "mWhiteBlanceMax", max);
			setField_int(env, thiz, "mWhiteBlanceDef", def);
		}
	}
	RETURN(result, jint);
}

/**
 * \brief Call `UVCCamera::setWhiteBlance` to set the WhiteBlance value.
 *
 * \pre `id_camera` is non-null for the native call; otherwise `JNI_ERR` is returned.
 *
 * \param[in] env (JNIEnv *) JNI environment.
 * \param[in] thiz (jobject) Java `UVCCamera` instance.
 * \param[in] id_camera (ID_TYPE) Native `UVCCamera` pointer handle stored in Java `mNativePtr`. The trampoline does not allocate or free this handle.
 * \param[in] whiteBlance (jint) Value passed to the native setter.
 *
 * \return Value returned by `UVCCamera::setWhiteBlance` when `id_camera` is non-null; `JNI_ERR` otherwise.
 */
static jint nativeSetWhiteBlance(JNIEnv *env, jobject thiz,
	ID_TYPE id_camera, jint whiteBlance) {

	jint result = JNI_ERR;
	ENTER();
	UVCCamera *camera = reinterpret_cast<UVCCamera *>(id_camera);
	if (LIKELY(camera)) {
		result = camera->setWhiteBlance(whiteBlance);
	}
	RETURN(result, jint);
}

/**
 * \brief Call `UVCCamera::getWhiteBlance` to read the WhiteBlance value.
 *
 * \pre `id_camera` is non-null for the native call; otherwise `JNI_ERR` is returned.
 *
 * \param[in] env (JNIEnv *) JNI environment.
 * \param[in] thiz (jobject) Java `UVCCamera` instance.
 * \param[in] id_camera (ID_TYPE) Native `UVCCamera` pointer handle stored in Java `mNativePtr`. The trampoline does not allocate or free this handle.
 *
 * \return Value returned by `UVCCamera::getWhiteBlance` when `id_camera` is non-null; `JNI_ERR` otherwise.
 */
static jint nativeGetWhiteBlance(JNIEnv *env, jobject thiz,
	ID_TYPE id_camera) {

	jint result = 0;
	ENTER();
	UVCCamera *camera = reinterpret_cast<UVCCamera *>(id_camera);
	if (LIKELY(camera)) {
		result = camera->getWhiteBlance();
	}
	RETURN(result, jint);
}
//======================================================================
// The corresponding Java method must not be static
/**
 * \brief Update the WhiteBlanceCompo limit values exposed to the Java instance.
 *
 * \pre `id_camera` is non-null for a limit update.
 *
 * \post On success, writes `mWhiteBlanceCompoMin`, `mWhiteBlanceCompoMax`, `mWhiteBlanceCompoDef` on the Java instance.
 *
 * \param[in] env (JNIEnv *) JNI environment.
 * \param[in] thiz (jobject) Java `UVCCamera` instance.
 * \param[in] id_camera (ID_TYPE) Native `UVCCamera` pointer handle stored in Java `mNativePtr`. The trampoline does not allocate or free this handle.
 *
 * \return 0 on success, the `UVCCamera::updateWhiteBlanceCompoLimit` error otherwise, or `JNI_ERR` when `id_camera` is null.
 */
static jint nativeUpdateWhiteBlanceCompoLimit(JNIEnv *env, jobject thiz,
	ID_TYPE id_camera) {
	jint result = JNI_ERR;
	ENTER();
	UVCCamera *camera = reinterpret_cast<UVCCamera *>(id_camera);
	if (LIKELY(camera)) {
		int min, max, def;
		result = camera->updateWhiteBlanceCompoLimit(min, max, def);
		if (!result) {
			// Write the value back to Java
			setField_int(env, thiz, "mWhiteBlanceCompoMin", min);
			setField_int(env, thiz, "mWhiteBlanceCompoMax", max);
			setField_int(env, thiz, "mWhiteBlanceCompoDef", def);
		}
	}
	RETURN(result, jint);
}

/**
 * \brief Call `UVCCamera::setWhiteBlanceCompo` to set the WhiteBlanceCompo value.
 *
 * \pre `id_camera` is non-null for the native call; otherwise `JNI_ERR` is returned.
 *
 * \param[in] env (JNIEnv *) JNI environment.
 * \param[in] thiz (jobject) Java `UVCCamera` instance.
 * \param[in] id_camera (ID_TYPE) Native `UVCCamera` pointer handle stored in Java `mNativePtr`. The trampoline does not allocate or free this handle.
 * \param[in] whiteBlance_compo (jint) `whiteBlance_compo` argument.
 *
 * \return Value returned by `UVCCamera::setWhiteBlanceCompo` when `id_camera` is non-null; `JNI_ERR` otherwise.
 */
static jint nativeSetWhiteBlanceCompo(JNIEnv *env, jobject thiz,
	ID_TYPE id_camera, jint whiteBlance_compo) {

	jint result = JNI_ERR;
	ENTER();
	UVCCamera *camera = reinterpret_cast<UVCCamera *>(id_camera);
	if (LIKELY(camera)) {
		result = camera->setWhiteBlanceCompo(whiteBlance_compo);
	}
	RETURN(result, jint);
}

/**
 * \brief Call `UVCCamera::getWhiteBlanceCompo` to read the WhiteBlanceCompo value.
 *
 * \pre `id_camera` is non-null for the native call; otherwise `JNI_ERR` is returned.
 *
 * \param[in] env (JNIEnv *) JNI environment.
 * \param[in] thiz (jobject) Java `UVCCamera` instance.
 * \param[in] id_camera (ID_TYPE) Native `UVCCamera` pointer handle stored in Java `mNativePtr`. The trampoline does not allocate or free this handle.
 *
 * \return Value returned by `UVCCamera::getWhiteBlanceCompo` when `id_camera` is non-null; `JNI_ERR` otherwise.
 */
static jint nativeGetWhiteBlanceCompo(JNIEnv *env, jobject thiz,
	ID_TYPE id_camera) {

	jint result = 0;
	ENTER();
	UVCCamera *camera = reinterpret_cast<UVCCamera *>(id_camera);
	if (LIKELY(camera)) {
		result = camera->getWhiteBlanceCompo();
	}
	RETURN(result, jint);
}
//======================================================================
// The corresponding Java method must not be static
/**
 * \brief Update the BacklightComp limit values exposed to the Java instance.
 *
 * \pre `id_camera` is non-null for a limit update.
 *
 * \post On success, writes `mBacklightCompMin`, `mBacklightCompMax`, `mBacklightCompDef` on the Java instance.
 *
 * \param[in] env (JNIEnv *) JNI environment.
 * \param[in] thiz (jobject) Java `UVCCamera` instance.
 * \param[in] id_camera (ID_TYPE) Native `UVCCamera` pointer handle stored in Java `mNativePtr`. The trampoline does not allocate or free this handle.
 *
 * \return 0 on success, the `UVCCamera::updateBacklightCompLimit` error otherwise, or `JNI_ERR` when `id_camera` is null.
 */
static jint nativeUpdateBacklightCompLimit(JNIEnv *env, jobject thiz,
	ID_TYPE id_camera) {
	jint result = JNI_ERR;
	ENTER();
	UVCCamera *camera = reinterpret_cast<UVCCamera *>(id_camera);
	if (LIKELY(camera)) {
		int min, max, def;
		result = camera->updateBacklightCompLimit(min, max, def);
		if (!result) {
			// Write the value back to Java
			setField_int(env, thiz, "mBacklightCompMin", min);
			setField_int(env, thiz, "mBacklightCompMax", max);
			setField_int(env, thiz, "mBacklightCompDef", def);
		}
	}
	RETURN(result, jint);
}

/**
 * \brief Call `UVCCamera::setBacklightComp` to set the BacklightComp value.
 *
 * \pre `id_camera` is non-null for the native call; otherwise `JNI_ERR` is returned.
 *
 * \param[in] env (JNIEnv *) JNI environment.
 * \param[in] thiz (jobject) Java `UVCCamera` instance.
 * \param[in] id_camera (ID_TYPE) Native `UVCCamera` pointer handle stored in Java `mNativePtr`. The trampoline does not allocate or free this handle.
 * \param[in] backlight_comp (jint) `backlight_comp` argument.
 *
 * \return Value returned by `UVCCamera::setBacklightComp` when `id_camera` is non-null; `JNI_ERR` otherwise.
 */
static jint nativeSetBacklightComp(JNIEnv *env, jobject thiz,
	ID_TYPE id_camera, jint backlight_comp) {

	jint result = JNI_ERR;
	ENTER();
	UVCCamera *camera = reinterpret_cast<UVCCamera *>(id_camera);
	if (LIKELY(camera)) {
		result = camera->setBacklightComp(backlight_comp);
	}
	RETURN(result, jint);
}

/**
 * \brief Call `UVCCamera::getBacklightComp` to read the BacklightComp value.
 *
 * \pre `id_camera` is non-null for the native call; otherwise `JNI_ERR` is returned.
 *
 * \param[in] env (JNIEnv *) JNI environment.
 * \param[in] thiz (jobject) Java `UVCCamera` instance.
 * \param[in] id_camera (ID_TYPE) Native `UVCCamera` pointer handle stored in Java `mNativePtr`. The trampoline does not allocate or free this handle.
 *
 * \return Value returned by `UVCCamera::getBacklightComp` when `id_camera` is non-null; `JNI_ERR` otherwise.
 */
static jint nativeGetBacklightComp(JNIEnv *env, jobject thiz,
	ID_TYPE id_camera) {

	jint result = 0;
	ENTER();
	UVCCamera *camera = reinterpret_cast<UVCCamera *>(id_camera);
	if (LIKELY(camera)) {
		result = camera->getBacklightComp();
	}
	RETURN(result, jint);
}
//======================================================================
// The corresponding Java method must not be static
/**
 * \brief Update the Saturation limit values exposed to the Java instance.
 *
 * \pre `id_camera` is non-null for a limit update.
 *
 * \post On success, writes `mSaturationMin`, `mSaturationMax`, `mSaturationDef` on the Java instance.
 *
 * \param[in] env (JNIEnv *) JNI environment.
 * \param[in] thiz (jobject) Java `UVCCamera` instance.
 * \param[in] id_camera (ID_TYPE) Native `UVCCamera` pointer handle stored in Java `mNativePtr`. The trampoline does not allocate or free this handle.
 *
 * \return 0 on success, the `UVCCamera::updateSaturationLimit` error otherwise, or `JNI_ERR` when `id_camera` is null.
 */
static jint nativeUpdateSaturationLimit(JNIEnv *env, jobject thiz,
	ID_TYPE id_camera) {
	jint result = JNI_ERR;
	ENTER();
	UVCCamera *camera = reinterpret_cast<UVCCamera *>(id_camera);
	if (LIKELY(camera)) {
		int min, max, def;
		result = camera->updateSaturationLimit(min, max, def);
		if (!result) {
			// Write the value back to Java
			setField_int(env, thiz, "mSaturationMin", min);
			setField_int(env, thiz, "mSaturationMax", max);
			setField_int(env, thiz, "mSaturationDef", def);
		}
	}
	RETURN(result, jint);
}

/**
 * \brief Call `UVCCamera::setSaturation` to set the Saturation value.
 *
 * \pre `id_camera` is non-null for the native call; otherwise `JNI_ERR` is returned.
 *
 * \param[in] env (JNIEnv *) JNI environment.
 * \param[in] thiz (jobject) Java `UVCCamera` instance.
 * \param[in] id_camera (ID_TYPE) Native `UVCCamera` pointer handle stored in Java `mNativePtr`. The trampoline does not allocate or free this handle.
 * \param[in] saturation (jint) Value passed to the native setter.
 *
 * \return Value returned by `UVCCamera::setSaturation` when `id_camera` is non-null; `JNI_ERR` otherwise.
 */
static jint nativeSetSaturation(JNIEnv *env, jobject thiz,
	ID_TYPE id_camera, jint saturation) {

	jint result = JNI_ERR;
	ENTER();
	UVCCamera *camera = reinterpret_cast<UVCCamera *>(id_camera);
	if (LIKELY(camera)) {
		result = camera->setSaturation(saturation);
	}
	RETURN(result, jint);
}

/**
 * \brief Call `UVCCamera::getSaturation` to read the Saturation value.
 *
 * \pre `id_camera` is non-null for the native call; otherwise `JNI_ERR` is returned.
 *
 * \param[in] env (JNIEnv *) JNI environment.
 * \param[in] thiz (jobject) Java `UVCCamera` instance.
 * \param[in] id_camera (ID_TYPE) Native `UVCCamera` pointer handle stored in Java `mNativePtr`. The trampoline does not allocate or free this handle.
 *
 * \return Value returned by `UVCCamera::getSaturation` when `id_camera` is non-null; `JNI_ERR` otherwise.
 */
static jint nativeGetSaturation(JNIEnv *env, jobject thiz,
	ID_TYPE id_camera) {

	jint result = 0;
	ENTER();
	UVCCamera *camera = reinterpret_cast<UVCCamera *>(id_camera);
	if (LIKELY(camera)) {
		result = camera->getSaturation();
	}
	RETURN(result, jint);
}
//======================================================================
// The corresponding Java method must not be static
/**
 * \brief Update the Hue limit values exposed to the Java instance.
 *
 * \pre `id_camera` is non-null for a limit update.
 *
 * \post On success, writes `mHueMin`, `mHueMax`, `mHueDef` on the Java instance.
 *
 * \param[in] env (JNIEnv *) JNI environment.
 * \param[in] thiz (jobject) Java `UVCCamera` instance.
 * \param[in] id_camera (ID_TYPE) Native `UVCCamera` pointer handle stored in Java `mNativePtr`. The trampoline does not allocate or free this handle.
 *
 * \return 0 on success, the `UVCCamera::updateHueLimit` error otherwise, or `JNI_ERR` when `id_camera` is null.
 */
static jint nativeUpdateHueLimit(JNIEnv *env, jobject thiz,
	ID_TYPE id_camera) {
	jint result = JNI_ERR;
	ENTER();
	UVCCamera *camera = reinterpret_cast<UVCCamera *>(id_camera);
	if (LIKELY(camera)) {
		int min, max, def;
		result = camera->updateHueLimit(min, max, def);
		if (!result) {
			// Write the value back to Java
			setField_int(env, thiz, "mHueMin", min);
			setField_int(env, thiz, "mHueMax", max);
			setField_int(env, thiz, "mHueDef", def);
		}
	}
	RETURN(result, jint);
}

/**
 * \brief Call `UVCCamera::setHue` to set the Hue value.
 *
 * \pre `id_camera` is non-null for the native call; otherwise `JNI_ERR` is returned.
 *
 * \param[in] env (JNIEnv *) JNI environment.
 * \param[in] thiz (jobject) Java `UVCCamera` instance.
 * \param[in] id_camera (ID_TYPE) Native `UVCCamera` pointer handle stored in Java `mNativePtr`. The trampoline does not allocate or free this handle.
 * \param[in] hue (jint) Value passed to the native setter.
 *
 * \return Value returned by `UVCCamera::setHue` when `id_camera` is non-null; `JNI_ERR` otherwise.
 */
static jint nativeSetHue(JNIEnv *env, jobject thiz,
	ID_TYPE id_camera, jint hue) {

	jint result = JNI_ERR;
	ENTER();
	UVCCamera *camera = reinterpret_cast<UVCCamera *>(id_camera);
	if (LIKELY(camera)) {
		result = camera->setHue(hue);
	}
	RETURN(result, jint);
}

/**
 * \brief Call `UVCCamera::getHue` to read the Hue value.
 *
 * \pre `id_camera` is non-null for the native call; otherwise `JNI_ERR` is returned.
 *
 * \param[in] env (JNIEnv *) JNI environment.
 * \param[in] thiz (jobject) Java `UVCCamera` instance.
 * \param[in] id_camera (ID_TYPE) Native `UVCCamera` pointer handle stored in Java `mNativePtr`. The trampoline does not allocate or free this handle.
 *
 * \return Value returned by `UVCCamera::getHue` when `id_camera` is non-null; `JNI_ERR` otherwise.
 */
static jint nativeGetHue(JNIEnv *env, jobject thiz,
	ID_TYPE id_camera) {

	jint result = 0;
	ENTER();
	UVCCamera *camera = reinterpret_cast<UVCCamera *>(id_camera);
	if (LIKELY(camera)) {
		result = camera->getHue();
	}
	RETURN(result, jint);
}
//======================================================================
// The corresponding Java method must not be static
/**
 * \brief Update the AutoHue limit values exposed to the Java instance.
 *
 * \pre `id_camera` is non-null for a limit update.
 *
 * \post On success, writes `mAutoHueMin`, `mAutoHueMax`, `mAutoHueDef` on the Java instance.
 *
 * \param[in] env (JNIEnv *) JNI environment.
 * \param[in] thiz (jobject) Java `UVCCamera` instance.
 * \param[in] id_camera (ID_TYPE) Native `UVCCamera` pointer handle stored in Java `mNativePtr`. The trampoline does not allocate or free this handle.
 *
 * \return 0 on success, the `UVCCamera::updateAutoHueLimit` error otherwise, or `JNI_ERR` when `id_camera` is null.
 */
static jint nativeUpdateAutoHueLimit(JNIEnv *env, jobject thiz,
	ID_TYPE id_camera) {
	jint result = JNI_ERR;
	ENTER();
	UVCCamera *camera = reinterpret_cast<UVCCamera *>(id_camera);
	if (LIKELY(camera)) {
		int min, max, def;
		result = camera->updateAutoHueLimit(min, max, def);
		if (!result) {
			// Write the value back to Java
			setField_int(env, thiz, "mAutoHueMin", min);
			setField_int(env, thiz, "mAutoHueMax", max);
			setField_int(env, thiz, "mAutoHueDef", def);
		}
	}
	RETURN(result, jint);
}

/**
 * \brief Call `UVCCamera::setAutoHue` to set the AutoHue value.
 *
 * \pre `id_camera` is non-null for the native call; otherwise `JNI_ERR` is returned.
 *
 * \param[in] env (JNIEnv *) JNI environment.
 * \param[in] thiz (jobject) Java `UVCCamera` instance.
 * \param[in] id_camera (ID_TYPE) Native `UVCCamera` pointer handle stored in Java `mNativePtr`. The trampoline does not allocate or free this handle.
 * \param[in] autohue (jboolean) `autohue` argument.
 *
 * \return Value returned by `UVCCamera::setAutoHue` when `id_camera` is non-null; `JNI_ERR` otherwise.
 */
static jint nativeSetAutoHue(JNIEnv *env, jobject thiz,
	ID_TYPE id_camera, jboolean autohue) {

	jint result = JNI_ERR;
	ENTER();
	UVCCamera *camera = reinterpret_cast<UVCCamera *>(id_camera);
	if (LIKELY(camera)) {
		result = camera->setAutoHue(autohue);
	}
	RETURN(result, jint);
}

/**
 * \brief Call `UVCCamera::getAutoHue` to read the AutoHue value.
 *
 * \pre `id_camera` is non-null for the native call; otherwise `JNI_ERR` is returned.
 *
 * \param[in] env (JNIEnv *) JNI environment.
 * \param[in] thiz (jobject) Java `UVCCamera` instance.
 * \param[in] id_camera (ID_TYPE) Native `UVCCamera` pointer handle stored in Java `mNativePtr`. The trampoline does not allocate or free this handle.
 *
 * \return Value returned by `UVCCamera::getAutoHue` when `id_camera` is non-null; `JNI_ERR` otherwise.
 */
static jint nativeGetAutoHue(JNIEnv *env, jobject thiz,
	ID_TYPE id_camera) {

	jint result = JNI_ERR;
	ENTER();
	UVCCamera *camera = reinterpret_cast<UVCCamera *>(id_camera);
	if (LIKELY(camera)) {
		result = camera->getAutoHue();
	}
	RETURN(result, jint);
}
//======================================================================
// The corresponding Java method must not be static
/**
 * \brief Update the PowerlineFrequency limit values exposed to the Java instance.
 *
 * \pre `id_camera` is non-null for a limit update.
 *
 * \post On success, writes `mPowerlineFrequencyMin`, `mPowerlineFrequencyMax`, `mPowerlineFrequencyDef` on the Java instance.
 *
 * \param[in] env (JNIEnv *) JNI environment.
 * \param[in] thiz (jobject) Java `UVCCamera` instance.
 * \param[in] id_camera (ID_TYPE) Native `UVCCamera` pointer handle stored in Java `mNativePtr`. The trampoline does not allocate or free this handle.
 *
 * \return 0 on success, the `UVCCamera::updatePowerlineFrequencyLimit` error otherwise, or `JNI_ERR` when `id_camera` is null.
 */
static jint nativeUpdatePowerlineFrequencyLimit(JNIEnv *env, jobject thiz,
	ID_TYPE id_camera) {
	jint result = JNI_ERR;
	ENTER();
	UVCCamera *camera = reinterpret_cast<UVCCamera *>(id_camera);
	if (LIKELY(camera)) {
		int min, max, def;
		result = camera->updatePowerlineFrequencyLimit(min, max, def);
		if (!result) {
			// Write the value back to Java
			setField_int(env, thiz, "mPowerlineFrequencyMin", min);
			setField_int(env, thiz, "mPowerlineFrequencyMax", max);
			setField_int(env, thiz, "mPowerlineFrequencyDef", def);
		}
	}
	RETURN(result, jint);
}

/**
 * \brief Call `UVCCamera::setPowerlineFrequency` to set the PowerlineFrequency value.
 *
 * \pre `id_camera` is non-null for the native call; otherwise `JNI_ERR` is returned.
 *
 * \param[in] env (JNIEnv *) JNI environment.
 * \param[in] thiz (jobject) Java `UVCCamera` instance.
 * \param[in] id_camera (ID_TYPE) Native `UVCCamera` pointer handle stored in Java `mNativePtr`. The trampoline does not allocate or free this handle.
 * \param[in] frequency (jint) `frequency` argument.
 *
 * \return Value returned by `UVCCamera::setPowerlineFrequency` when `id_camera` is non-null; `JNI_ERR` otherwise.
 */
static jint nativeSetPowerlineFrequency(JNIEnv *env, jobject thiz,
	ID_TYPE id_camera, jint frequency) {

	jint result = JNI_ERR;
	ENTER();
	UVCCamera *camera = reinterpret_cast<UVCCamera *>(id_camera);
	if (LIKELY(camera)) {
		result = camera->setPowerlineFrequency(frequency);
	}
	RETURN(result, jint);
}

/**
 * \brief Call `UVCCamera::getPowerlineFrequency` to read the PowerlineFrequency value.
 *
 * \pre `id_camera` is non-null for the native call; otherwise `JNI_ERR` is returned.
 *
 * \param[in] env (JNIEnv *) JNI environment.
 * \param[in] thiz (jobject) Java `UVCCamera` instance.
 * \param[in] id_camera (ID_TYPE) Native `UVCCamera` pointer handle stored in Java `mNativePtr`. The trampoline does not allocate or free this handle.
 *
 * \return Value returned by `UVCCamera::getPowerlineFrequency` when `id_camera` is non-null; `JNI_ERR` otherwise.
 */
static jint nativeGetPowerlineFrequency(JNIEnv *env, jobject thiz,
	ID_TYPE id_camera) {

	jint result = 0;
	ENTER();
	UVCCamera *camera = reinterpret_cast<UVCCamera *>(id_camera);
	if (LIKELY(camera)) {
		result = camera->getPowerlineFrequency();
	}
	RETURN(result, jint);
}
//======================================================================
// The corresponding Java method must not be static
/**
 * \brief Update the Zoom limit values exposed to the Java instance.
 *
 * \pre `id_camera` is non-null for a limit update.
 *
 * \post On success, writes `mZoomMin`, `mZoomMax`, `mZoomDef` on the Java instance.
 *
 * \param[in] env (JNIEnv *) JNI environment.
 * \param[in] thiz (jobject) Java `UVCCamera` instance.
 * \param[in] id_camera (ID_TYPE) Native `UVCCamera` pointer handle stored in Java `mNativePtr`. The trampoline does not allocate or free this handle.
 *
 * \return 0 on success, the `UVCCamera::updateZoomLimit` error otherwise, or `JNI_ERR` when `id_camera` is null.
 */
static jint nativeUpdateZoomLimit(JNIEnv *env, jobject thiz,
	ID_TYPE id_camera) {
	jint result = JNI_ERR;
	ENTER();
	UVCCamera *camera = reinterpret_cast<UVCCamera *>(id_camera);
	if (LIKELY(camera)) {
		int min, max, def;
		result = camera->updateZoomLimit(min, max, def);
		if (!result) {
			// Write the value back to Java
			setField_int(env, thiz, "mZoomMin", min);
			setField_int(env, thiz, "mZoomMax", max);
			setField_int(env, thiz, "mZoomDef", def);
		}
	}
	RETURN(result, jint);
}

/**
 * \brief Call `UVCCamera::setZoom` to set the Zoom value.
 *
 * \pre `id_camera` is non-null for the native call; otherwise `JNI_ERR` is returned.
 *
 * \param[in] env (JNIEnv *) JNI environment.
 * \param[in] thiz (jobject) Java `UVCCamera` instance.
 * \param[in] id_camera (ID_TYPE) Native `UVCCamera` pointer handle stored in Java `mNativePtr`. The trampoline does not allocate or free this handle.
 * \param[in] zoom (jint) Value passed to the native setter.
 *
 * \return Value returned by `UVCCamera::setZoom` when `id_camera` is non-null; `JNI_ERR` otherwise.
 */
static jint nativeSetZoom(JNIEnv *env, jobject thiz,
	ID_TYPE id_camera, jint zoom) {

	jint result = JNI_ERR;
	ENTER();
	UVCCamera *camera = reinterpret_cast<UVCCamera *>(id_camera);
	if (LIKELY(camera)) {
		result = camera->setZoom(zoom);
	}
	RETURN(result, jint);
}

/**
 * \brief Call `UVCCamera::getZoom` to read the Zoom value.
 *
 * \pre `id_camera` is non-null for the native call; otherwise `JNI_ERR` is returned.
 *
 * \param[in] env (JNIEnv *) JNI environment.
 * \param[in] thiz (jobject) Java `UVCCamera` instance.
 * \param[in] id_camera (ID_TYPE) Native `UVCCamera` pointer handle stored in Java `mNativePtr`. The trampoline does not allocate or free this handle.
 *
 * \return Value returned by `UVCCamera::getZoom` when `id_camera` is non-null; `JNI_ERR` otherwise.
 */
static jint nativeGetZoom(JNIEnv *env, jobject thiz,
	ID_TYPE id_camera) {

	jint result = 0;
	ENTER();
	UVCCamera *camera = reinterpret_cast<UVCCamera *>(id_camera);
	if (LIKELY(camera)) {
		result = camera->getZoom();
	}
	RETURN(result, jint);
}
//======================================================================
// The corresponding Java method must not be static
/**
 * \brief Update the ZoomRel limit values exposed to the Java instance.
 *
 * \pre `id_camera` is non-null for a limit update.
 *
 * \post On success, writes `mZoomRelMin`, `mZoomRelMax`, `mZoomRelDef` on the Java instance.
 *
 * \param[in] env (JNIEnv *) JNI environment.
 * \param[in] thiz (jobject) Java `UVCCamera` instance.
 * \param[in] id_camera (ID_TYPE) Native `UVCCamera` pointer handle stored in Java `mNativePtr`. The trampoline does not allocate or free this handle.
 *
 * \return 0 on success, the `UVCCamera::updateZoomRelLimit` error otherwise, or `JNI_ERR` when `id_camera` is null.
 */
static jint nativeUpdateZoomRelLimit(JNIEnv *env, jobject thiz,
	ID_TYPE id_camera) {
	jint result = JNI_ERR;
	ENTER();
	UVCCamera *camera = reinterpret_cast<UVCCamera *>(id_camera);
	if (LIKELY(camera)) {
		int min, max, def;
		result = camera->updateZoomRelLimit(min, max, def);
		if (!result) {
			// Write the value back to Java
			setField_int(env, thiz, "mZoomRelMin", min);
			setField_int(env, thiz, "mZoomRelMax", max);
			setField_int(env, thiz, "mZoomRelDef", def);
		}
	}
	RETURN(result, jint);
}

/**
 * \brief Call `UVCCamera::setZoomRel` to set the ZoomRel value.
 *
 * \pre `id_camera` is non-null for the native call; otherwise `JNI_ERR` is returned.
 *
 * \param[in] env (JNIEnv *) JNI environment.
 * \param[in] thiz (jobject) Java `UVCCamera` instance.
 * \param[in] id_camera (ID_TYPE) Native `UVCCamera` pointer handle stored in Java `mNativePtr`. The trampoline does not allocate or free this handle.
 * \param[in] zoom_rel (jint) Value passed to the native setter.
 *
 * \return Value returned by `UVCCamera::setZoomRel` when `id_camera` is non-null; `JNI_ERR` otherwise.
 */
static jint nativeSetZoomRel(JNIEnv *env, jobject thiz,
	ID_TYPE id_camera, jint zoom_rel) {

	jint result = JNI_ERR;
	ENTER();
	UVCCamera *camera = reinterpret_cast<UVCCamera *>(id_camera);
	if (LIKELY(camera)) {
		result = camera->setZoomRel(zoom_rel);
	}
	RETURN(result, jint);
}

/**
 * \brief Call `UVCCamera::getZoomRel` to read the ZoomRel value.
 *
 * \pre `id_camera` is non-null for the native call; otherwise `JNI_ERR` is returned.
 *
 * \param[in] env (JNIEnv *) JNI environment.
 * \param[in] thiz (jobject) Java `UVCCamera` instance.
 * \param[in] id_camera (ID_TYPE) Native `UVCCamera` pointer handle stored in Java `mNativePtr`. The trampoline does not allocate or free this handle.
 *
 * \return Value returned by `UVCCamera::getZoomRel` when `id_camera` is non-null; `JNI_ERR` otherwise.
 */
static jint nativeGetZoomRel(JNIEnv *env, jobject thiz,
	ID_TYPE id_camera) {

	jint result = 0;
	ENTER();
	UVCCamera *camera = reinterpret_cast<UVCCamera *>(id_camera);
	if (LIKELY(camera)) {
		result = camera->getZoomRel();
	}
	RETURN(result, jint);
}
//======================================================================
// The corresponding Java method must not be static
/**
 * \brief Update the DigitalMultiplier limit values exposed to the Java instance.
 *
 * \pre `id_camera` is non-null for a limit update.
 *
 * \post On success, writes `mDigitalMultiplierMin`, `mDigitalMultiplierMax`, `mDigitalMultiplierDef` on the Java instance.
 *
 * \param[in] env (JNIEnv *) JNI environment.
 * \param[in] thiz (jobject) Java `UVCCamera` instance.
 * \param[in] id_camera (ID_TYPE) Native `UVCCamera` pointer handle stored in Java `mNativePtr`. The trampoline does not allocate or free this handle.
 *
 * \return 0 on success, the `UVCCamera::updateDigitalMultiplierLimit` error otherwise, or `JNI_ERR` when `id_camera` is null.
 */
static jint nativeUpdateDigitalMultiplierLimit(JNIEnv *env, jobject thiz,
	ID_TYPE id_camera) {
	jint result = JNI_ERR;
	ENTER();
	UVCCamera *camera = reinterpret_cast<UVCCamera *>(id_camera);
	if (LIKELY(camera)) {
		int min, max, def;
		result = camera->updateDigitalMultiplierLimit(min, max, def);
		if (!result) {
			// Write the value back to Java
			setField_int(env, thiz, "mDigitalMultiplierMin", min);
			setField_int(env, thiz, "mDigitalMultiplierMax", max);
			setField_int(env, thiz, "mDigitalMultiplierDef", def);
		}
	}
	RETURN(result, jint);
}

/**
 * \brief Call `UVCCamera::setDigitalMultiplier` to set the DigitalMultiplier value.
 *
 * \pre `id_camera` is non-null for the native call; otherwise `JNI_ERR` is returned.
 *
 * \param[in] env (JNIEnv *) JNI environment.
 * \param[in] thiz (jobject) Java `UVCCamera` instance.
 * \param[in] id_camera (ID_TYPE) Native `UVCCamera` pointer handle stored in Java `mNativePtr`. The trampoline does not allocate or free this handle.
 * \param[in] multiplier (jint) `multiplier` argument.
 *
 * \return Value returned by `UVCCamera::setDigitalMultiplier` when `id_camera` is non-null; `JNI_ERR` otherwise.
 */
static jint nativeSetDigitalMultiplier(JNIEnv *env, jobject thiz,
	ID_TYPE id_camera, jint multiplier) {

	jint result = JNI_ERR;
	ENTER();
	UVCCamera *camera = reinterpret_cast<UVCCamera *>(id_camera);
	if (LIKELY(camera)) {
		result = camera->setDigitalMultiplier(multiplier);
	}
	RETURN(result, jint);
}

/**
 * \brief Call `UVCCamera::getDigitalMultiplier` to read the DigitalMultiplier value.
 *
 * \pre `id_camera` is non-null for the native call; otherwise `JNI_ERR` is returned.
 *
 * \param[in] env (JNIEnv *) JNI environment.
 * \param[in] thiz (jobject) Java `UVCCamera` instance.
 * \param[in] id_camera (ID_TYPE) Native `UVCCamera` pointer handle stored in Java `mNativePtr`. The trampoline does not allocate or free this handle.
 *
 * \return Value returned by `UVCCamera::getDigitalMultiplier` when `id_camera` is non-null; `JNI_ERR` otherwise.
 */
static jint nativeGetDigitalMultiplier(JNIEnv *env, jobject thiz,
	ID_TYPE id_camera) {

	jint result = 0;
	ENTER();
	UVCCamera *camera = reinterpret_cast<UVCCamera *>(id_camera);
	if (LIKELY(camera)) {
		result = camera->getDigitalMultiplier();
	}
	RETURN(result, jint);
}
//======================================================================
// The corresponding Java method must not be static
/**
 * \brief Update the DigitalMultiplierLimit limit values exposed to the Java instance.
 *
 * \pre `id_camera` is non-null for a limit update.
 *
 * \post On success, writes `mDigitalMultiplierLimitMin`, `mDigitalMultiplierLimitMax`, `mDigitalMultiplierLimitDef` on the Java instance.
 *
 * \param[in] env (JNIEnv *) JNI environment.
 * \param[in] thiz (jobject) Java `UVCCamera` instance.
 * \param[in] id_camera (ID_TYPE) Native `UVCCamera` pointer handle stored in Java `mNativePtr`. The trampoline does not allocate or free this handle.
 *
 * \return 0 on success, the `UVCCamera::updateDigitalMultiplierLimitLimit` error otherwise, or `JNI_ERR` when `id_camera` is null.
 */
static jint nativeUpdateDigitalMultiplierLimitLimit(JNIEnv *env, jobject thiz,
	ID_TYPE id_camera) {
	jint result = JNI_ERR;
	ENTER();
	UVCCamera *camera = reinterpret_cast<UVCCamera *>(id_camera);
	if (LIKELY(camera)) {
		int min, max, def;
		result = camera->updateDigitalMultiplierLimitLimit(min, max, def);
		if (!result) {
			// Write the value back to Java
			setField_int(env, thiz, "mDigitalMultiplierLimitMin", min);
			setField_int(env, thiz, "mDigitalMultiplierLimitMax", max);
			setField_int(env, thiz, "mDigitalMultiplierLimitDef", def);
		}
	}
	RETURN(result, jint);
}

/**
 * \brief Call `UVCCamera::setDigitalMultiplierLimit` to set the DigitalMultiplier value.
 *
 * \pre `id_camera` is non-null for the native call; otherwise `JNI_ERR` is returned.
 *
 * \param[in] env (JNIEnv *) JNI environment.
 * \param[in] thiz (jobject) Java `UVCCamera` instance.
 * \param[in] id_camera (ID_TYPE) Native `UVCCamera` pointer handle stored in Java `mNativePtr`. The trampoline does not allocate or free this handle.
 * \param[in] multiplier_limit (jint) `multiplier_limit` argument.
 *
 * \return Value returned by `UVCCamera::setDigitalMultiplierLimit` when `id_camera` is non-null; `JNI_ERR` otherwise.
 */
static jint nativeSetDigitalMultiplierLimit(JNIEnv *env, jobject thiz,
	ID_TYPE id_camera, jint multiplier_limit) {

	jint result = JNI_ERR;
	ENTER();
	UVCCamera *camera = reinterpret_cast<UVCCamera *>(id_camera);
	if (LIKELY(camera)) {
		result = camera->setDigitalMultiplierLimit(multiplier_limit);
	}
	RETURN(result, jint);
}

/**
 * \brief Call `UVCCamera::getDigitalMultiplierLimit` to read the DigitalMultiplier value.
 *
 * \pre `id_camera` is non-null for the native call; otherwise `JNI_ERR` is returned.
 *
 * \param[in] env (JNIEnv *) JNI environment.
 * \param[in] thiz (jobject) Java `UVCCamera` instance.
 * \param[in] id_camera (ID_TYPE) Native `UVCCamera` pointer handle stored in Java `mNativePtr`. The trampoline does not allocate or free this handle.
 *
 * \return Value returned by `UVCCamera::getDigitalMultiplierLimit` when `id_camera` is non-null; `JNI_ERR` otherwise.
 */
static jint nativeGetDigitalMultiplierLimit(JNIEnv *env, jobject thiz,
	ID_TYPE id_camera) {

	jint result = 0;
	ENTER();
	UVCCamera *camera = reinterpret_cast<UVCCamera *>(id_camera);
	if (LIKELY(camera)) {
		result = camera->getDigitalMultiplierLimit();
	}
	RETURN(result, jint);
}
//======================================================================
// The corresponding Java method must not be static
/**
 * \brief Update the AnalogVideoStandard limit values exposed to the Java instance.
 *
 * \pre `id_camera` is non-null for a limit update.
 *
 * \post On success, writes `mAnalogVideoStandardMin`, `mAnalogVideoStandardMax`, `mAnalogVideoStandardDef` on the Java instance.
 *
 * \param[in] env (JNIEnv *) JNI environment.
 * \param[in] thiz (jobject) Java `UVCCamera` instance.
 * \param[in] id_camera (ID_TYPE) Native `UVCCamera` pointer handle stored in Java `mNativePtr`. The trampoline does not allocate or free this handle.
 *
 * \return 0 on success, the `UVCCamera::updateAnalogVideoStandardLimit` error otherwise, or `JNI_ERR` when `id_camera` is null.
 */
static jint nativeUpdateAnalogVideoStandardLimit(JNIEnv *env, jobject thiz,
	ID_TYPE id_camera) {
	jint result = JNI_ERR;
	ENTER();
	UVCCamera *camera = reinterpret_cast<UVCCamera *>(id_camera);
	if (LIKELY(camera)) {
		int min, max, def;
		result = camera->updateAnalogVideoStandardLimit(min, max, def);
		if (!result) {
			// Write the value back to Java
			setField_int(env, thiz, "mAnalogVideoStandardMin", min);
			setField_int(env, thiz, "mAnalogVideoStandardMax", max);
			setField_int(env, thiz, "mAnalogVideoStandardDef", def);
		}
	}
	RETURN(result, jint);
}

/**
 * \brief Call `UVCCamera::setAnalogVideoStandard` to set the AnalogVideoStandard value.
 *
 * \pre `id_camera` is non-null for the native call; otherwise `JNI_ERR` is returned.
 *
 * \param[in] env (JNIEnv *) JNI environment.
 * \param[in] thiz (jobject) Java `UVCCamera` instance.
 * \param[in] id_camera (ID_TYPE) Native `UVCCamera` pointer handle stored in Java `mNativePtr`. The trampoline does not allocate or free this handle.
 * \param[in] standard (jint) `standard` argument.
 *
 * \return Value returned by `UVCCamera::setAnalogVideoStandard` when `id_camera` is non-null; `JNI_ERR` otherwise.
 */
static jint nativeSetAnalogVideoStandard(JNIEnv *env, jobject thiz,
	ID_TYPE id_camera, jint standard) {

	jint result = JNI_ERR;
	ENTER();
	UVCCamera *camera = reinterpret_cast<UVCCamera *>(id_camera);
	if (LIKELY(camera)) {
		result = camera->setAnalogVideoStandard(standard);
	}
	RETURN(result, jint);
}

/**
 * \brief Call `UVCCamera::getAnalogVideoStandard` to read the AnalogVideoStandard value.
 *
 * \pre `id_camera` is non-null for the native call; otherwise `JNI_ERR` is returned.
 *
 * \param[in] env (JNIEnv *) JNI environment.
 * \param[in] thiz (jobject) Java `UVCCamera` instance.
 * \param[in] id_camera (ID_TYPE) Native `UVCCamera` pointer handle stored in Java `mNativePtr`. The trampoline does not allocate or free this handle.
 *
 * \return Value returned by `UVCCamera::getAnalogVideoStandard` when `id_camera` is non-null; `JNI_ERR` otherwise.
 */
static jint nativeGetAnalogVideoStandard(JNIEnv *env, jobject thiz,
	ID_TYPE id_camera) {

	jint result = 0;
	ENTER();
	UVCCamera *camera = reinterpret_cast<UVCCamera *>(id_camera);
	if (LIKELY(camera)) {
		result = camera->getAnalogVideoStandard();
	}
	RETURN(result, jint);
}
//======================================================================
// The corresponding Java method must not be static
/**
 * \brief Update the AnalogVideoLockState limit values exposed to the Java instance.
 *
 * \pre `id_camera` is non-null for a limit update.
 *
 * \post On success, writes `mAnalogVideoLockStateMin`, `mAnalogVideoLockStateMax`, `mAnalogVideoLockStateDef` on the Java instance.
 *
 * \param[in] env (JNIEnv *) JNI environment.
 * \param[in] thiz (jobject) Java `UVCCamera` instance.
 * \param[in] id_camera (ID_TYPE) Native `UVCCamera` pointer handle stored in Java `mNativePtr`. The trampoline does not allocate or free this handle.
 *
 * \return 0 on success, the `UVCCamera::updateAnalogVideoLockStateLimit` error otherwise, or `JNI_ERR` when `id_camera` is null.
 */
static jint nativeUpdateAnalogVideoLockStateLimit(JNIEnv *env, jobject thiz,
	ID_TYPE id_camera) {
	jint result = JNI_ERR;
	ENTER();
	UVCCamera *camera = reinterpret_cast<UVCCamera *>(id_camera);
	if (LIKELY(camera)) {
		int min, max, def;
		result = camera->updateAnalogVideoLockStateLimit(min, max, def);
		if (!result) {
			// Write the value back to Java
			setField_int(env, thiz, "mAnalogVideoLockStateMin", min);
			setField_int(env, thiz, "mAnalogVideoLockStateMax", max);
			setField_int(env, thiz, "mAnalogVideoLockStateDef", def);
		}
	}
	RETURN(result, jint);
}

/**
 * \brief Call `UVCCamera::setAnalogVideoLockState` to set the AnalogVideoLockState value.
 *
 * The Java method table entry is misspelled `nativeSetAnalogVideoLoackState`.
 *
 * \pre `id_camera` is non-null for the native call; otherwise `JNI_ERR` is returned.
 *
 * \param[in] env (JNIEnv *) JNI environment.
 * \param[in] thiz (jobject) Java `UVCCamera` instance.
 * \param[in] id_camera (ID_TYPE) Native `UVCCamera` pointer handle stored in Java `mNativePtr`. The trampoline does not allocate or free this handle.
 * \param[in] state (jint) `state` argument.
 *
 * \return Value returned by `UVCCamera::setAnalogVideoLockState` when `id_camera` is non-null; `JNI_ERR` otherwise.
 */
static jint nativeSetAnalogVideoLockState(JNIEnv *env, jobject thiz,
	ID_TYPE id_camera, jint state) {

	jint result = JNI_ERR;
	ENTER();
	UVCCamera *camera = reinterpret_cast<UVCCamera *>(id_camera);
	if (LIKELY(camera)) {
		result = camera->setAnalogVideoLockState(state);
	}
	RETURN(result, jint);
}

/**
 * \brief Call `UVCCamera::getAnalogVideoLockState` to read the AnalogVideoLockState value.
 *
 * The Java method table entry is misspelled `nativeGetAnalogVideoLoackState`.
 *
 * \pre `id_camera` is non-null for the native call; otherwise `JNI_ERR` is returned.
 *
 * \param[in] env (JNIEnv *) JNI environment.
 * \param[in] thiz (jobject) Java `UVCCamera` instance.
 * \param[in] id_camera (ID_TYPE) Native `UVCCamera` pointer handle stored in Java `mNativePtr`. The trampoline does not allocate or free this handle.
 *
 * \return Value returned by `UVCCamera::getAnalogVideoLockState` when `id_camera` is non-null; `JNI_ERR` otherwise.
 */
static jint nativeGetAnalogVideoLockState(JNIEnv *env, jobject thiz,
	ID_TYPE id_camera) {

	jint result = 0;
	ENTER();
	UVCCamera *camera = reinterpret_cast<UVCCamera *>(id_camera);
	if (LIKELY(camera)) {
		result = camera->getAnalogVideoLockState();
	}
	RETURN(result, jint);
}
//======================================================================
// The corresponding Java method must not be static
/**
 * \brief Update the Privacy limit values exposed to the Java instance.
 *
 * \pre `id_camera` is non-null for a limit update.
 *
 * \post On success, writes `mPrivacyMin`, `mPrivacyMax`, `mPrivacyDef` on the Java instance.
 *
 * \param[in] env (JNIEnv *) JNI environment.
 * \param[in] thiz (jobject) Java `UVCCamera` instance.
 * \param[in] id_camera (ID_TYPE) Native `UVCCamera` pointer handle stored in Java `mNativePtr`. The trampoline does not allocate or free this handle.
 *
 * \return 0 on success, the `UVCCamera::updatePrivacyLimit` error otherwise, or `JNI_ERR` when `id_camera` is null.
 */
static jint nativeUpdatePrivacyLimit(JNIEnv *env, jobject thiz,
	ID_TYPE id_camera) {
	jint result = JNI_ERR;
	ENTER();
	UVCCamera *camera = reinterpret_cast<UVCCamera *>(id_camera);
	if (LIKELY(camera)) {
		int min, max, def;
		result = camera->updatePrivacyLimit(min, max, def);
		if (!result) {
			// Write the value back to Java
			setField_int(env, thiz, "mPrivacyMin", min);
			setField_int(env, thiz, "mPrivacyMax", max);
			setField_int(env, thiz, "mPrivacyDef", def);
		}
	}
	RETURN(result, jint);
}

/**
 * \brief Call `UVCCamera::setPrivacy` to set the Privacy value.
 *
 * The Java boolean argument is converted to 1 for true and 0 for false.
 *
 * \pre `id_camera` is non-null for the native call; otherwise `JNI_ERR` is returned.
 *
 * \param[in] env (JNIEnv *) JNI environment.
 * \param[in] thiz (jobject) Java `UVCCamera` instance.
 * \param[in] id_camera (ID_TYPE) Native `UVCCamera` pointer handle stored in Java `mNativePtr`. The trampoline does not allocate or free this handle.
 * \param[in] privacy (jboolean) Value passed to the native setter.
 *
 * \return Value returned by `UVCCamera::setPrivacy` when `id_camera` is non-null; `JNI_ERR` otherwise.
 */
static jint nativeSetPrivacy(JNIEnv *env, jobject thiz,
	ID_TYPE id_camera, jboolean privacy) {

	jint result = JNI_ERR;
	ENTER();
	UVCCamera *camera = reinterpret_cast<UVCCamera *>(id_camera);
	if (LIKELY(camera)) {
		result = camera->setPrivacy(privacy ? 1: 0);
	}
	RETURN(result, jint);
}

/**
 * \brief Call `UVCCamera::getPrivacy` to read the Privacy value.
 *
 * \pre `id_camera` is non-null for the native call; otherwise `JNI_ERR` is returned.
 *
 * \param[in] env (JNIEnv *) JNI environment.
 * \param[in] thiz (jobject) Java `UVCCamera` instance.
 * \param[in] id_camera (ID_TYPE) Native `UVCCamera` pointer handle stored in Java `mNativePtr`. The trampoline does not allocate or free this handle.
 *
 * \return Value returned by `UVCCamera::getPrivacy` when `id_camera` is non-null; `JNI_ERR` otherwise.
 */
static jint nativeGetPrivacy(JNIEnv *env, jobject thiz,
	ID_TYPE id_camera) {

	jint result = JNI_ERR;
	ENTER();
	UVCCamera *camera = reinterpret_cast<UVCCamera *>(id_camera);
	if (LIKELY(camera)) {
		result = camera->getPrivacy();
	}
	RETURN(result, jint);
}
//**********************************************************************
//
//**********************************************************************
/**
 * \brief Find the Java class and register a JNI native method table.
 *
 * The inner `RegisterNatives` result shadows the outer result, so registration failures are logged but not returned.
 *
 * \pre `class_name` is resolvable by `FindClass` for registration to occur.
 *
 * \post `RegisterNatives` is attempted on the found class; failures are logged but not propagated.
 *
 * \param[in] env (JNIEnv*) JNI environment.
 * \param[in] class_name (char *) Java class name to find.
 * \param[in] methods (JNINativeMethod *) JNI native method table to register.
 * \param[in] num_methods (int) Number of entries in the method table.
 *
 * \return Always 0 in the current implementation; registration failures are logged but not propagated.
 */
jint registerNativeMethods(JNIEnv* env, const char *class_name, JNINativeMethod *methods, int num_methods) {
	int result = 0;

	jclass clazz = env->FindClass(class_name);
	if (LIKELY(clazz)) {
		int result = env->RegisterNatives(clazz, methods, num_methods);
		if (UNLIKELY(result < 0)) {
			LOGE("registerNativeMethods failed(class=%s)", class_name);
		}
	} else {
		LOGE("registerNativeMethods: class'%s' not found", class_name);
	}
	return result;
}

static JNINativeMethod methods[] = {
	{ "nativeCreate",					"()J", (void *) nativeCreate },
	{ "nativeDestroy",					"(J)V", (void *) nativeDestroy },
	//
	{ "nativeConnect",					"(JIIIIILjava/lang/String;)I", (void *) nativeConnect },
	{ "nativeRelease",					"(J)I", (void *) nativeRelease },

	{ "nativeSetStatusCallback",		"(JLcom/serenegiant/usb/IStatusCallback;)I", (void *) nativeSetStatusCallback },
	{ "nativeSetButtonCallback",		"(JLcom/serenegiant/usb/IButtonCallback;)I", (void *) nativeSetButtonCallback },

	{ "nativeGetSupportedSize",			"(J)Ljava/lang/String;", (void *) nativeGetSupportedSize },
	{ "nativeSetPreviewSize",			"(JIIIIIF)I", (void *) nativeSetPreviewSize },
	{ "nativeStartPreview",				"(J)I", (void *) nativeStartPreview },
	{ "nativeStopPreview",				"(J)I", (void *) nativeStopPreview },
	{ "nativeSetPreviewDisplay",		"(JLandroid/view/Surface;)I", (void *) nativeSetPreviewDisplay },
	{ "nativeSetFrameCallback",			"(JLcom/serenegiant/usb/IFrameCallback;I)I", (void *) nativeSetFrameCallback },

	{ "nativeSetCaptureDisplay",		"(JLandroid/view/Surface;)I", (void *) nativeSetCaptureDisplay },

	{ "nativeGetCtrlSupports",			"(J)J", (void *) nativeGetCtrlSupports },
	{ "nativeGetProcSupports",			"(J)J", (void *) nativeGetProcSupports },

	{ "nativeUpdateScanningModeLimit",	"(J)I", (void *) nativeUpdateScanningModeLimit },
	{ "nativeSetScanningMode",			"(JI)I", (void *) nativeSetScanningMode },
	{ "nativeGetScanningMode",			"(J)I", (void *) nativeGetScanningMode },
			
	{ "nativeUpdateExposureModeLimit",	"(J)I", (void *) nativeUpdateExposureModeLimit },
	{ "nativeSetExposureMode",			"(JI)I", (void *) nativeSetExposureMode },
	{ "nativeGetExposureMode",			"(J)I", (void *) nativeGetExposureMode },

	{ "nativeUpdateExposurePriorityLimit","(J)I", (void *) nativeUpdateExposurePriorityLimit },
	{ "nativeSetExposurePriority",		"(JI)I", (void *) nativeSetExposurePriority },
	{ "nativeGetExposurePriority",		"(J)I", (void *) nativeGetExposurePriority },
			
	{ "nativeUpdateExposureLimit",		"(J)I", (void *) nativeUpdateExposureLimit },
	{ "nativeSetExposure",				"(JI)I", (void *) nativeSetExposure },
	{ "nativeGetExposure",				"(J)I", (void *) nativeGetExposure },
			
	{ "nativeUpdateExposureRelLimit",	"(J)I", (void *) nativeUpdateExposureRelLimit },
	{ "nativeSetExposureRel",			"(JI)I", (void *) nativeSetExposureRel },
	{ "nativeGetExposureRel",			"(J)I", (void *) nativeGetExposureRel },
			
	{ "nativeUpdateAutoFocusLimit",		"(J)I", (void *) nativeUpdateAutoFocusLimit },
	{ "nativeSetAutoFocus",				"(JZ)I", (void *) nativeSetAutoFocus },
	{ "nativeGetAutoFocus",				"(J)I", (void *) nativeGetAutoFocus },

	{ "nativeUpdateFocusLimit",			"(J)I", (void *) nativeUpdateFocusLimit },
	{ "nativeSetFocus",					"(JI)I", (void *) nativeSetFocus },
	{ "nativeGetFocus",					"(J)I", (void *) nativeGetFocus },

	{ "nativeUpdateFocusRelLimit",		"(J)I", (void *) nativeUpdateFocusRelLimit },
	{ "nativeSetFocusRel",				"(JI)I", (void *) nativeSetFocusRel },
	{ "nativeGetFocusRel",				"(J)I", (void *) nativeGetFocusRel },
	
//	{ "nativeUpdateFocusSimpleLimit",	"(J)I", (void *) nativeUpdateFocusSimpleLimit },
//	{ "nativeSetFocusSimple",			"(JI)I", (void *) nativeSetFocusSimple },
//	{ "nativeGetFocusSimple",			"(J)I", (void *) nativeGetFocusSimple },
			
	{ "nativeUpdateIrisLimit",			"(J)I", (void *) nativeUpdateIrisLimit },
	{ "nativeSetIris",					"(JI)I", (void *) nativeSetIris },
	{ "nativeGetIris",					"(J)I", (void *) nativeGetIris },
	
	{ "nativeUpdateIrisRelLimit",		"(J)I", (void *) nativeUpdateIrisRelLimit },
	{ "nativeSetIrisRel",				"(JI)I", (void *) nativeSetIrisRel },
	{ "nativeGetIrisRel",				"(J)I", (void *) nativeGetIrisRel },
	
	{ "nativeUpdatePanLimit",			"(J)I", (void *) nativeUpdatePanLimit },
	{ "nativeSetPan",					"(JI)I", (void *) nativeSetPan },
	{ "nativeGetPan",					"(J)I", (void *) nativeGetPan },
	
	{ "nativeUpdateTiltLimit",			"(J)I", (void *) nativeUpdateTiltLimit },
	{ "nativeSetTilt",					"(JI)I", (void *) nativeSetTilt },
	{ "nativeGetTilt",					"(J)I", (void *) nativeGetTilt },
	
	{ "nativeUpdateRollLimit",			"(J)I", (void *) nativeUpdateRollLimit },
	{ "nativeSetRoll",					"(JI)I", (void *) nativeSetRoll },
	{ "nativeGetRoll",					"(J)I", (void *) nativeGetRoll },
	
	{ "nativeUpdatePanRelLimit",		"(J)I", (void *) nativeUpdatePanRelLimit },
	{ "nativeSetPanRel",				"(JI)I", (void *) nativeSetPanRel },
	{ "nativeGetPanRel",				"(J)I", (void *) nativeGetPanRel },
	
	{ "nativeUpdateTiltRelLimit",		"(J)I", (void *) nativeUpdateTiltRelLimit },
	{ "nativeSetTiltRel",				"(JI)I", (void *) nativeSetTiltRel },
	{ "nativeGetTiltRel",				"(J)I", (void *) nativeGetTiltRel },
	
	{ "nativeUpdateRollRelLimit",		"(J)I", (void *) nativeUpdateRollRelLimit },
	{ "nativeSetRollRel",				"(JI)I", (void *) nativeSetRollRel },
	{ "nativeGetRollRel",				"(J)I", (void *) nativeGetRollRel },
	
	{ "nativeUpdateAutoWhiteBlanceLimit","(J)I", (void *) nativeUpdateAutoWhiteBlanceLimit },
	{ "nativeSetAutoWhiteBlance",		"(JZ)I", (void *) nativeSetAutoWhiteBlance },
	{ "nativeGetAutoWhiteBlance",		"(J)I", (void *) nativeGetAutoWhiteBlance },

	{ "nativeUpdateAutoWhiteBlanceCompoLimit","(J)I", (void *) nativeUpdateAutoWhiteBlanceCompoLimit },
	{ "nativeSetAutoWhiteBlanceCompo",		"(JZ)I", (void *) nativeSetAutoWhiteBlanceCompo },
	{ "nativeGetAutoWhiteBlanceCompo",		"(J)I", (void *) nativeGetAutoWhiteBlanceCompo },
	
	{ "nativeUpdateWhiteBlanceLimit",	"(J)I", (void *) nativeUpdateWhiteBlanceLimit },
	{ "nativeSetWhiteBlance",			"(JI)I", (void *) nativeSetWhiteBlance },
	{ "nativeGetWhiteBlance",			"(J)I", (void *) nativeGetWhiteBlance },

	{ "nativeUpdateWhiteBlanceCompoLimit","(J)I", (void *) nativeUpdateWhiteBlanceCompoLimit },
	{ "nativeSetWhiteBlanceCompo",		"(JI)I", (void *) nativeSetWhiteBlanceCompo },
	{ "nativeGetWhiteBlanceCompo",		"(J)I", (void *) nativeGetWhiteBlanceCompo },
	
	{ "nativeUpdateBacklightCompLimit",	"(J)I", (void *) nativeUpdateBacklightCompLimit },
	{ "nativeSetBacklightComp",			"(JI)I", (void *) nativeSetBacklightComp },
	{ "nativeGetBacklightComp",			"(J)I", (void *) nativeGetBacklightComp },

	{ "nativeUpdateBrightnessLimit",	"(J)I", (void *) nativeUpdateBrightnessLimit },
	{ "nativeSetBrightness",			"(JI)I", (void *) nativeSetBrightness },
	{ "nativeGetBrightness",			"(J)I", (void *) nativeGetBrightness },

	{ "nativeUpdateContrastLimit",		"(J)I", (void *) nativeUpdateContrastLimit },
	{ "nativeSetContrast",				"(JI)I", (void *) nativeSetContrast },
	{ "nativeGetContrast",				"(J)I", (void *) nativeGetContrast },

	{ "nativeUpdateAutoContrastLimit",	"(J)I", (void *) nativeUpdateAutoContrastLimit },
	{ "nativeSetAutoContrast",			"(JZ)I", (void *) nativeSetAutoContrast },
	{ "nativeGetAutoContrast",			"(J)I", (void *) nativeGetAutoContrast },

	{ "nativeUpdateSharpnessLimit",		"(J)I", (void *) nativeUpdateSharpnessLimit },
	{ "nativeSetSharpness",				"(JI)I", (void *) nativeSetSharpness },
	{ "nativeGetSharpness",				"(J)I", (void *) nativeGetSharpness },

	{ "nativeUpdateGainLimit",			"(J)I", (void *) nativeUpdateGainLimit },
	{ "nativeSetGain",					"(JI)I", (void *) nativeSetGain },
	{ "nativeGetGain",					"(J)I", (void *) nativeGetGain },

	{ "nativeUpdateGammaLimit",			"(J)I", (void *) nativeUpdateGammaLimit },
	{ "nativeSetGamma",					"(JI)I", (void *) nativeSetGamma },
	{ "nativeGetGamma",					"(J)I", (void *) nativeGetGamma },

	{ "nativeUpdateSaturationLimit",	"(J)I", (void *) nativeUpdateSaturationLimit },
	{ "nativeSetSaturation",			"(JI)I", (void *) nativeSetSaturation },
	{ "nativeGetSaturation",			"(J)I", (void *) nativeGetSaturation },

	{ "nativeUpdateHueLimit",			"(J)I", (void *) nativeUpdateHueLimit },
	{ "nativeSetHue",					"(JI)I", (void *) nativeSetHue },
	{ "nativeGetHue",					"(J)I", (void *) nativeGetHue },

	{ "nativeUpdateAutoHueLimit",		"(J)I", (void *) nativeUpdateAutoHueLimit },
	{ "nativeSetAutoHue",				"(JZ)I", (void *) nativeSetAutoHue },
	{ "nativeGetAutoHue",				"(J)I", (void *) nativeGetAutoHue },
			
	{ "nativeUpdatePowerlineFrequencyLimit","(J)I", (void *) nativeUpdatePowerlineFrequencyLimit },
	{ "nativeSetPowerlineFrequency",	"(JI)I", (void *) nativeSetPowerlineFrequency },
	{ "nativeGetPowerlineFrequency",	"(J)I", (void *) nativeGetPowerlineFrequency },

	{ "nativeUpdateZoomLimit",			"(J)I", (void *) nativeUpdateZoomLimit },
	{ "nativeSetZoom",					"(JI)I", (void *) nativeSetZoom },
	{ "nativeGetZoom",					"(J)I", (void *) nativeGetZoom },
	
	{ "nativeUpdateZoomRelLimit",		"(J)I", (void *) nativeUpdateZoomRelLimit },
	{ "nativeSetZoomRel",				"(JI)I", (void *) nativeSetZoomRel },
	{ "nativeGetZoomRel",				"(J)I", (void *) nativeGetZoomRel },
	
	{ "nativeUpdateDigitalMultiplierLimit","(J)I", (void *) nativeUpdateDigitalMultiplierLimit },
	{ "nativeSetDigitalMultiplier","(JI)I", (void *) nativeSetDigitalMultiplier },
	{ "nativeGetDigitalMultiplier","(J)I", (void *) nativeGetDigitalMultiplier },
	
	{ "nativeUpdateDigitalMultiplierLimitLimit","(J)I", (void *) nativeUpdateDigitalMultiplierLimitLimit },
	{ "nativeSetDigitalMultiplierLimit","(JI)I", (void *) nativeSetDigitalMultiplierLimit },
	{ "nativeGetDigitalMultiplierLimit","(J)I", (void *) nativeGetDigitalMultiplierLimit },
	
	{ "nativeUpdateAnalogVideoStandardLimit","(J)I", (void *) nativeUpdateAnalogVideoStandardLimit },
	{ "nativeSetAnalogVideoStandard",		"(JI)I", (void *) nativeSetAnalogVideoStandard },
	{ "nativeGetAnalogVideoStandard",		"(J)I", (void *) nativeGetAnalogVideoStandard },
	
	{ "nativeUpdateAnalogVideoLockStateLimit","(J)I", (void *) nativeUpdateAnalogVideoLockStateLimit },
	{ "nativeSetAnalogVideoLoackState",	"(JI)I", (void *) nativeSetAnalogVideoLockState },
	{ "nativeGetAnalogVideoLoackState",	"(J)I", (void *) nativeGetAnalogVideoLockState },
	
	{ "nativeUpdatePrivacyLimit",		"(J)I", (void *) nativeUpdatePrivacyLimit },
	{ "nativeSetPrivacy",				"(JZ)I", (void *) nativeSetPrivacy },
	{ "nativeGetPrivacy",				"(J)I", (void *) nativeGetPrivacy },
};

/**
 * \brief Register the UVCCamera JNI native method table.
 *
 * \post The static `methods` table is registered for `com/serenegiant/usb/UVCCamera`; the current implementation returns 0.
 *
 * \param[in] env (JNIEnv *) JNI environment.
 *
 * \return 0 when `registerNativeMethods` returns a non-negative value, otherwise -1; in the current implementation this is always 0.
 */
int register_uvccamera(JNIEnv *env) {
	LOGV("register_uvccamera:");
	if (registerNativeMethods(env,
		"com/serenegiant/usb/UVCCamera",
		methods, NUM_ARRAY_ELEMENTS(methods)) < 0) {
		return -1;
	}
    return 0;
}
