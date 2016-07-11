package io.github.elgambitero.microspot_android;

/**
 * Created by elgambitero on 03/01/16.
 */

import android.Manifest;
import android.annotation.TargetApi;
import android.content.Context;
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
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.ImageReader;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.util.Log;
import android.util.Size;
import android.view.Surface;
import android.view.SurfaceView;
import android.view.TextureView;
import android.view.ViewGroup;

import java.util.Arrays;
import java.util.List;

import Utilities.SaveJpegTask;
import Utilities.SaveRawTask;

/** A basic Camera preview class */

public class Camera2Preview extends SurfaceView implements TextureView.SurfaceTextureListener {

    private CameraDevice mCamera;
    private CameraCaptureSession mSession;
    private Size mPreviewSize;
    private CameraCharacteristics mCharacteristics;
    private int mCaptureImageFormat;
    private Context mContext;
    private Surface mRawCaptureSurface, mJpegCaptureSurface, mPreviewSurface;
    private TextureView previewView;
    private CaptureResult mPendingResult;
    private boolean safeToShoot;
    public String nextPhotoName;

    private static boolean useRaw = false;

    private static String TAG = "Camera2Preview";

    public void setSafeToShoot(Boolean result){
        safeToShoot = result;
    }

    public boolean getSafeToShoot(){
        return safeToShoot;
    }

    /*===========
    * Constructor
    ===========*/

    public Camera2Preview(Context context, TextureView view) {
        super(context);
        mContext = context;
        previewView = view;
        previewView.setSurfaceTextureListener(this);

    }

    /*=================================
    * Surface texture listening methods
    =================================*/

    @Override
    public void onSurfaceTextureAvailable(SurfaceTexture surfaceTexture, int i, int i1) {
        try{
            initCamera(surfaceTexture, mContext);
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
    public void onSurfaceTextureUpdated(SurfaceTexture surfaceTexture) {}

    /*=======================================
    Camera and preview initialization methods
    =======================================*/

    @TargetApi(Build.VERSION_CODES.M)
    private void initCamera(SurfaceTexture surface,Context context) throws CameraAccessException {
        CameraManager cm = (CameraManager) context.getSystemService(Context.CAMERA_SERVICE);

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
        if (supportsRaw && useRaw) {
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


        mPreviewSurface = new Surface(surface);


        // set up capture surfaces and image readers..
        ImageReader rawReader = ImageReader.newInstance(jpegSize.getWidth(), jpegSize.getHeight(),
                ImageFormat.RAW_SENSOR, 1);
        rawReader.setOnImageAvailableListener(new ImageReader.OnImageAvailableListener() {
            @Override
            public void onImageAvailable(ImageReader reader) {
                (new SaveRawTask(getContext(), nextPhotoName , reader.acquireLatestImage(), mCharacteristics, mPendingResult)).execute();
            }
        }, null);
        mRawCaptureSurface = rawReader.getSurface();


        ImageReader jpegReader = ImageReader.newInstance(jpegSize.getWidth(), jpegSize.getHeight(),
                ImageFormat.JPEG, 1);
        jpegReader.setOnImageAvailableListener(new ImageReader.OnImageAvailableListener() {
            @Override
            public void onImageAvailable(ImageReader reader) {
                new SaveJpegTask(getContext(), nextPhotoName, reader.acquireLatestImage()).execute();
            }
        }, null);


        mJpegCaptureSurface = jpegReader.getSurface();


        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
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
                    initPreview(previewView);
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

    private void initPreview(TextureView previewView) {
        // scale preview size to fill screen width
        int screenWidth = getResources().getDisplayMetrics().widthPixels;
        float previewRatio = mPreviewSize.getWidth() / ((float) mPreviewSize.getHeight());
        int previewHeight = Math.round(screenWidth * previewRatio);

        //I don't think this does anything
        ViewGroup.LayoutParams params = previewView.getLayoutParams();
        params.width = screenWidth;
        params.height = previewHeight;


        List<Surface> surfaces = Arrays.asList(mPreviewSurface, mJpegCaptureSurface);


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



    /*=================
    * Capturing methods
    =================*/


    public void capture() {
        try {
            safeToShoot = false;
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

            builder.set(CaptureRequest.LENS_OPTICAL_STABILIZATION_MODE, CameraMetadata.LENS_OPTICAL_STABILIZATION_MODE_OFF);

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

    public void setNextPhotoName(String name){
        nextPhotoName = name;
    }


    /*===============
    Auxiliary methods
    ===============*/

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