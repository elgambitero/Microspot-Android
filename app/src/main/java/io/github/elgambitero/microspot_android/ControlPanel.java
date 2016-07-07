package io.github.elgambitero.microspot_android;


import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.os.IBinder;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.TextureView;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;


import io.github.elgambitero.microspot_android.SerialService.SerialBinder;


/**
 * Created by Jaime García Villena "garciavillena.jaime@gmail.com" on 6/21/16.
 */

public class ControlPanel extends AppCompatActivity implements View.OnClickListener{

    /*====================
    *
    * Variable declaration
    *
    ====================*/

    //Frame layout related
    Toolbar controlPanelToolbar;
    ImageView nocamview;

    //Buttons
    Button xPlusButton, yPlusButton, xMinusButton, yMinusButton, startSerialButton, homeAxisButton,
            stopSerialButton, resetButton;

    //Service binding variables
    Boolean isBound;
    SerialBinder serialBinder;

    //Camera preview variables
    TextureView mPreviewView;
    Camera2Preview camera2Preview;

    //debugging variables
    private static final String TAG = "ControlPanel";


    /*==================
    *
    * Activity lifecycle
    *
    ==================*/


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_control_panel);

        //Serial service cautions
        isBound = false;
        Log.d(TAG, "Attempting to bind");
        Intent i = new Intent(this, SerialService.class);
        isBound = getApplicationContext().bindService(i, serialConnection, Context.BIND_AUTO_CREATE);


    }


    @Override
    protected void onResume() {
        super.onResume();

        initializeLayout();


        camera2Preview = new Camera2Preview(this,mPreviewView);

    }

    /*======================
    *
    * Layout setting methods
    *
    ======================*/

    private void initializeLayout() {


        //Orientation options
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        //Control panel bar
        controlPanelToolbar = (Toolbar) findViewById(R.id.controlpaneltoolbar);
        setSupportActionBar(controlPanelToolbar);

        //Button assign
        xPlusButton = (Button) findViewById(R.id.X_plus);
        yPlusButton = (Button) findViewById(R.id.Y_plus);
        xMinusButton = (Button) findViewById(R.id.X_minus);
        yMinusButton = (Button) findViewById(R.id.Y_minus);
        startSerialButton = (Button) findViewById(R.id.start_serial);
        homeAxisButton = (Button) findViewById(R.id.home_axis);
        stopSerialButton = (Button) findViewById(R.id.stop_serial);
        resetButton = (Button) findViewById(R.id.reset_button);

        xPlusButton.setOnClickListener(this);
        yPlusButton.setOnClickListener(this);
        xMinusButton.setOnClickListener(this);
        yMinusButton.setOnClickListener(this);
        startSerialButton.setOnClickListener(this);
        homeAxisButton.setOnClickListener(this);
        stopSerialButton.setOnClickListener(this);
        resetButton.setOnClickListener(this);


        //Camera preview

        mPreviewView = (TextureView) findViewById(R.id.camera_preview_control_panel);
        nocamview = new ImageView(this);


    }


    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            default:
                Snackbar.make(v, "NOT IMPLEMENTED YET.",
                        Snackbar.LENGTH_LONG).setAction("Action", null).show();
                break;
            case R.id.start_serial:
                serialBinder.start();
                break;
            case R.id.home_axis:
                serialBinder.homeAxis();
                break;
            case R.id.stop_serial:
                serialBinder.close();
                break;
            case R.id.Y_minus:
                serialBinder.moveAxisRel(0.0,-10.0,2000.0);
                break;
            case R.id.Y_plus:
                serialBinder.moveAxisRel(0.0,10.0,2000.0);
                break;
            case R.id.X_minus:
                serialBinder.moveAxisRel(-10.0,0.0,2000.0);
                break;
            case R.id.X_plus:
                serialBinder.moveAxisRel(10.0,0.0,2000.0);
                break;
            case R.id.reset_button:
                serialBinder.reset();
        }
    }


    /*===============================
    *
    * Service connection declarations
    *
    ===============================*/

    private ServiceConnection serialConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName name, IBinder service){
            serialBinder = (SerialBinder) service;
            Log.d(TAG, "Attempted to bind.");
            if(serialBinder != null) {
                Log.d(TAG, "Service is bound successfully!");
            }else{
                Log.d(TAG, "Service binding error");
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName name){
            isBound = false;
        }
    };

}


