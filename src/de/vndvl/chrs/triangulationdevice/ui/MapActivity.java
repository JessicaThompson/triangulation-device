package de.vndvl.chrs.triangulationdevice.ui;

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

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.UiSettings;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;

import de.vndvl.chrs.triangulationdevice.R;
import de.vndvl.chrs.triangulationdevice.pd.PDDriver;
import de.vndvl.chrs.triangulationdevice.storage.PathStorage;
import de.vndvl.chrs.triangulationdevice.ui.partial.BluetoothIPCActivity;
import de.vndvl.chrs.triangulationdevice.ui.views.DraggableWeightView;
import de.vndvl.chrs.triangulationdevice.ui.views.RadarView;
import de.vndvl.chrs.triangulationdevice.ui.views.WaveformLabelView;
import de.vndvl.chrs.triangulationdevice.util.Typefaces;

/**
 * An {@link Activity} which lets the user create sessions, connect with other
 * devices, and see the current status of the other connected user.
 */
public class MapActivity extends BluetoothIPCActivity<Location> {
    private static final float DEFAULT_ZOOM = 19;

    private Resources resources;

    private boolean recording = false;
    private final PathStorage path = new PathStorage(this);
    private final PDDriver pd = new PDDriver(this);

    private WaveformLabelView myWaveform;
    private WaveformLabelView theirWaveform;
    private RadarView radar;

    private TextView myConnectionStatus;
    private Button startStopButton;
    private Button findNearbyButton;

    private DraggableWeightView waveforms;

    private GoogleMap map;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Typefaces.loadTypefaces(this);
        setContentView(R.layout.map_activity);
        this.resources = getResources();

        this.pd.initServices();

        this.myWaveform = (WaveformLabelView) findViewById(R.id.my_waveform);
        this.theirWaveform = (WaveformLabelView) findViewById(R.id.their_waveform);
        this.theirWaveform.setDeviceName(this.resources.getString(R.string.paired_device));

        this.waveforms = (DraggableWeightView) findViewById(R.id.waveform);
        this.waveforms.setListener(new DraggableWeightView.Listener() {
            @Override
            public void onChanged(double topBottomRatio) {
                MapActivity.this.pd.pdChangeXfade((float) topBottomRatio);
            }
        });

        this.radar = (RadarView) findViewById(R.id.devices_radar);
        this.map = ((MapFragment) getFragmentManager().findFragmentById(R.id.devices_map)).getMap();
        UiSettings mapSettings = this.map.getUiSettings();
        mapSettings.setCompassEnabled(true);
        mapSettings.setScrollGesturesEnabled(false);
        mapSettings.setRotateGesturesEnabled(false);
        mapSettings.setTiltGesturesEnabled(false);
        mapSettings.setZoomGesturesEnabled(false);
        mapSettings.setZoomControlsEnabled(false);

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
        this.myWaveform.setLocation(location);
        this.radar.setLocation(location);

        // Zoom the map to our position!
        LatLng latLng = new LatLng(location.getLatitude(), location.getLongitude());
        CameraPosition cameraPosition = new CameraPosition.Builder()
                .target(latLng)
                .zoom(DEFAULT_ZOOM)
                .bearing(location.getBearing())
                .build();
        this.map.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition));

        if (this.recording) {
            this.path.addMine(location);
        }

        this.pd.myLocationChanged(location);
    }

    /**
     * Called when the direction the phone is facing changes. The azimuth value
     * is in radians between our orientation and north, positive being in the
     * counter-clockwise direction.
     */
    @Override
    public void onCompassChanged(float azimuth) {
        this.radar.setAzimuth(azimuth);
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
    protected void onPause() {
        this.path.close();
        this.pd.stop();
        super.onPause();
    }

    @Override
    protected void successfulConnect() {
        super.successfulConnect();
        this.waveforms.activate();

        String statusText = this.resources.getString(R.string.paired_with_x, getConnectedDevice().getName());
        this.myConnectionStatus.setText(statusText);
        this.theirWaveform.setVisibility(View.VISIBLE);

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
        this.theirWaveform.setVisibility(View.GONE);

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
}
