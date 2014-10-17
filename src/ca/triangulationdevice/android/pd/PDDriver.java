package ca.triangulationdevice.android.pd;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;

import org.puredata.android.io.AudioParameters;
import org.puredata.android.service.PdService;
import org.puredata.android.utils.PdUiDispatcher;
import org.puredata.core.PdBase;
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

    private final Context context;
    private PdUiDispatcher dispatcher;
    private PdService pdService;

    private Location myLocation;
    private Location theirLocation;

    private HashMap<String, Float> myHMS;
    private HashMap<String, Float> theirHMS;

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
            this.pdService.startAudio();
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
        // TODO: Split extractHMS to lat OR long rather than lat AND long?
        // TODO: Storing HMS as ints could make things more efficient

        this.myLocation = location;
        this.myHMS = getHMS(this.myLocation);

        for (HashMap.Entry<String, Float> entry : this.myHMS.entrySet()) {
            System.out.println(entry.getKey());
            System.out.println(entry.getValue());
            PdBase.sendFloat(entry.getKey(), entry.getValue());
        }

        if (this.theirLocation != null) {
            pdChangeProximity(location, this.theirLocation);
        }
    }

    public void pdChangeProximity(Location myLocation, Location theirLocation) {
        this.myHMS = getHMS(myLocation);
        this.theirHMS = getHMS(theirLocation);

        float proxlat = Math.abs(this.myHMS.get("lats") - this.theirHMS.get("lats"));
        float proxlong = Math.abs(this.myHMS.get("longs") - this.theirHMS.get("longs"));

        PdBase.sendFloat("proxlat", proxlat);
        PdBase.sendFloat("proxlong", proxlong);
    }

    public HashMap<String, Float> getHMS(Location location) {
        /* Same thing as extractHMS but using strings */
        // TODO: Use regexes (rather than substrings) if they're faster

        HashMap<String, Float> result = new HashMap<String, Float>();
        double latitude = location.getLatitude();
        double longitude = location.getLongitude();

        // This int cast will/should always work properly
        int lath = (int) latitude;
        int longh = (int) longitude;

        String latStr = Location.convert(latitude, Location.FORMAT_SECONDS);
        String longStr = Location.convert(longitude, Location.FORMAT_SECONDS);

        float latm = Float.parseFloat(latStr.substring(latStr.indexOf(':') + 1, latStr.lastIndexOf(':')));
        float longm = Float.parseFloat(longStr.substring(longStr.indexOf(':') + 1, longStr.lastIndexOf(':')));

        float lats = Float.parseFloat(latStr.substring(latStr.lastIndexOf(':') + 1, latStr.indexOf('.')));
        float longs = Float.parseFloat(longStr.substring(longStr.lastIndexOf(':') + 1, longStr.indexOf('.')));

        result.put("lath", (float) lath);
        result.put("longh", (float) longh);
        result.put("latm", latm);
        result.put("longm", longm);
        result.put("lats", lats);
        result.put("longs", longs);
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
            // THE LEVEL MUST BE IN THE RANGE (0,1)
            PdBase.sendFloat("xfade", level);
        }
    }
}
