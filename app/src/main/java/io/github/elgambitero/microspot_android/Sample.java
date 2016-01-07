package io.github.elgambitero.microspot_android;

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

    public static List<Sample> createSamplesList(String aux[]) {
        List<Sample> samples = new ArrayList<Sample>();

        for (int i = 0; i < (aux.length); i++) {
            samples.add(new Sample(aux[i]));
        }

        return samples;
    }
}
