#ifndef UVCSTATUSCALLBACK_H_
#define UVCSTATUSCALLBACK_H_

#include "libUVCCamera.h"
#include <pthread.h>
#include <android/native_window.h>
#include "objectarray.h"

#pragma interface

/**
 * \brief JNI callback bridge for UVC status events.
 *
 * Registers a libuvc status callback and forwards status class, event,
 * selector, and attribute data to the Java callback object.
 *
 * Exports:
 *     UVCStatusCallback: Status callback bridge.
 *
 * Dependencies:
 *     - libuvc: UVC status callback registration.
 *     - JNI: Java callback object and method dispatch.
 */

// for callback to Java object
typedef struct {
	jmethodID onStatus;
} Fields_istatuscallback;

/**
 * \brief Forward UVC status events to a Java callback object.
 *
 * Thread Safety:
 *     Callback registration and event dispatch are synchronized with a
 *     private mutex.
 */
class UVCStatusCallback {
private:
	uvc_device_handle_t *mDeviceHandle;
 	pthread_mutex_t status_mutex;
 	jobject mStatusCallbackObj;
 	Fields_istatuscallback istatuscallback_fields;
 	void notifyStatusCallback(JNIEnv *env, uvc_status_class status_class, int event, int selector, uvc_status_attribute status_attribute, void *data, size_t data_len);
 	static void uvc_status_callback(uvc_status_class status_class, int event, int selector, uvc_status_attribute status_attribute, void *data, size_t data_len, void *user_ptr);
public:
	UVCStatusCallback(uvc_device_handle_t *devh);
	~UVCStatusCallback();

	/**
	 * \brief Set the Java object that receives status events.
	 *
	 * \param[in] env JNI environment.
	 * \param[in] status_callback_obj Java callback object; ownership remains with
	 *     the caller.
	 * \return 0 on success, or a negative error code if callback lookup fails.
	 */
	int setCallback(JNIEnv *env, jobject status_callback_obj);
};

#endif /* UVCSTATUSCALLBACK_H_ */
