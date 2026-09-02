package com.github.cptskippy.uvccamera.flutter;

import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.ImageFormat;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.hardware.usb.UsbDevice;
import android.media.MediaRecorder;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.util.Pair;

import androidx.annotation.NonNull;

import com.serenegiant.usb.Size;
import com.serenegiant.usb.USBMonitor;
import com.serenegiant.usb.UVCCamera;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import io.flutter.plugin.common.BinaryMessenger;
import io.flutter.plugin.common.EventChannel;
import io.flutter.view.TextureRegistry;

/**
 * Android-side UVC camera platform for the Flutter plugin.
 *
 * Owns the USB monitor and one {@link UvcCameraResources} entry per opened camera. Opens cameras with the
 * best-matching frame size and format, starts the preview into a Flutter surface producer, and bridges
 * device, error, status, and button events to Flutter through event channels.
 *
 * Methods that take a cameraId throw IllegalArgumentException when no resources are registered for that
 * camera. Event casting posts to the main looper because Flutter event sinks must be used from the
 * platform thread.
 */
/* package-private */ class UvcCameraPlatform {

/**
 * Log tag
 */
    private static final String TAG = UvcCameraPlatform.class.getSimpleName();

/**
 * libuvc's {@code uvc_status_class} to {@code UvcCameraStatusClass} enum mapping.
 */
    public static final Map<Integer, String> STATUS_CLASS_LIBUVC_VALUE_TO_ENUM_NAME = Map.of(
/* UVC_STATUS_CLASS_CONTROL */ 0x10, "control",
/* UVC_STATUS_CLASS_CONTROL_CAMERA */ 0x11, "controlCamera",
/* UVC_STATUS_CLASS_CONTROL_PROCESSING */ 0x12, "controlProcessing"
    );

/**
 * libuvc's {@code uvc_status_attribute} to {@code UvcCameraStatusAttribute} enum mapping.
 */
    public static final Map<Integer, String> STATUS_ATTRIBUTE_LIBUVC_VALUE_TO_ENUM_NAME = Map.of(
/* UVC_STATUS_ATTRIBUTE_VALUE_CHANGE */ 0x00, "valueChange",
/* UVC_STATUS_ATTRIBUTE_INFO_CHANGE */ 0x01, "infoChange",
/* UVC_STATUS_ATTRIBUTE_FAILURE_CHANGE */ 0x02, "errorChange",
/* UVC_STATUS_ATTRIBUTE_UNKNOWN */ 0xff, "unknown"
    );

/**
 * Main looper handler
 */
    private final Handler mainLooperHandler = new Handler(Looper.getMainLooper());

/**
 * Application context
 */
    private final WeakReference<Context> applicationContext;

/**
 * Binary messenger
 */
    private final WeakReference<BinaryMessenger> binaryMessenger;

/**
 * Texture registry
 */
    private final TextureRegistry textureRegistry;

/**
 * "uvccamera/device_events" event stream handler
 */
    private final UvcCameraDeviceEventStreamHandler deviceEventStreamHandler;

/**
 * USB monitor
 */
    private final USBMonitor usbMonitor;

/**
 * Pending device permission request device name
 */
    private String pendingDevicePermissionRequestDeviceName;

/**
 * Pending device permission request result handler
 */
    private UvcCameraDevicePermissionRequestResultHandler pendingDevicePermissionRequestResultHandler;

/**
 * Lock for {@link #pendingDevicePermissionRequestDeviceName} and
 * {@link #pendingDevicePermissionRequestResultHandler}
 */
    private final Object pendingDevicePermissionRequestLock = new Object();

/**
 * Opened camera resources
 */
    private final Map<Integer, UvcCameraResources> camerasResources = new ConcurrentHashMap<>();

/**
 * Create a new {@link UvcCameraPlatform} instance.
 *
 * Args:
 *     applicationContext: the application context
 *     binaryMessenger: the binary messenger
 *     textureRegistry: the texture registry
 *     deviceEventStreamHandler: the stream handler for the "uvccamera/device_events" event channel
 *
 * Side Effects:
 *     - Creates and registers a USB monitor whose device listener is bound to this platform
 */
    public UvcCameraPlatform(
            final @NonNull Context applicationContext,
            final @NonNull BinaryMessenger binaryMessenger,
            final @NonNull TextureRegistry textureRegistry,
            final @NonNull UvcCameraDeviceEventStreamHandler deviceEventStreamHandler
    ) {
        this.applicationContext = new WeakReference<>(applicationContext);
        this.binaryMessenger = new WeakReference<>(binaryMessenger);
        this.textureRegistry = textureRegistry;
        this.deviceEventStreamHandler = deviceEventStreamHandler;

        usbMonitor = new USBMonitor(applicationContext, new UvcCameraDeviceMonitorListener(this));
        usbMonitor.register();
    }

/**
 * Release the platform resources.
 *
 * Side Effects:
 *     - Unregisters and destroys the USB monitor
 *     - Clears the application context and binary messenger references
 */
/* package-private */ void release() {
        usbMonitor.unregister();
        usbMonitor.destroy();

        applicationContext.clear();
        binaryMessenger.clear();
    }

/**
 * Cast the device attached event.
 *
 * Args:
 *     device: the USB device
 */
/* package-private */ void castDeviceAttachedEvent(final UsbDevice device) {
        Log.v(TAG, "castDeviceAttachedEvent: device=" + device);

        final var eventSink = deviceEventStreamHandler.getEventSink();
        if (eventSink == null) {
            Log.w(TAG, "castDeviceAttachedEvent: event sink not found");
            return;
        }

        final var event = Map.of(
                "device", Map.of(
                        "name", device.getDeviceName(),
                        "deviceClass", device.getDeviceClass(),
                        "deviceSubclass", device.getDeviceSubclass(),
                        "vendorId", device.getVendorId(),
                        "productId", device.getProductId()
                ),
                "type", "attached"
        );

        mainLooperHandler.post(
                () -> eventSink.success(event)
        );
    }

/**
 * Cast the device detached event.
 *
 * Args:
 *     device: the USB device
 */
/* package-private */ void castDeviceDetachedEvent(final UsbDevice device) {
        Log.v(TAG, "castDeviceDetachedEvent: device=" + device);

        final var eventSink = deviceEventStreamHandler.getEventSink();
        if (eventSink == null) {
            Log.w(TAG, "castDeviceDetachedEvent: event sink not found");
            return;
        }

        final var event = Map.of(
                "device", Map.of(
                        "name", device.getDeviceName(),
                        "deviceClass", device.getDeviceClass(),
                        "deviceSubclass", device.getDeviceSubclass(),
                        "vendorId", device.getVendorId(),
                        "productId", device.getProductId()
                ),
                "type", "detached"
        );

        mainLooperHandler.post(
                () -> eventSink.success(event)
        );
    }

/**
 * Cast the device connected event.
 *
 * Args:
 *     device: the USB device
 */
/* package-private */ void castDeviceConnectedEvent(final UsbDevice device) {
        Log.v(TAG, "castDeviceConnectedEvent: device=" + device);

        final var eventSink = deviceEventStreamHandler.getEventSink();
        if (eventSink == null) {
            Log.w(TAG, "castDeviceConnectedEvent: event sink not found");
            return;
        }

        final var event = Map.of(
                "device", Map.of(
                        "name", device.getDeviceName(),
                        "deviceClass", device.getDeviceClass(),
                        "deviceSubclass", device.getDeviceSubclass(),
                        "vendorId", device.getVendorId(),
                        "productId", device.getProductId()
                ),
                "type", "connected"
        );

        mainLooperHandler.post(
                () -> eventSink.success(event)
        );
    }

/**
 * Cast the device disconnected event.
 *
 * Args:
 *     device: the USB device
 */
/* package-private */ void castDeviceDisconnectedEvent(final UsbDevice device) {
        Log.v(TAG, "castDeviceDisconnectedEvent: device=" + device);

        final var eventSink = deviceEventStreamHandler.getEventSink();
        if (eventSink == null) {
            Log.w(TAG, "castDeviceDisconnectedEvent: event sink not found");
            return;
        }

        final var event = Map.of(
                "device", Map.of(
                        "name", device.getDeviceName(),
                        "deviceClass", device.getDeviceClass(),
                        "deviceSubclass", device.getDeviceSubclass(),
                        "vendorId", device.getVendorId(),
                        "productId", device.getProductId()
                ),
                "type", "disconnected"
        );

        mainLooperHandler.post(
                () -> eventSink.success(event)
        );
    }

/**
 * Check whether the device supports UVC camera.
 *
 * Returns:
 *     true if the device supports UVC camera, false otherwise
 *
 * Raises:
 *     IllegalStateException: the application context reference has expired
 */
    public boolean isSupported() {
        final var applicationContext = this.applicationContext.get();
        if (applicationContext == null) {
            throw new IllegalStateException("applicationContext reference has expired");
        }

        return applicationContext.getPackageManager().hasSystemFeature(PackageManager.FEATURE_USB_HOST);
    }

/**
 * List the UVC camera devices.
 *
 * Returns:
 *     the list of UVC camera devices
 */
    public List<UsbDevice> getDevices() {
        return usbMonitor.getDeviceList();
    }

/**
 * Request permission to access the specified UVC camera device.
 *
 * Args:
 *     deviceName: the name of the UVC camera device
 *     resultHandler: the handler to be notified when the device permission request result is available
 *
 * Raises:
 *     IllegalArgumentException: no UVC camera device with the given name is connected
 *     IllegalStateException: a device permission request is already pending
 */
    public void requestDevicePermission(
            final @NonNull String deviceName,
            final @NonNull UvcCameraDevicePermissionRequestResultHandler resultHandler
    ) {
        Log.v(TAG, "requestDevicePermission: deviceName=" + deviceName + ", resultHandler=" + resultHandler);

        final var device = findDeviceByName(deviceName);
        if (device == null) {
            throw new IllegalArgumentException("Device not found: " + deviceName);
        }

        synchronized (pendingDevicePermissionRequestLock) {
            if (pendingDevicePermissionRequestResultHandler != null) {
                throw new IllegalStateException("A device permission request is already pending");
            }

            pendingDevicePermissionRequestResultHandler = resultHandler;
            pendingDevicePermissionRequestDeviceName = deviceName;
        }

        Log.d(TAG, "requestDevicePermission: device=" + device);
        usbMonitor.requestPermission(device);
    }

/**
 * Fulfill the pending device permission request.
 *
 * Args:
 *     usbDevice: the USB device
 */
/* package-private */ void fulfillDevicePermissionRequest(final UsbDevice usbDevice) {
        synchronized (UvcCameraPlatform.this.pendingDevicePermissionRequestLock) {
            if (UvcCameraPlatform.this.pendingDevicePermissionRequestResultHandler == null) {
                Log.w(TAG, "No pending device permission request");
                return;
            }

            if (!UvcCameraPlatform.this.pendingDevicePermissionRequestDeviceName.equals(usbDevice.getDeviceName())) {
                Log.w(TAG, "Pending device permission request device name mismatch");
                return;
            }

            pendingDevicePermissionRequestResultHandler.onResult(true);
            pendingDevicePermissionRequestResultHandler = null;
            pendingDevicePermissionRequestDeviceName = null;
        }
    }

/**
 * Reject the pending device permission request.
 *
 * Args:
 *     usbDevice: the USB device
 */
/* package-private */ void rejectDevicePermissionRequest(final UsbDevice usbDevice) {
        synchronized (pendingDevicePermissionRequestLock) {
            if (pendingDevicePermissionRequestResultHandler == null) {
                Log.w(TAG, "No pending device permission request");
                return;
            }

            if (!pendingDevicePermissionRequestDeviceName.equals(usbDevice.getDeviceName())) {
                Log.w(TAG, "Pending device permission request device name mismatch");
                return;
            }

            pendingDevicePermissionRequestResultHandler.onResult(false);
            pendingDevicePermissionRequestResultHandler = null;
            pendingDevicePermissionRequestDeviceName = null;
        }
    }

/**
 * Open the specified UVC camera device.
 *
 * Args:
 *     deviceName: the name of the UVC camera device
 *     desiredFrameArea: the desired frame area
 *
 * Returns:
 *     camera ID
 *
 * Raises:
 *     IllegalArgumentException: no UVC camera device with the given name is connected
 *     IllegalStateException: the binary messenger reference has expired, or the camera open, size
 *         query, callback setup, frame format selection, or preview start fails
 */
    public int openCamera(final @NonNull String deviceName, final int desiredFrameArea) {
        Log.v(TAG, "openCamera: deviceName=" + deviceName + ", desiredFrameArea=" + desiredFrameArea);

        final var device = findDeviceByName(deviceName);
        if (device == null) {
            throw new IllegalArgumentException("Device not found: " + deviceName);
        }

        final var binaryMessenger = this.binaryMessenger.get();
        if (binaryMessenger == null) {
            throw new IllegalStateException("binaryMessenger reference has expired");
        }

// NOTE: The device is already connected, this should just retrieve the device control block
        final var deviceCtrlBlock = usbMonitor.openDevice(device);

        final var camera = new UVCCamera();
        final var cameraId = deviceCtrlBlock.getConnection().getFileDescriptor();

        Log.d(TAG, "openCamera: opening camera");
        try {
            camera.open(deviceCtrlBlock);
        } catch (final Exception e) {
            camera.destroy();
            throw new IllegalStateException("Failed to open camera", e);
        }
        Log.d(TAG, "openCamera: camera opened");

        Log.d(TAG, "openCamera: looking for matching frame size");
        final List<Size> supportedSizes;
        try {
            supportedSizes = camera.getSupportedSizeList();
        } catch (final Exception e) {
            camera.close();
            camera.destroy();
            throw new IllegalStateException("Failed to get supported sizes", e);
        }
        final var supportedSizesWithAreaDelta = new ArrayList<Pair<Size, Integer>>(supportedSizes.size());
        for (final var size : supportedSizes) {
            final var areaDelta = size.width * size.height - desiredFrameArea;
            supportedSizesWithAreaDelta.add(new Pair<>(size, areaDelta));
        }
        Collections.sort(supportedSizesWithAreaDelta, (l, r) -> Integer.compare(r.second, l.second));
        final var desiredFrameSize = supportedSizesWithAreaDelta.get(0).first;
        Log.d(TAG, "openCamera: best size found: " + desiredFrameSize);

// Set the error callback
        Log.d(TAG, "openCamera: setting error callback");
        final var errorCallback = new UvcCameraErrorCallback(this, cameraId);

// Set the status callback
        Log.d(TAG, "openCamera: setting status callback");
        final var statusCallback = new UvcCameraStatusCallback(this, cameraId);
        try {
            camera.setStatusCallback(statusCallback);
        } catch (final Exception e) {
            camera.close();
            camera.destroy();
            throw new IllegalStateException("Failed to set status callback", e);
        }
        Log.d(TAG, "openCamera: status callback set");

// Set the button callback
        Log.d(TAG, "openCamera: setting button callback");
        final var buttonCallback = new UvcCameraButtonCallback(this, cameraId);
        try {
            camera.setButtonCallback(buttonCallback);
        } catch (final Exception e) {
            camera.close();
            camera.destroy();
            throw new IllegalStateException("Failed to set button callback", e);
        }

// Set the preview size and the frame format
        Log.d(TAG, "openCamera: setting preview size and frame format");
        Integer frameFormat = null;
        for (final var desiredFrameFormat : List.of(UVCCamera.FRAME_FORMAT_MJPEG, UVCCamera.FRAME_FORMAT_YUYV)) {
            try {
                camera.setPreviewSize(
                        desiredFrameSize.width,
                        desiredFrameSize.height,
                        desiredFrameFormat
                );
                frameFormat = desiredFrameFormat;
                break;
            } catch (final IllegalArgumentException e) {
                Log.w(TAG, "Unsupported frame format: " + desiredFrameFormat);
            }
        }
        if (frameFormat == null) {
            camera.close();
            camera.destroy();
            throw new IllegalStateException("No supported frame format found");
        }
        Log.d(TAG, "openCamera: preview size and frame format set: frameFormat=" + frameFormat);

// Set the preview display surface and start the preview
        Log.d(TAG, "openCamera: setting preview surface and starting preview");
        final var cameraSurfaceProducer = textureRegistry.createSurfaceProducer();
        cameraSurfaceProducer.setSize(desiredFrameSize.width, desiredFrameSize.height);
        cameraSurfaceProducer.setCallback(errorCallback.textureRegistrySurfaceProducerCallback);
        final var cameraSurface = cameraSurfaceProducer.getSurface();
        try {
            camera.setPreviewDisplay(cameraSurface);
            camera.startPreview();
        } catch (final Exception e) {
            camera.close();
            camera.destroy();
            cameraSurfaceProducer.release();
            throw new IllegalStateException("Failed to start preview", e);
        }

// Create the error event channel
        final var errorEventChannel = new EventChannel(
                binaryMessenger,
                "uvccamera/camera@" + cameraId + "/error_events"
        );
        final var errorEventStreamHandler = new UvcCameraErrorEventStreamHandler();
        errorEventChannel.setStreamHandler(errorEventStreamHandler);

// Create the status event channel
        final var statusEventChannel = new EventChannel(
                binaryMessenger,
                "uvccamera/camera@" + cameraId + "/status_events"
        );
        final var statusEventStreamHandler = new UvcCameraStatusEventStreamHandler();
        statusEventChannel.setStreamHandler(statusEventStreamHandler);

// Create the button event channel
        final var buttonEventChannel = new EventChannel(
                binaryMessenger,
                "uvccamera/camera@" + cameraId + "/button_events"
        );
        final var buttonEventStreamHandler = new UvcCameraButtonEventStreamHandler();
        buttonEventChannel.setStreamHandler(buttonEventStreamHandler);

        final var mediaRecorder = new MediaRecorder();

        camerasResources.put(cameraId, new UvcCameraResources(
                cameraId,
                cameraSurfaceProducer,
                cameraSurface,
                camera,
                errorEventChannel,
                errorEventStreamHandler,
                errorCallback,
                statusEventChannel,
                statusEventStreamHandler,
                statusCallback,
                buttonEventChannel,
                buttonEventStreamHandler,
                buttonCallback,
                mediaRecorder
        ));

        return cameraId;
    }

/**
 * Close the specified camera.
 *
 * Args:
 *     cameraId: the camera ID
 *
 * Raises:
 *     IllegalArgumentException: no camera resources are registered for the given camera ID
 */
    public void closeCamera(final int cameraId) {
        Log.v(TAG, "closeCamera: cameraId=" + cameraId);

        final var cameraResources = camerasResources.remove(cameraId);
        if (cameraResources == null) {
            throw new IllegalArgumentException("Camera resources not found: " + cameraId);
        }

        cameraResources.buttonEventChannel().setStreamHandler(null);
        cameraResources.statusEventChannel().setStreamHandler(null);
        cameraResources.errorEventChannel().setStreamHandler(null);

        Log.d(TAG, "closeCamera: releasing media recorder");
        try {
            final var mediaRecorder = cameraResources.mediaRecorder();
            mediaRecorder.reset();
            mediaRecorder.release();
            Log.d(TAG, "closeCamera: media recorder released");
        } catch (final Exception e) {
            Log.w(TAG, "closeCamera: failed to release media recorder", e);
        }

        Log.d(TAG, "closeCamera: stopping preview");
        try {
            cameraResources.camera().stopPreview();
            Log.d(TAG, "closeCamera: preview stopped");
        } catch (final Exception e) {
            Log.w(TAG, "closeCamera: failed to stop preview", e);
        }

        Log.d(TAG, "closeCamera: unsetting button callback");
        try {
            cameraResources.camera().setButtonCallback(null);
            Log.d(TAG, "closeCamera: button callback unset");
        } catch (final Exception e) {
            Log.w(TAG, "closeCamera: failed to unset button callback", e);
        }

        Log.d(TAG, "closeCamera: unsetting status callback");
        try {
            cameraResources.camera().setStatusCallback(null);
            Log.d(TAG, "closeCamera: status callback unset");
        } catch (final Exception e) {
            Log.w(TAG, "closeCamera: failed to unset status callback", e);
        }

        Log.d(TAG, "closeCamera: closing camera");
        try {
            cameraResources.camera().close();
            Log.d(TAG, "closeCamera: camera closed");
        } catch (final Exception e) {
            Log.w(TAG, "closeCamera: failed to close camera", e);
        }

        Log.d(TAG, "closeCamera: destroying camera");
        try {
            cameraResources.camera().destroy();
            Log.d(TAG, "closeCamera: camera destroyed");
        } catch (final Exception e) {
            Log.w(TAG, "closeCamera: failed to destroy camera", e);
        }

        Log.d(TAG, "closeCamera: unsetting camera surface producer callback");
        try {
            cameraResources.surfaceSurfaceProducer().setCallback(null);
            Log.d(TAG, "closeCamera: camera surface producer callback unset");
        } catch (final Exception e) {
            Log.w(TAG, "closeCamera: failed to unset camera surface producer callback", e);
        }

        Log.d(TAG, "closeCamera: releasing camera surface");
        try {
            cameraResources.surface().release();
            Log.d(TAG, "closeCamera: camera surface released");
        } catch (final Exception e) {
            Log.w(TAG, "closeCamera: failed to release camera surface", e);
        }

        Log.d(TAG, "closeCamera: releasing camera surface producer");
        try {
            cameraResources.surfaceSurfaceProducer().release();
            Log.d(TAG, "closeCamera: camera surface producer released");
        } catch (final Exception e) {
            Log.w(TAG, "closeCamera: failed to release camera surface producer", e);
        }
    }

/**
 * Get the camera texture ID.
 *
 * Args:
 *     cameraId: the camera ID
 *
 * Returns:
 *     the camera texture ID
 */
    public long getCameraTextureId(final int cameraId) {
        Log.v(TAG, "getCameraTextureId"
                + ": cameraId=" + cameraId
        );

        final var cameraResources = camerasResources.get(cameraId);
        if (cameraResources == null) {
            throw new IllegalArgumentException("Camera resources not found: " + cameraId);
        }

        return cameraResources.surfaceSurfaceProducer().id();
    }

/**
 * Cast the camera error event.
 *
 * Args:
 *     cameraId: the camera ID
 *     type: the error type
 *     reason: the error reason
 */
/* package-private */ void castCameraErrorEvent(final int cameraId, final String type, final String reason) {
        Log.v(TAG, "castCameraErrorEvent"
                + ": cameraId=" + cameraId
                + ", type=" + type
                + ", reason=" + reason
        );

        final var cameraResources = camerasResources.get(cameraId);
        if (cameraResources == null) {
            throw new IllegalArgumentException("Camera resources not found: " + cameraId);
        }

        final var eventSink = cameraResources.errorEventStreamHandler().getEventSink();
        if (eventSink == null) {
            Log.w(TAG, "castCameraErrorEvent: event sink not found");
            return;
        }

        final var eventMap = Map.of(
                "cameraId", cameraId,
                "error", Map.of(
                        "type", type,
                        "reason", reason
                )
        );

        mainLooperHandler.post(
                () -> eventSink.success(eventMap)
        );
    }

/**
 * Attach to the camera error callback.
 *
 * Args:
 *     cameraId: the camera ID
 */
    public void attachToCameraErrorCallback(final int cameraId) {
        Log.v(TAG, "attachToCameraErrorCallback: cameraId=" + cameraId);

        final var cameraResources = camerasResources.get(cameraId);
        if (cameraResources == null) {
            throw new IllegalArgumentException("Camera resources not found: " + cameraId);
        }

        cameraResources.errorCallback().enableEventsCasting();
    }

/**
 * Detach from the camera error callback.
 *
 * Args:
 *     cameraId: the camera ID
 */
    public void detachFromCameraErrorCallback(final int cameraId) {
        Log.v(TAG, "detachFromCameraErrorCallback: cameraId=" + cameraId);

        final var cameraResources = camerasResources.get(cameraId);
        if (cameraResources == null) {
            throw new IllegalArgumentException("Camera resources not found: " + cameraId);
        }

        cameraResources.errorCallback().disableEventsCasting();
    }

/**
 * Cast the camera status event.
 *
 * Args:
 *     cameraId: the camera ID
 *     statusClass: the status class
 *     event: the event
 *     selector: the selector
 *     statusAttribute: the status attribute
 *     data: the data
 */
/* package-private */ void castCameraStatusEvent(
            final int cameraId,
            int statusClass,
            int event,
            int selector,
            int statusAttribute,
            ByteBuffer data
    ) {
        Log.v(TAG, "castCameraStatusEvent"
                + ": cameraId=" + cameraId
                + ", statusClass=" + statusClass
                + ", event=" + event
                + ", selector=" + selector
                + ", statusAttribute=" + statusAttribute
                + ", data=" + data
        );

        final var cameraResources = camerasResources.get(cameraId);
        if (cameraResources == null) {
            throw new IllegalArgumentException("Camera resources not found: " + cameraId);
        }

        final var eventSink = cameraResources.statusEventStreamHandler().getEventSink();
        if (eventSink == null) {
            Log.w(TAG, "castCameraStatusEvent: event sink not found");
            return;
        }

        final var statusClassEnumName = STATUS_CLASS_LIBUVC_VALUE_TO_ENUM_NAME.get(statusClass);
        if (statusClassEnumName == null) {
            Log.w(TAG, "Unknown status class: " + statusClass);
            return;
        }

        final var statusAttributeEnumName = STATUS_ATTRIBUTE_LIBUVC_VALUE_TO_ENUM_NAME.get(statusAttribute);
        if (statusAttributeEnumName == null) {
            Log.w(TAG, "Unknown status attribute: " + statusAttribute);
            return;
        }

        final var eventMap = Map.of(
                "cameraId", cameraId,
                "payload", Map.of(
                        "statusClass", statusClassEnumName,
                        "event", event,
                        "selector", selector,
                        "statusAttribute", statusAttributeEnumName
                )
        );

        mainLooperHandler.post(
                () -> eventSink.success(eventMap)
        );
    }

/**
 * Attach to the camera status callback.
 *
 * Args:
 *     cameraId: the camera ID
 */
    public void attachToCameraStatusCallback(final int cameraId) {
        Log.v(TAG, "attachToCameraStatusCallback: cameraId=" + cameraId);

        final var cameraResources = camerasResources.get(cameraId);
        if (cameraResources == null) {
            throw new IllegalArgumentException("Camera resources not found: " + cameraId);
        }

        cameraResources.statusCallback().enableEventsCasting();
    }

/**
 * Detach from the camera status callback.
 *
 * Args:
 *     cameraId: the camera ID
 */
    public void detachFromCameraStatusCallback(final int cameraId) {
        Log.v(TAG, "detachFromCameraStatusCallback: cameraId=" + cameraId);

        final var cameraResources = camerasResources.get(cameraId);
        if (cameraResources == null) {
            throw new IllegalArgumentException("Camera resources not found: " + cameraId);
        }

        cameraResources.statusCallback().disableEventsCasting();
    }

/**
 * Cast the camera button event.
 *
 * Args:
 *     cameraId: the camera ID
 *     button: the button
 *     state: the state
 */
/* package-private */ void castCameraButtonEvent(final int cameraId, final int button, final int state) {
        Log.v(TAG, "castCameraButtonEvent"
                + ": cameraId=" + cameraId
                + ", button=" + button
                + ", state=" + state
        );

        final var cameraResources = camerasResources.get(cameraId);
        if (cameraResources == null) {
            throw new IllegalArgumentException("Camera resources not found: " + cameraId);
        }

        final var eventSink = cameraResources.buttonEventStreamHandler().getEventSink();
        if (eventSink == null) {
            Log.w(TAG, "castCameraButtonEvent: event sink not found");
            return;
        }

        final var eventMap = Map.of(
                "cameraId", cameraId,
                "button", button,
                "state", state
        );

        mainLooperHandler.post(
                () -> eventSink.success(eventMap)
        );
    }

/**
 * Attach to the camera button callback.
 *
 * Args:
 *     cameraId: the camera ID
 */
    public void attachToCameraButtonCallback(final int cameraId) {
        Log.v(TAG, "attachToCameraButtonCallback: cameraId=" + cameraId);

        final var cameraResources = camerasResources.get(cameraId);
        if (cameraResources == null) {
            throw new IllegalArgumentException("Camera resources not found: " + cameraId);
        }

        cameraResources.buttonCallback().enableEventsCasting();
    }

/**
 * Detach from the camera button callback.
 *
 * Args:
 *     cameraId: the camera ID
 */
    public void detachFromCameraButtonCallback(final int cameraId) {
        Log.v(TAG, "detachFromCameraButtonCallback: cameraId=" + cameraId);

        final var cameraResources = camerasResources.get(cameraId);
        if (cameraResources == null) {
            throw new IllegalArgumentException("Camera resources not found: " + cameraId);
        }

        cameraResources.buttonCallback().disableEventsCasting();
    }

/**
 * Get the supported sizes for the specified camera.
 *
 * Args:
 *     cameraId: the camera ID
 *
 * Returns:
 *     the supported sizes
 */
    public List<Size> getSupportedSizes(final int cameraId) {
        Log.v(TAG, "getSupportedSizes: cameraId=" + cameraId);

        final var cameraResources = camerasResources.get(cameraId);
        if (cameraResources == null) {
            throw new IllegalArgumentException("Camera resources not found: " + cameraId);
        }

        return UVCCamera.getSupportedSize(-1, cameraResources.camera().getSupportedSize());
    }

/**
 * Get the preview size for the specified camera.
 *
 * Args:
 *     cameraId: the camera ID
 *
 * Returns:
 *     the preview size
 */
    public Size getPreviewSize(final int cameraId) {
        Log.v(TAG, "getPreviewSize: cameraId=" + cameraId);

        final var cameraResources = camerasResources.get(cameraId);
        if (cameraResources == null) {
            throw new IllegalArgumentException("Camera resources not found: " + cameraId);
        }

        return cameraResources.camera().getPreviewSize();
    }

/**
 * Set the preview size for the specified camera.
 *
 * Args:
 *     cameraId: the camera ID
 *     frameWidth: the frame width
 *     frameHeight: the frame height
 *     frameFormat: the frame format
 */
    public void setPreviewSize(
            final int cameraId,
            final int frameWidth,
            final int frameHeight,
            final int frameFormat
    ) {
        Log.v(TAG, "setPreviewSize"
                + ": cameraId=" + cameraId
                + ", frameWidth=" + frameWidth
                + ", frameHeight=" + frameHeight
                + ", frameFormat=" + frameFormat
        );

        final var cameraResources = camerasResources.get(cameraId);
        if (cameraResources == null) {
            throw new IllegalArgumentException("Camera resources not found: " + cameraId);
        }

        cameraResources.camera().setPreviewSize(
                frameWidth,
                frameHeight,
                frameFormat == 4 ? UVCCamera.FRAME_FORMAT_YUYV : UVCCamera.FRAME_FORMAT_MJPEG
        );
    }

/**
 * Take a picture for the specified camera.
 *
 * Args:
 *     cameraId: the camera ID
 *     resultHandler: the handler to be notified when the picture is taken
 *
 * Raises:
 *     IllegalStateException: the application context reference has expired, or the picture file
 *         cannot be created
 */
    public void takePicture(final int cameraId, UvcCameraTakePictureResultHandler resultHandler) {
        Log.v(TAG, "takePicture"
                + ": cameraId=" + cameraId
        );

        final var cameraResources = camerasResources.get(cameraId);
        if (cameraResources == null) {
            throw new IllegalArgumentException("Camera resources not found: " + cameraId);
        }

        final var applicationContext = this.applicationContext.get();
        if (applicationContext == null) {
            throw new IllegalStateException("applicationContext reference has expired");
        }

        final var outputDir = applicationContext.getCacheDir();
        final File outputFile;
        try {
            outputFile = File.createTempFile("PIC", ".jpg", outputDir);
        } catch (IOException | SecurityException e) {
            throw new IllegalStateException("Failed to create picture file", e);
        }

        cameraResources.camera().setFrameCallback(
                new UvcCameraTakePictureFrameCallback(
                        this,
                        cameraId,
                        outputFile,
                        resultHandler
                ),
                UVCCamera.PIXEL_FORMAT_NV21
        );
    }

/**
 * Handle the taken picture.
 *
 * Args:
 *     cameraId: the camera ID
 *     outputFile: the output file
 *     frame: the frame
 *     resultHandler: the result handler
 */
/* package-private */ void handleTakenPicture(
            final int cameraId,
            final File outputFile,
            final ByteBuffer frame,
            final UvcCameraTakePictureResultHandler resultHandler
    ) {
        Log.v(TAG, "handleTakenPicture"
                + ": cameraId=" + cameraId
                + ", outputFile=" + outputFile
                + ", frame=" + frame
                + ", resultHandler=" + resultHandler
        );

        final var cameraResources = camerasResources.get(cameraId);
        if (cameraResources == null) {
            throw new IllegalArgumentException("Camera resources not found: " + cameraId);
        }

// Create copy of the frame data as the frame buffer is owned by the native side (libuvc)
        final var frameData = new byte[frame.remaining()];
        frame.get(frameData);

// NOTE: The frame callback should've been detached here yet that will cause a deadlock

// Save the taken picture to the file using the worker looper
        mainLooperHandler.post(() -> {
// Detach the frame callback
            cameraResources.camera().setFrameCallback(null, 0);

            try {
                saveTakenPictureToFile(cameraId, outputFile, frameData);
                resultHandler.onResult(outputFile, null);
            } catch(final Exception e) {
                Log.e(TAG, "Failed to save taken picture to file", e);
                resultHandler.onResult(null, e);
            }
        });
    }

/**
 * Save the taken picture to the specified file.
 *
 * Args:
 *     cameraId: the camera ID
 *     outputFile: the output file
 *     frameData: the frame data
 */
    private void saveTakenPictureToFile(final int cameraId, final File outputFile, final byte[] frameData) {
        Log.v(TAG, "saveTakenPictureToFile"
                + ": cameraId=" + cameraId
                + ", outputFile=" + outputFile
                + ", frameData=[... " + frameData.length + " byte(s) ...]"
        );

        final var cameraResources = camerasResources.get(cameraId);
        if (cameraResources == null) {
            throw new IllegalArgumentException("Camera resources not found: " + cameraId);
        }

        final var previewSize = cameraResources.camera().getPreviewSize();
        final var yuvImage = new YuvImage(
                frameData,
                ImageFormat.NV21,
                previewSize.width,
                previewSize.height,
                null
        );

        final FileOutputStream outputFileStream;
        try {
            outputFileStream = new FileOutputStream(outputFile);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to open picture file output stream", e);
        }

        try {
            yuvImage.compressToJpeg(
                    new Rect(0, 0, previewSize.width, previewSize.height),
                    100,
                    outputFileStream
            );
        } catch (Exception e) {
            throw new IllegalStateException("Failed to write picture file", e);
        } finally {
            try {
                outputFileStream.close();
            } catch (IOException e) {
                Log.w(TAG, "Failed to close picture file output stream", e);
            }
        }
    }

/**
 * Start video recording for the specified camera.
 *
 * Args:
 *     cameraId: the camera ID
 *     frameWidth: the frame width
 *     frameHeight: the frame height
 *
 * Returns:
 *     the video recording file
 *
 * Raises:
 *     IllegalStateException: the application context reference has expired, or the recording file
 *         cannot be created, prepared, or started
 */
    public File startVideoRecording(final int cameraId, final int frameWidth, final int frameHeight) {
        Log.v(TAG, "startVideoRecording"
                + ": cameraId=" + cameraId
                + ", frameWidth=" + frameWidth
                + ", frameHeight=" + frameHeight
        );

        final var cameraResources = camerasResources.get(cameraId);
        if (cameraResources == null) {
            throw new IllegalArgumentException("Camera resources not found: " + cameraId);
        }

        final var applicationContext = this.applicationContext.get();
        if (applicationContext == null) {
            throw new IllegalStateException("applicationContext reference has expired");
        }

        final var mediaRecorder = cameraResources.mediaRecorder();
        mediaRecorder.reset();

        final var outputDir = applicationContext.getCacheDir();
        final File outputFile;
        try {
            outputFile = File.createTempFile("REC", ".mp4", outputDir);
        } catch (IOException | SecurityException e) {
            throw new IllegalStateException("Failed to create video recording file", e);
        }

        mediaRecorder.setVideoSource(MediaRecorder.VideoSource.SURFACE);
        mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
        mediaRecorder.setVideoEncoder(MediaRecorder.VideoEncoder.H264);
        mediaRecorder.setVideoSize(frameWidth, frameHeight);
        mediaRecorder.setVideoFrameRate(30);
        mediaRecorder.setOutputFile(outputFile.getAbsolutePath());

        try {
            mediaRecorder.prepare();
        } catch (IOException e) {
            mediaRecorder.reset();

            throw new IllegalStateException("Failed to prepare media recorder", e);
        }

        try {
            final var mediaRecorderSurface = mediaRecorder.getSurface();
            cameraResources.camera().startCapture(mediaRecorderSurface);
            mediaRecorder.start();
        } catch (final Exception e) {
            mediaRecorder.reset();

            throw new IllegalStateException("Failed to start capture", e);
        }

        return outputFile;
    }

/**
 * Stop video recording for the specified camera.
 *
 * Args:
 *     cameraId: the camera ID
 */
    public void stopVideoRecording(final int cameraId) {
        Log.v(TAG, "stopVideoRecording: cameraId=" + cameraId);

        final var cameraResources = camerasResources.get(cameraId);
        if (cameraResources == null) {
            throw new IllegalArgumentException("Camera resources not found: " + cameraId);
        }

        cameraResources.camera().stopCapture();

        final var mediaRecorder = cameraResources.mediaRecorder();
        mediaRecorder.stop();
        mediaRecorder.reset();
    }

/**
 * Find the UVC camera device by name.
 *
 * Args:
 *     deviceName: the name of the UVC camera device
 *
 * Returns:
 *     the UVC camera device, or null if not found
 */
    private UsbDevice findDeviceByName(final @NonNull String deviceName) {
        for (final var device : usbMonitor.getDeviceList()) {
            if (device.getDeviceName().equals(deviceName)) {
                return device;
            }
        }

        return null;
    }

}
