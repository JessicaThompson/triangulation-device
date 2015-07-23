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

    private static final String FILENAME = "triangulationdevice_interfacetest_Jul20.pd";

    public Triangulation2Driver(Context context) throws IOException {
        super(context, FILENAME);
    }

    @Override
    public void start() {
        super.start();
        this.sendBang("trigger");
    }

    public void testCircle(float width, float height) {
        this.sendFloat("testcircleh", width);
        this.sendFloat("testcirclev", height);
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
        this.myHMS = getHMS(this.myLocation);

        for (HashMap.Entry<String, Float> entry : this.myHMS.entrySet()) {
            this.sendFloat(entry.getKey(), entry.getValue());
//             Log.d(TAG, entry.getKey() + ": " + entry.getValue());
        }
    }
}
