package io.github.elgambitero.microspot_android;


import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.provider.MediaStore;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import io.github.elgambitero.microspot_android.SerialService;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.Math;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * Created by Jaime García Villena "garciavillena.jaime@gmail.com" on 6/26/16.
 */
public class Scanning extends Fragment{

    ProgressBar progressBar;
    private int progressBarStatus = 0;
    private Handler progressBarHandler = new Handler();


    Double[] xCoord, yCoord;

    ScanningListener newScanListener;

    SerialService serialService;


    public interface ScanningListener{
        Double[] getXCoordinates();
        Double[] getYCoordinates();
        SerialService getSerialService();
        void endScan();
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.scanning_fragment,container,false);

        xCoord = newScanListener.getXCoordinates();
        yCoord = newScanListener.getYCoordinates();

        initializeLayout(view);


        return view;
    }

    @Override
    public void onResume() {
        super.onResume();

        serialService = newScanListener.getSerialService();

        scanProcess();

    }

    public void initializeLayout(View v){

        progressBar = (ProgressBar)v.findViewById(R.id.progressBar);
        progressBar.setVisibility(View.VISIBLE);
        progressBar.setMax(xCoord.length*yCoord.length);
        progressBar.setProgress(0);

    }

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
        Long waitTime = (Long)(Math.sqrt(xCoord²+yCoord²)/(2000.0/60000));
        try{
            wait(waitTime+500);
        }catch (Exception e){
            e.printStackTrace();
        }
        String shotName = xShotNum.toString() + "_" + yShotNum.toString() + ".jpg";
        File newPhoto = new File(
                String.valueOf(getContext().
                        getExternalFilesDir(String.valueOf(R.string.temp_scans_folder + shotName))));
        try{
            newPhoto.createNewFile();
        }catch (IOException e){
            e.printStackTrace();
        }
        Uri outputFileUri = Uri.fromFile(newPhoto);

        Intent cameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        cameraIntent.putExtra(MediaStore.EXTRA_OUTPUT, outputFileUri);

        startActivityForResult(cameraIntent, 0);
    }

    private void zipContentsAndExit(){
        String source = String.valueOf(getContext().getExternalFilesDir(String.valueOf(R.string.temp_scans_folder));
        String destination = String.valueOf(getContext().getExternalFilesDir(String.valueOf(R.string.samples_folder));
        zipFileAtPath(source, destination);
        newScanListener.endScan();
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
