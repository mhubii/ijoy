package com.example.max.ifp;

import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.design.widget.Snackbar;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

public class ConnectActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener, ConnectTaskFragment.TaskCallbacks {

    //#########################################################################
    //Class variables
    private static final String TAG_TASK_FRAGMENT = "task_fragment";
    private static ConnectTaskFragment mTaskFragment = null;

    private SharedPreferences pref;

    private Button button_connect;
    private Button button_disconnect;
    private Button button_ping;
    private Button button_send;
    private EditText editText_message;
    private TextView textView_ip;
    private TextView textView_port_send;
    private TextView textView_port_recv;

    private ProgressDialog progressDialog;
    private String progressDialogMessage;
    private Snackbar snackbarStatus;

    private String ip;
    private int portSend;
    private int portReceive;

    //#########################################################################

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.i("tcp", "ConnectActivity onCreate");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_connect);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.addDrawerListener(toggle);
        toggle.syncState();

        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);

        //#####################################################################
        if (mTaskFragment == null) {
            FragmentManager fm = getFragmentManager();
            //FragmentTransaction tr = getSupportFragmentManager().beginTransaction();
            FragmentTransaction tr = fm.beginTransaction();
            mTaskFragment = (ConnectTaskFragment) fm.findFragmentByTag(TAG_TASK_FRAGMENT);
            getSupportFragmentManager().executePendingTransactions();

            //if could not be found check if saved already - if not create new one
            if(mTaskFragment == null) {
                Log.i("tcp", "Connect create new fragment");
                mTaskFragment = new ConnectTaskFragment();
                tr.add(mTaskFragment, TAG_TASK_FRAGMENT);
                tr.addToBackStack(null);//TODO das ist neu
                tr.commit();
            } else {
                Log.i("tcp", "connect: fragment found");
            }
        }
        mTaskFragment.attach(this);

        //#####################################################################
        findInitUiElements();
    }

    public static ConnectTaskFragment getInstance(){
        return mTaskFragment;
    }

    //#########################################################################
    //#########################################################################
    //Functions for UI interaction
    private void findInitUiElements(){
        Log.i("tcp", "ConnectActivity findInitUiElements");
        //find
        button_connect = (Button)findViewById(R.id.button_connect);
        button_disconnect = (Button)findViewById(R.id.button_disconnect);
        button_ping = (Button)findViewById(R.id.button_ping);
        button_send = (Button)findViewById(R.id.button_send);
        editText_message = (EditText)findViewById(R.id.editText_message);
        textView_ip = (TextView)findViewById(R.id.textView_ip);
        textView_port_send = (TextView)findViewById(R.id.textView_port_send);
        textView_port_recv = (TextView)findViewById(R.id.textView_port_recv);

        //init
        pref = getApplicationContext().getSharedPreferences("MyPref", 0); // 0 - for private mode
        ip = pref.getString("ip", "notSet");
        portSend = pref.getInt("portSend", -1);
        portReceive = pref.getInt("portReceive", -1);
        textView_ip.setText(ip);
        textView_port_send.setText("" + portSend);
        textView_port_recv.setText("" + portReceive);
        showSnackbar(mTaskFragment.getConnectionStatus());
        if(mTaskFragment.getConnectionStatus() == 0){
            onDisconected();
        } else {
            onConnected();
        }

        progressDialog = new ProgressDialog(this);
    }

    private void onDisconected(){
        button_connect.setClickable(true);
        button_connect.setAlpha(1f);
        button_disconnect.setClickable(false);
        button_disconnect.setAlpha(.3f); //gray out button
        button_send.setClickable(false);
        button_send.setAlpha(.3f);
        button_ping.setClickable(false);
        button_ping.setAlpha(.3f);
    }

    private void onConnected(){
        button_connect.setClickable(false);
        button_connect.setAlpha(.3f);
        button_disconnect.setClickable(true);
        button_disconnect.setAlpha(1f);
        button_send.setClickable(true);
        button_send.setAlpha(1f);
        button_ping.setClickable(true);
        button_ping.setAlpha(1f);
    }

    public void onConnectButtonClicked(final View view){
        progressDialogMessage = "Tying to connect";
        progressDialog.setTitle("Connecting");
        progressDialog.setMessage(progressDialogMessage);
        progressDialog.setCancelable(true); // disable dismiss by tapping outside of the dialog
        progressDialog.show();
        mTaskFragment.onConnectButton(ip, portSend, portReceive);
    }

    public void showSnackbar(int type){
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);

        String msg = "";
        int color = R.color.colorBlack;
        switch (type){
            case 0: msg = "Not connected"; color = R.color.colorRed; break;
            case 1: msg = "Tying to connect"; color = R.color.colorOrange; break;
            case 2: msg = "Connection failed"; color = R.color.colorRed; break;
            case 3: msg = "Connected - waiting for ready"; color = R.color.colorLightGreen; break;
            case 4: msg = "Connected - ready"; color = R.color.colorGreen; break;
        }

        if(snackbarStatus != null){
            snackbarStatus.dismiss();
        }
        snackbarStatus = Snackbar.make(drawer, msg + " " + type, Snackbar.LENGTH_INDEFINITE)
                .setAction("Action", null); //TODO green not working
        snackbarStatus.getView().setBackgroundColor(ContextCompat.getColor(this, color));
        snackbarStatus.show();
    }

    public void onDisonnectButtonClicked(final View view){
        mTaskFragment.onDisconnectButton();
    }

    static int blub = 0;
    public void onPingButtonClicked(final View view){
        //mTaskFragment.onPingButton(ip);
        //showLoadDialog();
        //Common.showAlertDialog(this, "jsdahk", "aksjdhksdj");
        /*
        showSnackbar(blub);
        blub++;
        if(blub > 4){
            blub = 0;
        }
        */
        mTaskFragment.sendViaRecvPort(editText_message.getText().toString());
    }

    public void onSendButtonClicked(final View view){
        mTaskFragment.sendViaSendPort(editText_message.getText().toString());
    }

    public void showLoadDialog(){
        final ProgressDialog progress = new ProgressDialog(this);
        progress.setTitle("Loading");
        progress.setMessage("Wait while loading...");
        progress.setCancelable(true); // disable dismiss by tapping outside of the dialog
        progress.show();
        // To dismiss the dialog
        //progress.dismiss();
    }

    //#########################################################################
    //#########################################################################
    //Functions for ConnectTaskFragment

    static int i = 0;
    @Override
    public void onPreExecute(int id) { //TODO das war nur zum testen oder?
        Log.i("tcp", "joystick activity called from fragment " + i);
        if((i+1)%3==0){
            //Common.showAlertDialog(this, "blub", "nummer " + i);
            DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
            Common.showSnackbar(drawer,"nummer " + i);
        } else {
            Common.showToast(this, "nummer " + i);
        }
        i++;
    }

    @Override
    public void onProgressUpdate(final String type, final String message) {
        //progressDialog.dismiss();
        Log.i("tcp", "connect activity recieve: <" + message + ">");
        //editText_response.setText(message); //TODO gefahr von crash

        if(type.equals("connect")){
            try {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        progressDialogMessage += message;
                        progressDialog.setMessage(progressDialogMessage);
                    }
                });
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else if(type.equals("error")){
            onDisconected();
            Log.i("tcp", "Connect activity - connection died");
            //showAlertDialog("Connection lost", message);
            try {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Common.showAlertDialog(ConnectActivity.this, "Connection lost", message); //ok muss aber hier in "runOnUiThread" sein
                    }
                });
            } catch (Exception e) {
                e.printStackTrace();
            }

            progressDialog.dismiss();
        }
    }

    @Override
    public void onCancelled() {  }

    @Override
    public void onWarningReceived(String msg) { //TODO hier alten toast merken und wenn neuer kommt ueberschreiben
        Toast toast = Toast.makeText(this, msg, Toast.LENGTH_LONG);
        toast.setGravity(Gravity.TOP,0,100);
        toast.show();
    }

    @Override
    public void onConnectionStatusChange(int status) {
        showSnackbar(status);
        if(status == 0){
            onDisconected();
        } else {
            onConnected();
        }
        if(status == 4){
            progressDialog.dismiss();
        }
    }

    //#########################################################################
    //#########################################################################
    //Functions for Navigation

    @Override
    public void onBackPressed() {
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.connect, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            Intent m = new Intent(this.getApplicationContext(), SettingsActivity.class);
            startActivity(m);
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        // Handle navigation view item clicks here.
        int id = item.getItemId();

        if (id == R.id.nav_connect) {

        } else if (id == R.id.nav_joystick) {
            Intent m = new Intent(this.getApplicationContext(), JoystickActivity.class);
            startActivity(m);
        } else if (id == R.id.nav_joystick2) {
            Intent m = new Intent(this.getApplicationContext(), JoystickActivity2.class);
            startActivity(m);
        } else if (id == R.id.nav_settings) {
            Intent m = new Intent(this.getApplicationContext(), SettingsActivity.class);
            startActivity(m);
        }

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }
}




/*
    public void showAlertDialog(final String title, final String msg){
        try {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(ConnectActivity.this);

                    // set title
                    alertDialogBuilder.setTitle(title);

                    // set dialog message
                    alertDialogBuilder
                            .setMessage(msg)
                            .setCancelable(false)
                            .setPositiveButton("Close this Activity",new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog,int id) {
                                    // if this button is clicked, close
                                    // current activity
                                    ConnectActivity.this.finish();
                                }
                            })
                            .setNegativeButton("Hide",new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int id) {
                                    // if this button is clicked, just close
                                    // the dialog box and do nothing
                                    dialog.cancel();
                                }
                            });

                    // create alert dialog
                    AlertDialog alertDialog = alertDialogBuilder.create();

                    // show it
                    alertDialog.show();
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
        }

        /*
        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(
                this);

        // set title
        alertDialogBuilder.setTitle(title);

        // set dialog message
        alertDialogBuilder
                .setMessage(msg)
                .setCancelable(false)
                .setPositiveButton("Yes",new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog,int id) {
                        // if this button is clicked, close
                        // current activity
                        ConnectActivity.this.finish();
                    }
                })
                .setNegativeButton("No",new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        // if this button is clicked, just close
                        // the dialog box and do nothing
                        dialog.cancel();
                    }
                });

        // create alert dialog
        AlertDialog alertDialog = alertDialogBuilder.create();

        // show it
        alertDialog.show();

    }
    */