package com.example.android.HeartTracker;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.ImageFormat;
import android.hardware.Camera;
import android.os.AsyncTask;
import android.os.Environment;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.io.BufferedWriter;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static android.content.ContentValues.TAG;

/** A basic Camera preview class */
public class CameraPreview extends SurfaceView implements SurfaceHolder.Callback, Camera.PreviewCallback{
    private SurfaceHolder mHolder;
    private Camera mCamera;
    private int imageFormat;
    private boolean mProcessInProgress = false;
    private Bitmap mBitmap = null;
    private ImageView myCameraPreview;
    private int[] pixels = null;
    public int width ,height;
    private Camera.Parameters params;

    private List<Integer> redAVGs;

    private static final String DataFile = "RedAVGs.txt";
    private TextView avgText;

    public CameraPreview(Context context, Camera camera, ImageView mCameraPreview, LinearLayout layout, TextView avgText) {
        super(context);
        mCamera = camera;
        params = mCamera.getParameters();
        imageFormat = params.getPreviewFormat();

        int minWidth = Integer.MAX_VALUE;
        int minHeight = Integer.MAX_VALUE;

        for (Camera.Size previewSize: mCamera.getParameters().getSupportedPreviewSizes())
        {
            if(previewSize.width < minWidth || previewSize.height < minHeight) {
                minWidth= previewSize.width;
                minHeight = previewSize.height;
            }
        }
        params.setPreviewSize(minWidth, minHeight);
        params.setFlashMode(Camera.Parameters.FLASH_MODE_TORCH);
        width = minWidth;
        height = minHeight;
        Log.d("ImageSize","width: " + width + " height: " + height);

        mCamera.setParameters(params);
        myCameraPreview = mCameraPreview;
        // Install a SurfaceHolder.Callback so we get notified when the
        // underlying surface is created and destroyed.
        mHolder = getHolder();
        mHolder.addCallback(this);
        // deprecated setting, but required on Android versions prior to 3.0
        mHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
        layout.addView(myCameraPreview);

        redAVGs = new ArrayList<Integer>();
        this.avgText = avgText;
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
        // empty. Taken care of in our activity.
    }
    /* If the application is allowed to rotate, here is where you would change the camera preview
    * size and other formatting changes.*/
    public void surfaceChanged(SurfaceHolder holder, int format, int w, int h) {
        if (mHolder.getSurface() == null){
            return;
        }
        try {
            mCamera.stopPreview();
        } catch (Exception e){
        }
        try {
            mCamera.setPreviewDisplay(mHolder);
            mCamera.startPreview();

        } catch (Exception e){
            Log.d(TAG, "Error starting camera preview: " + e.getMessage());
        }
    }
    /*This method is overridden from the camera class to do stuff on every frame that is taken
    * from the camera, in the form of the byte[] bytes array.
    *
    * */
    @Override
    public void onPreviewFrame(byte[] bytes, Camera camera) {
        if (imageFormat == ImageFormat.NV21){
            if(mProcessInProgress){
                mCamera.addCallbackBuffer(bytes);
            }
            if (bytes == null){
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

            int r,g,b;

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
            int totalPixels = upperHalfHeight * tempWidth;
/*
            for (int i = 0; i < pixels.length; i++) {
                r = (pixels[i] >> 16) & 0xff;
                g = (pixels[i] >> 8) & 0xff;
                b = (pixels[i]) & 0xff;

                sumR+=r;
                sumG+=g;
                sumB+=b;

                //pixels[i] = 0xff000000 | (r << 16) | (g << 8) | b;
            }
*/
            //Log.i("pixel-length", String.valueOf(pixels.length));
            //Log.i("totalpixels", String.valueOf(totalPixels));
            //Log.i("AverageRedFrame", );
            int temp = sumR/totalPixels;
            redAVGs.add(temp);

            saveAsText("average_red_values.txt", Integer.parseInt(String.valueOf(temp)));





            mCamera.addCallbackBuffer(data);
            mProcessInProgress = false;
            return true;
        }

        @Override
        protected void onPostExecute(Boolean result){
            myCameraPreview.invalidate();
            mBitmap.setPixels(pixels, 0, height,0, 0, height, width);
            myCameraPreview.setImageBitmap(mBitmap);
            avgText.setText(String.valueOf(redAVGs.get(redAVGs.size() - 1)));
            //save(DataFile, redAVGs);
        }
    }

    /*Decoding and rotating methods from github
    * This method rotates the NV21 image (standard image that comes from the preview)
    * since this is a byte array, it must be switched correctly to match the pixels*/
    private byte[] rotateYUV420Degree90(byte[] data, int imageWidth, int imageHeight)
    {
        byte [] yuv = new byte[imageWidth*imageHeight*3/2];
        // Rotate the Y luma
        int i = 0;
        for(int x = 0;x < imageWidth;x++)
        {
            for(int y = imageHeight-1;y >= 0;y--)
            {
                yuv[i] = data[y*imageWidth+x];
                i++;
            }
        }
        // Rotate the U and V color components
        i = imageWidth*imageHeight*3/2-1;
        for(int x = imageWidth-1;x > 0;x=x-2)
        {
            for(int y = 0;y < imageHeight/2;y++)
            {
                yuv[i] = data[(imageWidth*imageHeight)+(y*imageWidth)+x];
                i--;
                yuv[i] = data[(imageWidth*imageHeight)+(y*imageWidth)+(x-1)];
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

    private void saveAsText(String fileName, int averageRedValue) {
        String state = Environment.getExternalStorageState();
        if (!Environment.MEDIA_MOUNTED.equals(state)) {
            // External storage is not available
            return;
        }

        File file = new File(Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_DOWNLOADS), fileName);

        try (FileWriter writer = new FileWriter(file, true);
             BufferedWriter bw = new BufferedWriter(writer);
             PrintWriter out = new PrintWriter(bw)) {

            // Write the average red value as a string
            out.println(String.valueOf(averageRedValue));

            Log.i("File saved", "Now");

        } catch (IOException e) {
            e.printStackTrace();
            Log.e("Error saving file", e.getMessage());
        }
    }

}
