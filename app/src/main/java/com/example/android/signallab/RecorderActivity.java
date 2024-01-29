package com.example.android.signallab;

import android.os.Bundle;
import android.os.Environment;
import android.content.Context;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioRecord;
import android.media.AudioTrack;
import android.media.MediaRecorder;
import android.renderscript.Sampler;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import com.gauravk.audiovisualizer.visualizer.BarVisualizer;

import java.io.DataOutputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

/*
* AudioManager provides access to the volume and ringer
* AudioRecord "pulls" or reads data into an array with the read() method.
* AudioTrack manages and plays a single audio resource for Java applications.
* It allows streaming of PCM audio buffers to the audio sink for playback.
* This is achieved by "pushing" the data to the AudioTrack object using one of the write() methods
 */
public class RecorderActivity extends AppCompatActivity {
    private boolean isRecording = false;
    private AudioManager am = null;
    private AudioRecord record = null;
    private AudioTrack track = null;
    private int sampleRateHz = 8000;
    private Button startRecording, stopRecording;
    BarVisualizer mViz;
    byte[] inAudioBuffer;
    byte[] outAudioBuffer;
    byte[] workBuffer;
    byte[] filter;
    private static final String DataFile = "Test.txt"; //Name of the file to which the data is exported


    private static int filterLength = 256;
    private static int bufferSize = 1024;
    private static int workBufferSize = bufferSize*10;
    private int bufferPosition;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
 
       	super.onCreate(savedInstanceState);
        workBuffer = new byte[workBufferSize];
        filter = new byte[filterLength];
        bufferPosition = 0;
        setContentView(R.layout.activity_recorder);
        setVolumeControlStream(AudioManager.MODE_IN_COMMUNICATION);
        /* Initializing AudioRecord and AudioTrack */
        initializeRecordAndTrack();

        mViz = findViewById(R.id.bar);

        am = (AudioManager) this.getSystemService(Context.AUDIO_SERVICE);
        //am.setSpeakerphoneOn(true);


        /*
        * Start a new thread to run recordAndPlay to not interfere with UI-thread.
        * Since we are probably going to do some visualization on the UI-thread, it cannot be stopped
        * by the record and play which takes up some of the processing power.
        */
        (new Thread()
        {
            @Override
            public void run()
            {
                recordAndPlay();
            }
        }).start();

        startRecording = findViewById(R.id.record);
        startRecording.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (!isRecording)
                {
                    startRecordingAndPlaying();
                }
            }
        });
        stopRecording = findViewById(R.id.stop);
        stopRecording.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (isRecording)
                {
                    stopRecordingAndPlaying();
                }
            }
        });

    }
    /*
    * Records audio with read(), into inAudioBuffer, with a maxiumum length of 1024 byte-values.
    * Then plays back the audio with write, from audioBuffer with a length of how many bytes
    * were read into the array.
    * num = the number of bytes read into the audioBuffer, zero, or an error code
    *
     */
    private void recordAndPlay()
    {
        inAudioBuffer = new byte[bufferSize];
        outAudioBuffer= new byte [bufferSize];
        int num;

        CalculationThread ct = null;

        double coeff = 1;
        double volume = 1;

        boolean runFilterCalculator = false;
        am.setMode(AudioManager.MODE_IN_COMMUNICATION);
        while (true)
        {
            if (isRecording)
            {
                /*Read from microphone and put in buffer*/
                num = record.read(inAudioBuffer, 0,bufferSize);

                /*Change volume by scaling audio sample values*/
                Log.i("value of coeff", "TESTING VALUE= "+coeff);
                //coeff/=100;
                //coeff = (coeff<1 ? 1: coeff);
                //coeff = (coeff>5 ? 5: coeff);
                //volume = Math.max(1, Math.min(5, volume));
                //volume /= (coeff/100);
                //volume = (volume<5 ? 5: volume);
                //volume = (volume>0.5 ? 0.5: volume);
                //volume = Math.max(1, Math.min(5, volume));
                //double volume=1/coeff;
                volume = 100 / coeff;
                volume = (volume>3 ? 3: volume);
                volume = (volume<0.2 ? 0.2: volume);

                for (int i = 0; i < inAudioBuffer.length; i+=2) {
                    /*Read one sample from the buffer*/
                    ByteBuffer bb = ByteBuffer.allocate(2);
                    bb.order(ByteOrder.LITTLE_ENDIAN);
                    bb.put(inAudioBuffer[i]);
                    bb.put(inAudioBuffer[i+1]);
                    short audioSample = bb.getShort(0);

                    /*Scale sample value*/
                    audioSample = (short) (audioSample * volume);

            	    /*TODO do stuff with audioSample data */
                    if(i%4 == 0){
                        audioSample = (short) (-audioSample);
                    }

                    /*Put scaled sample value in buffer*/
                    bb.putShort(0,audioSample);
                    outAudioBuffer[i]=bb.get(0);
                    outAudioBuffer[i + 1] = bb.get(1);
                }
                /*Copy audio data from inAudioBuffer to outAudioBuffer*/
                 //  outAudioBuffer = inAudioBuffer;

                /*Write from buffer to speaker*/
                track.write(outAudioBuffer, 0, num);

                if (ct == null)//if filterupdater is not running then...
                {
                    //copy data to workBuffer
                    //check that workBuffer is not overrun
                    if (bufferPosition + num > workBuffer.length - 1) {
                        java.lang.System.arraycopy(inAudioBuffer, 0, workBuffer, bufferPosition, workBuffer.length - bufferPosition - 1);
                        //start the filter updater now!!
                        runFilterCalculator = true;
                    } else {
                        java.lang.System.arraycopy(inAudioBuffer, 0, workBuffer, bufferPosition, num);
                        //check if the buffer is full
                        if (bufferPosition + num >= workBuffer.length - 1) {
                            //start the filter updater now!!
                            runFilterCalculator = true;
                        }
                    }

                    //should not update bufferposition if the buffer was reset to zero
                    if (runFilterCalculator) {
                        bufferPosition = 0;

                        ct = new CalculationThread();
                        ct.start();

                    } else {
                        bufferPosition += num;
                    }

                    runFilterCalculator= false;

                } else if (!ct.isAlive()) {
                    //Filter calculation is completed
                    coeff = ct.getFilterCoeffs();

                    ct = null;
                }

                try {
                    /* Graphical updates - display audio data from buffer*/
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            mViz.setRawAudioBytes(inAudioBuffer);

                        }
                    });
                    Thread.sleep(3);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }
    /*
    * Starts recording audio and plays the the audio that has been written to the AudioTrack
     */
    private void startRecordingAndPlaying()
    {
        record.startRecording();
        track.play();
        isRecording = true;
    }
    /*
    * Stops recording and pauses the audio being played
     */
    private void stopRecordingAndPlaying()
    {
        record.stop();
        track.pause();
        isRecording = false;
    }

    /*
    * Initializes the AudioRecord and AudioTrack.
    * Uses the sampleRate specified before, Format in Mono. Encoding in 16 bits.
    * getMinBufferSize returns the minimum buffer size required for the successful creation of an
    * AudioRecord object, in byte units.
     */
    private void initializeRecordAndTrack(){

        int minBufferSize = AudioRecord.getMinBufferSize(sampleRateHz,
                AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT);
        record = new AudioRecord(MediaRecorder.AudioSource.MIC, sampleRateHz,
                AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT, minBufferSize);
        int maxJitter = AudioTrack.getMinBufferSize(sampleRateHz,
                AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT);
        track = new AudioTrack(AudioManager.MODE_IN_COMMUNICATION, sampleRateHz,
                AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT, maxJitter,
                AudioTrack.MODE_STREAM);
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        record.release();
        track.release();
        if (mViz != null)
            mViz.release();
    }
    @Override
    protected void onPause() {
        super.onPause();
        record.release();
        track.release();
        if (mViz != null)
            mViz.release();
    }
    @Override
    protected void onResume() {
        super.onResume();
        initializeRecordAndTrack();
        mViz = findViewById(R.id.bar);
    }


    private void save(String FILE_NAME, byte[] Data) {
        String state = Environment.getExternalStorageState();
        if (!Environment.MEDIA_MOUNTED.equals(state)) {
            return;
        }
        File file = new File(Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_DOWNLOADS), FILE_NAME);

        DataOutputStream fos = null;

        try {
            file.createNewFile();
            fos = new DataOutputStream(new FileOutputStream(file, true));

        } catch (Exception e) {
            e.printStackTrace();
	    return;
        }

        for (int i = 0; i < Data.length; i++) {
            //String textData = String.valueOf(Data[i]) + "\n";
		
            try {
                //fos.write(textData.getBytes()); //The vector is saved in a txt-file on the device
		fos.writeByte(Data[i]);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
            try {
                fos.close();
                Log.i("New file saved", "Now");
            } catch (IOException e) {
                e.printStackTrace();
            }
       
    }


    class CalculationThread extends Thread {
            double coeff;

            CalculationThread() {
                coeff = 0;
            }

            public void run() {
                // compute filter than minPrime
                coeff = 0;

                for (int i = 0; i < workBuffer.length; i+=2) {
                    /*Read one sample from the buffer*/
                    ByteBuffer bb = ByteBuffer.allocate(2);
                    bb.order(ByteOrder.LITTLE_ENDIAN);
                    bb.put(workBuffer[i]);
                    bb.put(workBuffer[i + 1]);
                    short audioSample = bb.getShort(0);
                    coeff += Math.abs(audioSample);
                }

                coeff /= (workBuffer.length/2);

                save(DataFile, workBuffer);
            }

            public double getFilterCoeffs() {
                return coeff;
            }

            ;
    }
}