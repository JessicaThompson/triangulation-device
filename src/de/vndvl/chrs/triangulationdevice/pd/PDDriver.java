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

    public PDDriver(Context context) {
        this.context = context;
    }

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

    public void start() {
        startPdAudio();
        myLocationChanged(this.myLocation);
        pdChangeXfade(0.5f);
        PdBase.sendBang("trigger");
    }

    public void stop() {
        if (this.pdService != null) {
            this.pdService.stopAudio();
        }
    }

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

    public void theirLocationChanged(Location location) {
        this.theirLocation = location;
        pdChangeProximity(location, this.theirLocation);
    }

    public void myLocationChanged(Location location) {
        // TODO: Split extractHMS to lat OR long rather than lat AND long?
        // TODO: Storing HMS as ints could make things more efficient

        this.myLocation = location;
        myHMS = getHMS(myLocation);

        for (HashMap.Entry<String, Float> entry : myHMS.entrySet()) {
            System.out.println(entry.getKey());
            System.out.println(entry.getValue());
            PdBase.sendFloat(entry.getKey(), entry.getValue());
        }

        if (this.theirLocation != null) {
            pdChangeProximity(location, this.theirLocation);
        }
    }

    public void pdChangeProximity(Location myLocation, Location theirLocation) {
        myHMS = getHMS(myLocation);
        theirHMS = getHMS(theirLocation);

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

        // This int cast will/should always work properly
        int lath = (int) latitude;
        int longh = (int) longitude;

        double latm = Math.floor(60d * (Math.abs(latitude) - Math.abs(lath)));
        double longm = Math.floor(60d * (Math.abs(longitude) - Math.abs(longh)));

        double lats = Math.floor(3600d * (Math.abs(latitude) - Math.abs(lath) - latm / 60d));
        double longs = Math.floor(3600d * (Math.abs(longitude) - Math.abs(longh) - longm / 60d));

        result.put("lath", (float) lath);
        result.put("longh", (float) longh);
        result.put("latm", (float) latm);
        result.put("longm", (float) longm);
        result.put("lats", (float) lats);
        result.put("longs", (float) longs);
        return result;
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
