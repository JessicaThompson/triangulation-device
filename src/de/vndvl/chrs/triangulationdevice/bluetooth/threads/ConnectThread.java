package de.vndvl.chrs.triangulationdevice.bluetooth.threads;

import java.io.IOException;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.os.Handler;
import android.os.Parcelable;
import android.util.Log;
import de.vndvl.chrs.triangulationdevice.bluetooth.BluetoothIPCService;

/**
 * This thread runs while attempting to make an outgoing connection with a
 * device. It runs straight through; the connection either succeeds or fails.
 */
public class ConnectThread<T extends Parcelable> extends BluetoothThread<T> {
    private final static String TAG = "ConnectThread";

    private final BluetoothSocket mmSocket;
    private final BluetoothDevice mmDevice;

    public ConnectThread(BluetoothDevice device, BluetoothIPCService<T> service, Handler handler, Parcelable.Creator<T> creator, boolean secure) {
        super(service, handler, creator);
        this.mmDevice = device;
        BluetoothSocket tmp = null;

        // Get a BluetoothSocket for a connection with the given BluetoothDevice
        try {
            if (secure) {
                tmp = device.createRfcommSocketToServiceRecord(this.IPCservice.getUUID());
            } else {
                tmp = device.createInsecureRfcommSocketToServiceRecord(this.IPCservice.getUUID());
            }
        } catch (IOException e) {
            Log.e(TAG, "Secure create() failed", e);
        }
        this.mmSocket = tmp;
    }

    @Override
    public void run() {
        Log.i(TAG, "BEGIN mConnectThread");
        setName("ConnectThread");

        // Always cancel discovery because it will slow down a connection
        this.IPCservice.cancelDiscovery();

        // Make a connection to the BluetoothSocket
        try {
            // This is a blocking call and will only return on a
            // successful connection or an exception
            this.mmSocket.connect();
        } catch (IOException e) {
            // Close the socket
            try {
                this.mmSocket.close();
            } catch (IOException e2) {
                Log.e(TAG, "unable to close() socket during connection failure", e2);
            }
            Log.e(TAG, "Connection failed because of IO Exception.", e);
            this.IPCservice.connectionFailed();
            return;
        }

        // Reset the ConnectThread because we're done
        synchronized (this.IPCservice) {
            this.IPCservice.clearConnect();
        }

        // Start the connected thread
        this.IPCservice.connected(this.mmSocket, this.mmDevice);
    }

    public void cancel() {
        try {
            this.mmSocket.close();
        } catch (IOException e) {
            Log.e(TAG, "close() of connect socket failed", e);
        }
    }
}
