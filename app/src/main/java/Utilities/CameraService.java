package Utilities;

import android.Manifest;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
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
import android.hardware.camera2.TotalCaptureResult;
import android.media.ImageReader;
import android.os.Binder;
import android.os.IBinder;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.util.Log;
import android.util.Size;
import android.view.Surface;
import android.view.SurfaceView;
import android.view.TextureView;
import android.view.ViewGroup;

import java.util.Arrays;
import java.util.List;

/**
 * Created by Jaime García Villena "garciavillena.jaime@gmail.com" on 7/7/16.
 */
public class CameraService extends Service {

    /*========================
    * Camera workflow elements
    ========================*/

    private CameraDevice mCamera;
    private CameraCaptureSession mSession;
    private CameraCharacteristics mCharacteristics;
    private CameraManager cameraManager;
    private String cameraId;
    private int mCaptureImageFormat;
    private Surface mJpegCaptureSurface, mPreviewSurface;
    private CaptureResult mPendingResult;


    /*=======================
    * Camera output variables
    =======================*/

    Size jpegSize, previewSize;

    /*============================
    * Camera interfacing variables
    ============================*/

    private final IBinder cameraBinder = new CameraBinder();

    /*================
    * Status variables
    ================*/

    private boolean safeToShoot = false;
    private boolean cameraConnected = false;


    /*===================
    * Debugging variables
    ===================*/

    private String TAG = "CameraService";


    /*============
    * UI variables
    ============*/

    private TextureView textureView;
    private CameraPreview cameraPreview;
    private SurfaceTexture mSurfaceTexture;


    /*=================
    * Service interface
    =================*/


    public class CameraBinder extends Binder {

        public void getCameraPreview(Context context, TextureView view){

            // scale preview size to fill screen width
            int screenWidth = getResources().getDisplayMetrics().widthPixels;
            float previewRatio = previewSize.getWidth() / ((float) previewSize.getHeight());
            int previewHeight = Math.round(screenWidth * previewRatio);

            //I don't think this does anything
            ViewGroup.LayoutParams params = view.getLayoutParams();
            params.width = screenWidth;
            params.height = previewHeight;

            mSurfaceTexture = new SurfaceTexture();
            cameraPreview = new CameraPreview(context,view);

        }

    }


    /*=================
    * Service lifecycle
    =================*/

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return cameraBinder;
    }



    /*=========
    Constructor
    =========*/

    public CameraService() throws CameraAccessException {

        getBackCameraCC();

        getCameraSizes();

        getJpegSurface();

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            Log.e(TAG,"No permissions granted for camera");
            return;
        }else {

            cameraManager.openCamera(cameraId, new OpenCameraCallback(), null);
        }

        openSession();

    }


    /*=======================
    * Service private methods
    =======================*/

    private void getBackCameraCC() throws CameraAccessException {

        cameraManager = (CameraManager) this.getSystemService(Context.CAMERA_SERVICE);

        //Get the ID of the back facing camera.
        String[] cameraIds;
        try {
            cameraIds = cameraManager.getCameraIdList();
        } catch (CameraAccessException e) {
            e.printStackTrace();
            return;
        }
        cameraId = null;
        CameraCharacteristics cc = null;
        for(String id : cameraIds){
            cc = cameraManager.getCameraCharacteristics(id);
            if (cc.get(CameraCharacteristics.LENS_FACING) == CameraCharacteristics.LENS_FACING_BACK){
                cameraId = id;
                break;
            }
        }
        if (cameraId == null){
            throw new CameraAccessException(CameraAccessException.CAMERA_ERROR,
                    "Couldn't find suitable camera");
        }
        mCharacteristics = cc;

    }


    //Get the jpeg and preview sizes from the camera.
    private void getCameraSizes(){

        Size[] jpegSizes = mCharacteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)
                .getOutputSizes(ImageFormat.JPEG);
        jpegSize = jpegSizes[0];
        Size[] previewSizes = mCharacteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)
                .getOutputSizes(SurfaceTexture.class);
        previewSize = findOptimalPreviewSize(previewSizes,jpegSize);

    }


    //Get a TextureView object to pass to a UI
    private void getJpegSurface(){

        ImageReader jpegReader = ImageReader.newInstance(jpegSize.getWidth(), jpegSize.getHeight(),
                ImageFormat.JPEG,1);
        jpegReader.setOnImageAvailableListener(new PhotoListener(), null);
        mJpegCaptureSurface = jpegReader.getSurface();

    }


    //Open the session for the camera
    private void openSession(){

        List<Surface> surfaces = Arrays.asList(mJpegCaptureSurface);

        try {
            mCamera.createCaptureSession(surfaces, new OpenSessionCallback(), null);
        } catch (CameraAccessException e) {
            Log.d(TAG, "Failed to create camera capture session", e);
        }

    }

    private void openSessionWithPreview(){

        List<Surface> surfaces = Arrays.asList(mPreviewSurface, mJpegCaptureSurface);

        try {
            mCamera.createCaptureSession(surfaces, new OpenSessionCallback(), null);
        } catch (CameraAccessException e) {
            Log.d(TAG, "Failed to create camera capture session", e);
        }
    }


    private void preparePreview(){

        try{
            if(mCamera == null || mSession == null){
                return;
            }
            CaptureRequest.Builder builder = mCamera.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
            builder.addTarget(mPreviewSurface);
            builder.set(CaptureRequest.CONTROL_AF_MODE, CameraMetadata.CONTROL_MODE_OFF);

            mSession.setRepeatingRequest(builder.build(),new PreviewUpdateCallback(),null);

        }catch (CameraAccessException e){
            e.printStackTrace();
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


    /*=================
    * Callback routines
    =================*/

    private class OpenCameraCallback extends CameraDevice.StateCallback {

        @Override
        public void onOpened(@NonNull CameraDevice camera) {
            mCamera = camera;
            cameraConnected = true;
        }

        @Override
        public void onDisconnected(CameraDevice cameraDevice) {

        }

        @Override
        public void onError(CameraDevice cameraDevice, int i) {

        }

    }


    private class OpenSessionCallback extends CameraCaptureSession.StateCallback {

        @Override
        public void onConfigured(CameraCaptureSession session) {
            mSession = session;
            //updatePreview();
        }

        @Override
        public void onConfigureFailed(CameraCaptureSession session) {

        }

    }


    private class PreviewUpdateCallback extends CameraCaptureSession.CaptureCallback {

        @Override
        public void onCaptureCompleted(CameraCaptureSession session, CaptureRequest request,
                                       TotalCaptureResult result) {
            // if desired, we can get updated auto focus & auto exposure values here from 'result'
        }

    }


    private class PhotoCallback extends CameraCaptureSession.CaptureCallback{

        @Override
        public void onCaptureCompleted(CameraCaptureSession session, CaptureRequest request,
                                       TotalCaptureResult result) {
            // save this, as it's needed to create raw files
            mPendingResult = result;
        }

        @Override
        public void onCaptureFailed(CameraCaptureSession session, CaptureRequest request,
                                    CaptureFailure failure) {
            super.onCaptureFailed(session, request, failure);
            Log.e(TAG, "Image capture failed");
        }

    }


    /*=========
    * Listeners
    =========*/

    private class PhotoListener implements ImageReader.OnImageAvailableListener {

        @Override
        public void onImageAvailable(ImageReader imageReader) {
            //Routines for saving the image
        }

    }


    private class CameraPreview extends SurfaceView implements TextureView.SurfaceTextureListener{

        private Context mContext;

        public CameraPreview(Context context,TextureView view) {
            super(context);
            mContext = context;
            textureView = view;
            textureView.setSurfaceTextureListener(this);
        }

        @Override
        public void onSurfaceTextureAvailable(SurfaceTexture surfaceTexture, int i, int i1) {
            mSurfaceTexture = surfaceTexture;

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

    }


}
