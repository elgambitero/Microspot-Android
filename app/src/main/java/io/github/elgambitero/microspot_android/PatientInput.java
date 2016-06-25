package io.github.elgambitero.microspot_android;

import android.app.Activity;
import android.app.Fragment;
import android.content.Context;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;

/**
 * Created by Jaime García Villena "garciavillena.jaime@gmail.com" on 6/25/16.
 */

public class PatientInput extends Fragment implements View.OnClickListener{

    Button configScan;
    EditText patientId, annotation;

    PatientInputListener newScanListener;

    public interface PatientInputListener{
        void getPatientData(String id, String annotation);
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        try{
            newScanListener = (PatientInputListener) context;
        }catch (ClassCastException e){
            e.printStackTrace();
        }
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.patientinput_fragment,container,false);
        initializeLayout();

        return view;
    }

    private void initializeLayout(){
        configScan = (Button) configScan.findViewById();
        patientId = (EditText) patientId.findViewById(R.id.patientID);
        annotation = (EditText) annotation.findViewById(R.id.annotation);


        configScan.setOnClickListener(this);
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()){
            case R.id.configscan:
                newScanListener.getPatientData(patientId.getText().toString(),annotation.getText().toString());
        }
    }
}
