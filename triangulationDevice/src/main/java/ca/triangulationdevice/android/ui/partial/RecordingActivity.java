package ca.triangulationdevice.android.ui.partial;

import android.app.Activity;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothDevice;
import android.location.Location;
import android.media.AudioManager;
import android.os.Bundle;
import android.util.Log;

import com.mapbox.mapboxsdk.geometry.LatLng;

import java.io.IOException;

import ca.triangulationdevice.android.pd.PDDriver;
import ca.triangulationdevice.android.pd.Triangulation2Driver;
import ca.triangulationdevice.android.storage.PathStorage;
import ca.triangulationdevice.android.storage.PathStorage.SaveSessionTask;
import ca.triangulationdevice.android.util.VolumeLevelObserver;

/**
 * An {@link Activity} which lets the user create sessions, connect with other
 * devices, and see the current status of the other connected user.
 */
public abstract class RecordingActivity extends StepCounterActivity {
    @SuppressWarnings("unused")
    private static final String TAG = "RecordingActivity";

    protected boolean recording = false;
    private final PathStorage path = new PathStorage(this);
    protected Triangulation2Driver pd;

    private long lastCompassUpdate = 0;
    private float lastCompass = 0;
    private VolumeLevelObserver settingsContentObserver;

    private ProgressDialog savingDialog;
    private SaveSessionTask saveTask;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        this.saveTask = this.path.new SaveSessionTask() {
            @Override
            protected void onPostExecute(Void result) {
                RecordingActivity.this.savingDialog.dismiss();
            }
        };

        // Add a listener for volume changes.
//        this.settingsContentObserver = new VolumeLevelObserver(this, new VolumeLevelObserver.Listener() {
//            @Override
//            public void onVolumeChanged(int newVolume) {
//                double ratio = newVolume / 15d;
//            }
//        }, AudioManager.STREAM_MUSIC);
//        int currentVolume = this.settingsContentObserver.getCurrent();
//        double ratio = currentVolume / 15d;

        try {
            this.pd = new Triangulation2Driver(this);
            this.pd.initServices();
        } catch (IOException exception) {
            Log.e(TAG, exception.getMessage());
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
//        getApplicationContext().getContentResolver().unregisterContentObserver(this.settingsContentObserver);
        this.pd.close();
    }

    @Override
    public void onLocationChanged(Location location) {
        super.onLocationChanged(location);

        if (this.recording) {
            float[] lastOrientation = this.getLastOrientation();
            this.path.addMine(location, lastOrientation[0], lastOrientation[1], lastOrientation[2]);
            this.pd.myLocationChanged(location);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        this.path.open();
    }

    @Override
    protected void onStop() {
        this.path.close();
        super.onStop();
    }

    @Override
    protected void onCompassChanged(float azimuth, float pitch, float roll) {
        if (this.recording) {
            this.path.addMine(getLocation(), azimuth, pitch, roll);

            if (System.currentTimeMillis() - this.lastCompassUpdate > 200) {
                this.lastCompassUpdate = System.currentTimeMillis();
                this.lastCompass = azimuth;

                Location currentLocation = getLocation();
                LatLng currentLatLng = new LatLng(currentLocation.getLatitude(), currentLocation.getLongitude());
            }
        }
    }

    @Override
    protected void onStepCountChanged(float freq) {
        if (this.recording) {
            this.pd.myStepCountChanged(freq);
        }
    }
}
