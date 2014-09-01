package de.vndvl.chrs.triangulationdevice.bluetooth;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.UUID;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.Log;

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

    // Unique UUID for this application
    private final UUID MY_UUID_SECURE;

    // Member fields
    private final BluetoothAdapter bluetoothAdapter;
    private final Handler handler;
    private AcceptThread secureAcceptThread;
    private ConnectThread connectThread;
    private ConnectedThread connectedThread;
    private int state;

    // To help with parceling.
    private final Parcelable.Creator<T> creator;

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
        this.MY_UUID_SECURE = UUID.fromString(uuid);
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

    /**
     * Start the chat service. Specifically start AcceptThread to begin a
     * session in listening (server) mode. Called by the Activity onResume()
     */
    public synchronized void start() {
        Log.d(TAG, "start");

        // Cancel any thread attempting to make a connection
        if (this.connectThread != null) {
            this.connectThread.cancel();
            this.connectThread = null;
        }

        // Cancel any thread currently running a connection
        if (this.connectedThread != null) {
            this.connectedThread.cancel();
            this.connectedThread = null;
        }

        setState(STATE_LISTEN);

        // Start the thread to listen on a BluetoothServerSocket
        if (this.secureAcceptThread == null) {
            this.secureAcceptThread = new AcceptThread();
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
            if (this.connectThread != null) {
                this.connectThread.cancel();
                this.connectThread = null;
            }
        }

        // Cancel any thread currently running a connection
        if (this.connectedThread != null) {
            this.connectedThread.cancel();
            this.connectedThread = null;
        }

        // Start the thread to connect with the given device
        this.connectThread = new ConnectThread(device);
        this.connectThread.start();
        setState(STATE_CONNECTING);
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
        if (this.connectThread != null) {
            this.connectThread.cancel();
            this.connectThread = null;
        }

        // Cancel any thread currently running a connection
        if (this.connectedThread != null) {
            this.connectedThread.cancel();
            this.connectedThread = null;
        }

        // Cancel the accept thread because we only want to connect to one
        // device
        if (this.secureAcceptThread != null) {
            this.secureAcceptThread.cancel();
            this.secureAcceptThread = null;
        }

        // Start the thread to manage the connection and perform transmissions
        this.connectedThread = new ConnectedThread(socket);
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

        if (this.connectThread != null) {
            this.connectThread.cancel();
            this.connectThread = null;
        }

        if (this.connectedThread != null) {
            this.connectedThread.cancel();
            this.connectedThread = null;
        }

        if (this.secureAcceptThread != null) {
            this.secureAcceptThread.cancel();
            this.secureAcceptThread = null;
        }
        setState(STATE_NONE);
    }

    /**
     * Write to the ConnectedThread in an unsynchronized manner
     * 
     * @param out
     *            The bytes to write
     * @see ConnectedThread#write(byte[])
     */
    public void write(T out) {
        // Create temporary object
        ConnectedThread r;
        // Synchronize a copy of the ConnectedThread
        synchronized (this) {
            if (this.state != STATE_CONNECTED)
                return;
            r = this.connectedThread;
        }
        // Perform the write unsynchronized
        r.write(out);
    }

    /**
     * Indicate that the connection attempt failed and notify the UI Activity.
     */
    private void connectionFailed() {
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
    private void connectionLost() {
        // Send a failure message back to the Activity
        Message msg = this.handler.obtainMessage(BluetoothIPCService.MESSAGE_INFO);
        Bundle bundle = new Bundle();
        bundle.putString(BluetoothIPCService.INFO, "Device connection was lost");
        msg.setData(bundle);
        this.handler.sendMessage(msg);

        // Start the service over to restart listening mode
        BluetoothIPCService.this.start();
    }

    /**
     * This thread runs while listening for incoming connections. It behaves
     * like a server-side client. It runs until a connection is accepted (or
     * until cancelled).
     */
    private class AcceptThread extends Thread {
        // The local server socket
        private final BluetoothServerSocket mmServerSocket;

        public AcceptThread() {
            BluetoothServerSocket tmp = null;

            // Create a new listening server socket
            try {
                tmp = BluetoothIPCService.this.bluetoothAdapter.listenUsingRfcommWithServiceRecord(TAG, BluetoothIPCService.this.MY_UUID_SECURE);
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
            while (BluetoothIPCService.this.state != STATE_CONNECTED) {
                try {
                    // This is a blocking call and will only return on a
                    // successful connection or an exception
                    socket = this.mmServerSocket.accept();
                } catch (IOException e) {
                    break;
                }

                // If a connection was accepted
                if (socket != null) {
                    synchronized (BluetoothIPCService.this) {
                        switch (BluetoothIPCService.this.state) {
                        case STATE_LISTEN:
                        case STATE_CONNECTING:
                            // Situation normal. Start the connected thread.
                            connected(socket, socket.getRemoteDevice());
                            break;
                        case STATE_NONE:
                        case STATE_CONNECTED:
                            // Either not ready or already connected. Terminate
                            // new socket.
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

    /**
     * This thread runs while attempting to make an outgoing connection with a
     * device. It runs straight through; the connection either succeeds or
     * fails.
     */
    private class ConnectThread extends Thread {
        private final BluetoothSocket mmSocket;
        private final BluetoothDevice mmDevice;

        public ConnectThread(BluetoothDevice device) {
            this.mmDevice = device;
            BluetoothSocket tmp = null;

            // Get a BluetoothSocket for a connection with the
            // given BluetoothDevice
            try {
                tmp = device.createRfcommSocketToServiceRecord(BluetoothIPCService.this.MY_UUID_SECURE);
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
            BluetoothIPCService.this.bluetoothAdapter.cancelDiscovery();

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
                connectionFailed();
                return;
            }

            // Reset the ConnectThread because we're done
            synchronized (BluetoothIPCService.this) {
                BluetoothIPCService.this.connectThread = null;
            }

            // Start the connected thread
            connected(this.mmSocket, this.mmDevice);
        }

        public void cancel() {
            try {
                this.mmSocket.close();
            } catch (IOException e) {
                Log.e(TAG, "close() of connect socket failed", e);
            }
        }
    }

    /**
     * This thread runs during a connection with a remote device. It handles all
     * incoming and outgoing transmissions.
     */
    private class ConnectedThread extends Thread {
        private final BluetoothSocket socket;
        private final InputStream inStream;
        private final OutputStream outStream;

        public ConnectedThread(BluetoothSocket socket) {
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
                    T thing = unpack(buffer, BluetoothIPCService.this.creator);

                    // TODO: What if we get huge objects? Keep reading until we
                    // have the end of the object, right?

                    // Send the obtained bytes to the UI Activity
                    BluetoothIPCService.this.handler.obtainMessage(BluetoothIPCService.MESSAGE_READ, thing).sendToTarget();
                } catch (IOException e) {
                    if (BluetoothIPCService.this.state == STATE_CONNECTED) {
                        connectionLost();
                    }
                    // Start the service over to restart listening mode
                    BluetoothIPCService.this.start();
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
        public void write(T location) {
            try {
                byte[] parceledBytes = pack(location);
                this.outStream.write(parceledBytes);

                // Share the sent message back to the UI Activity
                BluetoothIPCService.this.handler.obtainMessage(BluetoothIPCService.MESSAGE_WRITE, location).sendToTarget();
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

    public byte[] pack(T parcelable) {
        Parcel parcel = Parcel.obtain();
        parcelable.writeToParcel(parcel, 0);
        byte[] bytes = parcel.marshall();
        parcel.recycle();
        return bytes;
    }

    public T unpack(byte[] bytes, Parcelable.Creator<T> creator) {
        Parcel parcel = Parcel.obtain();
        parcel.unmarshall(bytes, 0, bytes.length);
        parcel.setDataPosition(0);
        return creator.createFromParcel(parcel);
    }
}
