package io.github.elgambitero.microspot_android;

import android.content.Context;
import android.content.pm.PackageManager;
import android.hardware.Camera;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;

import java.util.List;

/**
 * Created by Jaime García Villena "garciavillena.jaime@gmail.com" on 6/21/16.
 */

public class ControlPanel extends AppCompatActivity{

    Toolbar calibToolbar;
    FrameLayout framePreview;
    CameraPreview preview;
    ImageView nocamview;
    Camera mCamera;

    Button xPlusButton;
    Button yPlusButton;
    Button xMinusButton;
    Button yMinusButton;
    Button startSerialButton;
    Button homeAxisButton;
    Button stopSerialButton;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_control_panel);
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
        calibToolbar = (Toolbar)findViewById(R.id.calibtoolbar);
        framePreview = (FrameLayout)findViewById(R.id.camera_preview);
        nocamview = new ImageView(this);

        xPlusButton = (Button)findViewById(R.id.X_plus);
        yPlusButton = (Button)findViewById(R.id.Y_plus);
        xMinusButton = (Button)findViewById(R.id.X_minus);
        yMinusButton = (Button)findViewById(R.id.Y_minus);
        startSerialButton = (Button)findViewById(R.id.start_serial);
        homeAxisButton = (Button)findViewById(R.id.home_axis);
        stopSerialButton = (Button)findViewById(R.id.stop_serial);
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


}

