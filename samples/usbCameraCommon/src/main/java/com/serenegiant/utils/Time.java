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
 * Provide a monotonic nanosecond clock for interval measurements.
 *
 * Wraps either SystemClock.elapsedRealtimeNanos() (API 17+) or
 * System.nanoTime(), chosen once by reset(). The static flag
 * prohibitElapsedRealtimeNanos (default true) forces the System.nanoTime()
 * path so behavior stays identical across API levels.
 */



public class Time {

	public static boolean prohibitElapsedRealtimeNanos = true;

	private static Time sTime;
	static {
		reset();
	}
	/**
	 * Return the current time on the selected monotonic nanosecond clock.
	 *
	 * Returns:
	 *     Nanoseconds on the clock chosen by reset(). Use differences of
	 *     successive calls to measure elapsed intervals.
	 */


	public static long nanoTime() {
		return sTime.timeNs();
	}
	/**
	 * Select the clock implementation and rebuild the static instance.
	 *
	 * Side Effects:
	 *     - Replaces the static sTime instance.
	 *
	 * Code Paths:
	 *     1. If prohibitElapsedRealtimeNanos is false and API >= 17 →
	 *        TimeJellyBeanMr1 (SystemClock.elapsedRealtimeNanos).
	 *     2. Otherwise → base Time (System.nanoTime).
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
		 * Return nanoseconds from SystemClock.elapsedRealtimeNanos().
		 *
		 * Returns:
		 *     Nanoseconds since boot, excluding deep sleep.
		 */


		public long timeNs() {
			return SystemClock.elapsedRealtimeNanos();
		}
	}

	protected long timeNs() {
		return System.nanoTime();
	}
}
