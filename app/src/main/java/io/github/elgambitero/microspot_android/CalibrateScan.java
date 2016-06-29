package io.github.elgambitero.microspot_android;

import android.content.Context;
import android.hardware.camera2.CameraAccessException;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.TextureView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;

/**
 * Created by Jaime García Villena "garciavillena.jaime@gmail.com" on 6/26/16.
 */
public class CalibrateScan extends Fragment implements View.OnClickListener{


    CalibrateScanListener newScanListener;

    TextureView mPreviewView;
    Camera2Preview camera2Preview;
    ImageView nocamview;

    Button startButton;

    public interface CalibrateScanListener{
        void setFocusAndNext() throws CameraAccessException;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        try{
            newScanListener = (CalibrateScanListener) context;
        }catch (ClassCastException e){
            e.printStackTrace();
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,  Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.calib_scan_fragment,container,false);

        initializeLayout(view);

        return view;
    }


    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        camera2Preview = new Camera2Preview(getContext(),mPreviewView);
    }


    private void initializeLayout(View view){
        mPreviewView = (TextureView) view.findViewById(R.id.camera_preview_scan);
        nocamview = new ImageView(getContext());

        startButton = (Button)view.findViewById(R.id.startScan);
        startButton.setOnClickListener(this);
    }


    @Override
    public void onClick(View view) {
        switch(view.getId()) {
            case R.id.startScan:
                try {
                    newScanListener.setFocusAndNext();
                } catch (CameraAccessException e) {
                    e.printStackTrace();
                }
        }
    }
}
