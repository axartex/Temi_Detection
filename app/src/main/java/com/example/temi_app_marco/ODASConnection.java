package com.example.temi_app_marco;

import android.util.Log;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;

public class ODASConnection {

    private static final String TAG = "ODAS_Connection";
    private final int MAX_LINES = 9;
    Thread serverThread = null;
    private final String[] direction = new String[MAX_LINES];
    private String local_message_1 = null;
    private final String[] local_direction = new String[MAX_LINES];
    public final double[][] ODAS_Output = new double[10000][4];
    //private PrintWriter out;
    private BufferedReader in_location;
    ServerSocket serverSocket_location; //ODAS localization of 4(can be changed) sound sources
    private boolean firstConnection = true;
    private boolean stop_localization = false;
    private static final int PORT_1 = 6000;
    private boolean status = false;
    private static ODASConnection connection;
    /*private int timeStamp_location = 0;
    private int timeStamp_classification = 0;*/
    //private int debug_run = 0;

    private SoundSourceVector sourceVector;
    private boolean isTurning = false;
    private double offset = 0;

    private ODASConnection() {
        sourceVector = SoundSourceVector.get();
        serverThread = new Thread(new ServerThread());
        serverThread.start();
    }

    public static ODASConnection get(){
        if(connection == null) {
            connection = new ODASConnection();
        }
        return connection;
    }

    public boolean getStatus(){
        return this.status;
    }

    public void startTurning(double angle) {
        Log.i(TAG, "START TURNING"+ String.valueOf(angle));
        isTurning = true;
        offset = angle;
    }

    public void stopTurning(){
        Log.i(TAG, "STOP TURNING");
        isTurning = false;
    }

    public void stopReadings(){
        isTurning = true;
    }

    class ServerThread implements Runnable{
        @Override
        public void run() {
            Socket clientSocket_location;
            try {
                if (firstConnection){
                    serverSocket_location = new ServerSocket(PORT_1);
                }
                status = false;
                try{
                    clientSocket_location = serverSocket_location.accept();
                    //out = new PrintWriter(clientSocket_location.getOutputStream(), true);
                    in_location = new BufferedReader(new InputStreamReader(clientSocket_location.getInputStream()));
                    status = true;
                    new Thread(new InputThread()).start();
                    if (firstConnection){
                        new Thread(new DirectionThread()).start();
                        firstConnection = false;
                    }
                }catch (IOException e){
                    e.printStackTrace();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private class InputThread implements Runnable{
        @Override
        public void run() {
            while(true){
                try{
                    for (int i = 0; i < MAX_LINES; i++){
                        local_message_1 = in_location.readLine();
                        if (local_message_1 == null){
                            break;
                        }
                        direction[i] = local_message_1;
                    }
                    if (local_message_1 == null){
                        direction[8] = null;
                        serverThread = new Thread(new ServerThread());
                        serverThread.start();
                        return;
                    }
                }catch (IOException e){
                    e.printStackTrace();
                }
            }
        }
    }
    private class DirectionThread implements Runnable{
        private int timeStamp = -1;

        @Override
        public void run() {
            int k = 0;
            while(true) {
                if (stop_localization){
                    return;
                }
                if (direction[8] != null && !isTurning) {
                    double x, y, z;
                    int id;
                    int bk1, bk2, bk3, bk4, bk5, bk6, bkt1, bkt2;
                    int localTimeStamp;
                    SoundSource[] soundSources = new SoundSource[4];
                    System.arraycopy(direction,0,local_direction,0,MAX_LINES);
                    bkt1 = local_direction[1].indexOf("\"timeStamp\"");
                    bkt2 = local_direction[1].indexOf(",");
                    localTimeStamp = Integer.parseInt(local_direction[1].substring(bkt1+13,bkt2));
                    if(localTimeStamp == timeStamp){
                        continue;
                    }
                    timeStamp = localTimeStamp;
                    for( int i = 3; i < 7; i++){
                        bk1 = local_direction[i].indexOf("\"id\"");
                        bk2 = local_direction[i].indexOf("\"tag\"");
                        bk3 = local_direction[i].indexOf("\"x\"");
                        bk4 = local_direction[i].indexOf("\"y\"", bk3 + 5);
                        bk5 = local_direction[i].indexOf("\"z\"", bk4 + 5);
                        bk6 = local_direction[i].indexOf("\"activity\"", bk5 + 5);
                        if (bk1 == -1){
                            break;
                        }
                        id = Integer.parseInt(local_direction[i].substring(bk1+6, bk2-2));
                        if (id == 0){
                            if(k<10000){
                                ODAS_Output[k][i-3] = -500;
                            }else {
                                //Log.i("FINNISH", "Finished ODAS" + (i-3));
                            }
                            continue;
                        }
                        x = Double.parseDouble(local_direction[i].substring(bk3+5, bk4-2));
                        y = Double.parseDouble(local_direction[i].substring(bk4+5, bk5-2));
                        z = Double.parseDouble(local_direction[i].substring(bk5+5, bk6-2));
                        soundSources[i-3] = SoundSource.getSource(x,y,z);
                        //Log.i(TAG, soundSources[i-3].toString());
                        if(k<10000){
                            ODAS_Output[k][i-3] = soundSources[i-3].getAzimuth();
                        }else {
                            //Log.i("FINNISH", "Finished ODAS" + (i-3));
                        }
                    }
                    k++;
                    sourceVector.addNewSource(soundSources, offset);
                }
            }
        }
    }
}
