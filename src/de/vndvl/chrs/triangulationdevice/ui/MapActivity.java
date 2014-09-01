package de.vndvl.chrs.triangulationdevice.ui;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Set;

import org.puredata.android.io.AudioParameters;
import org.puredata.android.service.PdService;
import org.puredata.android.utils.PdUiDispatcher;
import org.puredata.core.PdBase;
import org.puredata.core.utils.IoUtils;

import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothClass;
import android.bluetooth.BluetoothDevice;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.res.Resources;
import android.location.Location;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import de.vndvl.chrs.triangulationdevice.R;
import de.vndvl.chrs.triangulationdevice.bluetooth.BluetoothDeviceService;
import de.vndvl.chrs.triangulationdevice.bluetooth.BluetoothIPCService;
import de.vndvl.chrs.triangulationdevice.storage.PathStorage;
import de.vndvl.chrs.triangulationdevice.ui.partial.LocationActivity;
import de.vndvl.chrs.triangulationdevice.ui.views.DraggableWeightView;
import de.vndvl.chrs.triangulationdevice.ui.views.RadarView;
import de.vndvl.chrs.triangulationdevice.ui.views.WaveformView;
import de.vndvl.chrs.triangulationdevice.util.Typefaces;

public class MapActivity extends LocationActivity {
    private final static String TAG = "MapActivity";

    private final static int DISCOVERABILITY_DURATION = 5 * 1000;
    private final static String TRIANGULATION_DEVICE_UUID = "04364090-2bca-11e4-8c21-0800200c9a66";

    private Resources resources;

    private BluetoothIPCService bluetoothIPC;
    private BluetoothDeviceService deviceService;
    private BluetoothDevice connectedDevice;

    private ArrayList<BluetoothDevice> bluetoothDevices;
    private ArrayAdapter<String> bluetoothNames;

    private boolean recording = false;
    private PathStorage path = new PathStorage(this);

    private WaveformView myWaveform;
    private WaveformView theirWaveform;
    private RadarView radar;

    private TextView myConnectionStatus;
    private Button startStopButton;
    private Button findNearbyButton;

    private final Handler bluetoothHandler = new Handler(new MapActivityHandler());

    private DraggableWeightView waveforms;

    /* Pd code begins here */

    private PdUiDispatcher dispatcher;
    private PdService pdService = null;

    private final ServiceConnection pdConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            pdService = ((PdService.PdBinder)service).getService();
            try {
                // Initialize PD, load patch
                initPd();
                loadPatch();
            } catch (IOException e) {
                Log.e(TAG, e.toString());
                finish();
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            // Apparently this method will never be called?
        }
    };

    private void initPd() throws IOException {
        int sampleRate = AudioParameters.suggestSampleRate();
        pdService.initAudio(sampleRate, 0, 2, 10.0f);
            // sampleRate, inChannels, outChannels, bufferSize [ms]
        dispatcher = new PdUiDispatcher();
        PdBase.setReceiver(dispatcher);
        /* TODO: Listeners go here (if necessary) */
    }

    private void startPdAudio() {
        if (!pdService.isRunning()) {
            Intent intent = new Intent(MapActivity.this, MapActivity.class);
            pdService.startAudio(intent, R.drawable.icon, "Triangulation Device", "Return to Triangulation Device");
            // Starts audio and creates a notification pointing to this activity
            // To start audio with no notification, give startAudio() 0 args
        }
    }

    private void initSystemServices() {
        TelephonyManager telephonyManager = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
        telephonyManager.listen(new PhoneStateListener() {
            @Override
            public void onCallStateChanged(int state, String incomingNumber) {
                if (pdService == null) return;
                if (state == TelephonyManager.CALL_STATE_IDLE) {
                    startPdAudio();
                    // TODO: Handle possible edge case
                    // Person has app open, sound off, receives call
                    // (undesired sound starts when call ends?)
                } else {
                    pdService.stopAudio();
                }
            }
        }, PhoneStateListener.LISTEN_CALL_STATE);
    }

    private void loadPatch() throws IOException {
        File dir = getFilesDir();
        IoUtils.extractZipResource(getResources().openRawResource(R.raw.triangulationdevice_comp), dir, true);
        File patchFile = new File(dir, "triangulationdevice_comp.pd");
        PdBase.openPatch(patchFile.getAbsolutePath());
    }

    /* Pd code ends here (for the most part) */

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Bind & init PdService
        bindService(new Intent(this, PdService.class), pdConnection, BIND_AUTO_CREATE);
        initSystemServices();

        Typefaces.loadTypefaces(this);

        // Request a progress bar.
        this.requestWindowFeature(Window.FEATURE_PROGRESS);

        setContentView(R.layout.map_activity);
        resources = getResources();

        myWaveform = (WaveformView) findViewById(R.id.my_waveform);
        theirWaveform = (WaveformView) findViewById(R.id.their_waveform);
        theirWaveform.setDeviceName(resources.getString(R.string.paired_device));

        waveforms = (DraggableWeightView) findViewById(R.id.waveform);
        waveforms.setListener(new DraggableWeightView.Listener() {
            @Override
            public void onChanged(double topBottomRatio) {
                // TODO something to do with the new ratio (update the PD).
            }
        });

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

        // Set up the Bluetooth device discovery, react to its listener methods.
        deviceService = new BluetoothDeviceService(this);
        deviceService.setListener(new BluetoothDeviceService.Listener() {
            @Override
            public void unpairedDeviceFound(BluetoothDevice device) {
                int bluetoothClass = device.getBluetoothClass().getDeviceClass();
                if (bluetoothClass == BluetoothClass.Device.PHONE_SMART || bluetoothClass == BluetoothClass.Device.COMPUTER_LAPTOP) {
                    bluetoothDevices.add(device);
                    bluetoothNames.add(device.getName());
                }
            }

            @Override
            public void pairedDevices(Set<BluetoothDevice> pairedDevices) {
                for (BluetoothDevice device : pairedDevices) {
                    int bluetoothClass = device.getBluetoothClass().getDeviceClass();
                    if (bluetoothClass == BluetoothClass.Device.PHONE_SMART || bluetoothClass == BluetoothClass.Device.COMPUTER_LAPTOP) {
                        bluetoothDevices.add(device);
                        bluetoothNames.add(device.getName());
                    }
                }
            }
        });

        // Set up the IPC service.
        bluetoothIPC = new BluetoothIPCService(TRIANGULATION_DEVICE_UUID, bluetoothHandler);

        // Set us up for discoverability
        Intent discoverableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
        discoverableIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, DISCOVERABILITY_DURATION);
        startActivity(discoverableIntent);
    }

    @Override
    protected void onDestroy() {
        deviceService.destroy();
        super.onDestroy();
        // Unbind PdService
        unbindService(pdConnection);
    }

    @Override
    public void onLocationChanged(Location location) {
        sendFloat();
        myWaveform.setLocation(location);
        radar.setLocation(location);
        bluetoothIPC.write(location);

        if (recording) {
            path.addMine(location);
        }
    }

    @Override
    public void onCompassChanged(float azimuth) {
        radar.setAzimuth(azimuth);
    }

    public void theirLocationChanged(Location location) {
        theirWaveform.setLocation(location);
        radar.setOtherLocation(location);

        if (recording) {
            path.addTheirs(location);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        path.open();
        if (connectedDevice != null) {
            bluetoothIPC.start();
        }
    }

    @Override
    protected void onPause() {
        path.close();
        if (connectedDevice != null) {
            bluetoothIPC.stop();
        }

        super.onPause();
    }

    public void findNearby(View tappedView) {
        bluetoothDevices = new ArrayList<BluetoothDevice>();
        bluetoothNames = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1);
        deviceService.start();
        bluetoothIPC.start();

        // Create the dialog that shows new devices.
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(resources.getString(R.string.nearby_devices));
        builder.setAdapter(bluetoothNames, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int item) {
                deviceService.stop();
                connectTo(bluetoothDevices.get(item));
            }
        });
        builder.setNegativeButton(resources.getString(R.string.cancel), new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int item) {
                deviceService.stop();
                dialog.cancel();
            }
        });

        AlertDialog alert = builder.create();
        alert.show();
    }

    private void receiveConnection(BluetoothDevice device) {
        // Set device as our chosen device.
        Log.i(TAG, "Setting the device, receiveConnection() " + device);
        connectedDevice = device;
    }

    private void connectTo(BluetoothDevice device) {
        // Set device as our chosen device.
        setProgressBarIndeterminateVisibility(true);
        connectedDevice = device;
        bluetoothIPC.connect(device);
    }

    private void successfulConnect() {
        setProgressBarIndeterminateVisibility(false);
        bluetoothIPC.write(getLocation());
        waveforms.activate();

        String statusText = resources.getString(R.string.paired_with_x, connectedDevice.getName());
        myConnectionStatus.setText(statusText);
        theirWaveform.setVisibility(View.VISIBLE);

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
        setProgressBarIndeterminateVisibility(false);
        connectedDevice = null;
        myConnectionStatus.setText(resources.getString(R.string.not_connected));
        theirWaveform.setVisibility(View.GONE);

        bluetoothIPC.disconnect();

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
        this.recording = true;
        startStopButton.setText(resources.getString(R.string.stop));
        startStopButton.setBackgroundResource(R.drawable.stop_button);
        startStopButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startPdAudio();     // start Pd patch
                stop(v);
                // TODO: Useful stuff with this method. Start some audio or
                // something.
            }
        });
    }

    public void stop(View buttonView) {
        this.recording = false;

        AlertDialog.Builder alert = new AlertDialog.Builder(this);
        alert.setTitle("Save Session");
        alert.setMessage("Please enter a title to save this session.");

        // Set an EditText view to get user input
        final EditText input = new EditText(this);
        alert.setView(input);

        alert.setPositiveButton("Save", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                String title = input.getText().toString();
                MapActivity.this.path.finish(title);
            }
        });

        alert.setNegativeButton("Discard", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                MapActivity.this.path.finish(null);
            }
        });
        alert.show();

        startStopButton.setText(resources.getString(R.string.start));
        startStopButton.setBackgroundResource(R.drawable.start_button);
        startStopButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                pdService.stopAudio();    // stop Pd patch
                start(v);
                // TODO: Useful stuff with this method. Stop some audio or
                // something.
            }
        });
    }

    private class MapActivityHandler implements Handler.Callback {
        @Override
        public boolean handleMessage(Message msg) {
            switch (msg.what) {
            case BluetoothIPCService.NEW_DEVICE:
                receiveConnection((BluetoothDevice) msg.obj);
            case BluetoothIPCService.MESSAGE_STATE_CHANGE:
                switch (msg.arg1) {
                case BluetoothIPCService.STATE_CONNECTED:
                    // We're finally connected, so change the UI to
                    // reflect that.
                    successfulConnect();
                    break;
                case BluetoothIPCService.STATE_CONNECTING:
                    // Connecting state gets set once we choose a device
                    // so we'll ignore this.
                    break;
                case BluetoothIPCService.STATE_LISTEN:
                    // We're disconnected and listening to things.
                    if (connectedDevice != null) {
                        disconnectDevice(null);
                    }
                    break;
                case BluetoothIPCService.STATE_NONE:
                    // This is only when it's being set up and not
                    // listening yet, or stopped and in the process of
                    // shutting down.
                    break;
                }
                break;
            case BluetoothIPCService.MESSAGE_READ:
                Location theirLocation = (Location) msg.obj;
                theirLocationChanged(theirLocation);
                break;
            case BluetoothIPCService.MESSAGE_INFO:
                Log.i(TAG, msg.getData().getString(BluetoothIPCService.INFO));
                break;
            }
            return true;
        }
    };
}
