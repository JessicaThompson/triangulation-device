package ca.triangulationdevice.android.ui;

import android.app.Activity;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.location.Location;
import android.media.AudioManager;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import ca.triangulationdevice.android.R;
import ca.triangulationdevice.android.TriangulationApplication;
import ca.triangulationdevice.android.model.User;
import ca.triangulationdevice.android.model.UserManager;
import ca.triangulationdevice.android.pd.PDDriver;
import ca.triangulationdevice.android.storage.PathStorage;
import ca.triangulationdevice.android.storage.PathStorage.SaveSessionTask;
import ca.triangulationdevice.android.ui.partial.CompassActivity;
import ca.triangulationdevice.android.util.Typefaces;
import ca.triangulationdevice.android.util.VolumeLevelObserver;

import com.mapbox.mapboxsdk.api.ILatLng;
import com.mapbox.mapboxsdk.geometry.LatLng;
import com.mapbox.mapboxsdk.overlay.GpsLocationProvider;
import com.mapbox.mapboxsdk.overlay.Marker;
import com.mapbox.mapboxsdk.overlay.UserLocationOverlay;
import com.mapbox.mapboxsdk.views.MapView;
import com.mapbox.mapboxsdk.views.MapViewListener;
import com.mapbox.mapboxsdk.views.util.constants.MapViewConstants;

import java.util.HashMap;
import java.util.Map;

/**
 * An {@link Activity} which lets the user create sessions, connect with other
 * devices, and see the current status of the other connected user.
 */
public class MapActivity extends CompassActivity {
    @SuppressWarnings("unused")
    private static final String TAG = "MapActivity";
    private static final float DEFAULT_ZOOM = 19;

    private Resources resources;

    private final boolean recording = false;
    private final PathStorage path = new PathStorage(this);
    private final PDDriver pd = new PDDriver(this);

    private final Map<Marker, User> markerUserMap = new HashMap<>();

    private long lastCompassUpdate = 0;
    private float lastCompass = 0;
    private VolumeLevelObserver settingsContentObserver;

    private ProgressDialog savingDialog;
    private SaveSessionTask saveTask;
    private MapView mapView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        this.saveTask = this.path.new SaveSessionTask() {
            @Override
            protected void onPostExecute(Void result) {
                MapActivity.this.savingDialog.dismiss();
            }
        };

        Typefaces.loadTypefaces(this);
        setContentView(R.layout.map_activity);
        this.resources = getResources();

//        this.pd.initServices();
//        this.pd.setListener(new Listener() {
//            @Override
//            public void myFrequencyChanged(int wave_index, float newFrequency) {
//            }

//            @Override
//            public void theirFrequencyChanged(int wave_index, float newFrequency) {
//            }
//        });

        // Add a listener for volume changes.
        this.settingsContentObserver = new VolumeLevelObserver(this, new VolumeLevelObserver.Listener() {
            @Override
            public void onVolumeChanged(int newVolume) {
                double ratio = newVolume / 15d;
            }
        }, AudioManager.STREAM_MUSIC);
        int currentVolume = this.settingsContentObserver.getCurrent();
        double ratio = currentVolume / 15d;

        mapView = (MapView) this.findViewById(R.id.mapview);
        UserLocationOverlay myLocationOverlay = new UserLocationOverlay(new GpsLocationProvider(this), mapView);
        myLocationOverlay.enableMyLocation();
        myLocationOverlay.setDrawAccuracyEnabled(true);
        mapView.getOverlays().add(myLocationOverlay);

        this.addUsers();

        LatLng latLng = new LatLng(43.4587014, -80.5506638);
        mapView.setCenter(latLng);
        mapView.setZoom(16);

        mapView.setMapViewListener(new MapViewListener() {
            @Override
            public void onShowMarker(MapView mapView, Marker marker) {

            }

            @Override
            public void onHideMarker(MapView mapView, Marker marker) {

            }

            @Override
            public void onTapMarker(MapView mapView, Marker marker) {
                Intent intent = new Intent(MapActivity.this, ProfileActivity.class);
                intent.putExtra(ProfileActivity.EXTRA_USER_ID, markerUserMap.get(marker).id);
                startActivity(intent);
            }

            @Override
            public void onLongPressMarker(MapView mapView, Marker marker) {

            }

            @Override
            public void onTapMap(MapView mapView, ILatLng iLatLng) {

            }

            @Override
            public void onLongPressMap(MapView mapView, ILatLng iLatLng) {

            }
        });
    }

    private void addUsers() {
        for (User user : application.userManager.values()) {
            this.addUser(user);
        }
    }

    private void addUser(User user) {
        Marker marker = new UserMarker(mapView, user, resources);
        this.markerUserMap.put(marker, user);
        mapView.addMarker(marker);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        getApplicationContext().getContentResolver().unregisterContentObserver(this.settingsContentObserver);
//        this.pd.close();
    }

    @Override
    public void onLocationChanged(Location location) {
        super.onLocationChanged(location);

        // Zoom the map to our position!
        LatLng latLng = new LatLng(location.getLatitude(), location.getLongitude());
        mapView.setCenter(latLng);

        if (this.recording) {
            float[] lastOrientation = this.getLastOrientation();
//            this.path.addMine(location, lastOrientation[0], lastOrientation[1], lastOrientation[2]);
        }

//        this.pd.myLocationChanged(location);
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
//        if (this.otherMarker != null) {
//            this.otherMarker.setPosition(latLng);
//        } else {
//            this.otherMarker = this.map.addMarker(new MarkerOptions().position(latLng)
//                    .icon(BitmapDescriptorFactory.fromResource(R.drawable.paired_device)));
//        }

        if (this.recording) {
//            this.path.addTheirs(location);
        }

//        this.pd.theirLocationChanged(location);
    }

    @Override
    protected void onResume() {
        super.onResume();
//        this.path.open();
    }

    @Override
    protected void onStop() {
//        this.path.close();
        super.onStop();
    }

    @Override
    protected void onCompassChanged(float azimuth, float pitch, float roll) {
//        this.pd.pdChangeGyroscope(azimuth, pitch, roll);
//        this.path.addMine(getLocation(), azimuth, pitch, roll);

        if (System.currentTimeMillis() - this.lastCompassUpdate > 200) {
            this.lastCompassUpdate = System.currentTimeMillis();
            this.lastCompass = azimuth;

            Location currentLocation = getLocation();
            LatLng currentLatLng = new LatLng(currentLocation.getLatitude(), currentLocation.getLongitude());
        }
    }
}
