package com.example.android.HeartTracker;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.support.annotation.RequiresApi;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.Button;

public class MainActivity extends AppCompatActivity {

    @RequiresApi(api = Build.VERSION_CODES.M)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Button measureButton = findViewById(R.id.mVideoButton);

        measureButton.setOnClickListener(v -> {
            if (checkSelfPermission(Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
                // Request the camera permission if not granted
                Intent startRecordActivityIntent = new Intent(MainActivity.this,
                        MeasuringActivity.class);
                startActivity(startRecordActivityIntent);
            } else {
                requestPermissions(new String[]{Manifest.permission.CAMERA}, 400);
            }

        });

    }
}
