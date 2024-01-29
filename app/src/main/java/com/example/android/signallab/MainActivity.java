package com.example.android.signallab;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.Button;
import android.view.View;
import android.view.View.OnClickListener;

public class MainActivity extends AppCompatActivity {
    private Button accButton;
    private Button recButton;
    private Button vidButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        accButton = findViewById(R.id.mAcceleratorButton);
        recButton = findViewById(R.id.mRecorderButton);
        vidButton = findViewById(R.id.mVideoButton);
        accButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent startAccActivityIntent = new Intent(MainActivity.this,
                        AccelerometerActivity.class);
                startActivity(startAccActivityIntent);
            }
        });
        recButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent startRecordActivityIntent = new Intent(MainActivity.this,
                        RecorderActivity.class);
                startActivity(startRecordActivityIntent);
            }
        });
        vidButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent startRecordActivityIntent = new Intent(MainActivity.this,
                        VideoActivity.class);
                startActivity(startRecordActivityIntent);
            }
        });

    }
}
