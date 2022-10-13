package com.example.temi_app_marco;

import android.content.Context;

import java.io.FileOutputStream;
import java.io.IOException;

public class tools {

    private static final String FileNameODAS = "ODAS_output.csv";
    private static final String FileNameFilter = "Filter_output.csv";

    public static int inArray(int[] array, int key){
        for( int i = 0; i < array.length; i++ ){
            if(array[i] == key){
                return i;
            }
        }
        return -1;
    }

    //type = 1 is for ODAS
    //type = 2 is for Filter
    public static void writeFileFilter(double[] angles, int type, Context ctx) throws IOException {
        String FileName;
        if(type == 1){
            FileName = FileNameODAS;
        }else {
            FileName = FileNameFilter;
        }
        FileOutputStream out = null;
        out = ctx.openFileOutput(FileName, Context.MODE_APPEND);
        for(double angle : angles){
            String entry = String.valueOf(angle) + "\n";
            out.write(entry.getBytes());
        }
        out.close();
    }

    public static void writeFileODAS(double[][] angles, int type, Context ctx) throws IOException {
        String FileName;
        if(type == 1){
            FileName = FileNameODAS;
        }else {
            FileName = FileNameFilter;
        }
        FileOutputStream out = null;
        out = ctx.openFileOutput(FileName, Context.MODE_APPEND);
        for(double[] angle : angles){
            String entry = "";
            for(double value : angle){
                entry = entry + String.valueOf(value) + ",";
            }
            entry = entry + "\n";
            out.write(entry.getBytes());
        }
        out.close();
    }

    public static int argMax(double[] array){
        int maxIndex = 0;
        double max_value = array[0];
        for(int i = 0; i < array.length; i++){
            if ( max_value < array[i]){
                max_value = array[i];
                maxIndex = i;
            }
        }
        return maxIndex;
    }

    public static boolean isEmptyOrBlank(String string){
        return string == null || string.isEmpty() || string.trim().isEmpty();
    }

    public static double integralIn(double a, double b, String function, double[] params){
        switch (function){
            case "gaussian":
                return tools.gaussianIntegral(a, b, params);
            default:
                throw new IllegalStateException("Unexpected value: " + function);
        }
    }

    //Implementation of Simpson's 3/8 rule
    public static double gaussianIntegral(double a, double b, double[] params){
        if(params.length != 2){
            return -1;
        }
        int n = 1000;
        double h = (b-a)/n;
        double aux1 = (3*h)/8;
        double res = gaussianPdf(params[0], params[1], a) + gaussianPdf(params[0], params[1], b);
        for (int i = 1; i < n; i++){
            if(i%3 == 0){
                res = res + 2*gaussianPdf(params[0], params[1], a+i*h);
            }else {
                res = res + 3*gaussianPdf(params[0], params[1], a+i*h);
            }
        }
        res = res*h*3/8;
        return res;
    }

    public static double gaussianPdf(double mean, double stdDeviation, double x){
        double aux = ((x-mean)/stdDeviation);
        double var = stdDeviation*stdDeviation;
        return Math.exp(-0.5*aux*aux)/Math.sqrt(2*Math.PI*var);
    }

    public static double wrapTo180(double angle){
        angle = angle % 360; //angle in ]-359,359[
        angle = (angle+360) % 360; //angle in [0, 360[
        angle = (angle > 180) ? angle-360 : angle; // angle in ]-180,180]
        return angle;
    }
}
