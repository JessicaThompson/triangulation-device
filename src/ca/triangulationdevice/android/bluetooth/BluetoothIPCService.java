package ca.triangulationdevice.android.bluetooth;

import java.util.UUID;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.Parcelable;
import android.util.Log;
import ca.triangulationdevice.android.bluetooth.threads.AcceptThread;
import ca.triangulationdevice.android.bluetooth.threads.ConnectThread;
import ca.triangulationdevice.android.bluetooth.threads.ConnectedThread;

/**
 * This class does all the work for setting up and managing Bluetooth
 * connections with other devices. It has a thread that listens for incoming
 * connections, a thread for connecting with a device, and a thread for
 * performing data transmissions when connected.
 * 
 * Based on the BluetoothChatService example in the Android SDK.
 */
public class BluetoothIPCService<T extends Parcelable> {
    // Debugging
    private static final String TAG = "BluetoothIPCService";

    // Key names received from the BluetoothChatService Handler
    public static final String DEVICE_NAME = "device_name";
    public static final String INFO = "toast";

    // Whether to use a secure channel or not.
    private final static boolean SECURE = true;

    // Unique UUID for this application
    private final UUID secureUUID;

    // Member fields
    public final BluetoothAdapter bluetoothAdapter;
    private final Handler handler;
    private final Parcelable.Creator<T> creator;
    private AcceptThread<T> secureAcceptThread;
    private ConnectThread<T> connectThread;
    private ConnectedThread<T> connectedThread;
    private int state;

    // Message types sent from the BluetoothIPCService Handler
    public static final int MESSAGE_STATE_CHANGE = 1;
    public static final int MESSAGE_READ = 2;
    public static final int MESSAGE_WRITE = 3;
    public static final int NEW_DEVICE = 4;
    public static final int MESSAGE_INFO = 5;

    // Constants that indicate the current connection state
    public static final int STATE_NONE = 0; // we're doing nothing
    public static final int STATE_LISTEN = 1; // now listening for incoming
                                              // connections
    public static final int STATE_CONNECTING = 2; // now initiating an outgoing
                                                  // connection
    public static final int STATE_CONNECTED = 3; // now connected to a remote
                                                 // device
    public static final int STATE_DISCONNECTING = 4; // now closing an outgoing
                                                     // connection

    /**
     * Constructor. Prepares a new BluetoothIPC session.
     * 
     * @param context
     *            The UI Activity Context
     * @param handler
     *            A Handler to send messages back to the UI Activity
     */
    public BluetoothIPCService(String uuid, Handler handler, Parcelable.Creator<T> creator) {
        this.secureUUID = UUID.fromString(uuid);
        this.bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        this.state = STATE_NONE;
        this.handler = handler;
        this.creator = creator;
    }

    /**
     * Set the current state of the chat connection
     * 
     * @param state
     *            An integer defining the current connection state
     */
    private synchronized void setState(int state) {
        Log.d(TAG, "setState() " + state + " -> " + state);
        this.state = state;

        // Give the new state to the Handler so the UI Activity can update
        this.handler.obtainMessage(BluetoothIPCService.MESSAGE_STATE_CHANGE, state, -1).sendToTarget();
    }

    /**
     * Return the current connection state.
     */
    public synchronized int getState() {
        return this.state;
    }

    public UUID getUUID() {
        return this.secureUUID;
    }

    /**
     * Start the chat service. Specifically start AcceptThread to begin a
     * session in listening (server) mode. Called by the Activity onResume()
     */
    public synchronized void start() {
        Log.d(TAG, "start");

        // Cancel any thread attempting to make a connection
        clearConnect();

        // Cancel any thread currently running a connection
        clearConnected();

        setState(STATE_LISTEN);

        // Start the thread to listen on a BluetoothServerSocket
        if (this.secureAcceptThread == null) {
            this.secureAcceptThread = new AcceptThread<T>(this, this.handler, this.creator, SECURE);
            this.secureAcceptThread.start();
        }
    }

    /**
     * Start the ConnectThread to initiate a connection to a remote device.
     * 
     * @param device
     *            The BluetoothDevice to connect
     */
    public synchronized void connect(BluetoothDevice device) {
        Log.d(TAG, "connect to: " + device);

        // Cancel any thread attempting to make a connection
        if (this.state == STATE_CONNECTING) {
            clearConnect();
        }

        // Cancel any thread currently running a connection
        clearConnected();

        // Start the thread to connect with the given device
        this.connectThread = new ConnectThread<T>(device, this, this.handler, this.creator, SECURE);
        this.connectThread.start();
        setState(STATE_CONNECTING);
    }

    public synchronized void clearAccept() {
        if (this.secureAcceptThread != null) {
            this.secureAcceptThread.cancel();
            this.secureAcceptThread = null;
        }
    }

    public synchronized void clearConnect() {
        if (this.connectThread != null) {
            this.connectThread.cancel();
            this.connectThread = null;
        }
    }

    public synchronized void clearConnected() {
        if (this.connectedThread != null) {
            this.connectedThread.cancel();
            this.connectedThread = null;
        }
    }

    /**
     * Start the ConnectedThread to begin managing a Bluetooth connection
     * 
     * @param socket
     *            The BluetoothSocket on which the connection was made
     * @param device
     *            The BluetoothDevice that has been connected
     */
    public synchronized void connected(BluetoothSocket socket, BluetoothDevice device) {
        Log.d(TAG, "connected");

        // Cancel the thread that completed the connection
        clearConnect();

        // Cancel any thread currently running a connection
        clearConnected();

        // Cancel the accept thread because we only want to connect to one
        // device
        clearAccept();

        // Start the thread to manage the connection and perform transmissions
        this.connectedThread = new ConnectedThread<T>(this, this.handler, this.creator, socket);
        this.connectedThread.start();

        // Send the name of the connected device back to the UI Activity
        Message msg = this.handler.obtainMessage(BluetoothIPCService.NEW_DEVICE);
        msg.obj = device;
        this.handler.sendMessage(msg);

        setState(STATE_CONNECTED);
    }

    /**
     * Disconnect from another device, reset to listening.
     */
    public synchronized void disconnect() {
        Log.d(TAG, "disconnect()");
        setState(STATE_DISCONNECTING);
        this.start();
        setState(STATE_LISTEN);
    }

    /**
     * Stop all threads
     */
    public synchronized void stop() {
        Log.d(TAG, "stop()");
        clearConnect();
        clearConnected();
        clearAccept();
        setState(STATE_NONE);
    }

    public synchronized void cancelDiscovery() {
        this.bluetoothAdapter.cancelDiscovery();
    }

    /**
     * Write to the ConnectedThread in an unsynchronized manner
     * 
     * @param out
     *            The bytes to write
     * @see ConnectedThread#send(byte[])
     */
    public void send(T out) {
        // Create temporary object
        ConnectedThread<T> r;
        // Synchronize a copy of the ConnectedThread
        synchronized (this) {
            if (this.state != STATE_CONNECTED)
                return;
            r = this.connectedThread;
        }
        // Perform the write unsynchronized
        r.send(out);
    }

    /**
     * Indicate that the connection attempt failed and notify the UI Activity.
     */
    public void connectionFailed() {
        // Send a failure message back to the Activity
        Message msg = this.handler.obtainMessage(BluetoothIPCService.MESSAGE_INFO);
        Bundle bundle = new Bundle();
        bundle.putString(BluetoothIPCService.INFO, "Unable to connect device");
        msg.setData(bundle);
        this.handler.sendMessage(msg);

        // Start the service over to restart listening mode
        BluetoothIPCService.this.start();
    }

    /**
     * Indicate that the connection was lost and notify the UI Activity.
     */
    public void connectionLost() {
        // Send a failure message back to the Activity
        Message msg = this.handler.obtainMessage(BluetoothIPCService.MESSAGE_INFO);
        Bundle bundle = new Bundle();
        bundle.putString(BluetoothIPCService.INFO, "Device connection was lost");
        msg.setData(bundle);
        this.handler.sendMessage(msg);

        // Start the service over to restart listening mode
        BluetoothIPCService.this.start();
    }
}
