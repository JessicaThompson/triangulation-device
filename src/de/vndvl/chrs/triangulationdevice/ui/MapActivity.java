package de.vndvl.chrs.triangulationdevice.ui;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;

import org.puredata.android.io.AudioParameters;
import org.puredata.android.service.PdService;
import org.puredata.android.utils.PdUiDispatcher;
import org.puredata.core.PdBase;
import org.puredata.core.utils.IoUtils;

import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.res.Resources;
import android.location.Location;
import android.os.Bundle;
import android.os.IBinder;
import android.os.Parcelable.Creator;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import de.vndvl.chrs.triangulationdevice.R;
import de.vndvl.chrs.triangulationdevice.storage.PathStorage;
import de.vndvl.chrs.triangulationdevice.ui.partial.BluetoothIPCActivity;
import de.vndvl.chrs.triangulationdevice.ui.views.DraggableWeightView;
import de.vndvl.chrs.triangulationdevice.ui.views.RadarView;
import de.vndvl.chrs.triangulationdevice.ui.views.WaveformView;
import de.vndvl.chrs.triangulationdevice.util.Typefaces;

public class MapActivity extends BluetoothIPCActivity<Location> {
    private Resources resources;

    private boolean recording = false;
    private final PathStorage path = new PathStorage(this);

    private WaveformView myWaveform;
    private WaveformView theirWaveform;
    private RadarView radar;

    private TextView myConnectionStatus;
    private Button startStopButton;
    private Button findNearbyButton;

    private DraggableWeightView waveforms;

    private Location theirLocation;

    /* Pd code begins here */

    private PdUiDispatcher dispatcher;
    private PdService pdService = null;

    private final ServiceConnection pdConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            MapActivity.this.pdService = ((PdService.PdBinder) service).getService();
            try {
                // Initialize PD, load patch
                initPd();
                loadPatch();
            } catch (IOException e) {
                Log.e(getClass().toString(), e.toString());
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
        this.pdService.initAudio(sampleRate, 0, 2, 10.0f);
        // sampleRate, inChannels, outChannels, bufferSize [ms]
        this.dispatcher = new PdUiDispatcher();
        PdBase.setReceiver(this.dispatcher);
        /* TODO: Listeners go here (if necessary) */
    }

    private void startPdAudio() {
        if (!this.pdService.isRunning()) {
            Intent intent = new Intent(MapActivity.this, MapActivity.class);
            this.pdService.startAudio(intent, R.drawable.ab_logo, "Triangulation Device", "Return to Triangulation Device");
            // Starts audio and creates a notification pointing to this activity
            // To start audio with no notification, give startAudio() 0 args
        }
    }

    private void initSystemServices() {
        TelephonyManager telephonyManager = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
        telephonyManager.listen(new PhoneStateListener() {
            @Override
            public void onCallStateChanged(int state, String incomingNumber) {
                if (MapActivity.this.pdService == null)
                    return;
                if (state == TelephonyManager.CALL_STATE_IDLE) {
                    startPdAudio();
                    // TODO: Handle possible edge case
                    // Person has app open, sound off, receives call
                    // (undesired sound starts when call ends?)
                } else {
                    MapActivity.this.pdService.stopAudio();
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

    public void pdChangeMyLocation(Location location) {
        // I know you can convert to HMS format, but using that requires String
        // conversion & manipulation...
        // TODO: Use formatted strings to extract HMS rather than tons of math
        // TODO: Split extractHMS to lat OR long rather than lat AND long?
        Log.i(getClass().toString(), "pdChangeMyLocation " + location.toString());
        HashMap<String, Float> gpsNamesToVals = extractHMS(location);
        for (HashMap.Entry<String, Float> entry : gpsNamesToVals.entrySet()) {
            PdBase.sendFloat(entry.getKey(), entry.getValue());
        }

        if (this.theirLocation != null) {
            pdChangeProximity(location, this.theirLocation);
        }
    }

    public void pdChangeProximity(Location myLocation, Location theirLocation) {
        Log.i(getClass().toString(), "pdChangeProximity " + myLocation.toString());
        Log.i(getClass().toString(), "pdChangeProximity " + theirLocation.toString());
        HashMap<String, Float> myHMS = extractHMS(myLocation);
        HashMap<String, Float> theirHMS = extractHMS(theirLocation);

        float proxlat = Math.abs(myHMS.get("lats") - theirHMS.get("lats"));
        float proxlong = Math.abs(myHMS.get("longs") - theirHMS.get("longs"));

        PdBase.sendFloat("proxlat", proxlat);
        PdBase.sendFloat("proxlong", proxlong);
    }

    public HashMap<String, Float> extractHMS(Location location) {
        // Returns a HashMap of H, M, S doubles (values)
        // mapped to their Pd variable names (keys)
        HashMap<String, Float> result = new HashMap<String, Float>();
        double latitude = location.getLatitude();
        double longitude = location.getLongitude();
        double lath = Math.floor(latitude);
        double longh = Math.floor(longitude);
        double latm = Math.floor(60d * (latitude - lath));
        double longm = Math.floor(60d * (longitude - longh));
        double lats = Math.floor(3600d * (latitude - lath - latm / 60d));
        double longs = Math.floor(3600d * (longitude - longh - longm / 60d));

        result.put("lath", (float) lath);
        result.put("longh", (float) longh);
        result.put("latm", (float) latm);
        result.put("longm", (float) longm);
        result.put("lats", (float) lats);
        result.put("longs", (float) longs);
        return result;
    }

    public void pdChangeXfade(float level) {
        // Change the xFade between the two users
        // 0 = 100% user1 (me/my)
        // 1 = 100% user2 (them/their)
        if (0 <= level && level <= 1) {
            // THE LEVEL HAS TO BE IN THE RANGE OF (0,1)
            PdBase.sendFloat("xfade", level);
        }
    }

    /* Pd code ends here (for the most part) */

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Bind & init PdService
        bindService(new Intent(this, PdService.class), this.pdConnection, BIND_AUTO_CREATE);
        initSystemServices();

        Typefaces.loadTypefaces(this);

        setContentView(R.layout.map_activity);
        this.resources = getResources();

        this.myWaveform = (WaveformView) findViewById(R.id.my_waveform);
        this.theirWaveform = (WaveformView) findViewById(R.id.their_waveform);
        this.theirWaveform.setDeviceName(this.resources.getString(R.string.paired_device));

        this.waveforms = (DraggableWeightView) findViewById(R.id.waveform);
        this.waveforms.setListener(new DraggableWeightView.Listener() {
            @Override
            public void onChanged(double topBottomRatio) {
                double xfade = 1d - topBottomRatio;
                pdChangeXfade((float) xfade);
            }
        });

        this.radar = (RadarView) findViewById(R.id.devices_map);

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
        unbindService(this.pdConnection);
    }

    @Override
    public void onLocationChanged(Location location) {
        this.myWaveform.setLocation(location);
        this.radar.setLocation(location);

        if (this.recording) {
            this.path.addMine(location);
        }

        pdChangeMyLocation(location);
    }

    @Override
    public void onCompassChanged(float azimuth) {
        this.radar.setAzimuth(azimuth);
    }

    public void theirLocationChanged(Location location) {
        this.theirWaveform.setLocation(location);
        this.radar.setOtherLocation(location);

        if (this.recording) {
            this.path.addTheirs(location);
        }

        pdChangeProximity(getLocation(), this.theirLocation);
    }

    @Override
    protected void onResume() {
        super.onResume();
        this.path.open();
    }

    @Override
    protected void onPause() {
        this.path.close();
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

    public void start(View buttonView) {
        this.recording = true;
        this.startStopButton.setText(this.resources.getString(R.string.stop));
        this.startStopButton.setBackgroundResource(R.drawable.stop_button);
        this.startStopButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startPdAudio(); // start Pd patch
                pdChangeMyLocation(getLocation());
                pdChangeXfade(0f);
                PdBase.sendBang("trigger");
                stop(v);
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
            @Override
            public void onClick(DialogInterface dialog, int whichButton) {
                String title = input.getText().toString();
                MapActivity.this.path.finish(title);
            }
        });

        alert.setNegativeButton("Discard", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int whichButton) {
                MapActivity.this.path.finish(null);
            }
        });
        alert.show();

        this.startStopButton.setText(this.resources.getString(R.string.start));
        this.startStopButton.setBackgroundResource(R.drawable.start_button);
        this.startStopButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                MapActivity.this.pdService.stopAudio(); // stop Pd patch
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
