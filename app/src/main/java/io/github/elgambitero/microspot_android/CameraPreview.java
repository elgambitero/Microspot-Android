package io.github.elgambitero.microspot_android;

/**
 * Created by elgambitero on 03/01/16.
 */

import android.Manifest;
import android.annotation.TargetApi;
import android.content.Context;
import android.content.pm.PackageManager;
import android.content.res.Configuration;

import android.graphics.Camera;
import android.graphics.ImageFormat;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CameraMetadata;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.TotalCaptureResult;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.ImageReader;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.util.Log;
import android.util.Size;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.TextureView;
import android.view.ViewGroup;

import java.util.Arrays;
import java.util.List;

/** A basic Camera preview class */

public class CameraPreview extends SurfaceView implements TextureView.SurfaceTextureListener {

    private CameraDevice mCamera;
    private CameraCaptureSession mSession;
    Size mPreviewSize;
    private CameraCharacteristics mCharacteristics;
    private int mCaptureImageFormat;
    Context mContext;
    Surface mPreviewSurface;
    TextureView previewView;

    private static String TAG = "Camera2Preview";

    public CameraPreview(Context context,TextureView preview) {
        super(context);
        mContext = context;
        previewView = preview;

    }

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
    public void onSurfaceTextureUpdated(SurfaceTexture surfaceTexture) {

    }

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

    private void initPreview(TextureView view) {
        // scale preview size to fill screen width
        int screenWidth = getResources().getDisplayMetrics().widthPixels;
        float previewRatio = mPreviewSize.getWidth() / ((float) mPreviewSize.getHeight());
        int previewHeight = Math.round(screenWidth * previewRatio);
        ViewGroup.LayoutParams params = view.getLayoutParams();
        params.width = screenWidth;
        params.height = previewHeight;

        List<Surface> surfaces = Arrays.asList(mPreviewSurface);
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

}