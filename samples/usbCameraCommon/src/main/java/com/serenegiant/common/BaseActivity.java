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

package com.serenegiant.common;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.NonNull;
import android.support.annotation.StringRes;
import android.app.Activity;
import android.util.Log;
import android.widget.Toast;

import com.serenegiant.usbcameracommon.R;
import com.serenegiant.dialog.MessageDialogFragment;
import com.serenegiant.dialog.MessageDialogFragmentV4;
import com.serenegiant.utils.BuildCheck;
import com.serenegiant.utils.HandlerThreadHandler;
import com.serenegiant.utils.PermissionCheck;

/**
 * Base activity that schedules work on the UI and worker threads, manages
 * toast notifications, and handles runtime permission requests and results
 * for the USB camera sample applications.
 */

public class BaseActivity extends Activity
	implements MessageDialogFragmentV4.MessageDialogListener {

	private static boolean DEBUG = false;	// FIXME set to false for production
	private static final String TAG = BaseActivity.class.getSimpleName();

	/** Handler for UI-thread operations. */
	private final Handler mUIHandler = new Handler(Looper.getMainLooper());
	private final Thread mUiThread = mUIHandler.getLooper().getThread();
	private Handler mWorkerHandler;
	private long mWorkerThreadID = -1;

	@Override
	protected void onCreate(final Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		// create the worker thread
		if (mWorkerHandler == null) {
			mWorkerHandler = HandlerThreadHandler.createHandler(TAG);
			mWorkerThreadID = mWorkerHandler.getLooper().getThread().getId();
		}
	}

	@Override
	protected void onPause() {
		clearToast();
		super.onPause();
	}

	@Override
	protected synchronized void onDestroy() {
	// release the worker thread
		if (mWorkerHandler != null) {
			try {
				mWorkerHandler.getLooper().quit();
			} catch (final Exception e) {
			//
			}
			mWorkerHandler = null;
		}
		super.onDestroy();
	}

	//================================================================================
	/**
	 * Post a task to run on the UI thread, optionally after a delay.
	 *
	 * Args:
	 *     task: Runnable to execute; ignored when null.
	 *     duration: Delay in milliseconds before running the task.
	 */

	public final void runOnUiThread(final Runnable task, final long duration) {
		if (task == null) return;
		mUIHandler.removeCallbacks(task);
		if ((duration > 0) || Thread.currentThread() != mUiThread) {
			mUIHandler.postDelayed(task, duration);
		} else {
			try {
				task.run();
			} catch (final Exception e) {
				Log.w(TAG, e);
			}
		}
	}

	/**
	 * Remove a pending task from the UI thread queue.
	 *
	 * Args:
	 *     task: Runnable to remove; ignored when null.
	 */

	public final void removeFromUiThread(final Runnable task) {
		if (task == null) return;
		mUIHandler.removeCallbacks(task);
	}

	/**
	 * Queue a task to run on the worker thread, canceling any pending
	 * identical task so only the latest is executed.
	 *
	 * Args:
	 *     task: Runnable to execute; ignored when null.
	 *     delayMillis: Delay in milliseconds before running the task.
	 */
	protected final synchronized void queueEvent(final Runnable task, final long delayMillis) {
		if ((task == null) || (mWorkerHandler == null)) return;
		try {
			mWorkerHandler.removeCallbacks(task);
			if (delayMillis > 0) {
				mWorkerHandler.postDelayed(task, delayMillis);
			} else if (mWorkerThreadID == Thread.currentThread().getId()) {
				task.run();
			} else {
				mWorkerHandler.post(task);
			}
		} catch (final Exception e) {
		// ignore
		}
	}

	/**
	 * Cancel a pending task on the worker thread.
	 *
	 * Args:
	 *     task: Runnable to remove; ignored when null.
	 */
	protected final synchronized void removeEvent(final Runnable task) {
		if (task == null) return;
		try {
			mWorkerHandler.removeCallbacks(task);
		} catch (final Exception e) {
		// ignore
		}
	}

	//================================================================================
	private Toast mToast;
	/**
	 * Show a toast with a string resource and optional format arguments.
	 *
	 * Args:
	 *     msg: String resource identifier for the toast message.
	 *     args: Format arguments for the string resource.
	 */
	protected void showToast(@StringRes final int msg, final Object... args) {
		removeFromUiThread(mShowToastTask);
		mShowToastTask = new ShowToastTask(msg, args);
		runOnUiThread(mShowToastTask, 0);
	}

	/**
	 * Cancel any active toast and clear the pending toast task.
	 */
	protected void clearToast() {
		removeFromUiThread(mShowToastTask);
		mShowToastTask = null;
		try {
			if (mToast != null) {
				mToast.cancel();
				mToast = null;
			}
		} catch (final Exception e) {
		// ignore
		}
	}

	private ShowToastTask mShowToastTask;
	private final class ShowToastTask implements Runnable {
		final int msg;
		final Object args;
		private ShowToastTask(@StringRes final int msg, final Object... args) {
			this.msg = msg;
			this.args = args;
		}

		@Override
		/**
		 * Display the queued toast message on the UI thread.
		 */


		public void run() {
			try {
				if (mToast != null) {
					mToast.cancel();
					mToast = null;
				}
				final String _msg = (args != null) ? getString(msg, args) : getString(msg);
				mToast = Toast.makeText(BaseActivity.this, _msg, Toast.LENGTH_SHORT);
				mToast.show();
			} catch (final Exception e) {
			// ignore
			}
		}
	}

	//================================================================================
	/**
	 * Handle the result of a permission message dialog.
	 *
	 * Args:
	 *     dialog: The dialog that produced the result.
	 *     requestCode: The request code the permission was requested with.
	 *     permissions: The permissions being requested.
	 *     result: True when the user confirmed the dialog.
	 */
	@SuppressLint("NewApi")
	@Override

	public void onMessageDialogResult(final MessageDialogFragmentV4 dialog, final int requestCode, final String[] permissions, final boolean result) {
		if (result) {
		// request the permissions when the user confirmed the dialog
			if (BuildCheck.isMarshmallow()) {
				requestPermissions(permissions, requestCode);
				return;
			}
		}
		// when the dialog was canceled or on pre-Android 6, check the result and call checkPermissionResult
		for (final String permission: permissions) {
			checkPermissionResult(requestCode, permission, PermissionCheck.hasPermission(this, permission));
		}
	}

	/**
	 * Receive the result of a runtime permission request.
	 *
	 * Args:
	 *     requestCode: The request code the permission was requested with.
	 *     permissions: The permissions that were requested.
	 *     grantResults: The grant result for each permission.
	 */
	@Override

	public void onRequestPermissionsResult(final int requestCode, @NonNull final String[] permissions, @NonNull final int[] grantResults) {
		super.onRequestPermissionsResult(requestCode, permissions, grantResults);	// no-op, but call super for correctness
		final int n = Math.min(permissions.length, grantResults.length);
		for (int i = 0; i < n; i++) {
			checkPermissionResult(requestCode, permissions[i], grantResults[i] == PackageManager.PERMISSION_GRANTED);
		}
	}

	/**
	 * Check a permission request result and show a toast when the
	 * permission was not granted.
	 *
	 * Args:
	 *     requestCode: The request code the permission was requested with.
	 *     permission: The permission that was requested.
	 *     result: True when the permission was granted.
	 */
	protected void checkPermissionResult(final int requestCode, final String permission, final boolean result) {
	// show a message when the permission is missing
		if (!result && (permission != null)) {
			if (Manifest.permission.RECORD_AUDIO.equals(permission)) {
				showToast(R.string.permission_audio);
			}
			if (Manifest.permission.WRITE_EXTERNAL_STORAGE.equals(permission)) {
				showToast(R.string.permission_ext_storage);
			}
			if (Manifest.permission.INTERNET.equals(permission)) {
				showToast(R.string.permission_network);
			}
		}
	}

	// request codes for dynamic permission requests
	protected static final int REQUEST_PERMISSION_WRITE_EXTERNAL_STORAGE = 0x12345;
	protected static final int REQUEST_PERMISSION_AUDIO_RECORDING = 0x234567;
	protected static final int REQUEST_PERMISSION_NETWORK = 0x345678;
	protected static final int REQUEST_PERMISSION_CAMERA = 0x537642;

	/**
	 * Check for external storage write permission, showing an explanation
	 * dialog when it is missing.
	 *
	 * Returns:
	 *     True when external storage write permission is available.
	 */
	protected boolean checkPermissionWriteExternalStorage() {
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S_V2) {
			return Environment.isExternalStorageManager() || Environment.isExternalStorageEmulated();
		} else {
			if (!PermissionCheck.hasWriteExternalStorage(this)) {
				MessageDialogFragment.showDialog(this, REQUEST_PERMISSION_WRITE_EXTERNAL_STORAGE,
						R.string.permission_title, R.string.permission_ext_storage_request,
						new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE});
				return false;
			}
		}
		return true;
	}

	/**
	 * Check for audio recording permission, showing an explanation dialog
	 * when it is missing.
	 *
	 * Returns:
	 *     True when audio recording permission is available.
	 */
	protected boolean checkPermissionAudio() {
		if (!PermissionCheck.hasAudio(this)) {
			MessageDialogFragment.showDialog(this, REQUEST_PERMISSION_AUDIO_RECORDING,
				R.string.permission_title, R.string.permission_audio_recording_request,
				new String[]{Manifest.permission.RECORD_AUDIO});
			return false;
		}
		return true;
	}

	/**
	 * Check for network access permission, showing an explanation dialog
	 * when it is missing.
	 *
	 * Returns:
	 *     True when network access permission is available.
	 */
	protected boolean checkPermissionNetwork() {
		if (!PermissionCheck.hasNetwork(this)) {
			MessageDialogFragment.showDialog(this, REQUEST_PERMISSION_NETWORK,
				R.string.permission_title, R.string.permission_network_request,
				new String[]{Manifest.permission.INTERNET});
			return false;
		}
		return true;
	}

	/**
	 * Check for camera access permission, showing an explanation dialog
	 * when it is missing.
	 *
	 * Returns:
	 *     True when camera access permission is available.
	 */
	protected boolean checkPermissionCamera() {
		if (!PermissionCheck.hasCamera(this)) {
			MessageDialogFragment.showDialog(this, REQUEST_PERMISSION_CAMERA,
				R.string.permission_title, R.string.permission_camera_request,
				new String[]{Manifest.permission.CAMERA});
			return false;
		}
		return true;
	}

}
