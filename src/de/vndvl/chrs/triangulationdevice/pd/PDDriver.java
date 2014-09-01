package de.vndvl.chrs.triangulationdevice.pd;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;

import org.puredata.android.io.AudioParameters;
import org.puredata.android.service.PdService;
import org.puredata.android.utils.PdUiDispatcher;
import org.puredata.core.PdBase;
import org.puredata.core.utils.IoUtils;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.location.Location;
import android.os.IBinder;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.util.Log;
import de.vndvl.chrs.triangulationdevice.R;

/**
 * An encapsulation of our PD stuff.
 */
public class PDDriver {

    private final Context context;
    private PdUiDispatcher dispatcher;
    private PdService pdService;

    private Location myLocation;
    private Location theirLocation;

    private final ServiceConnection pdConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            PDDriver.this.pdService = ((PdService.PdBinder) service).getService();
            try {
                initPd();
                loadPatch();
            } catch (IOException e) {
                Log.e(getClass().toString(), e.toString());
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            // Apparently this method will never be called?
        }
    };

    /**
     * It's a constructor! Ain't it cute?
     * 
     * @param context
     *            A context from which to get access to strings and start
     *            {@link Service}s.
     */
    public PDDriver(Context context) {
        this.context = context;
    }

    /**
     * Initializes our background PureData service and sets a manager to listen
     * for phone calls and shut us up if needed.
     */
    public void initServices() {
        Intent pdIntent = new Intent(this.context, PdService.class);
        this.context.bindService(pdIntent, this.pdConnection, Context.BIND_AUTO_CREATE);

        TelephonyManager telephonyManager = (TelephonyManager) this.context.getSystemService(Context.TELEPHONY_SERVICE);
        telephonyManager.listen(new PhoneStateListener() {
            @Override
            public void onCallStateChanged(int state, String incomingNumber) {
                if (PDDriver.this.pdService == null)
                    return;
                if (state == TelephonyManager.CALL_STATE_IDLE) {
                    startPdAudio();
                    // TODO: Handle possible edge case
                    // Person has app open, sound off, receives call
                    // (undesired sound starts when call ends?)
                } else {
                    PDDriver.this.pdService.stopAudio();
                }
            }
        }, PhoneStateListener.LISTEN_CALL_STATE);
    }

    /**
     * Start the audio!
     */
    public void start() {
        startPdAudio();
        myLocationChanged(this.myLocation);
        pdChangeXfade(0.5f);
        PdBase.sendBang("trigger");
    }

    /**
     * Stop the audio. Awwww.
     */
    public void stop() {
        if (this.pdService != null) {
            this.pdService.stopAudio();
        }
    }

    /**
     * Unbinds our background service and closes everything.
     */
    public void close() {
        this.context.unbindService(this.pdConnection);
    }

    private void initPd() throws IOException {
        int sampleRate = AudioParameters.suggestSampleRate();
        int inChannels = 0;
        int outChannels = 2;
        float bufferSize = 10;
        this.pdService.initAudio(sampleRate, inChannels, outChannels, bufferSize);
        this.dispatcher = new PdUiDispatcher();
        PdBase.setReceiver(this.dispatcher);
    }

    private void startPdAudio() {
        if (!this.pdService.isRunning()) {
            Intent intent = new Intent(this.context, this.context.getClass());
            this.pdService.startAudio(intent, R.drawable.ab_logo, "Triangulation Device", "Return to Triangulation Device");
            // Starts audio and creates a notification pointing to this activity
            // To start audio with no notification, give startAudio() 0 args
        }
    }

    private void loadPatch() throws IOException {
        File dir = this.context.getFilesDir();
        IoUtils.extractZipResource(this.context.getResources().openRawResource(R.raw.triangulationdevice_comp), dir, true);
        File patchFile = new File(dir, "triangulationdevice_comp.pd");
        PdBase.openPatch(patchFile.getAbsolutePath());
    }

    /**
     * Updates the "other" location, sending it to our PD patch.
     * 
     * @param location
     *            A new {@link Location} to use for the other person.
     */
    public void theirLocationChanged(Location location) {
        this.theirLocation = location;
        pdChangeProximity(location, this.theirLocation);
    }

    /**
     * Updates "our" location, sending it to our PD patch.
     * 
     * @param location
     *            A new {@link Location} to use for us.
     */
    public void myLocationChanged(Location location) {
        this.myLocation = location;

        // I know you can convert to HMS format, but using that requires String
        // conversion & manipulation...
        // TODO: Use formatted strings to extract HMS rather than tons of math
        // TODO: Split extractHMS to lat OR long rather than lat AND long?
        HashMap<String, Float> gpsNamesToVals = extractHMS(location);
        for (HashMap.Entry<String, Float> entry : gpsNamesToVals.entrySet()) {
            PdBase.sendFloat(entry.getKey(), entry.getValue());
        }

        if (this.theirLocation != null) {
            pdChangeProximity(location, this.theirLocation);
        }
    }

    private void pdChangeProximity(Location myLocation, Location theirLocation) {
        HashMap<String, Float> myHMS = extractHMS(myLocation);
        HashMap<String, Float> theirHMS = extractHMS(theirLocation);

        float proxlat = Math.abs(myHMS.get("lats") - theirHMS.get("lats"));
        float proxlong = Math.abs(myHMS.get("longs") - theirHMS.get("longs"));

        PdBase.sendFloat("proxlat", proxlat);
        PdBase.sendFloat("proxlong", proxlong);
    }

    private HashMap<String, Float> extractHMS(Location location) {
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

    /**
     * Updates our PD patch with the cross-fader value from our UI.
     * 
     * @param level
     */
    public void pdChangeXfade(float level) {
        // Change the xFade between the two users
        // 0 = 100% user1 (me/my)
        // 1 = 100% user2 (them/their)
        if (0 <= level && level <= 1) {
            // THE LEVEL HAS TO BE IN THE RANGE OF (0,1)
            PdBase.sendFloat("xfade", level);
        }
    }
}
