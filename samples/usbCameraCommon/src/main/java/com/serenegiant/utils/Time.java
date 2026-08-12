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

import android.annotation.SuppressLint;
import android.os.SystemClock;
/**
 * Manages Time functionality.
 *
 * Responsibility: Provides core Time operations for the USB camera stack.
 *
 * Lifecycle: Instantiated → configured → used → released.
 *
 * Thread Safety: Methods are synchronized where applicable; otherwise not thread-safe.
 * State Machine:
 *   Initialized → Active → Released
 *   Error (from any active state)
 *
 * Example:
 *     // Example usage of Time
 */
/**
 * Manages Time functionality.
 *
 * Responsibility: Provides core Time operations for the USB camera stack.
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
 *     // Example usage of Time
 */



public class Time {

	public static boolean prohibitElapsedRealtimeNanos = true;

	private static Time sTime;
	static {
		reset();
	}
/**
 * Nanotime.
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


	public static long nanoTime() {
		return sTime.timeNs();
	}
/**
 * Reset.
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

	
	public static void reset() {
		if (!prohibitElapsedRealtimeNanos && BuildCheck.isJellyBeanMr1()) {
			sTime = new TimeJellyBeanMr1();
		} else {
			sTime = new Time();
		}
	}
	
	private Time() {
	}
	
	@SuppressLint("NewApi")
	private static class TimeJellyBeanMr1 extends Time {
		/**
		 * Timens.
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
 * Timens.
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


		public long timeNs() {
			return SystemClock.elapsedRealtimeNanos();
		}
	}
	
	protected long timeNs() {
		return System.nanoTime();
	}
}
