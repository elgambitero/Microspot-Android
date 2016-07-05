package Services;


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
import java.util.HashMap;
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

    private String TAG = "SerialService";

    private UsbDevice device;
    private UsbDeviceConnection usbConnection;
    private UsbManager usbManager;
    private UsbSerialDevice serial;
    boolean connected;

    private ConcurrentLinkedQueue<String> inData = new ConcurrentLinkedQueue<String>();


    UsbSerialInterface.UsbReadCallback mCallback = new UsbSerialInterface.UsbReadCallback() { //Defining a Callback which triggers whenever data is read.
        @Override
        public void onReceivedData(byte[] arg0) {
            String data = null;
            try {
                data = new String(arg0, "UTF-8");
                data.concat("/r/n");
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }
            inData.add(data);

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

        public int moveAxis(Double x, Double y, Double speed){

            serial.write("g90\r\n".getBytes());
            String command = "g1 x" + x.toString() + " y" + y.toString() + " f" + speed.toString() + "\r\n";
            serial.write(command.getBytes());
            //Maybe check if something went wrong?
            return 0;

        }

        public long moveAxisRel(Double x, Double y, Double speed){

            serial.write("g91\r\n".getBytes());
            String command = "g1 x" + x.toString() + " y" + y.toString() + " f" + speed.toString() + "\r\n";
            serial.write(command.getBytes());
            long waitTime = (long) (Math.sqrt(Math.pow(x,2)+Math.pow(y,2))/0.005);
            if(sanityCheck()){ //Check if something went wrong
                return waitTime;
            }else{
                return -1;
            }

        }

        public int homeAxis(){

            serial.write("$h\r\n".getBytes());
            if(sanityCheck()){ //Check if something went wrong
                return 0;
            }else{
                return -1;
            }

        }

        public void start(){
            if(!connected){
                try {
                    initializeSerial();
                }catch (Exception e ){
                    e.printStackTrace();
                }
            }
        }

        public void close(){

            serial.close();
            connected = false;

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
            return false;
        }else{
            serial.read(mCallback);
            if(inData.element().contains("ok")){
                return true;
            }else{
                return false;
            }
        }
    }

}
