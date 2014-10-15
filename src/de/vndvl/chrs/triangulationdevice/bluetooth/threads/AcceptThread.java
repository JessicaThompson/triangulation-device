package de.vndvl.chrs.triangulationdevice.bluetooth.threads;

import java.io.IOException;

import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.os.Handler;
import android.os.Parcelable;
import android.util.Log;
import de.vndvl.chrs.triangulationdevice.bluetooth.BluetoothIPCService;

/**
 * This thread runs while listening for incoming connections. It behaves like a
 * server-side client. It runs until a connection is accepted (or until
 * cancelled).
 */
public class AcceptThread<T extends Parcelable> extends BluetoothThread<T> {
    private static final String TAG = "AcceptThread";

    // The local server socket
    private final BluetoothServerSocket mmServerSocket;

    public AcceptThread(BluetoothIPCService<T> service, Handler handler, Parcelable.Creator<T> creator) {
        super(service, handler, creator);
        BluetoothServerSocket tmp = null;

        // Create a new listening server socket
        try {
            tmp = this.IPCservice.bluetoothAdapter.listenUsingInsecureRfcommWithServiceRecord("BluetoothIPCService", this.IPCservice.getUUID());
        } catch (IOException e) {
            Log.e(TAG, "Secure listen() failed", e);
        }
        this.mmServerSocket = tmp;
    }

    @Override
    public void run() {
        Log.d(TAG, "BEGIN mAcceptThread" + this);
        setName("AcceptThread");

        BluetoothSocket socket = null;

        // Listen to the server socket if we're not connected
        while (!this.stateIs(BluetoothIPCService.STATE_CONNECTED)) {
            try {
                // This is a blocking call and will only return on a
                // successful connection or an exception
                socket = this.mmServerSocket.accept();
            } catch (IOException e) {
                e.printStackTrace();
                break;
            }

            // If a connection was accepted
            if (socket != null) {
                synchronized (this.IPCservice) {
                    switch (this.IPCservice.getState()) {
                    case BluetoothIPCService.STATE_LISTEN:
                    case BluetoothIPCService.STATE_CONNECTING:
                        // Situation normal. Start the connected thread.
                        this.IPCservice.connected(socket, socket.getRemoteDevice());
                        break;
                    case BluetoothIPCService.STATE_NONE:
                    case BluetoothIPCService.STATE_CONNECTED:
                        // Either not ready or already connected.
                        // Terminate new socket.
                        try {
                            socket.close();
                        } catch (IOException e) {
                            Log.e(TAG, "Could not close unwanted socket", e);
                        }
                        break;
                    }
                }
            }
        }
        Log.i(TAG, "END mAcceptThread");
    }

    public void cancel() {
        Log.d(TAG, "Secure cancel " + this);
        try {
            this.mmServerSocket.close();
        } catch (IOException e) {
            Log.e(TAG, "Secure close() of server failed", e);
        }
    }
}
