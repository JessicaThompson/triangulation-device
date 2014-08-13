package de.vndvl.chrs.triangulationdevice;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.UUID;

import android.app.Activity;
import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.IntentSender;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.location.Location;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
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

public class MapActivity extends FragmentActivity implements GooglePlayServicesClient.ConnectionCallbacks, GooglePlayServicesClient.OnConnectionFailedListener, LocationListener {

	private final static String NAME = "Triangulation Device";
	private final static UUID MY_UUID = UUID.fromString("fa87c0d0-afac-11de-8a39-0800200c9a28");
	
	private final static int SOCKET_CONNECTED = 1;
	private final static int DATA_RECEIVED = 2;
	private final static int MESSAGE_READ = 3;
	
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
	
	private boolean serverMode = false;
	private AcceptThread acceptThread;
	private ConnectThread connectThread;
	private ConnectedThread bluetoothConnection;
	private Handler bluetoothHandler = new Handler() {
		@Override
		public void handleMessage(Message msg) {
			switch (msg.what) {
				case SOCKET_CONNECTED: {
					bluetoothConnection = (ConnectedThread) msg.obj;
					
					if (!serverMode) {
						bluetoothConnection.write("this is a message".getBytes());
					}
					break;
				}
				case DATA_RECEIVED: {
					String data = (String) msg.obj;
					
					if (serverMode)
						bluetoothConnection.write(data.getBytes());
					}
					break;
				case MESSAGE_READ:
					// your code goes here
					break;
			}
		}
	};

	private WaveformView myWaveform;
	private WaveformView theirWaveform;
	private RadarView radar;
	
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
		Typefaces.loadTypefaces(this);
		setContentView(R.layout.map_activity);
		resources = getResources();
		
		myWaveform = (WaveformView) findViewById(R.id.my_waveform);
		theirWaveform = (WaveformView) findViewById(R.id.their_waveform);
		theirWaveform.setDeviceName(resources.getString(R.string.paired_device));
		
		radar = (RadarView) findViewById(R.id.devices_map);
		
		startStopButton = (Button) findViewById(R.id.start_button);
		findNearbyButton = (Button) findViewById(R.id.find_nearby_button);
		myConnectionStatus = (TextView) findViewById(R.id.my_device_status_value);
		
		// Set our fancy, custom fonts.
		findNearbyButton.setTypeface(Typefaces.raleway);
		startStopButton.setTypeface(Typefaces.raleway);
		myConnectionStatus.setTypeface(Typefaces.raleway);
		TextView myDeviceStatusTitle = (TextView) findViewById(R.id.my_device_status_title);
		myDeviceStatusTitle.setTypeface(Typefaces.ralewaySemiBold);

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
			} else {
				acceptThread = new AcceptThread();
			}
		}
	}
	
	@Override
	public void onLocationChanged(Location location) {
		myWaveform.setLocation(location);
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
		
		// Start the accepting-connections listener.
		if (acceptThread == null) {
			acceptThread = new AcceptThread();
			acceptThread.start();
		}
	}

	@Override
	protected void onPause() {
		editor.putBoolean("KEY_UPDATES_ON", updatesRequested);
		editor.commit();
		
		// Stop the accepting-connections listener.
		acceptThread.cancel();
		acceptThread = null;
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
				dialog.cancel();
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
		theirWaveform.setVisibility(View.VISIBLE);
		
		// Set up the connection thread and start it.
		connectThread = new ConnectThread(chosenDevice);
		connectThread.start();
		
		// Set radar to connected state.
		radar.connected(true);
		
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
		// Clear chosen device.
		chosenDevice = null;
		myConnectionStatus.setText(resources.getString(R.string.not_connected));
		theirWaveform.setVisibility(View.GONE);
		
		// Stop connection thread if it's active
		if (connectThread != null) {
			connectThread.cancel();
			connectThread = null;
		}
		if (bluetoothConnection != null) {
			bluetoothConnection.cancel();
			bluetoothConnection = null;
		}
		
		// Set radar to disconnected state.
		radar.connected(false);
		
		// Reset the "Find Nearby Devices" button.
		findNearbyButton.setText(resources.getString(R.string.find_nearby_devices));
		findNearbyButton.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_action_location_found, 0, 0, 0);
		findNearbyButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				findNearby(v);
			}
		});
	}
	
	public void start(View buttonView) {
		startStopButton.setText(resources.getString(R.string.stop));
		startStopButton.setBackgroundResource(R.drawable.stop_button);
		startStopButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				stop(v);
				// TODO: Useful stuff with this method.
			}
		});
	}
	
	public void stop(View buttonView) {
		startStopButton.setText(resources.getString(R.string.start));
		startStopButton.setBackgroundResource(R.drawable.start_button);
		startStopButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				start(v);
				// TODO: Useful stuff with this method.
			}
		});
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
	
	private void manageConnectedSocket(BluetoothSocket socket) {
		bluetoothConnection = new ConnectedThread(socket);
		bluetoothConnection.start();

        // Send the name of the connected device back to the UI Activity
//        Message msg = bluetoothHandler.obtainMessage();
//        Bundle bundle = new Bundle();
//        bundle.putString(BluetoothChat.DEVICE_NAME, device.getName());
//        msg.setData(bundle);
//        bluetoothHandler.sendMessage(msg);
	}

	public class AcceptThread extends Thread {
	    private final BluetoothServerSocket mmServerSocket;
	 
	    public AcceptThread() {
	        // Use a temporary object that is later assigned to mmServerSocket,
	        // because mmServerSocket is final
	        BluetoothServerSocket tmp = null;
	        try {
	            // MY_UUID is the app's UUID string, also used by the client code
	            tmp = bluetoothAdapter.listenUsingRfcommWithServiceRecord(NAME, MY_UUID);
	        } catch (IOException e) { }
	        mmServerSocket = tmp;
	    }
	 
	    public void run() {
	        BluetoothSocket socket = null;
	        // Keep listening until exception occurs or a socket is returned
	        while (true) {
	            try {
	                socket = mmServerSocket.accept();
		            // If a connection was accepted
		            if (socket != null) {
		                // Do work to manage the connection (in a separate thread)
		                manageConnectedSocket(socket);
		                mmServerSocket.close();
		                break;
		            }
	            } catch (IOException e) {
	                break;
	            }
	        }
	    }
	 
	    /** Will cancel the listening socket, and cause the thread to finish */
	    public void cancel() {
	        try {
	            mmServerSocket.close();
	        } catch (IOException e) { }
	    }
	}
	
	private class ConnectThread extends Thread {
	    private final BluetoothSocket mmSocket;
	    private final BluetoothDevice mmDevice;
	 
	    public ConnectThread(BluetoothDevice device) {
	        // Use a temporary object that is later assigned to mmSocket,
	        // because mmSocket is final
	        BluetoothSocket tmp = null;
	        mmDevice = device;
	 
	        // Get a BluetoothSocket to connect with the given BluetoothDevice
	        try {
	            // MY_UUID is the app's UUID string, also used by the server code
	            tmp = device.createRfcommSocketToServiceRecord(MY_UUID);
	        } catch (IOException e) { }
	        mmSocket = tmp;
	    }
	 
	    public void run() {
	        // Cancel discovery because it will slow down the connection
	        bluetoothAdapter.cancelDiscovery();
	 
	        try {
	            // Connect the device through the socket. This will block
	            // until it succeeds or throws an exception
	            mmSocket.connect();
	        } catch (IOException connectException) {
	            // Unable to connect; close the socket and get out
	            try {
	                mmSocket.close();
	            } catch (IOException closeException) { }
	            return;
	        }
	 
	        // Do work to manage the connection (in a separate thread)
	        manageConnectedSocket(mmSocket);
	    }
	 
	    /** Will cancel an in-progress connection, and close the socket */
	    public void cancel() {
	        try {
	            mmSocket.close();
	        } catch (IOException e) { }
	    }
	}
	
	private class ConnectedThread extends Thread {
	    private final BluetoothSocket mmSocket;
	    private final InputStream mmInStream;
	    private final OutputStream mmOutStream;
	 
	    public ConnectedThread(BluetoothSocket socket) {
	        mmSocket = socket;
	        InputStream tmpIn = null;
	        OutputStream tmpOut = null;
	 
	        // Get the input and output streams, using temp objects because
	        // member streams are final
	        try {
	            tmpIn = socket.getInputStream();
	            tmpOut = socket.getOutputStream();
	        } catch (IOException e) { }
	 
	        mmInStream = tmpIn;
	        mmOutStream = tmpOut;
	    }
	 
	    public void run() {
	        byte[] buffer = new byte[1024];  // buffer store for the stream
	        int bytes; // bytes returned from read()
	 
	        // Keep listening to the InputStream until an exception occurs
	        while (true) {
	            try {
	                // Read from the InputStream
	                bytes = mmInStream.read(buffer);
	                // Send the obtained bytes to the UI activity
	                bluetoothHandler.obtainMessage(MESSAGE_READ, bytes, -1, buffer).sendToTarget();
	            } catch (IOException e) {
	                break;
	            }
	        }
	    }
	 
	    /* Call this from the main activity to send data to the remote device */
	    public void write(byte[] bytes) {
	        try {
	            mmOutStream.write(bytes);
	        } catch (IOException e) { }
	    }
	 
	    /* Call this from the main activity to shutdown the connection */
	    public void cancel() {
	        try {
	            mmSocket.close();
	        } catch (IOException e) { }
	    }
	}
}
