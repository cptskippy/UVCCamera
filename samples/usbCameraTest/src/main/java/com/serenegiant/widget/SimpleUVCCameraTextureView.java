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

package com.serenegiant.widget;

import android.content.Context;
import android.util.AttributeSet;
import android.view.TextureView;

/**
 * Display camera preview in a TextureView while maintaining a requested aspect ratio.
 *
 * Extends TextureView to provide aspect-ratio-aware measurement for UVC camera preview.
 * Lifecycle hooks onResume/onPause are no-ops; actual camera control is external.
 *
 * Properties:
 *     mRequestedAspect: Desired width/height ratio; -1.0 means no constraint.
 *
 * Thread Safety:
 *     Must be used on UI thread. Measurement and layout occur on UI thread.
 *
 * State Machine:
 *     Uninitialized → Measured → LaidOut
 *     Aspect ratio changes trigger remeasure.
 */
public class SimpleUVCCameraTextureView extends TextureView	// API >= 14
	implements AspectRatioViewInterface {

    private double mRequestedAspect = -1.0;

	/**
	 * Create SimpleUVCCameraTextureView with default style.
	 *
	 * Args:
	 *     context: Context used for inflating view resources.
	 *
	 * Side Effects:
	 *     Delegates to three-argument constructor.
	 *
	 * Code Paths:
	 *     1. Always delegates to constructor with null attrs and defStyle 0.
	 */
	public SimpleUVCCameraTextureView(final Context context) {
		this(context, null, 0);
	}

	/**
	 * Create SimpleUVCCameraTextureView from XML attributes.
	 *
	 * Args:
	 *     context: Context used for inflating view resources.
	 *     attrs: AttributeSet from XML layout.
	 *
	 * Side Effects:
	 *     Delegates to three-argument constructor.
	 *
	 * Code Paths:
	 *     1. Always delegates to constructor with defStyle 0.
	 */
	public SimpleUVCCameraTextureView(final Context context, final AttributeSet attrs) {
		this(context, attrs, 0);
	}

	/**
	 * Create SimpleUVCCameraTextureView with explicit style.
	 *
	 * Args:
	 *     context: Context used for inflating view resources.
	 *     attrs: AttributeSet from XML layout; may be null.
	 *     defStyle: Default style resource identifier.
	 *
	 * Side Effects:
	 *     Calls super constructor to initialize TextureView.
	 *
	 * Code Paths:
	 *     1. Always initializes TextureView with provided parameters.
	 */
	public SimpleUVCCameraTextureView(final Context context, final AttributeSet attrs, final int defStyle) {
		super(context, attrs, defStyle);
	}

	/**
	 * Handle resume event from hosting component.
	 *
	 * No-op implementation; camera control is managed externally.
	 *
	 * Side Effects:
	 *     None.
	 *
	 * Code Paths:
	 *     1. Always returns without action.
	 */
	@Override
	public void onResume() {
	}

	/**
	 * Handle pause event from hosting component.
	 *
	 * No-op implementation; camera control is managed externally.
	 *
	 * Side Effects:
	 *     None.
	 *
	 * Code Paths:
	 *     1. Always returns without action.
	 */
	@Override
	public void onPause() {
	}

	/**
	 * Set desired aspect ratio for view measurement.
	 *
	 * Updates requested aspect ratio and triggers layout if changed.
	 *
	 * Args:
	 *     aspectRatio: Width divided by height; must be >= 0.
	 *
	 * Raises:
	 *     IllegalArgumentException: If aspectRatio is negative.
	 *
	 * Side Effects:
	 *     Updates mRequestedAspect and requests layout when ratio changes.
	 *
	 * Code Paths:
	 *     1. If aspectRatio < 0 → throws IllegalArgumentException.
	 *     2. If aspectRatio equals current → no action.
	 *     3. If aspectRatio differs → updates field and requests layout.
	 */
	@Override
    public void setAspectRatio(final double aspectRatio) {
        if (aspectRatio < 0) {
            throw new IllegalArgumentException();
        }
        if (mRequestedAspect != aspectRatio) {
            mRequestedAspect = aspectRatio;
            requestLayout();
        }
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {

		if (mRequestedAspect > 0) {
			int initialWidth = MeasureSpec.getSize(widthMeasureSpec);
			int initialHeight = MeasureSpec.getSize(heightMeasureSpec);

			final int horizPadding = getPaddingLeft() + getPaddingRight();
			final int vertPadding = getPaddingTop() + getPaddingBottom();
			initialWidth -= horizPadding;
			initialHeight -= vertPadding;

			final double viewAspectRatio = (double)initialWidth / initialHeight;
			final double aspectDiff = mRequestedAspect / viewAspectRatio - 1;

			if (Math.abs(aspectDiff) > 0.01) {
				if (aspectDiff > 0) {
					// width priority decision
					initialHeight = (int) (initialWidth / mRequestedAspect);
				} else {
					// height priority decison
					initialWidth = (int) (initialHeight * mRequestedAspect);
				}
				initialWidth += horizPadding;
				initialHeight += vertPadding;
				widthMeasureSpec = MeasureSpec.makeMeasureSpec(initialWidth, MeasureSpec.EXACTLY);
				heightMeasureSpec = MeasureSpec.makeMeasureSpec(initialHeight, MeasureSpec.EXACTLY);
			}
		}

        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
    }

}
