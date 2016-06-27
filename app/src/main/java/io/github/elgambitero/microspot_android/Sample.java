package io.github.elgambitero.microspot_android;

import android.content.Context;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * Created by CodePath
 * Adapted by Jaime García Villena on 05/01/16.
 */
public class Sample {
    private String _Title, _Id, _annotations;
    private Double[] _intervals = new Double[2];
    private Integer[] _shots = new Integer[2];


    public Sample(String filename, Context context){
        linkedFile = new File(filename);
        String folderstring = context.getExternalFilesDir("/scans").getPath();
        _Title = filename.substring(folderstring.length()+1,
                filename.length()-4);
    }

    /*===========================
    * Sample info getting methods
      ===========================*/


    public String getTitle() {
        return _Title;
    }
    public String getId() {
        return _Id;
    }
    public String getAnno() {
        return _annotations;
    }

    public Integer[] getShots(){
        return _shots;
    }

    public Double[] getIntervals(){
        return _intervals;
    }


  /*===========================
  * Sample info setting methods
    ===========================*/

    public void setTitle(String s) {
        _Title = s;
    }

    public void setId(String s) {
        _Id = s;
    }
    public void setAnno(String s) {
        _annotations = s;
    }



    public File linkedFile;

    public static List<Sample> createSamplesList(Context context) {
        List<Sample> samples = new ArrayList<Sample>();
        File scanFolder = context.getExternalFilesDir(String.valueOf(R.string.samples_folder));
        File scanFiles[] = scanFolder.listFiles();
        if (scanFolder.listFiles()!=null) {
            for (int i = 0; i < (scanFiles.length); i++) {
                samples.add(new Sample(scanFiles[i].getPath(),context));
            }
        }

        return samples;
    }

    //This method fills this class instance data about the particular sample.
    public void getScanInfo(Context context) throws IOException {
        //First unzip the info.txt and store it in a temp folder
        deleteTempFile(context);
        File tempFile = new File(context.getExternalFilesDir("/temp").
                        getPath()+"/info.txt");
        OutputStream out = new FileOutputStream(tempFile);
        FileInputStream fin = new FileInputStream(linkedFile.getPath());
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
        FileInputStream infoFIS = new FileInputStream(tempFile);
        DataInputStream infoDIS = new  DataInputStream(infoFIS);
        InputStreamReader infoISR = new InputStreamReader(infoDIS);
        BufferedReader infoBR = new BufferedReader(infoISR);
        aux = infoBR.readLine();
        while(aux != null){
            int divider = aux.indexOf(" = ");
            String field = aux.substring(0, divider);
            String content = aux.substring(divider+3,aux.length());
            switch(field){
                case "PatientID":
                    _Id = content;
                    break;
                case "annotations":
                    _annotations=content;
                    break;
                default:
                    Double contentDouble = 0.0;
                    try {
                        contentDouble = Double.parseDouble(content);
                    }catch (NumberFormatException e){
                        e.printStackTrace();
                        contentDouble = 0.0;
                    }
                    switch(field) {
                        case "intervalX":
                            _intervals[0] = contentDouble;
                            break;
                        case "intervalY":
                            _intervals[1] = contentDouble;
                            break;
                        default:
                            Integer contentInteger = 0;
                            try{
                                contentInteger = Integer.parseInt(content);
                            }catch (NumberFormatException e){
                                e.printStackTrace();
                            }
                            switch(field) {
                               case "shotsX":
                                   _shots[0] = contentInteger;
                                   break;
                               case "shotsY":
                                   _shots[1] = contentInteger;
                                   break;
                            }
                            break;
                    }
                    break;
            }
            aux = infoBR.readLine();
            infoBR.close();
            infoDIS.close();
            infoFIS.close();
        }


    }

    public boolean deleteTempFile(Context context){ //more like resetTempFile
        File file = new File(
                String.valueOf(context.
                        getExternalFilesDir(String.valueOf(R.string.temp_info_file))));
        boolean deleted = file.delete();
        file = new File(
                String.valueOf(context.
                        getExternalFilesDir(String.valueOf(R.string.temp_info_file))));
        return deleted;
    }
}
