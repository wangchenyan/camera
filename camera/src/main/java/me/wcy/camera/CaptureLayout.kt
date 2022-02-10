package me.wcy.camera

import android.animation.AnimatorSet
import android.animation.ValueAnimator
import android.annotation.TargetApi
import android.content.Context
import android.os.Build
import android.util.AttributeSet
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.animation.LinearInterpolator
import android.widget.FrameLayout
import android.widget.ImageView

/**
 * Created by hzwangchenyan on 2017/6/14.
 */
class CaptureLayout @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {
    private val captureRetryLayout: View
    private val btnCapture: ImageView
    private val btnRetry: ImageView
    private val btnClose: ImageView
    private var mClickListener: ClickListener? = null
    private var isExpanded = false

    interface ClickListener {
        fun onCaptureClick()
        fun onOkClick()
        fun onRetryClick()
        fun onCloseClick()
    }

    init {
        isClickable = true
        LayoutInflater.from(context).inflate(R.layout.camera_capture_layout, this, true)
        captureRetryLayout = findViewById(R.id.camera_capture_retry_layout)
        btnCapture = findViewById(R.id.camera_capture)
        btnRetry = findViewById(R.id.camera_retry)
        btnClose = findViewById(R.id.camera_close)
        btnCapture.setOnClickListener {
            if (!isExpanded) {
                mClickListener?.onCaptureClick()
            } else {
                mClickListener?.onOkClick()
            }
        }
        btnRetry.setOnClickListener {
            mClickListener?.onRetryClick()
        }
        btnRetry.isEnabled = false
        btnClose.setOnClickListener {
            mClickListener?.onCloseClick()
        }
    }

    fun setClickListener(listener: ClickListener?) {
        mClickListener = listener
    }

    fun setExpanded(expanded: Boolean) {
        if (isExpanded == expanded) {
            return
        }
        isExpanded = expanded
        if (isExpanded) {
            expand()
        } else {
            fold()
        }
    }

    private fun expand() {
        btnCapture.setImageResource(R.drawable.ic_camera_done)
        btnRetry.isEnabled = true
        btnClose.visibility = GONE
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
            playExpandAnimation()
        } else {
            val captureParams = btnCapture.layoutParams as LayoutParams
            captureParams.width = CameraUtils.dp2px(context, 80f)
            captureParams.height = CameraUtils.dp2px(context, 80f)
            captureParams.gravity = Gravity.END
            val layoutParams = captureRetryLayout.layoutParams as LayoutParams
            layoutParams.width = CameraUtils.dp2px(context, 280f)
            captureRetryLayout.requestLayout()
        }
    }

    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    private fun playExpandAnimation() {
        val scaleAnimator = ValueAnimator.ofInt(
            CameraUtils.dp2px(context, 60f),
            CameraUtils.dp2px(context, 80f)
        )
        scaleAnimator.interpolator = LinearInterpolator()
        scaleAnimator.duration = 100
        scaleAnimator.addUpdateListener { animation ->
            val value = animation.animatedValue as Int
            val captureParams = btnCapture.layoutParams as LayoutParams
            captureParams.width = value
            captureParams.height = value
            captureParams.gravity = Gravity.CENTER
            btnCapture.requestLayout()
        }
        val transAnimator = ValueAnimator.ofInt(
            CameraUtils.dp2px(context, 80f),
            CameraUtils.dp2px(context, 280f)
        )
        transAnimator.interpolator = LinearInterpolator()
        transAnimator.duration = 200
        transAnimator.addUpdateListener { animation ->
            val value = animation.animatedValue as Int
            val captureParams = btnCapture.layoutParams as LayoutParams
            captureParams.gravity = Gravity.END
            val layoutParams = captureRetryLayout.layoutParams as LayoutParams
            layoutParams.width = value
            captureRetryLayout.requestLayout()
        }
        val animatorSet = AnimatorSet()
        animatorSet.playSequentially(scaleAnimator, transAnimator)
        animatorSet.start()
    }

    private fun fold() {
        btnCapture.setImageResource(0)
        btnRetry.isEnabled = false
        btnClose.visibility = VISIBLE
        val length = CameraUtils.dp2px(context, 60f)
        val captureParams = btnCapture.layoutParams as LayoutParams
        captureParams.width = length
        captureParams.height = length
        captureParams.gravity = Gravity.CENTER
        val layoutParams = captureRetryLayout.layoutParams as LayoutParams
        layoutParams.width = CameraUtils.dp2px(context, 80f)
        captureRetryLayout.requestLayout()
    }
}