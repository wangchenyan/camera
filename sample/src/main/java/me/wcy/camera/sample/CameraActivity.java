package me.wcy.camera.sample;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.util.Log;
import android.view.Window;
import android.view.WindowManager;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

import me.wcy.camera.CameraView;

/**
 * Created by hzwangchenyan on 2017/6/15.
 */
public class CameraActivity extends Activity implements CameraView.Listener {
    private static final String TAG = "CameraActivity";
    private CameraView mCameraView;
    private String mPath;

    public static void startForResult(Activity activity, String path, int requestCode) {
        Intent intent = new Intent(activity, CameraActivity.class);
        intent.putExtra(MediaStore.EXTRA_OUTPUT, path);
        intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        activity.startActivityForResult(intent, requestCode);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);

        mCameraView = new CameraView(this);
        setContentView(mCameraView);

        mPath = getIntent().getStringExtra(MediaStore.EXTRA_OUTPUT);
        if (TextUtils.isEmpty(mPath)) {
            finish();
            return;
        }

        mCameraView.setListener(this);
    }

    @Override
    public void onCapture(Bitmap bitmap) {
        File file = new File(mPath);
        if (!file.getParentFile().exists()) {
            file.getParentFile().mkdirs();
        }

        try {
            FileOutputStream out = new FileOutputStream(file);
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, out);
            out.flush();
            out.close();
        } catch (IOException e) {
            Log.e(TAG, "save picture error", e);
        }

        if (file.exists()) {
            Intent data = new Intent();
            data.setData(Uri.parse(mPath));
            setResult(RESULT_OK, data);
        }

        finish();
    }

    @Override
    public void onCameraClose() {
        finish();
    }

    @Override
    public void onCameraError(Throwable th) {
        Log.e(TAG, "camera error", th);
        onCameraClose();
    }

    @Override
    protected void onResume() {
        super.onResume();
        mCameraView.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        mCameraView.onPause();
    }
}
