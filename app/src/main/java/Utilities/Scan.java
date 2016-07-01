package Utilities;

import android.os.Environment;
import android.util.Log;

import io.github.elgambitero.microspot_android.Scanning;


/**
 * Created by Jaime García Villena "garciavillena.jaime@gmail.com" on 6/29/16.
 */
public class Scan implements Runnable {

    private Double [] xCoord, yCoord;
    private Scanning.ScanningListener newScanListener;
    private final String TAG = "Scanning";
    private String nextPhotoName;
    private String shotName;

    private Thread t = null;

    public Scan(Double [] xCoord, Double [] yCoord, String next, String name, Scanning.ScanningListener scanListener) {
        this.xCoord = xCoord;
        this.yCoord = yCoord;

        this.newScanListener = scanListener;
    }

    public void run() {
        Double incX, incY;
        incX = xCoord[0]-25.0;
        incY = yCoord[0]-7.5;
        int i = 1;
        int j = 1;

        try {
            makeScan(incX,incY,j,i);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public void start() {
        if(t == null) {
            t = new Thread(this, "scanThread");
            t.start();
        }
    }

    private void makeScan(Double xCoord, Double yCoord, Integer xShotNum, Integer yShotNum) throws InterruptedException {

        newScanListener.moveAxisRel(xCoord, yCoord, 2000.0);

        Long waitTime = (long)(Math.sqrt(Math.pow(xCoord,2)+Math.pow(yCoord,2))/0.005);
        try{
            Log.d(TAG,"Waiting for " + waitTime.toString() + " milliseconds");
            Thread.sleep(waitTime+500);
            Log.d(TAG,"Waited");
        }catch (Exception e){
            e.printStackTrace();
        }
        shotName = "/" + xShotNum.toString() + "_" + yShotNum.toString() + ".jpg";
        nextPhotoName = String.valueOf(Environment.getExternalStorageDirectory().getAbsolutePath() + shotName);
    }
}
