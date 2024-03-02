package com.example.android.HeartTracker;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.widget.Button;
import android.widget.TextView;

public class ResultActivity extends AppCompatActivity {
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_result);
        TextView resultView = findViewById(R.id.resultText);
        Button homeButton = findViewById(R.id.homeButton);
        Button measureButton = findViewById(R.id.measureButton);

        Bundle b = getIntent().getExtras();
        if(b!=null){
            String bpm = (String) b.get("BPM");
            resultView.setText(String.format("%s bpm",bpm));
        }

        homeButton.setOnClickListener((view) -> {
            finish();
            Intent i = new Intent(ResultActivity.this, MainActivity.class);
            startActivity(i);
        });

        measureButton.setOnClickListener((view) -> {
            finish();
            Intent i = new Intent(ResultActivity.this, MeasuringActivity.class);
            startActivity(i);
        });
    }
}
