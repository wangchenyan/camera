package me.wcy.camera.sample

import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.Window
import android.view.WindowManager
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import me.wcy.camera.CameraView
import me.wcy.camera.CameraView.CameraListener
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

/**
 * Created by hzwangchenyan on 2017/6/15.
 */
class CameraActivity : AppCompatActivity() {
    private lateinit var mCameraView: CameraView
    private lateinit var mPath: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        window.setFlags(
            WindowManager.LayoutParams.FLAG_FULLSCREEN,
            WindowManager.LayoutParams.FLAG_FULLSCREEN
        )

        val path = intent.getStringExtra(MediaStore.EXTRA_OUTPUT)
        if (path.isNullOrEmpty()) {
            Toast.makeText(this, "保存路径不能为空", Toast.LENGTH_SHORT).show()
            finish()
            return
        }
        mPath = path

        mCameraView = CameraView(this)
        setContentView(mCameraView)

        mCameraView.setCameraListener(object : CameraListener {
            override fun onCapture(bitmap: Bitmap) {
                val file = File(path)
                if (!file.parentFile.exists()) {
                    file.parentFile.mkdirs()
                }
                try {
                    val out = FileOutputStream(file)
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 100, out)
                    out.flush()
                    out.close()
                } catch (e: IOException) {
                    Log.e(TAG, "save picture error", e)
                }
                if (file.exists()) {
                    val data = Intent()
                    data.data = Uri.parse(path)
                    setResult(RESULT_OK, data)
                }
                finish()
            }

            override fun onCameraClose() {
                finish()
            }

            override fun onCameraError(th: Throwable?) {
                Log.e(TAG, "camera error", th)
                onCameraClose()
            }
        })
    }

    override fun onResume() {
        super.onResume()
        mCameraView.onResume()
    }

    override fun onPause() {
        super.onPause()
        mCameraView.onPause()
    }

    companion object {
        private const val TAG = "CameraActivity"

        fun startForResult(activity: AppCompatActivity, path: String, requestCode: Int) {
            val intent = Intent(activity, CameraActivity::class.java)
            intent.putExtra(MediaStore.EXTRA_OUTPUT, path)
            intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            activity.startActivityForResult(intent, requestCode)
        }
    }
}