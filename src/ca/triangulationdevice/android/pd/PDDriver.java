package ca.triangulationdevice.android.pd;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;

import org.puredata.android.io.AudioParameters;
import org.puredata.android.service.PdService;
import org.puredata.android.utils.PdUiDispatcher;
import org.puredata.core.PdBase;
import org.puredata.core.PdListener;
import org.puredata.core.utils.IoUtils;

import android.app.Service;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.location.Location;
import android.os.IBinder;
import android.util.Log;
import de.vndvl.chrs.triangulationdevice.R;

/**
 * An encapsulation of our PD stuff.
 */
public class PDDriver {

    private static final String TAG = "PDDriver";

    private boolean started = false;

    private final Context context;
    private PdUiDispatcher dispatcher;
    private PdService pdService;

    private Location myLocation;
    private Location theirLocation;

    private HashMap<String, Float> myHMS;

    private Listener listener;

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

    private float azimuth;

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
    }

    /**
     * Start the audio!
     */
    public void start() {
        this.started = true;
        startPdAudio();
        myLocationChanged(this.myLocation);
        pdChangeXfade(0.5f);
        PdBase.sendBang("trigger");
    }

    /**
     * Stop the audio. Awwww.
     */
    public void stop() {
        this.started = false;
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
        int inChannels = 1;
        int outChannels = 2;
        float bufferSize = 10;
        this.pdService.initAudio(sampleRate, inChannels, outChannels, bufferSize);
        this.dispatcher = new PdUiDispatcher();
        PdBase.setReceiver(this.dispatcher);

        this.dispatcher.addListener("u1GPSwave", new PdListener.Adapter() {
            @Override
            public void receiveFloat(String source, float value) {
                // Log.d(TAG, "u1GPSwave: " + value);
                if (PDDriver.this.started) {
                    PDDriver.this.listener.myFrequencyChanged(0, value);
                }
            }
        });

        this.dispatcher.addListener("u2proxwave", new PdListener.Adapter() {
            @Override
            public void receiveFloat(String source, float value) {
                // Log.d(TAG, "u2proxwave: " + value);
                if (PDDriver.this.started) {
                    PDDriver.this.listener.theirFrequencyChanged(0, value);
                }
            }
        });
    }

    private void startPdAudio() {
        if (!this.pdService.isRunning()) {
            this.pdService.startAudio();
            // Starts audio and creates a notification pointing to this activity
            // To start audio with no notification, give startAudio() 0 args
        }
    }

    private void loadPatch() throws IOException {
        File dir = this.context.getFilesDir();
        IoUtils.extractZipResource(this.context.getResources().openRawResource(R.raw.triangulationdevice_comp), dir, true);
        File patchFile = new File(dir, "triangulationdevice_compREV_10_28.4.pd");
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
        pdChangeProximity(this.myLocation, this.theirLocation);
    }

    /**
     * Updates "our" location, sending it to our PD patch.
     * 
     * @param location
     *            A new {@link Location} to use for us.
     */
    public void myLocationChanged(Location location) {
        this.myLocation = location;
        this.myHMS = getHMS(this.myLocation);

        for (HashMap.Entry<String, Float> entry : this.myHMS.entrySet()) {
            PdBase.sendFloat(entry.getKey(), entry.getValue());
            Log.d(TAG, entry.getKey() + ": " + entry.getValue());
        }

        if (this.theirLocation != null) {
            pdChangeProximity(location, this.theirLocation);
        }
    }

    public void pdChangeGyroscope(float azimuth, float pitch, float roll) {
        this.azimuth = azimuth;
        PdBase.sendFloat("azimuth", azimuth);
        PdBase.sendFloat("pitch", pitch);
        PdBase.sendFloat("roll", roll);
    }

    public void pdChangeProximity(Location myLocation, Location theirLocation) {
        this.myHMS = getHMS(myLocation);
        Log.d(TAG, String.format("Me: %s, them: %s", myLocation, theirLocation));

        // Send over the bearing.
        float bearing = (float) Math.toRadians(myLocation.bearingTo(theirLocation)) + this.azimuth;
        PdBase.sendFloat("androidbearing", bearing);
        Log.d(TAG, "androidbearing: " + bearing);

        // Send over the distance.
        float distance = myLocation.distanceTo(theirLocation);
        PdBase.sendFloat("androidproximity", distance);
        Log.d(TAG, "androidproximity: " + distance);
    }

    public HashMap<String, Float> getHMS(Location location) {
        /* Same thing as extractHMS but using strings */

        HashMap<String, Float> result = new HashMap<String, Float>();
        double latitude = location.getLatitude();
        double longitude = location.getLongitude();

        int lath = (int) latitude;
        int longh = (int) longitude;

        String latStr = Location.convert(latitude, Location.FORMAT_SECONDS);
        String longStr = Location.convert(longitude, Location.FORMAT_SECONDS);

        float latm = Float.parseFloat(latStr.substring(latStr.indexOf(':') + 1, latStr.lastIndexOf(':')));
        float longm = Float.parseFloat(longStr.substring(longStr.indexOf(':') + 1, longStr.lastIndexOf(':')));

        float lats = Float.parseFloat(latStr.substring(latStr.lastIndexOf(':') + 1, latStr.indexOf('.')));
        float longs = Float.parseFloat(longStr.substring(longStr.lastIndexOf(':') + 1, longStr.indexOf('.')));

        result.put("androidlath", (float) lath);
        result.put("androidlongh", (float) longh);
        result.put("androidlatm", latm);
        result.put("androidlongm", longm);
        result.put("androidlats", lats);
        result.put("androidlongs", longs);
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
        PdBase.sendFloat("androidxfade", Math.min(1, Math.max(0, level)));
    }

    public void setListener(Listener listener) {
        this.listener = listener;
    }

    public interface Listener {
        public void myFrequencyChanged(int wave_index, float newFrequency);

        public void theirFrequencyChanged(int wave_index, float newFrequency);
    }
}
