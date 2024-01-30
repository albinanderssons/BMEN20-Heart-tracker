package com.example.android.HeartTracker;

import android.content.Intent;
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
                Intent startRecordActivityIntent = new Intent(MainActivity.this,
                        MeasuringActivity.class);
                startActivity(startRecordActivityIntent);
            }
        });

    }
}
