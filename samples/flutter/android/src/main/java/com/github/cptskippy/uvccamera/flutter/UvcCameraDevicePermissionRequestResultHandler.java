package com.github.cptskippy.uvccamera.flutter;

/**
 * Handler to be notified when the device permission request result is available.
 */
@FunctionalInterface
/* package-private */ interface UvcCameraDevicePermissionRequestResultHandler {

/**
 * Report the result of a device permission request.
 *
 * Args:
 *     granted: true if the device permission was granted, false otherwise
 */
    void onResult(boolean granted);

}
