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
import android.widget.Toast;


import Services.SerialService;
import Services.SerialService.SerialBinder;


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
    Button xPlusButton;
    Button yPlusButton;
    Button xMinusButton;
    Button yMinusButton;
    Button startSerialButton;
    Button homeAxisButton;
    Button stopSerialButton;

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

        xPlusButton.setOnClickListener(this);
        yPlusButton.setOnClickListener(this);
        xMinusButton.setOnClickListener(this);
        yMinusButton.setOnClickListener(this);
        startSerialButton.setOnClickListener(this);
        homeAxisButton.setOnClickListener(this);
        stopSerialButton.setOnClickListener(this);


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


/*
    private Camera initializeCamera(){
        if(checkCameraHardware(this)){
            Camera cam;
            int cameraId = -1;
            int numberOfCameras = Camera.getNumberOfCameras();
            for (int i = 0; i < numberOfCameras; i++) {
                Camera.CameraInfo info = new Camera.CameraInfo();
                Camera.getCameraInfo(i, info);
                if (info.facing == Camera.CameraInfo.CAMERA_FACING_BACK) {
                    cameraId = i;
                    break;
                }
            }
            cam = getCameraInstance(cameraId);
            if (cam == null){
                Log.println(1, "Err", "NO CAMERA INSTANCED");
            }
            return cam;
        }else{
            return null;
        }

    }


    private boolean checkCameraHardware(Context context) {
        if (context.getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA)){
            // this device has a camera
            return true;
        } else {
            // no camera on this device
            return false;
        }
    }

    public static Camera getCameraInstance(int i){
        Camera c = null;
        try {
            c = Camera.open();
            // attempt to get a Camera instance
        }
        catch (Exception e){
            // Camera is not available (in use or does not exist)
        }
        return c; // returns null if camera is unavailable
    }

    private void setCamFocusMode(){

        if(null == mCamera) {
            return;
        }


        Camera.Parameters parameters = mCamera.getParameters();
        List<String> focusModes = parameters.getSupportedFocusModes();
        if(focusModes.contains(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE)){
            parameters.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE);
        } else
        if(focusModes.contains(Camera.Parameters.FOCUS_MODE_AUTO)){
            parameters.setFocusMode(Camera.Parameters.FOCUS_MODE_AUTO);
        }

        mCamera.setParameters(parameters);
    }
*/

