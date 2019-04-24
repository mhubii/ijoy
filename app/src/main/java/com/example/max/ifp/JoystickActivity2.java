package com.example.max.ifp;

//TODO check min,maxTilt, center slider on release

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Point;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.content.ContextCompat;
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
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import java.text.DecimalFormat;

public class JoystickActivity2 extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener, ConnectTaskFragment.TaskCallbacks {

    //#########################################################################
    //Class variables
    SharedPreferences pref;
    private ConnectTaskFragment mTaskFragment;

    private double mX;
    private double mY;
    private double mR;

    private float minTilt;
    private float maxTilt;

    private int mWidth;
    private int mHeight;

    private int mBorderSize = 0;
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

    private ImageView imageView_joystickLeft;
    private ImageView imageView_joystickRight;
    private ImageView imageView_slider;
    private Button button_sensors;

    private Snackbar snackbarStatus;

    private SensorManager sManager;

    private float gravity[]; //Gravity rotational data
    private float magnetic[]; //for magnetic rotational data
    private float accels[] = new float[3];
    private float mags[] = new float[3];
    private float[] values = new float[3];

    // azimuth, pitch and roll
    private float azimuth;
    private float pitch;
    private float roll;

    private boolean useSensors = false;

    //#########################################################################

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_joystick2);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.addDrawerListener(toggle);
        toggle.syncState();

        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);

        mTaskFragment = ConnectActivity.getInstance();
        mTaskFragment.attach(this);

        findInitUiElements();
    }

    //#########################################################################
    //#########################################################################
    //Functions for UI interaction

    private void findInitUiElements(){
        //find
        imageView_joystickLeft = (ImageView)findViewById(R.id.imageView_joystickLeft);
        imageView_joystickRight = (ImageView)findViewById(R.id.imageView_joystickRight);
        imageView_slider = (ImageView)findViewById(R.id.imageView_slider);
        button_sensors = (Button)findViewById(R.id.button_sensors);
        button_sensors.setLayoutParams(new TableRow.LayoutParams(mBmpSize, TableRow.LayoutParams.WRAP_CONTENT));

        //init

        pref = getApplicationContext().getSharedPreferences("MyPref", 0); // 0 - for private mode
        minTilt = pref.getFloat("minTilt", 15f)/100;
        maxTilt = pref.getFloat("maxTilt", 60f)/100;

        sManager = (SensorManager) this.getSystemService(SENSOR_SERVICE);
        sManager.registerListener(mySensorEventListener, sManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER),SensorManager.SENSOR_DELAY_NORMAL);
        sManager.registerListener(mySensorEventListener, sManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD),SensorManager.SENSOR_DELAY_NORMAL);

        getDimensions();
        initUI();
        showSnackbar(mTaskFragment.getConnectionStatus());
    }

    private void getDimensions(){
        Display display = getWindowManager().getDefaultDisplay();
        Point size = new Point();
        display.getSize(size);
        mWidth = size.x-100;
        mHeight = size.y-100;
        //20dp from edge of screen
        mBmpSize = 650;
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
        Bitmap bmp3;
        Canvas canvas3;

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
        bmp2 = Bitmap.createBitmap(mBmpSize, mBmpSize, Bitmap.Config.ARGB_8888);
        bmp3 = Bitmap.createBitmap(mBmpSize, mButtonSize*2+mBorderSize*2, Bitmap.Config.ARGB_8888);

        canvas = new Canvas(bmp);
        canvas.drawRect(0,0,bmp.getHeight(), bmp.getWidth(), paintWhite);
        canvas.drawCircle(bmp.getWidth()/2, bmp.getHeight()/2, mButtonSize, paintRed);
        canvas.drawRect(mRectBorder, mRectBorder, mRectBorder+mRectSize, mRectBorder+mRectSize, paintBlack);
        canvas.drawCircle(bmp.getWidth()/2, bmp.getHeight()/2, mCircleSize, paintBlack);
        canvas.drawLine(bmp.getWidth()/2, mRectBorder, bmp.getWidth()/2, bmp.getHeight()-mRectBorder, paintBlack);

        canvas2 = new Canvas(bmp2);
        canvas2.drawRect(0,0,bmp.getHeight(), bmp.getWidth(), paintWhite);
        canvas2.drawCircle(bmp.getWidth()/2, bmp.getHeight()/2, mButtonSize, paintRed);
        canvas2.drawRect(mRectBorder, mRectBorder, mRectBorder+mRectSize, mRectBorder+mRectSize, paintBlack);
        canvas2.drawCircle(bmp.getWidth()/2, bmp.getHeight()/2, mCircleSize, paintBlack);
        canvas2.drawLine(mRectBorder, bmp.getWidth()/2, bmp.getWidth()-mRectBorder, bmp.getHeight()/2, paintBlack);

        canvas3 = new Canvas(bmp3);
        canvas3.drawRect(0,0,bmp3.getWidth(), bmp3.getHeight(), paintWhite);
        canvas3.drawCircle(bmp3.getWidth()/2, bmp3.getHeight()/2, mButtonSize, paintRed);
        canvas3.drawRect(mRectBorder, bmp3.getHeight(), mRectBorder+mRectSize, bmp3.getHeight()/2, paintBlack);

        imageView_joystickLeft.setOnTouchListener(handleTouch_joystickLeft);
        imageView_joystickRight.setOnTouchListener(handleTouch_joystickRight);
        imageView_slider.setOnTouchListener(handleTouch_slider);

        imageView_joystickLeft.setImageBitmap(bmp);
        imageView_joystickRight.setImageBitmap(bmp2);
        imageView_slider.setImageBitmap(bmp3);
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

    public void redrawJoystickLeft(int x, int y){
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

        //get distance from center
        //make sure "button" is inside or on outer rectangle
        x = bmp.getWidth()/2;

        if(y<mRectBorder){
            y = mRectBorder;
        } else if(y>mRectBorder+mRectSize){
            y = mRectBorder+mRectSize;
        }

        //#####################################
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

        //redraw joystick
        canvas.drawLine(bmp.getWidth()/2, mRectBorder, bmp.getWidth()/2, bmp.getHeight()-mRectBorder, paintBlack);

        canvas.drawCircle(x, y, mButtonSize, paintRed);
        canvas.drawCircle(x, y, 15, paintBlack);

        canvas.drawRect(mRectBorder, mRectBorder, mRectBorder+mRectSize, mRectBorder+mRectSize, paintBlack);
        canvas.drawCircle(bmp.getWidth()/2, bmp.getHeight()/2, mCircleSize, paintBlack);

        imageView_joystickLeft.setImageBitmap(bmp);
        imageView_joystickLeft.invalidate();
        sendVector();
    }

    public void redrawJoystickRight(int x, int y){
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

        //get distance from center
        //make sure "button" is inside or on outer rectangle
        if(x<mRectBorder){
            x = mRectBorder;
        } else if(x>mRectBorder+mRectSize){
            x = mRectBorder+mRectSize;
        }

        y = bmp.getHeight()/2;

        //#####################################
        //maximal distance from center
        int maxDist = mBmpSize/2-mRectBorder;
        //set y
        //left half - rotate left
        if(x < mBmpSize/2){
            yVector = -1* maxY * ((mBmpSize/2-x)/(float)maxDist);
            //right half - rotate right
        } else {
            yVector = (maxY * ((x-mBmpSize/2)/(float)maxDist));
        }

        //redraw joystick
        canvas.drawLine(mRectBorder, bmp.getWidth()/2, bmp.getWidth()-mRectBorder, bmp.getHeight()/2, paintBlack);

        canvas.drawCircle(x, y, mButtonSize, paintRed);
        canvas.drawCircle(x, y, 15, paintBlack);

        canvas.drawRect(mRectBorder, mRectBorder, mRectBorder+mRectSize, mRectBorder+mRectSize, paintBlack);
        canvas.drawCircle(bmp.getWidth()/2, bmp.getHeight()/2, mCircleSize, paintBlack);

        imageView_joystickRight.setImageBitmap(bmp);
        imageView_joystickRight.invalidate();
        sendVector();
    }

    public void redrawSlider(int x){
        //TODO
        /*
        wenn beruehrt muss -mButtonSize/2 von x position abzeogen werden
        aber wenn losgelassen wurde dann nicht
         */
        //final TextView textView3 = (TextView)findViewById(R.id.editText3);

        Bitmap bmp3;
        Canvas canvas3;

        Paint paintRed;
        Paint paintBlack;
        Paint paintWhite;

        paintRed = new Paint();
        paintRed.setColor(Color.RED);
        paintRed.setStyle(Paint.Style.FILL);

        //TODO remove?
        if(useSensors && x != (mBmpSize/2+mBorderSize+mButtonSize/2)){
            paintRed.setColor(Color.CYAN);
            paintRed.setStyle(Paint.Style.FILL);
        }

        paintBlack = new Paint();
        paintBlack.setColor(Color.BLACK);
        paintBlack.setStyle(Paint.Style.STROKE);

        paintWhite = new Paint();
        paintWhite.setColor(Color.LTGRAY);
        paintWhite.setStyle(Paint.Style.FILL);

        bmp3 = Bitmap.createBitmap(mBmpSize, mButtonSize*2+mBorderSize*2, Bitmap.Config.ARGB_8888);
        canvas3 = new Canvas(bmp3);

        canvas3.drawRect(0,0,bmp3.getWidth(), bmp3.getHeight(), paintWhite);
        canvas3.drawRect(mRectBorder, bmp3.getHeight()/2, mRectBorder+mRectSize, bmp3.getHeight()/2, paintBlack);

        if(x == -1){
            x = bmp3.getWidth()/2;
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
            aVector = -1* maxY * (float)((mBmpSize/2-x)/(float)maxDist);
            //right half - rotate right
        } else {
            aVector = (maxY * (float)((x-mBmpSize/2)/(float)maxDist));
        }

        DecimalFormat df = new DecimalFormat("#.##");
        String vector = "(" + df.format(xVector) + ", " + df.format(yVector) + ", " + df.format(aVector) + ")";
        //textView2.setText(vector);

        canvas3.drawCircle(x, bmp3.getHeight()/2, mButtonSize, paintRed);
        canvas3.drawCircle(x, bmp3.getHeight()/2, 15, paintBlack);

        //canvas.drawRect(mRectBorder, mRectBorder, mRectBorder+mRectSize, mRectBorder+mRectSize, paintBlack);
        //canvas.drawCircle(bmp.getWidth()/2, bmp.getHeight()/2, mCircleSize, paintBlack);

        imageView_slider.setImageBitmap(bmp3);
        imageView_slider.invalidate();
        sendVector();
    }

    private View.OnTouchListener handleTouch_joystickLeft = new View.OnTouchListener() {

        @Override
        public boolean onTouch(View v, MotionEvent event) {

            int x = (int) event.getX();
            int y = (int) event.getY();
            String buttonName = "";

            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    Log.i("TAG", buttonName + "touched down");
                    break;
                case MotionEvent.ACTION_MOVE:
                    Log.i("TAG", buttonName + "moving: (" + x + ", " + y + ")");
                    redrawJoystickLeft(x, y);
                    break;
                case MotionEvent.ACTION_UP:
                    Log.i("TAG", buttonName + "touched up");
                    redrawJoystickLeft(-1, -1);
                    break;
            }

            return true;
        }
    };

    private View.OnTouchListener handleTouch_joystickRight = new View.OnTouchListener() {

        @Override
        public boolean onTouch(View v, MotionEvent event) {

            int x = (int) event.getX();
            int y = (int) event.getY();
            String buttonName = "";

            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    Log.i("TAG", buttonName + "touched down");
                    break;
                case MotionEvent.ACTION_MOVE:
                    Log.i("TAG", buttonName + "moving: (" + x + ", " + y + ")");
                    redrawJoystickRight(x, y);
                    break;
                case MotionEvent.ACTION_UP:
                    Log.i("TAG", buttonName + "touched up");
                    redrawJoystickRight(-1, -1);
                    break;
            }

            return true;
        }
    };

    private View.OnTouchListener handleTouch_slider = new View.OnTouchListener() {

        @Override
        public boolean onTouch(View v, MotionEvent event) {

            if(useSensors){
                return true;
            }

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
    //Functions for Sensor events

    private SensorEventListener mySensorEventListener = new SensorEventListener() {
        public void onAccuracyChanged(Sensor sensor, int accuracy) {
        }

        public void onSensorChanged(SensorEvent event) {
            switch (event.sensor.getType()) {
                case Sensor.TYPE_MAGNETIC_FIELD:
                    mags = event.values.clone();
                    break;
                case Sensor.TYPE_ACCELEROMETER:
                    accels = event.values.clone();
                    break;
            }

            if(!useSensors){
                return;
            }

            if (mags != null && accels != null) {
                gravity = new float[9];
                magnetic = new float[9];
                SensorManager.getRotationMatrix(gravity, magnetic, accels, mags);
                float[] outGravity = new float[9];
                SensorManager.remapCoordinateSystem(gravity, SensorManager.AXIS_X,SensorManager.AXIS_Z, outGravity);
                SensorManager.getOrientation(outGravity, values);

                azimuth = values[0] * 57.2957795f;
                pitch =values[1] * 57.2957795f;
                roll = values[2] * 57.2957795f;
                mags = null;
                accels = null;

                //adapt angle if window is in reverse landcape mode
                int rotation =  getWindowManager().getDefaultDisplay().getRotation();
                if(rotation == 1){
                    roll += 180;
                }


                float x = 0;

                //get how far device is tilted to which side
                if(roll >= 0 && roll < 90) {
                    //left
                    x =  -1 * (1 - (roll / 90));
                } else if (roll < 0 && roll >= -90){
                    x = -1;
                } else if (roll >= 90 && roll < 180){
                    //right
                    x = (1*(roll-90)/90);
                } else {
                    x = 1;
                }

                //get value from tilt
                int diffDist = mBmpSize/2;
                float diffPercentage = 0;
                if(Math.abs(x) < minTilt){
                    //middle position
                    //redrawSlider(diffDist+mBorderSize+mButtonSize/2);
                    redrawSlider(mBmpSize/2);
                } else if(Math.abs(x) < maxTilt){
                    diffPercentage = Math.signum(x) * (Math.abs(x)- minTilt)/(maxTilt - minTilt);
                    redrawSlider(diffDist + mBorderSize + mButtonSize/2 + (int)(diffPercentage*diffDist));
                } else {
                    //maximum position
                    if(roll < 90){
                        //maximum left
                        redrawSlider( mBorderSize + mButtonSize/2);
                    } else {
                        //maximum right
                        redrawSlider(mBmpSize -  mBorderSize - mButtonSize/2);
                    }
                }
            }
        }
    };

    public void onSensorButtonClicked(View view){
        useSensors = !useSensors; //toggle state
        if(useSensors){
            button_sensors.setText(R.string.button_sensor_active);
        } else {
            button_sensors.setText(R.string.button_sensor_inactive);
            redrawSlider(-1);
        }
    }

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
            //showAlertDialog("connection lost", "kjhdaskjd");
        }
        Log.i("tcp", "joystick activity2 recieve: <" + message + ">");
        //editText_response.setText(message); //TODO gefahr von crash

        if(type.equals("error")){
            Log.i("tcp", "joystick activity2 - connection died");
            //showAlertDialog("Connection lost", message);

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
        getMenuInflater().inflate(R.menu.joystick_activity2, menu);
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

    @SuppressWarnings("StatementWithEmptyBody")
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

        } else if (id == R.id.nav_settings) {
            Intent m = new Intent(this.getApplicationContext(), SettingsActivity.class);
            startActivity(m);
        }

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }
}
