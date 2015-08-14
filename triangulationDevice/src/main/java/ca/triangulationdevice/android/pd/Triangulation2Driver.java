package ca.triangulationdevice.android.pd;

import android.content.Context;
import android.graphics.RectF;
import android.location.Location;
import android.util.Log;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.util.HashMap;

import ca.triangulationdevice.android.ui.views.OvalsView;

public class Triangulation2Driver extends PDDriver implements OvalsView.CircleChangedListener {

    private static final String TAG = "Triangulation2Driver";

    private static final int SCALE = 30;

    private HashMap<String, Float> myHMS;
    private Location myLocation;
    private Location theirLocation;

    private static final String FILENAME = "triangulationdevice_interfacetest_Aug11.pd";
    private static final String AUDIO_FILENAME = "~bytes";
    public static final String AUDIO_SUFFIX = "wav";

    public Triangulation2Driver(Context context) throws IOException {
        super(context, FILENAME);
    }

    @Override
    public void start() {
        super.start();
        this.sendBang("start");
    }

    @Override
    public void stop() {
        super.stop();
        this.sendBang("stop");
    }

    public void record() {
        this.sendBang("android1startrecording");
    }

    public void save(String filename) throws IOException {
        File audio = new File(context.getFilesDir() + "/" + AUDIO_FILENAME + "." + AUDIO_SUFFIX);
        File output = new File(context.getFilesDir() + "/" + filename + "." + AUDIO_SUFFIX);
        this.copy(audio, output);
    }

    protected void copy(File src, File dst) throws IOException {
        InputStream in = new FileInputStream(src);
        OutputStream out = new FileOutputStream(dst);

        // Transfer bytes from in to out
        byte[] buf = new byte[1024];
        int len;
        while ((len = in.read(buf)) > 0) {
            out.write(buf, 0, len);
        }
        in.close();
        out.close();
    }

    /**
     * Triggered when circle bounds change, updates PD patch with new values.
     *
     * @param index The index of the circle to be updated.
     * @param bounds A {@link RectF} which holds the circle's bounds.
     */
    public void onCircleChanged(int index, RectF bounds, int size) {
        float radius = size / 2;

        float top = SCALE * Math.max(0f, Math.min(1f, Math.abs(bounds.centerY() - bounds.top) / radius));
        float right = SCALE * Math.max(0f, Math.min(1f, Math.abs(bounds.right - bounds.centerX()) / radius));
        float bottom = SCALE * Math.max(0f, Math.min(1f, Math.abs(bounds.bottom - bounds.centerY()) / radius));
        float left = SCALE * Math.max(0f, Math.min(1f, Math.abs(bounds.centerX() - bounds.left) / radius));

        // From PD patch, indices are ordered clockwise from the top.
        // 1 is top, 2 is right, 3 is bottom, 4 is left, etc.
        this.sendFloat(String.format("android1c%d.%d", index, 1), top);
        this.sendFloat(String.format("android1c%d.%d", index, 2), right);
        this.sendFloat(String.format("android1c%d.%d", index, 3), bottom);
        this.sendFloat(String.format("android1c%d.%d", index, 4), left);
    }

    /**
     * Updates the "other" location, sending it to our PD patch.
     *
     * @param location
     *            A new {@link Location} to use for the other person.
     */
    public void theirLocationChanged(Location location) {
        this.theirLocation = location;

        this.sendFloat("android2long", getSeconds(this.theirLocation.getLongitude()));
        this.sendFloat("android2lat", getSeconds(this.theirLocation.getLatitude()));

        this.updateDiff();
    }

    /**
     * Updates "our" location, sending it to our PD patch.
     *
     * @param location
     *            A new {@link Location} to use for us.
     */
    public void myLocationChanged(Location location) {
        this.myLocation = location;

        this.sendFloat("android1long", getSeconds(this.myLocation.getLongitude()));
        this.sendFloat("android1lat", getSeconds(this.myLocation.getLatitude()));

        this.updateDiff();
    }

    private void updateDiff() {
        if (this.myLocation != null && this.theirLocation != null) {
            this.sendFloat("androidbearing", this.myLocation.bearingTo(theirLocation));
            this.sendFloat("androidproximity", this.myLocation.distanceTo(theirLocation));
        }
    }

    public void theirStepCountChanged(float freq) {
        this.sendFloat("android2stepcounter", freq);
    }

    public void myStepCountChanged(float freq) {
        this.sendFloat("android1stepcounter", freq);
    }

    private float getSeconds(double tude) {
        return (float) (((tude % 1) * 60) % 1) * 60;
    }
}
