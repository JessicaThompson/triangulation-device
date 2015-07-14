package ca.triangulationdevice.android.ui.partial;

import android.app.Activity;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.content.res.Resources;
import android.location.Location;
import android.media.AudioManager;
import android.os.Bundle;

import com.mapbox.mapboxsdk.api.ILatLng;
import com.mapbox.mapboxsdk.geometry.LatLng;
import com.mapbox.mapboxsdk.overlay.GpsLocationProvider;
import com.mapbox.mapboxsdk.overlay.Marker;
import com.mapbox.mapboxsdk.overlay.UserLocationOverlay;
import com.mapbox.mapboxsdk.views.MapView;
import com.mapbox.mapboxsdk.views.MapViewListener;

import java.util.HashMap;
import java.util.Map;

import ca.triangulationdevice.android.R;
import ca.triangulationdevice.android.model.User;
import ca.triangulationdevice.android.pd.PDDriver;
import ca.triangulationdevice.android.storage.PathStorage;
import ca.triangulationdevice.android.storage.PathStorage.SaveSessionTask;
import ca.triangulationdevice.android.ui.ProfileActivity;
import ca.triangulationdevice.android.ui.UserMarker;
import ca.triangulationdevice.android.util.Typefaces;
import ca.triangulationdevice.android.util.VolumeLevelObserver;

/**
 * An {@link Activity} which lets the user create sessions, connect with other
 * devices, and see the current status of the other connected user.
 */
public abstract class AudioActivity extends CompassActivity {
    @SuppressWarnings("unused")
    private static final String TAG = "AudioActivity";

    private final boolean recording = false;
    private final PathStorage path = new PathStorage(this);
    private final PDDriver pd = new PDDriver(this);

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
                AudioActivity.this.savingDialog.dismiss();
            }
        };

        this.pd.initServices();
        this.pd.setListener(new PDDriver.Listener() {
            @Override
            public void myFrequencyChanged(int wave_index, float newFrequency) {
            }

            @Override
            public void theirFrequencyChanged(int wave_index, float newFrequency) {
            }
        });

        // Add a listener for volume changes.
        this.settingsContentObserver = new VolumeLevelObserver(this, new VolumeLevelObserver.Listener() {
            @Override
            public void onVolumeChanged(int newVolume) {
                double ratio = newVolume / 15d;
            }
        }, AudioManager.STREAM_MUSIC);
        int currentVolume = this.settingsContentObserver.getCurrent();
        double ratio = currentVolume / 15d;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        getApplicationContext().getContentResolver().unregisterContentObserver(this.settingsContentObserver);
        this.pd.close();
    }

    @Override
    public void onLocationChanged(Location location) {
        super.onLocationChanged(location);

        if (this.recording) {
            float[] lastOrientation = this.getLastOrientation();
            this.path.addMine(location, lastOrientation[0], lastOrientation[1], lastOrientation[2]);
        }

        this.pd.myLocationChanged(location);
    }

    /**
     * Called when the connected {@link BluetoothDevice} sends us a new Location
     * value.
     * 
     * @param location
     *            The new value for the connected other device's location.
     */
    public void theirLocationChanged(Location location) {
        LatLng latLng = new LatLng(location.getLatitude(), location.getLongitude());

        if (this.recording) {
            this.path.addTheirs(location);
        }

        this.pd.theirLocationChanged(location);
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
        this.pd.pdChangeGyroscope(azimuth, pitch, roll);
        this.path.addMine(getLocation(), azimuth, pitch, roll);

        if (System.currentTimeMillis() - this.lastCompassUpdate > 200) {
            this.lastCompassUpdate = System.currentTimeMillis();
            this.lastCompass = azimuth;

            Location currentLocation = getLocation();
            LatLng currentLatLng = new LatLng(currentLocation.getLatitude(), currentLocation.getLongitude());
        }
    }
}
