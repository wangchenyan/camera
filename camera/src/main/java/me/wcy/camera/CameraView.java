package me.wcy.camera;

import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Build;
import android.util.AttributeSet;
import android.util.Log;
import android.view.GestureDetector;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.RotateAnimation;
import android.widget.FrameLayout;
import android.widget.ImageView;

/**
 * Created by hzwangchenyan on 2017/6/13.
 */
public class CameraView extends FrameLayout implements SurfaceHolder.Callback,
        CaptureLayout.ClickListener, View.OnClickListener, SensorEventListener {
    private static final String TAG = "CameraView";
    private SurfaceView mSurfaceView;
    private View mFocusView;
    private View mSwitchWrapper;
    private View mSwitchView;
    private ImageView mPictureView;
    private CaptureLayout mCaptureLayout;
    private GestureDetector mGestureDetector;
    private ScaleGestureDetector mScaleGestureDetector;
    private SensorManager mSensorManager;
    private CameraListener mCameraListener;
    private Bitmap mPicture;
    private int mSensorRotation;
    private boolean isSurfaceCreated;

    public interface CameraListener {
        void onCapture(Bitmap bitmap);

        void onCameraClose();

        void onCameraError(Throwable th);
    }

    public CameraView(Context context) {
        this(context, null);
    }

    public CameraView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public CameraView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        setBackgroundColor(Color.BLACK);
        LayoutInflater.from(getContext()).inflate(R.layout.camera_layout, this, true);

        mSurfaceView = (SurfaceView) findViewById(R.id.camera_surface);
        mFocusView = findViewById(R.id.camera_focus);
        mSwitchWrapper = findViewById(R.id.camera_switch_wrapper);
        mSwitchView = findViewById(R.id.camera_switch);
        mPictureView = (ImageView) findViewById(R.id.camera_picture_preview);
        mCaptureLayout = (CaptureLayout) findViewById(R.id.camera_capture_layout);

        CameraManager.getInstance().init(getContext());

        mSurfaceView.setOnTouchListener(surfaceTouchListener);
        // fix `java.lang.RuntimeException: startPreview failed` on api 10
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.GINGERBREAD_MR1) {
            mSurfaceView.getHolder().setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
        }
        mSurfaceView.getHolder().addCallback(this);
        mCaptureLayout.setClickListener(this);
        mSwitchWrapper.setVisibility(CameraManager.getInstance().hasMultiCamera() ? VISIBLE : GONE);
        mSwitchWrapper.setOnClickListener(this);

        mGestureDetector = new GestureDetector(getContext(), simpleOnGestureListener);
        mScaleGestureDetector = new ScaleGestureDetector(getContext(), onScaleGestureListener);
        mSensorManager = (SensorManager) getContext().getSystemService(Context.SENSOR_SERVICE);
    }

    public void setCameraListener(CameraListener listener) {
        mCameraListener = listener;
    }

    public void onResume() {
        Log.d(TAG, "onResume");

        if (!CameraManager.getInstance().isOpened() && isSurfaceCreated) {
            CameraManager.getInstance().open(new CameraManager.Callback<Boolean>() {
                @Override
                public void onEvent(Boolean success) {
                    if (!success && mCameraListener != null) {
                        mCameraListener.onCameraError(new Exception("open camera failed"));
                    }
                }
            });
        }

        mSensorManager.registerListener(this, mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER), SensorManager.SENSOR_DELAY_NORMAL);
    }

    public void onPause() {
        Log.d(TAG, "onPause");

        if (CameraManager.getInstance().isOpened()) {
            CameraManager.getInstance().close();
        }

        mSensorManager.unregisterListener(this);
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        Log.d(TAG, "onDetachedFromWindow");
        mCameraListener = null;
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        Log.d(TAG, "surfaceCreated");
        isSurfaceCreated = true;
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        Log.d(TAG, "surfaceChanged");
        CameraManager.getInstance().setSurfaceHolder(holder);

        if (CameraManager.getInstance().isOpened()) {
            CameraManager.getInstance().close();
        }

        CameraManager.getInstance().open(new CameraManager.Callback<Boolean>() {
            @Override
            public void onEvent(Boolean success) {
                if (!success && mCameraListener != null) {
                    mCameraListener.onCameraError(new Exception("open camera failed"));
                }
            }
        });
    }

    @Override
    public void surfaceDestroyed(final SurfaceHolder holder) {
        Log.d(TAG, "surfaceDestroyed");
        isSurfaceCreated = false;
        CameraManager.getInstance().setSurfaceHolder(null);
    }

    @Override
    public void onCaptureClick() {
        CameraManager.getInstance().takePicture(new CameraManager.Callback<Bitmap>() {
            @Override
            public void onEvent(Bitmap bitmap) {
                if (bitmap != null) {
                    mSurfaceView.setVisibility(GONE);
                    mSwitchWrapper.setVisibility(GONE);
                    mPictureView.setVisibility(VISIBLE);
                    mPicture = bitmap;
                    mPictureView.setImageBitmap(mPicture);
                    mCaptureLayout.setExpanded(true);
                } else {
                    // 不知道什么原因拍照失败，重新预览
                    onRetryClick();
                }
            }
        });
    }

    @Override
    public void onOkClick() {
        if (mPicture != null && mCameraListener != null) {
            mCameraListener.onCapture(mPicture);
        }
    }

    @Override
    public void onRetryClick() {
        mPicture = null;
        mCaptureLayout.setExpanded(false);
        mSurfaceView.setVisibility(VISIBLE);
        mSwitchWrapper.setVisibility(CameraManager.getInstance().hasMultiCamera() ? VISIBLE : GONE);
        mPictureView.setImageBitmap(null);
        mPictureView.setVisibility(GONE);
    }

    @Override
    public void onCloseClick() {
        if (mCameraListener != null) {
            mCameraListener.onCameraClose();
        }
    }

    @Override
    public void onClick(View v) {
        if (v == mSwitchWrapper) {
            CameraManager.getInstance().switchCamera(new CameraManager.Callback<Boolean>() {
                @Override
                public void onEvent(Boolean success) {
                    if (!success && mCameraListener != null) {
                        mCameraListener.onCameraError(new Exception("switch camera failed"));
                    }
                }
            });
        }
    }

    private OnTouchListener surfaceTouchListener = new OnTouchListener() {
        @Override
        public boolean onTouch(View v, MotionEvent event) {
            mScaleGestureDetector.onTouchEvent(event);
            if (mScaleGestureDetector.isInProgress()) {
                return true;
            }

            return mGestureDetector.onTouchEvent(event);
        }
    };

    private GestureDetector.SimpleOnGestureListener simpleOnGestureListener = new GestureDetector.SimpleOnGestureListener() {
        @Override
        public boolean onDown(MotionEvent e) {
            Log.d(TAG, "onDown");
            if (!CameraManager.getInstance().isOpened()) {
                return false;
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
                mFocusView.removeCallbacks(timeoutRunnable);
                mFocusView.postDelayed(timeoutRunnable, 1500);

                mFocusView.setVisibility(VISIBLE);
                LayoutParams focusParams = (LayoutParams) mFocusView.getLayoutParams();
                focusParams.leftMargin = (int) e.getX() - focusParams.width / 2;
                focusParams.topMargin = (int) e.getY() - focusParams.height / 2;
                mFocusView.setLayoutParams(focusParams);

                ObjectAnimator scaleX = ObjectAnimator.ofFloat(mFocusView, "scaleX", 1, 0.5f);
                scaleX.setDuration(300);
                ObjectAnimator scaleY = ObjectAnimator.ofFloat(mFocusView, "scaleY", 1, 0.5f);
                scaleY.setDuration(300);
                ObjectAnimator alpha = ObjectAnimator.ofFloat(mFocusView, "alpha", 1f, 0.3f, 1f, 0.3f, 1f, 0.3f, 1f);
                alpha.setDuration(600);
                AnimatorSet animatorSet = new AnimatorSet();
                animatorSet.play(scaleX).with(scaleY).before(alpha);
                animatorSet.start();

                CameraManager.Callback<Boolean> focusCallback = new CameraManager.Callback<Boolean>() {
                    @Override
                    public void onEvent(Boolean success) {
                        if (mFocusView.getTag() == this && mFocusView.getVisibility() == VISIBLE) {
                            mFocusView.setVisibility(INVISIBLE);
                        }
                    }
                };
                mFocusView.setTag(focusCallback);
                CameraManager.getInstance().setFocus(e.getX(), e.getY(), focusCallback);
            }

            return CameraManager.getInstance().hasMultiCamera();
        }

        /**
         * 前置摄像头可能不会回调对焦成功，因此需要手动隐藏对焦框
         */
        private Runnable timeoutRunnable = new Runnable() {
            @Override
            public void run() {
                if (mFocusView.getVisibility() == VISIBLE) {
                    mFocusView.setVisibility(INVISIBLE);
                }
            }
        };

        @Override
        public boolean onDoubleTap(MotionEvent e) {
            Log.d(TAG, "onDoubleTap");
            CameraManager.getInstance().switchCamera(new CameraManager.Callback<Boolean>() {
                @Override
                public void onEvent(Boolean success) {
                    if (!success && mCameraListener != null) {
                        mCameraListener.onCameraError(new Exception("switch camera failed"));
                    }
                }
            });
            return true;
        }
    };

    private ScaleGestureDetector.OnScaleGestureListener onScaleGestureListener = new ScaleGestureDetector.OnScaleGestureListener() {
        private float mLastSpan;

        @Override
        public boolean onScale(ScaleGestureDetector detector) {
            Log.d(TAG, "onScale");
            float span = detector.getCurrentSpan() - mLastSpan;
            mLastSpan = detector.getCurrentSpan();
            if (CameraManager.getInstance().isOpened()) {
                CameraManager.getInstance().setZoom(span);
                return true;
            }
            return false;
        }

        @Override
        public boolean onScaleBegin(ScaleGestureDetector detector) {
            mLastSpan = detector.getCurrentSpan();
            return CameraManager.getInstance().isOpened();
        }

        @Override
        public void onScaleEnd(ScaleGestureDetector detector) {
        }
    };

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() != Sensor.TYPE_ACCELEROMETER) {
            return;
        }

        int rotation = CameraUtils.calculateSensorRotation(event.values[0], event.values[1]);
        if (rotation >= 0 && rotation != mSensorRotation) {
            Log.d(TAG, "screen rotation changed from " + mSensorRotation + " to " + rotation);
            playRotateAnimation(mSensorRotation, rotation);
            CameraManager.getInstance().setSensorRotation(rotation);
            mSensorRotation = rotation;
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
    }

    private void playRotateAnimation(int oldRotation, int newRotation) {
        if (!CameraManager.getInstance().hasMultiCamera()) {
            return;
        }

        int diff = newRotation - oldRotation;
        if (diff > 180) {
            diff = diff - 360;
        } else if (diff < -180) {
            diff = diff + 360;
        }
        RotateAnimation rotate = new RotateAnimation(-oldRotation, -oldRotation - diff, Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF, 0.5f);
        rotate.setDuration(200);
        rotate.setFillAfter(true);
        mSwitchView.startAnimation(rotate);
    }
}
