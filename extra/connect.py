#!/usr/bin/env python3
import socket
import signal
import subprocess
import os

HOST = "0.0.0.0"  # Standard loopback interface address (localhost)
PORT = 1500 # Port to listen on (non-privileged ports are > 1023)

with socket.socket(socket.AF_INET, socket.SOCK_STREAM) as s:
    s.bind((HOST, PORT))
    s.listen()
    conn, addr = s.accept()
    with conn:
        print(f"Connected by {addr}")
        while True:
            data = conn.recv(1024)
            if not data:
                break
            print(data)
            if data == b"ligar":
                print("Ligar programa")
                pro = subprocess.Popen("./odaslive -c respeaker_mic_array.cfg", stdout=subprocess.PIPE, shell=True, preexec_fn=os.setsid) # change command to execute
            elif data == b"desligar":
                print("Desligar programa")
                os.killpg(os.getpgid(pro.pid), signal.SIGTERM)  # Send the signal to all the process groups