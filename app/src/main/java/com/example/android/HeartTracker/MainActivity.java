package com.example.android.HeartTracker;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.Button;
import android.view.View;
import android.view.View.OnClickListener;

public class MainActivity extends AppCompatActivity {
    private Button measureButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        measureButton = findViewById(R.id.mVideoButton);

        measureButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if (checkSelfPermission(Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
                    // Request the camera permission if not granted
                    Intent startRecordActivityIntent = new Intent(MainActivity.this,
                            MeasuringActivity.class);
                    startActivity(startRecordActivityIntent);
                } else {
                    requestPermissions(new String[]{Manifest.permission.CAMERA}, 400);
                }

            }
        });

    }
}
