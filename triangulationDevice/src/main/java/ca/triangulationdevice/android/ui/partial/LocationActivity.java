package ca.triangulationdevice.android.ui.partial;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.IntentSender;
import android.content.SharedPreferences;
import android.location.Location;
import android.os.Bundle;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;

/**
 * An abstract convenience {@link Activity} which updates subclasses with new
 * {@link Location}s as they come in. Subclasses must implement
 * {@link #onLocationChanged(Location)} and deal with new location events which
 * come in.
 */
public abstract class LocationActivity extends TriangulationActivity implements GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener, LocationListener {
    private final static int CONNECTION_FAILURE_RESOLUTION_REQUEST = 9000;
    private final static int FASTEST_INTERVAL = 50;
    private final static int INTERVAL = 100;

    private SharedPreferences prefs;
    private SharedPreferences.Editor editor;
    private boolean updatesRequested;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Open the shared preferences to save the fact that we want updates.
        this.prefs = getSharedPreferences("SharedPreferences", Context.MODE_PRIVATE);
        this.editor = this.prefs.edit();
    }

    @Override
    protected void onStart() {
        super.onStart();
        // Connect the location client.
    }

    @Override
    protected void onStop() {
        super.onStop();
        // Disconnect the location client.
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
                break;
            // No default because the onConnectionFailed displayed an error
            // message for us.
            }
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

    @Override
    public void onLocationChanged(Location location) {
        // TODO Auto-generated method stub

    }

    @Override
    public void onConnected(Bundle connectionHint) {
        // TODO Auto-generated method stub

    }

    @Override
    public void onConnectionSuspended(int cause) {
        // TODO Auto-generated method stub

    }

    public Location getLocation() {
        return null;
    }
}
