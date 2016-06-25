package io.github.elgambitero.microspot_android;


import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.hardware.Camera;
import android.os.Bundle;
import android.os.IBinder;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.Toast;


import java.util.List;

import io.github.elgambitero.microspot_android.SerialService.SerialBinder;


/**
 * Created by Jaime García Villena "garciavillena.jaime@gmail.com" on 6/21/16.
 */

public class ControlPanel extends AppCompatActivity implements View.OnClickListener{

    //Frame layout related
    Toolbar controlPanelToolbar;
    FrameLayout framePreview;
    CameraPreview preview;
    ImageView nocamview;
    Camera mCamera;

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
    SerialService serialService;

    //debugging variables
    private static final String TAG = "ControlPanel";


    /*==================
    * Activity lifecycle
      ==================*/


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_control_panel);
        isBound = false;
        Log.d(TAG, "Attempting to bind");
        Intent i = new Intent(this, SerialService.class);
        //startService(i);
        getApplicationContext().bindService(i, serialConnection, Context.BIND_AUTO_CREATE);
    }


    @Override
    protected void onResume() {
        super.onResume();

        initializeLayout();

        mCamera = initializeCamera();
        if(mCamera!=null){
            setCamFocusMode();
            preview = new CameraPreview(this,mCamera);
            framePreview.addView(preview);
        }else{

            nocamview.setImageResource(R.drawable.errorsign);
            framePreview.addView(nocamview);
        }


    }

    private void initializeLayout(){
        controlPanelToolbar = (Toolbar)findViewById(R.id.controlpaneltoolbar);
        framePreview = (FrameLayout)findViewById(R.id.camera_preview);
        nocamview = new ImageView(this);

        xPlusButton = (Button)findViewById(R.id.X_plus);
        yPlusButton = (Button)findViewById(R.id.Y_plus);
        xMinusButton = (Button)findViewById(R.id.X_minus);
        yMinusButton = (Button)findViewById(R.id.Y_minus);
        startSerialButton = (Button)findViewById(R.id.start_serial);
        homeAxisButton = (Button)findViewById(R.id.home_axis);
        stopSerialButton = (Button)findViewById(R.id.stop_serial);

        xPlusButton.setOnClickListener(this);
        yPlusButton.setOnClickListener(this);
        xMinusButton.setOnClickListener(this);
        yMinusButton.setOnClickListener(this);
        startSerialButton.setOnClickListener(this);
        homeAxisButton.setOnClickListener(this);
        stopSerialButton.setOnClickListener(this);

        setSupportActionBar(controlPanelToolbar);
    }

    @Override
    protected void onDestroy() {
        unbindService(serialConnection);
        super.onDestroy();

    }

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

    /** Check if this device has a camera */
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

    /* Set Auto focus */
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




    @Override
    public void onClick(View v) {
        switch(v.getId()){
            default:
                Snackbar.make(v, "NOT IMPLEMENTED YET.",
                        Snackbar.LENGTH_LONG).setAction("Action", null).show();
                break;
            case R.id.start_serial:
                if(!isBound) {
                    try {
                        serialService.initializeSerial();
                    } catch (Exception e) {
                        e.printStackTrace();
                        Toast.makeText(getApplicationContext(), "Couldn't connect to MicroSpot", Toast.LENGTH_LONG).show();
                    }
                }
                break;
            case R.id.home_axis:
                serialService.homeAxis();
                break;
            case R.id.stop_serial:
                try {
                    serialService.close();
                    Toast.makeText(getApplicationContext(), "MicroSpot disconnected", Toast.LENGTH_LONG).show();
                }catch (Exception e){
                    e.printStackTrace();
                    Toast.makeText(getApplicationContext(), "MicroSpot is already disconnected", Toast.LENGTH_LONG).show();
                }
                break;
            case R.id.Y_minus:
                serialService.moveAxis("Y",-10.0,2000.0);
                break;
            case R.id.Y_plus:
                serialService.moveAxis("Y",10.0,2000.0);
                break;
            case R.id.X_minus:
                serialService.moveAxis("X",-10.0,2000.0);
                break;
            case R.id.X_plus:
                serialService.moveAxis("X",10.0,2000.0);
            break;
        }
    }


    /*===============================
    * Service connection declarations
      ===============================*/

    private ServiceConnection serialConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName name, IBinder service){
            SerialBinder binder = (SerialBinder) service;
            serialService = binder.getService();
            Log.d(TAG, "Attempted to bind.");
            if(serialService != null) {
                isBound = true;
                Log.d(TAG, "Service is bonded successfully!");
            }else{
                Log.d(TAG, "Service bounding error");
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName name){
            isBound = false;
        }
    };

}

