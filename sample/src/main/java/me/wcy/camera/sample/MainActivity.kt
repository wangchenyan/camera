package me.wcy.camera.sample

import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.widget.Button
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat

class MainActivity : AppCompatActivity() {
    private lateinit var picture: ImageView
    private lateinit var start: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        start = findViewById(R.id.btn_start)
        picture = findViewById(R.id.iv_picture)
        start.setOnClickListener {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M
                && (hasPermission(android.Manifest.permission.CAMERA).not()
                        || hasPermission(android.Manifest.permission.READ_EXTERNAL_STORAGE).not()
                        || hasPermission(android.Manifest.permission.WRITE_EXTERNAL_STORAGE).not())
            ) {
                requestPermissions(
                    arrayOf(
                        android.Manifest.permission.CAMERA,
                        android.Manifest.permission.READ_EXTERNAL_STORAGE,
                        android.Manifest.permission.WRITE_EXTERNAL_STORAGE
                    ), 0
                )
                return@setOnClickListener
            }

            picture.setImageBitmap(null)
            val path = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
                .toString() + "/camera_" + System.currentTimeMillis() + ".jpg"
            CameraActivity.startForResult(this@MainActivity, path, 0)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == RESULT_OK && data != null) {
            val path = data.dataString
            val bitmap = BitmapFactory.decodeFile(path)
            picture.setImageBitmap(bitmap)
        }
    }

    private fun hasPermission(permission: String): Boolean {
        return ContextCompat.checkSelfPermission(
            this,
            permission
        ) == PackageManager.PERMISSION_GRANTED
    }
}