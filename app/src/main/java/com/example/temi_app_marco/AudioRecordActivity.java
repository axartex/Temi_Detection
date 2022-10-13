package com.example.temi_app_marco;

import android.Manifest;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import AlizeSpkRec.AlizeException;

public class AudioRecordActivity extends AppCompatActivity {

    protected static final int RECORD_FS = 8000;
    protected static final int RECORD_CHANNELS = AudioFormat.CHANNEL_IN_MONO;
    protected static final int RECORD_ENCODING = AudioFormat.ENCODING_PCM_16BIT;
    protected static final int REQUEST_MICROPHONE = 23;
    protected static AudioRecord audioRecord = null;
    protected static int bufferSize = 2000;
    protected static int bytesPerElement = 2;
    protected boolean recordEmpty = true;
    protected boolean recordError = false;
    protected boolean recordExist = false;
    protected boolean isRecording = false;
    protected SpeakerRecognition speakerRecognitionSystem;
    protected Thread recordThread = null, sendSamplesThread = null, timerThread = null;
    protected long startTime;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        try {
            speakerRecognitionSystem = SpeakerRecognition.get(getApplicationContext());
        } catch (IOException | AlizeException e) {
            e.printStackTrace();
        }
    }

    protected boolean checkMicrophonePermission(){
        return ContextCompat.checkSelfPermission(getApplicationContext(),
                Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED;
    }

    protected void requestMicrophonePermission(){
        if (ActivityCompat.shouldShowRequestPermissionRationale(this,Manifest.permission.RECORD_AUDIO)) {
            new AlertDialog.Builder(this)
                    .setTitle("Permission needed")
                    .setMessage("This Permission is needed for the voice features!")
                    .setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            ActivityCompat.requestPermissions(AudioRecordActivity.this,
                                    new String[]{Manifest.permission.RECORD_AUDIO}, REQUEST_MICROPHONE);
                        }
                    })
                    .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            dialogInterface.dismiss();
                        }
                    }).create().show();
        }else{
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.RECORD_AUDIO}, REQUEST_MICROPHONE);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_MICROPHONE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Permission GRANTED", Toast.LENGTH_SHORT).show();
                start_record();
            } else {
                Toast.makeText(this, "Permission DENIED", Toast.LENGTH_SHORT).show();
            }
        }
    }

    /*protected static void start() {
        *//*minSize = AudioRecord.getMinBufferSize(RECORD_FS, RECORD_CHANNELS,
                RECORD_ENCODING);*//*
        audioRecord = start_record(RECORD_CHANNELS, minSize);
    }*/

    protected void setThreshold(double newThreshold){
        try {
            speakerRecognitionSystem.setDecisionThreshold(newThreshold);
        } catch (AlizeException e) {
            e.printStackTrace();
        }
    }

    protected void start_record(){
        if(!checkMicrophonePermission()){
            requestMicrophonePermission();
            return;
        }

        recordEmpty = true;
        recordError = false;
        isRecording = true;

        audioRecord = new AudioRecord(MediaRecorder.AudioSource.MIC, RECORD_FS,
                RECORD_CHANNELS, RECORD_ENCODING, bufferSize * bytesPerElement);
        audioRecord.startRecording();

        //If there is already a record we must reset Alize System
        if(recordExist){
            try {
                speakerRecognitionSystem.resetAudio();
                speakerRecognitionSystem.resetFeatures();
            } catch (AlizeException e) {
                e.printStackTrace();
            }
            recordExist = false;
        }

        final List<short[]> audioPackets = Collections.synchronizedList(new ArrayList<short[]>());

        //Audio Record thread
        recordThread = new Thread(new Runnable() {
            @Override
            public void run() {
                startTime = System.currentTimeMillis();
                short[] tmpBuffer = new short[bufferSize];
                int readSamples;
                short[] samples;
                while (audioRecord.getRecordingState() == AudioRecord.RECORDSTATE_RECORDING){
                    readSamples = audioRecord.read(tmpBuffer, 0, bufferSize);
                    if(readSamples > 0){
                        samples = new short[readSamples];
                        for ( int k = 0; k < readSamples; k++){
                            samples[k] = tmpBuffer[k];
                            if(samples[k] != 0){
                                recordEmpty = false;
                            }
                        }

                        synchronized (audioPackets) {
                            audioPackets.add(samples);
                        }
                    }
                }
            }
        }, "Audio Record Thread");

        //Send audio to Alize System thread
        sendSamplesThread = new Thread(new Runnable() {
            @Override
            public void run() {
                short[] nextSample;
                while((audioRecord.getRecordingState() == AudioRecord.RECORDSTATE_RECORDING) ||
                        (!audioPackets.isEmpty())){
                    nextSample = null;

                    synchronized (audioPackets){
                        if(!audioPackets.isEmpty()){
                            nextSample = audioPackets.get(0);
                            audioPackets.remove(0);
                        }
                    }
                    sendSamplesMethod(nextSample);
                }
                try {
                    recordThread.join();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                while (!audioPackets.isEmpty()){
                    nextSample = audioPackets.get(0);
                    audioPackets.remove(0);
                    sendSamplesMethod(nextSample);
                }
            }
            public void sendSamplesMethod(short[] nextSample){
                if(nextSample != null){
                    try {
                        speakerRecognitionSystem.addAudio(nextSample);
                    } catch (AlizeException e) {
                        e.printStackTrace();
                        recordError = true;
                    } catch (Throwable e) { // just like in demo code
                        e.printStackTrace();
                        recordError = true;
                    }
                }
            }
        }, "Send Samples Thread");

        /*//Timer manager thread
        timerThread = new Thread(new Runnable() {
            private Handler handler = new Handler();
            @Override
            public void run() {
                while (audioRecord.getRecordingState() == AudioRecord.RECORDSTATE_RECORDING){
                    handler.post(new Runnable() {
                        @Override
                        public void run() {
                            long currentTime = System.currentTimeMillis() - startTime;
                            String result = new SimpleDateFormat("mm:ss:SS").format(new Date(currentTime));
                        }
                    });
                    try {
                        Thread.sleep(10);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        }, "Timer Manager Thread");*/

        recordThread.start();
        sendSamplesThread.start();
        //timerThread.start();
    }

    /*public static void stop() {
        audioRecord = stop_record(audioRecord);
    }*/

    protected void stop_record(){
        if (audioRecord != null) {
            audioRecord.stop();
            try {
                recordThread.join();
                sendSamplesThread.join();
                if(timerThread != null) {
                    timerThread.join();
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            audioRecord.release();
            audioRecord = null;
            recordExist = !recordError;
            isRecording = false;
            recordThread = null;
            sendSamplesThread = null;
            timerThread = null;

            String toastText = getResources().getString(R.string.record_complete);
            if(recordError){
                try {
                    speakerRecognitionSystem.resetAudio();
                    speakerRecognitionSystem.resetFeatures();
                } catch (AlizeException e) {
                    e.printStackTrace();
                }
                toastText = getResources().getString(R.string.record_error);
            }
            Toast.makeText(getApplicationContext(), toastText, Toast.LENGTH_SHORT).show();
            onRecordStopped();
        }
    }

    protected void onRecordStopped(){}

    /*public static boolean return_status() {
        return audioRecord != null;
    }*/

    /*public static float getPitch() {
        short[] buffer;
        int r_shorts;
        buffer = new short[minSize];
        r_shorts = audioRecord.read(buffer, 0, minSize);
        float max = 0;
        if (r_shorts < 0) {
            throw new IllegalStateException("Recording Error: " + r_shorts);
        }
        for ( short k : buffer){
            max = Math.abs(k) > max? k:max;
        }
        return max;
    }*/

}
