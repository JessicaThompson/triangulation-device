package de.vndvl.chrs.triangulationdevice;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesClient;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.location.LocationClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.IntentSender;
import android.content.SharedPreferences;
import android.location.Location;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

public abstract class LocationActivity extends Activity implements GooglePlayServicesClient.ConnectionCallbacks, GooglePlayServicesClient.OnConnectionFailedListener, LocationListener {
    private final static int CONNECTION_FAILURE_RESOLUTION_REQUEST = 9000;
    private final static int FASTEST_INTERVAL = 1000;
    private final static int INTERVAL = 5 * FASTEST_INTERVAL;
    
    private SharedPreferences prefs;
    private SharedPreferences.Editor editor;
    private boolean updatesRequested;
    
    private LocationRequest locationRequest;
    private LocationClient locationClient;
    private Location currentLocation;
    
    public Location getLocation() {
        return currentLocation;
    }
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // Open the shared preferences to save the fact that we want updates.
        prefs = getSharedPreferences("SharedPreferences", Context.MODE_PRIVATE);
        editor = prefs.edit();
        updatesRequested = true;

        locationClient = new LocationClient(this, this, this);
        locationRequest = LocationRequest.create();
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        locationRequest.setInterval(INTERVAL);
        locationRequest.setFastestInterval(FASTEST_INTERVAL);
    }
    
    @Override
    protected void onStart() {
        super.onStart();
        // Connect the location client.
        locationClient.connect();
    }

    @Override
    protected void onStop() {
        super.onStop();
        // Disconnect the location client.
        locationClient.disconnect();
        if (locationClient.isConnected()) {
            locationClient.removeLocationUpdates(this);
        }
    }
    

    @Override
    protected void onPause() {
        editor.putBoolean("KEY_UPDATES_ON", updatesRequested);
        editor.commit();
        
        super.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (prefs.contains("KEY_UPDATES_ON")) {
            updatesRequested = prefs.getBoolean("KEY_UPDATES_ON", false);
        } else {
            editor.putBoolean("KEY_UPDATES_ON", false);
            editor.commit();
        }

        if (!servicesConnected()) {
            Toast.makeText(this, "Location services aren't connected - is your location visible?", Toast.LENGTH_LONG).show();
        }
    }
    
    public abstract void onLocationChanged(Location location);
    
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
                locationClient.connect();
                break;
                // No default because the onConnectionFailed displayed an error
                // message for us.
            }
        }
    }

    @Override
    public void onConnected(Bundle dataBundle) {
        currentLocation = locationClient.getLastLocation();
        if (currentLocation != null) {
            Log.i("Starting Location", String.format("%.6f, %.6f", currentLocation.getLatitude(), currentLocation.getLongitude()));
            onLocationChanged(currentLocation);
            
            if (updatesRequested) {
                locationClient.requestLocationUpdates(locationRequest, this);
            }
        }
    }

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
