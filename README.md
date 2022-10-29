# Temi_Detection

Temi Detection is an Android application based on a Temi robot. It combines the Temi robot SDK, the Alize API and the ODAS framework to produce a detectio program able of localizing and recognizing a subject. The ODAS framework should be instaled in a Raspberry Pi, with an external microphone array.

## ODAS Instalation

In order to install ODAS in your Raspberry Pi you should follow the instructions in:<br />
[ODAS Project](https://github.com/introlab/odas/wiki "Named link title")


## Alizé Instalation

The Alizé system is already installed in this project. However, if any problem occurs, you should delete the Alizé folder and reinstall the API by following the instructions in:<br />
[Alizé Project](https://github.com/ALIZE-Speaker-Recognition/android-alize "Named link title")

## Before Install 

The extra folder contains the connect.py file that is responsible for connecting the Temi to Rasperri Pi. This folder should be removed before installing the program on Temi and the connect.py file should be placed on the same folder as the ODAS runnable in the Raspberry Pi. The connect.py program was developed assuming that the Raspberry Pi is using the ReSpeaker 4-mic microphone array. If another board, or another configuration file for ODAS is used, this should be changed.

## Installation on Temi

To install this program on Temi you should:
1. Install Android Studio on your computer
2. Install ADB in your Android Studio
3. Be sure your computer, Temi and Raspberry Pi are all on the same WiFi network
4. Connect your computer to Temi (Follow the instructions on Temi Developers Tools)
5. Open this project on Android studio
6. Run this project on Temi (be sure that the selected device is Temi, aka Rockchip)
7. Run connection.py in Raspberry Pi (this file should be in the same folder as the ODAS runnable)

## Important Parameters

A few parameters and boolean values should be changed accordingly in order to use this project.
* The threshold for each recognition process can be changed in the LocateUserActivity.java file. It should be noted that the VOICE_THRESHOLD is also defined in the SpeakerRecognition.java file and should be changed there as well.
* The BETA value defined in the LocateUserActivity.java file is used to fuse the face and speaker recognition processes according to: <br />
finalScore = voiceScore * BETA + faceScore * (1-BETA)
* The MOVE_ALLOWED boolean in the LocateUserActivity.java file is used to allow Temi to move around (true) or only rotate (false)
* The BIAS value in SoundSource.java file should be changed accordingly to the difference in angles between Temi 0 degrees and Raspberry Pi 0 degrees
* The STD_DEVIATION and MEAN values in SoundSourceVector.java file are used to define the gaussian used to deal with ODAS readings' error. The STD_DEVIATION value can be changed for performance but the MEAN value should remain as 0.
* The ALPHA value in SoundSourceVector.java file is used by the filter applied to the ODAS readings according to: <br />
C(n) = (1 - ALPHA) U(n) + ALPHA * C(n-1), where C(n) is the output of the filter and U(n) is the reading from ODAS
