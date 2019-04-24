package com.example.max.ifp;

//TODO nur hochformat oder bei drehung ersetzen

import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Point;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.view.Display;
import android.view.Gravity;
import android.view.MotionEvent;
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
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import java.text.DecimalFormat;

public class JoystickActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener, ConnectTaskFragment.TaskCallbacks {

    //#########################################################################
    //Class variables
    private static final String TAG_TASK_FRAGMENT = "task_fragment";
    private ConnectTaskFragment mTaskFragment;

    private ImageView imageView_joystick;
    private ImageView imageView_slider;

    private Snackbar snackbarStatus;

    private int mWidth;
    private int mHeight;

    private int mBorderSize = 25;
    private int mButtonSize = 60;
    private int mCircleSize;
    private int mRectSize;
    private int mRectBorder;
    private int mBmpSize;

    private double maxX = 1;
    private double maxY = 1;
    private double maxA = 1;

    private double xVector = 0;
    private double yVector = 0;
    private double aVector = 0;

    //#########################################################################

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_joystick);
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
        /*
        FragmentManager fm = getFragmentManager();
        mTaskFragment = (ConnectTaskFragment) fm.findFragmentByTag(TAG_TASK_FRAGMENT);
        fm.executePendingTransactions();

        // If the Fragment is non-null, then it is currently being
        // retained across a configuration change.
        if (mTaskFragment == null) {
            Log.i("tcp", "Joystick create new fragment");
            mTaskFragment = new ConnectTaskFragment();
            fm.beginTransaction().add(mTaskFragment, TAG_TASK_FRAGMENT).commit();
        }
        */
        mTaskFragment = ConnectActivity.getInstance();
        mTaskFragment.attach(this);
        //#####################################################################
        findInitUiElements();
    }

    //#########################################################################
    //#########################################################################
    //Functions for UI interaction

    private void findInitUiElements(){
        //find
        imageView_joystick = (ImageView)findViewById(R.id.imageView_joystick);
        imageView_slider = (ImageView)findViewById(R.id.imageView_slider);

        //init
        getDimensions();
        initUI();
        showSnackbar(mTaskFragment.getConnectionStatus());
    }

    //set dimensions of "joystick" and "slider"
    private void getDimensions(){
        Display display = getWindowManager().getDefaultDisplay();
        Point size = new Point();
        display.getSize(size);
        mWidth = size.x-100;
        mHeight = size.y-100;
        //20dp from edge of screen
        mBmpSize = mWidth-20;
        //10dp from edge of bmp
        mCircleSize = ((mBmpSize/2)-mButtonSize-mBorderSize);
        mRectSize = mBmpSize-mButtonSize*2-mBorderSize*2;
        mRectBorder = (mBmpSize-mRectSize)/2;
        Log.i("tag", mWidth + "x" + mHeight);
    }

    public void initUI(){
        Bitmap bmp;
        Canvas canvas;
        Bitmap bmp2;
        Canvas canvas2;

        Paint paintRed;
        Paint paintBlack;
        Paint paintWhite;

        paintRed = new Paint();
        paintRed.setColor(Color.RED);
        paintRed.setStyle(Paint.Style.FILL);

        paintBlack = new Paint();
        paintBlack.setColor(Color.BLACK);
        paintBlack.setStyle(Paint.Style.STROKE);

        paintWhite = new Paint();
        paintWhite.setColor(Color.LTGRAY);
        paintWhite.setStyle(Paint.Style.FILL);

        bmp = Bitmap.createBitmap(mBmpSize, mBmpSize, Bitmap.Config.ARGB_8888);
        bmp2 = Bitmap.createBitmap(mBmpSize, mButtonSize*2+mBorderSize*2, Bitmap.Config.ARGB_8888);

        canvas = new Canvas(bmp);
        canvas.drawRect(0,0,bmp.getHeight(), bmp.getWidth(), paintWhite);
        canvas.drawCircle(bmp.getWidth()/2, bmp.getHeight()/2, mButtonSize, paintRed);
        canvas.drawRect(mRectBorder, mRectBorder, mRectBorder+mRectSize, mRectBorder+mRectSize, paintBlack);
        canvas.drawCircle(bmp.getWidth()/2, bmp.getHeight()/2, mCircleSize, paintBlack);

        canvas2 = new Canvas(bmp2);
        canvas2.drawRect(0,0,bmp2.getWidth(), bmp2.getHeight(), paintWhite);
        canvas2.drawCircle(bmp2.getWidth()/2, bmp2.getHeight()/2, mButtonSize, paintRed);
        canvas2.drawRect(mRectBorder, bmp2.getHeight()/2, mRectBorder+mRectSize, bmp2.getHeight()/2, paintBlack);

        imageView_joystick.setOnTouchListener(handleTouch_joystick);
        imageView_slider.setOnTouchListener(handleTouch_slider);

        imageView_joystick.setImageBitmap(bmp);
        imageView_slider.setImageBitmap(bmp2);
    }

    public void showToast(){
        try {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(JoystickActivity.this, "This is my Toast message!",
                            Toast.LENGTH_LONG).show();
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    //TODO similar for joystick2
    //TODO lock ui on not connected?
    public void showAlertDialog(final String title, final String msg){
        try {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(JoystickActivity.this);

                    // set title
                    alertDialogBuilder.setTitle(title);

                    // set dialog message
                    alertDialogBuilder
                            .setMessage(msg)
                            .setCancelable(false)
                            .setPositiveButton("Open connect Activity",new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog,int id) {
                                    // if this button is clicked, close
                                    // current activity
                                    Intent m = new Intent(getBaseContext(), ConnectActivity.class);
                                    startActivity(m);
                                    JoystickActivity.this.finish();
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

    }

    public void showSnackbar(int type){
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);

        String msg = "";
        int color = R.color.colorBlack;
        int length = Snackbar.LENGTH_INDEFINITE;
        switch (type){
            case 0: msg = "Not connected"; color = R.color.colorRed; break;
            case 1: msg = "Tying to connect"; color = R.color.colorOrange; break;
            case 2: msg = "Connection failed"; color = R.color.colorRed; break;
            case 3: msg = "Connected - waiting for ready"; color = R.color.colorLightGreen; break;
            case 4: msg = "Connected - ready"; color = R.color.colorGreen; break;
        }
        if(type == 3 || type == 4){
            length = Snackbar.LENGTH_SHORT;
        }

        if(snackbarStatus != null){
            snackbarStatus.dismiss();
        }
        snackbarStatus = Snackbar.make(drawer, msg + " " + type, length)
                .setAction("Action", null); //TODO green not working
        snackbarStatus.getView().setBackgroundColor(ContextCompat.getColor(this, color));
        snackbarStatus.show();
    }

    //#########################################################################
    //#########################################################################
    //Functions for Joystick

    public void sendVector(){
        mTaskFragment.sendMessage("(" + xVector + ", " + yVector + ", " + aVector + ")");
    }

    public void redrawSlider(int x){
        //TODO
        /*
        wenn beruehrt muss -mButtonSize/2 von x position abzeogen werden
        aber wenn losgelassen wurde dann nicht
         */

        Bitmap bmp2;
        Canvas canvas2;

        Paint paintRed;
        Paint paintBlack;
        Paint paintWhite;

        paintRed = new Paint();
        paintRed.setColor(Color.RED);
        paintRed.setStyle(Paint.Style.FILL);

        paintBlack = new Paint();
        paintBlack.setColor(Color.BLACK);
        paintBlack.setStyle(Paint.Style.STROKE);

        paintWhite = new Paint();
        paintWhite.setColor(Color.LTGRAY);
        paintWhite.setStyle(Paint.Style.FILL);

        bmp2 = Bitmap.createBitmap(mBmpSize, mButtonSize*2+mBorderSize*2, Bitmap.Config.ARGB_8888);
        canvas2 = new Canvas(bmp2);

        canvas2.drawRect(0,0,bmp2.getWidth(), bmp2.getHeight(), paintWhite);
        canvas2.drawRect(mRectBorder, bmp2.getHeight()/2, mRectBorder+mRectSize, bmp2.getHeight()/2, paintBlack);

        if(x == -1){
            x = bmp2.getWidth()/2;
        }

        //make sure "button" is inside or on outer rectangle

        if(x<mRectBorder){
            x = mRectBorder;
        } else if(x>mRectBorder+mRectSize){
            x = mRectBorder+mRectSize;
        }

        //#########################################

        //maximal distance from center
        int maxDist = mBmpSize/2-mRectBorder;
        //set a - not set by joystick
        //left half - rotate left
        if(x < mBmpSize/2){
            aVector = -1* maxY * ((mBmpSize/2-x)/(float)maxDist);
            //right half - rotate right
        } else {
            aVector = (maxY * ((x-mBmpSize/2)/(float)maxDist));
        }

        DecimalFormat df = new DecimalFormat("#.##");
        String vector = "(" + df.format(xVector) + ", " + df.format(yVector) + ", " + df.format(aVector) + ")";

        canvas2.drawCircle(x, bmp2.getHeight()/2, mButtonSize, paintRed);
        canvas2.drawCircle(x, bmp2.getHeight()/2, 15, paintBlack);

        //canvas.drawRect(mRectBorder, mRectBorder, mRectBorder+mRectSize, mRectBorder+mRectSize, paintBlack);
        //canvas.drawCircle(bmp.getWidth()/2, bmp.getHeight()/2, mCircleSize, paintBlack);

        imageView_slider.setImageBitmap(bmp2);
        imageView_slider.invalidate();
        sendVector();
    }

    public void redrawJoystick(int x, int y){
        Bitmap bmp;
        Canvas canvas;

        Paint paintRed;
        Paint paintBlack;
        Paint paintWhite;

        paintRed = new Paint();
        paintRed.setColor(Color.RED);
        paintRed.setStyle(Paint.Style.FILL);

        paintBlack = new Paint();
        paintBlack.setColor(Color.BLACK);
        paintBlack.setStyle(Paint.Style.STROKE);

        paintWhite = new Paint();
        paintWhite.setColor(Color.LTGRAY);
        paintWhite.setStyle(Paint.Style.FILL);

        bmp = Bitmap.createBitmap(mBmpSize, mBmpSize, Bitmap.Config.ARGB_8888);
        canvas = new Canvas(bmp);

        canvas.drawRect(0,0,bmp.getHeight(), bmp.getWidth(), paintWhite);


        if( x == -1 ){
            x = bmp.getWidth()/2;
        }
        if( y == -1 ){
            y = bmp.getHeight()/2;
        }

        float hdiff = bmp.getHeight()/2 - x;
        hdiff = (float)Math.sqrt(hdiff*hdiff);

        float vdiff = bmp.getWidth()/2 - y;
        vdiff = (float)Math.sqrt(vdiff*vdiff);

        float angle = (float)Math.atan2(y-mBmpSize/2, x-mBmpSize/2);

        //make sure "button" is inside or on outer rectangle
        if(x<mRectBorder){
            x = mRectBorder;
        } else if(x>mRectBorder+mRectSize){
            x = mRectBorder+mRectSize;
        }
        if(y<mRectBorder){
            y = mRectBorder;
        } else if(y>mRectBorder+mRectSize){
            y = mRectBorder+mRectSize;
        }

        //#####################################

        //textView.setText("" + angle + "(" + dist + ")");
        Log.i("TAG", hdiff + " " + vdiff + " " + angle);

        //maximal distance from center
        int maxDist = mBmpSize/2-mRectBorder;
        //set x
        //upper half - move forward
        if(y <= mBmpSize/2){
            xVector = maxX * ((mBmpSize/2-y)/(float)maxDist);
            //lower half - move backwards
        } else {
            xVector = -1 * (maxX * ((y-(mBmpSize/2))/(float)maxDist));
        }

        //set y - not set by joystick
        //left half - rotate left
        if(x < mBmpSize/2){
            yVector = -1* maxY * ((mBmpSize/2-x)/(float)maxDist);
            //right half - rotate right
        } else {
            yVector = (maxY * ((x-mBmpSize/2)/(float)maxDist));
        }

        DecimalFormat df = new DecimalFormat("#.##");
        String vector = "(" + df.format(xVector) + ", " + df.format(yVector) + ", " + df.format(aVector) + ")";

        //MainActivity.sendMessage("d");
        //MainActivity.sendMessage("d\n\r" + vector);

        //redraw joystick
        canvas.drawLine(bmp.getWidth()/2,bmp.getHeight()/2, x, y, paintBlack);


        canvas.drawCircle(x, y, mButtonSize, paintRed);
        canvas.drawCircle(x, y, 15, paintBlack);

        canvas.drawRect(mRectBorder, mRectBorder, mRectBorder+mRectSize, mRectBorder+mRectSize, paintBlack);
        canvas.drawCircle(bmp.getWidth()/2, bmp.getHeight()/2, mCircleSize, paintBlack);

        imageView_joystick.setImageBitmap(bmp);
        imageView_joystick.invalidate();
        sendVector();
    }

    private View.OnTouchListener handleTouch_joystick = new View.OnTouchListener() {

        @Override
        public boolean onTouch(View v, MotionEvent event) {

            v.performClick(); //TODO warnung falls nicht aber warum?
            int x = (int) event.getX();
            int y = (int) event.getY();
            String buttonName = "";

            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    Log.i("TAG", buttonName + "touched down");
                    break;
                case MotionEvent.ACTION_MOVE:
                    Log.i("TAG", buttonName + "moving: (" + x + ", " + y + ")");
                    redrawJoystick(x, y);
                    break;
                case MotionEvent.ACTION_UP:
                    Log.i("TAG", buttonName + "touched up");
                    redrawJoystick(-1, -1);
                    break;
            }

            return true;
        }
    };

    private View.OnTouchListener handleTouch_slider = new View.OnTouchListener() {

        @Override
        public boolean onTouch(View v, MotionEvent event) {

            v.performClick(); //TODO warnung falls nicht aber warum?
            int x = (int) event.getX();
            String buttonName = "";

            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    Log.i("TAG", buttonName + "touched down");
                    break;
                case MotionEvent.ACTION_MOVE:
                    Log.i("TAG", buttonName + "moving: (" + x + ", " + ")");
                    redrawSlider(x);
                    break;
                case MotionEvent.ACTION_UP:
                    Log.i("TAG", buttonName + "touched up");
                    redrawSlider(-1);
                    break;
            }

            return true;
        }
    };

    //#########################################################################
    //#########################################################################
    //Functions for ConnectTaskFragment

    static int i = 0;
    @Override
    public void onPreExecute(int id) {
        Log.i("tcp", "joystick activity called from fragment " + i);
        if((i+1)%3==0){
            Common.showAlertDialog(this, "blub", "nummer " + i);
        } else {
            Common.showToast(this, "nummer " + i);
        }
        i++;
    }

    @Override
    public void onProgressUpdate(final String type, final String message) {
        //progressDialog.dismiss();
        if(message == "-1"){
            showAlertDialog("connection lost", "kjhdaskjd");
        }
        Log.i("tcp", "joystick activity recieve: <" + message + ">");
        //editText_response.setText(message); //TODO gefahr von crash

        if(type.equals("error")){
            Log.i("tcp", "joystick activity - connection died");
            showAlertDialog("Connection lost", message);

        }

    }

    @Override
    public void onCancelled() {  }

    @Override
    public void onWarningReceived(String msg) {
        Toast toast = Toast.makeText(this, msg, Toast.LENGTH_LONG);
        toast.setGravity(Gravity.TOP,0,100);
        toast.show();
    }

    @Override
    public void onConnectionStatusChange(int status) {
        showSnackbar(status);
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
            Intent m = new Intent(this.getApplicationContext(), ConnectActivity.class);
            startActivity(m);
        } else if (id == R.id.nav_joystick) {
            //this activity - do nothing
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
