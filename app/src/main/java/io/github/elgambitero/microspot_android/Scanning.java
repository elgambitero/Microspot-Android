package io.github.elgambitero.microspot_android;


import android.content.Context;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;

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


    private final String TAG = "Scanning";

    String nextPhotoName;

    Double[] xCoord, yCoord;

    ScanningListener newScanListener;


    public interface ScanningListener{
        Double[] getXCoordinates();
        Double[] getYCoordinates();
        String getPatientId();
        void moveAxisRel(Double xCoord, Double yCoord, Double speed);
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

        xCoord = newScanListener.getXCoordinates();
        yCoord = newScanListener.getYCoordinates();

    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        Log.d(TAG,"Invoke thread");
        scanThread();
    }

    /*==============
    * Layout methods
    ================*/

    public void initializeLayout(View v){

        progressBar = (ProgressBar)v.findViewById(R.id.progressBar);
        progressBar.setVisibility(View.VISIBLE);
        progressBar.setMax(xCoord.length*yCoord.length);
        progressBar.setProgress(0);

    }


    /*================
    Scan cycle methods
    ==================*/


    private void zipContentsAndExit(){
        String source = String.valueOf(getContext()
                .getExternalFilesDir(String.valueOf(R.string.temp_scans_folder)));
        String destination = String.valueOf(getContext()
                .getExternalFilesDir(String.valueOf(R.string.samples_folder)))
                    + "/" + newScanListener.getPatientId() + ".zip";
        zipFileAtPath(source, destination);
        newScanListener.endScan();
    }

    /*=============
    Scanning thread
    ===============*/

    private void scanThread(){
        new Thread(new Runnable() {
            @Override
            public void run() {
                File newDir = new File(
                        String.valueOf(getContext().
                                getExternalFilesDir(String.valueOf(R.string.temp_scans_folder))));
                newDir.mkdirs();
                Log.d(TAG,"Dirs made");
                Double incX, incY;
                incX = xCoord[0]-25.0;
                incY = yCoord[0]-7.5;
                int i = 1;
                int j = 1;
                do{
                    do{
                        try {
                            makeScan(incX,incY,j,i);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                        progressBar.setProgress(i*xCoord.length + j);
                        if(j == xCoord.length){
                            incX = xCoord[0]-xCoord[j-1];
                        }else{
                            incX = xCoord[j]-xCoord[j-1];
                        }
                        incY = 0.0;
                        j++;
                    }while(j<=xCoord.length);
                    j = 1;
                    if(i == yCoord.length){
                        incY = yCoord[0]-yCoord[j-1];
                    }else{
                        incY = yCoord[j]-yCoord[j-1];
                    }
                    Log.d(TAG,"Moving with vector: (" + incX.toString() + "," + incY.toString() + ")");
                    i++;
                }while(i<=yCoord.length);

                zipContentsAndExit();
            }

            private void makeScan(Double xCoord, Double yCoord, Integer xShotNum, Integer yShotNum) throws InterruptedException {

                newScanListener.moveAxisRel(xCoord, yCoord, 2000.0);
                Long waitTime = (long)(Math.sqrt(Math.pow(xCoord,2)+Math.pow(yCoord,2))/0.005);
                try{
                    Log.d(TAG,"Waiting for " + waitTime.toString() + " milliseconds");
                    Thread.sleep(waitTime+500);
                    Log.d(TAG,"Waited");
                }catch (Exception e){
                    e.printStackTrace();
                }
                String shotName = xShotNum.toString() + "_" + yShotNum.toString() + ".jpg";
                nextPhotoName = String.valueOf(getContext().
                        getExternalFilesDir(String.valueOf(R.string.temp_scans_folder + shotName)));

                //Make the photo with nextPhotoName filename!!!

            }
        }).start();
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
