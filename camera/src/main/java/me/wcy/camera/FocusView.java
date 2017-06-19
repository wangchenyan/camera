package me.wcy.camera;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;

/**
 * Created by hzwangchenyan on 2017/6/15.
 */
public class FocusView extends View {
    private Paint mPaint;
    private int mStrokeWidth = 2;
    private int mLineLength = 12;

    public FocusView(Context context) {
        this(context, null);
    }

    public FocusView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public FocusView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        mStrokeWidth = CameraUtils.dp2px(getContext(), mStrokeWidth);
        mLineLength = CameraUtils.dp2px(getContext(), mLineLength);
        mPaint = new Paint();
        mPaint.setAntiAlias(true);
        mPaint.setColor(0xFF00CC00);
        mPaint.setStyle(Paint.Style.STROKE);
        mPaint.setStrokeWidth(mStrokeWidth);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        int offset = mStrokeWidth / 2;
        canvas.drawRect(offset, offset, getWidth() - offset, getHeight() - offset, mPaint);
        canvas.drawLine(0, getHeight() / 2, mLineLength, getHeight() / 2, mPaint);
        canvas.drawLine(getWidth() / 2, 0, getWidth() / 2, mLineLength, mPaint);
        canvas.drawLine(getWidth(), getHeight() / 2, getWidth() - mLineLength, getHeight() / 2, mPaint);
        canvas.drawLine(getWidth() / 2, getHeight(), getWidth() / 2, getHeight() - mLineLength, mPaint);
    }
}
