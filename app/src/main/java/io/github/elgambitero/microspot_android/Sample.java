package io.github.elgambitero.microspot_android;

import android.content.Context;
import android.os.Environment;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by CodePath
 * Adapted by elgambitero on 05/01/16.
 */
public class Sample {
    String _Id;

    public Sample(String Id){
        _Id = Id;
    }

    public String getTitle() {
        return _Id;
    }

    public static List<Sample> createSamplesList(Context context) {
        List<Sample> samples = new ArrayList<Sample>();
        File scanFolder = context.getExternalFilesDir("/scans");
        File scanFiles[] = scanFolder.listFiles();
        if (scanFolder.listFiles()!=null) {
            for (int i = 0; i < (scanFiles.length); i++) {
                //samples.add(new Sample(aux[i]));
                samples.add(new Sample(scanFiles[i].getName()));
            }
        }

        return samples;
    }
}
