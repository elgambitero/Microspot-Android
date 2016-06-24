package io.github.elgambitero.microspot_android;

import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.hardware.Camera;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbManager;
import android.os.Bundle;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;

import com.felhr.usbserial.UsbSerialDevice;
import com.felhr.usbserial.UsbSerialInterface;


import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by Jaime García Villena "garciavillena.jaime@gmail.com" on 6/21/16.
 */

public class ControlPanel extends AppCompatActivity implements View.OnClickListener{

    //Frame layout related
    Toolbar controlPanelToolbar;
    FrameLayout framePreview;
    CameraPreview preview;
    ImageView nocamview;
    Camera mCamera;

    //Buttons
    Button xPlusButton;
    Button yPlusButton;
    Button xMinusButton;
    Button yMinusButton;
    Button startSerialButton;
    Button homeAxisButton;
    Button stopSerialButton;

    //Serial related

    public final String ACTION_USB_PERMISSION = "com.hariharan.arduinousb.USB_PERMISSION";

    UsbDevice device;
    UsbDeviceConnection usbConnection;
    UsbManager usbManager;
    UsbSerialDevice serial;


    UsbSerialInterface.UsbReadCallback mCallback = new UsbSerialInterface.UsbReadCallback() { //Defining a Callback which triggers whenever data is read.
        @Override
        public void onReceivedData(byte[] arg0) {
            String data;
            try {
                data = new String(arg0, "UTF-8");
                data.concat("/r/n");
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }


        }
    };


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_control_panel);

        IntentFilter filter = new IntentFilter();
        filter.addAction(ACTION_USB_PERMISSION);
        filter.addAction(UsbManager.ACTION_USB_DEVICE_ATTACHED);
        filter.addAction(UsbManager.ACTION_USB_DEVICE_DETACHED);
        registerReceiver(broadcastReceiver, filter);
    }


    @Override
    protected void onResume() {
        super.onResume();

        initializeLayout();

        mCamera = initializeCamera();
        if(mCamera!=null){
            setCamFocusMode();
            preview = new CameraPreview(this,mCamera);
            framePreview.addView(preview);
        }else{

            nocamview.setImageResource(R.drawable.errorsign);
            framePreview.addView(nocamview);
        }
    }

    private void initializeLayout(){
        controlPanelToolbar = (Toolbar)findViewById(R.id.controlpaneltoolbar);
        framePreview = (FrameLayout)findViewById(R.id.camera_preview);
        nocamview = new ImageView(this);

        xPlusButton = (Button)findViewById(R.id.X_plus);
        yPlusButton = (Button)findViewById(R.id.Y_plus);
        xMinusButton = (Button)findViewById(R.id.X_minus);
        yMinusButton = (Button)findViewById(R.id.Y_minus);
        startSerialButton = (Button)findViewById(R.id.start_serial);
        homeAxisButton = (Button)findViewById(R.id.home_axis);
        stopSerialButton = (Button)findViewById(R.id.stop_serial);

        xPlusButton.setOnClickListener(this);
        yPlusButton.setOnClickListener(this);
        xMinusButton.setOnClickListener(this);
        yMinusButton.setOnClickListener(this);
        startSerialButton.setOnClickListener(this);
        homeAxisButton.setOnClickListener(this);
        stopSerialButton.setOnClickListener(this);

        setSupportActionBar(controlPanelToolbar);
    }

    private Camera initializeCamera(){
        if(checkCameraHardware(this)){
            Camera cam;
            int cameraId = -1;
            int numberOfCameras = Camera.getNumberOfCameras();
            for (int i = 0; i < numberOfCameras; i++) {
                Camera.CameraInfo info = new Camera.CameraInfo();
                Camera.getCameraInfo(i, info);
                if (info.facing == Camera.CameraInfo.CAMERA_FACING_BACK) {
                    cameraId = i;
                    break;
                }
            }
            cam = getCameraInstance(cameraId);
            if (cam == null){
                Log.println(1, "Err", "NO CAMERA INSTANCED");
            }
            return cam;
        }else{
            return null;
        }

    }

    /** Check if this device has a camera */
    private boolean checkCameraHardware(Context context) {
        if (context.getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA)){
            // this device has a camera
            return true;
        } else {
            // no camera on this device
            return false;
        }
    }

    public static Camera getCameraInstance(int i){
        Camera c = null;
        try {
            c = Camera.open();
            // attempt to get a Camera instance
        }
        catch (Exception e){
            // Camera is not available (in use or does not exist)
        }
        return c; // returns null if camera is unavailable
    }

    private void setCamFocusMode(){

        if(null == mCamera) {
            return;
        }

    /* Set Auto focus */
        Camera.Parameters parameters = mCamera.getParameters();
        List<String> focusModes = parameters.getSupportedFocusModes();
        if(focusModes.contains(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE)){
            parameters.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE);
        } else
        if(focusModes.contains(Camera.Parameters.FOCUS_MODE_AUTO)){
            parameters.setFocusMode(Camera.Parameters.FOCUS_MODE_AUTO);
        }

        mCamera.setParameters(parameters);
    }



    public void initializeSerial() throws InterruptedException {

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
                    break;
            }
        }

    }

    @Override
    public void onClick(View v) {
        switch(v.getId()){
            default:
                Snackbar.make(v, "NOT IMPLEMENTED YET.",
                        Snackbar.LENGTH_LONG).setAction("Action", null).show();
                break;
            case R.id.start_serial:
                try {
                    initializeSerial();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                break;
            case R.id.home_axis:
                serial.write("$h\r\n".getBytes());
                break;
            case R.id.stop_serial:
                serial.close();
                break;
            case R.id.Y_minus:
                serial.write("G91\r\n".getBytes());
                serial.write("g0 y-10\r\n".getBytes());
                break;
            case R.id.Y_plus:
                serial.write("g91\r\n".getBytes());
                serial.write("g0 y10\r\n".getBytes());
                break;
            case R.id.X_minus:
                serial.write("g91\r\n".getBytes());
                serial.write("g0 x-10\r\n".getBytes());
                break;
            case R.id.X_plus:
                serial.write("g91\r\n".getBytes());
                serial.write("g0 x10\r\n".getBytes());
                break;
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
                            serial.read(mCallback); //


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
                onClick(startSerialButton);
            } else if (intent.getAction().equals(UsbManager.ACTION_USB_DEVICE_DETACHED)) {
                onClick(stopSerialButton);
            }
        };
    };

}

