package me.wcy.camera;

import android.annotation.TargetApi;
import android.content.Context;
import android.graphics.ImageFormat;
import android.graphics.Point;
import android.graphics.Rect;
import android.hardware.Camera;
import android.os.Build;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by hzwangchenyan on 2017/6/13.
 */
public class CameraUtils {
    private static final String TAG = "CameraUtils";

    public static void setPreviewParams(Point screenSize, Camera.Parameters parameters) {
        if (screenSize == null || parameters == null) {
            return;
        }

        List<Camera.Size> previewSizeList = parameters.getSupportedPreviewSizes();
        Camera.Size previewSize = findProperSize(screenSize, previewSizeList);
        if (previewSize != null) {
            parameters.setPreviewSize(previewSize.width, previewSize.height);
            Log.d(TAG, "previewSize: width: " + previewSize.width + ", height: " + previewSize.height);
        }

        List<Camera.Size> pictureSizeList = parameters.getSupportedPictureSizes();
        Camera.Size pictureSize = findProperSize(screenSize, pictureSizeList);
        if (pictureSize != null) {
            parameters.setPictureSize(pictureSize.width, pictureSize.height);
            Log.d(TAG, "pictureSize: width: " + pictureSize.width + ", height: " + pictureSize.height);
        }

        List<String> focusModeList = parameters.getSupportedFocusModes();
        if (focusModeList != null && focusModeList.contains(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE)) {
            parameters.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE);
        }

        List<Integer> pictureFormatList = parameters.getSupportedPictureFormats();
        if (pictureFormatList != null && pictureFormatList.contains(ImageFormat.JPEG)) {
            parameters.setPictureFormat(ImageFormat.JPEG);
            parameters.setJpegQuality(100);
        }
    }

    /**
     * 找出最合适的尺寸，规则如下：
     * 1.将尺寸按比例分组，找出比例最接近屏幕比例的尺寸组
     * 2.在比例最接近的尺寸组中找出最接近屏幕尺寸且大于屏幕尺寸的尺寸
     * 3.如果没有找到，则忽略2中第二个条件再找一遍，应该是最合适的尺寸了
     */
    private static Camera.Size findProperSize(Point screenSize, List<Camera.Size> sizeList) {
        if (screenSize == null || sizeList == null) {
            return null;
        }

        int screenWidth = screenSize.x;
        int screenHeight = screenSize.y;

        List<List<Camera.Size>> ratioListList = new ArrayList<>();
        for (Camera.Size size : sizeList) {
            addRatioList(ratioListList, size);
        }

        final float screenRatio = (float) screenWidth / screenHeight;
        List<Camera.Size> bestRatioList = null;
        float ratioDiff = Float.MAX_VALUE;
        for (List<Camera.Size> ratioList : ratioListList) {
            float ratio = (float) ratioList.get(0).width / ratioList.get(0).height;
            float newRatioDiff = Math.abs(ratio - screenRatio);
            if (newRatioDiff < ratioDiff) {
                bestRatioList = ratioList;
                ratioDiff = newRatioDiff;
            }
        }

        Camera.Size bestSize = null;
        int diff = Integer.MAX_VALUE;
        assert bestRatioList != null;
        for (Camera.Size size : bestRatioList) {
            int newDiff = Math.abs(size.width - screenWidth) + Math.abs(size.height - screenHeight);
            if (size.height >= screenHeight && newDiff < diff) {
                bestSize = size;
                diff = newDiff;
            }
        }

        if (bestSize != null) {
            return bestSize;
        }

        diff = Integer.MAX_VALUE;
        for (Camera.Size size : bestRatioList) {
            int newDiff = Math.abs(size.width - screenWidth) + Math.abs(size.height - screenHeight);
            if (newDiff < diff) {
                bestSize = size;
                diff = newDiff;
            }
        }

        return bestSize;
    }

    private static void addRatioList(List<List<Camera.Size>> ratioListList, Camera.Size size) {
        float ratio = (float) size.width / size.height;
        for (List<Camera.Size> ratioList : ratioListList) {
            float mine = (float) ratioList.get(0).width / ratioList.get(0).height;
            if (ratio == mine) {
                ratioList.add(size);
                return;
            }
        }

        List<Camera.Size> ratioList = new ArrayList<>();
        ratioList.add(size);
        ratioListList.add(ratioList);
    }

    @TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
    public static void setFocusArea(Point screenSize, Camera.Parameters parameters, float x, float y) {
        if (screenSize == null || parameters == null) {
            return;
        }

        if (parameters.getMaxNumFocusAreas() > 0) {
            Rect focusRect = calculateTapArea(screenSize, x, y, 1f);
            Log.d(TAG, "focusRect: " + focusRect);
            List<Camera.Area> focusAreas = new ArrayList<>(1);
            focusAreas.add(new Camera.Area(focusRect, 800));
            parameters.setFocusAreas(focusAreas);
        }

        if (parameters.getMaxNumMeteringAreas() > 0) {
            Rect meteringRect = calculateTapArea(screenSize, x, y, 1.5f);
            Log.d(TAG, "meteringRect: " + meteringRect);
            List<Camera.Area> meteringAreas = new ArrayList<>(1);
            meteringAreas.add(new Camera.Area(meteringRect, 800));
            parameters.setMeteringAreas(meteringAreas);
        }

        parameters.setFocusMode(Camera.Parameters.FOCUS_MODE_AUTO);
    }

    /**
     * 转换对焦区域
     * 范围(-1000, -1000, 1000, 1000)
     */
    private static Rect calculateTapArea(Point screenSize, float x, float y, float coefficient) {
        float focusAreaSize = 200;
        int areaSize = (int) (focusAreaSize * coefficient);
        int screenWidth = screenSize.x;
        int screenHeight = screenSize.y;
        int centerX = (int) (x / screenHeight * 2000 - 1000);
        int centerY = (int) (y / screenWidth * 2000 - 1000);
        int left = clamp(centerX - (areaSize / 2), -1000, 1000);
        int top = clamp(centerY - (areaSize / 2), -1000, 1000);
        int right = clamp(left + areaSize, -1000, 1000);
        int bottom = clamp(top + areaSize, -1000, 1000);
        return new Rect(left, top, right, bottom);
    }

    private static int clamp(int x, int min, int max) {
        if (x > max) {
            return max;
        }
        if (x < min) {
            return min;
        }
        return x;
    }

    /**
     * 根据屏幕宽度和最大缩放倍数计算缩放单位
     */
    public static boolean setZoom(Point screenSize, Camera.Parameters parameters, float span) {
        if (screenSize == null || parameters == null) {
            return false;
        }

        int screenMin = screenSize.y;
        int maxZoom = parameters.getMaxZoom();
        int unit = screenMin / 5 / maxZoom;
        int zoom = (int) (span / unit);
        int lastZoom = parameters.getZoom();
        int currZoom = lastZoom + zoom;
        currZoom = Math.min(Math.max(0, currZoom), maxZoom);
        parameters.setZoom(currZoom);
        return lastZoom != currZoom;
    }

    /**
     * 只有倾斜角度比较大时才判定为屏幕旋转
     *
     * @return -1，表示旋转角度不够大
     */
    public static int calculateSensorRotation(float x, float y) {
        if (Math.abs(x) > 6 && Math.abs(y) < 4) {
            if (x > 6) {
                return 270;
            } else {
                return 90;
            }
        } else if (Math.abs(y) > 6 && Math.abs(x) < 4) {
            if (y > 6) {
                return 0;
            } else {
                return 180;
            }
        }

        return -1;
    }

    public static int dp2px(Context context, float dpValue) {
        float density = context.getResources().getDisplayMetrics().density;
        return (int) (dpValue * density + 0.5f);
    }
}
