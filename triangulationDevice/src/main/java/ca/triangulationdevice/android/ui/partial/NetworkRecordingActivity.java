package ca.triangulationdevice.android.ui.partial;

import android.location.Location;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.widget.Toast;

import java.net.InetAddress;
import java.net.UnknownHostException;

import ca.triangulationdevice.android.ipc.NetworkIPCService;
import ca.triangulationdevice.android.model.User;

public abstract class NetworkRecordingActivity extends RecordingActivity {

    private static final String TAG = "NetworkRecording";

    private NetworkIPCService<Location> transferService;
    private InetAddress address;

    protected void setAddress(String ip) {
        try {
            address = InetAddress.getByName(ip);
        } catch (UnknownHostException e) {
            Toast.makeText(this, "Other user's IP address corrupted!", Toast.LENGTH_LONG).show();
            Log.e(TAG, "Unable to get InetAddress from other user's IP", e);
            this.finish();
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Handler transferHandler = new Handler(new NetworkRecordingHandler());
        User currentUser = this.application.userManager.getCurrentUser();
        transferService = new NetworkIPCService<>(currentUser, transferHandler, Location.CREATOR);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        this.transferService.disconnect();
    }

    @Override
    protected void onResume() {
        super.onResume();
        this.transferService.connect(address);
    }

    @Override
    protected void onPause() {
        this.transferService.stop();
        super.onPause();
    }

    @Override
    public void onLocationChanged(Location location) {
        super.onLocationChanged(location);
        if (this.transferService.getState() == NetworkIPCService.STATE_CONNECTED) {
            Log.i(TAG, "Writing a new location");
            this.transferService.write(location);
        }
    }

    protected void onLocationReceived(Location location) {
        Log.i(TAG, "Remote location: " + location.getLongitude() + ", " + location.getLatitude());
        this.pd.theirLocationChanged(location);
    }

    // A Handler to receive messages from another BluetoothIPCActivity.
    private class NetworkRecordingHandler implements Handler.Callback {
        @Override
        public boolean handleMessage(Message msg) {
            switch (msg.what) {
                case NetworkIPCService.NEW_DEVICE:
                    receiveConnection((String) msg.obj);
                case NetworkIPCService.MESSAGE_STATE_CHANGE:
                    switch (msg.arg1) {
                        case NetworkIPCService.STATE_CONNECTED:
                            // We're finally connected, so change the UI to
                            // reflect that.
                            successfulConnect();
                            break;
                        case NetworkIPCService.STATE_CONNECTING:
                            // Connecting state gets set once we choose a device
                            // so we'll ignore this.
                            break;
                        case NetworkIPCService.STATE_LISTEN:
                            // We're disconnected and listening to things again.
                            disconnect();
                            break;
                        case NetworkIPCService.STATE_NONE:
                            // This is only when it's being set up and not
                            // listening yet, or stopped and in the process of
                            // shutting down.
                            break;
                    }
                    break;
                case NetworkIPCService.MESSAGE_READ:
                    Log.i(TAG, msg.toString());
                    @SuppressWarnings("unchecked") Location receivedObject = (Location) msg.obj;
                    onLocationReceived(receivedObject);
                    break;
                case NetworkIPCService.MESSAGE_INFO:
                    Log.i(TAG, msg.getData().getString(NetworkIPCService.INFO));
                    break;
            }
            return true;
        }
    }

    protected abstract void receiveConnection(String id);
    protected abstract void successfulConnect();
    protected abstract void disconnect();
}
