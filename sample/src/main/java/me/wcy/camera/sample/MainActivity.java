package me.wcy.camera.sample;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;

public class MainActivity extends AppCompatActivity {
    private ImageView picture;
    private Button start;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        start = (Button) findViewById(R.id.btn_start);
        picture = (ImageView) findViewById(R.id.iv_picture);

        start.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                picture.setImageBitmap(null);
                String path = getExternalFilesDir(Environment.DIRECTORY_PICTURES) + "/camera_" + System.currentTimeMillis() + ".jpg";
                CameraActivity.startForResult(MainActivity.this, path, 0);
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == RESULT_OK && data != null) {
            String path = data.getDataString();
            Bitmap bitmap = BitmapFactory.decodeFile(path);
            picture.setImageBitmap(bitmap);
        }
    }
}
