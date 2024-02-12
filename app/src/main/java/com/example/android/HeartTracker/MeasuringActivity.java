package com.example.android.HeartTracker;

import android.content.Intent;
import android.hardware.Camera;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

public class MeasuringActivity extends AppCompatActivity {
    Camera mCamera;
    CameraPreview mPreview;
    ImageView convertedImageView;
    LinearLayout layoutForImage;
    FrameLayout preview;

    TextView avgText;
    TextView measuring_time;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_measure);

        // Make a image to put our converted preview frame.
        convertedImageView = new ImageView(this);
        // Get the mobiles camera and set it to our camera object.
        mCamera = getCameraInstance();
        // Create a layout to put our image.
        layoutForImage = findViewById(R.id.ll);
        // Creates our own camera preview object to  be able to make changes to the previews.
        avgText = findViewById(R.id.avgtext);
        measuring_time = findViewById(R.id.measuring_time);
        mPreview = new CameraPreview(this, mCamera, convertedImageView,layoutForImage,avgText, measuring_time);
        // Add our camerapreview to this activitys layout.
        Button btnStop = findViewById(R.id.btnStop);
        btnStop.setOnClickListener(view -> {
            onPause();
            stopMeasurement();
        });
        preview = (FrameLayout) findViewById(R.id.camera_preview);
        preview.addView(mPreview);
        preview.setMinimumWidth(mPreview.width);
        preview.setMinimumHeight(mPreview.height);
        //This is done to not show the real preview frame, and only our ImageView.
        preview.setVisibility(View.INVISIBLE);

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    }

    private void stopMeasurement(){
        Intent intent = new Intent(MeasuringActivity.this, MainActivity.class);
        startActivity(intent);
        finish();
    }



    // This is connected to the lifecycle of the activity
    @Override
    protected void onPause() {
        super.onPause();
        if (mCamera != null) {
            // Call stopPreview() to stop updating the preview surface.
            mCamera.setPreviewCallback(null);
            //mCamera.stopPreview();
            //mCamera.release();
            //mCamera = null;
        }

        mPreview = null;
    }
    // This is connected to the lifecycle of the activity
    @Override
    protected void onResume() {
        super.onResume();
        /*
        mCamera = getCameraInstance();
        mPreview = new CameraPreview(this, mCamera, convertedImageView,layoutForImage);
        preview.addView(mPreview);
        preview.setVisibility(View.INVISIBLE);
        */
    }

    /** A safe way to get an instance of the Camera object. */
    public static Camera getCameraInstance(){
        Camera c = null;
        try {
            c = Camera.open(); // attempt to get a Camera instance
        }
        catch (Exception e){
            // Camera is not available (in use or does not exist)
        }
        return c; // returns null if camera is unavailable
    }

}
