package me.wcy.camera

import android.annotation.SuppressLint
import android.annotation.TargetApi
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.graphics.Point
import android.hardware.Camera
import android.hardware.Camera.CameraInfo
import android.os.Build
import android.os.Handler
import android.os.HandlerThread
import android.os.Looper
import android.util.Log
import android.view.Surface
import android.view.SurfaceHolder
import android.view.WindowManager

/**
 * Created by hzwangchenyan on 2017/6/13.
 */
@SuppressLint("StaticFieldLeak")
object CameraManager {
    private enum class State {
        STATE_IDLE, STATE_OPENED, STATE_SHOOTING
    }

    private const val TAG = "CameraManager"

    private var mCameraIdBack = -1
    private var mCameraIdFront = -1
    private var mContext: Context? = null
    private var mCamera: Camera? = null
    private val mUiHandler: Handler
    private val mThreadHandler: Handler
    private var mSurfaceHolder: SurfaceHolder? = null
    private val mSurfaceSize = Point()
    private var mCameraId = 0
    private var mState = State.STATE_IDLE
    private var mSensorRotation = 0

    init {
        mUiHandler = Handler(Looper.getMainLooper())
        val handlerThread = HandlerThread("$TAG-Thread")
        handlerThread.start()
        mThreadHandler = Handler(handlerThread.looper)
        findCameras()
    }

    fun init(context: Context) {
        mContext = context.applicationContext
        mCameraId = -1
    }

    fun setSurfaceHolder(holder: SurfaceHolder?, width: Int, height: Int) {
        mSurfaceHolder = holder
        mSurfaceSize[height] = width
    }

    fun open(callback: (Boolean) -> Unit) {
        checkInitialize()
        mThreadHandler.post(object : SafeRunnable() {
            override fun runSafely() {
                openImmediate()
                val success = mState == State.STATE_OPENED
                mUiHandler.post { callback.invoke(success) }
            }
        })
    }

    private fun openImmediate() {
        closeImmediate()
        if (mSurfaceHolder == null) {
            return
        }
        if (mCameraId < 0 && mCameraIdBack >= 0) {
            mCameraId = mCameraIdBack
        }
        if (mCameraId < 0) {
            return
        }
        try {
            mCamera = Camera.open(mCameraId) ?: return
            val parameters = mCamera!!.parameters
            CameraUtils.setPreviewParams(mSurfaceSize, parameters)
            mCamera!!.parameters = parameters
            mCamera!!.setDisplayOrientation(getDisplayOrientation())
            mCamera!!.setPreviewDisplay(mSurfaceHolder)
            mCamera!!.startPreview()
            setState(State.STATE_OPENED)
        } catch (th: Throwable) {
            Log.e(TAG, "open camera failed", th)
        }
    }

    fun isOpened(): Boolean = mCamera != null && mState != State.STATE_IDLE

    fun hasMultiCamera(): Boolean = mCameraIdBack >= 0 && mCameraIdFront >= 0

    @TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
    fun setFocus(x: Float, y: Float, callback: (Boolean) -> Unit) {
        checkInitialize()
        mThreadHandler.post(object : SafeRunnable() {
            override fun runSafely() {
                if (mState != State.STATE_OPENED) {
                    return
                }
                mCamera!!.cancelAutoFocus()
                val parameters = mCamera!!.parameters
                CameraUtils.setFocusArea(mSurfaceSize, parameters, x, y)
                mCamera!!.parameters = parameters
                mCamera!!.autoFocus { success, camera ->
                    Log.d(TAG, "auto focus result: $success")
                    mUiHandler.post { callback.invoke(success) }
                }
            }
        })
    }

    fun setZoom(span: Float) {
        checkInitialize()
        mThreadHandler.post(object : SafeRunnable() {
            override fun runSafely() {
                if (mState != State.STATE_OPENED) {
                    return
                }
                val parameters = mCamera!!.parameters
                if (parameters.isZoomSupported) {
                    val valid = CameraUtils.setZoom(mSurfaceSize, parameters, span)
                    if (valid) {
                        mCamera!!.parameters = parameters
                    }
                }
            }
        })
    }

    fun switchCamera(callback: (Boolean) -> Unit) {
        checkInitialize()
        mThreadHandler.post(object : SafeRunnable() {
            override fun runSafely() {
                if (!hasMultiCamera()) {
                    return
                }
                mCameraId = when (mCameraId) {
                    mCameraIdBack -> mCameraIdFront
                    mCameraIdFront -> mCameraIdBack
                    else -> mCameraIdBack
                }
                openImmediate()
                val success = mState == State.STATE_OPENED
                mUiHandler.post { callback.invoke(success) }
            }
        })
    }

    fun setSensorRotation(rotation: Int) {
        mSensorRotation = rotation
    }

    fun takePicture(callback: (Result<Bitmap>) -> Unit) {
        checkInitialize()
        mThreadHandler.post(object : SafeRunnable() {
            override fun runSafely() {
                if (mState != State.STATE_OPENED) {
                    mUiHandler.post { callback.invoke(Result.failure(IllegalStateException("camera not open"))) }
                    return
                }
                setState(State.STATE_SHOOTING)
                mCamera!!.takePicture(null, null) { data, camera ->
                    closeImmediate()
                    if (data != null && data.isNotEmpty()) {
                        val bitmap = BitmapFactory.decodeByteArray(data, 0, data.size)
                        val matrix = Matrix()
                        var rotation = getDisplayOrientation() + mSensorRotation
                        if (mCameraId == mCameraIdBack) {
                            matrix.setRotate(rotation.toFloat())
                        } else {
                            rotation = (360 - rotation) % 360
                            matrix.setRotate(rotation.toFloat())
                            matrix.postScale(-1f, 1f)
                        }
                        val result = Bitmap.createBitmap(
                            bitmap,
                            0,
                            0,
                            bitmap.width,
                            bitmap.height,
                            matrix,
                            true
                        )
                        mUiHandler.post { callback.invoke(Result.success(result)) }
                    } else {
                        mUiHandler.post { callback.invoke(Result.failure(IllegalStateException("capture data is null or empty"))) }
                    }
                }
            }
        })
    }

    fun close() {
        mThreadHandler.post(object : SafeRunnable() {
            override fun runSafely() {
                closeImmediate()
            }
        })
    }

    private fun checkInitialize() {
        checkNotNull(mContext) { "camera manager is not initialized" }
    }

    private fun closeImmediate() {
        mCamera?.stopPreview()
        mCamera?.release()
        mCamera = null
        if (mState != State.STATE_IDLE) {
            setState(State.STATE_IDLE)
        }
    }

    private fun setState(state: State) {
        Log.d(TAG, "change state from $mState to $state")
        mState = state
    }

    private fun findCameras() {
        val info = CameraInfo()
        for (i in 0 until Camera.getNumberOfCameras()) {
            Camera.getCameraInfo(i, info)
            if (info.facing == CameraInfo.CAMERA_FACING_BACK) {
                mCameraIdBack = info.facing
            } else if (info.facing == CameraInfo.CAMERA_FACING_FRONT) {
                mCameraIdFront = info.facing
            }
        }
    }// back-facing

    // compensate the mirror
    private fun getDisplayOrientation(): Int {
        val info = CameraInfo()
        Camera.getCameraInfo(mCameraId, info)
        val windowManager = mContext!!.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        val rotation = windowManager.defaultDisplay.rotation
        var degrees = 0
        when (rotation) {
            Surface.ROTATION_0 -> degrees = 0
            Surface.ROTATION_90 -> degrees = 90
            Surface.ROTATION_180 -> degrees = 180
            Surface.ROTATION_270 -> degrees = 270
        }
        var result: Int
        if (info.facing == CameraInfo.CAMERA_FACING_FRONT) {
            result = (info.orientation + degrees) % 360
            result = (360 - result) % 360 // compensate the mirror
        } else {  // back-facing
            result = (info.orientation - degrees + 360) % 360
        }
        return result
    }

    private abstract class SafeRunnable : Runnable {
        override fun run() {
            try {
                runSafely()
            } catch (th: Throwable) {
                Log.e(TAG, "camera error", th)
            }
        }

        abstract fun runSafely()
    }
}