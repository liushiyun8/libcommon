package com.serenegiant.glutils;

import androidx.annotation.Nullable;

/**
 * 現在のスレッド上にGLコンテキストを生成する
 */
public class GLContext implements EGLConst {
	private static final boolean DEBUG = true;	// set false on production
	private static final String TAG = GLContext.class.getSimpleName();

	private final Object mSync = new Object();
	private final int mMaxClientVersion;
	@Nullable
	final EGLBase.IContext mSharedContext;
	private final int mFlags;
	@Nullable
	private EGLBase mEgl = null;
	@Nullable
	private ISurface mEglMasterSurface;
	private long mGLThreadId;

	/**
	 * コンストラクタ
	 * @param maxClientVersion 通常は2か3
	 * @param sharedContext 共有コンテキストの親となるIContext, nullなら自分がマスターのコンテキストとなる
	 * @param flags
	 */
	public GLContext(final int maxClientVersion,
		@Nullable final EGLBase.IContext sharedContext, final int flags) {

		mMaxClientVersion = maxClientVersion;
		mSharedContext = sharedContext;
		mFlags = flags;
	}

	@Override
	protected void finalize() throws Throwable {
		try {
			release();
		} finally {
			super.finalize();
		}
	}

	/**
	 * 関連するリソースを破棄する
	 * コンストラクタを呼び出したスレッド上で実行すること
	 */
	public void release() {
		synchronized (mSync) {
			mGLThreadId = 0;
			if (mEglMasterSurface != null) {
				mEglMasterSurface.release();
				mEglMasterSurface = null;
			}
			if (mEgl != null) {
				mEgl.release();
				mEgl = null;
			}
		}
	}

	public void initialize() {
		if ((mSharedContext == null)
			|| (mSharedContext instanceof EGLBase.IContext)) {

			final int stencilBits
				= (mFlags & EGL_FLAG_STENCIL_1BIT) == EGL_FLAG_STENCIL_1BIT ? 1
					: ((mFlags & EGL_FLAG_STENCIL_8BIT) == EGL_FLAG_STENCIL_8BIT ? 8 : 0);
			mEgl = EGLBase.createFrom(mMaxClientVersion, mSharedContext,
				(mFlags & EGL_FLAG_DEPTH_BUFFER) == EGL_FLAG_DEPTH_BUFFER,
				stencilBits,
				(mFlags & EGL_FLAG_RECORDABLE) == EGL_FLAG_RECORDABLE);
		}
		if (mEgl != null) {
			mEglMasterSurface = mEgl.createOffscreen(1, 1);
			mEglMasterSurface.makeCurrent();
			mGLThreadId = Thread.currentThread().getId();
		} else {
			throw new RuntimeException("failed to create EglCore");
		}
	}

	/**
	 * EGLBaseを取得
	 * @return
	 * @throws IllegalStateException
	 */
	public EGLBase getEgl() throws IllegalStateException {
		synchronized (mSync) {
			if (mEgl != null) {
				return mEgl;
			} else {
				throw new IllegalStateException();
			}
		}
	}

	/**
	 * EGLBase生成時のIConfigを取得
	 * @return
	 * @throws IllegalStateException
	 */
	public EGLBase.IConfig getConfig() throws IllegalStateException {
		synchronized (mSync) {
			if (mEgl != null) {
				return mEgl.getConfig();
			} else {
				throw new IllegalStateException();
			}
		}
	}

	/**
	 * EGLBase生成時のflagsを取得
	 * @return
	 */
	public int getFlags() {
		return mFlags;
	}

	/**
	 * IContextを取得
	 * @return
	 * @throws IllegalStateException
	 */
	public EGLBase.IContext getContext() throws IllegalStateException {
		if (mEgl != null) {
			mEgl.getContext();
		} else {
			throw new IllegalStateException();
		}
		return mEgl != null ? mEgl.getContext() : null;
	}

	/**
	 * マスターコンテキストを選択
	 * @throws IllegalStateException
	 */
	public void makeDefault() throws IllegalStateException {
		synchronized (mSync) {
			if (mEgl != null) {
				mEglMasterSurface.makeCurrent();
			} else {
				throw new IllegalStateException();
			}
		}
	}

	public long getGLThreadId() {
		synchronized (mSync) {
			return mGLThreadId;
		}
	}

	/**
	 * GLES2以上で初期化されているかどうか
	 * @return
	 */
	public boolean isGLES2() {
		return getGlVersion() > 1;
	}

	/**
	 * GLES3以上で初期化されているかどうか
	 * @return
	 */
	public boolean isGLES3() {
		return getGlVersion() > 2;
	}

	/**
	 * GLコンテキストのバージョンを取得
	 * @return
	 */
	public int getGlVersion() {
		synchronized (mSync) {
			return  (mEgl != null) ? mEgl.getGlVersion() : 0;
		}
	}
}