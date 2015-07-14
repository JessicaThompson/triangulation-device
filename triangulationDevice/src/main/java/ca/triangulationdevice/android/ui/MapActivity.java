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
import ca.triangulationdevice.android.ui.partial.AudioActivity;
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
public class MapActivity extends AudioActivity {
    @SuppressWarnings("unused")
    private static final String TAG = "MapActivity";
    private static final float DEFAULT_ZOOM = 16;

    private Resources resources;
    private final Map<Marker, User> markerUserMap = new HashMap<>();

    private MapView mapView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Typefaces.loadTypefaces(this);
        setContentView(R.layout.map_activity);
        this.resources = getResources();

        mapView = (MapView) this.findViewById(R.id.mapview);
        UserLocationOverlay myLocationOverlay = new UserLocationOverlay(new GpsLocationProvider(this), mapView);
        myLocationOverlay.enableMyLocation();
        myLocationOverlay.setDrawAccuracyEnabled(true);
        mapView.getOverlays().add(myLocationOverlay);

        this.addUsers();

        LatLng latLng = new LatLng(43.4587014, -80.5506638);
        mapView.setCenter(latLng);
        mapView.setZoom(DEFAULT_ZOOM);

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
    public void onLocationChanged(Location location) {
        super.onLocationChanged(location);

        // Zoom the map to our position!
        LatLng latLng = new LatLng(location.getLatitude(), location.getLongitude());
        mapView.setCenter(latLng);
    }
}
