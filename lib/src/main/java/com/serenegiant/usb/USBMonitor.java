/*
 *  UVCCamera
 *  library and sample to access to UVC web camera on non-rooted Android device
 *
 * Copyright (c) 2014-2017 saki t_saki@serenegiant.com
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 *
 *  All files in the folder are under this Apache License, Version 2.0.
 *  Files in the libjpeg-turbo, libusb, libuvc, rapidjson folder
 *  may have a different license, see the respective files.
 */

package com.serenegiant.usb;

import java.io.UnsupportedEncodingException;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import android.annotation.SuppressLint;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbInterface;
import android.hardware.usb.UsbManager;
import android.os.Build;
import android.os.Handler;
import android.text.TextUtils;
import android.util.Log;
import android.util.SparseArray;

import com.serenegiant.utils.BuildCheck;
import com.serenegiant.utils.HandlerThreadHandler;

/**
 * Monitor USB devices and manage permission and connection lifecycle for UVC cameras.
 *
 * Manages the lifecycle of USB device detection, permission requests, and connections.
 * Registers a BroadcastReceiver to listen for device attach/detach events, periodically
 * polls for connected devices, and maintains a map of open UsbControlBlock instances.
 *
 * Lifecycle:
 *     Constructed → Registered (via register()) → Unregistered (via unregister()) → Destroyed (via destroy())
 *     Must call register() after construction to begin monitoring. Call destroy() to release all resources.
 *
 * State Machine:
 *     Constructed → Registered (on register) → Unregistered (on unregister) → Constructed (on register again)
 *     Any state → Destroyed (on destroy, terminal state)
 *
 * Thread Safety:
 *     register/unregister are synchronized. Device callbacks are posted to a worker thread
 *     via mAsyncHandler. hasPermission and updatePermission synchronize on mHasPermissions.
 *
 * Properties:
 *     ACTION_USB_PERMISSION_BASE: Base action string for permission broadcasts.
 *     ACTION_USB_DEVICE_ATTACHED: System action for USB device attachment.
 */
public final class USBMonitor {

	private static final boolean DEBUG = false;	// TODO set false on production
	private static final String TAG = "USBMonitor";

	private static final String ACTION_USB_PERMISSION_BASE = "com.serenegiant.USB_PERMISSION.";
	private final String ACTION_USB_PERMISSION = ACTION_USB_PERMISSION_BASE + hashCode();

	public static final String ACTION_USB_DEVICE_ATTACHED = "android.hardware.usb.action.USB_DEVICE_ATTACHED";

	/**
	 * Maps connected UsbDevice to their UsbControlBlock instances.
	 * Populated when openDevice() is called, cleared on destroy().
	 */
	private final ConcurrentHashMap<UsbDevice, UsbControlBlock> mCtrlBlocks = new ConcurrentHashMap<UsbDevice, UsbControlBlock>();
	private final SparseArray<WeakReference<UsbDevice>> mHasPermissions = new SparseArray<WeakReference<UsbDevice>>();

	private final WeakReference<Context> mWeakContext;
	private final UsbManager mUsbManager;
	private final OnDeviceConnectListener mOnDeviceConnectListener;
	private PendingIntent mPermissionIntent = null;
	private List<DeviceFilter> mDeviceFilters = new ArrayList<DeviceFilter>();

	/**
	 * Handler for posting callback invocations to a worker thread.
	 */
	private final Handler mAsyncHandler;
	private volatile boolean destroyed;
	/**
	 * Receive USB device connection lifecycle callbacks from USBMonitor.
	 *
	 * Implementations receive notifications for device attach, detach, connect, disconnect,
	 * and permission cancellation events. All callbacks are invoked on a worker thread.
	 */
	public interface OnDeviceConnectListener {
		/**
		 * Notify that a USB device has been attached.
		 *
		 * Called when the device appears or is detected by the periodic device check.
		 * Permission may not yet be granted.
		 *
		 * Args:
		 *     device: The UsbDevice that was attached.
		 *
		 * Side Effects:
		 *     None. Implementation should initiate permission request if needed.
		 */
		public void onAttach(UsbDevice device);
		/**
		 * Notify that a USB device has been detached.
		 *
		 * Called after onDisconnect, when the system reports the device is removed.
		 *
		 * Args:
		 *     device: The UsbDevice that was detached.
		 *
		 * Side Effects:
		 *     None. Implementation should release references to the device.
		 */
		public void onDettach(UsbDevice device);
		/**
		 * Notify that a USB device has been opened successfully.
		 *
		 * Called after permission is granted and UsbControlBlock is created or reused.
		 *
		 * Args:
		 *     device: The UsbDevice that was opened.
		 *     ctrlBlock: The UsbControlBlock managing the device connection.
		 *     createNew: True if a new UsbControlBlock was created, false if reused.
		 *
		 * Side Effects:
		 *     None. Implementation typically starts preview or further initialization.
		 */
		public void onConnect(UsbDevice device, UsbControlBlock ctrlBlock, boolean createNew);
		/**
		 * Notify that a USB device has been disconnected.
		 *
		 * Called after the UsbControlBlock is closed, either due to removal or power off.
		 *
		 * Args:
		 *     device: The UsbDevice that was disconnected.
		 *     ctrlBlock: The UsbControlBlock that was closed.
		 *
		 * Side Effects:
		 *     None. Implementation should stop using the control block.
		 */
		public void onDisconnect(UsbDevice device, UsbControlBlock ctrlBlock);
		/**
		 * Notify that permission request was canceled or denied.
		 *
		 * Called when the user denies permission or the request fails.
		 *
		 * Args:
		 *     device: The UsbDevice for which permission was denied or canceled.
		 *
		 * Side Effects:
		 *     None. Implementation should handle the failure.
		 */
		public void onCancel(UsbDevice device);
	}

	/**
	 * Create a USBMonitor instance.
	 *
	 * Initializes UsbManager, async handler, and stores the listener. Monitoring starts
	 * only after register() is called.
	 *
	 * Args:
	 *     context: Application or Activity context used for registering receivers and PendingIntent.
	 *     listener: Callback listener for device events. Must not be null.
	 *
	 * Raises:
	 *     IllegalArgumentException: If listener is null.
	 *
	 * Side Effects:
	 *     Creates a HandlerThread for async callbacks.
	 */
	public USBMonitor(final Context context, final OnDeviceConnectListener listener) {
		if (DEBUG) Log.v(TAG, "USBMonitor:Constructor");
		if (listener == null)
			throw new IllegalArgumentException("OnDeviceConnectListener should not null.");
		mWeakContext = new WeakReference<Context>(context);
		mUsbManager = (UsbManager)context.getSystemService(Context.USB_SERVICE);
		mOnDeviceConnectListener = listener;
		mAsyncHandler = HandlerThreadHandler.createHandler(TAG);
		destroyed = false;
		if (DEBUG) Log.v(TAG, "USBMonitor:mUsbManager=" + mUsbManager);
	}

	/**
	 * Release all resources and stop monitoring.
	 *
	 * Unregisters the broadcast receiver, closes all open UsbControlBlock instances,
	 * and quits the async handler thread. After calling destroy(), the monitor cannot
	 * be reused; create a new instance to resume monitoring.
	 *
	 * Side Effects:
	 *     - Calls unregister() if not already unregistered
	 *     - Closes all UsbControlBlock instances in mCtrlBlocks
	 *     - Clears mCtrlBlocks and mHasPermissions
	 *     - Quits the async handler looper
	 */
	public void destroy() {
		if (DEBUG) Log.i(TAG, "destroy:");
		unregister();
		if (!destroyed) {
			destroyed = true;
			// Close all monitored USB devices
			final Set<UsbDevice> keys = mCtrlBlocks.keySet();
			if (keys != null) {
				UsbControlBlock ctrlBlock;
				try {
					for (final UsbDevice key: keys) {
						ctrlBlock = mCtrlBlocks.remove(key);
						if (ctrlBlock != null) {
							ctrlBlock.close();
						}
					}
				} catch (final Exception e) {
					Log.e(TAG, "destroy:", e);
				}
			}
			mCtrlBlocks.clear();
			try {
				mAsyncHandler.getLooper().quit();
			} catch (final Exception e) {
				Log.e(TAG, "destroy:", e);
			}
		}
	}

	/**
	 * Register BroadcastReceiver to monitor USB device attach/detach events.
	 *
	 * Creates a PendingIntent for permission requests and registers the receiver
	 * with appropriate flags for the Android API level. Starts periodic device
	 * polling via mDeviceCheckRunnable at 1-second intervals.
	 *
	 * Raises:
	 *     IllegalStateException: If monitor has been destroyed.
	 *
	 * Side Effects:
	 *     - Registers BroadcastReceiver for ACTION_USB_PERMISSION and ACTION_USB_DEVICE_DETACHED
	 *     - Starts periodic device check runnable on async handler
	 */
	public synchronized void register() throws IllegalStateException {
		if (destroyed) throw new IllegalStateException("already destroyed");
		if (mPermissionIntent == null) {
			if (DEBUG) Log.i(TAG, "register:");
			final Context context = mWeakContext.get();
			if (context != null) {
				final var permissionIntent = new Intent(ACTION_USB_PERMISSION);
				permissionIntent.setPackage(context.getPackageName());
				if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
					mPermissionIntent = PendingIntent.getBroadcast(context, 0, permissionIntent, PendingIntent.FLAG_MUTABLE);
				} else {
					mPermissionIntent = PendingIntent.getBroadcast(context, 0, permissionIntent, 0);
				}
				final IntentFilter filter = new IntentFilter(ACTION_USB_PERMISSION);
				// ACTION_USB_DEVICE_ATTACHED never comes on some devices so it should not be added here
				filter.addAction(UsbManager.ACTION_USB_DEVICE_DETACHED);

				if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
				// For Android 13 (API level 33) and above, use receiver flags
					context.registerReceiver(mUsbReceiver, filter, Context.RECEIVER_NOT_EXPORTED);
				} else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
				// For Android 12 (API level 31) and above
					context.registerReceiver(mUsbReceiver, filter);
				} else {
				// For older versions of Android
					context.registerReceiver(mUsbReceiver, filter);
				}
			}
			// start connection check
			mDeviceCounts = 0;
			mAsyncHandler.postDelayed(mDeviceCheckRunnable, 1000);
		}
	}

	/**
	 * Unregister BroadcastReceiver and stop periodic device checks.
	 *
	 * Removes the device check runnable from the async handler and unregisters
	 * the broadcast receiver. Safe to call multiple times; no-op if already unregistered.
	 *
	 * Side Effects:
	 *     - Removes mDeviceCheckRunnable from mAsyncHandler
	 *     - Unregisters mUsbReceiver from context
	 */
	public synchronized void unregister() throws IllegalStateException {
	// Remove device check runnable
		mDeviceCounts = 0;
		if (!destroyed) {
			mAsyncHandler.removeCallbacks(mDeviceCheckRunnable);
		}
		if (mPermissionIntent != null) {
		//			if (DEBUG) Log.i(TAG, "unregister:");
			final Context context = mWeakContext.get();
			try {
				if (context != null) {
					context.unregisterReceiver(mUsbReceiver);
				}
			} catch (final Exception e) {
				Log.w(TAG, e);
			}
			mPermissionIntent = null;
		}
	}

	/**
	 * Check if monitor is registered and not destroyed.
	 *
	 * Returns true if mPermissionIntent is non-null and monitor is not destroyed.
	 *
	 * Returns:
	 *     True if registered, false otherwise.
	 */
	public synchronized boolean isRegistered() {
		return !destroyed && (mPermissionIntent != null);
	}

	/**
	 * Set a single device filter, replacing existing filters.
	 *
	 * Args:
	 *     filter: DeviceFilter to apply. Null clears filter list.
	 *
	 * Raises:
	 *     IllegalStateException: If monitor has been destroyed.
	 *
	 * Side Effects:
	 *     Clears and replaces mDeviceFilters list.
	 */
	public void setDeviceFilter(final DeviceFilter filter) throws IllegalStateException {
		if (destroyed) throw new IllegalStateException("already destroyed");
		mDeviceFilters.clear();
		mDeviceFilters.add(filter);
	}

	/**
	 * Add a single device filter to the current filter list.
	 *
	 * Args:
	 *     filter: DeviceFilter to add. Null values are added as-is.
	 *
	 * Raises:
	 *     IllegalStateException: If monitor has been destroyed.
	 */
	public void addDeviceFilter(final DeviceFilter filter) throws IllegalStateException {
		if (destroyed) throw new IllegalStateException("already destroyed");
		mDeviceFilters.add(filter);
	}

	/**
	 * Remove a single device filter from the current filter list.
	 *
	 * Args:
	 *     filter: DeviceFilter to remove.
	 *
	 * Raises:
	 *     IllegalStateException: If monitor has been destroyed.
	 */
	public void removeDeviceFilter(final DeviceFilter filter) throws IllegalStateException {
		if (destroyed) throw new IllegalStateException("already destroyed");
		mDeviceFilters.remove(filter);
	}

	/**
	 * Replace the current filter list with the provided filters.
	 *
	 * Args:
	 *     filters: List of DeviceFilter to set. Replaces existing filters.
	 *
	 * Raises:
	 *     IllegalStateException: If monitor has been destroyed.
	 */
	public void setDeviceFilter(final List<DeviceFilter> filters) throws IllegalStateException {
		if (destroyed) throw new IllegalStateException("already destroyed");
		mDeviceFilters.clear();
		mDeviceFilters.addAll(filters);
	}

	/**
	 * Add multiple device filters to the current filter list.
	 *
	 * Args:
	 *     filters: List of DeviceFilter to add.
	 *
	 * Raises:
	 *     IllegalStateException: If monitor has been destroyed.
	 */
	public void addDeviceFilter(final List<DeviceFilter> filters) throws IllegalStateException {
		if (destroyed) throw new IllegalStateException("already destroyed");
		mDeviceFilters.addAll(filters);
	}

	/**
	 * Remove multiple device filters from the current filter list.
	 *
	 * Args:
	 *     filters: List of DeviceFilter to remove.
	 *
	 * Raises:
	 *     IllegalStateException: If monitor has been destroyed.
	 */
	public void removeDeviceFilter(final List<DeviceFilter> filters) throws IllegalStateException {
		if (destroyed) throw new IllegalStateException("already destroyed");
		mDeviceFilters.removeAll(filters);
	}

	/**
	 * Return the number of connected USB devices matching the current filter.
	 *
	 * Returns:
	 *     Count of devices matching filters, zero if none.
	 *
	 * Raises:
	 *     IllegalStateException: If monitor has been destroyed.
	 */
	public int getDeviceCount() throws IllegalStateException {
		if (destroyed) throw new IllegalStateException("already destroyed");
		return getDeviceList().size();
	}

	/**
	 * Return list of USB devices matching current filters.
	 *
	 * Returns:
	 *     List of UsbDevice, empty if no match.
	 *
	 * Raises:
	 *     IllegalStateException: If monitor has been destroyed.
	 */
	public List<UsbDevice> getDeviceList() throws IllegalStateException {
		if (destroyed) throw new IllegalStateException("already destroyed");
		return getDeviceList(mDeviceFilters);
	}

	/**
	 * Return list of USB devices matching the provided filters.
	 *
	 * Iterates all connected devices and applies each filter. If a filter matches
	 * and is not an exclude filter, the device is added to results.
	 *
	 * Args:
	 *     filters: List of DeviceFilter to apply. Null or empty returns all devices.
	 *
	 * Returns:
	 *     List of matching UsbDevice, empty if none match.
	 *
	 * Raises:
	 *     IllegalStateException: If monitor has been destroyed.
	 */
	public List<UsbDevice> getDeviceList(final List<DeviceFilter> filters) throws IllegalStateException {
		if (destroyed) throw new IllegalStateException("already destroyed");
		final HashMap<String, UsbDevice> deviceList = mUsbManager.getDeviceList();
		final List<UsbDevice> result = new ArrayList<UsbDevice>();
		if (deviceList != null) {
			if ((filters == null) || filters.isEmpty()) {
				result.addAll(deviceList.values());
			} else {
				for (final UsbDevice device: deviceList.values() ) {
					for (final DeviceFilter filter: filters) {
						if ((filter != null) && filter.matches(device)) {
						// when filter matches
							if (!filter.isExclude) {
								result.add(device);
							}
							break;
						}
					}
				}
			}
		}
		return result;
	}

	/**
	 * Return list of USB devices matching the provided filter.
	 *
	 * Args:
	 *     filter: Single DeviceFilter to apply. Null returns all non-excluded devices.
	 *
	 * Returns:
	 *     List of matching UsbDevice, empty if none match.
	 *
	 * Raises:
	 *     IllegalStateException: If monitor has been destroyed.
	 */
	public List<UsbDevice> getDeviceList(final DeviceFilter filter) throws IllegalStateException {
		if (destroyed) throw new IllegalStateException("already destroyed");
		final HashMap<String, UsbDevice> deviceList = mUsbManager.getDeviceList();
		final List<UsbDevice> result = new ArrayList<UsbDevice>();
		if (deviceList != null) {
			for (final UsbDevice device: deviceList.values() ) {
				if ((filter == null) || (filter.matches(device) && !filter.isExclude)) {
					result.add(device);
				}
			}
		}
		return result;
	}

	/**
	 * Return iterator over all connected USB devices, without filtering.
	 *
	 * Returns:
	 *     Iterator of UsbDevice, or null if no devices are connected.
	 *
	 * Raises:
	 *     IllegalStateException: If monitor has been destroyed.
	 */
	public Iterator<UsbDevice> getDevices() throws IllegalStateException {
		if (destroyed) throw new IllegalStateException("already destroyed");
		Iterator<UsbDevice> iterator = null;
		final HashMap<String, UsbDevice> list = mUsbManager.getDeviceList();
		if (list != null)
			iterator = list.values().iterator();
		return iterator;
	}

	/**
	 * Log connected USB devices and their interfaces to LogCat.
	 *
	 * Useful for debugging device detection issues. Logs device key, device info,
	 * and interface descriptions for each connected device.
	 */
	public final void dumpDevices() {
		final HashMap<String, UsbDevice> list = mUsbManager.getDeviceList();
		if (list != null) {
			final Set<String> keys = list.keySet();
			if (keys != null && keys.size() > 0) {
				final StringBuilder sb = new StringBuilder();
				for (final String key: keys) {
					final UsbDevice device = list.get(key);
					final int num_interface = device != null ? device.getInterfaceCount() : 0;
					sb.setLength(0);
					for (int i = 0; i < num_interface; i++) {
						sb.append(String.format(Locale.US, "interface%d:%s", i, device.getInterface(i).toString()));
					}
					Log.i(TAG, "key=" + key + ":" + device + ":" + sb.toString());
				}
			} else {
				Log.i(TAG, "no device");
			}
		} else {
			Log.i(TAG, "no device");
		}
	}

	/**
	 * Check if the specified USB device has been granted permission.
	 *
	 * Updates internal permission tracking state via updatePermission().
	 *
	 * Args:
	 *     device: UsbDevice to check. Null returns false.
	 *
	 * Returns:
	 *     True if permission is granted, false otherwise.
	 *
	 * Raises:
	 *     IllegalStateException: If monitor has been destroyed.
	 */
	public final boolean hasPermission(final UsbDevice device) throws IllegalStateException {
		if (destroyed) throw new IllegalStateException("already destroyed");
		return updatePermission(device, device != null && mUsbManager.hasPermission(device));
	}

	/**
	 * Update internal permission tracking for a device.
	 *
	 * Maintains mHasPermissions sparse array by adding or removing the device key
	 * based on the hasPermission flag. Synchronized on mHasPermissions.
	 *
	 * Args:
	 *     device: UsbDevice to track.
	 *     hasPermission: True if permission granted, false if revoked.
	 *
	 * Returns:
	 *     The hasPermission flag passed in.
	 */
	private boolean updatePermission(final UsbDevice device, final boolean hasPermission) {
		final int deviceKey = getDeviceKey(device, true);
		synchronized (mHasPermissions) {
			if (hasPermission) {
				if (mHasPermissions.get(deviceKey) == null) {
					mHasPermissions.put(deviceKey, new WeakReference<UsbDevice>(device));
				}
			} else {
				mHasPermissions.remove(deviceKey);
			}
		}
		return hasPermission;
	}

	/**
	 * Request user permission to access a USB device.
	 *
	 * If the app already has permission, immediately calls processConnect().
	 * Otherwise, triggers the system permission dialog via UsbManager.requestPermission().
	 * On Galaxy devices with Android 5.1.x, catches spurious MDM_APP_MGMT exceptions.
	 *
	 * Args:
	 *     device: UsbDevice to request permission for. Null returns true after processCancel().
	 *
	 * Returns:
	 *     True if permission was already granted or an error occurred, false if dialog is pending.
	 *
	 * Code Paths:
	 *     1. If not registered → processCancel(device), return true
	 *     2. If device is null → processCancel(null), return true
	 *     3. If permission already granted → processConnect(device), return false
	 *     4. Otherwise → request system permission dialog, return false
	 */
	public synchronized boolean requestPermission(final UsbDevice device) {
	//		if (DEBUG) Log.v(TAG, "requestPermission:device=" + device);
		boolean result = false;
		if (isRegistered()) {
			if (device != null) {
				if (mUsbManager.hasPermission(device)) {
				// call onConnect if app already has permission
					processConnect(device);
				} else {
					try {
					// Request permission if not already granted
						mUsbManager.requestPermission(device, mPermissionIntent);
					} catch (final Exception e) {
					// Samsung Galaxy devices on Android 5.1.x throw spurious MDM_APP_MGMT exceptions
						Log.w(TAG, e);
						processCancel(device);
						result = true;
					}
				}
			} else {
				processCancel(device);
				result = true;
			}
		} else {
			processCancel(device);
			result = true;
		}
		return result;
	}

	/**
	 * Open a USB device and return its control block.
	 *
	 * Reuses an existing UsbControlBlock if one is already open for the device.
	 * Creates a new UsbControlBlock if not already open, which internally calls openDevice().
	 *
	 * Args:
	 *     device: UsbDevice to open. Must have permission.
	 *
	 * Returns:
	 *     UsbControlBlock for the device.
	 *
	 * Raises:
	 *     SecurityException: If permission has not been granted for the device.
	 */
	public UsbControlBlock openDevice(final UsbDevice device) throws SecurityException {
		if (hasPermission(device)) {
			UsbControlBlock result = mCtrlBlocks.get(device);
			if (result == null) {
				result = new UsbControlBlock(USBMonitor.this, device);    // Internally opens the USB device connection
				mCtrlBlocks.put(device, result);
			}
			return result;
		} else {
			throw new SecurityException("has no permission");
		}
	}

	/**
	 * BroadcastReceiver that handles USB permission grants, device attach, and device detach events.
	 *
	 * On ACTION_USB_PERMISSION: calls processConnect() or processCancel() based on grant result.
	 * On ACTION_USB_DEVICE_ATTACHED: updates permission state and calls processAttach().
	 * On ACTION_USB_DEVICE_DETACHED: removes UsbControlBlock, closes it, calls processDetach().
	 */
	private final BroadcastReceiver mUsbReceiver = new BroadcastReceiver() {

		/**
		 * Handle USB permission grant and device attach/detach broadcasts.
		 *
		 * Dispatches permission results to processConnect or processCancel, attach
		 * events to processAttach, and detach events to control-block cleanup plus
		 * processDettach. No-op when the monitor is destroyed.
		 */
		@Override
		public void onReceive(final Context context, final Intent intent) {
			if (destroyed) return;
			final String action = intent.getAction();
			if (ACTION_USB_PERMISSION.equals(action)) {
			// when received the result of requesting USB permission
				synchronized (USBMonitor.this) {
					final UsbDevice device = intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
					if (intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)) {
						if (device != null) {
						// get permission, call onConnect
							processConnect(device);
						}
					} else {
					// failed to get permission
						processCancel(device);
					}
				}
			} else if (UsbManager.ACTION_USB_DEVICE_ATTACHED.equals(action)) {
				final UsbDevice device = intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
				updatePermission(device, hasPermission(device));
				processAttach(device);
			} else if (UsbManager.ACTION_USB_DEVICE_DETACHED.equals(action)) {
			// when device removed
				final UsbDevice device = intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
				if (device != null) {
					UsbControlBlock ctrlBlock = mCtrlBlocks.remove(device);
					if (ctrlBlock != null) {
					// cleanup
						ctrlBlock.close();
					}
					mDeviceCounts = 0;
					processDettach(device);
				}
			}
		}
	};

	/**
	 * Count of connected and detected devices from last poll.
	 */
	private volatile int mDeviceCounts = 0;
	/**
	 * Runnable that periodically polls for connected devices every 2 seconds.
	 *
	 * Compares current device count and permission count against previous values.
	 * If either increased, posts onAttach callbacks for all devices to the listener.
	 */
	private final Runnable mDeviceCheckRunnable = new Runnable() {
		/**
		 * Poll connected devices and refresh permission state.
		 *
		 * If the device count or permission count increased since the last poll,
		 * posts onAttach for every connected device. Reschedules itself after 2 seconds.
		 */
		@Override
		public void run() {
			if (destroyed) return;
			final List<UsbDevice> devices = getDeviceList();
			final int n = devices.size();
			final int hasPermissionCounts;
			final int m;
			synchronized (mHasPermissions) {
				hasPermissionCounts = mHasPermissions.size();
				mHasPermissions.clear();
				for (final UsbDevice device: devices) {
					hasPermission(device);
				}
				m = mHasPermissions.size();
			}
			if ((n > mDeviceCounts) || (m > hasPermissionCounts)) {
				mDeviceCounts = n;
				if (mOnDeviceConnectListener != null) {
					for (int i = 0; i < n; i++) {
						final UsbDevice device = devices.get(i);
						mAsyncHandler.post(new Runnable() {
							/**
							 * Notify the listener that a newly detected device has been attached.
							 */
							@Override
							public void run() {
								mOnDeviceConnectListener.onAttach(device);
							}
						});
					}
				}
			}
			mAsyncHandler.postDelayed(this, 2000);	// confirm every 2 seconds
		}
	};

	/**
	 * Handle successful permission grant by creating or reusing a UsbControlBlock.
	 *
	 * If no control block exists for the device, creates a new one and stores it in mCtrlBlocks.
	 * Posts onConnect callback to the listener with the control block and creation flag.
	 *
	 * Args:
	 *     device: UsbDevice that was granted permission.
	 */
	private final void processConnect(final UsbDevice device) {
		if (destroyed) return;
		updatePermission(device, true);
		mAsyncHandler.post(new Runnable() {
			/**
			 * Create or reuse the device's control block and notify onConnect.
			 *
			 * Reuses an existing UsbControlBlock from mCtrlBlocks or creates and stores
			 * a new one, then invokes the listener's onConnect with the creation flag.
			 */
			@Override
			public void run() {
				if (DEBUG) Log.v(TAG, "processConnect:device=" + device);
				UsbControlBlock ctrlBlock;
				final boolean createNew;
				ctrlBlock = mCtrlBlocks.get(device);
				if (ctrlBlock == null) {
					ctrlBlock = new UsbControlBlock(USBMonitor.this, device);
					mCtrlBlocks.put(device, ctrlBlock);
					createNew = true;
				} else {
					createNew = false;
				}
				if (mOnDeviceConnectListener != null) {
					mOnDeviceConnectListener.onConnect(device, ctrlBlock, createNew);
				}
			}
		});
	}

	private final void processCancel(final UsbDevice device) {
		if (destroyed) return;
		if (DEBUG) Log.v(TAG, "processCancel:");
		updatePermission(device, false);
		if (mOnDeviceConnectListener != null) {
			mAsyncHandler.post(new Runnable() {
				/**
				 * Notify the listener that the permission request was canceled or denied.
				 */
				@Override
				public void run() {
					mOnDeviceConnectListener.onCancel(device);
				}
			});
		}
	}

	private final void processAttach(final UsbDevice device) {
		if (destroyed) return;
		if (DEBUG) Log.v(TAG, "processAttach:");
		if (mOnDeviceConnectListener != null) {
			mAsyncHandler.post(new Runnable() {
				/**
				 * Notify the listener that a device has been attached.
				 */
				@Override
				public void run() {
					mOnDeviceConnectListener.onAttach(device);
				}
			});
		}
	}

	private final void processDettach(final UsbDevice device) {
		if (destroyed) return;
		if (DEBUG) Log.v(TAG, "processDettach:");
		if (mOnDeviceConnectListener != null) {
			mAsyncHandler.post(new Runnable() {
				/**
				 * Notify the listener that a device has been detached.
				 */
				@Override
				public void run() {
					mOnDeviceConnectListener.onDettach(device);
				}
			});
		}
	}

	/**
	 * Generate a unique key string for a USB device for persistent settings storage.
	 *
	 * Delegates to getDeviceKeyName(device, null, false).
	 *
	 * Args:
	 *     device: UsbDevice to generate key for. Null returns empty string.
	 *
	 * Returns:
	 *     Unique key string composed of vendor ID, product ID, class, subclass, and protocol.
	 */
	public static final String getDeviceKeyName(final UsbDevice device) {
		return getDeviceKeyName(device, null, false);
	}

	/**
	 * Generate a unique key string for a USB device with optional new API fields.
	 *
	 * Delegates to getDeviceKeyName(device, null, useNewAPI).
	 *
	 * Args:
	 *     device: UsbDevice to generate key for. Null returns empty string.
	 *     useNewAPI: If true, includes serial number, manufacturer, and version (API 21+).
	 *
	 * Returns:
	 *     Unique key string composed of device identifiers.
	 */
	public static final String getDeviceKeyName(final UsbDevice device, final boolean useNewAPI) {
		return getDeviceKeyName(device, null, useNewAPI);
	}
	/**
	 * Generate a unique key string for a USB device with optional serial and API level fields.
	 *
	 * Concatenates vendor ID, product ID, class, subclass, protocol, optional serial,
	 * and (if useNewAPI is true) manufacturer name, configuration count, and version.
	 *
	 * Args:
	 *     device: UsbDevice to generate key for. Null returns empty string.
	 *     serial: Optional serial number to include. Empty or null skips serial field.
	 *     useNewAPI: If true, includes API 21+ fields (serial, manufacturer, config count, version).
	 *
	 * Returns:
	 *     Unique key string composed of device identifiers, separated by '#'.
	 */
	@SuppressLint("NewApi")
	public static final String getDeviceKeyName(final UsbDevice device, final String serial, final boolean useNewAPI) {
		if (device == null) return "";
		final StringBuilder sb = new StringBuilder();
		sb.append(device.getVendorId());			sb.append("#");	// API >= 12
		sb.append(device.getProductId());			sb.append("#");	// API >= 12
		sb.append(device.getDeviceClass());			sb.append("#");	// API >= 12
		sb.append(device.getDeviceSubclass());		sb.append("#");	// API >= 12
		sb.append(device.getDeviceProtocol());						// API >= 12
		if (!TextUtils.isEmpty(serial)) {
			sb.append("#");	sb.append(serial);
		}
		if (useNewAPI && BuildCheck.isAndroid5()) {
			sb.append("#");
			if (TextUtils.isEmpty(serial)) {
				try {
					sb.append(device.getSerialNumber());
					sb.append("#");
				} // API >= 21 & targetSdkVersion has to be <= 28
				catch(SecurityException ignore) {}
			}
			sb.append(device.getManufacturerName());	sb.append("#");	// API >= 21
			sb.append(device.getConfigurationCount());	sb.append("#");	// API >= 21
			if (BuildCheck.isMarshmallow()) {
				sb.append(device.getVersion());			sb.append("#");	// API >= 23
			}
		}
		//		if (DEBUG) Log.v(TAG, "getDeviceKeyName:" + sb.toString());
		return sb.toString();
	}

	/**
	 * Get integer hash key for a USB device.
	 *
	 * Args:
	 *     device: UsbDevice to hash. Null returns 0.
	 *
	 * Returns:
	 *     Hash code of the device's key string.
	 */
	public static final int getDeviceKey(final UsbDevice device) {
		return device != null ? getDeviceKeyName(device, null, false).hashCode() : 0;
	}

	/**
	 * Get integer hash key for a USB device with optional new API fields.
	 *
	 * Args:
	 *     device: UsbDevice to hash. Null returns 0.
	 *     useNewAPI: If true, includes API 21+ fields in key generation.
	 *
	 * Returns:
	 *     Hash code of the device's key string.
	 */
	public static final int getDeviceKey(final UsbDevice device, final boolean useNewAPI) {
		return device != null ? getDeviceKeyName(device, null, useNewAPI).hashCode() : 0;
	}

	/**
	 * Get integer hash key for a USB device with optional serial and API level fields.
	 *
	 * Args:
	 *     device: UsbDevice to hash. Null returns 0.
	 *     serial: Optional serial number to include in key generation.
	 *     useNewAPI: If true, includes API 21+ fields in key generation.
	 *
	 * Returns:
	 *     Hash code of the device's key string.
	 */
	public static final int getDeviceKey(final UsbDevice device, final String serial, final boolean useNewAPI) {
		return device != null ? getDeviceKeyName(device, serial, useNewAPI).hashCode() : 0;
	}

	/**
	 * Holds the USB device descriptor fields exposed to camera clients.
	 *
	 * The fields are populated from the device descriptor and cleared when the
	 * cached device information is reset.
	 */
	public static class UsbDeviceInfo {
		public String usb_version;
		public String manufacturer;
		public String product;
		public String version;
		public String serial;

		private void clear() {
			usb_version = manufacturer = product = version = serial = null;
		}

		@Override
		public String toString() {
			return String.format("UsbDevice:usb_version=%s,manufacturer=%s,product=%s,version=%s,serial=%s",
				usb_version != null ? usb_version : "",
				manufacturer != null ? manufacturer : "",
				product != null ? product : "",
				version != null ? version : "",
				serial != null ? serial : "");
		}
	}

	private static final int USB_DIR_OUT = 0;
	private static final int USB_DIR_IN = 0x80;
	private static final int USB_TYPE_MASK = (0x03 << 5);
	private static final int USB_TYPE_STANDARD = (0x00 << 5);
	private static final int USB_TYPE_CLASS = (0x01 << 5);
	private static final int USB_TYPE_VENDOR = (0x02 << 5);
	private static final int USB_TYPE_RESERVED = (0x03 << 5);
	private static final int USB_RECIP_MASK = 0x1f;
	private static final int USB_RECIP_DEVICE = 0x00;
	private static final int USB_RECIP_INTERFACE = 0x01;
	private static final int USB_RECIP_ENDPOINT = 0x02;
	private static final int USB_RECIP_OTHER = 0x03;
	private static final int USB_RECIP_PORT = 0x04;
	private static final int USB_RECIP_RPIPE = 0x05;
	private static final int USB_REQ_GET_STATUS = 0x00;
	private static final int USB_REQ_CLEAR_FEATURE = 0x01;
	private static final int USB_REQ_SET_FEATURE = 0x03;
	private static final int USB_REQ_SET_ADDRESS = 0x05;
	private static final int USB_REQ_GET_DESCRIPTOR = 0x06;
	private static final int USB_REQ_SET_DESCRIPTOR = 0x07;
	private static final int USB_REQ_GET_CONFIGURATION = 0x08;
	private static final int USB_REQ_SET_CONFIGURATION = 0x09;
	private static final int USB_REQ_GET_INTERFACE = 0x0A;
	private static final int USB_REQ_SET_INTERFACE = 0x0B;
	private static final int USB_REQ_SYNCH_FRAME = 0x0C;
	private static final int USB_REQ_SET_SEL = 0x30;
	private static final int USB_REQ_SET_ISOCH_DELAY = 0x31;
	private static final int USB_REQ_SET_ENCRYPTION = 0x0D;
	private static final int USB_REQ_GET_ENCRYPTION = 0x0E;
	private static final int USB_REQ_RPIPE_ABORT = 0x0E;
	private static final int USB_REQ_SET_HANDSHAKE = 0x0F;
	private static final int USB_REQ_RPIPE_RESET = 0x0F;
	private static final int USB_REQ_GET_HANDSHAKE = 0x10;
	private static final int USB_REQ_SET_CONNECTION = 0x11;
	private static final int USB_REQ_SET_SECURITY_DATA = 0x12;
	private static final int USB_REQ_GET_SECURITY_DATA = 0x13;
	private static final int USB_REQ_SET_WUSB_DATA = 0x14;
	private static final int USB_REQ_LOOPBACK_DATA_WRITE = 0x15;
	private static final int USB_REQ_LOOPBACK_DATA_READ = 0x16;
	private static final int USB_REQ_SET_INTERFACE_DS = 0x17;

	private static final int USB_REQ_STANDARD_DEVICE_SET = (USB_DIR_OUT | USB_TYPE_STANDARD | USB_RECIP_DEVICE);		// 0x10
	private static final int USB_REQ_STANDARD_DEVICE_GET = (USB_DIR_IN | USB_TYPE_STANDARD | USB_RECIP_DEVICE);			// 0x90
	private static final int USB_REQ_STANDARD_INTERFACE_SET = (USB_DIR_OUT | USB_TYPE_STANDARD | USB_RECIP_INTERFACE);	// 0x11
	private static final int USB_REQ_STANDARD_INTERFACE_GET = (USB_DIR_IN | USB_TYPE_STANDARD | USB_RECIP_INTERFACE);	// 0x91
	private static final int USB_REQ_STANDARD_ENDPOINT_SET = (USB_DIR_OUT | USB_TYPE_STANDARD | USB_RECIP_ENDPOINT);	// 0x12
	private static final int USB_REQ_STANDARD_ENDPOINT_GET = (USB_DIR_IN | USB_TYPE_STANDARD | USB_RECIP_ENDPOINT);		// 0x92

	private static final int USB_REQ_CS_DEVICE_SET  = (USB_DIR_OUT | USB_TYPE_CLASS | USB_RECIP_DEVICE);				// 0x20
	private static final int USB_REQ_CS_DEVICE_GET = (USB_DIR_IN | USB_TYPE_CLASS | USB_RECIP_DEVICE);					// 0xa0
	private static final int USB_REQ_CS_INTERFACE_SET = (USB_DIR_OUT | USB_TYPE_CLASS | USB_RECIP_INTERFACE);			// 0x21
	private static final int USB_REQ_CS_INTERFACE_GET = (USB_DIR_IN | USB_TYPE_CLASS | USB_RECIP_INTERFACE);			// 0xa1
	private static final int USB_REQ_CS_ENDPOINT_SET = (USB_DIR_OUT | USB_TYPE_CLASS | USB_RECIP_ENDPOINT);				// 0x22
	private static final int USB_REQ_CS_ENDPOINT_GET = (USB_DIR_IN | USB_TYPE_CLASS | USB_RECIP_ENDPOINT);				// 0xa2

	private static final int USB_REQ_VENDER_DEVICE_SET = (USB_DIR_OUT | USB_TYPE_CLASS | USB_RECIP_DEVICE);				// 0x40
	private static final int USB_REQ_VENDER_DEVICE_GET = (USB_DIR_IN | USB_TYPE_CLASS | USB_RECIP_DEVICE);				// 0xc0
	private static final int USB_REQ_VENDER_INTERFACE_SET = (USB_DIR_OUT | USB_TYPE_CLASS | USB_RECIP_INTERFACE);		// 0x41
	private static final int USB_REQ_VENDER_INTERFACE_GET = (USB_DIR_IN | USB_TYPE_CLASS | USB_RECIP_INTERFACE);		// 0xc1
	private static final int USB_REQ_VENDER_ENDPOINT_SET = (USB_DIR_OUT | USB_TYPE_CLASS | USB_RECIP_ENDPOINT);			// 0x42
	private static final int USB_REQ_VENDER_ENDPOINT_GET = (USB_DIR_IN | USB_TYPE_CLASS | USB_RECIP_ENDPOINT);			// 0xc2

	private static final int USB_DT_DEVICE = 0x01;
	private static final int USB_DT_CONFIG = 0x02;
	private static final int USB_DT_STRING = 0x03;
	private static final int USB_DT_INTERFACE = 0x04;
	private static final int USB_DT_ENDPOINT = 0x05;
	private static final int USB_DT_DEVICE_QUALIFIER = 0x06;
	private static final int USB_DT_OTHER_SPEED_CONFIG = 0x07;
	private static final int USB_DT_INTERFACE_POWER = 0x08;
	private static final int USB_DT_OTG = 0x09;
	private static final int USB_DT_DEBUG = 0x0a;
	private static final int USB_DT_INTERFACE_ASSOCIATION = 0x0b;
	private static final int USB_DT_SECURITY = 0x0c;
	private static final int USB_DT_KEY = 0x0d;
	private static final int USB_DT_ENCRYPTION_TYPE = 0x0e;
	private static final int USB_DT_BOS = 0x0f;
	private static final int USB_DT_DEVICE_CAPABILITY = 0x10;
	private static final int USB_DT_WIRELESS_ENDPOINT_COMP = 0x11;
	private static final int USB_DT_WIRE_ADAPTER = 0x21;
	private static final int USB_DT_RPIPE = 0x22;
	private static final int USB_DT_CS_RADIO_CONTROL = 0x23;
	private static final int USB_DT_PIPE_USAGE = 0x24;
	private static final int USB_DT_SS_ENDPOINT_COMP = 0x30;
	private static final int USB_DT_CS_DEVICE = (USB_TYPE_CLASS | USB_DT_DEVICE);
	private static final int USB_DT_CS_CONFIG = (USB_TYPE_CLASS | USB_DT_CONFIG);
	private static final int USB_DT_CS_STRING = (USB_TYPE_CLASS | USB_DT_STRING);
	private static final int USB_DT_CS_INTERFACE = (USB_TYPE_CLASS | USB_DT_INTERFACE);
	private static final int USB_DT_CS_ENDPOINT = (USB_TYPE_CLASS | USB_DT_ENDPOINT);
	private static final int USB_DT_DEVICE_SIZE = 18;

	/**
	 * Retrieve a string descriptor from a USB device by descriptor ID.
	 *
	 * Iterates through available language IDs, attempting to read the string descriptor
	 * as UTF-16LE. Filters out garbled responses containing invalid characters.
	 *
	 * Args:
	 *     connection: Open UsbDeviceConnection for control transfers.
	 *     id: String descriptor ID to retrieve.
	 *     languageCount: Number of language IDs in the languages array.
	 *     languages: Array of supported language IDs from the device's string zero descriptor.
	 *
	 * Returns:
	 *     Decoded string value, or null if descriptor could not be read.
	 */
	private static String getString(final UsbDeviceConnection connection, final int id, final int languageCount, final byte[] languages) {
		final byte[] work = new byte[256];
		String result = null;
		for (int i = 1; i <= languageCount; i++) {
			int ret = connection.controlTransfer(
				USB_REQ_STANDARD_DEVICE_GET, // USB_DIR_IN | USB_TYPE_STANDARD | USB_RECIP_DEVICE
				USB_REQ_GET_DESCRIPTOR,
				(USB_DT_STRING << 8) | id, languages[i], work, 256, 0);
			if ((ret > 2) && (work[0] == ret) && (work[1] == USB_DT_STRING)) {
			// Skip first two bytes (bLength & bDescriptorType), decode remaining as UTF-16LE
				try {
					result = new String(work, 2, ret - 2, "UTF-16LE");
					if (!"Љ".equals(result)) {	// Some devices return garbled data; filter it out
						break;
					} else {
						result = null;
					}
				} catch (final UnsupportedEncodingException e) {
				// ignore
				}
			}
		}
		return result;
	}

	/**
	 * Get detailed device information including vendor, product, version, and serial.
	 *
	 * Attempts to read string descriptors from the device. Falls back to control transfers
	 * if the device doesn't have permission or API level is insufficient.
	 *
	 * Args:
	 *     device: UsbDevice to query. Null returns empty UsbDeviceInfo.
	 *
	 * Returns:
	 *     UsbDeviceInfo populated with available device strings.
	 */
	public UsbDeviceInfo getDeviceInfo(final UsbDevice device) {
		return updateDeviceInfo(mUsbManager, device, null);
	}

	/**
	 * Get detailed device information using a Context to obtain UsbManager.
	 *
	 * Args:
	 *     context: Android Context for accessing USB service.
	 *     device: UsbDevice to query. Null returns empty UsbDeviceInfo.
	 *
	 * Returns:
	 *     UsbDeviceInfo populated with available device strings.
	 */
	public static UsbDeviceInfo getDeviceInfo(final Context context, final UsbDevice device) {
		return updateDeviceInfo((UsbManager)context.getSystemService(Context.USB_SERVICE), device, new UsbDeviceInfo());
	}

	/**
	 * Populate UsbDeviceInfo from device descriptors and system APIs.
	 *
	 * Tries Android 5+ APIs first (getManufacturerName, getProductName, getSerialNumber),
	 * then falls back to control transfers if permission is available. Finally, uses
	 * USBVendorId lookup and hex formatting as last resort for missing fields.
	 *
	 * Args:
	 *     manager: UsbManager for opening device connections and checking permissions.
	 *     device: UsbDevice to query. Null returns cleared info object.
	 *     _info: Existing UsbDeviceInfo to populate, or null to create new one.
	 *
	 * Returns:
	 *     Populated UsbDeviceInfo instance.
	 */
	public static UsbDeviceInfo updateDeviceInfo(final UsbManager manager, final UsbDevice device, final UsbDeviceInfo _info) {
		final UsbDeviceInfo info = _info != null ? _info : new UsbDeviceInfo();
		info.clear();

		if (device != null) {
			if (BuildCheck.isLollipop()) {
				info.manufacturer = device.getManufacturerName();
				info.product = device.getProductName();
				info.serial = device.getSerialNumber();
			}
			if (BuildCheck.isMarshmallow()) {
				info.usb_version = device.getVersion();
			}
			if ((manager != null) && manager.hasPermission(device)) {
				final UsbDeviceConnection connection = manager.openDevice(device);
				final byte[] desc = connection.getRawDescriptors();

				if (TextUtils.isEmpty(info.usb_version)) {
					info.usb_version = String.format("%x.%02x", ((int)desc[3] & 0xff), ((int)desc[2] & 0xff));
				}
				if (TextUtils.isEmpty(info.version)) {
					info.version = String.format("%x.%02x", ((int)desc[13] & 0xff), ((int)desc[12] & 0xff));
				}
				if (TextUtils.isEmpty(info.serial)) {
					info.serial = connection.getSerial();
				}

				final byte[] languages = new byte[256];
				int languageCount = 0;
				// controlTransfer(int requestType, int request, int value, int index, byte[] buffer, int length, int timeout)
				try {
					int result = connection.controlTransfer(
						USB_REQ_STANDARD_DEVICE_GET, // USB_DIR_IN | USB_TYPE_STANDARD | USB_RECIP_DEVICE
	    				USB_REQ_GET_DESCRIPTOR,
	    				(USB_DT_STRING << 8) | 0, 0, languages, 256, 0);
					if (result > 0) {
	        			languageCount = (result - 2) / 2;
					}
					if (languageCount > 0) {
						if (TextUtils.isEmpty(info.manufacturer)) {
							info.manufacturer = getString(connection, desc[14], languageCount, languages);
						}
						if (TextUtils.isEmpty(info.product)) {
							info.product = getString(connection, desc[15], languageCount, languages);
						}
						if (TextUtils.isEmpty(info.serial)) {
							info.serial = getString(connection, desc[16], languageCount, languages);
						}
					}
				} finally {
					connection.close();
				}
			}
			if (TextUtils.isEmpty(info.manufacturer)) {
				info.manufacturer = USBVendorId.vendorName(device.getVendorId());
			}
			if (TextUtils.isEmpty(info.manufacturer)) {
				info.manufacturer = String.format("%04x", device.getVendorId());
			}
			if (TextUtils.isEmpty(info.product)) {
				info.product = String.format("%04x", device.getProductId());
			}
		}
		return info;
	}

	/**
	 * Monitor USB devices and manage the open control block for the selected device.
	 *
	 * Holds the UsbDeviceConnection, device info, bus/device numbers, and interface mappings.
	 * Created by USBMonitor.openDevice() when permission is granted. Supports cloning to create
	 * independent connections to the same device.
	 *
	 * Lifecycle:
	 *     Created (via openDevice) → Active (interfaces claimed/released) → Closed (via close())
	 *     Can be cloned to create additional connections before closing.
	 *
	 * Thread Safety:
	 *     Not thread-safe. All operations should occur on the same thread that created the instance.
	 */
	public static final class UsbControlBlock implements Cloneable {
		private final WeakReference<USBMonitor> mWeakMonitor;
		private final WeakReference<UsbDevice> mWeakDevice;
		protected UsbDeviceConnection mConnection;
		protected final UsbDeviceInfo mInfo;
		private final int mBusNum;
		private final int mDevNum;
		private final SparseArray<SparseArray<UsbInterface>> mInterfaces = new SparseArray<SparseArray<UsbInterface>>();

		/**
		 * Create a UsbControlBlock for an open USB device.
		 *
		 * Opens the device connection via UsbManager, reads device descriptors,
		 * and parses bus/device numbers from the device name path.
		 *
		 * Args:
		 *     monitor: Parent USBMonitor providing UsbManager access. Must have permission for device.
		 *     device: UsbDevice to open. Must not be null.
		 */
		private UsbControlBlock(final USBMonitor monitor, final UsbDevice device) {
			if (DEBUG) Log.i(TAG, "UsbControlBlock:constructor");
			mWeakMonitor = new WeakReference<USBMonitor>(monitor);
			mWeakDevice = new WeakReference<UsbDevice>(device);
			mConnection = monitor.mUsbManager.openDevice(device);
			mInfo = updateDeviceInfo(monitor.mUsbManager, device, null);
			final String name = device.getDeviceName();
			final String[] v = !TextUtils.isEmpty(name) ? name.split("/") : null;
			int busnum = 0;
			int devnum = 0;
			if (v != null) {
				busnum = Integer.parseInt(v[v.length-2]);
				devnum = Integer.parseInt(v[v.length-1]);
			}
			mBusNum = busnum;
			mDevNum = devnum;
			//			if (DEBUG) {
				if (mConnection != null) {
					final int desc = mConnection.getFileDescriptor();
					final byte[] rawDesc = mConnection.getRawDescriptors();
					Log.i(TAG, String.format(Locale.US, "name=%s,desc=%d,busnum=%d,devnum=%d,rawDesc=", name, desc, busnum, devnum) + rawDesc);
				} else {
					Log.e(TAG, "could not connect to device " + name);
				}
				//			}
		}

		/**
		 * Copy constructor that creates a new connection to the same device.
		 *
		 * Opens a fresh UsbDeviceConnection and re-reads device descriptors.
		 * Used by clone() to create independent connections.
		 *
		 * Args:
		 *     src: Source UsbControlBlock to copy from.
		 *
		 * Raises:
		 *     IllegalStateException: If the source device has been removed or permission is lost.
		 */
		private UsbControlBlock(final UsbControlBlock src) throws IllegalStateException {
			final USBMonitor monitor = src.getUSBMonitor();
			final UsbDevice device = src.getDevice();
			if (device == null) {
				throw new IllegalStateException("device may already be removed");
			}
			mConnection = monitor.mUsbManager.openDevice(device);
			if (mConnection == null) {
				throw new IllegalStateException("device may already be removed or have no permission");
			}
			mInfo = updateDeviceInfo(monitor.mUsbManager, device, null);
			mWeakMonitor = new WeakReference<USBMonitor>(monitor);
			mWeakDevice = new WeakReference<UsbDevice>(device);
			mBusNum = src.mBusNum;
			mDevNum = src.mDevNum;
			// TODO: Need to track multiple control blocks per device; current HashMap replaces existing entry
		}

		/**
		 * Create a cloned connection to the same device.
		 *
		 * Opens a new UsbDeviceConnection while preserving bus/device numbers and interface mappings.
		 *
		 * Returns:
		 *     New UsbControlBlock with independent connection to the same device.
		 *
		 * Raises:
		 *     CloneNotSupportedException: If the source device has been removed or permission is lost.
		 */
		@Override
		public UsbControlBlock clone() throws CloneNotSupportedException {
			final UsbControlBlock ctrlblock;
			try {
				ctrlblock = new UsbControlBlock(this);
			} catch (final IllegalStateException e) {
				throw new CloneNotSupportedException(e.getMessage());
			}
			return ctrlblock;
		}

		public USBMonitor getUSBMonitor() {
			return mWeakMonitor.get();
		}

		public final UsbDevice getDevice() {
			return mWeakDevice.get();
		}

		/**
		 * Get the system-assigned device name path.
		 *
		 * Returns:
		 *     Device name string (e.g., "usb/1-1"), or empty string if device is null.
		 */
		public String getDeviceName() {
			final UsbDevice device = mWeakDevice.get();
			return device != null ? device.getDeviceName() : "";
		}

		/**
		 * Get the Android-assigned device ID.
		 *
		 * Returns:
		 *     Device ID integer, or 0 if device is null.
		 */
		public int getDeviceId() {
			final UsbDevice device = mWeakDevice.get();
			return device != null ? device.getDeviceId() : 0;
		}

		/**
		 * Get the device key string without new API fields.
		 *
		 * Returns:
		 *     Unique key string composed of vendor ID, product ID, class, subclass, and protocol.
		 */
		public String getDeviceKeyName() {
			return USBMonitor.getDeviceKeyName(mWeakDevice.get());
		}

		/**
		 * Get the device key string with optional new API fields.
		 *
		 * Args:
		 *     useNewAPI: If true, includes serial number, manufacturer, and version (API 21+).
		 *
		 * Returns:
		 *     Unique key string composed of device identifiers.
		 *
		 * Raises:
		 *     IllegalStateException: If connection is closed and useNewAPI is true.
		 */
		public String getDeviceKeyName(final boolean useNewAPI) throws IllegalStateException {
			if (useNewAPI) checkConnection();
			return USBMonitor.getDeviceKeyName(mWeakDevice.get(), mInfo.serial, useNewAPI);
		}

		/**
		 * Get integer hash of the device key string.
		 *
		 * Returns:
		 *     Hash code of the device's key string.
		 *
		 * Raises:
		 *     IllegalStateException: If connection is closed.
		 */
		public int getDeviceKey() throws IllegalStateException {
			checkConnection();
			return USBMonitor.getDeviceKey(mWeakDevice.get());
		}

		/**
		 * Get integer hash of the device key string with optional new API fields.
		 *
		 * Args:
		 *     useNewAPI: If true, includes serial number, manufacturer, and version (API 21+).
		 *
		 * Returns:
		 *     Hash code of the device's key string.
		 *
		 * Raises:
		 *     IllegalStateException: If connection is closed and useNewAPI is true.
		 */
		public int getDeviceKey(final boolean useNewAPI) throws IllegalStateException {
			if (useNewAPI) checkConnection();
			return USBMonitor.getDeviceKey(mWeakDevice.get(), mInfo.serial, useNewAPI);
		}

		/**
		 * Get the device key string including serial number.
		 *
		 * Returns:
		 *     Unique key string that includes the device's serial number.
		 */
		public String getDeviceKeyNameWithSerial() {
			return USBMonitor.getDeviceKeyName(mWeakDevice.get(), mInfo.serial, false);
		}

		/**
		 * Get integer hash of the device key string including serial number.
		 *
		 * Returns:
		 *     Hash code of the device's key string with serial number.
		 */
		public int getDeviceKeyWithSerial() {
			return getDeviceKeyNameWithSerial().hashCode();
		}

		/**
		 * Get the underlying UsbDeviceConnection for this device.
		 *
		 * Returns:
		 *     Open UsbDeviceConnection, or null if device is closed.
		 */
		public synchronized UsbDeviceConnection getConnection() {
			return mConnection;
		}

		/**
		 * Get the native file descriptor for the USB device connection.
		 *
		 * Returns:
		 *     File descriptor integer for passing to native code.
		 *
		 * Raises:
		 *     IllegalStateException: If connection is closed.
		 */
		public synchronized int getFileDescriptor() throws IllegalStateException {
			checkConnection();
			return mConnection.getFileDescriptor();
		}

		/**
		 * Get the raw USB device descriptors.
		 *
		 * Returns:
		 *     Byte array containing the device's descriptor data.
		 *
		 * Raises:
		 *     IllegalStateException: If connection is closed.
		 */
		public synchronized byte[] getRawDescriptors() throws IllegalStateException {
			checkConnection();
			return mConnection.getRawDescriptors();
		}

		/**
		 * Get the USB vendor ID.
		 *
		 * Returns:
		 *     Vendor ID integer, or 0 if the device is no longer available.
		 */
		public int getVenderId() {
			final UsbDevice device = mWeakDevice.get();
			return device != null ? device.getVendorId() : 0;
		}

		/**
		 * Get the USB product ID.
		 *
		 * Returns:
		 *     Product ID integer, or 0 if the device is no longer available.
		 */
		public int getProductId() {
			final UsbDevice device = mWeakDevice.get();
			return device != null ? device.getProductId() : 0;
		}

		/**
		 * Get the USB specification version string reported by the device.
		 *
		 * Returns:
		 *     USB version string.
		 */
		public String getUsbVersion() {
			return mInfo.usb_version;
		}

		/**
		 * Get the device manufacturer string.
		 *
		 * Returns:
		 *     Manufacturer name from device descriptors.
		 */
		public String getManufacture() {
			return mInfo.manufacturer;
		}

		/**
		 * Get the device product name.
		 *
		 * Returns:
		 *     Product name from device descriptors.
		 */
		public String getProductName() {
			return mInfo.product;
		}

		/**
		 * Get the device firmware/product version string.
		 *
		 * Returns:
		 *     Version string from device descriptors.
		 */
		public String getVersion() {
			return mInfo.version;
		}

		/**
		 * Get the device serial number.
		 *
		 * Returns:
		 *     Serial number string from device descriptors.
		 */
		public String getSerial() {
			return mInfo.serial;
		}

		public int getBusNum() {
			return mBusNum;
		}

		public int getDevNum() {
			return mDevNum;
		}

		/**
		 * Get a USB interface by ID, using alternate setting 0.
		 *
		 * Args:
		 *     interface_id: Interface identifier.
		 *
		 * Returns:
		 *     Matching UsbInterface, or null if not found.
		 *
		 * Raises:
		 *     IllegalStateException: If connection is closed.
		 */
		public synchronized UsbInterface getInterface(final int interface_id) throws IllegalStateException {
			return getInterface(interface_id, 0);
		}

		/**
		 * Get a USB interface by ID and alternate setting.
		 *
		 * Args:
		 *     interface_id: Interface identifier.
		 *     altsetting: Alternate setting identifier.
		 *
		 * Returns:
		 *     Matching UsbInterface, or null if not found.
		 *
		 * Raises:
		 *     IllegalStateException: If connection is closed.
		 */
		public synchronized UsbInterface getInterface(final int interface_id, final int altsetting) throws IllegalStateException {
			checkConnection();
			SparseArray<UsbInterface> intfs = mInterfaces.get(interface_id);
			if (intfs == null) {
				intfs = new SparseArray<UsbInterface>();
				mInterfaces.put(interface_id, intfs);
			}
			UsbInterface intf = intfs.get(altsetting);
			if (intf == null) {
				final UsbDevice device = mWeakDevice.get();
				final int n = device.getInterfaceCount();
				for (int i = 0; i < n; i++) {
					final UsbInterface temp = device.getInterface(i);
					if ((temp.getId() == interface_id) && (temp.getAlternateSetting() == altsetting)) {
						intf = temp;
						break;
					}
				}
				if (intf != null) {
					intfs.append(altsetting, intf);
				}
			}
			return intf;
		}

		/**
		 * Claim a specific USB interface without forcing.
		 *
		 * Args:
		 *     intf: Interface to claim.
		 */
		public synchronized void claimInterface(final UsbInterface intf) {
			claimInterface(intf, true);
		}

		public synchronized void claimInterface(final UsbInterface intf, final boolean force) {
			checkConnection();
			mConnection.claimInterface(intf, force);
		}

		/**
		 * Release a previously claimed USB interface.
		 *
		 * Args:
		 *     intf: Interface to release.
		 *
		 * Raises:
		 *     IllegalStateException: If connection is closed.
		 */
		public synchronized void releaseInterface(final UsbInterface intf) throws IllegalStateException {
			checkConnection();
			final SparseArray<UsbInterface> intfs = mInterfaces.get(intf.getId());
			if (intfs != null) {
				final int index = intfs.indexOfValue(intf);
				intfs.removeAt(index);
				if (intfs.size() == 0) {
					mInterfaces.remove(intf.getId());
				}
			}
			mConnection.releaseInterface(intf);
		}

		/**
		 * Close the device, releasing all claimed interfaces.
		 */
		public synchronized void close() {
			if (DEBUG) Log.i(TAG, "UsbControlBlock#close:");

			if (mConnection != null) {
				final int n = mInterfaces.size();
				for (int i = 0; i < n; i++) {
					final SparseArray<UsbInterface> intfs = mInterfaces.valueAt(i);
					if (intfs != null) {
						final int m = intfs.size();
						for (int j = 0; j < m; j++) {
							final UsbInterface intf = intfs.valueAt(j);
							mConnection.releaseInterface(intf);
						}
						intfs.clear();
					}
				}
				mInterfaces.clear();
				mConnection.close();
				mConnection = null;
				final USBMonitor monitor = mWeakMonitor.get();
				if (monitor != null) {
					if (monitor.mOnDeviceConnectListener != null) {
						monitor.mOnDeviceConnectListener.onDisconnect(mWeakDevice.get(), UsbControlBlock.this);
					}
					monitor.mCtrlBlocks.remove(getDevice());
				}
			}
		}

		@Override
		public boolean equals(final Object o) {
			if (o == null) return false;
			if (o instanceof UsbControlBlock) {
				final UsbDevice device = ((UsbControlBlock) o).getDevice();
				return device == null ? mWeakDevice.get() == null
						: device.equals(mWeakDevice.get());
			} else if (o instanceof UsbDevice) {
				return o.equals(mWeakDevice.get());
			}
			return super.equals(o);
		}

		//		@Override
		//		protected void finalize() throws Throwable {
		///			close();
		//			super.finalize();
		//		}

		private synchronized void checkConnection() throws IllegalStateException {
			if (mConnection == null) {
				throw new IllegalStateException("already closed");
			}
		}
	}

}
