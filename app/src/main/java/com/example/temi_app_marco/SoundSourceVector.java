package com.example.temi_app_marco;

import androidx.annotation.NonNull;

public class SoundSourceVector {
    private double maximum = 0;
    private int maximumId = 0;
    private SoundSource[] items;
    private static SoundSourceVector sourceVector = null;
    private static final int VECTOR_SIZE = 360;
    private static final double STD_DEVIATION = 1;
    private static final double MEAN = 0;
    private static final double ALPHA = 0.5;
    private static final String TAG = "OnSSVector";
    private int k = 0;
    public final double[] SSV_Output = new double[10000];

    //DO NOT FORGET: ALL AZIMUTH ANGLES IN INTERVAL ]-180, 180] !!!!
    private SoundSourceVector(){
        this.maximum = 0;
        this.maximumId = 0;
        this.items = new SoundSource[VECTOR_SIZE];
        for(int i = 0; i < VECTOR_SIZE; i++){
            this.items[i] = new SoundSource(i-179,0); //center is 179
        }
    }

    public static SoundSourceVector get(){
        if(sourceVector == null){
            sourceVector = new SoundSourceVector();
        }
        return sourceVector;
    }

    //TODO: SEE IF IT IS + OR - OFFSET
    //TODO: MUST BE TESTED WITH TEMI
    //TODO: CHECK IF ELEVATION WORKS
    //4 gaussians at the same time
    //all angles in ]-180, 180]
    //assume mean as 0 and x as difference restricted to the interval ]-180, 180]
    public void addNewSource(@NonNull SoundSource[] soundSources, double offset){
        double[] azimuth = new double[soundSources.length];
        double[] elevation = new double[soundSources.length];
        int localMaximumId = 0;
        maximum = 0;
        for( int j = 0; j < soundSources.length; j++){
            if(soundSources[j] == null){
                continue;
            }
            azimuth[j] = soundSources[j].getAzimuth() + offset;
            azimuth[j] = tools.wrapTo180(azimuth[j]);
            elevation[j] = soundSources[j].getElevation();
        }
        double confidence, finConfidence, angle;
        for (int i = 0; i < VECTOR_SIZE; i++){
            confidence = 0;
            for( int j = 0; j < soundSources.length; j++){
                if(soundSources[j] == null){
                    continue;
                }
                angle = tools.wrapTo180(this.items[i].getAzimuth() - azimuth[j]); //center Gaussian in zero
                confidence += tools.gaussianPdf(MEAN, STD_DEVIATION, angle);
                if(confidence > 0.01){
                    this.items[i].setElevation(elevation[j]);
                }
            }
            finConfidence = this.items[i].setConfidence(confidence, ALPHA);
            if(finConfidence > maximum){
                maximum = finConfidence;
                localMaximumId = i;
            }
        }
        if(maximum > 0.3){
            maximumId = localMaximumId;
        }
        //Log.i(TAG, "Most Probable Angle: " + this.items[maximumId].getAzimuth() + "\n confidence: " + maximum);
        if(k<10000){
            SSV_Output[k] = this.items[maximumId].getAzimuth();
            k++;
        }else {
            //Log.i("FINNISH", "Finished SSV");
        }
    }

    public SoundSource getMostProbableSource(){
        return this.items[maximumId];
    }


    //After analysing a SoundSource, you should reset its confidence in order to analyse other sources
    public void resetConfidence(SoundSource soundSource, double radius){
        double azimuth = soundSource.getAzimuth();
        double angle, finConfidence;
        maximum = 0;
        for(int i = 0; i < VECTOR_SIZE; i++){
            angle = tools.wrapTo180(this.items[i].getAzimuth()-azimuth);
            if( angle < radius){
                this.items[i].setConfidence(0,0);
            }
            finConfidence = this.items[i].getConfidence();
            if(finConfidence > maximum){
                maximum = finConfidence;
                maximumId = i;
            }
        }
    }


}
