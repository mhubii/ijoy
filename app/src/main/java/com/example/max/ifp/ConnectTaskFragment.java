package com.example.max.ifp;

import android.app.Activity;
import android.app.Fragment;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;

/*
    TODO
    MainActivity muss in Manifest
        android:launchMode="singleInstance" oder
        android:launchMode="singleTask" sein sonst wird mit falscher activity kommuniziert
 */

/**
 * This Fragment manages a single background task and retains
 * itself across configuration changes.
 */
public class ConnectTaskFragment extends Fragment {

    private static int connectionStatus;
    private static ConnectTask mTask;
    private int xVect = 0;
    private int yVect = 0;
    private int aVect = 0;
    private boolean sending = false;

    /**
     * Callback interface through which the fragment will report the
     * task's progress and results back to the Activity.
     */
    interface TaskCallbacks {
        void onPreExecute(int id);
        void onProgressUpdate(String type, String message);
        void onCancelled();
        void onWarningReceived(String msg);
        void onConnectionStatusChange(int status); //TODO
        //void onStatusChange();
    }

    private static TaskCallbacks mCallbacks;
    private static TcpClient mTcpClient;
    private static String ip;
    private static int portSend;
    private static int portReceive;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Retain this fragment across configuration changes.
        setRetainInstance(true);
        createHandler();
        connectionStatus = 0;
    }

    //send all notifications to calling activity
    public void attach(Activity activity){
        super.onAttach(activity);
        mCallbacks = (TaskCallbacks) activity;
    }

    @Override
    public void onAttach(Activity activity) {
        Log.i("tcp", "onAttach " + activity.getLocalClassName());
        mCallbacks = (TaskCallbacks) activity;
        super.onAttach(activity);
    }

    @Override
    public void onDetach() {
        super.onDetach();
        Log.i("tcp", "onDetach");
        mCallbacks = null;
    }

    //#########################################################################
    void createHandler(){ //TODO useless - handled in TcpClient
        final Handler handler=new Handler();
        handler.post(new Runnable(){
            @Override
            public void run() {
                // check connection then sleep
                if(mTcpClient != null){
                    //Log.i("tcp", "connected: " + mTcpClient.isConnected());
                    Log.i("tcp", "from handler");
                    //mTcpClient.sendMessage("d\n" + "from handler");
                }
                //Log.i("tcp", "handler sleeping");
                handler.postDelayed(this, 3000);
                //mCallbacks.onPreExecute(id);
            }
        });
    }

    int getConnectionStatus(){
        return connectionStatus;
    }

    //#########################################################################
    //#########################################################################
    //UI Methods
    public void onConnectButton(String _ip, int _portSend, int _portReceive){
        connectionStatus = 1;
        mCallbacks.onConnectionStatusChange(1);
        ip = _ip;
        portSend = _portSend;
        portReceive = _portReceive;
        mTask = (ConnectTask) new ConnectTask().execute("");
    }

    public void onDisconnectButton(){
        mTcpClient.stopClient();
        mTask.cancel(true);
        connectionStatus = 0;
        mCallbacks.onConnectionStatusChange(0);
    }

    public void sendViaSendPort(String msg){
        mTcpClient.sendMessage(msg);
    }

    public void sendViaRecvPort(String msg){
        mTcpClient.sendMessageReceiveSocket(msg);
    }

    public void onPingButton(String ip){
        mTcpClient.sendPing(ip);
    }

    //#########################################################################
    //#########################################################################
    public void sendMessage(String msg){
        Log.i("tcp", "trying to send message:" + msg);
        if(mTcpClient != null){
            //yarp expects "d" to intialize message - so send "d" - "return" - then actual message
            mTcpClient.sendMessage("d\n" + msg);
        }
    }

    //get code from message
    static int parseMessageToCode(String msg){
        switch (msg){
            case "(RobotStatus 0)": return 0;
            case "(RobotStatus 1)": return 1;
            case "(RobotStatus 2)": return 2;
            case "(RobotStatus 3)": return 3;
            case "(Warning 0)": return 4;
            case "(Warning 1)": return 5;
            case "(ERROR 0)": return 6;
            case "(ERROR 1)": return 7;
            case "(ERROR 2)": return 8;
            default: return -1;
        }
    }

    static String getMessageTextFromId(int id){
        String messages[] = {"Not Connected",
                "Not Initialized",
                "Initializing",
                "Initialization Done",
                "No Warning",
                "Inverse kinematics did not converge",
                "No Error",
                "Quadratic problem infeasible",
                "Hardware Limits"};

        if(id < 0){
            return "Unrecognized Command";
        }

        return messages[id];
    }

    public static void parseMessage(String msg){
        Log.i("tcp", "parsing Message: <" + msg + ">");

        String cleanMsg = msg.replaceAll("\r", "").replaceAll("\n", "");

        int code = parseMessageToCode(cleanMsg);

        if(code == 3){
            connectionStatus = 4;
            mCallbacks.onConnectionStatusChange(4);
        } else if(code == 1 || code == 2){
            connectionStatus = 3;
            mCallbacks.onConnectionStatusChange(3);
        }
        //TODO richtig verarbeiten nicht nur anzeigen
        mCallbacks.onWarningReceived("recieved: " + getMessageTextFromId(code) + "\n<" + cleanMsg + ">");

        Log.i("tcp", "recieved Code: " + getMessageTextFromId(code));
    }

    //#########################################################################
    //#########################################################################
    public static class ConnectTask extends AsyncTask<String, String, TcpClient> {

        @Override
        protected TcpClient doInBackground(String... message) {

            //we create a TCPClient object
            mTcpClient = new TcpClient(
                new TcpClient.OnMessageReceived() {
                    @Override
                    public void messageReceived(String message, boolean receiveSocket) {
                        if(receiveSocket && message.startsWith("welcome ")){
                            //mTcpClient.sendMessageReceiveSocket("r");
                        }

                        Log.i("tcp", "ConnectTask recieved: <" + message + ">");
                        publishProgress(message); //hand over to onProgressUpdate
                    }

                    @Override
                    public void statusChanged(final String type, final String status){
                        Log.i("tcp", "Status changed: " + status);

                        if(type.equals("connectionStatus")){
                            try{
                                connectionStatus = Integer.parseInt(status);
                                mCallbacks.onConnectionStatusChange(connectionStatus);
                                Log.i("tcp", "update connectionStatus to: " + connectionStatus);

                                //on successful connection "login" to yarp
                                if(connectionStatus == 3){
                                    mTcpClient.sendMessage("CONNECT app");
                                    mTcpClient.sendMessageReceiveSocket("CONNECT app");
                                }

                            } catch (Exception e){
                                //do nothing
                            }
                        } else {
                            mCallbacks.onProgressUpdate(type, status);
                        }
                    }
                }, ip, portSend, portReceive);
            Log.i("tag", "pre mTcpClient.run()");
            mTcpClient.run(); //TODO ausserhlab hier gibt networkOnMainThread exception

            return null;
        }

        @Override
        protected void onProgressUpdate(String... values) {
            super.onProgressUpdate(values);
            parseMessage(values[0]);
            Log.d("test", "response " + values[0]);
        }
    }
    //#########################################################################
    //#########################################################################
}