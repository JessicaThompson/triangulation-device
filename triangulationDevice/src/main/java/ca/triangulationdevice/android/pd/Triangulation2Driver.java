package ca.triangulationdevice.android.pd;

import android.content.Context;
import android.graphics.RectF;
import android.location.Location;
import android.util.Log;

import java.io.IOException;
import java.util.HashMap;

import ca.triangulationdevice.android.ui.views.OvalsView;

public class Triangulation2Driver extends PDDriver implements OvalsView.CircleChangedListener {

    private static final String TAG = "Triangulation2Driver";

    private static final int SCALE = 30;

    private HashMap<String, Float> myHMS;
    private Location myLocation;
    private Location theirLocation;

    private static final String FILENAME = "triangulationdevice_interfacetest_Aug9.pd";

    public Triangulation2Driver(Context context) throws IOException {
        super(context, FILENAME);
    }

    @Override
    public void start() {
        super.start();
        this.sendBang("trigger");
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
    }

    /**
     * Updates "our" location, sending it to our PD patch.
     *
     * @param location
     *            A new {@link Location} to use for us.
     */
    public void myLocationChanged(Location location) {
        this.myLocation = location;

        this.sendFloat("androidlongs", getSeconds(this.myLocation.getLongitude()));
        this.sendFloat("androidlats", getSeconds(this.myLocation.getLatitude()));
    }

    public void myStepCountChanged(float freq) {
        this.sendFloat("android1stepcounter", freq);
        Log.i(TAG, String.format("Step count: %.2f", freq));
    }

    private float getSeconds(double tude) {
        return (float) (((tude % 1) * 60) % 1) * 60;
    }
}
