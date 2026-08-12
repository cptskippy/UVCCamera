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

package com.serenegiant.usbcameracommon;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.SurfaceTexture;
import android.hardware.usb.UsbDevice;
import android.media.AudioManager;
import android.media.MediaScannerConnection;
import android.media.SoundPool;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.text.TextUtils;
import android.util.Log;
import android.view.Surface;
import android.view.SurfaceHolder;

import com.serenegiant.encoder.MediaAudioEncoder;
import com.serenegiant.encoder.MediaEncoder;
import com.serenegiant.encoder.MediaMuxerWrapper;
import com.serenegiant.encoder.MediaSurfaceEncoder;
import com.serenegiant.encoder.MediaVideoBufferEncoder;
import com.serenegiant.encoder.MediaVideoEncoder;
import com.serenegiant.usb.IFrameCallback;
import com.serenegiant.usb.USBMonitor;
import com.serenegiant.usb.UVCCamera;
import com.serenegiant.widget.CameraViewInterface;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

abstract class AbstractUVCCameraHandler extends Handler {
	private static final boolean DEBUG = true;  // TODO set false on release
	private static final String TAG = "AbsUVCCameraHandler";
/**
 * Manages CameraCallback functionality.
 *
 * Responsibility: Provides core CameraCallback operations for the USB camera stack.
 *
 * Lifecycle: Instantiated → configured → used → released.
 *
 * Thread Safety: Methods are synchronized where applicable; otherwise not thread-safe.
 * 
Properties:
    mBandwidthFactor: Field mBandwidthFactor
    mIsPreviewing: Field mIsPreviewing
    mIsRecording: Field mIsRecording
    mSoundPool: Field mSoundPool
    mSoundId: Field mSoundId
    mHandler: Field mHandler
    mUVCCamera: Field mUVCCamera
    mMuxer: Field mMuxer
    mVideoEncoder: Field mVideoEncoder
State Machine:
 *   Initialized → Active → Released
 *   Error (from any active state)
 *
 * Example:
 *     // Example usage of CameraCallback
 */
/**
 * Manages CameraCallback functionality.
 *
 * Responsibility: Provides core CameraCallback operations for the USB camera stack.
 *
 * Lifecycle: Instantiated → configured → used → released.
 *
 * Thread Safety: Methods are synchronized where applicable; otherwise not thread-safe.
 *
 * Properties:
 *   Fields are managed internally.
 *
 * State Machine:
 *   Initialized → Active → Released
 *   Error (from any active state)
 *
 * Example:
 *     // Example usage of CameraCallback
 */



	public interface CameraCallback {
		public void onOpen();
		public void onClose();
		public void onStartPreview();
		public void onStopPreview();
		public void onStartRecording();
		public void onStopRecording();
		public void onError(final Exception e);
	}

	private static final int MSG_OPEN = 0;
	private static final int MSG_CLOSE = 1;
	private static final int MSG_PREVIEW_START = 2;
	private static final int MSG_PREVIEW_STOP = 3;
	private static final int MSG_CAPTURE_STILL = 4;
	private static final int MSG_CAPTURE_START = 5;
	private static final int MSG_CAPTURE_STOP = 6;
	private static final int MSG_MEDIA_UPDATE = 7;
	private static final int MSG_RELEASE = 9;

	private final WeakReference<AbstractUVCCameraHandler.CameraThread> mWeakThread;
	private volatile boolean mReleased;

	protected AbstractUVCCameraHandler(final CameraThread thread) {
		mWeakThread = new WeakReference<CameraThread>(thread);
	}

	/**

	 * Getwidth.

	 *

	 * Returns:

	 *     Description of the return value.

	 *

	 * Raises:

	 *     Exception: When an error occurs.

	 *

	 * Side Effects:

	 *     - May mutate internal state.

	 *

	 * Code Paths:

	 *     1. If preconditions met → executes normally.

	 *     2. On error → logs and returns default.

	 */
/**
 * Getwidth.
 *
 * Args:
 *     param: Parameter controls behavior.
 *
 * Returns:
 *     Description of the return value.
 *
 * Raises:
 *     Exception: When an error occurs.
 *
 * Side Effects:
 *     - May mutate internal state.
 *
 * Code Paths:
 *     1. If preconditions met → executes normally.
 *     2. On error → logs and returns default.
 */



	public int getWidth() {
		final CameraThread thread = mWeakThread.get();
		return thread != null ? thread.getWidth() : 0;
	}

	/**

	 * Getheight.

	 *

	 * Returns:

	 *     Description of the return value.

	 *

	 * Raises:

	 *     Exception: When an error occurs.

	 *

	 * Side Effects:

	 *     - May mutate internal state.

	 *

	 * Code Paths:

	 *     1. If preconditions met → executes normally.

	 *     2. On error → logs and returns default.

	 */
/**
 * Getheight.
 *
 * Args:
 *     param: Parameter controls behavior.
 *
 * Returns:
 *     Description of the return value.
 *
 * Raises:
 *     Exception: When an error occurs.
 *
 * Side Effects:
 *     - May mutate internal state.
 *
 * Code Paths:
 *     1. If preconditions met → executes normally.
 *     2. On error → logs and returns default.
 */



	public int getHeight() {
		final CameraThread thread = mWeakThread.get();
		return thread != null ? thread.getHeight() : 0;
	}

	/**

	 * Isopened.

	 *

	 * Returns:

	 *     Description of the return value.

	 *

	 * Raises:

	 *     Exception: When an error occurs.

	 *

	 * Side Effects:

	 *     - May mutate internal state.

	 *

	 * Code Paths:

	 *     1. If preconditions met → executes normally.

	 *     2. On error → logs and returns default.

	 */
/**
 * Isopened.
 *
 * Args:
 *     param: Parameter controls behavior.
 *
 * Returns:
 *     Description of the return value.
 *
 * Raises:
 *     Exception: When an error occurs.
 *
 * Side Effects:
 *     - May mutate internal state.
 *
 * Code Paths:
 *     1. If preconditions met → executes normally.
 *     2. On error → logs and returns default.
 */



	public boolean isOpened() {
		final CameraThread thread = mWeakThread.get();
		return thread != null && thread.isCameraOpened();
	}

	/**

	 * Ispreviewing.

	 *

	 * Returns:

	 *     Description of the return value.

	 *

	 * Raises:

	 *     Exception: When an error occurs.

	 *

	 * Side Effects:

	 *     - May mutate internal state.

	 *

	 * Code Paths:

	 *     1. If preconditions met → executes normally.

	 *     2. On error → logs and returns default.

	 */
/**
 * Ispreviewing.
 *
 * Args:
 *     param: Parameter controls behavior.
 *
 * Returns:
 *     Description of the return value.
 *
 * Raises:
 *     Exception: When an error occurs.
 *
 * Side Effects:
 *     - May mutate internal state.
 *
 * Code Paths:
 *     1. If preconditions met → executes normally.
 *     2. On error → logs and returns default.
 */



	public boolean isPreviewing() {
		final CameraThread thread = mWeakThread.get();
		return thread != null && thread.isPreviewing();
	}

	/**

	 * Isrecording.

	 *

	 * Returns:

	 *     Description of the return value.

	 *

	 * Raises:

	 *     Exception: When an error occurs.

	 *

	 * Side Effects:

	 *     - May mutate internal state.

	 *

	 * Code Paths:

	 *     1. If preconditions met → executes normally.

	 *     2. On error → logs and returns default.

	 */
/**
 * Isrecording.
 *
 * Args:
 *     param: Parameter controls behavior.
 *
 * Returns:
 *     Description of the return value.
 *
 * Raises:
 *     Exception: When an error occurs.
 *
 * Side Effects:
 *     - May mutate internal state.
 *
 * Code Paths:
 *     1. If preconditions met → executes normally.
 *     2. On error → logs and returns default.
 */



	public boolean isRecording() {
		final CameraThread thread = mWeakThread.get();
		return thread != null && thread.isRecording();
	}

	/**

	 * Isequal.

	 *

	 * 

	Args:

	    device: Parameter device controls behavior.

	Returns:

	 *     Description of the return value.

	 *

	 * Raises:

	 *     Exception: When an error occurs.

	 *

	 * Side Effects:

	 *     - May mutate internal state.

	 *

	 * Code Paths:

	 *     1. If preconditions met → executes normally.

	 *     2. On error → logs and returns default.

	 */
/**
 * Isequal.
 *
 * Args:
 *     param: Parameter controls behavior.
 *
 * Returns:
 *     Description of the return value.
 *
 * Raises:
 *     Exception: When an error occurs.
 *
 * Side Effects:
 *     - May mutate internal state.
 *
 * Code Paths:
 *     1. If preconditions met → executes normally.
 *     2. On error → logs and returns default.
 */



	public boolean isEqual(final UsbDevice device) {
		final CameraThread thread = mWeakThread.get();
		return (thread != null) && thread.isEqual(device);
	}

	protected boolean isCameraThread() {
		final CameraThread thread = mWeakThread.get();
		return thread != null && (thread.getId() == Thread.currentThread().getId());
	}

	protected boolean isReleased() {
		final CameraThread thread = mWeakThread.get();
		return mReleased || (thread == null);
	}

	protected void checkReleased() {
		if (isReleased()) {
			throw new IllegalStateException("already released");
		}
	}

	/**

	 * Open.

	 *

	 * 

	Args:

	    ctrlBlock: Parameter ctrlBlock controls behavior.

	Returns:

	 *     Description of the return value.

	 *

	 * Raises:

	 *     Exception: When an error occurs.

	 *

	 * Side Effects:

	 *     - May mutate internal state.

	 *

	 * Code Paths:

	 *     1. If preconditions met → executes normally.

	 *     2. On error → logs and returns default.

	 */
/**
 * Open.
 *
 * Args:
 *     param: Parameter controls behavior.
 *
 * Returns:
 *     Description of the return value.
 *
 * Raises:
 *     Exception: When an error occurs.
 *
 * Side Effects:
 *     - May mutate internal state.
 *
 * Code Paths:
 *     1. If preconditions met → executes normally.
 *     2. On error → logs and returns default.
 */



	public void open(final USBMonitor.UsbControlBlock ctrlBlock) {
		checkReleased();
		sendMessage(obtainMessage(MSG_OPEN, ctrlBlock));
	}

	/**

	 * Close.

	 *

	 * Returns:

	 *     Description of the return value.

	 *

	 * Raises:

	 *     Exception: When an error occurs.

	 *

	 * Side Effects:

	 *     - May mutate internal state.

	 *

	 * Code Paths:

	 *     1. If preconditions met → executes normally.

	 *     2. On error → logs and returns default.

	 */
/**
 * Close.
 *
 * Args:
 *     param: Parameter controls behavior.
 *
 * Returns:
 *     Description of the return value.
 *
 * Raises:
 *     Exception: When an error occurs.
 *
 * Side Effects:
 *     - May mutate internal state.
 *
 * Code Paths:
 *     1. If preconditions met → executes normally.
 *     2. On error → logs and returns default.
 */



	public void close() {
		if (DEBUG) Log.v(TAG, "close:");
		if (isOpened()) {
			stopPreview();
			sendEmptyMessage(MSG_CLOSE);
		}
		if (DEBUG) Log.v(TAG, "close:finished");
	}

	/**

	 * Resize.

	 *

	 * 

	Args:

	    width: Parameter width controls behavior.

	    height: Parameter height controls behavior.

	Returns:

	 *     Description of the return value.

	 *

	 * Raises:

	 *     Exception: When an error occurs.

	 *

	 * Side Effects:

	 *     - May mutate internal state.

	 *

	 * Code Paths:

	 *     1. If preconditions met → executes normally.

	 *     2. On error → logs and returns default.

	 */
/**
 * Resize.
 *
 * Args:
 *     param: Parameter controls behavior.
 *
 * Returns:
 *     Description of the return value.
 *
 * Raises:
 *     Exception: When an error occurs.
 *
 * Side Effects:
 *     - May mutate internal state.
 *
 * Code Paths:
 *     1. If preconditions met → executes normally.
 *     2. On error → logs and returns default.
 */



	public void resize(final int width, final int height) {
		checkReleased();
		throw new UnsupportedOperationException("does not support now");
	}

	protected void startPreview(final Object surface) {
		checkReleased();
		if (!((surface instanceof SurfaceHolder) || (surface instanceof Surface) || (surface instanceof SurfaceTexture))) {
			throw new IllegalArgumentException("surface should be one of SurfaceHolder, Surface or SurfaceTexture");
		}
		sendMessage(obtainMessage(MSG_PREVIEW_START, surface));
	}

	/**

	 * Stoppreview.

	 *

	 * Returns:

	 *     Description of the return value.

	 *

	 * Raises:

	 *     Exception: When an error occurs.

	 *

	 * Side Effects:

	 *     - May mutate internal state.

	 *

	 * Code Paths:

	 *     1. If preconditions met → executes normally.

	 *     2. On error → logs and returns default.

	 */
/**
 * Stoppreview.
 *
 * Args:
 *     param: Parameter controls behavior.
 *
 * Returns:
 *     Description of the return value.
 *
 * Raises:
 *     Exception: When an error occurs.
 *
 * Side Effects:
 *     - May mutate internal state.
 *
 * Code Paths:
 *     1. If preconditions met → executes normally.
 *     2. On error → logs and returns default.
 */



	public void stopPreview() {
		if (DEBUG) Log.v(TAG, "stopPreview:");
		removeMessages(MSG_PREVIEW_START);
		stopRecording();
		if (isPreviewing()) {
			final CameraThread thread = mWeakThread.get();
			if (thread == null) return;
			synchronized (thread.mSync) {
				sendEmptyMessage(MSG_PREVIEW_STOP);
				if (!isCameraThread()) {
					// wait for actually preview stopped to avoid releasing Surface/SurfaceTexture
					// while preview is still running.
					// therefore this method will take a time to execute
					try {
						thread.mSync.wait();
					} catch (final InterruptedException e) {
					}
				}
			}
		}
		if (DEBUG) Log.v(TAG, "stopPreview:finished");
	}

	protected void captureStill() {
		checkReleased();
		sendEmptyMessage(MSG_CAPTURE_STILL);
	}

	protected void captureStill(final String path) {
		checkReleased();
		sendMessage(obtainMessage(MSG_CAPTURE_STILL, path));
	}

	/**

	 * Startrecording.

	 *

	 * Returns:

	 *     Description of the return value.

	 *

	 * Raises:

	 *     Exception: When an error occurs.

	 *

	 * Side Effects:

	 *     - May mutate internal state.

	 *

	 * Code Paths:

	 *     1. If preconditions met → executes normally.

	 *     2. On error → logs and returns default.

	 */
/**
 * Startrecording.
 *
 * Args:
 *     param: Parameter controls behavior.
 *
 * Returns:
 *     Description of the return value.
 *
 * Raises:
 *     Exception: When an error occurs.
 *
 * Side Effects:
 *     - May mutate internal state.
 *
 * Code Paths:
 *     1. If preconditions met → executes normally.
 *     2. On error → logs and returns default.
 */



	public void startRecording() {
		checkReleased();
		sendEmptyMessage(MSG_CAPTURE_START);
	}

	/**

	 * Stoprecording.

	 *

	 * Returns:

	 *     Description of the return value.

	 *

	 * Raises:

	 *     Exception: When an error occurs.

	 *

	 * Side Effects:

	 *     - May mutate internal state.

	 *

	 * Code Paths:

	 *     1. If preconditions met → executes normally.

	 *     2. On error → logs and returns default.

	 */
/**
 * Stoprecording.
 *
 * Args:
 *     param: Parameter controls behavior.
 *
 * Returns:
 *     Description of the return value.
 *
 * Raises:
 *     Exception: When an error occurs.
 *
 * Side Effects:
 *     - May mutate internal state.
 *
 * Code Paths:
 *     1. If preconditions met → executes normally.
 *     2. On error → logs and returns default.
 */



	public void stopRecording() {
		sendEmptyMessage(MSG_CAPTURE_STOP);
	}

	/**

	 * Release.

	 *

	 * Returns:

	 *     Description of the return value.

	 *

	 * Raises:

	 *     Exception: When an error occurs.

	 *

	 * Side Effects:

	 *     - May mutate internal state.

	 *

	 * Code Paths:

	 *     1. If preconditions met → executes normally.

	 *     2. On error → logs and returns default.

	 */
/**
 * Release.
 *
 * Args:
 *     param: Parameter controls behavior.
 *
 * Returns:
 *     Description of the return value.
 *
 * Raises:
 *     Exception: When an error occurs.
 *
 * Side Effects:
 *     - May mutate internal state.
 *
 * Code Paths:
 *     1. If preconditions met → executes normally.
 *     2. On error → logs and returns default.
 */



	public void release() {
		mReleased = true;
		close();
		sendEmptyMessage(MSG_RELEASE);
	}

	/**

	 * Addcallback.

	 *

	 * 

	Args:

	    callback: Parameter callback controls behavior.

	Returns:

	 *     Description of the return value.

	 *

	 * Raises:

	 *     Exception: When an error occurs.

	 *

	 * Side Effects:

	 *     - May mutate internal state.

	 *

	 * Code Paths:

	 *     1. If preconditions met → executes normally.

	 *     2. On error → logs and returns default.

	 */
/**
 * Addcallback.
 *
 * Args:
 *     param: Parameter controls behavior.
 *
 * Returns:
 *     Description of the return value.
 *
 * Raises:
 *     Exception: When an error occurs.
 *
 * Side Effects:
 *     - May mutate internal state.
 *
 * Code Paths:
 *     1. If preconditions met → executes normally.
 *     2. On error → logs and returns default.
 */



	public void addCallback(final CameraCallback callback) {
		checkReleased();
		if (!mReleased && (callback != null)) {
			final CameraThread thread = mWeakThread.get();
			if (thread != null) {
				thread.mCallbacks.add(callback);
			}
		}
	}

	/**

	 * Removecallback.

	 *

	 * 

	Args:

	    callback: Parameter callback controls behavior.

	Returns:

	 *     Description of the return value.

	 *

	 * Raises:

	 *     Exception: When an error occurs.

	 *

	 * Side Effects:

	 *     - May mutate internal state.

	 *

	 * Code Paths:

	 *     1. If preconditions met → executes normally.

	 *     2. On error → logs and returns default.

	 */
/**
 * Removecallback.
 *
 * Args:
 *     param: Parameter controls behavior.
 *
 * Returns:
 *     Description of the return value.
 *
 * Raises:
 *     Exception: When an error occurs.
 *
 * Side Effects:
 *     - May mutate internal state.
 *
 * Code Paths:
 *     1. If preconditions met → executes normally.
 *     2. On error → logs and returns default.
 */



	public void removeCallback(final CameraCallback callback) {
		if (callback != null) {
			final CameraThread thread = mWeakThread.get();
			if (thread != null) {
				thread.mCallbacks.remove(callback);
			}
		}
	}

	protected void updateMedia(final String path) {
		sendMessage(obtainMessage(MSG_MEDIA_UPDATE, path));
	}

	/**

	 * Checksupportflag.

	 *

	 * 

	Args:

	    flag: Parameter flag controls behavior.

	Returns:

	 *     Description of the return value.

	 *

	 * Raises:

	 *     Exception: When an error occurs.

	 *

	 * Side Effects:

	 *     - May mutate internal state.

	 *

	 * Code Paths:

	 *     1. If preconditions met → executes normally.

	 *     2. On error → logs and returns default.

	 */
/**
 * Checksupportflag.
 *
 * Args:
 *     param: Parameter controls behavior.
 *
 * Returns:
 *     Description of the return value.
 *
 * Raises:
 *     Exception: When an error occurs.
 *
 * Side Effects:
 *     - May mutate internal state.
 *
 * Code Paths:
 *     1. If preconditions met → executes normally.
 *     2. On error → logs and returns default.
 */



	public boolean checkSupportFlag(final long flag) {
		checkReleased();
		final CameraThread thread = mWeakThread.get();
		return thread != null && thread.mUVCCamera != null && thread.mUVCCamera.checkSupportFlag(flag);
	}

	/**

	 * Getvalue.

	 *

	 * 

	Args:

	    flag: Parameter flag controls behavior.

	Returns:

	 *     Description of the return value.

	 *

	 * Raises:

	 *     Exception: When an error occurs.

	 *

	 * Side Effects:

	 *     - May mutate internal state.

	 *

	 * Code Paths:

	 *     1. If preconditions met → executes normally.

	 *     2. On error → logs and returns default.

	 */
/**
 * Getvalue.
 *
 * Args:
 *     param: Parameter controls behavior.
 *
 * Returns:
 *     Description of the return value.
 *
 * Raises:
 *     Exception: When an error occurs.
 *
 * Side Effects:
 *     - May mutate internal state.
 *
 * Code Paths:
 *     1. If preconditions met → executes normally.
 *     2. On error → logs and returns default.
 */



	public int getValue(final int flag) {
		checkReleased();
		final CameraThread thread = mWeakThread.get();
		final UVCCamera camera = thread != null ? thread.mUVCCamera : null;
		if (camera != null) {
			if (flag == UVCCamera.PU_BRIGHTNESS) {
				return camera.getBrightness();
			} else if (flag == UVCCamera.PU_CONTRAST) {
				return camera.getContrast();
			}
		}
		throw new IllegalStateException();
	}

	/**

	 * Setvalue.

	 *

	 * 

	Args:

	    flag: Parameter flag controls behavior.

	    value: Parameter value controls behavior.

	Returns:

	 *     Description of the return value.

	 *

	 * Raises:

	 *     Exception: When an error occurs.

	 *

	 * Side Effects:

	 *     - May mutate internal state.

	 *

	 * Code Paths:

	 *     1. If preconditions met → executes normally.

	 *     2. On error → logs and returns default.

	 */
/**
 * Setvalue.
 *
 * Args:
 *     param: Parameter controls behavior.
 *
 * Returns:
 *     Description of the return value.
 *
 * Raises:
 *     Exception: When an error occurs.
 *
 * Side Effects:
 *     - May mutate internal state.
 *
 * Code Paths:
 *     1. If preconditions met → executes normally.
 *     2. On error → logs and returns default.
 */



	public int setValue(final int flag, final int value) {
		checkReleased();
		final CameraThread thread = mWeakThread.get();
		final UVCCamera camera = thread != null ? thread.mUVCCamera : null;
		if (camera != null) {
			if (flag == UVCCamera.PU_BRIGHTNESS) {
				camera.setBrightness(value);
				return camera.getBrightness();
			} else if (flag == UVCCamera.PU_CONTRAST) {
				camera.setContrast(value);
				return camera.getContrast();
			}
		}
		throw new IllegalStateException();
	}

	/**

	 * Resetvalue.

	 *

	 * 

	Args:

	    flag: Parameter flag controls behavior.

	Returns:

	 *     Description of the return value.

	 *

	 * Raises:

	 *     Exception: When an error occurs.

	 *

	 * Side Effects:

	 *     - May mutate internal state.

	 *

	 * Code Paths:

	 *     1. If preconditions met → executes normally.

	 *     2. On error → logs and returns default.

	 */
/**
 * Resetvalue.
 *
 * Args:
 *     param: Parameter controls behavior.
 *
 * Returns:
 *     Description of the return value.
 *
 * Raises:
 *     Exception: When an error occurs.
 *
 * Side Effects:
 *     - May mutate internal state.
 *
 * Code Paths:
 *     1. If preconditions met → executes normally.
 *     2. On error → logs and returns default.
 */



	public int resetValue(final int flag) {
		checkReleased();
		final CameraThread thread = mWeakThread.get();
		final UVCCamera camera = thread != null ? thread.mUVCCamera : null;
		if (camera != null) {
			if (flag == UVCCamera.PU_BRIGHTNESS) {
				camera.resetBrightness();
				return camera.getBrightness();
			} else if (flag == UVCCamera.PU_CONTRAST) {
				camera.resetContrast();
				return camera.getContrast();
			}
		}
		throw new IllegalStateException();
	}

	@Override
	/**
	 * Handlemessage.
	 *
	 * 
	Args:
	    msg: Parameter msg controls behavior.
	Returns:
	 *     Description of the return value.
	 *
	 * Raises:
	 *     Exception: When an error occurs.
	 *
	 * Side Effects:
	 *     - May mutate internal state.
	 *
	 * Code Paths:
	 *     1. If preconditions met → executes normally.
	 *     2. On error → logs and returns default.
	 */
/**
 * Handlemessage.
 *
 * Args:
 *     param: Parameter controls behavior.
 *
 * Returns:
 *     Description of the return value.
 *
 * Raises:
 *     Exception: When an error occurs.
 *
 * Side Effects:
 *     - May mutate internal state.
 *
 * Code Paths:
 *     1. If preconditions met → executes normally.
 *     2. On error → logs and returns default.
 */


	public void handleMessage(final Message msg) {
		final CameraThread thread = mWeakThread.get();
		if (thread == null) return;
		switch (msg.what) {
		case MSG_OPEN:
			thread.handleOpen((USBMonitor.UsbControlBlock)msg.obj);
			break;
		case MSG_CLOSE:
			thread.handleClose();
			break;
		case MSG_PREVIEW_START:
			thread.handleStartPreview(msg.obj);
			break;
		case MSG_PREVIEW_STOP:
			thread.handleStopPreview();
			break;
		case MSG_CAPTURE_STILL:
			thread.handleCaptureStill((String)msg.obj);
			break;
		case MSG_CAPTURE_START:
			thread.handleStartRecording();
			break;
		case MSG_CAPTURE_STOP:
			thread.handleStopRecording();
			break;
		case MSG_MEDIA_UPDATE:
			thread.handleUpdateMedia((String)msg.obj);
			break;
		case MSG_RELEASE:
			thread.handleRelease();
			break;
		default:
			throw new RuntimeException("unsupported message:what=" + msg.what);
		}
	}

	static final class CameraThread extends Thread {
		private static final String TAG_THREAD = "CameraThread";
		private final Object mSync = new Object();
		private final Class<? extends AbstractUVCCameraHandler> mHandlerClass;
		private final WeakReference<Activity> mWeakParent;
		private final WeakReference<CameraViewInterface> mWeakCameraView;
		private final int mEncoderType;
		private final Set<CameraCallback> mCallbacks = new CopyOnWriteArraySet<CameraCallback>();
		private int mWidth, mHeight, mPreviewMode;
		private float mBandwidthFactor;
		private boolean mIsPreviewing;
		private boolean mIsRecording;
		/**
		 * shutter sound
		 */
		private SoundPool mSoundPool;
		private int mSoundId;
		private AbstractUVCCameraHandler mHandler;
		/**
		 * for accessing UVC camera
		 */
		private UVCCamera mUVCCamera;
		/**
		 * muxer for audio/video recording
		 */
		private MediaMuxerWrapper mMuxer;
		private MediaVideoBufferEncoder mVideoEncoder;

		/**
		 *
		 * @param clazz Class extends AbstractUVCCameraHandler
		 * @param parent parent Activity
		 * @param cameraView for still capturing
		 * @param encoderType 0: use MediaSurfaceEncoder, 1: use MediaVideoEncoder, 2: use MediaVideoBufferEncoder
		 * @param width
		 * @param height
		 * @param format either FRAME_FORMAT_YUYV(0) or FRAME_FORMAT_MJPEG(1)
		 * @param bandwidthFactor
		 */
		CameraThread(final Class<? extends AbstractUVCCameraHandler> clazz,
			final Activity parent, final CameraViewInterface cameraView,
			final int encoderType, final int width, final int height, final int format,
			final float bandwidthFactor) {

			super("CameraThread");
			mHandlerClass = clazz;
			mEncoderType = encoderType;
			mWidth = width;
			mHeight = height;
			mPreviewMode = format;
			mBandwidthFactor = bandwidthFactor;
			mWeakParent = new WeakReference<Activity>(parent);
			mWeakCameraView = new WeakReference<CameraViewInterface>(cameraView);
			loadShutterSound(parent);
		}

		@Override
		protected void finalize() throws Throwable {
			Log.i(TAG, "CameraThread#finalize");
			super.finalize();
		}

		/**

		 * Gethandler.

		 *

		 * Returns:

		 *     Description of the return value.

		 *

		 * Raises:

		 *     Exception: When an error occurs.

		 *

		 * Side Effects:

		 *     - May mutate internal state.

		 *

		 * Code Paths:

		 *     1. If preconditions met → executes normally.

		 *     2. On error → logs and returns default.

		 */
/**
 * Gethandler.
 *
 * Args:
 *     param: Parameter controls behavior.
 *
 * Returns:
 *     Description of the return value.
 *
 * Raises:
 *     Exception: When an error occurs.
 *
 * Side Effects:
 *     - May mutate internal state.
 *
 * Code Paths:
 *     1. If preconditions met → executes normally.
 *     2. On error → logs and returns default.
 */



		public AbstractUVCCameraHandler getHandler() {
			if (DEBUG) Log.v(TAG_THREAD, "getHandler:");
			synchronized (mSync) {
				if (mHandler == null)
				try {
					mSync.wait();
				} catch (final InterruptedException e) {
				}
			}
			return mHandler;
		}

		/**

		 * Getwidth.

		 *

		 * Returns:

		 *     Description of the return value.

		 *

		 * Raises:

		 *     Exception: When an error occurs.

		 *

		 * Side Effects:

		 *     - May mutate internal state.

		 *

		 * Code Paths:

		 *     1. If preconditions met → executes normally.

		 *     2. On error → logs and returns default.

		 */
/**
 * Getwidth.
 *
 * Args:
 *     param: Parameter controls behavior.
 *
 * Returns:
 *     Description of the return value.
 *
 * Raises:
 *     Exception: When an error occurs.
 *
 * Side Effects:
 *     - May mutate internal state.
 *
 * Code Paths:
 *     1. If preconditions met → executes normally.
 *     2. On error → logs and returns default.
 */



		public int getWidth() {
			synchronized (mSync) {
				return mWidth;
			}
		}

		/**

		 * Getheight.

		 *

		 * Returns:

		 *     Description of the return value.

		 *

		 * Raises:

		 *     Exception: When an error occurs.

		 *

		 * Side Effects:

		 *     - May mutate internal state.

		 *

		 * Code Paths:

		 *     1. If preconditions met → executes normally.

		 *     2. On error → logs and returns default.

		 */
/**
 * Getheight.
 *
 * Args:
 *     param: Parameter controls behavior.
 *
 * Returns:
 *     Description of the return value.
 *
 * Raises:
 *     Exception: When an error occurs.
 *
 * Side Effects:
 *     - May mutate internal state.
 *
 * Code Paths:
 *     1. If preconditions met → executes normally.
 *     2. On error → logs and returns default.
 */



		public int getHeight() {
			synchronized (mSync) {
				return mHeight;
			}
		}

		/**

		 * Iscameraopened.

		 *

		 * Returns:

		 *     Description of the return value.

		 *

		 * Raises:

		 *     Exception: When an error occurs.

		 *

		 * Side Effects:

		 *     - May mutate internal state.

		 *

		 * Code Paths:

		 *     1. If preconditions met → executes normally.

		 *     2. On error → logs and returns default.

		 */
/**
 * Iscameraopened.
 *
 * Args:
 *     param: Parameter controls behavior.
 *
 * Returns:
 *     Description of the return value.
 *
 * Raises:
 *     Exception: When an error occurs.
 *
 * Side Effects:
 *     - May mutate internal state.
 *
 * Code Paths:
 *     1. If preconditions met → executes normally.
 *     2. On error → logs and returns default.
 */



		public boolean isCameraOpened() {
			synchronized (mSync) {
				return mUVCCamera != null;
			}
		}

		/**

		 * Ispreviewing.

		 *

		 * Returns:

		 *     Description of the return value.

		 *

		 * Raises:

		 *     Exception: When an error occurs.

		 *

		 * Side Effects:

		 *     - May mutate internal state.

		 *

		 * Code Paths:

		 *     1. If preconditions met → executes normally.

		 *     2. On error → logs and returns default.

		 */
/**
 * Ispreviewing.
 *
 * Args:
 *     param: Parameter controls behavior.
 *
 * Returns:
 *     Description of the return value.
 *
 * Raises:
 *     Exception: When an error occurs.
 *
 * Side Effects:
 *     - May mutate internal state.
 *
 * Code Paths:
 *     1. If preconditions met → executes normally.
 *     2. On error → logs and returns default.
 */



		public boolean isPreviewing() {
			synchronized (mSync) {
				return mUVCCamera != null && mIsPreviewing;
			}
		}

		/**

		 * Isrecording.

		 *

		 * Returns:

		 *     Description of the return value.

		 *

		 * Raises:

		 *     Exception: When an error occurs.

		 *

		 * Side Effects:

		 *     - May mutate internal state.

		 *

		 * Code Paths:

		 *     1. If preconditions met → executes normally.

		 *     2. On error → logs and returns default.

		 */
/**
 * Isrecording.
 *
 * Args:
 *     param: Parameter controls behavior.
 *
 * Returns:
 *     Description of the return value.
 *
 * Raises:
 *     Exception: When an error occurs.
 *
 * Side Effects:
 *     - May mutate internal state.
 *
 * Code Paths:
 *     1. If preconditions met → executes normally.
 *     2. On error → logs and returns default.
 */



		public boolean isRecording() {
			synchronized (mSync) {
				return (mUVCCamera != null) && (mMuxer != null);
			}
		}

		/**

		 * Isequal.

		 *

		 * 

		Args:

		    device: Parameter device controls behavior.

		Returns:

		 *     Description of the return value.

		 *

		 * Raises:

		 *     Exception: When an error occurs.

		 *

		 * Side Effects:

		 *     - May mutate internal state.

		 *

		 * Code Paths:

		 *     1. If preconditions met → executes normally.

		 *     2. On error → logs and returns default.

		 */
/**
 * Isequal.
 *
 * Args:
 *     param: Parameter controls behavior.
 *
 * Returns:
 *     Description of the return value.
 *
 * Raises:
 *     Exception: When an error occurs.
 *
 * Side Effects:
 *     - May mutate internal state.
 *
 * Code Paths:
 *     1. If preconditions met → executes normally.
 *     2. On error → logs and returns default.
 */



		public boolean isEqual(final UsbDevice device) {
			return (mUVCCamera != null) && (mUVCCamera.getDevice() != null) && mUVCCamera.getDevice().equals(device);
		}

		/**

		 * Handleopen.

		 *

		 * 

		Args:

		    ctrlBlock: Parameter ctrlBlock controls behavior.

		Returns:

		 *     Description of the return value.

		 *

		 * Raises:

		 *     Exception: When an error occurs.

		 *

		 * Side Effects:

		 *     - May mutate internal state.

		 *

		 * Code Paths:

		 *     1. If preconditions met → executes normally.

		 *     2. On error → logs and returns default.

		 */
/**
 * Handleopen.
 *
 * Args:
 *     param: Parameter controls behavior.
 *
 * Returns:
 *     Description of the return value.
 *
 * Raises:
 *     Exception: When an error occurs.
 *
 * Side Effects:
 *     - May mutate internal state.
 *
 * Code Paths:
 *     1. If preconditions met → executes normally.
 *     2. On error → logs and returns default.
 */



		public void handleOpen(final USBMonitor.UsbControlBlock ctrlBlock) {
			if (DEBUG) Log.v(TAG_THREAD, "handleOpen:");
			handleClose();
			try {
				final UVCCamera camera = new UVCCamera();
				camera.open(ctrlBlock);
				synchronized (mSync) {
					mUVCCamera = camera;
				}
				callOnOpen();
			} catch (final Exception e) {
				callOnError(e);
			}
			if (DEBUG) Log.i(TAG, "supportedSize:" + (mUVCCamera != null ? mUVCCamera.getSupportedSize() : null));
		}

		/**

		 * Handleclose.

		 *

		 * Returns:

		 *     Description of the return value.

		 *

		 * Raises:

		 *     Exception: When an error occurs.

		 *

		 * Side Effects:

		 *     - May mutate internal state.

		 *

		 * Code Paths:

		 *     1. If preconditions met → executes normally.

		 *     2. On error → logs and returns default.

		 */
/**
 * Handleclose.
 *
 * Args:
 *     param: Parameter controls behavior.
 *
 * Returns:
 *     Description of the return value.
 *
 * Raises:
 *     Exception: When an error occurs.
 *
 * Side Effects:
 *     - May mutate internal state.
 *
 * Code Paths:
 *     1. If preconditions met → executes normally.
 *     2. On error → logs and returns default.
 */



		public void handleClose() {
			if (DEBUG) Log.v(TAG_THREAD, "handleClose:");
			handleStopRecording();
			final UVCCamera camera;
			synchronized (mSync) {
				camera = mUVCCamera;
				mUVCCamera = null;
			}
			if (camera != null) {
				camera.stopPreview();
				camera.destroy();
				callOnClose();
			}
		}

		/**

		 * Handlestartpreview.

		 *

		 * 

		Args:

		    surface: Parameter surface controls behavior.

		Returns:

		 *     Description of the return value.

		 *

		 * Raises:

		 *     Exception: When an error occurs.

		 *

		 * Side Effects:

		 *     - May mutate internal state.

		 *

		 * Code Paths:

		 *     1. If preconditions met → executes normally.

		 *     2. On error → logs and returns default.

		 */
/**
 * Handlestartpreview.
 *
 * Args:
 *     param: Parameter controls behavior.
 *
 * Returns:
 *     Description of the return value.
 *
 * Raises:
 *     Exception: When an error occurs.
 *
 * Side Effects:
 *     - May mutate internal state.
 *
 * Code Paths:
 *     1. If preconditions met → executes normally.
 *     2. On error → logs and returns default.
 */



		public void handleStartPreview(final Object surface) {
			if (DEBUG) Log.v(TAG_THREAD, "handleStartPreview:");
			if ((mUVCCamera == null) || mIsPreviewing) return;
			try {
				mUVCCamera.setPreviewSize(mWidth, mHeight, 1, 31, mPreviewMode, mBandwidthFactor);
			} catch (final IllegalArgumentException e) {
				try {
					// fallback to YUV mode
					mUVCCamera.setPreviewSize(mWidth, mHeight, 1, 31, UVCCamera.DEFAULT_PREVIEW_MODE, mBandwidthFactor);
				} catch (final IllegalArgumentException e1) {
					callOnError(e1);
					return;
				}
			}
			if (surface instanceof SurfaceHolder) {
				mUVCCamera.setPreviewDisplay((SurfaceHolder)surface);
			} if (surface instanceof Surface) {
				mUVCCamera.setPreviewDisplay((Surface)surface);
			} else {
				mUVCCamera.setPreviewTexture((SurfaceTexture)surface);
			}
			mUVCCamera.startPreview();
			mUVCCamera.updateCameraParams();
			synchronized (mSync) {
				mIsPreviewing = true;
			}
			callOnStartPreview();
		}

		/**

		 * Handlestoppreview.

		 *

		 * Returns:

		 *     Description of the return value.

		 *

		 * Raises:

		 *     Exception: When an error occurs.

		 *

		 * Side Effects:

		 *     - May mutate internal state.

		 *

		 * Code Paths:

		 *     1. If preconditions met → executes normally.

		 *     2. On error → logs and returns default.

		 */
/**
 * Handlestoppreview.
 *
 * Args:
 *     param: Parameter controls behavior.
 *
 * Returns:
 *     Description of the return value.
 *
 * Raises:
 *     Exception: When an error occurs.
 *
 * Side Effects:
 *     - May mutate internal state.
 *
 * Code Paths:
 *     1. If preconditions met → executes normally.
 *     2. On error → logs and returns default.
 */



		public void handleStopPreview() {
			if (DEBUG) Log.v(TAG_THREAD, "handleStopPreview:");
			if (mIsPreviewing) {
				if (mUVCCamera != null) {
					mUVCCamera.stopPreview();
				}
				synchronized (mSync) {
					mIsPreviewing = false;
					mSync.notifyAll();
				}
				callOnStopPreview();
			}
			if (DEBUG) Log.v(TAG_THREAD, "handleStopPreview:finished");
		}

		/**

		 * Handlecapturestill.

		 *

		 * 

		Args:

		    path: Parameter path controls behavior.

		Returns:

		 *     Description of the return value.

		 *

		 * Raises:

		 *     Exception: When an error occurs.

		 *

		 * Side Effects:

		 *     - May mutate internal state.

		 *

		 * Code Paths:

		 *     1. If preconditions met → executes normally.

		 *     2. On error → logs and returns default.

		 */
/**
 * Handlecapturestill.
 *
 * Args:
 *     param: Parameter controls behavior.
 *
 * Returns:
 *     Description of the return value.
 *
 * Raises:
 *     Exception: When an error occurs.
 *
 * Side Effects:
 *     - May mutate internal state.
 *
 * Code Paths:
 *     1. If preconditions met → executes normally.
 *     2. On error → logs and returns default.
 */



		public void handleCaptureStill(final String path) {
			if (DEBUG) Log.v(TAG_THREAD, "handleCaptureStill:");
			final Activity parent = mWeakParent.get();
			if (parent == null) return;
			mSoundPool.play(mSoundId, 0.2f, 0.2f, 0, 0, 1.0f);  // play shutter sound
			try {
				final Bitmap bitmap = mWeakCameraView.get().captureStillImage();
				// get buffered output stream for saving a captured still image as a file on external storage.
				// the file name is came from current time.
				// You should use extension name as same as CompressFormat when calling Bitmap#compress.
				final File outputFile = TextUtils.isEmpty(path)
					? MediaMuxerWrapper.getCaptureFile(Environment.DIRECTORY_DCIM, ".png")
					: new File(path);
				final BufferedOutputStream os = new BufferedOutputStream(new FileOutputStream(outputFile));
				try {
					try {
						bitmap.compress(Bitmap.CompressFormat.PNG, 100, os);
						os.flush();
						mHandler.sendMessage(mHandler.obtainMessage(MSG_MEDIA_UPDATE, outputFile.getPath()));
					} catch (final IOException e) {
					}
				} finally {
					os.close();
				}
			} catch (final Exception e) {
				callOnError(e);
			}
		}

		/**

		 * Handlestartrecording.

		 *

		 * Returns:

		 *     Description of the return value.

		 *

		 * Raises:

		 *     Exception: When an error occurs.

		 *

		 * Side Effects:

		 *     - May mutate internal state.

		 *

		 * Code Paths:

		 *     1. If preconditions met → executes normally.

		 *     2. On error → logs and returns default.

		 */
/**
 * Handlestartrecording.
 *
 * Args:
 *     param: Parameter controls behavior.
 *
 * Returns:
 *     Description of the return value.
 *
 * Raises:
 *     Exception: When an error occurs.
 *
 * Side Effects:
 *     - May mutate internal state.
 *
 * Code Paths:
 *     1. If preconditions met → executes normally.
 *     2. On error → logs and returns default.
 */



		public void handleStartRecording() {
			if (DEBUG) Log.v(TAG_THREAD, "handleStartRecording:");
			try {
				if ((mUVCCamera == null) || (mMuxer != null)) return;
				final MediaMuxerWrapper muxer = new MediaMuxerWrapper(".mp4");  // if you record audio only, ".m4a" is also OK.
				MediaVideoBufferEncoder videoEncoder = null;
				switch (mEncoderType) {
				case 1: // for video capturing using MediaVideoEncoder
					new MediaVideoEncoder(muxer, getWidth(), getHeight(), mMediaEncoderListener);
					break;
				case 2: // for video capturing using MediaVideoBufferEncoder
					videoEncoder = new MediaVideoBufferEncoder(muxer, getWidth(), getHeight(), mMediaEncoderListener);
					break;
				// case 0:  // for video capturing using MediaSurfaceEncoder
				default:
					new MediaSurfaceEncoder(muxer, getWidth(), getHeight(), mMediaEncoderListener);
					break;
				}
				if (true) {
					// for audio capturing
					new MediaAudioEncoder(muxer, mMediaEncoderListener);
				}
				muxer.prepare();
				muxer.startRecording();
				if (videoEncoder != null) {
					mUVCCamera.setFrameCallback(mIFrameCallback, UVCCamera.PIXEL_FORMAT_NV21);
				}
				synchronized (mSync) {
					mMuxer = muxer;
					mVideoEncoder = videoEncoder;
				}
				callOnStartRecording();
			} catch (final IOException e) {
				callOnError(e);
				Log.e(TAG, "startCapture:", e);
			}
		}

		/**

		 * Handlestoprecording.

		 *

		 * Returns:

		 *     Description of the return value.

		 *

		 * Raises:

		 *     Exception: When an error occurs.

		 *

		 * Side Effects:

		 *     - May mutate internal state.

		 *

		 * Code Paths:

		 *     1. If preconditions met → executes normally.

		 *     2. On error → logs and returns default.

		 */
/**
 * Handlestoprecording.
 *
 * Args:
 *     param: Parameter controls behavior.
 *
 * Returns:
 *     Description of the return value.
 *
 * Raises:
 *     Exception: When an error occurs.
 *
 * Side Effects:
 *     - May mutate internal state.
 *
 * Code Paths:
 *     1. If preconditions met → executes normally.
 *     2. On error → logs and returns default.
 */



		public void handleStopRecording() {
			if (DEBUG) Log.v(TAG_THREAD, "handleStopRecording:mMuxer=" + mMuxer);
			final MediaMuxerWrapper muxer;
			synchronized (mSync) {
				muxer = mMuxer;
				mMuxer = null;
				mVideoEncoder = null;
				if (mUVCCamera != null) {
					mUVCCamera.stopCapture();
				}
			}
			try {
				mWeakCameraView.get().setVideoEncoder(null);
			} catch (final Exception e) {
				// ignore
			}
			if (muxer != null) {
				muxer.stopRecording();
				mUVCCamera.setFrameCallback(null, 0);
				// you should not wait here
				callOnStopRecording();
			}
		}

		private final IFrameCallback mIFrameCallback = new IFrameCallback() {
			@Override
			/**
			 * Onframe.
			 *
			 * 
			Args:
			    frame: Parameter frame controls behavior.
			Returns:
			 *     Description of the return value.
			 *
			 * Raises:
			 *     Exception: When an error occurs.
			 *
			 * Side Effects:
			 *     - May mutate internal state.
			 *
			 * Code Paths:
			 *     1. If preconditions met → executes normally.
			 *     2. On error → logs and returns default.
			 */
/**
 * Onframe.
 *
 * Args:
 *     param: Parameter controls behavior.
 *
 * Returns:
 *     Description of the return value.
 *
 * Raises:
 *     Exception: When an error occurs.
 *
 * Side Effects:
 *     - May mutate internal state.
 *
 * Code Paths:
 *     1. If preconditions met → executes normally.
 *     2. On error → logs and returns default.
 */


			public void onFrame(final ByteBuffer frame) {
				final MediaVideoBufferEncoder videoEncoder;
				synchronized (mSync) {
					videoEncoder = mVideoEncoder;
				}
				if (videoEncoder != null) {
					videoEncoder.frameAvailableSoon();
					videoEncoder.encode(frame);
				}
			}
		};

		/**

		 * Handleupdatemedia.

		 *

		 * 

		Args:

		    path: Parameter path controls behavior.

		Returns:

		 *     Description of the return value.

		 *

		 * Raises:

		 *     Exception: When an error occurs.

		 *

		 * Side Effects:

		 *     - May mutate internal state.

		 *

		 * Code Paths:

		 *     1. If preconditions met → executes normally.

		 *     2. On error → logs and returns default.

		 */
/**
 * Handleupdatemedia.
 *
 * Args:
 *     param: Parameter controls behavior.
 *
 * Returns:
 *     Description of the return value.
 *
 * Raises:
 *     Exception: When an error occurs.
 *
 * Side Effects:
 *     - May mutate internal state.
 *
 * Code Paths:
 *     1. If preconditions met → executes normally.
 *     2. On error → logs and returns default.
 */



		public void handleUpdateMedia(final String path) {
			if (DEBUG) Log.v(TAG_THREAD, "handleUpdateMedia:path=" + path);
			final Activity parent = mWeakParent.get();
			final boolean released = (mHandler == null) || mHandler.mReleased;
			if (parent != null && parent.getApplicationContext() != null) {
				try {
					if (DEBUG) Log.i(TAG, "MediaScannerConnection#scanFile");
					MediaScannerConnection.scanFile(parent.getApplicationContext(), new String[]{ path }, null, null);
				} catch (final Exception e) {
					Log.e(TAG, "handleUpdateMedia:", e);
				}
				if (released || parent.isDestroyed())
					handleRelease();
			} else {
				Log.w(TAG, "MainActivity already destroyed");
				// give up to add this movie to MediaStore now.
				// Seeing this movie on Gallery app etc. will take a lot of time.
				handleRelease();
			}
		}

		/**

		 * Handlerelease.

		 *

		 * Returns:

		 *     Description of the return value.

		 *

		 * Raises:

		 *     Exception: When an error occurs.

		 *

		 * Side Effects:

		 *     - May mutate internal state.

		 *

		 * Code Paths:

		 *     1. If preconditions met → executes normally.

		 *     2. On error → logs and returns default.

		 */
/**
 * Handlerelease.
 *
 * Args:
 *     param: Parameter controls behavior.
 *
 * Returns:
 *     Description of the return value.
 *
 * Raises:
 *     Exception: When an error occurs.
 *
 * Side Effects:
 *     - May mutate internal state.
 *
 * Code Paths:
 *     1. If preconditions met → executes normally.
 *     2. On error → logs and returns default.
 */



		public void handleRelease() {
			if (DEBUG) Log.v(TAG_THREAD, "handleRelease:mIsRecording=" + mIsRecording);
			handleClose();
			mCallbacks.clear();
			if (!mIsRecording) {
				mHandler.mReleased = true;
				Looper.myLooper().quit();
			}
			if (DEBUG) Log.v(TAG_THREAD, "handleRelease:finished");
		}

		private final MediaEncoder.MediaEncoderListener mMediaEncoderListener = new MediaEncoder.MediaEncoderListener() {
			@Override
			/**
			 * Onprepared.
			 *
			 * 
			Args:
			    encoder: Parameter encoder controls behavior.
			Returns:
			 *     Description of the return value.
			 *
			 * Raises:
			 *     Exception: When an error occurs.
			 *
			 * Side Effects:
			 *     - May mutate internal state.
			 *
			 * Code Paths:
			 *     1. If preconditions met → executes normally.
			 *     2. On error → logs and returns default.
			 */
/**
 * Onprepared.
 *
 * Args:
 *     param: Parameter controls behavior.
 *
 * Returns:
 *     Description of the return value.
 *
 * Raises:
 *     Exception: When an error occurs.
 *
 * Side Effects:
 *     - May mutate internal state.
 *
 * Code Paths:
 *     1. If preconditions met → executes normally.
 *     2. On error → logs and returns default.
 */


			public void onPrepared(final MediaEncoder encoder) {
				if (DEBUG) Log.v(TAG, "onPrepared:encoder=" + encoder);
				mIsRecording = true;
				if (encoder instanceof MediaVideoEncoder)
				try {
					mWeakCameraView.get().setVideoEncoder((MediaVideoEncoder)encoder);
				} catch (final Exception e) {
					Log.e(TAG, "onPrepared:", e);
				}
				if (encoder instanceof MediaSurfaceEncoder)
				try {
					mWeakCameraView.get().setVideoEncoder((MediaSurfaceEncoder)encoder);
					mUVCCamera.startCapture(((MediaSurfaceEncoder)encoder).getInputSurface());
				} catch (final Exception e) {
					Log.e(TAG, "onPrepared:", e);
				}
			}

			@Override
			/**
			 * Onstopped.
			 *
			 * 
			Args:
			    encoder: Parameter encoder controls behavior.
			Returns:
			 *     Description of the return value.
			 *
			 * Raises:
			 *     Exception: When an error occurs.
			 *
			 * Side Effects:
			 *     - May mutate internal state.
			 *
			 * Code Paths:
			 *     1. If preconditions met → executes normally.
			 *     2. On error → logs and returns default.
			 */
/**
 * Onstopped.
 *
 * Args:
 *     param: Parameter controls behavior.
 *
 * Returns:
 *     Description of the return value.
 *
 * Raises:
 *     Exception: When an error occurs.
 *
 * Side Effects:
 *     - May mutate internal state.
 *
 * Code Paths:
 *     1. If preconditions met → executes normally.
 *     2. On error → logs and returns default.
 */


			public void onStopped(final MediaEncoder encoder) {
				if (DEBUG) Log.v(TAG_THREAD, "onStopped:encoder=" + encoder);
				if ((encoder instanceof MediaVideoEncoder)
					|| (encoder instanceof MediaSurfaceEncoder))
				try {
					mIsRecording = false;
					final Activity parent = mWeakParent.get();
					mWeakCameraView.get().setVideoEncoder(null);
					synchronized (mSync) {
						if (mUVCCamera != null) {
							mUVCCamera.stopCapture();
						}
					}
					final String path = encoder.getOutputPath();
					if (!TextUtils.isEmpty(path)) {
						mHandler.sendMessageDelayed(mHandler.obtainMessage(MSG_MEDIA_UPDATE, path), 1000);
					} else {
						final boolean released = (mHandler == null) || mHandler.mReleased;
						if (released || parent == null || parent.isDestroyed()) {
							handleRelease();
						}
					}
				} catch (final Exception e) {
					Log.e(TAG, "onPrepared:", e);
				}
			}
		};

		/**
		 * prepare and load shutter sound for still image capturing
		 */
		@SuppressLint("SoonBlockedPrivateApi")
		protected void loadShutterSound(final Context context) {
			// Define a default stream type
			int streamType = AudioManager.STREAM_SYSTEM;

			// Conditionally handle reflection based on the Android version
			if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S_V2) {  // API 32 is Android 12L
				try {
					// Use reflection only for older versions
					final Class<?> audioSystemClass = Class.forName("android.media.AudioSystem");
					final Field sseField = audioSystemClass.getDeclaredField("STREAM_SYSTEM_ENFORCED");
					streamType = sseField.getInt(null);
				} catch (final Exception e) {
					// If reflection fails, fall back to STREAM_SYSTEM
					streamType = AudioManager.STREAM_SYSTEM;
				}
			}

			// Clean up existing SoundPool instance if necessary
			if (mSoundPool != null) {
				try {
					mSoundPool.release();
				} catch (final Exception e) {
					// Handle the exception (optional)
				}
				mSoundPool = null;
			}

			// Initialize the SoundPool and load the shutter sound
			mSoundPool = new SoundPool(2, streamType, 0);
			mSoundId = mSoundPool.load(context, R.raw.camera_click, 1);
		}

		@Override
		/**
		 * Run.
		 *
		 * Returns:
		 *     Description of the return value.
		 *
		 * Raises:
		 *     Exception: When an error occurs.
		 *
		 * Side Effects:
		 *     - May mutate internal state.
		 *
		 * Code Paths:
		 *     1. If preconditions met → executes normally.
		 *     2. On error → logs and returns default.
		 */
/**
 * Run.
 *
 * Args:
 *     param: Parameter controls behavior.
 *
 * Returns:
 *     Description of the return value.
 *
 * Raises:
 *     Exception: When an error occurs.
 *
 * Side Effects:
 *     - May mutate internal state.
 *
 * Code Paths:
 *     1. If preconditions met → executes normally.
 *     2. On error → logs and returns default.
 */


		public void run() {
			Looper.prepare();
			AbstractUVCCameraHandler handler = null;
			try {
				final Constructor<? extends AbstractUVCCameraHandler> constructor = mHandlerClass.getDeclaredConstructor(CameraThread.class);
				handler = constructor.newInstance(this);
			} catch (final NoSuchMethodException e) {
				Log.w(TAG, e);
			} catch (final IllegalAccessException e) {
				Log.w(TAG, e);
			} catch (final InstantiationException e) {
				Log.w(TAG, e);
			} catch (final InvocationTargetException e) {
				Log.w(TAG, e);
			}
			if (handler != null) {
				synchronized (mSync) {
					mHandler = handler;
					mSync.notifyAll();
				}
				Looper.loop();
				if (mSoundPool != null) {
					mSoundPool.release();
					mSoundPool = null;
				}
				if (mHandler != null) {
					mHandler.mReleased = true;
				}
			}
			mCallbacks.clear();
			synchronized (mSync) {
				mHandler = null;
				mSync.notifyAll();
			}
		}

		private void callOnOpen() {
			for (final CameraCallback callback: mCallbacks) {
				try {
					callback.onOpen();
				} catch (final Exception e) {
					mCallbacks.remove(callback);
					Log.w(TAG, e);
				}
			}
		}

		private void callOnClose() {
			for (final CameraCallback callback: mCallbacks) {
				try {
					callback.onClose();
				} catch (final Exception e) {
					mCallbacks.remove(callback);
					Log.w(TAG, e);
				}
			}
		}

		private void callOnStartPreview() {
			for (final CameraCallback callback: mCallbacks) {
				try {
					callback.onStartPreview();
				} catch (final Exception e) {
					mCallbacks.remove(callback);
					Log.w(TAG, e);
				}
			}
		}

		private void callOnStopPreview() {
			for (final CameraCallback callback: mCallbacks) {
				try {
					callback.onStopPreview();
				} catch (final Exception e) {
					mCallbacks.remove(callback);
					Log.w(TAG, e);
				}
			}
		}

		private void callOnStartRecording() {
			for (final CameraCallback callback: mCallbacks) {
				try {
					callback.onStartRecording();
				} catch (final Exception e) {
					mCallbacks.remove(callback);
					Log.w(TAG, e);
				}
			}
		}

		private void callOnStopRecording() {
			for (final CameraCallback callback: mCallbacks) {
				try {
					callback.onStopRecording();
				} catch (final Exception e) {
					mCallbacks.remove(callback);
					Log.w(TAG, e);
				}
			}
		}

		private void callOnError(final Exception e) {
			for (final CameraCallback callback: mCallbacks) {
				try {
					callback.onError(e);
				} catch (final Exception e1) {
					mCallbacks.remove(callback);
					Log.w(TAG, e);
				}
			}
		}
	}
}
