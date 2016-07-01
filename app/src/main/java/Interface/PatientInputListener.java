package Interface;

import android.hardware.camera2.CameraAccessException;

/**
 * Created by Jaime García Villena "garciavillena.jaime@gmail.com" on 6/29/16.
 */
public interface PatientInputListener {
    void writePatientDataAndNext(String id, String annotation) throws CameraAccessException;
}
