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

import java.util.Locale;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * Represents a video size and frame interval configuration for UVC devices.
 *
 * Encapsulates width, height, format type, frame type, and frame rate intervals
 * as reported by native UVC/libuvc. Used for selecting preview/capture resolutions
 * and frame rates. Objects are created, configured with intervals, and may be
 * parceled for IPC. Lifecycle: create → configure intervals → update frame rates →
 * use → parcel/unparcel.
 *
 * Properties:
 *     type: Native uvc_raw_format_t value; 9999 indicates still image.
 *     frame_type: Native raw_frame_t value for androUSB; unused by libuvc.
 *     index: Format index from device enumeration.
 *     width: Frame width in pixels.
 *     height: Frame height in pixels.
 *     frameIntervalType: Interval description type; -1 = none, 0 = min/max/step, >0 = count.
 *     frameIntervalIndex: Current selected frame rate index into fps array.
 *     intervals: Raw interval values in 100ns units; null if not applicable.
 *     fps: Computed frame rates in frames per second derived from intervals.
 *
 * Thread Safety:
 *     Not thread-safe. Instances should be confined to a single thread or externally synchronized.
 *     Parcelable methods assume single-threaded access.
 */
public class Size implements Parcelable {
	//
	/**
	 * Native uvc_raw_format_t value for the video format. 9999 indicates still image.
	 */
	public int type;
	/**
	 * Native raw_frame_t value used by androUSB. Not used by libuvc.
	 */
	public int frame_type;
	/**
	 * Format index from device enumeration.
	 */
	public int index;
	/**
	 * Frame width in pixels.
	 */
	public int width;
	/**
	 * Frame height in pixels.
	 */
	public int height;
	/**
	 * Interval description type. -1 = none, 0 = min/max/step range, >0 = explicit count.
	 */
	public int frameIntervalType;
	/**
	 * Current selected frame rate index into the fps array.
	 */
	public int frameIntervalIndex;
	/**
	 * Raw interval values in 100ns units. Null if intervals are not applicable.
	 */
	public int[] intervals;
	/**
	 * Computed frame rates in frames per second derived from intervals.
	 */
	public float[] fps;
	private String frameRates;

	/**
	 * Create a Size with basic dimensions and no frame intervals.
	 *
	 * Args:
	 *     _type: Native raw_format_t value; 9999 indicates still image.
	 *     _frame_type: Native raw_frame_t value for androUSB.
	 *     _index: Format index.
	 *     _width: Width in pixels.
	 *     _height: Height in pixels.
	 *
	 * Side Effects:
	 *     - Initializes fields to provided values.
	 *     - Sets frameIntervalType to -1 and intervals to null.
	 *     - Calls updateFrameRate() to compute fps and frameRates.
	 */
	public Size(final int _type, final int _frame_type, final int _index, final int _width, final int _height) {
		type = _type;
		frame_type = _frame_type;
		index = _index;
		width = _width;
		height = _height;
		frameIntervalType = -1;
		frameIntervalIndex = 0;
		intervals = null;
		updateFrameRate();
	}

	/**
	 * Create a Size with min/max/step frame interval range.
	 *
	 * Args:
	 *     _type: Native raw_format_t value; 9999 indicates still image.
	 *     _frame_type: Native raw_frame_t value.
	 *     _index: Format index.
	 *     _width: Width in pixels.
	 *     _height: Height in pixels.
	 *     _min_intervals: Minimum interval in 100ns units.
	 *     _max_intervals: Maximum interval in 100ns units.
	 *     _step: Step between intervals in 100ns units.
	 *
	 * Side Effects:
	 *     - Initializes fields and builds intervals array with min/max/step.
	 *     - Calls updateFrameRate() to populate fps.
	 *
	 * Code Paths:
	 *     1. Always builds intervals[0]=min, intervals[1]=max, intervals[2]=step.
	 *     2. frameIntervalType set to 0 to indicate range mode.
	 */
	public Size(final int _type, final int _frame_type, final int _index, final int _width, final int _height, final int _min_intervals, final int _max_intervals, final int _step) {
		type = _type;
		frame_type = _frame_type;
		index = _index;
		width = _width;
		height = _height;
		frameIntervalType = 0;
		frameIntervalIndex = 0;
		intervals = new int[3];
		intervals[0] = _min_intervals;
		intervals[1] = _max_intervals;
		intervals[2] = _step;
		updateFrameRate();
	}

	/**
	 * Create a Size with explicit frame intervals.
	 *
	 * Args:
	 *     _type: Native raw_format_t value; 9999 indicates still image.
	 *     _frame_type: Native raw_frame_t value.
	 *     _index: Format index.
	 *     _width: Width in pixels.
	 *     _height: Height in pixels.
	 *     _intervals: Array of interval values in 100ns units; null or empty disables intervals.
	 *
	 * Side Effects:
	 *     - Copies intervals if present; sets frameIntervalType to array length.
	 *     - Calls updateFrameRate().
	 *
	 * Code Paths:
	 *     1. If _intervals is non-null and length >0 → frameIntervalType = length, intervals copied.
	 *     2. Otherwise → frameIntervalType = -1, intervals = null.
	 */
	public Size(final int _type, final int _frame_type, final int _index, final int _width, final int _height, final int[] _intervals) {
		type = _type;
		frame_type = _frame_type;
		index = _index;
		width = _width;
		height = _height;
		final int n = _intervals != null ? _intervals.length : -1;
		if (n > 0) {
			frameIntervalType = n;
			intervals = new int[n];
			System.arraycopy(_intervals, 0, intervals, 0, n);
		} else {
			frameIntervalType = -1;
			intervals = null;
		}
		frameIntervalIndex = 0;
		updateFrameRate();
	}

	/**
	 * Create a Size by copying another Size instance.
	 *
	 * Args:
	 *     other: Source Size to copy.
	 *
	 * Side Effects:
	 *     - Copies all fields including deep copy of intervals.
	 *     - Calls updateFrameRate() to recompute fps and frameRates.
	 */
	public Size(final Size other) {
		type = other.type;
		frame_type = other.frame_type;
		index = other.index;
		width = other.width;
		height = other.height;
		frameIntervalType = other.frameIntervalType;
		frameIntervalIndex = other.frameIntervalIndex;
		final int n = other.intervals != null ? other.intervals.length : -1;
		if (n > 0) {
			intervals = new int[n];
			System.arraycopy(other.intervals, 0, intervals, 0, n);
		} else {
			intervals = null;
		}
		updateFrameRate();
	}

	private Size(final Parcel source) {
		// Read order must match the write order in writeToParcel
		type = source.readInt();
		frame_type = source.readInt();
		index = source.readInt();
		width = source.readInt();
		height = source.readInt();
		frameIntervalType = source.readInt();
		frameIntervalIndex = source.readInt();
		if (frameIntervalType >= 0) {
			if (frameIntervalType > 0) {
				intervals = new int[frameIntervalType];
			} else {
				intervals = new int[3];
			}
			source.readIntArray(intervals);
		} else {
			intervals = null;
		}
		updateFrameRate();
	}

	/**
	 * Replace this Size's contents with another Size.
	 *
	 * Args:
	 *     other: Source Size to copy from; null is ignored.
	 *
	 * Returns:
	 *     This Size instance for chaining.
	 *
	 * Side Effects:
	 *     - Overwrites all fields with values from other.
	 *     - Deep copies intervals array.
	 *     - Calls updateFrameRate().
	 *
	 * Code Paths:
	 *     1. If other is null → returns this unchanged.
	 *     2. If other is non-null → copies fields, updates frame rates.
	 */
	public Size set(final Size other) {
		if (other != null) {
			type = other.type;
			frame_type = other.frame_type;
			index = other.index;
			width = other.width;
			height = other.height;
			frameIntervalType = other.frameIntervalType;
			frameIntervalIndex = other.frameIntervalIndex;
			final int n = other.intervals != null ? other.intervals.length : -1;
			if (n > 0) {
				intervals = new int[n];
				System.arraycopy(other.intervals, 0, intervals, 0, n);
			} else {
				intervals = null;
			}
			updateFrameRate();
		}
		return this;
	}

	/**
	 * Get the current frame rate for the selected interval index.
	 *
	 * Returns:
	 *     Frame rate in frames per second for frameIntervalIndex.
	 *
	 * Raises:
	 *     IllegalStateException: If fps is not ready or frameIntervalIndex is out of bounds.
	 *
	 * Code Paths:
	 *     1. If frameIntervalIndex is within fps bounds → returns fps[frameIntervalIndex].
	 *     2. Otherwise → throws IllegalStateException.
	 */
	public float getCurrentFrameRate() throws IllegalStateException {
		final int n = fps != null ? fps.length : 0;
		if ((frameIntervalIndex >= 0) && (frameIntervalIndex < n)) {
			return fps[frameIntervalIndex];
		}
		throw new IllegalStateException("unknown frame rate or not ready");
	}

	/**
	 * Select the frame rate closest to the requested value.
	 *
	 * Args:
	 *     frameRate: Desired frame rate in frames per second.
	 *
	 * Side Effects:
	 *     - Updates frameIntervalIndex to first fps entry <= frameRate.
	 *
	 * Code Paths:
	 *     1. Iterates fps array; sets index to first entry <= frameRate.
	 *     2. If no entry satisfies condition → frameIntervalIndex becomes -1.
	 */
	public void setCurrentFrameRate(final float frameRate) {
		// Select the closest supported frame rate
		int index = -1;
		final int n = fps != null ? fps.length : 0;
		for (int i = 0; i < n; i++) {
			if (fps[i] <= frameRate) {
				index = i;
				break;
			}
		}
		frameIntervalIndex = index;
	}

	/**
	 * Describe the contents for Parcelable.
	 *
	 * Returns:
	 *     0 indicating no special objects.
	 */
	@Override
	public int describeContents() {
		return 0;
	}

	/**
	 * Write this Size to a Parcel.
	 *
	 * Args:
	 *     dest: Parcel to write into.
	 *     flags: Parcel flags.
	 *
	 * Side Effects:
	 *     - Writes fields to dest in defined order.
	 */
	@Override
	public void writeToParcel(final Parcel dest, final int flags) {
		dest.writeInt(type);
		dest.writeInt(frame_type);
		dest.writeInt(index);
		dest.writeInt(width);
		dest.writeInt(height);
		dest.writeInt(frameIntervalType);
		dest.writeInt(frameIntervalIndex);
		if (intervals != null) {
			dest.writeIntArray(intervals);
		}
	}

	/**
	 * Recompute fps array and frameRates string from intervals.
	 *
	 * Side Effects:
	 *     - Populates fps array based on frameIntervalType.
	 *     - Updates frameRates string representation.
	 *     - Resets frameIntervalIndex to 0 if out of bounds.
	 *
	 * Code Paths:
	 *     1. If frameIntervalType >0 → fps[i] = 10,000,000 / intervals[i].
	 *     2. If frameIntervalType ==0 → generate fps from min/max/step range.
	 *     3. If step <=0 → generate fps by incrementing 1.0 fps.
	 *     4. On exception → fps set to null.
	 */
	public void updateFrameRate() {
		final int n = frameIntervalType;
		if (n > 0) {
			fps = new float[n];
			for (int i = 0; i < n; i++) {
				final float _fps = fps[i] = 10000000.0f / intervals[i];
			}
		} else if (n == 0) {
			try {
				final int min = Math.min(intervals[0], intervals[1]);
				final int max = Math.max(intervals[0], intervals[1]);
				final int step = intervals[2];
				if (step > 0) {
					int m = 0;
					for (int i = min; i <= max; i+= step) { m++; }
					fps = new float[m];
					m = 0;
					for (int i = min; i <= max; i+= step) {
						final float _fps = fps[m++] = 10000000.0f / i;
					}
				} else {
					final float max_fps = 10000000.0f / min;
					int m = 0;
					for (float fps = 10000000.0f / min; fps <= max_fps; fps += 1.0f) { m++; }
					fps = new float[m];
					m = 0;
					for (float fps = 10000000.0f / min; fps <= max_fps; fps += 1.0f) {
						this.fps[m++] = fps;
					}
				}
			} catch (final Exception e) {
				// Ignore malformed interval data where min/max are unexpectedly zero
				fps = null;
			}
		}
		final int m = fps != null ? fps.length : 0;
		final StringBuilder sb = new StringBuilder();
		sb.append("[");
		for (int i = 0; i < m; i++) {
			sb.append(String.format(Locale.US, "%4.1f", fps[i]));
			if (i < m-1) {
				sb.append(",");
			}
		}
		sb.append("]");
		frameRates = sb.toString();
		if (frameIntervalIndex > m) {
			frameIntervalIndex = 0;
		}
	}

	/**
	 * Return a human-readable representation of this Size.
	 *
	 * Returns:
	 *     String containing width x height @ frame rate with type, frame, index, and frame rates.
	 *
	 * Side Effects:
	 *     - Calls getCurrentFrameRate(); swallows exceptions and uses 0.0f on error.
	 */
	@Override
	public String toString() {
		float frame_rate = 0.0f;
		try {
			frame_rate = getCurrentFrameRate();
		} catch (final Exception e) {
		}
		return String.format(Locale.US, "Size(%dx%d@%4.1f,type:%d,frame:%d,index:%d,%s)", width, height, frame_rate, type, frame_type, index, frameRates);
	}

	/**
	 * Parcelable creator for Size instances.
	 */
	public static final Creator<Size> CREATOR = new Parcelable.Creator<Size>() {
		@Override
		public Size createFromParcel(final Parcel source) {
			return new Size(source);
		}
		@Override
		public Size[] newArray(final int size) {
			return new Size[size];
		}
	};
}
