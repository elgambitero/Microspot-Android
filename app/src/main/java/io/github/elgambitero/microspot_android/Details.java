package io.github.elgambitero.microspot_android;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.WindowManager;
import android.widget.TextView;


/**
 * Created by elgambitero on 07/01/16.
 */
public class Details extends AppCompatActivity{

    TextView patientID, annotation;
    String stringPatientID, stringAnno;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.scan_details);
        Bundle gotStats = getIntent().getExtras();
        stringPatientID = gotStats.getString("PatientID");
        stringAnno = gotStats.getString("annotations");
        initLayout();
        initStatusBar();
    }

    private void initLayout(){
        patientID = (TextView)findViewById(R.id.detailPatientID);
        patientID.setText(stringPatientID);
        annotation = (TextView)findViewById(R.id.detailAnnotation);
        annotation.setText(stringAnno);
    }

    private void initStatusBar(){

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
        getWindow().setStatusBarColor(getResources().getColor(R.color.colorPrimaryDark));

    }
}
