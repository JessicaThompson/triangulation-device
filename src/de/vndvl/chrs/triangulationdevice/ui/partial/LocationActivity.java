package de.vndvl.chrs.triangulationdevice.ui.partial;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.IntentSender;
import android.content.SharedPreferences;
import android.location.Location;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesClient;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.location.LocationClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;

/**
 * An abstract convenience {@link Activity} which updates subclasses with new
 * {@link Location}s as they come in. Subclasses must implement
 * {@link #onLocationChanged(Location)} and deal with new location events which
 * come in.
 */
public abstract class LocationActivity extends TriangulationActivity implements GooglePlayServicesClient.ConnectionCallbacks, GooglePlayServicesClient.OnConnectionFailedListener, LocationListener {
    private final static int CONNECTION_FAILURE_RESOLUTION_REQUEST = 9000;
    private final static int FASTEST_INTERVAL = 250;
    private final static int INTERVAL = 500;

    private SharedPreferences prefs;
    private SharedPreferences.Editor editor;
    private boolean updatesRequested;

    private LocationRequest locationRequest;
    private LocationClient locationClient;
    private Location currentLocation;

    /**
     * Called when a new Location value is received from the sensor system.
     * 
     * @param location
     *            The new Location value.
     */
    @Override
    public abstract void onLocationChanged(Location location);

    /**
     * Called to synchronously obtain a {@link Location} value, in case one is
     * needed when something is initialized.
     * 
     * @return A non-null location.
     */
    public Location getLocation() {
        return this.currentLocation;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Open the shared preferences to save the fact that we want updates.
        this.prefs = getSharedPreferences("SharedPreferences", Context.MODE_PRIVATE);
        this.editor = this.prefs.edit();
        this.updatesRequested = true;

        this.locationClient = new LocationClient(this, this, this);
        this.locationRequest = LocationRequest.create();
        this.locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        this.locationRequest.setInterval(INTERVAL);
        this.locationRequest.setFastestInterval(FASTEST_INTERVAL);
    }

    @Override
    protected void onStart() {
        super.onStart();
        // Connect the location client.
        this.locationClient.connect();
    }

    @Override
    protected void onStop() {
        super.onStop();
        // Disconnect the location client.
        this.locationClient.disconnect();
        if (this.locationClient.isConnected()) {
            this.locationClient.removeLocationUpdates(this);
        }
    }

    @Override
    protected void onPause() {
        this.editor.putBoolean("KEY_UPDATES_ON", this.updatesRequested);
        this.editor.commit();

        super.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (this.prefs.contains("KEY_UPDATES_ON")) {
            this.updatesRequested = this.prefs.getBoolean("KEY_UPDATES_ON", false);
        } else {
            this.editor.putBoolean("KEY_UPDATES_ON", false);
            this.editor.commit();
        }

        if (!servicesConnected()) {
            Toast.makeText(this, "Location services aren't connected - is your location visible?", Toast.LENGTH_LONG).show();
        }
    }

    private boolean servicesConnected() {
        // Check that Google Play services is available
        int errorCode = GooglePlayServicesUtil.isGooglePlayServicesAvailable(this);
        if (errorCode != ConnectionResult.SUCCESS) {
            GooglePlayServicesUtil.getErrorDialog(errorCode, this, 0).show();
            return false;
        } else {
            return true;
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        // We'll hit here if
        switch (requestCode) {
        case CONNECTION_FAILURE_RESOLUTION_REQUEST:
            // If the result code is Activity.RESULT_OK, try to connect again
            switch (resultCode) {
            case Activity.RESULT_OK:
                this.locationClient.connect();
                break;
            // No default because the onConnectionFailed displayed an error
            // message for us.
            }
        }
    }

    /**
     * Called when connected to the GPS service. Just starts the location
     * updates for now, but subclasses can hook into here to update UI if
     * needed.
     * 
     * @param dataBundle
     *            Some bundle, I dunno, bro.
     */
    @Override
    public void onConnected(Bundle dataBundle) {
        this.currentLocation = this.locationClient.getLastLocation();
        if (this.currentLocation != null) {
            Log.i("Starting Location", String.format("%.6f, %.6f", this.currentLocation.getLatitude(), this.currentLocation.getLongitude()));
            onLocationChanged(this.currentLocation);

            if (this.updatesRequested) {
                this.locationClient.requestLocationUpdates(this.locationRequest, this);
            }
        }
    }

    /**
     * Called when disconnected from the GPS service. Subclasses can hook into
     * here to update UI when this happens, if needed.
     */
    @Override
    public void onDisconnected() {
        // Display the connection status
        Toast.makeText(this, "GPS Disconnected. Please re-connect.", Toast.LENGTH_SHORT).show();
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
                // Thrown if Google Play services canceled the original
                // PendingIntent.
                e.printStackTrace();
            }
        } else {
            // If no resolution is available, display a dialog to the user with
            // the error.
            GooglePlayServicesUtil.getErrorDialog(connectionResult.getErrorCode(), this, 0).show();
        }
    }
}
