package me.wcy.camera

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Build
import android.util.AttributeSet
import android.util.Log
import android.view.*
import android.view.GestureDetector.SimpleOnGestureListener
import android.view.ScaleGestureDetector.OnScaleGestureListener
import android.view.animation.Animation
import android.view.animation.RotateAnimation
import android.widget.FrameLayout
import android.widget.ImageView
import androidx.core.view.isVisible
import me.wcy.camera.CaptureLayout.ClickListener

/**
 * Created by hzwangchenyan on 2017/6/13.
 */
@SuppressLint("ClickableViewAccessibility")
class CameraView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(
    context, attrs, defStyleAttr
), SurfaceHolder.Callback, ClickListener, SensorEventListener {
    private val mSurfaceView: SurfaceView
    private val mFocusView: View
    private val mSwitchWrapper: View
    private val mSwitchView: View
    private val mPictureView: ImageView
    private val mCaptureLayout: CaptureLayout
    private val mSensorManager: SensorManager
    private var mCameraListener: CameraListener? = null
    private var mPicture: Bitmap? = null
    private var mSensorRotation = 0
    private var isSurfaceCreated = false

    interface CameraListener {
        fun onCapture(bitmap: Bitmap)
        fun onCameraClose()
        fun onCameraError(th: Throwable?)
    }

    init {
        setBackgroundColor(Color.BLACK)
        LayoutInflater.from(context).inflate(R.layout.camera_layout, this, true)
        mSurfaceView = findViewById(R.id.camera_surface)
        mFocusView = findViewById(R.id.camera_focus)
        mSwitchWrapper = findViewById(R.id.camera_switch_wrapper)
        mSwitchView = findViewById(R.id.camera_switch)
        mPictureView = findViewById(R.id.camera_picture_preview)
        mCaptureLayout = findViewById(R.id.camera_capture_layout)

        CameraManager.init(context)
        mSensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager

        mSurfaceView.setOnTouchListener { v, event ->
            mScaleGestureDetector.onTouchEvent(event)
            if (mScaleGestureDetector.isInProgress) {
                true
            } else {
                mGestureDetector.onTouchEvent(event)
            }
        }
        // fix `java.lang.RuntimeException: startPreview failed` on api 10
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.GINGERBREAD_MR1) {
            mSurfaceView.holder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS)
        }
        mSurfaceView.holder.addCallback(this)
        mCaptureLayout.setClickListener(this)
        mSwitchWrapper.isVisible = CameraManager.hasMultiCamera()
        mSwitchWrapper.setOnClickListener {
            CameraManager.switchCamera { success ->
                if (!success) {
                    mCameraListener?.onCameraError(Exception("switch camera failed"))
                }
            }
        }
    }

    private val mGestureDetector = GestureDetector(context, object : SimpleOnGestureListener() {
        override fun onDown(e: MotionEvent): Boolean {
            Log.d(TAG, "onDown")
            if (!CameraManager.isOpened()) {
                return false
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
                mFocusView.removeCallbacks(timeoutRunnable)
                mFocusView.postDelayed(timeoutRunnable, 1500)
                mFocusView.visibility = VISIBLE
                val focusParams = mFocusView.layoutParams as LayoutParams
                focusParams.leftMargin = e.x.toInt() - focusParams.width / 2
                focusParams.topMargin = e.y.toInt() - focusParams.height / 2
                mFocusView.layoutParams = focusParams
                val scaleX = ObjectAnimator.ofFloat(mFocusView, "scaleX", 1f, 0.5f)
                scaleX.duration = 300
                val scaleY = ObjectAnimator.ofFloat(mFocusView, "scaleY", 1f, 0.5f)
                scaleY.duration = 300
                val alpha = ObjectAnimator.ofFloat(
                    mFocusView, "alpha",
                    1f, 0.3f, 1f, 0.3f, 1f, 0.3f, 1f
                )
                alpha.duration = 600
                val animatorSet = AnimatorSet()
                animatorSet.play(scaleX).with(scaleY).before(alpha)
                animatorSet.start()
                val focusCallback: (Boolean) -> Unit = { success ->
                    if (mFocusView.tag === this && mFocusView.visibility == VISIBLE) {
                        mFocusView.visibility = INVISIBLE
                    }
                }
                mFocusView.tag = focusCallback
                CameraManager.setFocus(e.x, e.y, focusCallback)
            }
            return CameraManager.hasMultiCamera()
        }

        /**
         * 前置摄像头可能不会回调对焦成功，因此需要手动隐藏对焦框
         */
        private val timeoutRunnable = Runnable {
            if (mFocusView.visibility == VISIBLE) {
                mFocusView.visibility = INVISIBLE
            }
        }

        override fun onDoubleTap(e: MotionEvent): Boolean {
            Log.d(TAG, "onDoubleTap")
            CameraManager.switchCamera { success ->
                if (!success) {
                    mCameraListener?.onCameraError(Exception("switch camera failed"))
                }
            }
            return true
        }
    })

    private var mScaleGestureDetector =
        ScaleGestureDetector(context, object : OnScaleGestureListener {
            private var mLastSpan = 0f
            override fun onScale(detector: ScaleGestureDetector): Boolean {
                val span = detector.currentSpan - mLastSpan
                mLastSpan = detector.currentSpan
                if (CameraManager.isOpened()) {
                    CameraManager.setZoom(span)
                    return true
                }
                return false
            }

            override fun onScaleBegin(detector: ScaleGestureDetector): Boolean {
                Log.d(TAG, "onScaleBegin")
                mLastSpan = detector.currentSpan
                return CameraManager.isOpened()
            }

            override fun onScaleEnd(detector: ScaleGestureDetector) {
                Log.d(TAG, "onScaleEnd")
            }
        })

    fun setCameraListener(listener: CameraListener?) {
        mCameraListener = listener
    }

    fun onResume() {
        Log.d(TAG, "onResume")
        if (!CameraManager.isOpened() && isSurfaceCreated) {
            CameraManager.open { success ->
                if (!success) {
                    mCameraListener?.onCameraError(Exception("open camera failed"))
                }
            }
        }
        mSensorManager.registerListener(
            this,
            mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER),
            SensorManager.SENSOR_DELAY_NORMAL
        )
    }

    fun onPause() {
        Log.d(TAG, "onPause")
        if (CameraManager.isOpened()) {
            CameraManager.close()
        }
        mSensorManager.unregisterListener(this)
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        Log.d(TAG, "onDetachedFromWindow")
        mCameraListener = null
    }

    override fun surfaceCreated(holder: SurfaceHolder) {
        Log.d(TAG, "surfaceCreated")
        isSurfaceCreated = true
    }

    override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
        Log.d(TAG, "surfaceChanged")
        CameraManager.setSurfaceHolder(holder, width, height)
        if (CameraManager.isOpened()) {
            CameraManager.close()
        }
        CameraManager.open { success ->
            if (!success) {
                mCameraListener?.onCameraError(Exception("open camera failed"))
            }
        }
    }

    override fun surfaceDestroyed(holder: SurfaceHolder) {
        Log.d(TAG, "surfaceDestroyed")
        isSurfaceCreated = false
        CameraManager.setSurfaceHolder(null, 0, 0)
    }

    override fun onCaptureClick() {
        CameraManager.takePicture { result ->
            if (result.isSuccess) {
                mSurfaceView.isVisible = false
                mSwitchWrapper.isVisible = false
                mPictureView.isVisible = true
                mPicture = result.getOrNull()!!
                mPictureView.setImageBitmap(mPicture)
                mCaptureLayout.setExpanded(true)
            } else {
                // 不知道什么原因拍照失败，重新预览
                onRetryClick()
            }
        }
    }

    override fun onOkClick() {
        if (mPicture != null) {
            mCameraListener?.onCapture(mPicture!!)
        }
    }

    override fun onRetryClick() {
        mPicture = null
        mCaptureLayout.setExpanded(false)
        mSurfaceView.isVisible = true
        mSwitchWrapper.isVisible = CameraManager.hasMultiCamera()
        mPictureView.setImageBitmap(null)
        mPictureView.isVisible = false
    }

    override fun onCloseClick() {
        if (mCameraListener != null) {
            mCameraListener!!.onCameraClose()
        }
    }

    override fun onSensorChanged(event: SensorEvent) {
        if (event.sensor.type != Sensor.TYPE_ACCELEROMETER) {
            return
        }
        val rotation = CameraUtils.calculateSensorRotation(event.values[0], event.values[1])
        if (rotation >= 0 && rotation != mSensorRotation) {
            Log.d(TAG, "screen rotation changed from $mSensorRotation to $rotation")
            playRotateAnimation(mSensorRotation, rotation)
            CameraManager.setSensorRotation(rotation)
            mSensorRotation = rotation
        }
    }

    override fun onAccuracyChanged(sensor: Sensor, accuracy: Int) {}

    private fun playRotateAnimation(oldRotation: Int, newRotation: Int) {
        if (!CameraManager.hasMultiCamera()) {
            return
        }
        var diff = newRotation - oldRotation
        if (diff > 180) {
            diff -= 360
        } else if (diff < -180) {
            diff += 360
        }
        val rotate = RotateAnimation(
            (-oldRotation).toFloat(),
            (-oldRotation - diff).toFloat(),
            Animation.RELATIVE_TO_SELF,
            0.5f,
            Animation.RELATIVE_TO_SELF,
            0.5f
        )
        rotate.duration = 200
        rotate.fillAfter = true
        mSwitchView.startAnimation(rotate)
    }

    companion object {
        private const val TAG = "CameraView"
    }
}