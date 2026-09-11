package com.github.cptskippy.uvccamera.flutter;

import android.hardware.usb.UsbDevice;
import android.util.Log;

import androidx.annotation.NonNull;

import com.serenegiant.usb.USBMonitor;

/**
 * Relay USB monitor device events to the UVC camera platform.
 *
 * Implements {@link USBMonitor.OnDeviceConnectListener}. Attach and detach events are cast as device
 * events, while connect and disconnect events also settle the pending device permission request.
 */
/* package-private */ class UvcCameraDeviceMonitorListener implements USBMonitor.OnDeviceConnectListener {

/**
 * Log tag
 */
    private static final String TAG = UvcCameraDeviceMonitorListener.class.getCanonicalName();

/**
 * The UVC camera platform
 */
    private final UvcCameraPlatform uvcCameraPlatform;

/**
 * Create a new {@link UvcCameraDeviceMonitorListener} instance.
 *
 * Args:
 *     uvcCameraPlatform: the UVC camera platform
 */
    public UvcCameraDeviceMonitorListener(final UvcCameraPlatform uvcCameraPlatform) {
        this.uvcCameraPlatform = uvcCameraPlatform;
    }

/**
 * Cast the device attached event to the device event stream.
 *
 * Args:
 *     device: the attached USB device
 */
    @Override
    public void onAttach(@NonNull UsbDevice device) {
        Log.v(TAG, "onAttach: device=" + device);

        uvcCameraPlatform.castDeviceAttachedEvent(device);
    }

/**
 * Cast the device detached event to the device event stream.
 *
 * Args:
 *     device: the detached USB device
 */
    @Override
    public void onDettach(@NonNull UsbDevice device) {
        Log.v(TAG, "onDettach: device=" + device);

        uvcCameraPlatform.castDeviceDetachedEvent(device);
    }

/**
 * Handle a USB device connect event.
 *
 * Args:
 *     device: the connected USB device
 *     ctrlBlock: the USB control block for the device
 *     createNew: true if the control block was newly created for this connect
 *
 * Code Paths:
 *     1. Fulfill any pending device permission request for this device (UVCCamera connects
 *        automatically once permission is granted)
 *     2. Cast the device connected event to the device event stream
 */
    @Override
    public void onConnect(@NonNull UsbDevice device, @NonNull USBMonitor.UsbControlBlock ctrlBlock, boolean createNew) {
        Log.v(TAG, "onConnect"
                + ": device=" + device
                + ", ctrlBlock=" + ctrlBlock
                + ", createNew=" + createNew
        );

// NOTE: UVCCamera automatically connects on permission being granted, so handling permission request result
        uvcCameraPlatform.fulfillDevicePermissionRequest(device);

        uvcCameraPlatform.castDeviceConnectedEvent(device);
    }

/**
 * Cast the device disconnected event to the device event stream.
 *
 * Args:
 *     device: the disconnected USB device
 *     ctrlBlock: the USB control block for the device
 */
    @Override
    public void onDisconnect(@NonNull UsbDevice device, @NonNull USBMonitor.UsbControlBlock ctrlBlock) {
        Log.v(TAG, "onDisconnect: device=" + device + ", ctrlBlock=" + ctrlBlock);

        uvcCameraPlatform.castDeviceDisconnectedEvent(device);
    }

/**
 * Reject the pending device permission request when the user cancels the permission dialog.
 *
 * Args:
 *     device: the USB device the permission was requested for
 */
    @Override
    public void onCancel(@NonNull UsbDevice device) {
        Log.v(TAG, "onCancel: device=" + device);

        uvcCameraPlatform.rejectDevicePermissionRequest(device);
    }

}
