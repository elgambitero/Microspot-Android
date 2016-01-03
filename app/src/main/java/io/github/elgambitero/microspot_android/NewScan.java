package io.github.elgambitero.microspot_android;


import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;

/**
 * Created by elgambitero on 30/12/15.
 */
public class NewScan extends AppCompatActivity implements View.OnClickListener{

    Button beginscan;
    EditText patientId, annotation;
    Toolbar toolbar;


    @Override
    protected void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        setContentView(R.layout.newscan);
        initializeLayout();
        setSupportActionBar(toolbar);
    }

    private void initializeLayout(){
        beginscan = (Button)findViewById(R.id.beginscan);
        patientId = (EditText)findViewById(R.id.patientID);
        annotation = (EditText)findViewById(R.id.annotation);
        toolbar = (Toolbar)findViewById(R.id.newtoolbar);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
        getWindow().setStatusBarColor(getResources().getColor(R.color.colorPrimaryDark));

        beginscan.setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        switch(v.getId()){
            case R.id.beginscan:
                Intent i = new Intent(getPackageName()+".CALIBRATE");
                startActivity(i);
                break;
        }
    }

    @Override
    public void onPause(){
        super.onPause();
        finish();
    }
}
