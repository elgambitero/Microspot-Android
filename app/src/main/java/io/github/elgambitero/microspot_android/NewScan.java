package io.github.elgambitero.microspot_android;


import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;

import io.github.elgambitero.microspot_android.PatientInput;

/**
 * Created by elgambitero on 30/12/15.
 */
public class NewScan extends AppCompatActivity implements View.OnClickListener, PatientInput.PatientInputListener{

    Button beginscan;
    EditText patientId, annotation;
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

    /*=====================
    Layout handling methods
    =======================*/

    private void initializeLayout(){
        toolbar = (Toolbar)findViewById(R.id.newtoolbar);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
        getWindow().setStatusBarColor(getResources().getColor(R.color.colorPrimaryDark));

    }

    @Override
    public void onClick(View v) {
        switch(v.getId()){
            case R.id.beginscan:
                newSample = new Sample();

                Intent i = new Intent(getPackageName()+".Scan");
                startActivity(i);
                break;
        }
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
        OutputStream out = new FileOutputStream(tempFile);
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
}

    @Override
    public void getPatientData(String id, String annotation) {
        OutputStream out = getTempFile(true);

    }
}
