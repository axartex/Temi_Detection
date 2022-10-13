package com.example.temi_app_marco;

import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.robotemi.sdk.Robot;
import com.robotemi.sdk.listeners.OnRobotReadyListener;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.List;

public class MainActivity extends AudioRecordActivity implements View.OnClickListener, OnRobotReadyListener{

    private static final String TAG = "Activity_temi";
    TextView textView_Ip, textView_noSubjects;
    Button create_subject;
    Button delete_all_subject;
    Button print_results;
    Button ODAS_ON;
    Button ODAS_OFF;
    ListView subjectListView;
    private SubjectListAdapter adapter;
    private Subject[] subjArray;
    private Robot robot;
    private static SubjectList subjectList;
    private static Subject subject;
    public static ODASConnection connection;
    public static RaspConnection raspConnection;

    //constant values
    public static String SERVER_IP = "";
    private final int PORT = 6000;
    private SoundSourceVector sourceVector;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Log.i(TAG, "--onCreate--");

        robot = Robot.getInstance();

        subjectList = SubjectList.get();

        textView_Ip = (TextView) findViewById(R.id.textView_IP);
        textView_noSubjects = (TextView) findViewById(R.id.noUsers);

        ImageView imageView_ist = (ImageView) findViewById(R.id.ISTImage);

        create_subject = (Button) findViewById(R.id.create_subject_button);
        create_subject.setOnClickListener(this);

        delete_all_subject = (Button) findViewById(R.id.delete_all_subjects_button);
        delete_all_subject.setOnClickListener(this);

        print_results = (Button) findViewById(R.id.printResults);
        print_results.setOnClickListener(this);

        ODAS_ON = (Button) findViewById(R.id.connectON);
        ODAS_ON.setOnClickListener(this);

        ODAS_OFF = (Button) findViewById(R.id.connectOFF);
        ODAS_OFF.setOnClickListener(this);

        List<Subject> tempSubjList = subjectList.getItems();
        subjArray = new Subject[tempSubjList.size()];
        subjArray = tempSubjList.toArray(subjArray);
        adapter = new SubjectListAdapter(MainActivity.this, R.layout.adapter_item, tempSubjList);

        subjectListView = (ListView) findViewById(R.id.subjectListView);
        subjectListView.setAdapter(adapter);

        updateListView();

        try {
            SERVER_IP = getIpAddress();
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }
        String textViewText = "IP: " + SERVER_IP + "\nPort: " + String.valueOf(PORT);
        textView_Ip.setText(textViewText);
        connection = ODASConnection.get();
        sourceVector = SoundSourceVector.get();
        raspConnection = RaspConnection.get();
    }

    @Override
    protected void onStart() {
        super.onStart();
        Log.i(TAG, "--onStart--");
        robot.addOnRobotReadyListener(this);
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
        updateListView();
        Log.i(TAG, "--onResume--");
    }

    @Override
    protected void onStop() {
        super.onStop();
        Log.i(TAG, "--onStop--");
        robot.removeOnRobotReadyListener(this);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.i(TAG, "--onDestroy--");
    }

    @Override
    public void onRobotReady(boolean isReady) {
        if (isReady){
            try{
                final ActivityInfo activityInfo = getPackageManager().getActivityInfo(getComponentName(),
                        PackageManager.GET_META_DATA);
                robot.onStart(activityInfo);
            } catch (PackageManager.NameNotFoundException e){
                throw new RuntimeException(e);
            }
        }
    }

    public void updateSubjectOnClickHandler(View view) {
        subject = (Subject) view.getTag();
        Intent intent_subject_activity = new Intent(MainActivity.this, EditSubjectActivity.class);
        int currSubjectId = subject.getId();
        intent_subject_activity.putExtra("SubjectId", String.valueOf(currSubjectId));
        startActivity(intent_subject_activity);
    }

    public void locateSubjectOnClickHandler(View view) {
        subject = (Subject) view.getTag();
        Intent intent_locate_activity = new Intent(MainActivity.this, LocateUserActivity.class);
        int currSubjectId = subject.getId();
        intent_locate_activity.putExtra("SubjectId", String.valueOf(currSubjectId));
        startActivity(intent_locate_activity);
    }

    public void deleteSubjectOnClickHandler(View view) {
        final Subject subjectToRemove = (Subject) view.getTag();
        if(!subjectList.isEmpty()){
            subjectList.removeSubject(subjectToRemove);
        }
        adapter.remove(subjectToRemove);
        String toastText = "Removed " + subjectToRemove.getName() + "!";
        Toast.makeText(this, toastText, Toast.LENGTH_SHORT).show();
        updateListView();
    }

    private void updateListView() {
        if(subjectList.isEmpty()){
            if(adapter.getCount() == 0) {
                textView_noSubjects.setVisibility(View.VISIBLE);
                delete_all_subject.setEnabled(false);
            } else {
                textView_noSubjects.setVisibility(View.INVISIBLE);
            }
        } else {
            textView_noSubjects.setVisibility(View.INVISIBLE);
            delete_all_subject.setEnabled(true);
        }
    }

    private String getIpAddress() throws UnknownHostException {
        WifiManager wifiManager = (WifiManager) getApplicationContext().getSystemService(WIFI_SERVICE);
        assert wifiManager != null: "Problem with wifiManager";
        WifiInfo wifiInfo = wifiManager.getConnectionInfo();
        int ipInt = wifiInfo.getIpAddress();
        return InetAddress.getByAddress(ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN).putInt(ipInt).array()).getHostAddress();
    }

    @Override
    public void onClick(View view) {
        Intent intent_subject_activity = new Intent(MainActivity.this, EditSubjectActivity.class);
        switch (view.getId())
        {
            case R.id.create_subject_button:
                //intent.putExtra("Mode", 0);
                //intent.addFlags(*FLAGS*)
                startActivity(intent_subject_activity);
                break;
            case R.id.delete_all_subjects_button:
                subjectList.removeAllSubjects();
                adapter.clear();
                updateListView();
                break;
            case R.id.printResults:
                try {
                    tools.writeFileODAS(connection.ODAS_Output,1,MainActivity.this);
                    tools.writeFileFilter(sourceVector.SSV_Output,2,MainActivity.this);
                    Toast.makeText(MainActivity.this, "OUTPUT DONE", Toast.LENGTH_SHORT).show();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                break;
            case R.id.connectON:
                raspConnection.startODAS();
                break;
            case R.id.connectOFF:
                raspConnection.stopODAS();
                break;
            default:
                throw new IllegalStateException("Unexpected value: " + view.getId());
        }
    }
}