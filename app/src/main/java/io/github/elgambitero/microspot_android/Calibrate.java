package io.github.elgambitero.microspot_android;

import android.os.Bundle;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;

/**
 * Created by elgambitero on 02/01/16.
 */
public class Calibrate extends AppCompatActivity implements View.OnClickListener{

    Button topLeft,topRight, bottomLeft, bottomRight;
    Toolbar calibToolbar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_calibrate);
        initializeVars();
        setSupportActionBar(calibToolbar);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
        getWindow().setStatusBarColor(getResources().getColor(R.color.colorPrimaryDark));
    }

    private void initializeVars(){
        topLeft = (Button)findViewById(R.id.TopLeft);
        topRight = (Button)findViewById(R.id.TopRight);
        bottomLeft = (Button)findViewById(R.id.BottomLeft);
        bottomRight = (Button)findViewById(R.id.BottomRight);
        calibToolbar = (Toolbar)findViewById(R.id.calibtoolbar);
        topLeft.setOnClickListener(this);
        topRight.setOnClickListener(this);
        bottomLeft.setOnClickListener(this);
        bottomRight.setOnClickListener(this);

    }


    @Override
    public void onClick(View v) {
        switch(v.getId()){
            default:
                Snackbar.make(v, "Axis movement is not yet implemented",
                        Snackbar.LENGTH_LONG).setAction("Action", null).show();
        }
    }
}
