package ca.triangulationdevice.android.ui.partial;

import android.app.Activity;
import android.content.IntentSender;
import android.location.Location;
import android.os.Bundle;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;

/**
 * An abstract convenience {@link Activity} which updates subclasses with new
 * {@link Location}s as they come in. Subclasses must implement
 * {@link #onLocationChanged(Location)} and deal with new location events which
 * come in.
 */
public abstract class LocationActivity extends TriangulationActivity implements GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener, LocationListener {
    private final static int FASTEST_INTERVAL = 50;
    private final static int INTERVAL = 100;
    private static final String REQUESTING_LOCATION_UPDATES_KEY = "req_updates";
    private static final String LOCATION_KEY = "location";
    private static final int CONNECTION_FAILURE_RESOLUTION_REQUEST = 9000;

    private GoogleApiClient googleAPIClient;
    protected Location lastLocation;
    private LocationRequest locationRequest;
    private boolean requestingLocationUpdates;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        updateValuesFromBundle(savedInstanceState);
        this.buildGoogleApiClient();
        this.createLocationRequest();
        googleAPIClient.connect();
    }

    protected synchronized void buildGoogleApiClient() {
        googleAPIClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build();
    }

    protected void createLocationRequest() {
        locationRequest = new LocationRequest();
        locationRequest.setInterval(INTERVAL);
        locationRequest.setFastestInterval(FASTEST_INTERVAL);
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
    }

    private void updateValuesFromBundle(Bundle savedInstanceState) {
        if (savedInstanceState != null) {
            // Update the value of requestingLocationUpdates from the Bundle, and
            // make sure that the Start Updates and Stop Updates buttons are
            // correctly enabled or disabled.
            if (savedInstanceState.keySet().contains(REQUESTING_LOCATION_UPDATES_KEY)) {
                requestingLocationUpdates = savedInstanceState.getBoolean(REQUESTING_LOCATION_UPDATES_KEY);
            }

            // Update the value of mCurrentLocation from the Bundle and update the
            // UI to show the correct latitude and longitude.
            if (savedInstanceState.keySet().contains(LOCATION_KEY)) {
                // Since LOCATION_KEY was found in the Bundle, we can be sure that
                // mCurrentLocationis not null.
                lastLocation = savedInstanceState.getParcelable(LOCATION_KEY);
            }
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        stopLocationUpdates();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (googleAPIClient.isConnected() && requestingLocationUpdates) {
            startLocationUpdates();
        }
    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        // Google Play services can resolve some errors it detects (like
        // location services being turned off). If the error has a resolution,
        // we can send an Intent to start a Google Play services activity that
        // can resolve it.
        if (connectionResult.hasResolution()) {
            try {
                connectionResult.startResolutionForResult(this, CONNECTION_FAILURE_RESOLUTION_REQUEST);
            } catch (IntentSender.SendIntentException e) {
                // Thrown if Google Play services canceled the original PendingIntent.
                e.printStackTrace();
            }
        } else {
            // If no resolution is available, display a dialog to the user with the error.
            GooglePlayServicesUtil.getErrorDialog(connectionResult.getErrorCode(), this, 0).show();
        }
    }

    public void onSaveInstanceState(Bundle savedInstanceState) {
        savedInstanceState.putBoolean(REQUESTING_LOCATION_UPDATES_KEY, requestingLocationUpdates);
        savedInstanceState.putParcelable(LOCATION_KEY, lastLocation);
        super.onSaveInstanceState(savedInstanceState);
    }

    private void startLocationUpdates() {
        requestingLocationUpdates = true;
        LocationServices.FusedLocationApi.requestLocationUpdates(googleAPIClient, locationRequest, this);
    }

    private void stopLocationUpdates() {
        if (requestingLocationUpdates) {
            LocationServices.FusedLocationApi.removeLocationUpdates(googleAPIClient, this);
        }
    }

    @Override
    public void onConnected(Bundle connectionHint) {
        lastLocation = LocationServices.FusedLocationApi.getLastLocation(googleAPIClient);
        startLocationUpdates();
    }

    @Override
    public void onLocationChanged(Location location) {
        this.lastLocation = location;
    }

    @Override
    public void onConnectionSuspended(int cause) {
    }

    public Location getLocation() {
        return lastLocation;
    }
}
