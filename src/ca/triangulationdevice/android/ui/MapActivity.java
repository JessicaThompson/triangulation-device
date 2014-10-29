package ca.triangulationdevice.android.ui;

import android.app.Activity;
import android.app.AlertDialog;
import android.bluetooth.BluetoothDevice;
import android.content.DialogInterface;
import android.content.res.Resources;
import android.location.Location;
import android.os.Bundle;
import android.os.Parcelable.Creator;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import ca.triangulationdevice.android.pd.PDDriver;
import ca.triangulationdevice.android.pd.PDDriver.Listener;
import ca.triangulationdevice.android.storage.PathStorage;
import ca.triangulationdevice.android.ui.partial.BluetoothIPCActivity;
import ca.triangulationdevice.android.ui.views.RadarView;
import ca.triangulationdevice.android.ui.views.ResizableWaveformsView;
import ca.triangulationdevice.android.ui.views.WaveView;
import ca.triangulationdevice.android.ui.views.WaveformLabelView;
import ca.triangulationdevice.android.util.Typefaces;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import de.vndvl.chrs.triangulationdevice.R;

/**
 * An {@link Activity} which lets the user create sessions, connect with other
 * devices, and see the current status of the other connected user.
 */
public class MapActivity extends BluetoothIPCActivity<Location> {
    @SuppressWarnings("unused")
    private static final String TAG = "MapActivity";
    private static final float DEFAULT_ZOOM = 19;

    private Resources resources;

    private boolean recording = false;
    private final PathStorage path = new PathStorage(this);
    private final PDDriver pd = new PDDriver(this);

    private ResizableWaveformsView waveforms;
    private WaveformLabelView myWaveform;
    private WaveformLabelView theirWaveform;
    private WaveView myWaveView;
    private WaveView theirWaveView;
    private RadarView radar;

    private TextView myConnectionStatus;
    private Button startStopButton;
    private Button findNearbyButton;

    private long lastCompassUpdate = 0;
    private float lastCompass = 0;
    private GoogleMap map;
    private Marker otherMarker;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Typefaces.loadTypefaces(this);
        setContentView(R.layout.map_activity);
        this.resources = getResources();

        this.myWaveform = (WaveformLabelView) findViewById(R.id.my_waveform);
        this.theirWaveform = (WaveformLabelView) findViewById(R.id.their_waveform);
        this.theirWaveform.setDeviceName(this.resources.getString(R.string.paired_device));

        this.myWaveView = (WaveView) this.myWaveform.findViewById(R.id.waveform);
        this.myWaveView.setColor(this.resources.getColor(R.color.waveform_mine));
        this.theirWaveView = (WaveView) this.theirWaveform.findViewById(R.id.waveform);
        this.theirWaveView.setColor(this.resources.getColor(R.color.waveform_theirs));

        this.pd.initServices();
        this.pd.setListener(new Listener() {
            @Override
            public void myFrequencyChanged(int wave_index, float newFrequency) {
                MapActivity.this.myWaveView.setFrequency(wave_index, newFrequency);
            }

            @Override
            public void theirFrequencyChanged(int wave_index, float newFrequency) {
                MapActivity.this.theirWaveView.setFrequency(wave_index, newFrequency);
            }
        });

        this.waveforms = (ResizableWaveformsView) findViewById(R.id.double_waveform);
        this.waveforms.setListener(new ResizableWaveformsView.Listener() {
            @Override
            public void onChanged(double topBottomRatio) {
                MapActivity.this.pd.pdChangeXfade((float) topBottomRatio);
            }
        });

        this.radar = (RadarView) findViewById(R.id.devices_radar);
        this.map = ((MapFragment) getFragmentManager().findFragmentById(R.id.devices_map)).getMap();

        this.startStopButton = (Button) findViewById(R.id.start_button);
        this.findNearbyButton = (Button) findViewById(R.id.find_nearby_button);
        this.myConnectionStatus = (TextView) findViewById(R.id.my_device_status_value);

        // Set our fancy, custom fonts.
        this.findNearbyButton.setTypeface(Typefaces.raleway);
        this.startStopButton.setTypeface(Typefaces.raleway);
        this.myConnectionStatus.setTypeface(Typefaces.raleway);
        TextView myDeviceStatusTitle = (TextView) findViewById(R.id.my_device_status_title);
        myDeviceStatusTitle.setTypeface(Typefaces.ralewaySemiBold);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        this.pd.close();
    }

    @Override
    public void onLocationChanged(Location location) {
        super.onLocationChanged(location);
        this.myWaveform.setLocation(location);
        this.radar.setLocation(location);
        this.bluetoothSend(location);

        // Zoom the map to our position!
        LatLng latLng = new LatLng(location.getLatitude(), location.getLongitude());
        CameraPosition cameraPosition = new CameraPosition.Builder()
                .target(latLng)
                .bearing((float) Math.toDegrees(this.lastCompass))
                .zoom(DEFAULT_ZOOM)
                .build();
        this.map.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition));

        if (this.recording) {
            this.path.addMine(location);
        }

        this.pd.myLocationChanged(location);
    }

    /**
     * Called when the connected {@link BluetoothDevice} sends us a new Location
     * value.
     * 
     * @param location
     *            The new value for the connected other device's location.
     */
    public void theirLocationChanged(Location location) {
        this.theirWaveform.setLocation(location);
        this.radar.setOtherLocation(location);

        LatLng latLng = new LatLng(location.getLatitude(), location.getLongitude());
        if (this.otherMarker != null) {
            this.otherMarker.setPosition(latLng);
        } else {
            this.otherMarker = this.map.addMarker(new MarkerOptions().position(latLng)
                    .icon(BitmapDescriptorFactory.fromResource(R.drawable.paired_device)));
        }

        if (this.recording) {
            this.path.addTheirs(location);
        }

        this.pd.theirLocationChanged(location);
    }

    @Override
    protected void onResume() {
        super.onResume();
        this.path.open();
    }

    @Override
    protected void onStop() {
        this.path.close();
        super.onStop();
    }

    @Override
    protected void successfulConnect() {
        super.successfulConnect();
        this.waveforms.activate();

        String statusText = this.resources.getString(R.string.paired_with_x, getConnectedDevice().getName());
        this.myConnectionStatus.setText(statusText);

        // Set radar to connected state.
        this.radar.connected(true);

        // Clear the "Find Nearby Devices" button.
        this.findNearbyButton.setText(this.resources.getString(R.string.disconnect));
        this.findNearbyButton.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, 0);
        this.findNearbyButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                disconnectDevice(v);
            }
        });
    }

    @Override
    protected void disconnectDevice(View buttonView) {
        super.disconnectDevice(buttonView);
        this.myConnectionStatus.setText(this.resources.getString(R.string.not_connected));
        this.waveforms.deactivate();

        // Set radar to disconnected state.
        this.radar.connected(false);

        // Reset the "Find Nearby Devices" button.
        this.findNearbyButton.setText(this.resources.getString(R.string.find_nearby_devices));
        this.findNearbyButton.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_action_location_found, 0, 0, 0);
        this.findNearbyButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                findNearby(v);
            }
        });
    }

    /**
     * Starts the audio processing from our current location. Also starts to
     * record the individual values for later retrieval.
     * 
     * Also updates the UI, changing the "Start" button (which calls this
     * method) to a "Stop" button (which calls {@link #stop(View)}).
     * 
     * @param buttonView
     *            A {@link View}, in case this method is called from an XML
     *            layout.
     */
    public void start(View buttonView) {
        this.recording = true;
        this.pd.start();

        this.startStopButton.setText(this.resources.getString(R.string.stop));
        this.startStopButton.setBackgroundResource(R.drawable.stop_button);
        this.startStopButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                stop(v);
            }
        });
    }

    /**
     * Stops the audio processing from our current location. Also stops the
     * recording and prompts the user to name it in case they want to save.
     * 
     * Also updates the UI, changing the "Stop" button (which calls this method)
     * to a "Start" button (which calls {@link #start(View)}).
     * 
     * @param buttonView
     *            A {@link View}, in case this method is called from an XML
     *            layout.
     */
    public void stop(View buttonView) {
        this.recording = false;
        this.pd.stop();

        this.myWaveView.clear();
        this.theirWaveView.clear();

        AlertDialog.Builder alert = new AlertDialog.Builder(this);
        alert.setTitle("Save Session");
        alert.setMessage("Please enter a title to save this session.");

        // Set an EditText view to get user input
        final EditText input = new EditText(this);
        alert.setView(input);

        alert.setPositiveButton("Save", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int whichButton) {
                String title = input.getText().toString();
                MapActivity.this.path.finish(title);
            }
        });

        alert.setNegativeButton("Discard", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int whichButton) {
                // Do nothing, don't save it.
                // TODO: But still send it to the server?
            }
        });
        alert.show();

        this.startStopButton.setText(this.resources.getString(R.string.start));
        this.startStopButton.setBackgroundResource(R.drawable.start_button);
        this.startStopButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                start(v);
            }
        });
    }

    @Override
    protected void onMessageReceived(Location message) {
        theirLocationChanged(message);
    }

    @Override
    protected Location getDefault() {
        return getLocation();
    }

    @Override
    protected Creator<Location> getCreator() {
        return Location.CREATOR;
    }

    @Override
    protected void onCompassChanged(float azimuth, float pitch, float roll) {
        this.pd.pdChangeGyroscope(azimuth, pitch, roll);

        if (System.currentTimeMillis() - this.lastCompassUpdate > 200) {
            this.lastCompassUpdate = System.currentTimeMillis();
            this.lastCompass = azimuth;
            this.radar.setAzimuth(azimuth);

            Location currentLocation = getLocation();
            LatLng currentLatLng = new LatLng(currentLocation.getLatitude(), currentLocation.getLongitude());
            CameraPosition newPosition = CameraPosition.builder().target(currentLatLng).bearing((float) Math.toDegrees(azimuth)).zoom(DEFAULT_ZOOM).build();
            this.map.moveCamera(CameraUpdateFactory.newCameraPosition(newPosition));
        }
    }
}
