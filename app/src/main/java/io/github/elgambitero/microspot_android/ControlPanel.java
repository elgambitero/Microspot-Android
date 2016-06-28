package io.github.elgambitero.microspot_android;


import android.Manifest;
import android.annotation.TargetApi;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.graphics.ImageFormat;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CameraMetadata;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.CaptureResult;
import android.hardware.camera2.TotalCaptureResult;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.ImageReader;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.util.Size;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;


import java.io.File;
import java.util.Arrays;
import java.util.List;

import io.github.elgambitero.microspot_android.SerialService.SerialBinder;


/**
 * Created by Jaime García Villena "garciavillena.jaime@gmail.com" on 6/21/16.
 */

public class ControlPanel extends AppCompatActivity implements View.OnClickListener, TextureView.SurfaceTextureListener {

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
    SerialService serialService;

    //Camera related variables
    private CameraDevice mCamera;
    private CameraCaptureSession mSession;
    private TextureView mPreviewView;
    private Surface mRawCaptureSurface, mJpegCaptureSurface, mPreviewSurface;
    private CaptureResult mPendingResult;
    private Size mPreviewSize;
    private File mPhotoDir;
    private CameraCharacteristics mCharacteristics;
    private int mCaptureImageFormat;

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
        mPreviewView.setSurfaceTextureListener(this);

    }


    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            default:
                Snackbar.make(v, "NOT IMPLEMENTED YET.",
                        Snackbar.LENGTH_LONG).setAction("Action", null).show();
                break;
            case R.id.start_serial:
                if (!isBound) {
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
                } catch (Exception e) {
                    e.printStackTrace();
                    Toast.makeText(getApplicationContext(), "MicroSpot is already disconnected", Toast.LENGTH_LONG).show();
                }
                break;
            case R.id.Y_minus:
                serialService.moveAxis("Y", -10.0, 2000.0);
                break;
            case R.id.Y_plus:
                serialService.moveAxis("Y", 10.0, 2000.0);
                break;
            case R.id.X_minus:
                serialService.moveAxis("X", -10.0, 2000.0);
                break;
            case R.id.X_plus:
                serialService.moveAxis("X", 10.0, 2000.0);
                break;
        }
    }

    /*======================
    *
    * Camera setting methods
    *
    ======================*/

    @TargetApi(Build.VERSION_CODES.M)
    private void initCamera(SurfaceTexture surface) throws CameraAccessException {
        CameraManager cm = (CameraManager) getSystemService(CAMERA_SERVICE);

        // get ID of rear-facing camera
        String[] cameraIds = cm.getCameraIdList();
        String cameraId = null;
        CameraCharacteristics cc = null;
        for (String id : cameraIds) {
            cc = cm.getCameraCharacteristics(id);
            if (cc.get(CameraCharacteristics.LENS_FACING) == CameraCharacteristics.LENS_FACING_BACK) {
                cameraId = id;
                break;
            }
        }
        if (cameraId == null) {
            throw new CameraAccessException(CameraAccessException.CAMERA_ERROR, "Couldn't find suitable camera");
        }

        mCharacteristics = cc;
        StreamConfigurationMap streamConfigs = cc.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);

        // determine supported output formats..
        boolean supportsRaw = false, supportsJpeg = false;
        for (int format : streamConfigs.getOutputFormats()) {
            if (format == ImageFormat.RAW_SENSOR) {
                supportsRaw = true;
            } else if (format == ImageFormat.JPEG) {
                supportsJpeg = true;
            }
        }
        if (supportsRaw) {
            mCaptureImageFormat = ImageFormat.RAW_SENSOR;
        } else if (supportsJpeg) {
            mCaptureImageFormat = ImageFormat.JPEG;
        } else {
            throw new CameraAccessException(CameraAccessException.CAMERA_ERROR, "Couldn't find supported image format");
        }

        // alternatively, make a way for the user to select a capture size..
        Size[] rawSizes = streamConfigs.getOutputSizes(ImageFormat.RAW_SENSOR);
        Size[] jpegSizes = streamConfigs.getOutputSizes(ImageFormat.JPEG);

        Size jpegSize = null;
        Size rawSize = null;

        if(rawSizes == null){
            Log.e(TAG,"NO RAW SIZES FOUND");

        }else{
            rawSize = rawSizes[0];
        }
        if(jpegSizes == null){
            Log.e(TAG,"NO JPEG SIZES FOUND");
        }else{
            jpegSize = jpegSizes[0];
        }

        // find the preview size that best matches the aspect ratio of the camera sensor..
        Size[] previewSizes = streamConfigs.getOutputSizes(SurfaceTexture.class);
        mPreviewSize = findOptimalPreviewSize(previewSizes, jpegSize);
        if (mPreviewSize == null) {
            return;
        }

        // set up capture surfaces and image readers..
        mPreviewSurface = new Surface(surface);
        ImageReader rawReader = ImageReader.newInstance(jpegSize.getWidth(), jpegSize.getHeight(),
                ImageFormat.RAW_SENSOR, 1);
        rawReader.setOnImageAvailableListener(new ImageReader.OnImageAvailableListener() {
            @Override
            public void onImageAvailable(ImageReader reader) {
                //new SaveRawTask(ControlPanel.this, mPhotoDir, reader.acquireLatestImage(),
                //mCharacteristics, mPendingResult).execute();
            }
        }, null);
        /*
        mRawCaptureSurface = rawReader.getSurface();
        ImageReader jpegReader = ImageReader.newInstance(jpegSize.getWidth(), jpegSize.getHeight(),
                ImageFormat.JPEG, 1);
        jpegReader.setOnImageAvailableListener(new ImageReader.OnImageAvailableListener() {
            @Override
            public void onImageAvailable(ImageReader reader) {
                //new SaveJpegTask(ControlPanel.this, mPhotoDir, reader.acquireLatestImage()).execute();
            }
        }, null);
        mJpegCaptureSurface = jpegReader.getSurface();
        */

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            Log.e(TAG,"No permissions granted for camera");
            return;
        }else {

            cm.openCamera(cameraId, new CameraDevice.StateCallback() {
                @Override
                public void onOpened(@NonNull CameraDevice camera) {
                    mCamera = camera;
                    initPreview();
                }

                @Override
                public void onDisconnected(@NonNull CameraDevice camera) {

                }

                @Override
                public void onError(@NonNull CameraDevice camera, int error) {

                }
            }, null);
        }
    }

    private void initPreview() {
        // scale preview size to fill screen width
        int screenWidth = getResources().getDisplayMetrics().widthPixels;
        float previewRatio = mPreviewSize.getWidth() / ((float) mPreviewSize.getHeight());
        int previewHeight = Math.round(screenWidth * previewRatio);
        ViewGroup.LayoutParams params = mPreviewView.getLayoutParams();
        params.width = screenWidth;
        params.height = previewHeight;

        List<Surface> surfaces = Arrays.asList(mPreviewSurface, mRawCaptureSurface, mJpegCaptureSurface);
        try {
            mCamera.createCaptureSession(surfaces, new CameraCaptureSession.StateCallback() {
                @Override
                public void onConfigured(CameraCaptureSession session) {
                    mSession = session;
                    updatePreview();
                }

                @Override
                public void onConfigureFailed(CameraCaptureSession session) {

                }
            }, null);
        } catch (CameraAccessException e) {
            Log.d(TAG, "Failed to create camera capture session", e);
        }
    }

    /**
     * Call this whenever some camera control changes (e.g., focus distance, white balance, etc) that should affect the preview
     */
    private void updatePreview() {
        try {
            if (mCamera == null || mSession == null) {
                return;
            }
            CaptureRequest.Builder builder = mCamera.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
            builder.addTarget(mPreviewSurface);

            builder.set(CaptureRequest.CONTROL_AF_MODE, CameraMetadata.CONTROL_AF_MODE_OFF);

//            builder.set(CaptureRequest.LENS_FOCUS_DISTANCE, ...)
//            builder.set(CaptureRequest.SENSOR_SENSITIVITY, ...)
//            builder.set(CaptureRequest.CONTROL_AWB_MODE, ...)
//            builder.set(CaptureRequest.CONTROL_EFFECT_MODE, ...)
//            builder.set(CaptureRequest.CONTROL_AE_EXPOSURE_COMPENSATION, ...)
//            etc...

            mSession.setRepeatingRequest(builder.build(), new CameraCaptureSession.CaptureCallback() {
                @Override
                public void onCaptureCompleted(CameraCaptureSession session, CaptureRequest request, TotalCaptureResult result) {
                    // if desired, we can get updated auto focus & auto exposure values here from 'result'
                }
            }, null);
        } catch (CameraAccessException e) {
            Log.e(TAG, "Failed to start preview");
        }
    }

    private Size findOptimalPreviewSize(Size[] sizes, Size targetSize) {
        float targetRatio = targetSize.getWidth() * 1.0f / targetSize.getHeight();
        float tolerance = 0.1f;
        int screenWidth = getResources().getDisplayMetrics().widthPixels;
        int maxPixels = screenWidth * Math.round(screenWidth * targetRatio);
        int width, height;
        float ratio;
        for (Size size : sizes) {
            width = size.getWidth();
            height = size.getHeight();
            if (width * height <= maxPixels) {
                ratio = ((float) width) / height;
                if (Math.abs(ratio - targetRatio) < tolerance) {
                    return size;
                }
            }
        }
        return null;
    }



    /*===============================
    *
    * Service connection declarations
    *
    ===============================*/

    private ServiceConnection serialConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName name, IBinder service){
            SerialBinder binder = (SerialBinder) service;
            serialService = binder.getService();
            Log.d(TAG, "Attempted to bind.");
            if(serialService != null) {
                Log.d(TAG, "Service is bound successfully!");
                try {
                    serialService.initializeSerial();
                }catch (Exception e){
                    e.printStackTrace();
                }
            }else{
                Log.d(TAG, "Service binding error");
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName name){
            isBound = false;
        }
    };



    /*==============================
    *
    * SurfaceTextureListener methods
    *
    ==============================*/


    @Override
    public void onSurfaceTextureAvailable(SurfaceTexture surfaceTexture, int i, int i1) {
        try{
            initCamera(surfaceTexture);
        }catch (CameraAccessException e){
            Log.e(TAG,"Failed to open camera");
        }
    }

    @Override
    public void onSurfaceTextureSizeChanged(SurfaceTexture surfaceTexture, int i, int i1) {

    }

    @Override
    public boolean onSurfaceTextureDestroyed(SurfaceTexture surfaceTexture) {
        if(mCamera != null){
            mCamera.close();
            mCamera = null;
        }
        mSession = null;
        return false;
    }

    @Override
    public void onSurfaceTextureUpdated(SurfaceTexture surfaceTexture) {

    }
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

