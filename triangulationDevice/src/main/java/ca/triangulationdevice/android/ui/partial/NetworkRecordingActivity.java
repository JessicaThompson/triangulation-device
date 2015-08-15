package ca.triangulationdevice.android.ui.partial;

import android.content.Intent;
import android.location.Location;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.widget.Toast;

import com.couchbase.lite.CouchbaseLiteException;

import ca.triangulationdevice.android.ipc.FloatZeroMQClient;
import ca.triangulationdevice.android.ipc.FloatZeroMQServer;
import ca.triangulationdevice.android.ipc.ParcelableZeroMQClient;
import ca.triangulationdevice.android.ipc.ParcelableZeroMQServer;
import ca.triangulationdevice.android.model.Session;
import ca.triangulationdevice.android.model.User;

public abstract class NetworkRecordingActivity extends RecordingActivity {

    public static final String ID_EXTRA = "userid";
    public static final int PARCELABLE = 0;
    public static final int FLOAT = 1;

    private static final String TAG = "NetworkRecording";

    private boolean solo = true;
    private Thread parcelableThread;
    private Thread floatThread;
    private String ip;
    protected User otherUser;
    private float theirStepCount = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Handler transferHandler = new Handler(new NetworkRecordingHandler());
        User currentUser = this.application.userManager.getCurrentUser();
        ParcelableZeroMQServer<Location> zeroServer = new ParcelableZeroMQServer<>(transferHandler, Location.CREATOR);
        parcelableThread = new Thread(zeroServer);
        FloatZeroMQServer floatServer = new FloatZeroMQServer(transferHandler);
        floatThread = new Thread(floatServer);

        Intent intent = getIntent();
        try {
            String id = intent.getStringExtra(ID_EXTRA);
            if (id != null && !id.equals(this.application.userManager.getCurrentUser().id)) {
                solo = false;
                otherUser = this.application.userManager.getUser(id);
                ip = otherUser.ip;
            }
        } catch (CouchbaseLiteException e) {
            this.finish();
            Toast.makeText(this, "Could not load user - uh oh?", Toast.LENGTH_LONG).show();
            Log.e(TAG, "Could not load user: " + e.getMessage());
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (!solo) {
            if (!this.parcelableThread.isAlive())
                this.parcelableThread.start();
            if (!this.floatThread.isAlive())
                this.floatThread.start();
        }
    }

    @Override
    protected void onPause() {
        if (!solo) {
            try {
                this.parcelableThread.join(500);
                this.floatThread.join(500);
            } catch (InterruptedException e) {
                // Don't care.
            }
        }
        super.onPause();
    }

    @Override
    public void onLocationChanged(Location location) {
        super.onLocationChanged(location);
        if (!solo && started)
            this.sendLocation(location);
    }

    @Override
    protected void onStepCountChanged(float newCount) {
        super.onStepCountChanged(newCount);
        if (!solo && started)
            this.sendStepCount(newCount);
    }

    protected void sendLocation(Location location) {
        Log.d(TAG, "Sending location: " + location.getLongitude() + ", " + location.getLatitude());
            new ParcelableZeroMQClient<>(ip).execute(location);
    }

    protected void sendStepCount(float stepCount) {
        Log.d(TAG, "Sending step count: " + stepCount);
        new FloatZeroMQClient(ip).execute(stepCount);
    }

    protected void onLocationReceived(Location location) {
        Log.d(TAG, "Remote location: " + location.getLongitude() + ", " + location.getLatitude());
        if (started) {
            this.pd.theirLocationChanged(location);
            if (recording) {
                this.session.paths.get(Session.Path.THEIRS).addPoint(location, 0, 0, 0, 0);
            }
        }
    }

    protected void onStepCountReceived(float stepCount) {
        Log.d(TAG, "Remote step count: " + stepCount);
        if (started) {
            this.pd.theirStepCountChanged(stepCount);
            theirStepCount = stepCount;
        }
    }

    // A Handler to receive messages from another BluetoothIPCActivity.
    private class NetworkRecordingHandler implements Handler.Callback {
        @Override
        public boolean handleMessage(Message msg) {
            switch (msg.what) {
                case PARCELABLE:
                    @SuppressWarnings("unchecked") Location receivedObject = (Location) msg.obj;
                    onLocationReceived(receivedObject);
                    break;
                case FLOAT:
                    onStepCountReceived((Float) msg.obj);
                    break;
                default:
                    Log.d(TAG, "No idea what this message is." + msg.toString());
                    break;
            }
            return true;
        }
    }
}
