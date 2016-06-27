package io.github.elgambitero.microspot_android;


import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;

import android.util.Log;
import android.view.WindowManager;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

/**
 * Created by elgambitero on 30/12/15.
 */
public class NewScan extends AppCompatActivity implements PatientInput.PatientInputListener,
        ConfigScan.ConfigScanListener,
        CalibrateScan.CalibrateScanListener,
        Scanning.ScanningListener{

    Toolbar toolbar;
    OutputStream out;

    private Double[] _intervals = new Double[2];
    private Integer[] _shots = new Integer[2];

    //Service binding variables
    Boolean isBound;
    SerialService serialService;
    private static final String TAG = "NewScan";
    String _patientId;

    /*========================
    Activity lifecycle Methods
    ==========================*/

    @Override
    protected void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        setContentView(R.layout.newscan);
        initializeLayout();
        setSupportActionBar(toolbar);
        out = getTempFile(true);

        isBound = false;
        Log.d(TAG, "Attempting to bind");
        Intent i = new Intent(this, SerialService.class);
        isBound = getApplicationContext().bindService(i, serialConnection, Context.BIND_AUTO_CREATE);

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
                PatientInput step1 = new PatientInput();
                fragTran.replace(R.id.newScanSteps, step1);
                break;
            case 1:
                ConfigScan step2 = new ConfigScan();
                fragTran.replace(R.id.newScanSteps,step2);
                break;
            case 2:
                serialService.homeAxis();
                serialService.homeAxis();
                serialService.axisTo(25.0,7.5,2000.0);
                CalibrateScan step3 = new CalibrateScan();
                fragTran.replace(R.id.newScanSteps,step3);
                break;
            case 3:
                Scanning step4 = new Scanning();
                fragTran.replace(R.id.newScanSteps,step4);
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


    /*===============================
    Fragment interface implementation
    =================================*/

    @Override
    public void writePatientDataAndNext(String id, String annotation) {
        try {
            _patientId = id;
            out.write(("PatientId = " + id + "\r\n").getBytes());
            out.write(("annotations = " + annotation + "\r\n").getBytes());
        }catch (Exception e) {
            e.printStackTrace();
        }
        goToStep(1);
    }

    @Override
    public void writeGridDataAndNext(Double intervalX, Double intervalY, Integer shotsX, Integer shotsY){
        _intervals[0] = intervalX;
        _intervals[1] = intervalY;
        _shots[0] = shotsX;
        _shots[1] = shotsY;
        try {
            out.write(("intervalX = " + intervalX.toString() + "\r\n").getBytes());
            out.write(("intervalY = " + intervalY.toString() + "\r\n").getBytes());
            out.write(("shotsX = " + shotsX.toString() + "\r\n").getBytes());
            out.write(("shotsY = " + shotsY.toString() + "\r\n").getBytes());
        }catch (Exception e) {
            e.printStackTrace();
        }
        try {
            out.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        goToStep(2);
    }

    @Override
    public void setFocusAndNext(){
        goToStep(3);
    }





    @Override
    public Double[] getXCoordinates() {
        Double[] xCoord;
        xCoord = new Double[_shots[0]];
        for(int i = 0; i <_shots[0]; i++){
            xCoord[i]=25.0 -(_intervals[0]*_shots[0]/2) + i*_intervals[0];
        }
        return xCoord;
    }

    @Override
    public Double[] getYCoordinates() {
        Double[] yCoord;
        yCoord = new Double[_shots[1]];
        for(int i = 0; i <_shots[1]; i++){
            yCoord[i]=7.5 -(_intervals[1]*_shots[1]/2) + i*_intervals[1];
        }
        return yCoord;
    }


    @Override
    public void moveAxisRel(Double xCoord, Double yCoord, Double speed){
        serialService.moveAxisRel(xCoord, yCoord, speed);
    }

    @Override
    public String getPatientId(){
        return _patientId;
    }

    @Override
    public void endScan(){
        deleteTempFile(this);

        /*
        android.support.v4.app.FragmentTransaction fragTran;
        fragTran = fragmentManager.beginTransaction();
        fragTran.remove(R.id.newScanSteps);
        fragTran.addToBackStack(null);
        fragTran.commit();
        */

        Intent i = new Intent(getPackageName()+".MANAGESAMPLES");
        startActivity(i);
    }

    /*===============================
    * Service connection declarations
      ===============================*/

    private ServiceConnection serialConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName name, IBinder service){
            SerialService.SerialBinder binder = (SerialService.SerialBinder) service;
            serialService = binder.getService();
            Log.d(TAG, "Attempted to bind.");
            if(serialService != null) {
                Log.d(TAG, "Service is bound successfully!");
                try {
                    serialService.initializeSerial();
                }catch (Exception e){
                    e.printStackTrace();
                }
            }else{
                Log.d(TAG, "Service binding error");
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName name){
            isBound = false;
        }
    };

}
