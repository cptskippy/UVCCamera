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
 * Android API level checks for UVCCamera.
 *
 * Module:
 *     com.serenegiant.utils — stateless platform version gating.
 *
 * Contract:
 *     All public methods compare Build.VERSION.SDK_INT against an Android release
 *     threshold and return true when the current device meets or exceeds it.
 *     Methods have no side effects and are safe to call from any thread.
 *
 * Naming:
 *     Codename methods (isFroyo, isKitKat, ...) check the named release.
 *     Numeric aliases (isAndroid2_2, isAndroid4_4, ...) check the same threshold.
 */

public final class BuildCheck {

	private static final boolean check(final int value) {
		return (Build.VERSION.SDK_INT >= value);
	}

	/** Returns true when the device is running a current development build. */
	public static boolean isCurrentDevelopment() {
		return (Build.VERSION.SDK_INT == Build.VERSION_CODES.CUR_DEVELOPMENT);
	}

	/** Returns true when SDK_INT >= BASE (Android 1.0). */
	public static boolean isBase() {
		return check(Build.VERSION_CODES.BASE);
	}

	/** Returns true when SDK_INT >= BASE_1_1 (Android 1.1). */
	public static boolean isBase11() {
		return check(Build.VERSION_CODES.BASE_1_1);
	}

	/** Returns true when SDK_INT >= CUPCAKE (Android 1.5). */
	public static boolean isCupcake() {
		return check(Build.VERSION_CODES.CUPCAKE);
	}

	/** Alias for isCupcake. */
	public static boolean isAndroid1_5() {
		return check(Build.VERSION_CODES.CUPCAKE);
	}

	/** Returns true when SDK_INT >= DONUT (Android 1.6). */
	public static boolean isDonut() {
		return check(Build.VERSION_CODES.DONUT);
	}

	/** Alias for isDonut. */
	public static boolean isAndroid1_6() {
		return check(Build.VERSION_CODES.DONUT);
	}

	/** Returns true when SDK_INT >= ECLAIR (Android 2.0). */
	public static boolean isEclair() {
		return check(Build.VERSION_CODES.ECLAIR);
	}

	/** Alias for isEclair. */
	public static boolean isAndroid2_0() {
		return check(Build.VERSION_CODES.ECLAIR);
	}

	/** Returns true when SDK_INT >= ECLAIR_0_1 (Android 2.0.1). */
	public static boolean isEclair01() {
		return check(Build.VERSION_CODES.ECLAIR_0_1);
	}

	/** Returns true when SDK_INT >= ECLAIR_MR1 (Android 2.1). */
	public static boolean isEclairMR1() {
		return check(Build.VERSION_CODES.ECLAIR_MR1);
	}

	/** Returns true when SDK_INT >= FROYO (Android 2.2). */
	public static boolean isFroyo() {
		return check(Build.VERSION_CODES.FROYO);
	}

	/** Alias for isFroyo. */
	public static boolean isAndroid2_2() {
		return check(Build.VERSION_CODES.FROYO);
	}

	/** Returns true when SDK_INT >= GINGERBREAD (Android 2.3). */
	public static boolean isGingerBread() {
		return check(Build.VERSION_CODES.GINGERBREAD);
	}

	/** Alias for isGingerBread. */
	public static boolean isAndroid2_3() {
		return check(Build.VERSION_CODES.GINGERBREAD);
	}

	/** Returns true when SDK_INT >= GINGERBREAD_MR1 (Android 2.3.3). */
	public static boolean isGingerBreadMR1() {
		return check(Build.VERSION_CODES.GINGERBREAD_MR1);
	}

	/** Alias for isGingerBreadMR1. */
	public static boolean isAndroid2_3_3() {
		return check(Build.VERSION_CODES.GINGERBREAD_MR1);
	}

	/** Returns true when SDK_INT >= HONEYCOMB (Android 3.0). */
	public static boolean isHoneyComb() {
		return check(Build.VERSION_CODES.HONEYCOMB);
	}

	/** Alias for isHoneyComb. */
	public static boolean isAndroid3() {
		return check(Build.VERSION_CODES.HONEYCOMB);
	}

	/** Returns true when SDK_INT >= HONEYCOMB_MR1 (Android 3.1). */
	public static boolean isHoneyCombMR1() {
		return check(Build.VERSION_CODES.HONEYCOMB_MR1);
	}

	/** Alias for isHoneyCombMR1. */
	public static boolean isAndroid3_1() {
		return check(Build.VERSION_CODES.HONEYCOMB_MR1);
	}

	/** Returns true when SDK_INT >= HONEYCOMB_MR2 (Android 3.2). */
	public static boolean isHoneyCombMR2() {
		return check(Build.VERSION_CODES.HONEYCOMB_MR2);
	}

	/** Alias for isHoneyCombMR2. */
	public static boolean isAndroid3_2() {
		return check(Build.VERSION_CODES.HONEYCOMB_MR2);
	}

	/** Returns true when SDK_INT >= ICE_CREAM_SANDWICH (Android 4.0). */
	public static boolean isIcecreamSandwich() {
		return check(Build.VERSION_CODES.ICE_CREAM_SANDWICH);
	}

	/** Alias for isIcecreamSandwich. */
	public static boolean isAndroid4() {
		return check(Build.VERSION_CODES.ICE_CREAM_SANDWICH);
	}

	/** Returns true when SDK_INT >= ICE_CREAM_SANDWICH_MR1 (Android 4.0.3). */
	public static boolean isIcecreamSandwichMR1() {
		return check(Build.VERSION_CODES.ICE_CREAM_SANDWICH_MR1);
	}

	/** Alias for isIcecreamSandwichMR1. */
	public static boolean isAndroid4_0_3() {
		return check(Build.VERSION_CODES.ICE_CREAM_SANDWICH_MR1);
	}

	/** Returns true when SDK_INT >= JELLY_BEAN (Android 4.1). */
	public static boolean isJellyBean() {
		return check(Build.VERSION_CODES.JELLY_BEAN);
	}

	/** Alias for isJellyBean. */
	public static boolean isAndroid4_1() {
		return check(Build.VERSION_CODES.JELLY_BEAN);
	}

	/** Returns true when SDK_INT >= JELLY_BEAN_MR1 (Android 4.2). */
	public static boolean isJellyBeanMr1() {
		return check(Build.VERSION_CODES.JELLY_BEAN_MR1);
	}

	/** Alias for isJellyBeanMr1. */
	public static boolean isAndroid4_2() {
		return check(Build.VERSION_CODES.JELLY_BEAN_MR1);
	}

	/** Returns true when SDK_INT >= JELLY_BEAN_MR2 (Android 4.3). */
	public static boolean isJellyBeanMR2() {
		return check(Build.VERSION_CODES.JELLY_BEAN_MR2);
	}

	/** Alias for isJellyBeanMR2. */
	public static boolean isAndroid4_3() {
		return check(Build.VERSION_CODES.JELLY_BEAN_MR2);
	}

	/** Returns true when SDK_INT >= KITKAT (Android 4.4). */
	public static boolean isKitKat() {
		return check(Build.VERSION_CODES.KITKAT);
	}

	/** Alias for isKitKat. */
	public static boolean isAndroid4_4() {
		return check(Build.VERSION_CODES.KITKAT);
	}

	/** Returns true when SDK_INT >= KITKAT_WATCH (Android 4.4W). */
	public static boolean isKitKatWatch() {
		return (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT_WATCH);
	}

	/** Returns true when SDK_INT >= LOLLIPOP (Android 5.0). */
	public static boolean isL() {
		return (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP);
	}

	/** Alias for isL. */
	public static boolean isLollipop() {
		return (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP);
	}

	/** Alias for isL. */
	public static boolean isAndroid5() {
		return check(Build.VERSION_CODES.LOLLIPOP);
	}

	/** Returns true when SDK_INT >= LOLLIPOP_MR1 (Android 5.1). */
	public static boolean isLollipopMR1() {
		return (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP_MR1);
	}

	/** Returns true when SDK_INT >= M (Android 6.0). */
	public static boolean isM() {
		return check(Build.VERSION_CODES.M);
	}

	/** Alias for isM. */
	public static boolean isMarshmallow() {
		return check(Build.VERSION_CODES.M);
	}

	/** Alias for isM. */
	public static boolean isAndroid6() {
		return check(Build.VERSION_CODES.M);
	}

	/** Returns true when SDK_INT >= N (Android 7.0). */
	public static boolean isN() {
		return check(Build.VERSION_CODES.N);
	}

	/** Alias for isN. */
	public static boolean isNougat() {
		return check(Build.VERSION_CODES.N);
	}

	/** Alias for isN. */
	public static boolean isAndroid7() {
		return check(Build.VERSION_CODES.N);
	}

	/** Returns true when SDK_INT >= N_MR1 (Android 7.1). */
	public static boolean isNMR1() {
		return check(Build.VERSION_CODES.N_MR1);
	}

	/** Alias for isNMR1. */
	public static boolean isNougatMR1() {
		return check(Build.VERSION_CODES.N_MR1);
	}

	/** Returns true when SDK_INT >= O (Android 8.0). */
	public static boolean isO() {
		return check(Build.VERSION_CODES.O);
	}

	/** Alias for isO. */
	public static boolean isOreo() {
		return check(Build.VERSION_CODES.O);
	}

	/** Alias for isO. */
	public static boolean isAndroid8() {
		return check(Build.VERSION_CODES.O);
	}

	/** Returns true when SDK_INT >= O_MR1 (Android 8.1). */
	public static boolean isOMR1() {
		return check(Build.VERSION_CODES.O_MR1);
	}

	/** Alias for isOMR1. */
	public static boolean isOreoMR1() {
		return check((Build.VERSION_CODES.O_MR1));
	}
}
