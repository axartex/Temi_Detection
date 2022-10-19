package com.example.temi_app_marco;

import android.content.Context;
import android.widget.Toast;

import androidx.annotation.CheckResult;

import com.robotemi.sdk.Robot;
import com.robotemi.sdk.permission.Permission;

import java.util.Collections;
import java.util.List;

public class TemiTools {

    //constant values
    private static final int REQUEST_CODE_FACE_START = 1;
    private static final int REQUEST_CODE_FACE_STOP = 2;
    private static final int REQUEST_CODE_DETECTION_WITH_DISTANCE = 3;
    private static final int REQUEST_CODE_TRACK_USER = 4;

    @CheckResult
    public static boolean requestPermissionIfNeeded(Permission permission, int requestCode, Robot robot){
        if(robot.checkSelfPermission(permission) == Permission.GRANTED){
            return false;
        }
        List<Permission> permissions = Collections.singletonList(permission);
        robot.requestPermissions(permissions, requestCode);
        return true;
    }

    public static void startFaceRecognition(Robot robot, Context context) {
        if (requestPermissionIfNeeded(Permission.FACE_RECOGNITION, REQUEST_CODE_FACE_START, robot)){
            return;
        }
        robot.startFaceRecognition();
        Toast.makeText(context, "Face Recognition Started!", Toast.LENGTH_SHORT).show();
    }

    public static void stopFaceRecognition(Robot robot, Context context){
        if (requestPermissionIfNeeded(Permission.FACE_RECOGNITION, REQUEST_CODE_FACE_STOP, robot)){
            return;
        }
        robot.stopFaceRecognition();
        Toast.makeText(context, "Face Recognition Stopped!", Toast.LENGTH_SHORT).show();
    }

    public static void toggleKioskMode(boolean state, Robot robot, Context context){
        if(state){
            if(robot.isKioskModeOn()){
                Toast.makeText(context, "Already on Kiosk Mode!", Toast.LENGTH_SHORT).show();
                return;
            }
            robot.setKioskModeOn(true);
            Toast.makeText(context, "Kiosk Mode activated!", Toast.LENGTH_SHORT).show();
        }else {
            if(!robot.isKioskModeOn()){
                Toast.makeText(context, "Kiosk Mode already disabled!", Toast.LENGTH_SHORT).show();
                return;
            }
            robot.setKioskModeOn(false);
            Toast.makeText(context, "Kiosk Mode disabled!", Toast.LENGTH_SHORT).show();
        }
    }

    public static void toggleDetectionModeWithDistance(Robot robot, float distance, boolean state, Context context){
        if(requestPermissionIfNeeded(Permission.SETTINGS, REQUEST_CODE_DETECTION_WITH_DISTANCE, robot)){
            return;
        }
        if(state){
            if(robot.isDetectionModeOn()){
                Toast.makeText(context, "Detection Mode already activated!", Toast.LENGTH_SHORT).show();
                return;
            }
            robot.setDetectionModeOn(true,distance);
            Toast.makeText(context, "Detection Mode activated!", Toast.LENGTH_SHORT).show();
        }else {
            if(!robot.isDetectionModeOn()){
                Toast.makeText(context, "Detection Mode already disabled!", Toast.LENGTH_SHORT).show();
                return;
            }
            robot.setDetectionModeOn(false);
            Toast.makeText(context, "Detection Mode disabled!", Toast.LENGTH_SHORT).show();
        }
    }

    public static void toggleUserTracking(Robot robot, boolean state, Context context){
        if(requestPermissionIfNeeded(Permission.SETTINGS, REQUEST_CODE_TRACK_USER, robot)){
            return;
        }
        if(state){
            if(robot.isTrackUserOn()){
                Toast.makeText(context, "User Tracking already activated!", Toast.LENGTH_SHORT).show();
                return;
            }
            robot.setTrackUserOn(true);
            Toast.makeText(context, "User Tracking activated!", Toast.LENGTH_SHORT).show();
        }else {
            if(!robot.isTrackUserOn()){
                Toast.makeText(context, "User Tracking already disabled!", Toast.LENGTH_SHORT).show();
                return;
            }
            robot.setTrackUserOn(false);
            Toast.makeText(context, "User Tracking disabled!", Toast.LENGTH_SHORT).show();
        }
    }

    public static void rotateBy(Robot robot, int degrees, float speed){
        if (speed > 1){
            speed = 1;
        }else if(speed < 0) {
            throw new IllegalStateException("Unexpected value for speed: " + speed);
        }
        robot.turnBy(degrees,speed);
    }

    public static void tiltScreenTo(Robot robot, int degrees, float speed){
        if (speed > 1){
            speed = 1;
        }else if(speed < 0) {
            throw new IllegalStateException("Unexpected value for speed: " + speed);
        }
        if (degrees < -25){
            degrees = -25;
        } else if(degrees > 55){
            degrees = 55;
        }
        robot.tiltAngle(degrees, speed);
    }

    public static void moveForward(Robot robot, boolean smart){
        long t = System.currentTimeMillis();
        long end = t + 500;
        while (System.currentTimeMillis() < end){
            robot.skidJoy(0.5f,0, smart);
        }
    }

    public static void moveBackward(Robot robot){
        long t = System.currentTimeMillis();
        long end = t + 500;
        while (System.currentTimeMillis() < end){
            robot.skidJoy(-1,0, true);
        }
    }

    public static void moveLeft(Robot robot){
        long t = System.currentTimeMillis();
        long end = t + 500;
        while (System.currentTimeMillis() < end){
            robot.skidJoy(0,1, true);
        }
    }

    public static void moveRight(Robot robot){
        long t = System.currentTimeMillis();
        long end = t + 500;
        while (System.currentTimeMillis() < end){
            robot.skidJoy(0,-1, true);
        }
    }

    public static void stopMovement(Robot robot){
        robot.stopMovement();
    }

}
