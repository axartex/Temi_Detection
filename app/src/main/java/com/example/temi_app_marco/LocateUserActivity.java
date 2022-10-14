package com.example.temi_app_marco;

import android.content.Intent;
import android.graphics.BitmapFactory;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.Switch;
import android.widget.TextView;

import androidx.annotation.NonNull;

import com.robotemi.sdk.Robot;
import com.robotemi.sdk.constants.ContentType;
import com.robotemi.sdk.face.ContactModel;
import com.robotemi.sdk.face.OnFaceRecognizedListener;
import com.robotemi.sdk.listeners.OnDetectionDataChangedListener;
import com.robotemi.sdk.listeners.OnMovementStatusChangedListener;
import com.robotemi.sdk.model.DetectionData;
import com.robotemi.sdk.permission.OnRequestPermissionResultListener;
import com.robotemi.sdk.permission.Permission;

import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import AlizeSpkRec.AlizeException;
import AlizeSpkRec.SimpleSpkDetSystem;

public class LocateUserActivity extends AudioRecordActivity implements
        OnRequestPermissionResultListener, View.OnClickListener, OnFaceRecognizedListener,
        OnDetectionDataChangedListener, OnMovementStatusChangedListener, CompoundButton.OnCheckedChangeListener {

    private static final String TAG = "Activity_Locate_User";
    private static final String FaceTAG = "onFaceRecognized";
    private static final String VoiceTAG = "OnRecordStopped";
    private static final String ConnectionTAG = "OnConnection";
    private static final String LocalizationTAG = "OnLocalized";
    private static final String DetectionTAG = "OnDetected";
    private static final String RecognitionTAG = "OnRecognized";
    TextView subjectNameTV;
    ImageView subjectFaceIV;
    Button startButton;
    Button startLocalizationButton;
    Button startRecognitionButton;
    Button stopButton;
    TextView IpTV;
    TextView resultTV;
    Switch recognitionSwitch;
    Button returnButton;
    TextView connectionTV;
    TextView faceRecTV;
    TextView speakerRecTV;
    TextView logTV;
    Button clearButton;
    private int curId;
    private SubjectList subjectList;
    private Robot robot;
    private Subject subject;
    private Subject facePrediction;
    private Subject voicePrediction;
    private boolean faceMatch;
    private boolean voiceMatch;
    private double faceScore;
    private double voiceScore;
    private boolean match;
    private DetectionData detectedUser;
    public static ODASConnection connection;
    private static SoundSourceVector soundSources;
    public static RaspConnection raspConnection;
    private final ExecutorService executorService = Executors.newSingleThreadExecutor();
    private double[] faceRecognized; //face recognition probabilities vector
    private int totalFRec; //total number of face recognition
    private double offset;
    private boolean inLocalization;
    private boolean recognitionMode; //false = parallel and true = serial
    private boolean inRecognition;
    private boolean noStopping;
    private boolean conStatus;
    private boolean inDetection = false;
    private boolean inMovement;
    private boolean aborted;
    protected Thread locateThread = null;
    //constant values
    private static final int REQUEST_CODE_FACE_START = 1;
    private static final int REQUEST_CODE_FACE_STOP = 2;
    //private final int REQUEST_MICROPHONE = 23;
    public static String SERVER_IP = "";
    private final int PORT = 6000;
    private final boolean MOVE_ALLOWED = true;
    private static final double FACE_THRESHOLD = 0.7;
    private static final double VOICE_THRESHOLD = 50;
    private static final double BETA = 0.3;
    private boolean isTurning;
    private boolean firstDetection;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_locate_user);
        Log.i(TAG, "--onCreate--");

        String currentId = null;
        Bundle extras = getIntent().getExtras();
        if(extras != null) {
            currentId = extras.getString("SubjectId");
        }

        subjectList = SubjectList.get();
        soundSources = SoundSourceVector.get();

        offset = 0;
        match = false;
        isTurning = false;

        robot = Robot.getInstance();
        robot.addOnRequestPermissionResultListener(this);

        if( currentId == null){
            throw new IllegalStateException("Subject Id not defined!");
        }
        curId = Integer.parseInt(currentId);
        subject = subjectList.searchId(curId);

        faceRecognized = new double[subjectList.getSize()+1];
        totalFRec = 0;
        faceMatch = false;
        voiceMatch = false;
        inMovement = false;
        aborted = false;

        recognitionMode = false;
        noStopping = true;
        firstDetection = true;

        subjectNameTV = (TextView) findViewById(R.id.nameTextView);
        subjectFaceIV = (ImageView) findViewById(R.id.faceImageView);
        startButton = (Button) findViewById(R.id.start_detection_button);
        startButton.setOnClickListener(this);
        startLocalizationButton = (Button) findViewById(R.id.test_localization_button);
        startLocalizationButton.setOnClickListener(this);
        startRecognitionButton = (Button) findViewById(R.id.test_recognition_button);
        startRecognitionButton.setOnClickListener(this);
        stopButton = (Button) findViewById(R.id.stop_detection_button);
        stopButton.setOnClickListener(this);
        IpTV = (TextView) findViewById(R.id.ipTextView);
        resultTV = (TextView) findViewById(R.id.finalResultTV);
        recognitionSwitch = (Switch) findViewById(R.id.recognition_switch);
        recognitionSwitch.setOnCheckedChangeListener(this);
        returnButton = (Button) findViewById(R.id.returnLocButton);
        returnButton.setOnClickListener(this);

        connectionTV = (TextView) findViewById(R.id.connectionStatusTV);
        faceRecTV = (TextView) findViewById(R.id.faceStatusTV);
        speakerRecTV = (TextView) findViewById(R.id.speakerStatusTV);
        logTV = (TextView) findViewById(R.id.logTextView);
        clearButton = (Button) findViewById(R.id.clearLogButton);
        clearButton.setOnClickListener(this);

        try {
            SERVER_IP = getIpAddress();
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }
        String IpTextViewText = "IP: " + SERVER_IP + "\nPort: " + String.valueOf(PORT);
        IpTV.setText(IpTextViewText);

        subjectNameTV.setText(subject.getName());
        if(subject.getImageKey() != null){
            showFaceRecognitionImage(subject.getImageKey());
        }

        connection = ODASConnection.get();
        raspConnection = RaspConnection.get();
        conStatus = connection.getStatus();
        if(conStatus){
            String textViewText = getResources().getString(R.string.connectionState) +
                    getResources().getString(R.string.connected_state);
            connectionTV.setText(textViewText);
        }

    }

    @Override
    protected void onStart() {
        super.onStart();
        Log.i(TAG, "--onStart--");
        robot.addOnDetectionDataChangedListener(this);
        robot.addOnFaceRecognizedListener(this);
        robot.addOnMovementStatusChangedListener(this);
    }

    @Override
    protected void onRestart() {
        super.onRestart();
        Log.i(TAG, "--onRestart--");
    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.i(TAG, "--onPause--");
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.i(TAG, "--onResume--");
    }

    @Override
    protected void onStop() {
        super.onStop();
        Log.i(TAG, "--onStop--");
        robot.removeOnDetectionDataChangedListener(this);
        robot.removeOnFaceRecognizedListener(this);
        robot.removeOnMovementStatusChangedListener(this);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.i(TAG, "--onDestroy--");
        robot.removeOnRequestPermissionResultListener(this);
        if(!executorService.isShutdown()){
            executorService.shutdownNow();
        }
    }

    @Override
    public void onFaceRecognized(@NotNull List<ContactModel> contactModelList) {
        Subject tempSubject = null;
        if(contactModelList.isEmpty()){
            Log.i(TAG, "onFaceRecognized: User Left");
            printLog(FaceTAG, "User Left");
            TemiTools.startFaceRecognition(robot, LocateUserActivity.this);
            return;
        }
        String msg_text;
        for( ContactModel contactModel : contactModelList){
            if (tools.isEmptyOrBlank(contactModel.getUserId())){
                msg_text = getResources().getString(R.string.unknown_face);
                Log.i(TAG, "onFaceRecognized: Unknown Face");
                printLog(FaceTAG, msg_text);
                updateFaceRecognitionProb(subjectList.getSize());
            } else {
                Log.i(TAG, "onFaceRecognized:" + contactModel.getFirstName() + " " +
                        contactModel.getLastName());
                tempSubject = subjectList.searchUserId(contactModel.getUserId());
                msg_text = getResources().getString(R.string.face_recognized);
                if(tempSubject == null){
                    msg_text = msg_text + " but not in system";
                    updateFaceRecognitionProb(subjectList.getSize());
                } else {
                    msg_text = msg_text + " " + tempSubject.getName();
                    updateFaceRecognitionProb(tempSubject.getId());
                }
                printLog(FaceTAG, msg_text);
            }
        }
        int indexPrediction = tools.argMax(faceRecognized);
        double localScore = faceRecognized[indexPrediction];
        facePrediction = subjectList.searchId(indexPrediction);
        faceScore = faceRecognized[curId];
        if (facePrediction == null){
            faceRecTV.setText("Unknown Subject");
            faceMatch = false;
        }else {
            if(subject.equals(facePrediction) && faceScore > FACE_THRESHOLD){
                faceMatch = true;
            }else {
                faceMatch = false;
            }
            String textViewText = getResources().getString(R.string.faceState);
            textViewText = textViewText + "\n" + "Id: " + subject.getId() + " - " + subject.getName()
                    + "\n" + String.valueOf(faceMatch) + "\n" + "Score: " + String.valueOf(localScore);
            faceRecTV.setText(textViewText);
        }
        TemiTools.startFaceRecognition(robot, LocateUserActivity.this);
    }

    @Override
    public void onDetectionDataChanged(@NonNull DetectionData detectionData) {
        if( !detectionData.isDetected())
        {
            return;
        }
        double userAngle = (detectionData.getAngle()*180)/Math.PI;
        String message = "onDetectionData: User Detected" + String.valueOf(detectionData.getDistance()) +
                "\nAngle: " + String.valueOf(userAngle);
        Log.i(TAG, message);
        printLog("", message);
        detectedUser = detectionData;
        if(inLocalization){
            if(!firstDetection) {
                firstDetection = false;
                TemiTools.stopMovement(robot);
                isTurning = true;
                TemiTools.rotateBy(robot, (int) Math.round(userAngle), 1);
                message = "onDetectionData: Turn By: " + String.valueOf(userAngle);
                Log.i(TAG, message);
                printLog("", message);
                Handler handler = new Handler();
                handler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        inMovement = true;
                    }
                },5000); //Delay 5s
            }
            if(detectedUser.getDistance() > 1 && !aborted){
                return;
            }
            inLocalization = false;
            inMovement = false;
            TemiTools.stopMovement(robot);
            try {
                locateThread.join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            TemiTools.toggleDetectionModeWithDistance(robot, 2.0f, false,LocateUserActivity.this);
            printLog(LocalizationTAG, "User Detected");
            locateThread = null;

            if(inDetection){
                recognition();
            }
        }
    }

    @Override
    protected void onRecordStopped() {
        String result = "";
        String speakerName;
        if(recordError){
            printLog(VoiceTAG, getResources().getString(R.string.record_error));
            voiceMatch = false;
            voiceScore = 0;
            result = getResources().getString(R.string.record_error);
            return;
        }
        try {
            SimpleSpkDetSystem.SpkRecResult verificationResult = speakerRecognitionSystem.verifySpeakerModel(curId);
            voiceScore = verificationResult.score;
            if(verificationResult.match){
                result = "Match!\n" + "Score:\n" + verificationResult.score;
                voicePrediction = subject;
                voiceMatch = true;
                //voiceScore = verificationResult.score;
            } else {
                voiceMatch = false;
                SimpleSpkDetSystem.SpkRecResult identificationResult = speakerRecognitionSystem.identifySpeaker();
                if(identificationResult.match){
                    voicePrediction = subjectList.searchId(Integer.parseInt(identificationResult.speakerId));
                    speakerName = voicePrediction.getName();
                    result = "No Match!\nOther user match:" + speakerName + "\n" + "Score:\n" + identificationResult.score;
                    //voiceScore = identificationResult.score;
                } else {
                    result = "No match!\n Score:\n" + identificationResult.score;
                    //voiceScore = 0;
                }
            }
        } catch (AlizeException e) {
            e.printStackTrace();
        }
        Log.i(TAG, "onRecordStopped:" + result);
        printLog(VoiceTAG, result);
        result = getResources().getString(R.string.speakerState) + result;
        speakerRecTV.setText(result);

        try {
            speakerRecognitionSystem.resetAudio();
            speakerRecognitionSystem.resetFeatures();
        } catch (AlizeException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onRequestPermissionResult(@NonNull Permission permission, int grantResult, int requestCode) {
        if(grantResult == Permission.DENIED){ return;}
        if(permission == Permission.FACE_RECOGNITION){
            if( requestCode == REQUEST_CODE_FACE_START){
                TemiTools.startFaceRecognition(robot, LocateUserActivity.this);
            }else if (requestCode == REQUEST_CODE_FACE_STOP){
                TemiTools.stopFaceRecognition(robot, LocateUserActivity.this);
            }
        }
    }

    @Override
    public void onClick(View v) {
        Intent intent = new Intent(LocateUserActivity.this, MainActivity.class);
        Handler handler = new Handler();
        switch (v.getId())
        {
            case R.id.start_detection_button:
                inRecognition = true;
                inLocalization = true;
                start_detection();
                break;
            case R.id.test_localization_button:
                inLocalization = true;
                printLog(DetectionTAG, getResources().getString(R.string.locateState));
                resultTV.setText(getResources().getString(R.string.locateState));
                handler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        localization();
                    }
                },5000); //Delay 5s
                break;
            case R.id.test_recognition_button:
                inRecognition = true;
                printLog(DetectionTAG, getResources().getString(R.string.reconState));
                resultTV.setText(getResources().getString(R.string.reconState));
                TemiTools.toggleDetectionModeWithDistance(robot, 2.0f, true,LocateUserActivity.this);
                recognition();
                break;
            case R.id.stop_detection_button:
                finalResult();
                break;
            case R.id.returnLocButton:
                startActivity(intent);
                break;
            case R.id.clearLogButton:
                logTV.setText("");
                break;
            default:
                throw new IllegalStateException("Unexpected value: " + v.getId());
        }
    }

    private void recognition(){
        if(!inRecognition){
            return;
        }
        if(!robot.isSelectedKioskApp()){
            robot.requestToBeKioskApp();
        }
        String faceText = getResources().getString(R.string.faceState);
        faceRecTV.setText(faceText);
        String speakerText = getResources().getString(R.string.speakerState);
        speakerRecTV.setText(speakerText);
        TemiTools.startFaceRecognition(robot, LocateUserActivity.this);
        start_record();
        printLog(RecognitionTAG, getResources().getString(R.string.record_start));
        Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                stop_record();
                printLog(RecognitionTAG, getResources().getString(R.string.record_complete));
                if(!recognitionMode){
                    recognitionFusion();
                }else {
                    recognition_serial();
                }
                TemiTools.stopFaceRecognition(robot, LocateUserActivity.this);
                resetFaceRecognitionProb();
                if(inDetection){
                    if(match){
                        finalResult();
                    }
                    else{
                        localization();
                    }
                }
            }
        },10000); //Delay 10s
    }

    //to use serial recognition the voice threshold should be lower
    private void recognition_serial(){
        if(!voiceMatch){
            match = false;
            return;
        }
        match = faceMatch;
    }

    private void recognitionFusion(){
        double localConfidence;
        double faceDistance;
        double voiceDistance;
        if (recordError){
            match = faceMatch;
            String logText = (match) ? "Match" : "No Match";
            printLog(RecognitionTAG, logText);
        }
        else if(voiceMatch && faceMatch){
            match = true;
            printLog(RecognitionTAG, "Match");
        } else if(!voiceMatch && !faceMatch) {
            match = false;
            printLog(RecognitionTAG, "No Match");
        } else{
            faceDistance = (faceScore - FACE_THRESHOLD)/FACE_THRESHOLD;
            voiceDistance = (voiceScore - VOICE_THRESHOLD)/VOICE_THRESHOLD;
            localConfidence = voiceDistance * BETA + faceDistance * (1-BETA);
            if(localConfidence > 0){
                match = true;
                printLog(RecognitionTAG, "Match");
            }else{
                match = false;
                printLog(RecognitionTAG, "No Match");
            }
            printLog(RecognitionTAG, "FaceDist: " + String.valueOf(faceDistance) +
                    "\n VoiceDist: " + String.valueOf(voiceDistance) +
                    "\n FinalConfidence: " + String.valueOf(localConfidence));
        }
    }

    private void localization() {
        if(!inLocalization){
            return;
        }
        if(!raspConnection.returnState()){
            Log.i(TAG, "Connection OFF!");
            String textViewText = getResources().getString(R.string.connectionState) +
                    getResources().getString(R.string.holding_state);
            connectionTV.setText(textViewText);
            printLog(ConnectionTAG, "Connection OFF!");
            return;
        }
        raspConnection.startODAS();
        String textViewText = getResources().getString(R.string.connectionState) + getResources().getString(R.string.connected_state);
        connectionTV.setText(textViewText);
        if(detectedUser != null){
            TemiTools.rotateBy(robot,180,1);
            detectedUser = null;
        }
        TemiTools.toggleDetectionModeWithDistance(robot, 2.0f, true,LocateUserActivity.this);
        printLog(LocalizationTAG, "Locating");
        locateThread = new Thread(new LocateThread());
        locateThread.start();
    }

    @Override
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        recognitionMode = isChecked;
        double localThreshold = isChecked ? 30 : 50;
        setThreshold(localThreshold);
    }

    class LocateThread implements Runnable{
        SoundSource mostProbable;
        double azimuth;
        double turnByAngle;
        @Override
        public void run() {
            while(inLocalization){
                if(!inMovement) {
                    conStatus = connection.getStatus();
                    if (isTurning || !conStatus) {
                        //Log.i(LocalizationTAG, "isTurning! Wait Please");
                        continue;
                    }
                    mostProbable = soundSources.getMostProbableSource();
                    azimuth = mostProbable.getAzimuth();
                    turnByAngle = azimuth - offset;
                    turnByAngle = tools.wrapTo180(turnByAngle);
                /*Log.i(LocalizationTAG, "Rotate by: " + String.valueOf(turnByAngle) + "\nRead: "
                        + String.valueOf(azimuth));*/
                    if (Math.abs(turnByAngle) > 5) {
                        //Log.i(LocalizationTAG, "Rotate by: " + String.valueOf((int) Math.round(turnByAngle)));
                        offset = offset + (int) Math.round(turnByAngle);
                        connection.startTurning(offset);
                        raspConnection.stopODAS();
                        isTurning = true;
                        TemiTools.rotateBy(robot, (int) Math.round(turnByAngle), 1);
                    } else if (MOVE_ALLOWED) {
                        TemiTools.moveForward(robot);
                        isTurning = true;
                    }
                }else if(MOVE_ALLOWED){
                    TemiTools.moveForward(robot);
                }
            }
            TemiTools.tiltScreenTo(robot, (int) Math.round(mostProbable.getElevation()),1); //TODO: Test this
            soundSources.resetConfidence(mostProbable,5);//TODO: ADJUST THIS VALUE
        }
    }

    private void start_detection() {
        match = false;
        noStopping = true;
        inDetection = true;
        resultTV.setText(getResources().getString(R.string.runningState));
        Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                localization();
            }
        },5000); //Delay 5s
    }

    private void finalResult(){
        stop_detection();
        if(match){
            resultTV.setText(getResources().getString(R.string.finishedState));
            printLog(DetectionTAG, getResources().getString(R.string.finishedState));
        }else {
            printLog(DetectionTAG, getResources().getString(R.string.droppedState));
            resultTV.setText(getResources().getString(R.string.droppedState));
        }
    }

    private void stop_detection() {
        inLocalization = false;
        inRecognition = false;
        inMovement = false;
        isTurning = false;
        connection.stopTurning();
        TemiTools.stopMovement(robot);
        if(locateThread != null) {
            try {
                locateThread.join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        locateThread = null;
        TemiTools.stopFaceRecognition(robot, LocateUserActivity.this);
        resetFaceRecognitionProb();
        TemiTools.toggleDetectionModeWithDistance(robot, 2.0f, false,LocateUserActivity.this);
        conStatus = connection.getStatus();
        if(conStatus) {
            raspConnection.stopODAS();
        }
        soundSources.resetConfidence(new SoundSource(0,0),180);
        offset = 0;
        detectedUser = null;
        if(isRecording){
            stop_record();
        }
        printLog(DetectionTAG, getResources().getString(R.string.droppedState));
        resultTV.setText(getResources().getString(R.string.droppedState));
        noStopping = false;
    }

    private void printLog( String tag, String msg){
        String logMessage = tag + ": " + msg + "\n";
        logTV.append(logMessage);
    }

    private void showFaceRecognitionImage(String imageKey) {
        executorService.execute((Runnable) new Runnable(){
            @Override
            public void run() {
                InputStream inputStream = robot.getInputStreamByMediaKey(ContentType.FACE_RECOGNITION_IMAGE,
                        imageKey);
                assert inputStream != null;
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        subjectFaceIV.setVisibility(View.VISIBLE);
                        subjectFaceIV.setImageBitmap(BitmapFactory.decodeStream(inputStream));
                        try{
                            inputStream.close();
                        } catch (IOException e){
                            e.printStackTrace();
                        }
                    }
                });
            }
        });
    }

    private void updateFaceRecognitionProb(int input){
        int localTotalFRec = totalFRec + 1;
        for(int i = 0; i < faceRecognized.length; i++){
            faceRecognized[i] *= totalFRec;
            if(i == input){
               faceRecognized[i]++;
            }
            faceRecognized[i] /= localTotalFRec;
        }
        totalFRec = localTotalFRec;
    }

    private void resetFaceRecognitionProb(){
        totalFRec = 0;
        Arrays.fill(faceRecognized, 0);
    }

    private String getIpAddress() throws UnknownHostException {
        WifiManager wifiManager = (WifiManager) getApplicationContext().getSystemService(WIFI_SERVICE);
        assert wifiManager != null: "Problem with wifiManager";
        WifiInfo wifiInfo = wifiManager.getConnectionInfo();
        int ipInt = wifiInfo.getIpAddress();
        return InetAddress.getByAddress(ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN).putInt(ipInt).array()).getHostAddress();
    }

    @Override
    public void onMovementStatusChanged(@NonNull String type, @NonNull String status) {
        if(type.equals(TYPE_TURN_BY)){
            Log.i("MOVEMENT", "TurnBy"+status);
            switch (status){
                case STATUS_COMPLETE:
                case STATUS_ABORT:
                    connection.stopTurning();
                    raspConnection.startODAS();
                    isTurning = false;
                    break;
                default:
                    break;
            }
        }else if(type.equals(TYPE_SKID_JOY)){
            switch (status) {
                case STATUS_COMPLETE:
                case STATUS_ABORT:
                    isTurning = false;
                    break;
                default:
                    break;
            }
            /*Log.i("MOVEMENT", "SkidJoy"+status);
            if (STATUS_ABORT.equals(status)) {
                if(inMovement){
                    aborted = true;
                    return;
                }
                inLocalization = false;
                try {
                    locateThread.join();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                TemiTools.toggleDetectionModeWithDistance(robot, 2.0f, false,LocateUserActivity.this);
                printLog(LocalizationTAG, "Localization Aborted!");
                locateThread = null;
                if(inDetection){
                    localization();
                }
            }*/
        }
    }
}