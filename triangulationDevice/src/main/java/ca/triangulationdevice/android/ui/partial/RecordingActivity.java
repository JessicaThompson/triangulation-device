package ca.triangulationdevice.android.ui.partial;

import android.app.Activity;
import android.app.ProgressDialog;
import android.location.Location;
import android.os.Bundle;
import android.util.Log;

import com.couchbase.lite.CouchbaseLiteException;

import java.io.IOException;
import java.util.Date;

import ca.triangulationdevice.android.model.Session;
import ca.triangulationdevice.android.model.User;
import ca.triangulationdevice.android.pd.Triangulation2Driver;
import ca.triangulationdevice.android.util.GetCityTask;

/**
 * An {@link Activity} which lets the user create sessions, connect with other
 * devices, and see the current status of the other connected user.
 */
public abstract class RecordingActivity extends StepCounterActivity {
    @SuppressWarnings("unused")
    private static final String TAG = "RecordingActivity";

    protected boolean started = false;
    protected boolean recording = false;
    protected Triangulation2Driver pd;

    protected Session session;

    private long lastCompassUpdate = 0;
    private float lastCompass = 0;
    private float lastStepCount = 0;

    private ProgressDialog savingDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        try {
            this.pd = new Triangulation2Driver(this);
            this.pd.initServices();
        } catch (IOException exception) {
            Log.e(TAG, exception.getMessage());
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        this.pd.close();
    }

    @Override
    public void onLocationChanged(Location location) {
        super.onLocationChanged(location);

        if (this.started) {
            if (this.recording) {
                float[] lastOrientation = this.getLastOrientation();
                this.session.paths.get(Session.Path.MINE).addPoint(location, lastOrientation[0], lastOrientation[1], lastOrientation[2], lastStepCount);
            }
            this.pd.myLocationChanged(location);
        }
    }

    @Override
    protected void onCompassChanged(float azimuth, float pitch, float roll) {
        if (this.started) {
//            this.session.paths.get(Session.Path.MINE).addPoint(getLocation(), azimuth, pitch, roll);

            if (System.currentTimeMillis() - this.lastCompassUpdate > 200) {
                this.lastCompassUpdate = System.currentTimeMillis();
                this.lastCompass = azimuth;
            }
        }
    }

    @Override
    protected void onStepCountChanged(float freq) {
        this.lastStepCount = freq;
        if (this.started) {
            this.pd.myStepCountChanged(freq);
        }
    }

    protected void startAudio() {
        this.started = true;
        this.pd.start();
    }

    protected void startRecording(User user) {
        this.recording = true;
        this.session = new Session();
        this.session.ownerId = user.id;
        this.pd.record();
    }

    protected void stop() {
        if (this.started) {
            this.session.saved = new Date();
        }
        this.started = false;
        this.pd.stop();
    }

    protected void save(String filename, String title, String description) throws IOException, CouchbaseLiteException {
        this.pd.save(filename);
        this.session.audioFilename = filename;
        this.session.title = title;
        this.session.description = description;
        this.session.startLocation = this.session.paths.get(Session.Path.MINE).points.get(0).location;
        GetCityTask task = new GetCityTask(this, this.session.startLocation) {
            @Override
            protected void onPostExecute(String result) {
                session.location = result;
                try {
                    application.userManager.add(session);
                } catch (CouchbaseLiteException e) {
                    e.printStackTrace();
                }
            }
        };
        task.execute();
    }
}
