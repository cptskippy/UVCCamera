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

import android.os.Build;

/**
 * Check Android API level against known releases.
 *
 * Provides stateless static helpers that compare Build.VERSION.SDK_INT
 * against Android API constants. Used throughout the codebase to gate
 * features behind minimum SDK requirements without scattering version
 * numbers.
 *
 * Lifecycle:
 *     Stateless utility — no lifecycle. Methods can be called at any time.
 *
 * Thread Safety:
 *     Thread-safe. All methods read immutable Build.VERSION state and
 *     perform only local computation.
 *
 * Properties:
 *     None — class holds no mutable state.
 *
 * State Machine:
 *     N/A — stateless utility class.
 */

public final class BuildCheck {

	private static final boolean check(final int value) {
		return (Build.VERSION.SDK_INT >= value);
	}

	/**
	 * Check if device meets Android API level for isCurrentDevelopment.
	 *
	 * Magic version number for a current development build, which has not yet turned into an official release. API=10000 /
	 *
	 * Returns:
	 *     true if SDK_INT meets the target condition; false otherwise.
	 *
	 * Side Effects:
	 *     None.
	 *
	 * Code Paths:
	 *     1. If SDK_INT satisfies the target condition → returns true
	 *     2. Else → returns false
	 */
	public static boolean isCurrentDevelopment() {
		return (Build.VERSION.SDK_INT == Build.VERSION_CODES.CUR_DEVELOPMENT);
	}

	/**
	 * Check if device meets Android API level for isBase.
	 *
	 * October 2008: The original, first, version of Android.  Yay!, API>=1 /
	 *
	 * Returns:
	 *     true if SDK_INT meets the target condition; false otherwise.
	 *
	 * Side Effects:
	 *     None.
	 *
	 * Code Paths:
	 *     1. If SDK_INT satisfies the target condition → returns true
	 *     2. Else → returns false
	 */
	public static boolean isBase() {
		return check(Build.VERSION_CODES.BASE);
	}

	/**
	 * Check if device meets Android API level for isBase11.
	 *
	 * February 2009: First Android update, officially called 1.1., API>=2 /
	 *
	 * Returns:
	 *     true if SDK_INT meets the target condition; false otherwise.
	 *
	 * Side Effects:
	 *     None.
	 *
	 * Code Paths:
	 *     1. If SDK_INT satisfies the target condition → returns true
	 *     2. Else → returns false
	 */
	public static boolean isBase11() {
		return check(Build.VERSION_CODES.BASE_1_1);
	}

	/**
	 * Check if device meets Android API level for isCupcake.
	 *
	 * May 2009: Android 1.5., API>=3 /
	 *
	 * Returns:
	 *     true if SDK_INT meets the target condition; false otherwise.
	 *
	 * Side Effects:
	 *     None.
	 *
	 * Code Paths:
	 *     1. If SDK_INT satisfies the target condition → returns true
	 *     2. Else → returns false
	 */
	public static boolean isCupcake() {
		return check(Build.VERSION_CODES.CUPCAKE);
	}

	/**
	 * Check if device meets Android API level for isAndroid1_5.
	 *
	 * May 2009: Android 1.5., API>=3 /
	 *
	 * Returns:
	 *     true if SDK_INT meets the target condition; false otherwise.
	 *
	 * Side Effects:
	 *     None.
	 *
	 * Code Paths:
	 *     1. If SDK_INT satisfies the target condition → returns true
	 *     2. Else → returns false
	 */
	public static boolean isAndroid1_5() {
		return check(Build.VERSION_CODES.CUPCAKE);
	}

	/**
	 * Check if device meets Android API level for isDonut.
	 *
	 * September 2009: Android 1.6., API>=4 /
	 *
	 * Returns:
	 *     true if SDK_INT meets the target condition; false otherwise.
	 *
	 * Side Effects:
	 *     None.
	 *
	 * Code Paths:
	 *     1. If SDK_INT satisfies the target condition → returns true
	 *     2. Else → returns false
	 */
	public static boolean isDonut() {
		return check(Build.VERSION_CODES.DONUT);
	}

	/**
	 * Check if device meets Android API level for isAndroid1_6.
	 *
	 * September 2009: Android 1.6., API>=4 /
	 *
	 * Returns:
	 *     true if SDK_INT meets the target condition; false otherwise.
	 *
	 * Side Effects:
	 *     None.
	 *
	 * Code Paths:
	 *     1. If SDK_INT satisfies the target condition → returns true
	 *     2. Else → returns false
	 */
	public static boolean isAndroid1_6() {
		return check(Build.VERSION_CODES.DONUT);
	}

	/**
	 * Check if device meets Android API level for isEclair.
	 *
	 * November 2009: Android 2.0, API>=5 /
	 *
	 * Returns:
	 *     true if SDK_INT meets the target condition; false otherwise.
	 *
	 * Side Effects:
	 *     None.
	 *
	 * Code Paths:
	 *     1. If SDK_INT satisfies the target condition → returns true
	 *     2. Else → returns false
	 */
	public static boolean isEclair() {
		return check(Build.VERSION_CODES.ECLAIR);
	}

	/**
	 * Check if device meets Android API level for isAndroid2_0.
	 *
	 * November 2009: Android 2.0, API>=5 /
	 *
	 * Returns:
	 *     true if SDK_INT meets the target condition; false otherwise.
	 *
	 * Side Effects:
	 *     None.
	 *
	 * Code Paths:
	 *     1. If SDK_INT satisfies the target condition → returns true
	 *     2. Else → returns false
	 */
	public static boolean isAndroid2_0() {
		return check(Build.VERSION_CODES.ECLAIR);
	}

	/**
	 * Check if device meets Android API level for isEclair01.
	 *
	 * December 2009: Android 2.0.1, API>=6 /
	 *
	 * Returns:
	 *     true if SDK_INT meets the target condition; false otherwise.
	 *
	 * Side Effects:
	 *     None.
	 *
	 * Code Paths:
	 *     1. If SDK_INT satisfies the target condition → returns true
	 *     2. Else → returns false
	 */
	public static boolean isEclair01() {
		return check(Build.VERSION_CODES.ECLAIR_0_1);
	}

	/**
	 * Check if device meets Android API level for isEclairMR1.
	 *
	 * January 2010: Android 2.1, API>=7 /
	 *
	 * Returns:
	 *     true if SDK_INT meets the target condition; false otherwise.
	 *
	 * Side Effects:
	 *     None.
	 *
	 * Code Paths:
	 *     1. If SDK_INT satisfies the target condition → returns true
	 *     2. Else → returns false
	 */
	public static boolean isEclairMR1() {
		return check(Build.VERSION_CODES.ECLAIR_MR1);
	}

	/**
	 * Check if device meets Android API level for isFroyo.
	 *
	 * June 2010: Android 2.2, API>=8 /
	 *
	 * Returns:
	 *     true if SDK_INT meets the target condition; false otherwise.
	 *
	 * Side Effects:
	 *     None.
	 *
	 * Code Paths:
	 *     1. If SDK_INT satisfies the target condition → returns true
	 *     2. Else → returns false
	 */
	public static boolean isFroyo() {
		return check(Build.VERSION_CODES.FROYO);
	}

	/**
	 * Check if device meets Android API level for isAndroid2_2.
	 *
	 * June 2010: Android 2.2, API>=8 /
	 *
	 * Returns:
	 *     true if SDK_INT meets the target condition; false otherwise.
	 *
	 * Side Effects:
	 *     None.
	 *
	 * Code Paths:
	 *     1. If SDK_INT satisfies the target condition → returns true
	 *     2. Else → returns false
	 */
	public static boolean isAndroid2_2() {
		return check(Build.VERSION_CODES.FROYO);
	}

	/**
	 * Check if device meets Android API level for isGingerBread.
	 *
	 * November 2010: Android 2.3, API>=9 /
	 *
	 * Returns:
	 *     true if SDK_INT meets the target condition; false otherwise.
	 *
	 * Side Effects:
	 *     None.
	 *
	 * Code Paths:
	 *     1. If SDK_INT satisfies the target condition → returns true
	 *     2. Else → returns false
	 */
	public static boolean isGingerBread() {
		return check(Build.VERSION_CODES.GINGERBREAD);
	}

	/**
	 * Check if device meets Android API level for isAndroid2_3.
	 *
	 * November 2010: Android 2.3, API>=9 /
	 *
	 * Returns:
	 *     true if SDK_INT meets the target condition; false otherwise.
	 *
	 * Side Effects:
	 *     None.
	 *
	 * Code Paths:
	 *     1. If SDK_INT satisfies the target condition → returns true
	 *     2. Else → returns false
	 */
	public static boolean isAndroid2_3() {
		return check(Build.VERSION_CODES.GINGERBREAD);
	}

	/**
	 * Check if device meets Android API level for isGingerBreadMR1.
	 *
	 * February 2011: Android 2.3.3., API>=10 /
	 *
	 * Returns:
	 *     true if SDK_INT meets the target condition; false otherwise.
	 *
	 * Side Effects:
	 *     None.
	 *
	 * Code Paths:
	 *     1. If SDK_INT satisfies the target condition → returns true
	 *     2. Else → returns false
	 */
	public static boolean isGingerBreadMR1() {
		return check(Build.VERSION_CODES.GINGERBREAD_MR1);
	}

	/**
	 * Check if device meets Android API level for isAndroid2_3_3.
	 *
	 * February 2011: Android 2.3.3., API>=10 /
	 *
	 * Returns:
	 *     true if SDK_INT meets the target condition; false otherwise.
	 *
	 * Side Effects:
	 *     None.
	 *
	 * Code Paths:
	 *     1. If SDK_INT satisfies the target condition → returns true
	 *     2. Else → returns false
	 */
	public static boolean isAndroid2_3_3() {
		return check(Build.VERSION_CODES.GINGERBREAD_MR1);
	}

	/**
	 * Check if device meets Android API level for isHoneyComb.
	 *
	 * February 2011: Android 3.0., API>=11 /
	 *
	 * Returns:
	 *     true if SDK_INT meets the target condition; false otherwise.
	 *
	 * Side Effects:
	 *     None.
	 *
	 * Code Paths:
	 *     1. If SDK_INT satisfies the target condition → returns true
	 *     2. Else → returns false
	 */
	public static boolean isHoneyComb() {
		return check(Build.VERSION_CODES.HONEYCOMB);
	}

	/**
	 * Check if device meets Android API level for isAndroid3.
	 *
	 * February 2011: Android 3.0., API>=11 /
	 *
	 * Returns:
	 *     true if SDK_INT meets the target condition; false otherwise.
	 *
	 * Side Effects:
	 *     None.
	 *
	 * Code Paths:
	 *     1. If SDK_INT satisfies the target condition → returns true
	 *     2. Else → returns false
	 */
	public static boolean isAndroid3() {
		return check(Build.VERSION_CODES.HONEYCOMB);
	}

	/**
	 * Check if device meets Android API level for isHoneyCombMR1.
	 *
	 * May 2011: Android 3.1., API>=12 /
	 *
	 * Returns:
	 *     true if SDK_INT meets the target condition; false otherwise.
	 *
	 * Side Effects:
	 *     None.
	 *
	 * Code Paths:
	 *     1. If SDK_INT satisfies the target condition → returns true
	 *     2. Else → returns false
	 */
	public static boolean isHoneyCombMR1() {
		return check(Build.VERSION_CODES.HONEYCOMB_MR1);
	}

	/**
	 * Check if device meets Android API level for isAndroid3_1.
	 *
	 * May 2011: Android 3.1., API>=12 /
	 *
	 * Returns:
	 *     true if SDK_INT meets the target condition; false otherwise.
	 *
	 * Side Effects:
	 *     None.
	 *
	 * Code Paths:
	 *     1. If SDK_INT satisfies the target condition → returns true
	 *     2. Else → returns false
	 */
	public static boolean isAndroid3_1() {
		return check(Build.VERSION_CODES.HONEYCOMB_MR1);
	}

	/**
	 * Check if device meets Android API level for isHoneyCombMR2.
	 *
	 * June 2011: Android 3.2., API>=13 /
	 *
	 * Returns:
	 *     true if SDK_INT meets the target condition; false otherwise.
	 *
	 * Side Effects:
	 *     None.
	 *
	 * Code Paths:
	 *     1. If SDK_INT satisfies the target condition → returns true
	 *     2. Else → returns false
	 */
	public static boolean isHoneyCombMR2() {
		return check(Build.VERSION_CODES.HONEYCOMB_MR2);
	}

	/**
	 * Check if device meets Android API level for isAndroid3_2.
	 *
	 * June 2011: Android 3.2., API>=13 /
	 *
	 * Returns:
	 *     true if SDK_INT meets the target condition; false otherwise.
	 *
	 * Side Effects:
	 *     None.
	 *
	 * Code Paths:
	 *     1. If SDK_INT satisfies the target condition → returns true
	 *     2. Else → returns false
	 */
	public static boolean isAndroid3_2() {
		return check(Build.VERSION_CODES.HONEYCOMB_MR2);
	}

	/**
	 * Check if device meets Android API level for isIcecreamSandwich.
	 *
	 * October 2011: Android 4.0., API>=14 /
	 *
	 * Returns:
	 *     true if SDK_INT meets the target condition; false otherwise.
	 *
	 * Side Effects:
	 *     None.
	 *
	 * Code Paths:
	 *     1. If SDK_INT satisfies the target condition → returns true
	 *     2. Else → returns false
	 */
	public static boolean isIcecreamSandwich() {
		return check(Build.VERSION_CODES.ICE_CREAM_SANDWICH);
	}

	/**
	 * Check if device meets Android API level for isAndroid4.
	 *
	 * October 2011: Android 4.0., API>=14 /
	 *
	 * Returns:
	 *     true if SDK_INT meets the target condition; false otherwise.
	 *
	 * Side Effects:
	 *     None.
	 *
	 * Code Paths:
	 *     1. If SDK_INT satisfies the target condition → returns true
	 *     2. Else → returns false
	 */
	public static boolean isAndroid4() {
		return check(Build.VERSION_CODES.ICE_CREAM_SANDWICH);
	}

	/**
	 * Check if device meets Android API level for isIcecreamSandwichMR1.
	 *
	 * December 2011: Android 4.0.3., API>=15 /
	 *
	 * Returns:
	 *     true if SDK_INT meets the target condition; false otherwise.
	 *
	 * Side Effects:
	 *     None.
	 *
	 * Code Paths:
	 *     1. If SDK_INT satisfies the target condition → returns true
	 *     2. Else → returns false
	 */
	public static boolean isIcecreamSandwichMR1() {
		return check(Build.VERSION_CODES.ICE_CREAM_SANDWICH_MR1);
	}

	/**
	 * Check if device meets Android API level for isAndroid4_0_3.
	 *
	 * December 2011: Android 4.0.3., API>=15 /
	 *
	 * Returns:
	 *     true if SDK_INT meets the target condition; false otherwise.
	 *
	 * Side Effects:
	 *     None.
	 *
	 * Code Paths:
	 *     1. If SDK_INT satisfies the target condition → returns true
	 *     2. Else → returns false
	 */
	public static boolean isAndroid4_0_3() {
		return check(Build.VERSION_CODES.ICE_CREAM_SANDWICH_MR1);
	}

	/**
	 * Check if device meets Android API level for isJellyBean.
	 *
	 * June 2012: Android 4.1., API>=16 /
	 *
	 * Returns:
	 *     true if SDK_INT meets the target condition; false otherwise.
	 *
	 * Side Effects:
	 *     None.
	 *
	 * Code Paths:
	 *     1. If SDK_INT satisfies the target condition → returns true
	 *     2. Else → returns false
	 */
	public static boolean isJellyBean() {
		return check(Build.VERSION_CODES.JELLY_BEAN);
	}

	/**
	 * Check if device meets Android API level for isAndroid4_1.
	 *
	 * June 2012: Android 4.1., API>=16 /
	 *
	 * Returns:
	 *     true if SDK_INT meets the target condition; false otherwise.
	 *
	 * Side Effects:
	 *     None.
	 *
	 * Code Paths:
	 *     1. If SDK_INT satisfies the target condition → returns true
	 *     2. Else → returns false
	 */
	public static boolean isAndroid4_1() {
		return check(Build.VERSION_CODES.JELLY_BEAN);
	}

	/**
	 * Check if device meets Android API level for isJellyBeanMr1.
	 *
	 * November 2012: Android 4.2, Moar jelly beans!, API>=17 /
	 *
	 * Returns:
	 *     true if SDK_INT meets the target condition; false otherwise.
	 *
	 * Side Effects:
	 *     None.
	 *
	 * Code Paths:
	 *     1. If SDK_INT satisfies the target condition → returns true
	 *     2. Else → returns false
	 */
	public static boolean isJellyBeanMr1() {
		return check(Build.VERSION_CODES.JELLY_BEAN_MR1);
	}

	/**
	 * Check if device meets Android API level for isAndroid4_2.
	 *
	 * November 2012: Android 4.2, Moar jelly beans!, API>=17 /
	 *
	 * Returns:
	 *     true if SDK_INT meets the target condition; false otherwise.
	 *
	 * Side Effects:
	 *     None.
	 *
	 * Code Paths:
	 *     1. If SDK_INT satisfies the target condition → returns true
	 *     2. Else → returns false
	 */
	public static boolean isAndroid4_2() {
		return check(Build.VERSION_CODES.JELLY_BEAN_MR1);
	}

	/**
	 * Check if device meets Android API level for isJellyBeanMR2.
	 *
	 * July 2013: Android 4.3, the revenge of the beans., API>=18 /
	 *
	 * Returns:
	 *     true if SDK_INT meets the target condition; false otherwise.
	 *
	 * Side Effects:
	 *     None.
	 *
	 * Code Paths:
	 *     1. If SDK_INT satisfies the target condition → returns true
	 *     2. Else → returns false
	 */
	public static boolean isJellyBeanMR2() {
		return check(Build.VERSION_CODES.JELLY_BEAN_MR2);
	}

	/**
	 * Check if device meets Android API level for isAndroid4_3.
	 *
	 * July 2013: Android 4.3, the revenge of the beans., API>=18 /
	 *
	 * Returns:
	 *     true if SDK_INT meets the target condition; false otherwise.
	 *
	 * Side Effects:
	 *     None.
	 *
	 * Code Paths:
	 *     1. If SDK_INT satisfies the target condition → returns true
	 *     2. Else → returns false
	 */
	public static boolean isAndroid4_3() {
		return check(Build.VERSION_CODES.JELLY_BEAN_MR2);
	}

	/**
	 * Check if device meets Android API level for isKitKat.
	 *
	 * October 2013: Android 4.4, KitKat, another tasty treat., API>=19 /
	 *
	 * Returns:
	 *     true if SDK_INT meets the target condition; false otherwise.
	 *
	 * Side Effects:
	 *     None.
	 *
	 * Code Paths:
	 *     1. If SDK_INT satisfies the target condition → returns true
	 *     2. Else → returns false
	 */
	public static boolean isKitKat() {
		return check(Build.VERSION_CODES.KITKAT);
	}

	/**
	 * Check if device meets Android API level for isAndroid4_4.
	 *
	 * October 2013: Android 4.4, KitKat, another tasty treat., API>=19 /
	 *
	 * Returns:
	 *     true if SDK_INT meets the target condition; false otherwise.
	 *
	 * Side Effects:
	 *     None.
	 *
	 * Code Paths:
	 *     1. If SDK_INT satisfies the target condition → returns true
	 *     2. Else → returns false
	 */
	public static boolean isAndroid4_4() {
		return check(Build.VERSION_CODES.KITKAT);
	}

	/**
	 * Check if device meets Android API level for isKitKatWatch.
	 *
	 * Android 4.4W: KitKat for watches, snacks on the run., API>=20 /
	 *
	 * Returns:
	 *     true if SDK_INT meets the target condition; false otherwise.
	 *
	 * Side Effects:
	 *     None.
	 *
	 * Code Paths:
	 *     1. If SDK_INT satisfies the target condition → returns true
	 *     2. Else → returns false
	 */
	public static boolean isKitKatWatch() {
		return (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT_WATCH);
	}

	/**
	 * Check if device meets Android API level for isL.
	 *
	 * Lollipop.  A flat one with beautiful shadows.  But still tasty., API>=21 /
	 *
	 * Returns:
	 *     true if SDK_INT meets the target condition; false otherwise.
	 *
	 * Side Effects:
	 *     None.
	 *
	 * Code Paths:
	 *     1. If SDK_INT satisfies the target condition → returns true
	 *     2. Else → returns false
	 */
	public static boolean isL() {
		return (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP);
	}

	/**
	 * Check if device meets Android API level for isLollipop.
	 *
	 * Lollipop.  A flat one with beautiful shadows.  But still tasty., API>=21 /
	 *
	 * Returns:
	 *     true if SDK_INT meets the target condition; false otherwise.
	 *
	 * Side Effects:
	 *     None.
	 *
	 * Code Paths:
	 *     1. If SDK_INT satisfies the target condition → returns true
	 *     2. Else → returns false
	 */
	public static boolean isLollipop() {
		return (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP);
	}

	/**
	 * Check if device meets Android API level for isAndroid5.
	 *
	 * Lollipop.  A flat one with beautiful shadows.  But still tasty., API>=21 /
	 *
	 * Returns:
	 *     true if SDK_INT meets the target condition; false otherwise.
	 *
	 * Side Effects:
	 *     None.
	 *
	 * Code Paths:
	 *     1. If SDK_INT satisfies the target condition → returns true
	 *     2. Else → returns false
	 */
	public static boolean isAndroid5() {
		return check(Build.VERSION_CODES.LOLLIPOP);
	}

	/**
	 * Check if device meets Android API level for isLollipopMR1.
	 *
	 * Lollipop with an extra sugar coating on the outside!, API>=22 /
	 *
	 * Returns:
	 *     true if SDK_INT meets the target condition; false otherwise.
	 *
	 * Side Effects:
	 *     None.
	 *
	 * Code Paths:
	 *     1. If SDK_INT satisfies the target condition → returns true
	 *     2. Else → returns false
	 */
	public static boolean isLollipopMR1() {
		return (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP_MR1);
	}

	/**
	 * Check if device meets Android API level for isM.
	 *
	 * Marshmallow.  A flat one with beautiful shadows.  But still tasty., API>=23 /
	 *
	 * Returns:
	 *     true if SDK_INT meets the target condition; false otherwise.
	 *
	 * Side Effects:
	 *     None.
	 *
	 * Code Paths:
	 *     1. If SDK_INT satisfies the target condition → returns true
	 *     2. Else → returns false
	 */
	public static boolean isM() {
		return check(Build.VERSION_CODES.M);
	}

	/**
	 * Check if device meets Android API level for isMarshmallow.
	 *
	 * Marshmallow.  A flat one with beautiful shadows.  But still tasty., API>=23 /
	 *
	 * Returns:
	 *     true if SDK_INT meets the target condition; false otherwise.
	 *
	 * Side Effects:
	 *     None.
	 *
	 * Code Paths:
	 *     1. If SDK_INT satisfies the target condition → returns true
	 *     2. Else → returns false
	 */
	public static boolean isMarshmallow() {
		return check(Build.VERSION_CODES.M);
	}

	/**
	 * Check if device meets Android API level for isAndroid6.
	 *
	 * Marshmallow.  A flat one with beautiful shadows.  But still tasty., API>=23 /
	 *
	 * Returns:
	 *     true if SDK_INT meets the target condition; false otherwise.
	 *
	 * Side Effects:
	 *     None.
	 *
	 * Code Paths:
	 *     1. If SDK_INT satisfies the target condition → returns true
	 *     2. Else → returns false
	 */
	public static boolean isAndroid6() {
		return check(Build.VERSION_CODES.M);
	}

	/**
	 * Check if device meets Android API level for isN.
	 *
	 * 虫歯の元, API >= 24 /
	 *
	 * Returns:
	 *     true if SDK_INT meets the target condition; false otherwise.
	 *
	 * Side Effects:
	 *     None.
	 *
	 * Code Paths:
	 *     1. If SDK_INT satisfies the target condition → returns true
	 *     2. Else → returns false
	 */
	public static boolean isN() {
		return check(Build.VERSION_CODES.N);
	}

	/**
	 * Check if device meets Android API level for isNougat.
	 *
	 * 歯にくっつくやつ, API >= 24 /
	 *
	 * Returns:
	 *     true if SDK_INT meets the target condition; false otherwise.
	 *
	 * Side Effects:
	 *     None.
	 *
	 * Code Paths:
	 *     1. If SDK_INT satisfies the target condition → returns true
	 *     2. Else → returns false
	 */
	public static boolean isNougat() {
		return check(Build.VERSION_CODES.N);
	}
	/**
	 * Check if device meets Android API level for isAndroid7.
	 *
	 * API >= 24 /
	 *
	 * Returns:
	 *     true if SDK_INT meets the target condition; false otherwise.
	 *
	 * Side Effects:
	 *     None.
	 *
	 * Code Paths:
	 *     1. If SDK_INT satisfies the target condition → returns true
	 *     2. Else → returns false
	 */
	public static boolean isAndroid7() {
		return check(Build.VERSION_CODES.N);
	}
	
	/**
	 * Check if device meets Android API level for isNMR1.
	 *
	 * API>=25 /
	 *
	 * Returns:
	 *     true if SDK_INT meets the target condition; false otherwise.
	 *
	 * Side Effects:
	 *     None.
	 *
	 * Code Paths:
	 *     1. If SDK_INT satisfies the target condition → returns true
	 *     2. Else → returns false
	 */
	public static boolean isNMR1() {
		return check(Build.VERSION_CODES.N_MR1);
	}
	
	/**
	 * Check if device meets Android API level for isNougatMR1.
	 *
	 * API>=25 /
	 *
	 * Returns:
	 *     true if SDK_INT meets the target condition; false otherwise.
	 *
	 * Side Effects:
	 *     None.
	 *
	 * Code Paths:
	 *     1. If SDK_INT satisfies the target condition → returns true
	 *     2. Else → returns false
	 */
	public static boolean isNougatMR1() {
		return check(Build.VERSION_CODES.N_MR1);
	}

	/**
	 * Check if device meets Android API level for isO.
	 *
	 * おれおれぇー API>=26 /
	 *
	 * Returns:
	 *     true if SDK_INT meets the target condition; false otherwise.
	 *
	 * Side Effects:
	 *     None.
	 *
	 * Code Paths:
	 *     1. If SDK_INT satisfies the target condition → returns true
	 *     2. Else → returns false
	 */
	public static boolean isO() {
		return check(Build.VERSION_CODES.O);
	}
	
	/**
	 * Check if device meets Android API level for isOreo.
	 *
	 * おれおれぇー API>=26 /
	 *
	 * Returns:
	 *     true if SDK_INT meets the target condition; false otherwise.
	 *
	 * Side Effects:
	 *     None.
	 *
	 * Code Paths:
	 *     1. If SDK_INT satisfies the target condition → returns true
	 *     2. Else → returns false
	 */
	public static boolean isOreo() {
		return check(Build.VERSION_CODES.O);
	}
	
	/**
	 * Check if device meets Android API level for isAndroid8.
	 *
	 * おれおれぇー API>=26 /
	 *
	 * Returns:
	 *     true if SDK_INT meets the target condition; false otherwise.
	 *
	 * Side Effects:
	 *     None.
	 *
	 * Code Paths:
	 *     1. If SDK_INT satisfies the target condition → returns true
	 *     2. Else → returns false
	 */
	public static boolean isAndroid8() {
		return check(Build.VERSION_CODES.O);
	}
	
	/**
	 * Check if device meets Android API level for isOMR1.
	 *
	 * おれおれぇー API>=27 /
	 *
	 * Returns:
	 *     true if SDK_INT meets the target condition; false otherwise.
	 *
	 * Side Effects:
	 *     None.
	 *
	 * Code Paths:
	 *     1. If SDK_INT satisfies the target condition → returns true
	 *     2. Else → returns false
	 */
	public static boolean isOMR1() {
		return check(Build.VERSION_CODES.O_MR1);
	}

	/**
	 * Check if device meets Android API level for isOreoMR1.
	 *
	 * おれおれぇー MR1 API>=27 /
	 *
	 * Returns:
	 *     true if SDK_INT meets the target condition; false otherwise.
	 *
	 * Side Effects:
	 *     None.
	 *
	 * Code Paths:
	 *     1. If SDK_INT satisfies the target condition → returns true
	 *     2. Else → returns false
	 */
	public static boolean isOreoMR1() {
		return check((Build.VERSION_CODES.O_MR1));
	}
}
