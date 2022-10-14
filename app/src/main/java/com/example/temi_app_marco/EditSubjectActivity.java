package com.example.temi_app_marco;

import android.content.Intent;
import android.graphics.BitmapFactory;
import android.media.AudioRecord;
import android.os.Bundle;
import android.os.Handler;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.robotemi.sdk.Robot;
import com.robotemi.sdk.constants.ContentType;
import com.robotemi.sdk.face.ContactModel;
import com.robotemi.sdk.face.OnFaceRecognizedListener;
import com.robotemi.sdk.permission.OnRequestPermissionResultListener;
import com.robotemi.sdk.permission.Permission;

import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import AlizeSpkRec.AlizeException;
import AlizeSpkRec.IdAlreadyExistsException;

public class EditSubjectActivity extends AudioRecordActivity implements OnFaceRecognizedListener,
        AdapterView.OnItemSelectedListener, View.OnClickListener, OnRequestPermissionResultListener {

    private static final String TAG = "Activity_Edit_Subject";
    Spinner gender_spinner;
    EditText name_text;
    Button startFaceButton;
    Button submitSubjectButton;
    Button returnButton;
    Button startRecordButton;
    Button stopRecordButton;
    ImageView imageViewFace;
    TextView timeText;
    private String currentId;
    private Subject subject = null;
    private SubjectList subjectList;
    private String subjectName;
    private String userId = null;
    private String subjectGender;
    private int subjectId;
    private String subjectImageKey = null;
    private boolean genderSelected = false;
    private boolean alreadyExist = false;
    private Robot robot;

    private final ExecutorService executorService = Executors.newSingleThreadExecutor();

    //constant values
    private static final int REQUEST_CODE_FACE_START = 1;
    private static final int REQUEST_CODE_FACE_STOP = 2;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.edit_subject);
        Log.i(TAG, "--onCreate--");

        Bundle extras = getIntent().getExtras();
        if(extras != null) {
            currentId = extras.getString("SubjectId");
        }

        subjectList = SubjectList.get();
        subjectName = "";

        robot = Robot.getInstance();
        robot.addOnRequestPermissionResultListener(this);

        if( currentId != null){
            int curId = Integer.parseInt(currentId);
            subject = subjectList.searchId(curId);
        }

        gender_spinner = (Spinner) findViewById(R.id.spinner);
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(this,
                android.R.layout.simple_list_item_1, getResources().getStringArray(R.array.gender_array));
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        gender_spinner.setAdapter(adapter);
        gender_spinner.setOnItemSelectedListener(this);

        name_text = (EditText) findViewById(R.id.speaker_fname);
        name_text.addTextChangedListener(editSubjectNameListener);

        startFaceButton = (Button) findViewById(R.id.startFaceButton);
        startFaceButton.setOnClickListener(this);
        submitSubjectButton = (Button) findViewById(R.id.submitSubjectButton);
        submitSubjectButton.setOnClickListener(this);
        returnButton = (Button) findViewById(R.id.returnButton);
        returnButton.setOnClickListener(this);
        startRecordButton = (Button) findViewById(R.id.startRecordButton);
        startRecordButton.setOnClickListener(this);
        stopRecordButton = (Button) findViewById(R.id.stopRecordButton);
        stopRecordButton.setOnClickListener(this);

        imageViewFace = (ImageView) findViewById(R.id.faceImage);

        timeText = (TextView) findViewById(R.id.timeText);

        if ( subject != null){
            name_text.setText(subject.getName());
        }

    }

    @Override
    protected void onStart() {
        super.onStart();
        Log.i(TAG, "--onStart--");
        robot.addOnFaceRecognizedListener(this);
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
        robot.removeOnFaceRecognizedListener(this);
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

    private final TextWatcher editSubjectNameListener = new TextWatcher() {
        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) { }

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {
            subjectName = s.toString();
            if (!subjectList.isEmpty()){
                alreadyExist = subjectList.searchName(subjectName) != null;
                if(subject != null){
                    alreadyExist = alreadyExist || subject.getName().equals(subjectName); //TODO: TEST THIS
                }
                if(alreadyExist){
                    name_text.setError(getResources().getString(R.string.subject_exist));
                    submitSubjectButton.setVisibility(View.GONE);
                    returnButton.setVisibility(View.VISIBLE);
                }
            }
        }

        @Override
        public void afterTextChanged(Editable s) {
            if(!subjectName.isEmpty()){
                if(genderSelected && !alreadyExist){
                    returnButton.setVisibility(View.GONE);
                    submitSubjectButton.setVisibility(View.VISIBLE);
                }
            }else {
                submitSubjectButton.setVisibility(View.GONE);
                returnButton.setVisibility(View.VISIBLE);
            }
        }
    };

    @Override
    public void onFaceRecognized(@NonNull List<ContactModel> contactModelList) {
        if(contactModelList.isEmpty()){
            Log.i(TAG, "onFaceRecognized: User Left");
            TemiTools.startFaceRecognition(robot, EditSubjectActivity.this);
            return;
        }
        String imageKey = null;
        for( ContactModel contactModel : contactModelList) {
            if (tools.isEmptyOrBlank(contactModel.getUserId())) {
                Log.i(TAG, "onFaceRecognized: Unknown Face");
                String toast_text = getResources().getString(R.string.unknown_face);
                Toast.makeText(this, toast_text, Toast.LENGTH_SHORT).show();
            } else {
                Log.i(TAG, "onFaceRecognized:" + contactModel.getFirstName() + " " +
                        contactModel.getLastName());
                String name = contactModel.getFirstName() + " " + contactModel.getLastName();
                name_text.setText(name);
                subjectName = name;
                subjectGender = contactModel.getGender();
                imageKey = contactModel.getImageKey();
                subjectImageKey = imageKey;
                userId = contactModel.getUserId();
                TemiTools.stopFaceRecognition(robot, EditSubjectActivity.this);
            }
        }
        if(!tools.isEmptyOrBlank(imageKey)){
            showFaceRecognitionImage(imageKey);
        }
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
                        imageViewFace.setVisibility(View.VISIBLE);
                        imageViewFace.setImageBitmap(BitmapFactory.decodeStream(inputStream));
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

    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
        genderSelected = true;
        subjectGender = parent.getItemAtPosition(position).toString();
    }

    @Override
    public void onNothingSelected(AdapterView<?> parent) {
        genderSelected = false;
    }

    @Override
    public void onClick(View v) {
        Intent intent = new Intent(EditSubjectActivity.this, MainActivity.class);
        switch (v.getId()){
            case R.id.startFaceButton:
                if(!robot.isSelectedKioskApp()){
                    robot.requestToBeKioskApp();
                }
                TemiTools.startFaceRecognition(robot, EditSubjectActivity.this);
                break;
            case R.id.submitSubjectButton:
                try {
                    editSubject();
                } catch (AlizeException | IdAlreadyExistsException e) {
                    e.printStackTrace();
                }
                startActivity(intent);
                break;
            case R.id.returnButton:
                startActivity(intent);
                break;
            case R.id.startRecordButton:
                timeText.setText(R.string.default_time);
                start_record();
                if (!isRecording){
                    break;
                }
                startRecordButton.setVisibility(View.GONE);
                stopRecordButton.setVisibility(View.VISIBLE);
                timerThread = new Thread(new TimerThread());
                timerThread.start();
                break;
            case R.id.stopRecordButton:
                stopRecordButton.setVisibility(View.GONE);
                startRecordButton.setVisibility(View.VISIBLE);
                stop_record();
                break;
            default:
                throw new IllegalStateException("Unexpected value: " + v.getId());
        }
    }

    private void editSubject() throws AlizeException, IdAlreadyExistsException {
        if (subject != null){
            subject.editName(subjectName);
            subject.editGender(subjectGender);
            subjectId = subject.getId();
            speakerRecognitionSystem.updateSpeakerModel(subjectId);
        }else{
            subject = subjectList.addSubject(subjectName, subjectGender);
            subjectId = subject.getId();
            speakerRecognitionSystem.createSpeakerModel(subjectId);
        }
        if (userId != null){
            subject.setUserId(userId);
            subject.setImageKey(subjectImageKey);
        }
        speakerRecognitionSystem.resetAudio();
        speakerRecognitionSystem.resetFeatures();
    }

    @Override
    public void onRequestPermissionResult(@NonNull Permission permission, int grantResult, int requestCode) {
        if(grantResult == Permission.DENIED){ return;}
        if(permission == Permission.FACE_RECOGNITION){
            if( requestCode == REQUEST_CODE_FACE_START){
                TemiTools.startFaceRecognition(robot, EditSubjectActivity.this);
            }else if (requestCode == REQUEST_CODE_FACE_STOP){
                TemiTools.stopFaceRecognition(robot, EditSubjectActivity.this);
            }
        }
    }

    @Override
    protected void onRecordStopped() {
        if(recordEmpty){
            timeText.setText(R.string.default_time);
            submitSubjectButton.setVisibility(View.GONE);
            returnButton.setVisibility(View.VISIBLE);
            Toast.makeText(EditSubjectActivity.this, "No Sound Detected!", Toast.LENGTH_SHORT).show();
        }
        else if (!recordExist){
            timeText.setText(R.string.default_time);
            submitSubjectButton.setVisibility(View.GONE);
            returnButton.setVisibility(View.VISIBLE);
        }
        else if(!subjectName.isEmpty() && genderSelected && !alreadyExist){
            returnButton.setVisibility(View.GONE);
            submitSubjectButton.setVisibility(View.VISIBLE);
        }
    }

    //Timer Manager Thread
    class TimerThread implements Runnable{
        private Handler handler = new Handler();
        @Override
        public void run() {
            while (audioRecord.getRecordingState() == AudioRecord.RECORDSTATE_RECORDING){
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        long currentTime = System.currentTimeMillis() - startTime;
                        String result = new SimpleDateFormat("mm:ss:SS").format(new Date(currentTime));
                        timeText.setText(result);
                    }
                });
                try {
                    Thread.sleep(10);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }

}