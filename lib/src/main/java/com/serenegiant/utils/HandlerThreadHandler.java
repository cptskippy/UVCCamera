package com.serenegiant.utils;
/*
 * libcommon
 * utility/helper classes for myself
 *
 * Copyright (c) 2014-2018 saki t_saki@serenegiant.com
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
*/

import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;

/**
 * Provide Handler bound to a dedicated HandlerThread.
 *
 * Creates a HandlerThread on demand and returns a Handler that posts
 * messages to that thread. Used to offload work from the main thread
 * without requiring callers to manage thread lifecycle.
 *
 * Lifecycle:
 *     HandlerThread is created and started by factory methods. The
 *     HandlerThread runs until explicitly quit; this class does not
 *     manage termination.
 *
 * Thread Safety:
 *     HandlerThread creation is not synchronized. Each factory call
 *     creates an independent thread. Handler methods are thread-safe
 *     per Android Handler contract.
 *
 * Properties:
 *     TAG: Log tag constant for HandlerThreadHandler.
 *
 * State Machine:
 *     N/A — factory class with no mutable state.
 */

public class HandlerThreadHandler extends Handler {
	private static final String TAG = "HandlerThreadHandler";

	/**
	 * Create HandlerThreadHandler with default tag.
	 *
	 * Delegates to createHandler(TAG) to create a HandlerThread using the
	 * class log tag as the thread name.
	 *
	 * Returns:
	 *     New HandlerThreadHandler bound to a newly started HandlerThread.
	 *
	 * Side Effects:
	 *     - Creates a HandlerThread with name TAG
	 *     - Starts the HandlerThread
	 *
	 * Code Paths:
	 *     1. Calls createHandler(TAG) → returns handler
	 */
	public static final HandlerThreadHandler createHandler() {
		return createHandler(TAG);
	}

	/**
	 * Create HandlerThreadHandler with named HandlerThread.
	 *
	 * Creates a HandlerThread with the specified name, starts it, and
	 * returns a Handler bound to its Looper.
	 *
	 * Args:
	 *     name: Thread name for the HandlerThread. Used for debugging.
	 *
	 * Returns:
	 *     New HandlerThreadHandler bound to the newly started HandlerThread.
	 *
	 * Side Effects:
	 *     - Creates a HandlerThread with the given name
	 *     - Starts the HandlerThread
	 *
	 * Code Paths:
	 *     1. Creates HandlerThread(name) → starts → returns HandlerThreadHandler
	 */
	public static final HandlerThreadHandler createHandler(final String name) {
		final HandlerThread thread = new HandlerThread(name);
		thread.start();
		return new HandlerThreadHandler(thread.getLooper());
	}

	/**
	 * Create HandlerThreadHandler with default tag and callback.
	 *
	 * Delegates to createHandler(TAG, callback) to create a HandlerThread
	 * using the class log tag and the provided Handler.Callback.
	 *
	 * Args:
	 *     callback: Handler.Callback invoked for unhandled messages.
	 *
	 * Returns:
	 *     New HandlerThreadHandler bound to a newly started HandlerThread.
	 *
	 * Side Effects:
	 *     - Creates a HandlerThread with name TAG
	 *     - Starts the HandlerThread
	 *
	 * Code Paths:
	 *     1. Calls createHandler(TAG, callback) → returns handler
	 */
	public static final HandlerThreadHandler createHandler(final Callback callback) {
		return createHandler(TAG, callback);
	}

	/**
	 * Create HandlerThreadHandler with named HandlerThread and callback.
	 *
	 * Creates a HandlerThread with the specified name, starts it, and
	 * returns a Handler bound to its Looper with the provided callback.
	 *
	 * Args:
	 *     name: Thread name for the HandlerThread. Used for debugging.
	 *     callback: Handler.Callback invoked for unhandled messages.
	 *
	 * Returns:
	 *     New HandlerThreadHandler bound to the newly started HandlerThread.
	 *
	 * Side Effects:
	 *     - Creates a HandlerThread with the given name
	 *     - Starts the HandlerThread
	 *
	 * Code Paths:
	 *     1. Creates HandlerThread(name) → starts → returns HandlerThreadHandler(looper, callback)
	 */
	public static final HandlerThreadHandler createHandler(final String name, final Callback callback) {
		final HandlerThread thread = new HandlerThread(name);
		thread.start();
		return new HandlerThreadHandler(thread.getLooper(), callback);
	}

	private HandlerThreadHandler(final Looper looper) {
		super(looper);
	}

	private HandlerThreadHandler(final Looper looper, final Callback callback) {
		super(looper, callback);
	}

}
