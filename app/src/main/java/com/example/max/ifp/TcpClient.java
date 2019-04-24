package com.example.max.ifp;

import android.util.Log;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ConnectException;
import java.net.InetAddress;
import java.net.Socket;

class TcpClient {

    private static String SERVER_IP; //server IP address
    private static int SERVER_PORT_SEND;
    private static int SERVER_PORT_RECEIVE;
    // message to send to the server
    private String mServerMessageReceive;
    private String mServerMessageSend;
    // sends message received notifications
    private OnMessageReceived mMessageListener = null;
    // while this is true, the server will continue running
    private boolean mRun = false;
    private boolean mStarting = false;
    // used to send messages
    private PrintWriter mBufferOutSend;
    private PrintWriter mBufferOutReceive;
    // used to read messages from the server
    private BufferedReader mBufferInReceive;
    private BufferedReader mBufferInSend;

    private Socket socket_send;
    private Socket socket_receive;

    private static Process process_ping;

    TcpClient(OnMessageReceived listener, String ip, int portSend, int portReceive) {
        mMessageListener = listener;
        SERVER_IP = ip;
        SERVER_PORT_SEND = portSend;
        SERVER_PORT_RECEIVE = portReceive;
    }

    void sendMessage(final String message) {
        if (mBufferOutSend != null && !mBufferOutSend.checkError()) {
            Thread thread = new Thread(new Runnable() {

                @Override
                public void run() {
                    try {
                        mBufferOutSend.println(message);
                        mBufferOutSend.flush();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            });
            thread.start();
        }
    }

    void sendMessageReceiveSocket(final String message) {
        if (mBufferOutSend != null && !mBufferOutSend.checkError()) {
            Thread thread = new Thread(new Runnable() {

                @Override
                public void run() {
                    try {
                        mBufferOutReceive.println(message);
                        mBufferOutReceive.flush();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            });
            thread.start();
        }
    }

    void stopClient() {

        mRun = false;

        if (mBufferOutSend != null) {
            mBufferOutSend.flush();
            mBufferOutSend.close();
        }

        emitStatusChanged("connectionStatus", "0");
        mMessageListener = null;
        mBufferInReceive = null;
        mBufferInSend = null;
        mBufferOutSend = null;
        mServerMessageReceive = null;
        mServerMessageSend = null;
        try {
            if(process_ping != null){
                process_ping.destroy();
            }
            if(socket_receive != null){
                socket_receive.close();
            }
            if(socket_send != null){
                socket_send.close();
            }
        } catch (Exception e){
            //do nothing
        }
    }

    boolean sendPing(String ip){

        boolean returnValue = false;

        String cmd = "ping -c 5 -w 5 " + ip;

        try {
            process_ping = Runtime.getRuntime().exec(cmd);

            BufferedReader stdInput = new BufferedReader(new InputStreamReader(process_ping.getInputStream()));

            String s;
            while ((s = stdInput.readLine()) != null) {
                Log.i("tcp", s);
            }
            process_ping.waitFor();
            if(process_ping.exitValue() == 0){
                returnValue = true;
            }
            Log.i("tcp", "ping return value: " + process_ping.exitValue());
            process_ping.destroy();
        } catch (Exception e) {
            e.printStackTrace();
            Log.i("tcp", e.toString());
        }

        return returnValue;
    }

    private void emitStatusChanged(String type, String message){
        if(mMessageListener != null) {
            mMessageListener.statusChanged(type, message);
        }
    }


    boolean isConnected(){
        return (mStarting || (socket_send.isConnected() && socket_receive.isConnected()));
    }

    void run() {

        mStarting = true;
        mRun = true;

        //emitStatusChanged("connectionStatus", "1");
        emitStatusChanged("connect", "\nSending Ping");

        //check if server is reachable
        if(!sendPing(SERVER_IP)){
            emitStatusChanged("connectionStatus", "0");
            emitStatusChanged("connect", "Ping Failed");
            emitStatusChanged("error", "Ping failed - Check connection");
            Log.i("tcp", "ping failed");
            return;
        } else {
            emitStatusChanged("connect", " ok\n");
            Log.i("tcp", "ping success");
        }

        try {
            //create sockets
            emitStatusChanged("connect", "try to create send socket");
            InetAddress serverAddr_send = InetAddress.getByName(SERVER_IP);
            socket_send = new Socket(serverAddr_send, SERVER_PORT_SEND);
            Log.i("tcp", "socket_send created");
            emitStatusChanged("connect", " ok\n");

            emitStatusChanged("connect", "try to create receive socket");
            InetAddress serverAddr_receive = InetAddress.getByName(SERVER_IP);
            socket_receive = new Socket(serverAddr_receive, SERVER_PORT_RECEIVE);
            Log.i("tcp", "socket_receive created");
            emitStatusChanged("connect", " ok\n");

            try {
                //initialize buffers
                mBufferOutSend = new PrintWriter(socket_send.getOutputStream());
                mBufferOutReceive = new PrintWriter(socket_receive.getOutputStream());
                mBufferInReceive = new BufferedReader(new InputStreamReader(socket_receive.getInputStream()));
                int charsRead;
                char[] buffer = new char[1024]; //choose your buffer size if you need other than 1024

                emitStatusChanged("connect", "checking connect");
                //check connection one last time
                long startTime = System.currentTimeMillis();
                long timeout = 10000;
                boolean success = false;
                while( System.currentTimeMillis()-startTime < timeout ){
                    if(socket_send.isConnected() && socket_receive.isConnected()){
                        success = true;
                        mStarting = false;
                        break;
                    }
                }
                if (success){ //TODO check stability
                    Log.i("tcpClient", "connected succesfully");
                    emitStatusChanged("connectionStatus", "3");
                    emitStatusChanged("connect", " ok\n");
                    emitStatusChanged("connect", "\nwaiting for ready signal");
                } else {
                    Log.i("tcpClient", "connection failed");
                    stopClient();
                }

                read2();
                while (mRun) {
                    Log.i("tcp", "client running in while loop");
                    charsRead = mBufferInReceive.read(buffer);
                    mServerMessageReceive = new String(buffer).substring(0, charsRead);
                    Log.i("tcp", "socketReceive recieved: <" + mServerMessageReceive + ">");
                    if (mMessageListener != null) {
                        mMessageListener.messageReceived(mServerMessageReceive, true);
                    }
                    mServerMessageReceive = null;
                }
            //exception while connection is running
            } catch (Exception e) {
                Log.i("tcp", "exception to string: " + e.toString());
                // java.net.SocketException: Connection reset

                Log.e("TCP", "S: Error", e);

            } finally {
                //the sockets must be closed. It is not possible to reconnect
                //once closed, which means a new sockets instance have to be created.
                emitStatusChanged("connectionStatus", "0");
                emitStatusChanged("error", "connection closed");
                socket_send.close();
                socket_receive.close();
            }

        //exception on creation of connection
        } catch (ConnectException e) {
            Log.e("TCP", "C: Error Connect Exception");
            emitStatusChanged("connectionStatus", "0");
            emitStatusChanged("error", e.toString());

        } catch (Exception e) {
            Log.e("TCP", "C: Error ", e);
                emitStatusChanged("connectionStatus", "0");
                emitStatusChanged("error", e.toString());
        }

    }

    private void read2(){
        Thread thread = new Thread(new Runnable() {

            @Override
            public void run() {
                try {
                    //initialize buffer
                    mBufferInSend = new BufferedReader(new InputStreamReader(socket_send.getInputStream()));
                    int charsRead;
                    char[] buffer = new char[1024]; //choose your buffer size if you need other than 1024

                    while (mRun) {
                        Log.i("tcp", "client running in while loop send");
                        charsRead = mBufferInSend.read(buffer);
                        mServerMessageSend = new String(buffer).substring(0, charsRead);
                        Log.i("tcp", "socketSend recieved: " + mServerMessageSend);
                        if (mMessageListener != null) {
                            mMessageListener.messageReceived(mServerMessageSend, false);
                        }
                        mServerMessageSend = null;
                    }
                    //exception while connection is running
                } catch (Exception e) {
                    mRun = false;
                    Log.i("tcp", "exception to string: " + e.toString());
                    // java.net.SocketException: Connection reset

                    Log.e("TCP", "S: Error", e);
                } finally {
                    //the sockets must be closed. It is not possible to reconnect
                    //once closed, which means a new sockets instance have to be created.
                    try {
                        socket_send.close();
                        socket_receive.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        });
        thread.start();
    }

    //Declare the interface. The method messageReceived(String message) will must be implemented in the MyActivity
    //class at on asynckTask doInBackground
    public interface OnMessageReceived {
        void messageReceived(String message, boolean recieveSocket);
        void statusChanged(String type, String status);
    }

}
