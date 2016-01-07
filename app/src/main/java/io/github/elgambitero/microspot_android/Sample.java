package io.github.elgambitero.microspot_android;

import android.content.Context;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * Created by CodePath
 * Adapted by elgambitero on 05/01/16.
 */
public class Sample {
    private String _Title,_Id, _annotations, _shotsX, _shotsY, _gridSize;


    public Sample(String filename, Context context){
        linkedFile = new File(filename);
        String folderstring = context.getExternalFilesDir("/scans").getPath();
        _Title = filename.substring(folderstring.length()+1,
                filename.length()-4);
    }

    public String getTitle() {
        return _Title;
    }

    public File linkedFile;

    public static List<Sample> createSamplesList(Context context) {
        List<Sample> samples = new ArrayList<Sample>();
        File scanFolder = context.getExternalFilesDir("/scans");
        File scanFiles[] = scanFolder.listFiles();
        if (scanFolder.listFiles()!=null) {
            for (int i = 0; i < (scanFiles.length); i++) {
                samples.add(new Sample(scanFiles[i].getPath(),context));
            }
        }

        return samples;
    }

    //This method fills this class instance data about the particular sample.
    public void getScanInfo(File scan,Context context) throws IOException {
        //First unzip the info.txt and store it in a temp folder
        deleteTempFile(context);
        OutputStream out = new FileOutputStream(
                context.getExternalFilesDir(String.valueOf(R.string.temp_info_file)));
        FileInputStream fin = new FileInputStream(scan.getPath());
        BufferedInputStream bin = new BufferedInputStream(fin);
        ZipInputStream zin = new ZipInputStream(bin);
        ZipEntry ze = null;
        while ((ze = zin.getNextEntry()) != null) {
            if (ze.getName().equals("info.txt")) {
                byte[] buffer = new byte[8192];
                int len;
                while ((len = zin.read(buffer)) != -1) {
                    out.write(buffer, 0, len);
                }
                out.close();
                break;
            }
        }
        String aux;
        FileInputStream infoFIS = new FileInputStream(context.getExternalFilesDir(
                String.valueOf(R.string.temp_info_file)).getPath());
        DataInputStream infoDIS = new  DataInputStream(infoFIS);
        InputStreamReader infoISR = new InputStreamReader(infoDIS);
        BufferedReader infoBR = new BufferedReader(infoISR);
        aux = infoBR.readLine();
        while(aux != null){
            aux = infoBR.readLine();
            int divider = aux.indexOf(" = ");
            String field = aux.substring(0,divider);
            String content = aux.substring(divider+3,aux.length());
            switch(field){
                case "PatientID":
                    _Id = content;
                    break;
                case "annotations":
                    _annotations=content;
                    break;
                case "shotsX":
                    _shotsX=content;
                    break;
                case "shotsY":
                    _shotsY=content;
                    break;
                case "gridSize":
                    _gridSize=content;
                    break;
            }
            infoBR.close();
            infoDIS.close();
            infoFIS.close();
        }


    }

    public boolean deleteTempFile(Context context){
        File file = new File(
                context.getExternalFilesDir(String.valueOf(R.string.temp_info_file)).getPath());
        boolean deleted = file.delete();
        return deleted;
    }
}
