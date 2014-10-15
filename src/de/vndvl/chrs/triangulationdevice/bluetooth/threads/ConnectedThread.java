package de.vndvl.chrs.triangulationdevice.bluetooth.threads;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import android.bluetooth.BluetoothSocket;
import android.os.Handler;
import android.os.Parcelable;
import android.util.Log;
import de.vndvl.chrs.triangulationdevice.bluetooth.BluetoothIPCService;

/**
 * This thread runs during a connection with a remote device. It handles all
 * incoming and outgoing transmissions.
 */
public class ConnectedThread<T extends Parcelable> extends BluetoothThread<T> {
    private final static String TAG = "ConnectedThread";

    private final BluetoothSocket socket;
    private final InputStream inStream;
    private final OutputStream outStream;

    public ConnectedThread(BluetoothIPCService<T> service, Handler handler, Parcelable.Creator<T> creator, BluetoothSocket socket) {
        super(service, handler, creator);

        Log.d(TAG, "create ConnectedThread");
        this.socket = socket;
        InputStream tmpIn = null;
        OutputStream tmpOut = null;

        // Get the BluetoothSocket input and output streams
        try {
            tmpIn = socket.getInputStream();
            tmpOut = socket.getOutputStream();
        } catch (IOException e) {
            Log.e(TAG, "temp sockets not created", e);
        }

        this.inStream = tmpIn;
        this.outStream = tmpOut;
    }

    @Override
    public void run() {
        Log.i(TAG, "BEGIN mConnectedThread");
        byte[] buffer = new byte[1024];

        // Keep listening to the InputStream while connected
        while (true) {
            try {
                // Read from the InputStream
                this.inStream.read(buffer);
                T thing = unpack(buffer, this.creator);

                // TODO: What if we get huge objects? Keep reading until we
                // have the end of the object, right?

                // Send the obtained bytes to the UI Activity
                this.sendMessage(BluetoothIPCService.MESSAGE_READ, thing);
            } catch (IOException e) {
                if (this.stateIs(BluetoothIPCService.STATE_CONNECTED)) {
                    this.IPCservice.connectionLost();
                }

                // Start the service over to restart listening mode
                break;
            }
        }
    }

    /**
     * Write to the connected OutStream.
     * 
     * @param buffer
     *            The bytes to write
     */
    public void send(T location) {
        try {
            byte[] parceledBytes = pack(location);
            this.outStream.write(parceledBytes);

            // Share the sent message back to the UI Activity
            this.handler.obtainMessage(BluetoothIPCService.MESSAGE_WRITE, location).sendToTarget();
        } catch (IOException e) {
            Log.e(TAG, "Exception during write", e);
        }
    }

    public void cancel() {
        try {
            this.socket.close();
        } catch (IOException e) {
            Log.e(TAG, "close() of connect socket failed", e);
        }
    }
}