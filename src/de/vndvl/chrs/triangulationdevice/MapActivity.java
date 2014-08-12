package de.vndvl.chrs.triangulationdevice;

import java.util.ArrayList;

import android.app.Activity;
import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.IntentSender;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.graphics.Typeface;
import android.location.Location;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesClient;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.location.LocationClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;

public class MapActivity extends FragmentActivity implements
		GooglePlayServicesClient.ConnectionCallbacks,
		GooglePlayServicesClient.OnConnectionFailedListener, LocationListener {

	/*
	 * Define a request code to send to Google Play services This code is
	 * returned in Activity.onActivityResult
	 */
	private final static int CONNECTION_FAILURE_RESOLUTION_REQUEST = 9000;
	private final static int REQUEST_ENABLE_BLUETOOTH = 9000;
	
	private final static int DISCOVERABILITY_DURATION = 5 * 1000;

	private Resources resources;
	private SharedPreferences prefs;
	private SharedPreferences.Editor editor;
	private boolean updatesRequested;

	private LocationRequest locationRequest;
	private LocationClient locationClient;
	private Location currentLocation;
	
	private BluetoothAdapter bluetoothAdapter;
	private ArrayList<BluetoothDevice> bluetoothDevices;
	private ArrayAdapter<String> bluetoothNames;
	private BluetoothDevice chosenDevice;

	private TextView myLongitudeView;
	private TextView myLatitudeView;
	private TextView myConnectionStatus;
	private Button startStopButton;
	private Button findNearbyButton;
	
	// Create a BroadcastReceiver for ACTION_FOUND
	private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
	    public void onReceive(Context context, Intent intent) {
	        String action = intent.getAction();
	        // When discovery finds a device
	        if (BluetoothDevice.ACTION_FOUND.equals(action)) {
	            // Get the BluetoothDevice object from the Intent
	        	BluetoothDevice device = (BluetoothDevice) intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
	        	bluetoothDevices.add(device);
	        	bluetoothNames.add(device.getName());
	        }
	    }
	};
	// Register the BroadcastReceiver
	private IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.map_activity);
		
		resources = getResources();

		myLongitudeView = (TextView) findViewById(R.id.my_device_long_value);
		myLatitudeView = (TextView) findViewById(R.id.my_device_lat_value);
		myConnectionStatus = (TextView) findViewById(R.id.my_device_status_value);
		startStopButton = (Button) findViewById(R.id.start_button);
		findNearbyButton = (Button) findViewById(R.id.find_nearby_button);
		
		// Set our fancy, custom fonts.
		Typeface raleway = Typeface.createFromAsset(getAssets(), "fonts/Raleway-Regular.otf");
		Typeface ralewaySemiBold = Typeface.createFromAsset(getAssets(), "fonts/Raleway-SemiBold.otf");
		Typeface ralewayBold = Typeface.createFromAsset(getAssets(), "fonts/Raleway-Bold.otf");
		Typeface ralewayLight = Typeface.createFromAsset(getAssets(), "fonts/Raleway-Light.otf");
		myLongitudeView.setTypeface(raleway);
		myLatitudeView.setTypeface(raleway);
		startStopButton.setTypeface(raleway);
		((TextView) findViewById(R.id.my_device_label)).setTypeface(raleway);
		((TextView) findViewById(R.id.my_device_long_title)).setTypeface(ralewaySemiBold);
		((TextView) findViewById(R.id.my_device_long_value)).setTypeface(ralewayLight);
		((TextView) findViewById(R.id.my_device_lat_title)).setTypeface(ralewaySemiBold);
		((TextView) findViewById(R.id.my_device_lat_value)).setTypeface(ralewayLight);
		((TextView) findViewById(R.id.my_device_status_title)).setTypeface(ralewaySemiBold);
		((TextView) findViewById(R.id.my_device_status_value)).setTypeface(raleway);
		((TextView) findViewById(R.id.find_nearby_button)).setTypeface(raleway);

		// Open the shared preferences to save the fact that we want updates.
		prefs = getSharedPreferences("SharedPreferences", Context.MODE_PRIVATE);
		editor = prefs.edit();
		updatesRequested = true;

		locationClient = new LocationClient(this, this, this);
		locationRequest = LocationRequest.create();
		locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
		locationRequest.setInterval(5000);
		locationRequest.setFastestInterval(1000);
		
		// Set up the Bluetooth adapter, make sure Bluetooth is on.
		bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
		if (bluetoothAdapter != null) {
		    if (!bluetoothAdapter.isEnabled()) {
			    Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
			    startActivityForResult(enableBtIntent, REQUEST_ENABLE_BLUETOOTH);
			}
		}
	}
	
	private void setStartButton() {
		startStopButton.setText(resources.getString(R.string.start));
		startStopButton.setBackgroundDrawable(resources.getDrawable(R.drawable.start_button));
	}
	
	private void setStopButton() {
		startStopButton.setText(resources.getString(R.string.stop));
		startStopButton.setBackgroundDrawable(resources.getDrawable(R.drawable.stop_button));
	}

	@Override
	public void onLocationChanged(Location location) {
		// Report to the UI that the location was updated
		myLongitudeView.setText(String.format("%.6f", location.getLongitude()));
		myLatitudeView.setText(String.format("%.6f", location.getLatitude()));
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

	@Override
	protected void onPause() {
		editor.putBoolean("KEY_UPDATES_ON", updatesRequested);
		editor.commit();
		super.onPause();
	}
	
	public void findNearby(View tappedView) {
		// Set us up for discoverability
		Intent discoverableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
		discoverableIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, DISCOVERABILITY_DURATION);
		startActivity(discoverableIntent);
		
		// Register the bluetooth adapter to hold the devices as they come in.
		registerReceiver(mReceiver, filter);
		bluetoothAdapter.startDiscovery();
		bluetoothDevices = new ArrayList<BluetoothDevice>();
		bluetoothNames = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1);
		
		// Create the dialog that shows new devices.
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setTitle(resources.getString(R.string.nearby_devices));
		builder.setAdapter(bluetoothNames, new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int item) {
				unregisterReceiver(mReceiver);
				bluetoothAdapter.cancelDiscovery();
				connectTo(bluetoothDevices.get(item));
			}
		});
		builder.setNegativeButton(resources.getString(R.string.cancel), new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int item) {
				unregisterReceiver(mReceiver);
				bluetoothAdapter.cancelDiscovery();
				dialog.dismiss();
			}
		});
		
		AlertDialog alert = builder.create();
		alert.show();
	}
	
	private void connectTo(BluetoothDevice device) {
		// Set device as our chosen device.
		chosenDevice = device;
		String statusText = resources.getString(R.string.paired_with_x, chosenDevice.getName());
		myConnectionStatus.setText(statusText);
		
		// Clear the "Find Nearby Devices" button.
		findNearbyButton.setText(resources.getString(R.string.disconnect));
		findNearbyButton.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, 0);
		findNearbyButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				disconnectDevice(v);
			}
		});
	}
	
	private void disconnectDevice(View buttonView) {
		chosenDevice = null;
		myConnectionStatus.setText(resources.getString(R.string.not_connected));
		
		// Reset the "Find Nearby Devices" button.
		findNearbyButton.setText(resources.getString(R.string.find_nearby_devices));
		findNearbyButton.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_action_location_found, 0, 0, 0);
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
				Toast.makeText(this, "Try the request again...?", Toast.LENGTH_LONG).show();
				break;
			}
		}
	}

	@Override
	public void onConnected(Bundle dataBundle) {
		currentLocation = locationClient.getLastLocation();
		onLocationChanged(currentLocation);
		
		if (updatesRequested) {
			locationClient.requestLocationUpdates(locationRequest, this);
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
