package com.example.temi_app_marco;

import android.content.Context;
import android.util.Log;

import java.io.IOException;
import java.io.InputStream;

import AlizeSpkRec.AlizeException;
import AlizeSpkRec.IdAlreadyExistsException;
import AlizeSpkRec.SimpleSpkDetSystem;

public class SpeakerRecognition extends SimpleSpkDetSystem {

    private static SpeakerRecognition speakerRecognition = null;
    private final static String TAG = "Alize_System";
    private final static double THRESHOLD = 47;

    private SpeakerRecognition(InputStream configInput, String workdirPath) throws AlizeException, IOException {
        super(configInput, workdirPath);
    }

    public static SpeakerRecognition get(Context context) throws IOException, AlizeException {
        if( speakerRecognition == null){
            InputStream configFile = context.getAssets().open("AlizeDefault.cfg");
            speakerRecognition = new SpeakerRecognition(configFile, context.getFilesDir().getPath());
            configFile.close();

            //load UBM
            InputStream backgroundModel = context.getAssets().open("gmm/world.gmm");
            speakerRecognition.loadBackgroundModel(backgroundModel);
            backgroundModel.close();

            speakerRecognition.setDecisionThreshold(THRESHOLD);
            //check if everything is successfully loaded
            speakerRecognition.displayStatus();
        }
        return speakerRecognition;
    }

    private void displayStatus() throws AlizeException {
        Log.i(TAG, "***************************************************");
        Log.i(TAG, "System status:");
        Log.i(TAG, " Number of features: " + speakerRecognition.featureCount());
        Log.i(TAG, " Number of speakers: " + speakerRecognition.speakerCount());
        Log.i(TAG, " UBM successfully loaded: " + speakerRecognition.isUBMLoaded());
        Log.i(TAG, "***************************************************");
    }

    public void createSpeakerModel(int userId) throws IdAlreadyExistsException {
        String modelId = String.valueOf(userId);
        super.createSpeakerModel(modelId);
    }

    public SpkRecResult verifySpeakerModel(int userId) throws AlizeException {
        String targetSpeakerId = String.valueOf(userId);
        return super.verifySpeaker(targetSpeakerId);
    }

    public void updateSpeakerModel(int userId) throws AlizeException {
        String modelId = String.valueOf(userId);
        super.adaptSpeakerModel(modelId);
    }

    public void removeSpeakerModel(int userId) throws AlizeException{
        String modelId = String.valueOf(userId);
        super.removeSpeaker(modelId);
    }

    public void removeSpeakerModel(String userId) throws AlizeException{
        super.removeSpeaker(userId);
    }

    public void removeAllSpeakerModels() throws AlizeException {
        for(String speaker: speakerIDs()) {
            this.removeSpeakerModel(speaker);
        }
    }
}
