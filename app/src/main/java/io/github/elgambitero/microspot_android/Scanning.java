package io.github.elgambitero.microspot_android;


import android.content.Context;
import android.content.pm.PackageManager;
import android.hardware.Camera;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ProgressBar;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.Math;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * Created by Jaime García Villena "garciavillena.jaime@gmail.com" on 6/26/16.
 */
public class Scanning extends Fragment{

    ProgressBar progressBar;


    private final String TAG = "Scanning";

    String nextPhotoName;

    Double[] xCoord, yCoord;

    ScanningListener newScanListener;

    Camera mCamera;
    CameraPreview mPreview;
    FrameLayout framePreview;

    SerialService serialService;


    public interface ScanningListener{
        Double[] getXCoordinates();
        Double[] getYCoordinates();
        String getPatientId();
        SerialService getSerialService();
        void endScan();
    }

    /*================
    Fragment lifecycle
    ==================*/

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.scanning_fragment,container,false);

        initializeLayout(view);


        return view;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        try{
            newScanListener = (ScanningListener) context;
        }catch (ClassCastException e){
            e.printStackTrace();
        }

        serialService = newScanListener.getSerialService();

        xCoord = newScanListener.getXCoordinates();
        yCoord = newScanListener.getYCoordinates();

    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        mCamera = initializeCamera();
        if(mCamera!=null){
            setCamFocusMode();
            mPreview = new CameraPreview(getContext(),mCamera);
            framePreview.addView(mPreview);
        }
    }

    /*==============
    * Layout methods
    ================*/

    public void initializeLayout(View v){

        progressBar = (ProgressBar)v.findViewById(R.id.progressBar);
        progressBar.setVisibility(View.VISIBLE);
        progressBar.setMax(xCoord.length*yCoord.length);
        progressBar.setProgress(0);
        framePreview = (FrameLayout)v.findViewById(R.id.camera_preview_scanning);

    }


    /*================
    Scan cycle methods
    ==================*/

    private void scanProcess(){

        File newDir = new File(
                String.valueOf(getContext().
                        getExternalFilesDir(String.valueOf(R.string.temp_scans_folder))));
        newDir.mkdirs();


        for(int i = 0; i< yCoord.length;i++){
            for(int j = 0;j < xCoord.length; j++){
               makeScan(xCoord[j],yCoord[i],j,i);
               progressBar.setProgress(i*yCoord.length + j*xCoord.length);
            }
        }

        zipContentsAndExit();

    }

    private void makeScan(Double xCoord, Double yCoord, Integer xShotNum, Integer yShotNum){

        serialService.axisTo(xCoord, yCoord, 2000.0);
        Long waitTime = (long)(Math.sqrt(Math.pow(xCoord,2)+Math.pow(yCoord,2))/(2000.0/60000));
        try{
            wait(waitTime+500);
        }catch (Exception e){
            e.printStackTrace();
        }
        String shotName = xShotNum.toString() + "_" + yShotNum.toString() + ".jpg";
        nextPhotoName = String.valueOf(getContext().
                getExternalFilesDir(String.valueOf(R.string.temp_scans_folder + shotName))); //I NEED to pass this to PictureCallback


        mCamera.takePicture(null, null, mPicture);

    }


    private void zipContentsAndExit(){
        String source = String.valueOf(getContext()
                .getExternalFilesDir(String.valueOf(R.string.temp_scans_folder)));
        String destination = String.valueOf(getContext()
                .getExternalFilesDir(String.valueOf(R.string.samples_folder)))
                    + "/" + newScanListener.getPatientId() + ".zip";
        zipFileAtPath(source, destination);
        mCamera.release();
        newScanListener.endScan();
    }

    /*=====================
    Camera handling methods
    =======================*/

    private Camera initializeCamera(){
        if(checkCameraHardware(getContext())){
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


    private Camera.PictureCallback mPicture = new Camera.PictureCallback() {

        @Override
        public void onPictureTaken(byte[] data, Camera camera) {

            //File pictureFile = getPhotoFile();
            File pictureFile = getOutputMediaFile(1);
            if (pictureFile == null){
                Log.d(TAG, "Error creating media file, check storage permissions: ");
                return;
            }
            try {
                FileOutputStream fos = new FileOutputStream(pictureFile);
                fos.write(data);
                fos.close();
            } catch (FileNotFoundException e) {
                Log.d(TAG, "File not found: " + e.getMessage());
            } catch (IOException e) {
                Log.d(TAG, "Error accessing file: " + e.getMessage());
            }
            safeToTakePicture = true;
        }
    };

    /** Create a File for saving an image or video */
    private static File getOutputMediaFile(int type){
        // To be safe, you should check that the SDCard is mounted
        // using Environment.getExternalStorageState() before doing this.

        File mediaStorageDir = new File(Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_PICTURES), "MyCameraApp");
        // This location works best if you want the created images to be shared
        // between applications and persist after your app has been uninstalled.

        // Create the storage directory if it does not exist
        if (! mediaStorageDir.exists()){
            if (! mediaStorageDir.mkdirs()){
                Log.d("MyCameraApp", "failed to create directory");
                return null;
            }
        }

        // Create a media file name
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        File mediaFile;
        if (type == 1){
            mediaFile = new File(mediaStorageDir.getPath() + File.separator +
                    "IMG_"+ timeStamp + ".jpg");
        } else if(type == 2) {
            mediaFile = new File(mediaStorageDir.getPath() + File.separator +
                    "VID_"+ timeStamp + ".mp4");
        } else {
            return null;
        }

        return mediaFile;
    }

    private File getPhotoFile(){

        File newPhoto = new File(getPhotoName());
        try{
            newPhoto.createNewFile();
        }catch (IOException e){
            e.printStackTrace();
        }
        return newPhoto;
    }

    private String getPhotoName(){
        return nextPhotoName;
    }


    /*=========================================================================
    Zipping functions
    Original code by HailZeon "http://stackoverflow.com/users/2053024/hailzeon"
    ===========================================================================*/

    public boolean zipFileAtPath(String sourcePath, String toLocation) {
        final int BUFFER = 2048;

        File sourceFile = new File(sourcePath);
        try {
            BufferedInputStream origin;
            FileOutputStream dest = new FileOutputStream(toLocation);
            ZipOutputStream out = new ZipOutputStream(new BufferedOutputStream(
                    dest));
            if (sourceFile.isDirectory()) {
                zipSubFolder(out, sourceFile, sourceFile.getParent().length());
            } else {
                byte data[] = new byte[BUFFER];
                FileInputStream fi = new FileInputStream(sourcePath);
                origin = new BufferedInputStream(fi, BUFFER);
                ZipEntry entry = new ZipEntry(getLastPathComponent(sourcePath));
                out.putNextEntry(entry);
                int count;
                while ((count = origin.read(data, 0, BUFFER)) != -1) {
                    out.write(data, 0, count);
                }
            }
            out.close();
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    private void zipSubFolder(ZipOutputStream out, File folder,
                              int basePathLength) throws IOException {

        final int BUFFER = 2048;

        File[] fileList = folder.listFiles();
        BufferedInputStream origin;
        for (File file : fileList) {
            if (file.isDirectory()) {
                zipSubFolder(out, file, basePathLength);
            } else {
                byte data[] = new byte[BUFFER];
                String unmodifiedFilePath = file.getPath();
                String relativePath = unmodifiedFilePath
                        .substring(basePathLength);
                FileInputStream fi = new FileInputStream(unmodifiedFilePath);
                origin = new BufferedInputStream(fi, BUFFER);
                ZipEntry entry = new ZipEntry(relativePath);
                out.putNextEntry(entry);
                int count;
                while ((count = origin.read(data, 0, BUFFER)) != -1) {
                    out.write(data, 0, count);
                }
                origin.close();
            }
        }
    }

    public String getLastPathComponent(String filePath) {
        String[] segments = filePath.split("/");
        if (segments.length == 0)
            return "";
        String lastPathComponent = segments[segments.length - 1];
        return lastPathComponent;
    }

}
