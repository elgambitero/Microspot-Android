package Utilities;

import android.content.Context;
import android.media.Image;
import android.media.MediaScannerConnection;
import android.os.AsyncTask;
import android.widget.Toast;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.nio.ByteBuffer;

/**
 * Created by Jaime García Villena "garciavillena.jaime@gmail.com" on 6/30/16.
 */
public class SaveJpegTask extends AsyncTask<Void, Void, Boolean> {

    private File mFile;
    private Image mImage;
    private WeakReference<Context> mContextRef;


    public SaveJpegTask(Context context, String filename, Image image) {
        mContextRef = new WeakReference<>(context);
        mFile = new File(filename);
        mImage = image;
    }

    @Override
    protected Boolean doInBackground(Void... params) {
        ByteBuffer buffer = mImage.getPlanes()[0].getBuffer();
        byte[] bytes = new byte[buffer.capacity()];
        buffer.get(bytes);
        mImage.close();
        try {
            new FileOutputStream(mFile).write(bytes);
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
                //finishedPhoto(result);
            } else {
                Toast.makeText(context, "Error saving image", Toast.LENGTH_LONG).show();
            }
        }
    }
}