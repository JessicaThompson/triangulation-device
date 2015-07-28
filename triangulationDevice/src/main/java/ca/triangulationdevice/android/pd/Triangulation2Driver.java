package ca.triangulationdevice.android.pd;

import android.content.Context;
import android.location.Location;
import android.util.Log;

import java.io.IOException;
import java.util.HashMap;

public class Triangulation2Driver extends PDDriver {

    private static final String TAG = "Triangulation2Driver";

    private HashMap<String, Float> myHMS;
    private Location myLocation;
    private Location theirLocation;

    private static final String FILENAME = "triangulationdevice_interfacetest_Jul27_v3.pd";

    public Triangulation2Driver(Context context) throws IOException {
        super(context, FILENAME);
    }

    @Override
    public void start() {
        super.start();
        this.sendBang("trigger");
        this.sendFloat("control_octave_(24_-_60)", 30);
        Log.i(TAG, String.format("Sent control_octave_(24_-60) = %.2f", 30f));
    }

    public void testCircle(float width, float height) {
        this.sendFloat("modulator_freq1", width / 30);
        this.sendFloat("modulator_freq2", height / 30);
        Log.i(TAG, String.format("Sent modulator_freq1 = %.2f modulator_freq2 = %.2f", width / 30, height / 30));

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

    private float getSeconds(double tude) {
        return (float) (((tude % 1) * 60) % 1) * 60;
    }
}
