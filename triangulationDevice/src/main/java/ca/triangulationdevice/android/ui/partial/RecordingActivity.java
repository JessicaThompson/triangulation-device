package ca.triangulationdevice.android.ui.partial;

import android.app.Activity;
import android.app.ProgressDialog;
import android.location.Location;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;

import com.mapbox.mapboxsdk.geometry.LatLng;

import java.io.IOException;
import java.util.Date;

import ca.triangulationdevice.android.ipc.NetworkIPCService;
import ca.triangulationdevice.android.model.Path;
import ca.triangulationdevice.android.model.Session;
import ca.triangulationdevice.android.model.User;
import ca.triangulationdevice.android.pd.Triangulation2Driver;
import ca.triangulationdevice.android.util.VolumeLevelObserver;

/**
 * An {@link Activity} which lets the user create sessions, connect with other
 * devices, and see the current status of the other connected user.
 */
public abstract class RecordingActivity extends StepCounterActivity {
    @SuppressWarnings("unused")
    private static final String TAG = "RecordingActivity";

    protected boolean recording = false;
    protected Triangulation2Driver pd;

    private Session session;

    private long lastCompassUpdate = 0;
    private float lastCompass = 0;
    private VolumeLevelObserver settingsContentObserver;

    private NetworkIPCService<Location> transferService;
    private Handler transferHandler;

    private ProgressDialog savingDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Add a listener for volume changes.
//        this.settingsContentObserver = new VolumeLevelObserver(this, new VolumeLevelObserver.Listener() {
//            @Override
//            public void onVolumeChanged(int newVolume) {
//                double ratio = newVolume / 15d;
//            }
//        }, AudioManager.STREAM_MUSIC);
//        int currentVolume = this.settingsContentObserver.getCurrent();
//        double ratio = currentVolume / 15d;

        transferHandler = new Handler();
        transferService = new NetworkIPCService<>(transferHandler, Location.CREATOR);

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
            this.session.paths.get(Path.MINE).addPoint(location, lastOrientation[0], lastOrientation[1], lastOrientation[2]);
            this.pd.myLocationChanged(location);
        }
    }

    @Override
    protected void onCompassChanged(float azimuth, float pitch, float roll) {
        if (this.recording) {
            this.session.paths.get(Path.MINE).addPoint(getLocation(), azimuth, pitch, roll);

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

    protected void startRecording(User user) {
        this.recording = true;
        this.session = new Session();
        this.session.ownerId = user.id;
        this.session.paths.set(Path.MINE, new Path());
        this.session.paths.set(Path.THEIRS, new Path());
    }

    protected void stopRecording() {
        this.recording = false;
        this.session.saved = new Date();
    }
}
