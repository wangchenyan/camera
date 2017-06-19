package me.wcy.camera;

import android.annotation.TargetApi;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.graphics.Point;
import android.hardware.Camera;
import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.WindowManager;

/**
 * Created by hzwangchenyan on 2017/6/13.
 */
public class CameraManager {
    private static final String TAG = "CameraManager";

    private enum State {
        STATE_IDLE,
        STATE_OPENED,
        STATE_SHOOTING
    }

    public interface Callback<T> {
        void onEvent(T t);
    }

    private int CAMERA_ID_BACK = -1;
    private int CAMERA_ID_FRONT = -1;

    private Context mContext;
    private Handler mUiHandler;
    private Handler mThreadHandler;
    private Camera mCamera;
    private Point mScreenSize;
    private int mCameraId;
    private State mState = State.STATE_IDLE;
    private int mSensorRotation;

    public static CameraManager getInstance() {
        return SingletonHolder.instance;
    }

    private static class SingletonHolder {
        private static CameraManager instance = new CameraManager();
    }

    private CameraManager() {
        mUiHandler = new Handler(Looper.getMainLooper());
        HandlerThread handlerThread = new HandlerThread(TAG + "-Thread");
        handlerThread.start();
        mThreadHandler = new Handler(handlerThread.getLooper());
        findCameras();
    }

    public void init(Context context) {
        mContext = context.getApplicationContext();
        DisplayMetrics displayMetrics = mContext.getResources().getDisplayMetrics();
        int width = Math.max(displayMetrics.widthPixels, displayMetrics.heightPixels);
        int height = Math.min(displayMetrics.widthPixels, displayMetrics.heightPixels);
        mScreenSize = new Point(width, height);
        mCameraId = -1;
    }

    public void open(final SurfaceHolder holder, final Callback<Boolean> callback) {
        checkInitialize();
        mThreadHandler.post(new SafeRunnable() {
            @Override
            public void runSafely() {
                openImmediate(holder);

                final boolean success = (mState == State.STATE_OPENED);
                mUiHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        callback.onEvent(success);
                    }
                });
            }
        });
    }

    private void openImmediate(SurfaceHolder holder) {
        closeImmediate();

        if (holder == null || holder.getSurface() == null || !holder.getSurface().isValid()) {
            return;
        }

        if (mCameraId < 0 && CAMERA_ID_BACK >= 0) {
            mCameraId = CAMERA_ID_BACK;
        }

        if (mCameraId < 0) {
            return;
        }

        try {
            mCamera = Camera.open(mCameraId);

            Camera.Parameters parameters = mCamera.getParameters();
            CameraUtils.setPreviewParams(mScreenSize, parameters);

            mCamera.setParameters(parameters);
            mCamera.setDisplayOrientation(getDisplayOrientation());
            mCamera.setPreviewDisplay(holder);
            mCamera.startPreview();
            setState(State.STATE_OPENED);
        } catch (Throwable th) {
            Log.e(TAG, "open camera failed", th);
        }
    }

    public boolean isOpened() {
        return mCamera != null && mState != State.STATE_IDLE;
    }

    public boolean hasMultiCamera() {
        return CAMERA_ID_BACK >= 0 && CAMERA_ID_FRONT >= 0;
    }

    @TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
    public void setFocus(final float x, final float y, final Callback<Boolean> callback) {
        checkInitialize();
        mThreadHandler.post(new SafeRunnable() {
            @Override
            public void runSafely() {
                if (mState != State.STATE_OPENED) {
                    return;
                }

                mCamera.cancelAutoFocus();
                Camera.Parameters parameters = mCamera.getParameters();
                CameraUtils.setFocusArea(mScreenSize, parameters, x, y);
                mCamera.setParameters(parameters);
                mCamera.autoFocus(new Camera.AutoFocusCallback() {
                    @Override
                    public void onAutoFocus(final boolean success, Camera camera) {
                        Log.d(TAG, "auto focus result: " + success);

                        mUiHandler.post(new Runnable() {
                            @Override
                            public void run() {
                                if (callback != null) {
                                    callback.onEvent(success);
                                }
                            }
                        });
                    }
                });
            }
        });
    }

    public void setZoom(final float span) {
        checkInitialize();
        mThreadHandler.post(new SafeRunnable() {
            @Override
            public void runSafely() {
                if (mState != State.STATE_OPENED) {
                    return;
                }

                Camera.Parameters parameters = mCamera.getParameters();
                if (parameters.isZoomSupported()) {
                    boolean valid = CameraUtils.setZoom(mScreenSize, parameters, span);
                    if (valid) {
                        mCamera.setParameters(parameters);
                    }
                }
            }
        });
    }

    public void switchCamera(final SurfaceHolder holder, final Callback<Boolean> callback) {
        checkInitialize();
        mThreadHandler.post(new SafeRunnable() {
            @Override
            public void runSafely() {
                if (!hasMultiCamera()) {
                    return;
                }

                if (mCameraId == CAMERA_ID_BACK) {
                    mCameraId = CAMERA_ID_FRONT;
                } else if (mCameraId == CAMERA_ID_FRONT) {
                    mCameraId = CAMERA_ID_BACK;
                } else {
                    mCameraId = CAMERA_ID_BACK;
                }

                openImmediate(holder);

                final boolean success = (mState == State.STATE_OPENED);
                mUiHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        if (callback != null) {
                            callback.onEvent(success);
                        }
                    }
                });
            }
        });
    }

    public void setSensorRotation(int rotation) {
        mSensorRotation = rotation;
    }

    public void takePicture(final Callback<Bitmap> callback) {
        checkInitialize();
        mThreadHandler.post(new SafeRunnable() {
            @Override
            public void runSafely() {
                if (mState != State.STATE_OPENED) {
                    return;
                }

                setState(State.STATE_SHOOTING);

                mCamera.takePicture(null, null, new Camera.PictureCallback() {
                    @Override
                    public void onPictureTaken(byte[] data, Camera camera) {
                        closeImmediate();

                        final Bitmap result;
                        if (data != null && data.length > 0) {
                            Bitmap bitmap = BitmapFactory.decodeByteArray(data, 0, data.length);
                            Matrix matrix = new Matrix();
                            int rotation = getDisplayOrientation() + mSensorRotation;
                            if (mCameraId == CAMERA_ID_BACK) {
                                matrix.setRotate(rotation);
                            } else {
                                rotation = (360 - rotation) % 360;
                                matrix.setRotate(rotation);
                                matrix.postScale(-1, 1);
                            }
                            result = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
                        } else {
                            result = null;
                        }

                        mUiHandler.post(new Runnable() {
                            @Override
                            public void run() {
                                if (callback != null) {
                                    callback.onEvent(result);
                                }
                            }
                        });
                    }
                });
            }
        });
    }

    public void close() {
        mThreadHandler.post(new SafeRunnable() {
            @Override
            public void runSafely() {
                closeImmediate();
            }
        });
    }

    private void checkInitialize() {
        if (mContext == null) {
            throw new IllegalStateException("camera manager is not initialized");
        }
    }

    private void closeImmediate() {
        if (mCamera != null) {
            mCamera.stopPreview();
            mCamera.release();
            mCamera = null;
        }

        if (mState != State.STATE_IDLE) {
            setState(State.STATE_IDLE);
        }
    }

    private void setState(State state) {
        Log.d(TAG, "change state from " + mState + " to " + state);
        mState = state;
    }

    private void findCameras() {
        Camera.CameraInfo info = new Camera.CameraInfo();
        for (int i = 0; i < Camera.getNumberOfCameras(); i++) {
            Camera.getCameraInfo(i, info);
            if (info.facing == Camera.CameraInfo.CAMERA_FACING_BACK) {
                CAMERA_ID_BACK = info.facing;
            } else if (info.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
                CAMERA_ID_FRONT = info.facing;
            }
        }
    }

    private int getDisplayOrientation() {
        Camera.CameraInfo info = new Camera.CameraInfo();
        Camera.getCameraInfo(mCameraId, info);
        WindowManager windowManager = (WindowManager) mContext.getSystemService(Context.WINDOW_SERVICE);
        int rotation = windowManager.getDefaultDisplay().getRotation();
        int degrees = 0;
        switch (rotation) {
            case Surface.ROTATION_0:
                degrees = 0;
                break;
            case Surface.ROTATION_90:
                degrees = 90;
                break;
            case Surface.ROTATION_180:
                degrees = 180;
                break;
            case Surface.ROTATION_270:
                degrees = 270;
                break;
        }

        int result;
        if (info.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
            result = (info.orientation + degrees) % 360;
            result = (360 - result) % 360;  // compensate the mirror
        } else {  // back-facing
            result = (info.orientation - degrees + 360) % 360;
        }
        return result;
    }

    private static abstract class SafeRunnable implements Runnable {
        @Override
        public final void run() {
            try {
                runSafely();
            } catch (Throwable th) {
                Log.e(TAG, "camera error", th);
            }
        }

        public abstract void runSafely();
    }
}
