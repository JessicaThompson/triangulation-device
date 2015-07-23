package ca.triangulationdevice.android.ui;

import android.app.Activity;
import android.content.Intent;
import android.content.res.Resources;
import android.location.Location;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

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
import ca.triangulationdevice.android.ui.marker.UserMarker;
import ca.triangulationdevice.android.ui.partial.CompassActivity;
import ca.triangulationdevice.android.util.Typefaces;

/**
 * An {@link Activity} which lets the user create sessions, connect with other
 * devices, and see the current status of the other connected user.
 */
public class BrowseUserActivity extends CompassActivity {
    @SuppressWarnings("unused")
    private static final String TAG = "BrowseUserActivity";
    private static final float DEFAULT_ZOOM = 16;

    private Resources resources;
    private final Map<Marker, User> markerUserMap = new HashMap<>();

    private MapView mapView;
    private ViewGroup miniPlayer;
    private ViewGroup miniProfile;

    private ImageView miniProfileImage;
    private TextView miniName;
    private TextView miniLocation;
    private TextView miniDescription;

    private User currentUser;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Typefaces.loadTypefaces(this);
        setContentView(R.layout.map_activity);
        this.resources = getResources();

        mapView = (MapView) this.findViewById(R.id.mapview);
        miniPlayer = (ViewGroup) findViewById(R.id.mini_playback);
        miniProfile = (ViewGroup) findViewById(R.id.mini_profile);

        miniProfileImage = (ImageView) miniProfile.findViewById(R.id.mini_profile_image);
        miniName = (TextView) miniProfile.findViewById(R.id.mini_name);
        miniLocation = (TextView) miniProfile.findViewById(R.id.mini_location);
        miniDescription = (TextView) miniProfile.findViewById(R.id.mini_walk_description);

        // Setup the map and user overlay.
        UserLocationOverlay myLocationOverlay = new UserLocationOverlay(new GpsLocationProvider(this), mapView);
        myLocationOverlay.enableMyLocation();
        myLocationOverlay.setDrawAccuracyEnabled(true);
        mapView.getOverlays().add(myLocationOverlay);

        // Add the existing users to the map overlay.
        this.addUsers();

        // Center our map.
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
                User tappedUser = markerUserMap.get(marker);
                if (tappedUser == currentUser) {
                    openProfile(tappedUser);
                } else {
                    showMiniProfile(tappedUser);
                }
            }

            @Override
            public void onLongPressMarker(MapView mapView, Marker marker) {

            }

            @Override
            public void onTapMap(MapView mapView, ILatLng iLatLng) {
                hideMiniProfile();
            }

            @Override
            public void onLongPressMap(MapView mapView, ILatLng iLatLng) {

            }
        });
    }

    @Override
    public void onBackPressed() {
        if (currentUser != null) {
            this.hideMiniProfile();
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public void onLocationChanged(Location location) {
        super.onLocationChanged(location);

        // Zoom the map to our position!
        LatLng latLng = new LatLng(location.getLatitude(), location.getLongitude());
        mapView.setCenter(latLng);
    }

    @Override
    protected void onCompassChanged(float outputX, float outputY, float outputZ) {
        // TODO: anything?
    }

    public void openCurrentProfile(View v) {
        if (currentUser != null) openProfile(currentUser);
    }

    private void openProfile(User user) {
        Intent intent = new Intent(BrowseUserActivity.this, ProfileActivity.class);
        intent.putExtra(ProfileActivity.EXTRA_USER_ID, user.id);
        startActivity(intent);
    }

    private void showMiniProfile(User user) {
        this.currentUser = user;

        if (user.picture != null) {
            this.miniProfileImage.setImageDrawable(user.picture);
            this.miniProfileImage.setVisibility(View.VISIBLE);
        } else {
            this.miniProfileImage.setVisibility(View.GONE);
        }
        this.miniName.setText(user.name);
        this.miniLocation.setText(user.location);
        this.miniDescription.setText(user.description);

        miniPlayer.setVisibility(View.VISIBLE);
        miniProfile.setVisibility(View.VISIBLE);
    }

    private void hideMiniProfile() {
        currentUser = null;
        miniPlayer.setVisibility(View.GONE);
        miniProfile.setVisibility(View.GONE);
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

    public void connect(View v) {
        startActivity(new Intent(this, RecordWalkActivity.class));
    }
}
