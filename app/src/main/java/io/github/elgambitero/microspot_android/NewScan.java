package io.github.elgambitero.microspot_android;


import android.Manifest;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.graphics.ImageFormat;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CameraMetadata;
import android.hardware.camera2.CaptureFailure;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.CaptureResult;
import android.hardware.camera2.DngCreator;
import android.hardware.camera2.TotalCaptureResult;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.Image;
import android.media.ImageReader;
import android.media.MediaScannerConnection;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.IBinder;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;

import android.util.Log;
import android.util.Size;
import android.view.Surface;
import android.view.TextureView;
import android.view.WindowManager;
import android.widget.Toast;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.lang.ref.WeakReference;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.List;

/**
 * Created by elgambitero on 30/12/15.
 */
public class NewScan extends AppCompatActivity implements PatientInput.PatientInputListener,
        ConfigScan.ConfigScanListener,
        CalibrateScan.CalibrateScanListener,
        Scanning.ScanningListener,
        TextureView.SurfaceTextureListener{

    /*=====================
    *
    * Variable declarations
    *
    =====================*/

    Toolbar toolbar;
    OutputStream out;

    private Double[] _intervals = new Double[2];
    private Integer[] _shots = new Integer[2];

    //Serial service binding variables
    Boolean isBound;
    SerialService serialService;
    private static final String TAG = "NewScan";
    String _patientId;

    //Camera handling variables
    private CameraDevice mCamera;
    private CameraCaptureSession mSession;
    private Surface mRawCaptureSurface, mJpegCaptureSurface, mPreviewSurface;
    private CaptureResult mPendingResult;
    private Size mPreviewSize;
    private CameraCharacteristics mCharacteristics;
    private int mCaptureImageFormat;

    TextureView mPreviewView;
    private String _nextPhotoName;


    /*==========================
    *
    * Activity lifecycle Methods
    *
    ==========================*/

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.newscan);
        try {
            initializeLayout();
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
        setSupportActionBar(toolbar);
        out = getTempFile(true);

        isBound = false;
        Log.d(TAG, "Attempting to bind");
        Intent i = new Intent(this, SerialService.class);
        isBound = getApplicationContext().bindService(i, serialConnection, Context.BIND_AUTO_CREATE);


    }

    @Override
    public void onPause() {
        super.onPause();
        finish();
    }

    /*=====================================
    *
    * Layout handling methods and variables
    *
    =====================================*/

    android.support.v4.app.FragmentManager fragmentManager = getSupportFragmentManager();


    private void initializeLayout() throws CameraAccessException {
        toolbar = (Toolbar) findViewById(R.id.newtoolbar);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
        getWindow().setStatusBarColor(getResources().getColor(R.color.colorPrimaryDark));

        goToStep(0);
    }

    private void goToStep(int step) throws CameraAccessException {
        android.support.v4.app.FragmentTransaction fragTran;
        fragTran = fragmentManager.beginTransaction();
        switch (step) {
            case 0:
                PatientInput step1 = new PatientInput();
                fragTran.replace(R.id.newScanSteps, step1);
                break;
            case 1:
                ConfigScan step2 = new ConfigScan();
                fragTran.replace(R.id.newScanSteps, step2);
                break;
            case 2:
                serialService.homeAxis();
                serialService.homeAxis();
                serialService.axisTo(25.0, 7.5, 2000.0);
                CalibrateScan step3 = new CalibrateScan();
                fragTran.replace(R.id.newScanSteps, step3);
                break;
            case 3:
                Scanning step4 = new Scanning();
                fragTran.replace(R.id.newScanSteps, step4);
        }
        fragTran.addToBackStack(null);
        fragTran.commit();
    }

    /*===================
    *
    File handling methods
    *
    ===================*/

    private OutputStream getTempFile(boolean makeNew) {
        if (makeNew) {
            deleteTempFile(this);
        }
        File tempFile = new File(getApplicationContext().getExternalFilesDir("/temp").getPath()
                + "info.txt");
        OutputStream out = null;
        try {
            out = new FileOutputStream(tempFile);
        } catch (Exception e) {
            e.printStackTrace();
        }

        return out;
    }

    public boolean deleteTempFile(Context context) { //more like resetTempFile
        File file = new File(
                String.valueOf(context.
                        getExternalFilesDir(String.valueOf(R.string.temp_info_file))));
        boolean deleted = file.delete();
        file = new File(
                String.valueOf(context.
                        getExternalFilesDir(String.valueOf(R.string.temp_info_file))));
        return deleted;
    }


    /*================================
    *
    Fragment interface implementations
    *
    ================================*/

    /*====================
    PatientInput interface
    ====================*/

    @Override
    public void writePatientDataAndNext(String id, String annotation) throws CameraAccessException {
        try {
            _patientId = id;
            out.write(("PatientId = " + id + "\r\n").getBytes());
            out.write(("annotations = " + annotation + "\r\n").getBytes());
        } catch (Exception e) {
            e.printStackTrace();
        }
        goToStep(1);
    }


    /*==================
    ConfigScan interface
    ==================*/

    @Override
    public void writeGridDataAndNext(Double intervalX, Double intervalY, Integer shotsX, Integer shotsY) throws CameraAccessException {
        _intervals[0] = intervalX;
        _intervals[1] = intervalY;
        _shots[0] = shotsX;
        _shots[1] = shotsY;
        try {
            out.write(("intervalX = " + intervalX.toString() + "\r\n").getBytes());
            out.write(("intervalY = " + intervalY.toString() + "\r\n").getBytes());
            out.write(("shotsX = " + shotsX.toString() + "\r\n").getBytes());
            out.write(("shotsY = " + shotsY.toString() + "\r\n").getBytes());
        } catch (Exception e) {
            e.printStackTrace();
        }
        try {
            out.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        goToStep(2);
    }


    /*=====================
    CalibrateScan interface
    =====================*/


    @Override
    public void setFocusAndNext() throws CameraAccessException {
        goToStep(3);
    }


    @Override
    public void setCalibCameraPreview(TextureView t) {
        mPreviewView = t;
        mPreviewView.setSurfaceTextureListener(this);
    }



    /*=================
    Scanning interfaces
    =================*/

    @Override
    public Double[] getXCoordinates() {
        Double[] xCoord;
        xCoord = new Double[_shots[0]];
        for (int i = 0; i < _shots[0]; i++) {
            xCoord[i] = 25.0 - (_intervals[0] * _shots[0] / 2) + i * _intervals[0];
        }
        return xCoord;
    }

    @Override
    public Double[] getYCoordinates() {
        Double[] yCoord;
        yCoord = new Double[_shots[1]];
        for (int i = 0; i < _shots[1]; i++) {
            yCoord[i] = 7.5 - (_intervals[1] * _shots[1] / 2) + i * _intervals[1];
        }
        return yCoord;
    }


    @Override
    public void moveAxisRel(Double xCoord, Double yCoord, Double speed) {
        serialService.moveAxisRel(xCoord, yCoord, speed);
    }

    @Override
    public String getPatientId() {
        return _patientId;
    }

    @Override
    public void setNextPhotoName(String name){
        _nextPhotoName = name;
        return;
    }

    @Override
    public void endScan() {
        deleteTempFile(this);

        /*
        android.support.v4.app.FragmentTransaction fragTran;
        fragTran = fragmentManager.beginTransaction();
        fragTran.remove(R.id.newScanSteps);
        fragTran.addToBackStack(null);
        fragTran.commit();
        */

        Intent i = new Intent(getPackageName() + ".MANAGESAMPLES");
        startActivity(i);
    }

    /*===============================
    *
    * Service connection declarations
    *
    ===============================*/

    private ServiceConnection serialConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            SerialService.SerialBinder binder = (SerialService.SerialBinder) service;
            serialService = binder.getService();
            Log.d(TAG, "Attempted to bind.");
            if (serialService != null) {
                Log.d(TAG, "Service is bound successfully!");
                try {
                    serialService.initializeSerial();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            } else {
                Log.d(TAG, "Service binding error");
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            isBound = false;
        }
    };


    /*=================================================
    Camera2 methods. Based on code snippet by @natevogt
    =================================================*/

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
        Size rawSize = streamConfigs.getOutputSizes(ImageFormat.RAW_SENSOR)[0];
        Size jpegSize = streamConfigs.getOutputSizes(ImageFormat.JPEG)[0];

        // find the preview size that best matches the aspect ratio of the camera sensor..
        Size[] previewSizes = streamConfigs.getOutputSizes(SurfaceTexture.class);
        mPreviewSize = findOptimalPreviewSize(previewSizes, rawSize);
        if (mPreviewSize == null) {
            return;
        }

        // set up capture surfaces and image readers..
        mPreviewSurface = new Surface(surface);
        ImageReader rawReader = ImageReader.newInstance(rawSize.getWidth(), rawSize.getHeight(),
                ImageFormat.RAW_SENSOR, 1);
        rawReader.setOnImageAvailableListener(new ImageReader.OnImageAvailableListener() {
            @Override
            public void onImageAvailable(ImageReader reader) {
                new SaveRawTask(NewScan.this, _nextPhotoName, reader.acquireLatestImage(),
                        mCharacteristics, mPendingResult).execute();
            }
        }, null);
        mRawCaptureSurface = rawReader.getSurface();
        ImageReader jpegReader = ImageReader.newInstance(jpegSize.getWidth(), jpegSize.getHeight(),
                ImageFormat.JPEG, 1);
        jpegReader.setOnImageAvailableListener(new ImageReader.OnImageAvailableListener() {
            @Override
            public void onImageAvailable(ImageReader reader) {
                new SaveJpegTask(NewScan.this, _nextPhotoName, reader.acquireLatestImage()).execute();
            }
        }, null);
        mJpegCaptureSurface = jpegReader.getSurface();

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
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
        WindowManager.LayoutParams params = (WindowManager.LayoutParams) mPreviewView.getLayoutParams();
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

    /**
     * This should be triggered by a capture button press or something similar
     */
    public void capture() {
        try {
            CaptureRequest.Builder builder = mCamera.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE);
            builder.set(CaptureRequest.CONTROL_MODE, CameraMetadata.CONTROL_MODE_AUTO);

            // we probably don't want to be auto focusing while an image is being captured.
            builder.set(CaptureRequest.CONTROL_AF_MODE, CameraMetadata.CONTROL_AF_MODE_OFF);
            builder.set(CaptureRequest.CONTROL_AF_TRIGGER, CameraMetadata.CONTROL_AF_TRIGGER_IDLE);

            // set options here that the user has changed
//            builder.set(CaptureRequest.LENS_FOCUS_DISTANCE, ...)
//            builder.set(CaptureRequest.CONTROL_AWB_MODE, ...)
//            builder.set(CaptureRequest.CONTROL_EFFECT_MODE, ...)
//            builder.set(CaptureRequest.CONTROL_AE_EXPOSURE_COMPENSATION, ...)
//            etc...

            builder.set(CaptureRequest.LENS_OPTICAL_STABILIZATION_MODE, CameraMetadata.LENS_OPTICAL_STABILIZATION_MODE_ON);

            if (mCaptureImageFormat == ImageFormat.JPEG) {
                builder.addTarget(mJpegCaptureSurface);
                builder.set(CaptureRequest.JPEG_QUALITY, (byte) 100);
            } else {
                builder.addTarget(mRawCaptureSurface);
                builder.set(CaptureRequest.STATISTICS_LENS_SHADING_MAP_MODE, CaptureRequest.STATISTICS_LENS_SHADING_MAP_MODE_ON);
            }

            mSession.capture(builder.build(), new CameraCaptureSession.CaptureCallback() {
                @Override
                public void onCaptureCompleted(CameraCaptureSession session, CaptureRequest request, TotalCaptureResult result) {
                    // save this, as it's needed to create raw files
                    mPendingResult = result;
                }

                @Override
                public void onCaptureFailed(CameraCaptureSession session, CaptureRequest request, CaptureFailure failure) {
                    super.onCaptureFailed(session, request, failure);
                    Log.e(TAG, "Image capture failed");
                }
            }, null);
        } catch (CameraAccessException e) {
            Log.e(TAG, "Image capture failed", e);
        }
    }

    @Override
    public void onSurfaceTextureAvailable(SurfaceTexture surfaceTexture, int i, int i1) {

    }

    @Override
    public void onSurfaceTextureSizeChanged(SurfaceTexture surfaceTexture, int i, int i1) {

    }

    @Override
    public boolean onSurfaceTextureDestroyed(SurfaceTexture surfaceTexture) {
        return false;
    }

    @Override
    public void onSurfaceTextureUpdated(SurfaceTexture surfaceTexture) {

    }

    private static class SaveRawTask extends AsyncTask<Void, Void, Boolean> {

        private WeakReference<Context> mContextRef;
        private File mFile;
        private Image mImage;
        private DngCreator mDngCreator;

        public SaveRawTask(Context context, String dir, Image image, CameraCharacteristics characteristics, CaptureResult metadata) {
            mContextRef = new WeakReference<>(context);
            mFile = new File(dir);
            mImage = image;
            mDngCreator = new DngCreator(characteristics, metadata);
        }

        @Override
        protected Boolean doInBackground(Void... params) {
            try {
                mDngCreator.writeImage(new FileOutputStream(mFile), mImage);
                mDngCreator.close();
                mImage.close();
                return true;
            } catch (IOException e) {
                return false;
            }
        }

        @Override
        protected void onPostExecute(Boolean result) {
            Context context = mContextRef.get();
            if (context != null) {
                if (result) {
                    MediaScannerConnection.scanFile(context, new String[]{mFile.getAbsolutePath()}, null, null);
                    Toast.makeText(context, "Image captured!", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(context, "Error saving image", Toast.LENGTH_LONG).show();
                }
            }
        }
    }

    private static class SaveJpegTask extends AsyncTask<Void, Void, Boolean> {

        private File mFile;
        private Image mImage;
        private WeakReference<Context> mContextRef;

        public SaveJpegTask(Context context, String dir, Image image) {
            mContextRef = new WeakReference<>(context);
            mFile = new File(dir);
            mImage = image;
        }

        @Override
        protected Boolean doInBackground(Void... params) {
            ByteBuffer buffer = mImage.getPlanes()[0].getBuffer();
            byte[] bytes = new byte[buffer.capacity()];
            buffer.get(bytes);
            mImage.close();
            try {
                new FileOutputStream(mFile).write(bytes);
                return true;
            } catch (IOException e) {
                return false;
            }
        }

        @Override
        protected void onPostExecute(Boolean result) {
            Context context = mContextRef.get();
            if (context != null) {
                if (result) {
                    MediaScannerConnection.scanFile(context, new String[]{mFile.getAbsolutePath()}, null, null);
                    Toast.makeText(context, "Image captured!", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(context, "Error saving image", Toast.LENGTH_LONG).show();
                }
            }
        }
    }

    /**
     * Given a target size for raw output, search available preview sizes for one with a similar
     * aspect ratio that does not exceed screen size.
     */
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

}
