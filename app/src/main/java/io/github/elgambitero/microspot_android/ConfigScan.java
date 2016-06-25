package io.github.elgambitero.microspot_android;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

/**
 * Created by Jaime García Villena "garciavillena.jaime@gmail.com" on 6/25/16.
 */
public class ConfigScan extends Fragment implements View.OnClickListener{

    EditText intervalX, intervalY, shotsX, shotsY;
    Button calibScan;

    ConfigScanListener newScanListener;

    public interface ConfigScanListener{
        void writeGridDataAndNext(Double intervalX, Double intervalY, Integer shotsX, Integer shotsY);
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        try{
            newScanListener = (ConfigScanListener) context;
        }catch (ClassCastException e){
            e.printStackTrace();
        }
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.scan_config_fragment,container,false);

        initializeLayout(view);

        return view;
    }

    private void initializeLayout(View view){

        intervalX = (EditText) view.findViewById(R.id.intXField);
        intervalY = (EditText) view.findViewById(R.id.intYField);
        shotsX = (EditText) view.findViewById(R.id.shotXField);
        shotsY = (EditText) view.findViewById(R.id.shotYField);

        calibScan = (Button) view.findViewById(R.id.calibScanBut);

        calibScan.setOnClickListener(this);

    }

    @Override
    public void onClick(View view) {
        switch(view.getId()){
            case R.id.calibScanBut:

                Double[] intervals = new Double[2];
                Integer[] shots = new Integer[2];
                String content  = intervalX.getText().toString();
                if(!content.matches("")){
                    intervals[0] = Double.parseDouble(content);
                }else{
                    intervals[0] = 2.0;
                }
                content = intervalY.getText().toString();
                if(!content.matches("")){
                    intervals[1] = Double.parseDouble(content);
                }else{
                    intervals[1] = 5.0;
                }
                content = shotsX.getText().toString();
                if(!content.matches("")){
                    shots[0] = Integer.parseInt(content);
                }else{
                    shots[0] = 25;
                }
                content = shotsY.getText().toString();
                if(!content.matches("")){
                    shots[1] = Integer.parseInt(content);
                }else{
                    shots[1] = 3;
                }
                Double width = shots[0]*intervals[0];
                Double height = shots[1]*intervals[1];
                if((width>50.0)||(height>15.0)){
                    String grid = width.toString() + "x" + height.toString();
                    Toast.makeText(getActivity(), "Grid of " + grid + "doesn't fit the sample. Maximum size is 50x15", Toast.LENGTH_SHORT).show();
                }else{
                    newScanListener.writeGridDataAndNext(intervals[0],intervals[1],shots[0],shots[1]);
                }


        }
    }
}
