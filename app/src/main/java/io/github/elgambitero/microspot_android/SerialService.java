package io.github.elgambitero.microspot_android;


import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbManager;
import android.os.Binder;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.util.Log;
import android.widget.Toast;

import com.felhr.usbserial.UsbSerialDevice;
import com.felhr.usbserial.UsbSerialInterface;

import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Created by Jaime García Villena "garciavillena.jaime@gmail.com" on 6/24/16.
 * Licensed under the MIT license (check MIT_LICENSE file for more info)
 * Based on @felHR85 UsbSerial library
 */

public class SerialService extends Service {

    public final String ACTION_USB_PERMISSION = "com.hariharan.arduinousb.USB_PERMISSION";

    private final IBinder serialBinder = new SerialBinder();

    private static double xMax = 50.0;
    private static double yMax = 15.0;

    private String TAG = "SerialService";

    private UsbDevice device;
    private UsbDeviceConnection usbConnection;
    private UsbManager usbManager;
    private UsbSerialDevice serial;
    boolean connected;

    //this variable holds the position of the carriage.
    private double[] position = {0.0,0.0};

    private String data = "";

    UsbSerialInterface.UsbReadCallback mCallback = new UsbSerialInterface.UsbReadCallback() { //Defining a Callback which triggers whenever data is read.
        @Override
        public void onReceivedData(byte[] arg0) {
            if(arg0.length != 0) {
                try {
                    data = new String(arg0, StandardCharsets.UTF_8);

                } catch (Exception e) {
                    e.printStackTrace();
                }
                Log.d(TAG, "Microspot message: " + data);
            }

        }
    };



    /*=========================
    * Service lifecycle methods
    =========================*/

    @Override
    public void onCreate() {

        super.onCreate();

        //Ask for permission to use the USB
        try {
            IntentFilter filter = new IntentFilter();
            filter.addAction(ACTION_USB_PERMISSION);
            filter.addAction(UsbManager.ACTION_USB_DEVICE_ATTACHED);
            filter.addAction(UsbManager.ACTION_USB_DEVICE_DETACHED);
            registerReceiver(broadcastReceiver, filter);
        }catch (Exception e){
            e.printStackTrace();
            Toast.makeText(getApplicationContext(), "Permissions must be granted to connect to MicroSpot", Toast.LENGTH_LONG).show();
            stopSelf();
        }

        connected = false;
        try{
            initializeSerial();
        }catch (InterruptedException e) {
            Toast.makeText(getApplicationContext(), "MicroSpot not connected", Toast.LENGTH_LONG).show();
            e.printStackTrace();
        }

    }

    @Override
    public void onDestroy() {
        unregisterReceiver(broadcastReceiver);
        super.onDestroy();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        Log.d(TAG,"onBind called");
        return serialBinder;
    }



    /*=================
    * Service interface
    =================*/

    public class SerialBinder extends Binder {

        public void start(){
            if(!connected){
                try {
                    initializeSerial();
                }catch (Exception e ){
                    e.printStackTrace();
                }
            }
        }

        public void reset() {

            try{
                serial.close();
            }catch (Exception e)
            {
                e.printStackTrace();;
            }

            try {
                initializeSerial();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }



        }

        public long moveAxis(Double x, Double y, Double speed){

            //Saturate values not to go outside boundaries
            x = Math.max(Math.min(xMax,x),0.0);
            y = Math.max(Math.min(yMax,y),0.0);

            if(sanityCheck()){
                //Log.d(TAG,"Moving axis to (" + x.toString() + "," + y.toString() + ")");
                serial.write("g90\r\n".getBytes());
                String command = "g1 x" + x.toString() + " y" + y.toString() + " f" + speed.toString() + "\r\n";
                serial.write(command.getBytes());
                long waitTime = (long) (Math.sqrt(Math.pow(x-position[0],2)+Math.pow(y-position[1],2))/0.005);
                position = new double[] {x, y};
                return waitTime;
            }else{
                return -1;
            }

        }

        public long moveAxisRel(Double x, Double y, Double speed){

            //Saturate the increment so the MicroSpot doesn't go outside boundaries.
            x = Math.max(Math.min(xMax,x + position[0]),0.0) - position[0];
            y = Math.max(Math.min(yMax,y + position[1]),0.0) - position[1];

            if(x == 0.0 && y == 0.0){
                return 0;
            }

            if(sanityCheck()){ //Check if something went wrong
                //Log.d(TAG,"Moving axis by (" + x.toString() + "," + y.toString() + ")");
                serial.write("g91\r\n".getBytes());
                String command = "g1 x" + x.toString() + " y" + y.toString() + " f" + speed.toString() + "\r\n";
                serial.write(command.getBytes());

                long waitTime = (long) (Math.sqrt(Math.pow(x,2)+Math.pow(y,2))/0.005);
                position[0] += x;
                position[1] += y;
                return waitTime;
            }else{
                return -1;
            }

        }

        public int homeAxis(){
            
            if(sanityCheck()){ //Check if something went wrong
                //Log.d(TAG,"Homing axis");
                serial.write("$h\r\n".getBytes());
                position = new double[] {0.0,0.0};

                return 0;
            }else{
                return -1;
            }

        }

        public void close(){

            if(sanityCheck()){
                serial.close();
                connected = false;

            }

        }

        public int setLight(Integer value){

            if(sanityCheck() && value <= 255 && value >=0){
                String command = "S" + value.toString() + "\r\n";
                serial.write("M03\r\n".getBytes());
                serial.write(command.getBytes());
                return 0;
            }else{
                return -1;
            }

        }

        public double[] getPosition(){
            return position;
        }

    }





    /*======================
    * Serial related methods
    * ======================*/

    private void initializeSerial() throws InterruptedException {

        usbManager = (UsbManager) getSystemService(Context.USB_SERVICE);

        HashMap<String, UsbDevice> usbDevices = usbManager.getDeviceList();
        if(!usbDevices.isEmpty())
        {
            boolean keep = true;
            for(Map.Entry<String, UsbDevice> entry : usbDevices.entrySet())
            {
                device = entry.getValue();
                int deviceVID = device.getVendorId();
                int devicePID = device.getProductId();
                if(deviceVID != 0x1d6b || (devicePID != 0x0001 || devicePID != 0x0002 || devicePID != 0x0003))
                {
                    // We are supposing here there is only one device connected and it is our serial device
                    PendingIntent pi = PendingIntent.getBroadcast(this, 0,
                            new Intent(ACTION_USB_PERMISSION), 0);
                    usbManager.requestPermission(device, pi);
                    keep = false;
                }
                else
                {
                    usbConnection = null;
                    device = null;
                }

                if(!keep)
                    Toast.makeText(getApplicationContext(), "Phone connected to MicroSpot", Toast.LENGTH_LONG).show();
                    break;
            }
        }

    }



    private final BroadcastReceiver broadcastReceiver = new BroadcastReceiver() { //Broadcast Receiver to automatically start and stop the Serial connection.
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(ACTION_USB_PERMISSION)) {
                boolean granted =
                        intent.getExtras().getBoolean(UsbManager.EXTRA_PERMISSION_GRANTED);
                if (granted) {
                    usbConnection = usbManager.openDevice(device);
                    serial = UsbSerialDevice.createUsbSerialDevice(device, usbConnection);
                    if (serial != null) {
                        if (serial.open()) { //Set Serial Connection Parameters.
                            serial.setBaudRate(115200);
                            serial.setDataBits(UsbSerialInterface.DATA_BITS_8);
                            serial.setStopBits(UsbSerialInterface.STOP_BITS_1);
                            serial.setParity(UsbSerialInterface.PARITY_NONE);
                            serial.setFlowControl(UsbSerialInterface.FLOW_CONTROL_OFF);
                            serial.read(mCallback);
                            connected = true;

                        } else {
                            Log.d("SERIAL", "PORT NOT OPEN");
                        }
                    } else {
                        Log.d("SERIAL", "PORT IS NULL");
                    }
                } else {
                    Log.d("SERIAL", "PERM NOT GRANTED");
                }
            } else if (intent.getAction().equals(UsbManager.ACTION_USB_DEVICE_ATTACHED)) {
                try {
                    initializeSerial();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            } else if (intent.getAction().equals(UsbManager.ACTION_USB_DEVICE_DETACHED)) {
                serial.close();
                connected = false;
            }
        }
    };

    private boolean sanityCheck(){

        if(!connected){
            Log.d(TAG,"Sanity check failed");
            return false;
        }else{
            //serial.read(mCallback);
            return true;
        }
    }

}
