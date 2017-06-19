package me.wcy.camera;

import android.animation.AnimatorSet;
import android.animation.ValueAnimator;
import android.annotation.TargetApi;
import android.content.Context;
import android.os.Build;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.animation.LinearInterpolator;
import android.widget.FrameLayout;
import android.widget.ImageView;

/**
 * Created by hzwangchenyan on 2017/6/14.
 */
public class CaptureLayout extends FrameLayout implements View.OnClickListener {
    private ImageView btnCapture;
    private ImageView btnRetry;
    private ImageView btnClose;

    private Listener mListener;
    private boolean isExpanded;

    public interface Listener {
        void onCaptureClick();

        void onOkClick();

        void onCancelClick();

        void onCloseClick();
    }

    public CaptureLayout(Context context) {
        this(context, null);
    }

    public CaptureLayout(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public CaptureLayout(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        setClickable(true);
        LayoutInflater.from(getContext()).inflate(R.layout.camera_capture_layout, this, true);
        btnCapture = (ImageView) findViewById(R.id.camera_capture);
        btnRetry = (ImageView) findViewById(R.id.camera_retry);
        btnClose = (ImageView) findViewById(R.id.camera_close);

        btnCapture.setOnClickListener(this);
        btnRetry.setOnClickListener(this);
        btnRetry.setEnabled(false);
        btnClose.setOnClickListener(this);
    }

    public void setListener(Listener listener) {
        mListener = listener;
    }

    public void setExpanded(boolean expanded) {
        if (isExpanded == expanded) {
            return;
        }

        isExpanded = expanded;
        if (isExpanded) {
            expand();
        } else {
            fold();
        }
    }

    @Override
    public void onClick(View v) {
        if (v == btnCapture) {
            if (mListener != null) {
                if (!isExpanded) {
                    mListener.onCaptureClick();
                } else {
                    mListener.onOkClick();
                }
            }
        } else if (v == btnRetry) {
            if (mListener != null) {
                mListener.onCancelClick();
            }
        } else if (v == btnClose) {
            if (mListener != null) {
                mListener.onCloseClick();
            }

        }
    }

    private void expand() {
        btnCapture.setImageResource(R.drawable.ic_camera_done);
        btnRetry.setEnabled(true);
        btnClose.setVisibility(GONE);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
            playExpandAnimation();
        } else {
            LayoutParams captureParams = (LayoutParams) btnCapture.getLayoutParams();
            captureParams.width = CameraUtils.dp2px(getContext(), 80);
            captureParams.height = CameraUtils.dp2px(getContext(), 80);
            captureParams.leftMargin = CameraUtils.dp2px(getContext(), 200);
            captureParams.gravity = Gravity.END;
            btnCapture.setLayoutParams(captureParams);
        }
    }

    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    private void playExpandAnimation() {
        final int baseLength = CameraUtils.dp2px(getContext(), 60);
        ValueAnimator scaleAnimator = ValueAnimator.ofInt(0, CameraUtils.dp2px(getContext(), 20));
        scaleAnimator.setInterpolator(new LinearInterpolator());
        scaleAnimator.setDuration(100);
        scaleAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                int value = (int) animation.getAnimatedValue();
                LayoutParams captureParams = (LayoutParams) btnCapture.getLayoutParams();
                captureParams.width = baseLength + value;
                captureParams.height = baseLength + value;
                captureParams.gravity = Gravity.CENTER;
                btnCapture.setLayoutParams(captureParams);
            }
        });

        ValueAnimator transAnimator = ValueAnimator.ofInt(0, CameraUtils.dp2px(getContext(), 200));
        transAnimator.setInterpolator(new LinearInterpolator());
        transAnimator.setDuration(200);
        transAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                int value = (int) animation.getAnimatedValue();
                LayoutParams captureParams = (LayoutParams) btnCapture.getLayoutParams();
                captureParams.leftMargin = value;
                captureParams.gravity = Gravity.END;
                btnCapture.setLayoutParams(captureParams);
            }
        });

        AnimatorSet animatorSet = new AnimatorSet();
        animatorSet.playSequentially(scaleAnimator, transAnimator);
        animatorSet.start();
    }

    private void fold() {
        btnCapture.setImageResource(0);
        btnRetry.setEnabled(false);
        btnClose.setVisibility(VISIBLE);

        int length = CameraUtils.dp2px(getContext(), 60);
        LayoutParams captureParams = (LayoutParams) btnCapture.getLayoutParams();
        captureParams.width = length;
        captureParams.height = length;
        captureParams.leftMargin = 0;
        captureParams.gravity = Gravity.CENTER;
        btnCapture.setLayoutParams(captureParams);
    }
}
