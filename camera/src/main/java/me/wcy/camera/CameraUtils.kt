package me.wcy.camera

import android.annotation.TargetApi
import android.content.Context
import android.graphics.ImageFormat
import android.graphics.Point
import android.graphics.Rect
import android.hardware.Camera
import android.os.Build
import android.util.Log

/**
 * Created by hzwangchenyan on 2017/6/13.
 */
object CameraUtils {
    private const val TAG = "CameraUtils"

    fun setPreviewParams(surfaceSize: Point, parameters: Camera.Parameters?) {
        if (surfaceSize.x <= 0 || surfaceSize.y <= 0 || parameters == null) {
            return
        }
        val previewSizeList = parameters.supportedPreviewSizes
        val previewSize = findProperSize(surfaceSize, previewSizeList)
        if (previewSize != null) {
            parameters.setPreviewSize(previewSize.width, previewSize.height)
            Log.d(
                TAG,
                "previewSize: width: " + previewSize.width + ", height: " + previewSize.height
            )
        }
        val pictureSizeList = parameters.supportedPictureSizes
        val pictureSize = findProperSize(surfaceSize, pictureSizeList)
        if (pictureSize != null) {
            parameters.setPictureSize(pictureSize.width, pictureSize.height)
            Log.d(
                TAG,
                "pictureSize: width: " + pictureSize.width + ", height: " + pictureSize.height
            )
        }
        val focusModeList = parameters.supportedFocusModes
        if (focusModeList != null && focusModeList.contains(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE)) {
            parameters.focusMode = Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE
        }
        val pictureFormatList = parameters.supportedPictureFormats
        if (pictureFormatList != null && pictureFormatList.contains(ImageFormat.JPEG)) {
            parameters.pictureFormat = ImageFormat.JPEG
            parameters.jpegQuality = 100
        }
    }

    /**
     * 找出最合适的尺寸，规则如下：
     * 1.将尺寸按比例分组，找出比例最接近屏幕比例的尺寸组
     * 2.在比例最接近的尺寸组中找出最接近屏幕尺寸且大于屏幕尺寸的尺寸
     * 3.如果没有找到，则忽略2中第二个条件再找一遍，应该是最合适的尺寸了
     */
    private fun findProperSize(surfaceSize: Point, sizeList: List<Camera.Size>?): Camera.Size? {
        if (surfaceSize.x <= 0 || surfaceSize.y <= 0 || sizeList == null) {
            return null
        }
        val surfaceWidth = surfaceSize.x
        val surfaceHeight = surfaceSize.y
        val ratioListList: MutableList<MutableList<Camera.Size>> = ArrayList()
        for (size in sizeList) {
            addRatioList(ratioListList, size)
        }
        val surfaceRatio = surfaceWidth.toFloat() / surfaceHeight
        var bestRatioList: List<Camera.Size>? = null
        var ratioDiff = Float.MAX_VALUE
        for (ratioList in ratioListList) {
            val ratio = ratioList[0].width.toFloat() / ratioList[0].height
            val newRatioDiff = Math.abs(ratio - surfaceRatio)
            if (newRatioDiff < ratioDiff) {
                bestRatioList = ratioList
                ratioDiff = newRatioDiff
            }
        }
        var bestSize: Camera.Size? = null
        var diff = Int.MAX_VALUE
        assert(bestRatioList != null)
        for (size in bestRatioList!!) {
            val newDiff =
                Math.abs(size.width - surfaceWidth) + Math.abs(size.height - surfaceHeight)
            if (size.height >= surfaceHeight && newDiff < diff) {
                bestSize = size
                diff = newDiff
            }
        }
        if (bestSize != null) {
            return bestSize
        }
        diff = Int.MAX_VALUE
        for (size in bestRatioList) {
            val newDiff =
                Math.abs(size.width - surfaceWidth) + Math.abs(size.height - surfaceHeight)
            if (newDiff < diff) {
                bestSize = size
                diff = newDiff
            }
        }
        return bestSize
    }

    private fun addRatioList(
        ratioListList: MutableList<MutableList<Camera.Size>>,
        size: Camera.Size
    ) {
        val ratio = size.width.toFloat() / size.height
        for (ratioList in ratioListList) {
            val mine = ratioList[0].width.toFloat() / ratioList[0].height
            if (ratio == mine) {
                ratioList.add(size)
                return
            }
        }
        val ratioList: MutableList<Camera.Size> = ArrayList()
        ratioList.add(size)
        ratioListList.add(ratioList)
    }

    @TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
    fun setFocusArea(surfaceSize: Point, parameters: Camera.Parameters?, x: Float, y: Float) {
        if (surfaceSize.x <= 0 || surfaceSize.y <= 0 || parameters == null) {
            return
        }
        if (parameters.maxNumFocusAreas > 0) {
            val focusRect = calculateTapArea(surfaceSize, x, y, 1f)
            Log.d(TAG, "focusRect: $focusRect")
            val focusAreas: MutableList<Camera.Area> = ArrayList(1)
            focusAreas.add(Camera.Area(focusRect, 800))
            parameters.focusAreas = focusAreas
        }
        if (parameters.maxNumMeteringAreas > 0) {
            val meteringRect = calculateTapArea(surfaceSize, x, y, 1.5f)
            Log.d(TAG, "meteringRect: $meteringRect")
            val meteringAreas: MutableList<Camera.Area> = ArrayList(1)
            meteringAreas.add(Camera.Area(meteringRect, 800))
            parameters.meteringAreas = meteringAreas
        }
        parameters.focusMode = Camera.Parameters.FOCUS_MODE_AUTO
    }

    /**
     * 转换对焦区域
     * 范围(-1000, -1000, 1000, 1000)
     */
    private fun calculateTapArea(surfaceSize: Point, x: Float, y: Float, coefficient: Float): Rect {
        val focusAreaSize = 200f
        val areaSize = (focusAreaSize * coefficient).toInt()
        val surfaceWidth = surfaceSize.x
        val surfaceHeight = surfaceSize.y
        val centerX = (x / surfaceHeight * 2000 - 1000).toInt()
        val centerY = (y / surfaceWidth * 2000 - 1000).toInt()
        val left = clamp(centerX - areaSize / 2, -1000, 1000)
        val top = clamp(centerY - areaSize / 2, -1000, 1000)
        val right = clamp(left + areaSize, -1000, 1000)
        val bottom = clamp(top + areaSize, -1000, 1000)
        return Rect(left, top, right, bottom)
    }

    private fun clamp(x: Int, min: Int, max: Int): Int {
        return Math.min(Math.max(x, min), max)
    }

    /**
     * 根据屏幕宽度和最大缩放倍数计算缩放单位
     */
    fun setZoom(surfaceSize: Point, parameters: Camera.Parameters?, span: Float): Boolean {
        if (surfaceSize.x <= 0 || surfaceSize.y <= 0 || parameters == null) {
            return false
        }
        val maxZoom = parameters.maxZoom
        val unit = surfaceSize.y / 5 / maxZoom
        val zoom = (span / unit).toInt()
        val lastZoom = parameters.zoom
        var currZoom = lastZoom + zoom
        currZoom = clamp(currZoom, 0, maxZoom)
        parameters.zoom = currZoom
        return lastZoom != currZoom
    }

    /**
     * 只有倾斜角度比较大时才判定为屏幕旋转
     *
     * @return -1，表示旋转角度不够大
     */
    fun calculateSensorRotation(x: Float, y: Float): Int {
        if (Math.abs(x) > 6 && Math.abs(y) < 4) {
            return if (x > 6) {
                270
            } else {
                90
            }
        } else if (Math.abs(y) > 6 && Math.abs(x) < 4) {
            return if (y > 6) {
                0
            } else {
                180
            }
        }
        return -1
    }

    fun dp2px(context: Context, dpValue: Float): Int {
        val density = context.resources.displayMetrics.density
        return (dpValue * density + 0.5f).toInt()
    }
}