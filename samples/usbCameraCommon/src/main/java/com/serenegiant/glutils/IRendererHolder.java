package com.serenegiant.glutils;
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

import android.graphics.SurfaceTexture;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.Surface;

/**
 * 分配描画インターフェース
 */

public interface IRendererHolder extends IRendererCommon {
	/**
	 * 実行中かどうか
	 *
	 * Returns:
	 *     Whether the running condition is true.
	 */
	public boolean isRunning();
	/**
	 * 関係するすべてのリソースを開放する。再利用できない
	 */
	public void release();

	@Nullable
	public EGLBase.IContext getContext();

	/**
	 * マスター用の映像を受け取るためのSurfaceを取得
	 *
	 * Returns:
	 *     The surface.
	 */
	public Surface getSurface();

	/**
	 * マスター用の映像を受け取るためのSurfaceTextureを取得
	 *
	 * Returns:
	 *     The surface texture.
	 */
	public SurfaceTexture getSurfaceTexture();

	/**
	 * マスター用の映像を受け取るためのマスターをチェックして無効なら再生成要求する
	 */
	public void reset();

	/**
	 * マスター映像サイズをサイズ変更要求
	 *
	 * Args:
	 *     width: The width value in pixels.
	 *     height: The height value in pixels.
	 */
	public void resize(final int width, final int height)
		throws IllegalStateException;

	/**
	 * 分配描画用のSurfaceを追加
	 * このメソッドは指定したSurfaceが追加されるか
	 * interruptされるまでカレントスレッドをブロックする。
	 *
	 * Args:
	 *     id: 普通は#hashCodeを使う.
	 *     surface,: should be one of Surface, SurfaceTexture or SurfaceHolder.
	 *     isRecordable: The is recordable value.
	 */
	public void addSurface(final int id, final Object surface,
		final boolean isRecordable)
			throws IllegalStateException, IllegalArgumentException;

	/**
	 * 分配描画用のSurfaceを追加
	 * このメソッドは指定したSurfaceが追加されるか
	 * interruptされるまでカレントスレッドをブロックする。
	 *
	 * Args:
	 *     id: 普通は#hashCodeを使う.
	 *     surface,: should be one of Surface, SurfaceTexture or SurfaceHolder.
	 *     isRecordable: The is recordable value.
	 *     maxFps: 0以下なら制限しない.
	 */
	public void addSurface(final int id, final Object surface,
		final boolean isRecordable, final int maxFps)
			throws IllegalStateException, IllegalArgumentException;

	/**
	 * 分配描画用のSurfaceを削除
	 * このメソッドは指定したSurfaceが削除されるか
	 * interruptされるまでカレントスレッドをブロックする。
	 *
	 * Args:
	 *     id: The ID value.
	 */
	public void removeSurface(final int id);

	/**
	 * 分配描画用のSurfaceを全て削除
	 * このメソッドはSurfaceが削除されるか
	 * interruptされるまでカレントスレッドをブロックする。
	 */
	public void removeSurfaceAll();

	/**
	 * 分配描画用のSurfaceを指定した色で塗りつぶす
	 *
	 * Args:
	 *     id: The ID value.
	 *     color: The color value.
	 */
	public void clearSurface(final int id, final int color);

	/**
	 * 分配描画用のSurfaceを指定した色で塗りつぶす
	 *
	 * Args:
	 *     color: The color value.
	 */
	public void clearSurfaceAll(final int color);

	/**
	 * モデルビュー変換行列をセット
	 *
	 * Args:
	 *     id: The ID value.
	 *     offset: The offset value.
	 *     matrix: offset以降に16要素以上.
	 */
	public void setMvpMatrix(final int id,
		final int offset, @NonNull final float[] matrix);

	/**
	 * 分配描画用のSurfaceへの描画が有効かどうかを取得
	 *
	 * Args:
	 *     id: The ID value.
	 *
	 * Returns:
	 *     Whether the enabled condition is true.
	 */
	public boolean isEnabled(final int id);

	/**
	 * 分配描画用のSurfaceへの描画の有効・無効を切替
	 *
	 * Args:
	 *     id: The ID value.
	 *     enable: Whether the feature is enabled.
	 */
	public void setEnabled(final int id, final boolean enable);

	/**
	 * 強制的に現在の最新のフレームを描画要求する
	 * 分配描画用Surface全てが更新されるので注意
	 */
	public void requestFrame();

	/**
	 * 追加されている分配描画用のSurfaceの数を取得
	 *
	 * Returns:
	 *     The count.
	 */
	public int getCount();

	/**
	 * 静止画を撮影する
	 * 撮影完了を待機しない
	 *
	 * Args:
	 *     path: The path value.
	 */
	public void captureStillAsync(final String path);

	/**
	 * 静止画を撮影する
	 * 撮影完了を待機しない
	 *
	 * Args:
	 *     path: The path value.
	 *     captureCompression: JPEGの圧縮率, pngの時は無視.
	 */
	public void captureStillAsync(final String path, final int captureCompression);

	/**
	 * 静止画を撮影する
	 * 撮影完了を待機する
	 *
	 * Args:
	 *     path: The path value.
	 */
	public void captureStill(final String path);

	/**
	 * 静止画を撮影する
	 * 撮影完了を待機する
	 *
	 * Args:
	 *     path: The path value.
	 *     captureCompression: JPEGの圧縮率, pngの時は無視.
	 */
	public void captureStill(final String path, final int captureCompression);

}
