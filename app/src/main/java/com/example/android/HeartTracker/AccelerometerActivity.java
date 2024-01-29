package com.example.android.HeartTracker;


import android.content.Context;
import android.graphics.Color;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.series.DataPoint;
import com.jjoe64.graphview.series.LineGraphSeries;

public class AccelerometerActivity extends AppCompatActivity implements SensorEventListener {

    GraphView graph;
    TextView xVal, yVal, zVal;
    SensorManager sensorManager;
    Sensor accelerometer;
    LineGraphSeries<DataPoint> seriesX, seriesY, seriesZ;
    Button startButton;
    boolean collectValues = false;
    int counter; // X-axis
    float gravity[] = new float[3];
    float alpha = 0.8f;
    float linear_acceleration[] = new float[3];



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_accelerometer);

        // Graph X-axis counter initialization so that the graph starts at zero
        counter = 0;

        // Initialize button and set a listener to activate measurement upon clicking on the button
        startButton = findViewById(R.id.startButton);
        startButton.setOnClickListener(new View.OnClickListener() {
            // Simply sets a boolean to true, to start collect values from sensor.
            // The sensor always listens, but won't do anything unless collectValues = true in our case
            @Override
            public void onClick(View view) {
                if(!collectValues) {
                    collectValues = true;
                    startButton.setText("Stop measuring");
                } else{
                    collectValues = false;
                    startButton.setText("Start measuring");
                }
            }
        });

        // Views mapping, connecting variables to the layout
        graph = findViewById(R.id.graph);
        xVal = findViewById(R.id.xValueView);
        yVal = findViewById(R.id.yValueView);
        zVal = findViewById(R.id.zValueView);

        // Initializing the sensor manager with an accelerometer, and registering a listener.
        sensorManager = (SensorManager)getSystemService(Context.SENSOR_SERVICE);
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_UI);

        // Setting up initialized datapoints for each series of data
        // (x-values, y-values and z-values) with thier respective colors
        seriesX = new LineGraphSeries<>(new DataPoint[] {
                new DataPoint(0, 0),
        });
        seriesX.setColor(Color.RED);
        graph.addSeries(seriesX);

        seriesY = new LineGraphSeries<>(new DataPoint[] {
                new DataPoint(0, 0),
        });
        seriesY.setColor(Color.GREEN);
        graph.addSeries(seriesY);

        seriesZ = new LineGraphSeries<>(new DataPoint[] {
                new DataPoint(0, 0),
        });
        seriesZ.setColor(Color.BLUE);
        graph.addSeries(seriesZ);

    }


    @Override
    public void onSensorChanged(SensorEvent event) {
        if(collectValues) {
            float x, y, z;

            // Move along X-axis
            counter++;

            // Get sensor data
            x = event.values[0];
            y = event.values[1];
            z = event.values[2];

            gravity[0] = alpha * gravity[0] + (1 - alpha) * x;
            gravity[1] = alpha * gravity[1] + (1 - alpha) * y;
            gravity[2] = alpha * gravity[2] + (1 - alpha) * z;

            // Subtract gravity to get linear acceleration
            linear_acceleration[0] = x - gravity[0];
            linear_acceleration[1] = y - gravity[1];
            linear_acceleration[2] = z - gravity[2];

            // Update the text view
            xVal.setText(String.valueOf(linear_acceleration[0]));
            yVal.setText(String.valueOf(linear_acceleration[1]));
            zVal.setText(String.valueOf(linear_acceleration[2]));

            /*TODO do stuff with x,y,z values*/

            // Add data to series
            seriesX.appendData(new DataPoint(counter, linear_acceleration[0]), true, 30, false);
            seriesY.appendData(new DataPoint(counter, linear_acceleration[1]), true, 30, false);
            seriesZ.appendData(new DataPoint(counter, linear_acceleration[2]), true, 30, false);

            // Add series to graph
            if (counter % 10 == 0){
             graph.addSeries(seriesX);
             graph.addSeries(seriesY);
             graph.addSeries(seriesZ);}

        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        //Auto-generated method.
    }
    protected void onPause() {
        // unregister listener
        super.onPause();
        sensorManager.unregisterListener(this);
    }
    protected void onResume() {
        // register listener again
        super.onResume();
        sensorManager.registerListener(this,
                sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER),
                SensorManager.SENSOR_DELAY_NORMAL);
    }
}