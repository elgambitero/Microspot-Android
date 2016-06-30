package Utilities;

import android.content.Context;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CaptureResult;
import android.hardware.camera2.DngCreator;
import android.media.Image;
import android.media.MediaScannerConnection;
import android.os.AsyncTask;
import android.widget.Toast;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.ref.WeakReference;

/**
 * Created by Jaime García Villena "garciavillena.jaime@gmail.com" on 6/30/16.
 */
public class SaveRawTask extends AsyncTask<Void, Void, Boolean> {

    private WeakReference<Context> mContextRef;
    private File mFile;
    private Image mImage;
    private DngCreator mDngCreator;

    public SaveRawTask(Context context, String dir, Image image, CameraCharacteristics characteristics, CaptureResult metadata) {
        mContextRef = new WeakReference<>(context);
        mFile = new File(dir, System.currentTimeMillis() + ".dng");
        mImage = image;
        mDngCreator = new DngCreator(characteristics, metadata);
    }

    @Override
    protected Boolean doInBackground(Void... params) {
        try {
            mDngCreator.writeImage(new FileOutputStream(mFile), mImage);
            mDngCreator.close();
            mImage.close();
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    @Override
    protected void onPostExecute(Boolean result) {
        Context context = mContextRef.get();
        if (context != null) {
            if (result) {
                MediaScannerConnection.scanFile(context, new String[]{mFile.getAbsolutePath()}, null, null);
                Toast.makeText(context, "Image captured!", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(context, "Error saving image", Toast.LENGTH_LONG).show();
            }
        }
    }
}