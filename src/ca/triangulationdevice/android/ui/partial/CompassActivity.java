package ca.triangulationdevice.android.ui.partial;

import android.app.Activity;
import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;

/**
 * An {@link Activity} which updates subclasses with the compass direction that
 * the current user is pointed to (the "azimuth"). Subclasses must implement
 * {@link #onCompassChanged()} and deal with new direction values as they come
 * in.
 */
public abstract class CompassActivity extends TriangulationActivity implements SensorEventListener {
    private SensorManager sensorManager;
    private Sensor accelerometer;
    private Sensor magnetometer;
    private float[] gravity;
    private float[] geomagnetic;

    /**
     * Called when our orientation is updated.
     * 
     * @param azimuth
     *            The azimuth in radians between our orientation and north,
     *            positive being in the counter-clockwise direction.
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
                float orientation[] = new float[3];
                SensorManager.getOrientation(R, orientation);
                this.onCompassChanged(orientation[0]); // orientation contains:
                                                       // azimut, pitch and roll
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
