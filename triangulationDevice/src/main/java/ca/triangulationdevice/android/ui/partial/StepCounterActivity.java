package ca.triangulationdevice.android.ui.partial;

import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.util.Log;

public abstract class StepCounterActivity extends CompassActivity implements SensorEventListener {

    private static final String TAG = "StepCounterActivity";
    private static final float ALPHA = 0.25f;

    float lastSteps = 0;
    long lastUpdated = 0;

    private Sensor stepSensor;
    private float lastFreq = 0;

    @Override
    protected void onCreate(Bundle savedState) {
        super.onCreate(savedState);
        Sensor maybeStepSensor = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER);
        if (maybeStepSensor != null) {
            stepSensor = maybeStepSensor;
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        sensorManager.registerListener(this, stepSensor, SensorManager.SENSOR_DELAY_NORMAL);
        lastUpdated = System.nanoTime();
    }

    @Override
    protected void onPause() {
        super.onPause();
        sensorManager.unregisterListener(this);
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        super.onSensorChanged(event);

        if (event.sensor.getType() == Sensor.TYPE_STEP_COUNTER) {
            // Get the data we need in steps and seconds.
            float steps = event.values[0];
            float elapsed = (event.timestamp - lastUpdated) / 1000000000f;

            // Steps per minute = total steps / elapsed time in minutes.
            float freq = (steps - lastSteps) / (elapsed / 60f);
            float smoothed = this.lastFreq + ALPHA * (freq - this.lastFreq);
            onStepCountChanged(smoothed);

            lastFreq = smoothed;
            lastSteps = steps;
            lastUpdated = event.timestamp;
        }
    }

    abstract protected void onStepCountChanged(float freq);
}
