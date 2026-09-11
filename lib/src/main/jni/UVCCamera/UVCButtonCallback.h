#ifndef UVCBUTTONCALLBACK_H_
#define UVCBUTTONCALLBACK_H_

#include "libUVCCamera.h"
#include <pthread.h>
#include <android/native_window.h>
#include "objectarray.h"

#pragma interface

/**
 * \brief JNI callback bridge for UVC button events.
 *
 * Registers a libuvc button callback and forwards button press/release
 * events to the Java callback object.
 *
 * Exports:
 *     UVCButtonCallback: Button callback bridge.
 *
 * Dependencies:
 *     - libuvc: UVC button callback registration.
 *     - JNI: Java callback object and method dispatch.
 */

// for callback to Java object
typedef struct {
	jmethodID onButton;
} Fields_ibuttoncallback;

/**
 * \brief Forward UVC button events to a Java callback object.
 *
 * Thread Safety:
 *     Callback registration and event dispatch are synchronized with a
 *     private mutex.
 */
class UVCButtonCallback {
private:
	uvc_device_handle_t *mDeviceHandle;
 	pthread_mutex_t button_mutex;
 	jobject mButtonCallbackObj;
 	Fields_ibuttoncallback ibuttoncallback_fields;
 	void notifyButtonCallback(JNIEnv *env, int button, int state);
 	static void uvc_button_callback(int button, int state, void *user_ptr);
public:
	UVCButtonCallback(uvc_device_handle_t *devh);
	~UVCButtonCallback();

	/**
	 * \brief Set the Java object that receives button events.
	 *
	 * \param[in] env JNI environment.
	 * \param[in] button_callback_obj Java callback object; ownership remains with
	 *     the caller.
	 * \return 0 on success, or a negative error code if callback lookup fails.
	 */
	int setCallback(JNIEnv *env, jobject button_callback_obj);
};

#endif /* UVCBUTTONCALLBACK_H_ */
