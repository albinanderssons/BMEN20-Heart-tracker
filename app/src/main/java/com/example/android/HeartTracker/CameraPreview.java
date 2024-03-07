package com.example.android.HeartTracker;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.ImageFormat;
import android.hardware.Camera;
import android.os.AsyncTask;
import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.Array;
import java.util.ArrayList;
import java.util.List;
import static android.content.ContentValues.TAG;
import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.GridLabelRenderer;
import com.jjoe64.graphview.series.DataPoint;
import com.jjoe64.graphview.series.LineGraphSeries;

/** A basic Camera preview class */
public class CameraPreview extends SurfaceView implements SurfaceHolder.Callback, Camera.PreviewCallback {
    private SurfaceHolder mHolder;
    private Camera mCamera;
    private int imageFormat;
    private boolean mProcessInProgress = false;
    private Bitmap mBitmap = null;
    private ImageView myCameraPreview;
    private int[] pixels = null;
    public int width, height;
    private Camera.Parameters params;

    private long startTime;

    private List<Double> redAVGs;
    //private List<Double> smoothedAvgs;
    private List<Double> timeStamps;

    private GraphView graph;
    private TextView avgText;
    private TextView measuring_time;
    private Button btnStop;
    LineGraphSeries<DataPoint> plotRedAvg;
    private int framesCounter;

    private boolean isrunning;
    private double timestamp;
    private boolean isMeasuring;
    private static int MEASURE_TIME = 15;
    private static int FirstDelayInSecs = 2;
    private static String light_too_weak = "Hold your finger steady";
    private static String start_measuring = "Starting Measurement...";
    private AppCompatActivity parent;

    public CameraPreview(Context context, Camera camera, ImageView mCameraPreview, LinearLayout layout, TextView avgText, TextView measuring_time, GraphView graph,AppCompatActivity parent) {
        super(context);
        mCamera = camera;
        params = mCamera.getParameters();
        imageFormat = params.getPreviewFormat();

        int minWidth = Integer.MAX_VALUE;
        int minHeight = Integer.MAX_VALUE;

        for (Camera.Size previewSize : mCamera.getParameters().getSupportedPreviewSizes()) {
            if (previewSize.width < minWidth || previewSize.height < minHeight) {
                minWidth = previewSize.width;
                minHeight = previewSize.height;
            }
        }
        params.setPreviewSize(minWidth, minHeight);
        params.setFlashMode(Camera.Parameters.FLASH_MODE_TORCH);
        width = minWidth;
        height = minHeight;

        mCamera.setParameters(params);
        myCameraPreview = mCameraPreview;
        // Install a SurfaceHolder.Callback so we get notified when the
        // underlying surface is created and destroyed.
        mHolder = getHolder();
        mHolder.addCallback(this);
        // deprecated setting, but required on Android versions prior to 3.0
        mHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
        layout.addView(myCameraPreview);

        redAVGs = new ArrayList<Double>();
        //smoothedAvgs = new ArrayList<Double>();
        timeStamps = new ArrayList<Double>();
        startTime = System.currentTimeMillis();

        this.avgText = avgText;
        this.measuring_time = measuring_time;
        this.graph = graph;
        graph.getViewport().setXAxisBoundsManual(true);
        graph.getViewport().setMinX(0);
        graph.getViewport().setMaxX(2);
        graph.getGridLabelRenderer().setHorizontalLabelsVisible(false);
        graph.getGridLabelRenderer().setVerticalLabelsVisible(false);
        graph.getGridLabelRenderer().setGridStyle(GridLabelRenderer.GridStyle.NONE);

        framesCounter = 0;
        isrunning = true;
        isMeasuring = false;
        this.parent = parent;
        plotRedAvg = new LineGraphSeries<>();
    }

    public void surfaceCreated(SurfaceHolder holder) {
        // The Surface has been created, now tell the camera where to draw the preview.
        try {

            mCamera.setPreviewDisplay(holder);
            mCamera.setPreviewCallback(this);
        } catch (IOException e) {
            Log.d(TAG, "Error setting camera preview: " + e.getMessage());
        }
    }

    public void surfaceDestroyed(SurfaceHolder holder) {

    }

    /* If the application is allowed to rotate, here is where you would change the camera preview
     * size and other formatting changes.*/
    public void surfaceChanged(SurfaceHolder holder, int format, int w, int h) {
        if (mHolder.getSurface() == null) {
            return;
        }
        try {
            mCamera.stopPreview();
        } catch (Exception e) {
        }
        try {
            mCamera.setPreviewDisplay(mHolder);
            mCamera.startPreview();

        } catch (Exception e) {
            Log.d(TAG, "Error starting camera preview: " + e.getMessage());
        }
    }

    /*This method is overridden from the camera class to do stuff on every frame that is taken
     * from the camera, in the form of the byte[] bytes array.
     *
     * */
    @Override
    public void onPreviewFrame(byte[] bytes, Camera camera) {
        if (imageFormat == ImageFormat.NV21 && isrunning) {
            if (mProcessInProgress) {
                mCamera.addCallbackBuffer(bytes);
            }
            if (bytes == null) {
                return;
            }
            mCamera.addCallbackBuffer(bytes);
            /*
             * Here we rotate the byte array (because of some wierd feature in my phone at least)
             * if your picture is horizontal, delete the rotation of the byte array.
             * */
            bytes = rotateYUV420Degree90(bytes, width, height);

            if (mBitmap == null) {
                mBitmap = Bitmap.createBitmap(height, width,
                        Bitmap.Config.ARGB_8888);
                myCameraPreview.setImageBitmap(mBitmap);
            }

            myCameraPreview.invalidate();
            mProcessInProgress = true;
            mCamera.addCallbackBuffer(bytes);
            // Start our background thread to process images
            new ProcessPreviewDataTask().execute(bytes);

        }
    }

    /* This class is run on another thread in the background, and when it's done with the decoding,
     * onPostExectue is called to set the new pixel array to the image we have.
     * In doInBackground you can change the values of the RGB pixel array to correspond to your
     * preferred colors. */
    private class ProcessPreviewDataTask extends AsyncTask<byte[], Void, Boolean> {

        @Override
        protected Boolean doInBackground(byte[]... datas) {
            byte[] data = datas[0];
            // I use the tempWidth and tempHeight because of the rotation of the image, if your
            // picture is horizontal, use width and height instead.
            int tempWidth = height;
            int tempHeight = width;

            // Here we decode the image to a RGB array.
            pixels = decodeYUV420SP(data, tempWidth, tempHeight);

            int sumR = 0;

            int r, g, b;

            //Testing only considering upper half
            int upperHalfHeight = tempHeight / 2;
            int leftHalfWidth = tempWidth / 2;

            for (int y = 0; y < upperHalfHeight; y++) {
                for (int x = 0; x < leftHalfWidth; x++) {
                    int i = y * tempWidth + x;

                    r = (pixels[i] >> 16) & 0xff;
                    g = (pixels[i] >> 8) & 0xff;
                    b = (pixels[i]) & 0xff;

                    sumR += r;

                    pixels[i] = 0xff000000 | (r << 16) | (g << 8) | b;
                }
            }
            double totalPixels = upperHalfHeight * tempWidth;
            double redAvg = sumR / totalPixels;

            if(redAvg >= 80){
                if(!isMeasuring)isMeasuring = true;
            }else{
                if(isMeasuring)isMeasuring = false;
                startTime = System.currentTimeMillis();
            }

            double timeNow = System.currentTimeMillis();
            timestamp = (timeNow - startTime) / 1000d;

            if(isMeasuring && timestamp > FirstDelayInSecs){
                redAVGs.add(redAvg);
                timeStamps.add(timestamp-FirstDelayInSecs);
                framesCounter++;
            }else{
                redAVGs = new ArrayList<Double>();
                timeStamps = new ArrayList<Double>();
                framesCounter = 0;
            }
            mCamera.addCallbackBuffer(data);
            mProcessInProgress = false;
            return true;
        }

        @Override
        protected void onPostExecute(Boolean result) {
            myCameraPreview.invalidate();
            mBitmap.setPixels(pixels, 0, height, 0, 0, height, width);
            myCameraPreview.setImageBitmap(mBitmap);

            if(!isMeasuring){
                plotRedAvg = new LineGraphSeries<>();
                graph.removeAllSeries();
                avgText.setText(light_too_weak);
                measuring_time.setText("");
                return;
            }

            if(timestamp <= FirstDelayInSecs){
                avgText.setText(start_measuring);
                measuring_time.setText("");
                return;
            }
            plotRedAvg.appendData(new DataPoint(timestamp, redAVGs.get(redAVGs.size()-1)), true, 40, false);
            if(framesCounter%10 == 0)
            {
                plotRedAvg.setColor(Color.WHITE);
                graph.addSeries(plotRedAvg);
            }

            if (timestamp-FirstDelayInSecs >= MEASURE_TIME) {
                double avgDeltaT = getAvgDeltaT();
                double bpm = fft(redAVGs.toArray(new Double[0]), framesCounter, avgDeltaT) * 60;

                isrunning = false;
                Intent intent = new Intent(parent, ResultActivity.class);
                intent.putExtra("BPM",String.format("%.0f", bpm));
                parent.finish();
                parent.startActivity(intent);
            } else if (framesCounter % 60 == 0) {
                int peaks = numberOfPeaks(smooth(redAVGs), 0.0);
                double bpmEstimate = (peaks / timestamp) * 60;
                avgText.setText(String.format("%.0f",bpmEstimate));
                avgText.append(" bpm");
                measuring_time.setText(String.format("%.4f", timeStamps.get(timeStamps.size() - 1)));
            }

            String current_avg_text = avgText.getText().toString();
            if(current_avg_text.compareTo(light_too_weak) == 0 || current_avg_text.compareTo(start_measuring) == 0)avgText.setText("");
            measuring_time.setText(String.format("%.1f", Math.abs(15.0-timeStamps.get(timeStamps.size() - 1))));
            measuring_time.append(" sec");
        }

    }


    /*Decoding and rotating methods from github
     * This method rotates the NV21 image (standard image that comes from the preview)
     * since this is a byte array, it must be switched correctly to match the pixels*/
    private byte[] rotateYUV420Degree90(byte[] data, int imageWidth, int imageHeight) {
        byte[] yuv = new byte[imageWidth * imageHeight * 3 / 2];
        // Rotate the Y luma
        int i = 0;
        for (int x = 0; x < imageWidth; x++) {
            for (int y = imageHeight - 1; y >= 0; y--) {
                yuv[i] = data[y * imageWidth + x];
                i++;
            }
        }
        // Rotate the U and V color components
        i = imageWidth * imageHeight * 3 / 2 - 1;
        for (int x = imageWidth - 1; x > 0; x = x - 2) {
            for (int y = 0; y < imageHeight / 2; y++) {
                yuv[i] = data[(imageWidth * imageHeight) + (y * imageWidth) + x];
                i--;
                yuv[i] = data[(imageWidth * imageHeight) + (y * imageWidth) + (x - 1)];
                i--;
            }
        }
        return yuv;
    }

    /* Decodes the image from the NV21 format into an RGB-array with integers.
     * Since the NV21 array is made out of bytes, and one pixel is made out of 1.5 bytes, this is
     * quite hard to understand. If you want more information on this you can read about it on
     * */
    public int[] decodeYUV420SP(byte[] yuv, int width, int height) {

        final int frameSize = width * height;

        int rgb[] = new int[width * height];
        final int ii = 0;
        final int ij = 0;
        final int di = +1;
        final int dj = +1;

        int a = 0;
        for (int i = 0, ci = ii; i < height; ++i, ci += di) {
            for (int j = 0, cj = ij; j < width; ++j, cj += dj) {
                int y = (0xff & ((int) yuv[ci * width + cj]));
                int v = (0xff & ((int) yuv[frameSize + (ci >> 1) * width + (cj & ~1) + 0]));
                int u = (0xff & ((int) yuv[frameSize + (ci >> 1) * width + (cj & ~1) + 1]));
                y = y < 16 ? 16 : y;

                int r = (int) (1.164f * (y - 16) + 1.596f * (v - 128));
                int g = (int) (1.164f * (y - 16) - 0.813f * (v - 128) - 0.391f * (u - 128));
                int b = (int) (1.164f * (y - 16) + 2.018f * (u - 128));

                r = r < 0 ? 0 : (r > 255 ? 255 : r);
                g = g < 0 ? 0 : (g > 255 ? 255 : g);
                b = b < 0 ? 0 : (b > 255 ? 255 : b);

                rgb[a++] = 0xff000000 | (r << 16) | (g << 8) | b;
            }
        }
        return rgb;
    }


    private double fft(Double[] input, int size, double avgDeltaT) {
        double[] output = new double[2 * size];
        double max_amp = 0;
        double max_freq = 0;

        for (int i = 0; i < size; i++) output[i] = input[i];

        DoubleFft1d fft = new DoubleFft1d(size);
        fft.realForward(output);

        for(int i = 0; i < 2 * size; i+=2) {
            double a = output[i];
            double b = output[i+1];

            double pds = Math.pow(a,2) + Math.pow(b,2);
            double curr_freq = (double) i / (avgDeltaT * ((2 * size) - 1));

            if (curr_freq >= 0.75 && curr_freq <= 10 / 3d) {
                if(max_amp < pds){
                    max_amp = pds;
                    max_freq = curr_freq;
                }
            }
        }
        return max_freq;
    }

    private double getAvgDeltaT(){
        double avgDeltaT = 0;
        for (int i = 0; i < timeStamps.size() - 1; i++) {
            avgDeltaT += timeStamps.get(i + 1) - timeStamps.get(i);
        }
        avgDeltaT /= (timeStamps.size() - 1);
        return avgDeltaT;
    }

    public int numberOfPeaks(List<Double> signal, double threshold) {
        int numPeaks = 0;
        for (int i = 1; i < signal.size() - 1; i++) {
            // Check if the difference between the current point and its neighbor is larger than the threshold
            if (Math.abs(signal.get(i)- signal.get(i - 1)) >= threshold && Math.abs(signal.get(i) - signal.get(i+1)) >= threshold) {
                if (signal.get(i) > signal.get(i - 1) && signal.get(i) > signal.get(i + 1)) {
                    numPeaks++;
                }
            }
        }
        return numPeaks;
    }

    public double smoothCurrentValue(double current, List<Double> previousValues) {
        // Apply exponential moving average
        double smoothedValue = current;

        if (previousValues.isEmpty()) {
            return current;
        }
        double smoothedSum = current;
        int windowSize = previousValues.size()+1;

        for(Double v : previousValues){
            smoothedSum+=v;
        }
        smoothedValue = smoothedSum/windowSize;

        return smoothedValue;
    }

    public List<Double> smooth(List<Double> avgReds) {
        int windowSize = 7;
        List<Double> smoothedValues = new ArrayList<>();
        List<Double> buffer = new ArrayList<>();
        for (double value : avgReds) {
            buffer.add(value);
            if (buffer.size() > windowSize) {
                buffer.remove(0);
            }
            double sum = 0.0;
            for (double bufferedValue : buffer) {
                sum += bufferedValue;
            }
            double movingAverage = sum / buffer.size();
            smoothedValues.add(movingAverage);
        }
        return smoothedValues;
    }

}
