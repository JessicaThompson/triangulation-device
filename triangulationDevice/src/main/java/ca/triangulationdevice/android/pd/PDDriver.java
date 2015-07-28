package ca.triangulationdevice.android.pd;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashMap;

import android.app.Service;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.location.Location;
import android.os.IBinder;
import android.util.Log;
import android.widget.Toast;

import org.puredata.android.io.AudioParameters;
import org.puredata.android.service.PdService;
import org.puredata.android.utils.PdUiDispatcher;
import org.puredata.core.PdBase;

/**
 * An encapsulation of our PD stuff.
 */
public abstract class PDDriver {

    private static final String TAG = "PDDriver";
    private final String filename;

    private static final int IN_CHANNELS = 0;
    private static final int OUT_CHANNELS = 2;
    private static final float BUFFER_SIZE = 10;

    private boolean started = false;

    private final Context context;
    private PdUiDispatcher dispatcher;
    private PdService pdService;

    private final ServiceConnection pdConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            PDDriver.this.pdService = ((PdService.PdBinder) service).getService();
            try {
                PDDriver.this.initPd();
                PDDriver.this.loadPatch();
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
    public PDDriver(Context context, String filename) throws IOException {
        this.context = context;
        this.filename = filename;
        this.copyAssetToFilesDir(context, filename);
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
        this.startPdAudio();
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
        this.pdService.initAudio(sampleRate, IN_CHANNELS, OUT_CHANNELS, BUFFER_SIZE);
        this.dispatcher = new PdUiDispatcher();
        PdBase.setReceiver(this.dispatcher);
    }

    private void copyAssetToFilesDir(Context context, String assetName) throws IOException {
        InputStream myInput = context.getAssets().open(assetName);

        OutputStream myOutput = new FileOutputStream(context.getFilesDir() + assetName);
        byte[] buffer = new byte[1024];
        int length;

        while ((length = myInput.read(buffer)) > 0) {
            myOutput.write(buffer, 0, length);
        }
        myInput.close();
        myOutput.flush();
        myOutput.close();
    }

    private void startPdAudio() {
        if (!this.pdService.isRunning()) {
            // Starts audio and creates a notification pointing to this activity
            // To start audio with no notification, give startAudio() 0 args
            this.pdService.startAudio();
        } else {
            Log.e(TAG, "Tried to start PD audio, but service wasn't running.");
        }
    }

    private void loadPatch() throws IOException {
        File dir = this.context.getFilesDir();
        File patchFile = new File(dir, filename);
        PdBase.openPatch(patchFile.getAbsolutePath());
    }

    protected void sendFloat(String key, float value) {
        PdBase.sendFloat(key, value);
    }

    protected void sendBang(String key) {
        PdBase.sendBang(key);
    }

    public HashMap<String, Float> getHMS(Location location) {
        HashMap<String, Float> result = new HashMap<>();
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
}
