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

/**
 * Define contract for views that maintain aspect ratio and handle lifecycle.
 *
 * Implementations must support setting aspect ratio and receiving pause/resume notifications.
 *
 * Thread Safety:
 *     Methods must be called on UI thread.
 *
 * Properties:
 *     None defined by interface; implementations manage internal state.
 */
public interface AspectRatioViewInterface {
    /**
     * Set aspect ratio to maintain during layout.
     *
     * Args:
     *     aspectRatio: Width divided by height; must be >= 0.
     *
     * Raises:
     *     IllegalArgumentException: If aspectRatio is negative.
     *
     * Side Effects:
     *     May trigger remeasure/layout in implementation.
     *
     * Code Paths:
     *     1. Valid ratio → updates internal state.
     *     2. Invalid ratio → throws IllegalArgumentException.
     */
    public void setAspectRatio(double aspectRatio);
    /**
     * Notify view that hosting component is pausing.
     *
     * Side Effects:
     *     Implementation may release resources.
     *
     * Code Paths:
     *     1. Always called on pause; implementation decides action.
     */
    public void onPause();
    /**
     * Notify view that hosting component is resuming.
     *
     * Side Effects:
     *     Implementation may reacquire resources.
     *
     * Code Paths:
     *     1. Always called on resume; implementation decides action.
     */
    public void onResume();
}
