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
 * {@link #onCompassChanged()} and deal with new direction values as they come
 * in.
 */
public abstract class CompassActivity extends LocationActivity implements SensorEventListener {
    private SensorManager sensorManager;
    private Sensor accelerometer;
    private Sensor magnetometer;
    private float[] gravity;
    private float[] geomagnetic;
    private Location location;

    /**
     * Called when our orientation is updated.
     * 
     * @param azimuth
     *            The azimuth in radians between our orientation and north,
     *            positive being in the counter-clockwise direction. Corrected
     *            to true north.
     */
    protected abstract void onCompassChanged(float azimuth);

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
    public void onLocationChanged(Location location) {
        this.location = location;
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
                if (this.location != null) {
                    GeomagneticField geoField = new GeomagneticField(
                            Double.valueOf(this.location.getLatitude()).floatValue(),
                            Double.valueOf(this.location.getLongitude()).floatValue(),
                            Double.valueOf(this.location.getAltitude()).floatValue(),
                            System.currentTimeMillis());

                    float azimuth = orientation[0];
                    azimuth -= Math.toRadians(geoField.getDeclination());

                    // Call our subclasses.
                    this.onCompassChanged(azimuth);
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
