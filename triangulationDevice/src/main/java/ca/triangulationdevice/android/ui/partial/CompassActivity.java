package ca.triangulationdevice.android.ui.partial;

import android.app.Activity;
import android.content.Context;
import android.hardware.GeomagneticField;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.os.Bundle;

/**
 * An {@link Activity} which updates subclasses with the compass direction that
 * the current user is pointed to (the "azimuth"). Subclasses must implement
 * {@link #onCompassChanged(float, float, float)} and deal with new direction values as they come
 * in.
 */
public abstract class CompassActivity extends LocationActivity implements SensorEventListener {
    protected SensorManager sensorManager;
    private Sensor accelerometer;
    private Sensor magnetometer;
    private float[] gravity;
    private float[] geomagnetic;

    /*
     * time smoothing constant for low-pass filter 0 <= alpha <= 1 ; a smaller
     * value basically means more smoothing See:
     * http://en.wikipedia.org/wiki/Low-pass_filter#Discrete-time_realization
     */
    private static final float ALPHA = 0.05f;
    private float lastAzimuth = 0f;
    private float lastPitch = 0f;
    private float lastRoll = 0f;

    /**
     * Called when our orientation is updated.
     * 
     * @param outputX
     *            The azimuth in radians between our orientation and north,
     *            positive being in the counter-clockwise direction. Corrected
     *            to true north.
     */
    protected abstract void onCompassChanged(float outputX, float outputY, float outputZ);

    protected float[] getLastOrientation() {
        return new float[] { this.lastAzimuth, this.lastPitch, this.lastRoll };

    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        Sensor maybeAccelerometer = this.sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        Sensor maybeMagnetometer = this.sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
        if (maybeAccelerometer != null && maybeMagnetometer != null) {
            this.accelerometer = maybeAccelerometer;
            this.magnetometer = maybeMagnetometer;
        }
    }

    /**
     * Called when the accuracy has changed. I don't really give much of a shit
     * about this method's values, but you can subclass it if you care.
     * 
     * @param sensor
     *            The {@link Sensor} whose accuracy has changed.
     * @param accuracy
     *            The new accuracy value.
     */
    @Override
    public final void onAccuracyChanged(Sensor sensor, int accuracy) {
        // NOOP.
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            this.gravity = event.values;
        } else if (event.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD) {
            this.geomagnetic = event.values;
        }

        if (this.gravity != null && this.geomagnetic != null) {
            float R[] = new float[9];
            float I[] = new float[9];
            boolean success = SensorManager.getRotationMatrix(R, I, this.gravity, this.geomagnetic);
            if (success) {
                float orientation[] = new float[3]; // orientation contains:
                                                    // azimut, pitch and roll
                SensorManager.getOrientation(R, orientation);

                // We need a location to send this information, because
                // magnetic north is different than our north. This will correct
                // and location-based drift.
                //
                // If we don't have a location, we'll ignore the compass for
                // now, the reading will be inaccurate anyways.
                if (this.lastLocation != null) {
                    GeomagneticField geoField = new GeomagneticField(
                            Double.valueOf(this.lastLocation.getLatitude()).floatValue(),
                            Double.valueOf(this.lastLocation.getLongitude()).floatValue(),
                            Double.valueOf(this.lastLocation.getAltitude()).floatValue(),
                            System.currentTimeMillis());

                    float azimuth = orientation[0];
                    float pitch = orientation[1];
                    float roll = orientation[2];

                    azimuth -= Math.toRadians(geoField.getDeclination());

                    // The edges of the curve are +pi and -pi, so when it
                    // crosses over the line there'll be a big jump.
                    if (Math.abs(azimuth - this.lastAzimuth) >= Math.PI) {
                        // Use a low-pass filter to smooth the compass signal.
                        // http://en.wikipedia.org/wiki/Low-pass_filter#Algorithmic_implementation
                        azimuth = this.lastAzimuth + ALPHA * (azimuth - this.lastAzimuth);
                        pitch = this.lastPitch + ALPHA * (pitch - this.lastPitch);
                        roll = this.lastRoll + ALPHA * (roll - this.lastRoll);
                    }

                    // Send it to subclasses.
                    this.onCompassChanged(azimuth, pitch, roll);
                    this.lastAzimuth = azimuth;
                    this.lastPitch = pitch;
                    this.lastRoll = roll;
                }
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        this.sensorManager.registerListener(this, this.accelerometer, SensorManager.SENSOR_DELAY_UI);
        this.sensorManager.registerListener(this, this.magnetometer, SensorManager.SENSOR_DELAY_UI);
    }

    @Override
    protected void onPause() {
        super.onPause();
        this.sensorManager.unregisterListener(this);
    }

}
