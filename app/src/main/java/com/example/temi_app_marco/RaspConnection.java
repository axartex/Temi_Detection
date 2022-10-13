package com.example.temi_app_marco;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;

public class RaspConnection{

    private Socket clientSocket;
    private PrintWriter out;
    private static RaspConnection connection;
    private static final String SERVER_IP = "10.1.21.153";
    private static final int PORT = 1500;
    private Thread clientThread;

    private RaspConnection(){
        clientThread = new Thread(new ClientThread());
        clientThread.start();
    }

    public static RaspConnection get(){
        if(connection == null) {
            connection = new RaspConnection();
        }
        if(connection.clientSocket == null){
            connection = new RaspConnection();
        }
        return connection;
    }

    public void startODAS(){
        try {
            sendMessage("ON");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void stopODAS(){
        try {
            sendMessage("OFF");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void sendMessage(String msg) throws IOException {
        out.print(msg);
        out.flush();
    }

    public void stopConnection() throws IOException {
        out.close();
        clientSocket.close();
    }

    class ClientThread implements Runnable {

        @Override
        public void run() {

            try {
                InetAddress serverAddr = InetAddress.getByName(SERVER_IP);
                clientSocket = new Socket(serverAddr, PORT);
                out = new PrintWriter(clientSocket.getOutputStream(), true);

            } catch (UnknownHostException e1) {
                e1.printStackTrace();
            } catch (IOException e1) {
                e1.printStackTrace();
            }

        }

    }
}
