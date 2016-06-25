package io.github.elgambitero.microspot_android;


import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;

/**
 * Created by Jaime García Villena "garciavillena.jaime@gmail.com" on 6/26/16.
 */
public class Scanning extends Fragment{

    ProgressBar progressBar;

    Double[] xCoord, yCoord;

    ScanningListener newScanListener;

    public interface ScanningListener{
        Integer[] getXCoordinates();
        Integer[] getYCoordinates();
        void makePhoto();
        void nextPosition();//An iterator maybe?
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.scanning_fragment,container,false);

        initializeLayout(view);

        return view;
    }

    public void initializeLayout(View v){

        progressBar = (ProgressBar)v.findViewById(R.id.progressBar);

    }
}
