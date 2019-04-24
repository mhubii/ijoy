package com.example.max.ifp;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.EditText;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

public class SettingsActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener {

    //#########################################################################
    //Class variables
    SharedPreferences pref;
    SharedPreferences.Editor editor;

    EditText editText_ip;
    EditText editText_port_send;
    EditText editText_port_receive;
    TextView textView_minTilt;
    TextView textView_maxTilt;
    SeekBar seekBar_minTilt;
    SeekBar seekBar_maxTilt;


    //#########################################################################

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.addDrawerListener(toggle);
        toggle.syncState();

        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);

        findInitUiElements();

    }

    //#########################################################################
    //#########################################################################
    //Functions for UI interaction
    private void findInitUiElements(){
        //find
        editText_ip = (EditText) findViewById(R.id.editText_ip);
        editText_port_send = (EditText) findViewById(R.id.editText_port_send);
        editText_port_receive = (EditText) findViewById(R.id.editText_port_receive);
        textView_minTilt = (TextView)findViewById(R.id.textView_minTilt);
        textView_maxTilt = (TextView)findViewById(R.id.textView_maxTilt);
        seekBar_minTilt = (SeekBar)findViewById(R.id.seekBar_minTilt);
        seekBar_maxTilt = (SeekBar)findViewById(R.id.seekBar_maxTilt);

        //init
        pref = getApplicationContext().getSharedPreferences("MyPref", 0); // 0 - for private mode
        editor = pref.edit();
        editText_ip.setText(pref.getString("ip", "notSet"));
        editText_port_send.setText("" + pref.getInt("portSend", -1));
        editText_port_receive.setText("" + pref.getInt("portReceive", -1));

        //TODO evt zusammenschieben
        int min = (int)(pref.getFloat("minTilt", 15f));
        int max = (int)(pref.getFloat("maxTilt", 60f)-10); //sub 10 because scale is shifted

        Log.i("tcp" , "load " + min + ", " + (max+10));
        seekBar_minTilt.setProgress(min);
        seekBar_maxTilt.setProgress(max);
        textView_minTilt.setText("Minimum Tilt : " + seekBar_minTilt.getProgress());
        textView_maxTilt.setText("Maximum Tilt : " + (seekBar_maxTilt.getProgress()+10)); //add 10 because scale is shifted

        seekBar_minTilt.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {  }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {  }

            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if(progress >= seekBar_maxTilt.getProgress()){
                    seekBar_maxTilt.setProgress(progress);
                }
                textView_minTilt.setText("Minimum Tilt : " + progress);
            }
        });

        seekBar_maxTilt.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {  }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {  }

            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if(progress <= seekBar_minTilt.getProgress()){
                    seekBar_minTilt.setProgress(progress);
                }
                textView_maxTilt.setText("Maximum Tilt : " + (progress+10));
            }
        });
    }

    public void onSaveSettingsButton(final View view){
        editor.putString("ip", editText_ip.getText().toString());
        editor.putInt("portSend", Integer.parseInt(editText_port_send.getText().toString()));
        editor.putInt("portReceive", Integer.parseInt(editText_port_receive.getText().toString()));
        editor.putFloat("minTilt", (float)seekBar_minTilt.getProgress());
        editor.putFloat("maxTilt", (float)(seekBar_maxTilt.getProgress()+10));
        editor.apply(); //oder editor.commit

        Log.i("tcp" , "put " + (float)seekBar_minTilt.getProgress() + ", " + (float)(seekBar_maxTilt.getProgress()+10));

        Toast.makeText(this, "Settings saved", Toast.LENGTH_LONG).show();
    }

    public void onClearSettingsButton(final View view){
        editor.clear();
        editor.apply();

        editText_ip.setText("notSet");
        editText_port_send.setText("" + -1);
        editText_port_receive.setText("" + -1);
        //TODO evt default fuer slider

        Toast.makeText(this, "Settings cleared", Toast.LENGTH_LONG).show();
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
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        // Handle navigation view item clicks here.
        int id = item.getItemId();

        if (id == R.id.nav_connect) {
            Intent m = new Intent(this.getApplicationContext(), ConnectActivity.class);
            startActivity(m);
        } else if (id == R.id.nav_joystick) {
            Intent m = new Intent(this.getApplicationContext(), JoystickActivity.class);
            startActivity(m);
        } else if (id == R.id.nav_joystick2) {
            Intent m = new Intent(this.getApplicationContext(), JoystickActivity2.class);
            startActivity(m);
        } else if (id == R.id.nav_settings) {

        }

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }
}
