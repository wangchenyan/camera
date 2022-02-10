package me.wcy.camera

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View

/**
 * Created by hzwangchenyan on 2017/6/15.
 */
class FocusView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {
    private val mPaint: Paint
    private var mStrokeWidth = 2
    private var mLineLength = 12

    init {
        mStrokeWidth = CameraUtils.dp2px(context, mStrokeWidth.toFloat())
        mLineLength = CameraUtils.dp2px(context, mLineLength.toFloat())
        mPaint = Paint()
        mPaint.isAntiAlias = true
        mPaint.color = -0xff3400
        mPaint.style = Paint.Style.STROKE
        mPaint.strokeWidth = mStrokeWidth.toFloat()
    }

    override fun onDraw(canvas: Canvas) {
        val offset = mStrokeWidth / 2
        canvas.drawRect(
            offset.toFloat(),
            offset.toFloat(),
            (width - offset).toFloat(),
            (height - offset).toFloat(),
            mPaint
        )
        canvas.drawLine(
            0f,
            (height / 2).toFloat(),
            mLineLength.toFloat(),
            (height / 2).toFloat(),
            mPaint
        )
        canvas.drawLine(
            (width / 2).toFloat(),
            0f,
            (width / 2).toFloat(),
            mLineLength.toFloat(),
            mPaint
        )
        canvas.drawLine(
            width.toFloat(),
            (height / 2).toFloat(),
            (width - mLineLength).toFloat(),
            (height / 2).toFloat(),
            mPaint
        )
        canvas.drawLine(
            (width / 2).toFloat(),
            height.toFloat(),
            (width / 2).toFloat(),
            (height - mLineLength).toFloat(),
            mPaint
        )
    }
}