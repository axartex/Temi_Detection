# Temi_Detection

Temi Detection is an Android application based on a Temi robot. It combines the Temi robot SDK, the Alize API and the ODAS framework to produce a detectio program able of localizing and recognizing a subject. The ODAS framework should be instaled in a Raspberry Pi, with an external microphone array.

## ODAS Instalation

In order to install ODAS in your Raspberry Pi you should follow the instructions in:
[ODAS Project](https://github.com/introlab/odas/wiki "Named link title")


## Alizé Instalation

The Alizé system is already installed in this project. However, if any problem occurs, you should delete the Alizé folder and reinstall the API by following the instructions in:
[Alizé Project](https://github.com/ALIZE-Speaker-Recognition/android-alize "Named link title")

## Installation on Temi

To install this program on Temi you should:
1. Install Android Studio on your computer
2. Install ADB in your Android Studio
3. Be sure your computer, Temi and Raspberry Pi are all on the same WiFi network
4. Connect your computer to Temi (Follow the instructions on Temi Developers Tools)
5. Open this project on Android studio
6. Run this project on Temi (be sure that the selected device is Temi, aka Rockchip)
7. Start connection.py in Raspberry Pi (this file should be in the same folder as the ODAS runnable)

#Important Parameters
