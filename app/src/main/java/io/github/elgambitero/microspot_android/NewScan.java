package io.github.elgambitero.microspot_android;


import android.app.FragmentTransaction;
import android.content.Context;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;

import android.view.WindowManager;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;

/**
 * Created by elgambitero on 30/12/15.
 */
public class NewScan extends AppCompatActivity implements PatientInput.PatientInputListener{

    Toolbar toolbar;


    /*========================
    Activity lifecycle Methods
    ==========================*/

    @Override
    protected void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        setContentView(R.layout.newscan);
        initializeLayout();
        setSupportActionBar(toolbar);

    }

    @Override
    public void onPause(){
        super.onPause();
        finish();
    }

    /*===================================
    Layout handling methods and variables
    =====================================*/

    android.support.v4.app.FragmentManager fragmentManager = getSupportFragmentManager();


    private void initializeLayout(){
        toolbar = (Toolbar)findViewById(R.id.newtoolbar);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
        getWindow().setStatusBarColor(getResources().getColor(R.color.colorPrimaryDark));

        goToStep(0);
    }

    private void goToStep(int step) {
        android.support.v4.app.FragmentTransaction fragTran;
        fragTran = fragmentManager.beginTransaction();
        switch (step) {
            case 0:
                PatientInput fragment = new PatientInput();
                fragTran.replace(R.id.newScanSteps, fragment);
                break;
        }
        fragTran.addToBackStack(null);
        fragTran.commit();
    }

    /*===================
    File handling methods
    =====================*/

    private OutputStream getTempFile(boolean makeNew){
        if(makeNew) {
            deleteTempFile(this);
        }
        File tempFile = new File(getApplicationContext().getExternalFilesDir("/temp").getPath()
            +"info.txt");
        OutputStream out=null;
        try {
            out = new FileOutputStream(tempFile);
        }catch (Exception e) {
            e.printStackTrace();
        }

        return out;
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


    @Override
    public void getPatientData(String id, String annotation) {
        OutputStream out = getTempFile(true);
        try {
            out.write(("PatientId = " + id + "\r\n").getBytes());
            out.write(("annotations = " + annotation + "\r\n").getBytes());
        }catch (Exception e) {
            e.printStackTrace();
        }
    }


}
