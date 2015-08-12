package ca.triangulationdevice.android.ipc;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;

import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.Log;

/**
 * This class does all the work for setting up and managing Network
 * connections with other devices. It has a thread that listens for incoming
 * connections, a thread for connecting with a device, and a thread for
 * performing data transmissions when connected.
 *
 * Based on the BluetoothChatService example in the Android SDK.
 */
public class NetworkIPCService<T extends Parcelable> {
    // Debugging
    private static final String TAG = "NetworkIPCService";

    // Key names received from the BluetoothChatService Handler
    public static final String DEVICE_NAME = "device_name";
    public static final String INFO = "toast";

    private static final int PORT = 5000;

    // Member fields
    private final Handler handler;
    private AcceptThread secureAcceptThread;
    private ConnectThread connectThread;
    private TransferThread transferThread;
    private int state;

    // To help with parceling.
    private final Parcelable.Creator<T> creator;

    // Message types sent from the NetworkIPCService Handler
    public static final int MESSAGE_STATE_CHANGE = 1;
    public static final int MESSAGE_READ = 2;
    public static final int MESSAGE_WRITE = 3;
    public static final int NEW_DEVICE = 4;
    public static final int MESSAGE_INFO = 5;

    // Constants that indicate the current connection state
    public static final int STATE_NONE = 0; // we're doing nothing
    public static final int STATE_LISTEN = 1; // now listening for incoming connections
    public static final int STATE_CONNECTING = 2; // now initiating an outgoing connection
    public static final int STATE_CONNECTED = 3; // now connected to a remote device
    public static final int STATE_DISCONNECTING = 4; // now closing an outgoing connection

    /**
     * Constructor. Prepares a new BluetoothIPC session.
     *
     * @param handler
     *            A Handler to send messages back to the UI Activity
     * @param creator
     *            A Creator to make parcelable objects for your object to transfer.
     */
    public NetworkIPCService(Handler handler, Parcelable.Creator<T> creator) {
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
        this.handler.obtainMessage(NetworkIPCService.MESSAGE_STATE_CHANGE, state, -1).sendToTarget();
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
        if (this.transferThread != null) {
            this.transferThread.cancel();
            this.transferThread = null;
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
     * @param address
     *            The InetAddress to connect to.
     */
    public synchronized void connect(InetAddress address) {
        Log.d(TAG, "connect to: " + address);

        // Cancel any thread attempting to make a connection
        if (this.state == STATE_CONNECTING) {
            if (this.connectThread != null) {
                this.connectThread.cancel();
                this.connectThread = null;
            }
        }

        // Cancel any thread currently running a connection
        if (this.transferThread != null) {
            this.transferThread.cancel();
            this.transferThread = null;
        }

        // Start the thread to connect with the given device
        this.connectThread = new ConnectThread(address);
        this.connectThread.start();
        setState(STATE_CONNECTING);
    }

    /**
     * Start the TransferThread to begin managing a network connection
     *
     * @param socket
     *            The Socket on which the connection was made
     */
    public synchronized void connected(Socket socket) {
        Log.d(TAG, "connected");

        // Cancel the thread that completed the connection
        if (this.connectThread != null) {
            this.connectThread.cancel();
            this.connectThread = null;
        }

        // Cancel any thread currently running a connection
        if (this.transferThread != null) {
            this.transferThread.cancel();
            this.transferThread = null;
        }

        // Cancel the accept thread because we only want to connect to one
        // device
        if (this.secureAcceptThread != null) {
            this.secureAcceptThread.cancel();
            this.secureAcceptThread = null;
        }

        // Start the thread to manage the connection and perform transmissions
        this.transferThread = new TransferThread(socket);
        this.transferThread.start();

        // Send the name of the connected device back to the UI Activity
        Message msg = this.handler.obtainMessage(NetworkIPCService.NEW_DEVICE);
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

        if (this.transferThread != null) {
            this.transferThread.cancel();
            this.transferThread = null;
        }

        if (this.secureAcceptThread != null) {
            this.secureAcceptThread.cancel();
            this.secureAcceptThread = null;
        }
        setState(STATE_NONE);
    }

    /**
     * Write to the TransferThread in an unsynchronized manner
     *
     * @param out
     *            The object to write
     * @see TransferThread#write(T)
     */
    public void write(T out) {
        // Create temporary object
        TransferThread r;
        // Synchronize a copy of the TransferThread
        synchronized (this) {
            if (this.state != STATE_CONNECTED)
                return;
            r = this.transferThread;
        }
        // Perform the write unsynchronized
        r.write(out);
    }

    /**
     * Indicate that the connection attempt failed and notify the UI Activity.
     */
    private void connectionFailed() {
        // Send a failure message back to the Activity
        Message msg = this.handler.obtainMessage(NetworkIPCService.MESSAGE_INFO);
        Bundle bundle = new Bundle();
        bundle.putString(NetworkIPCService.INFO, "Unable to connect device");
        msg.setData(bundle);
        this.handler.sendMessage(msg);

        // Start the service over to restart listening mode
        NetworkIPCService.this.start();
    }

    /**
     * Indicate that the connection was lost and notify the UI Activity.
     */
    private void connectionLost() {
        // Send a failure message back to the Activity
        Message msg = this.handler.obtainMessage(NetworkIPCService.MESSAGE_INFO);
        Bundle bundle = new Bundle();
        bundle.putString(NetworkIPCService.INFO, "Device connection was lost");
        msg.setData(bundle);
        this.handler.sendMessage(msg);

        // Start the service over to restart listening mode
        NetworkIPCService.this.start();
    }

    /**
     * This thread runs while listening for incoming connections. It behaves
     * like a server-side client. It runs until a connection is accepted (or
     * until cancelled).
     */
    private class AcceptThread extends Thread {
        // The local server socket
        private final ServerSocket serverSocket;

        public AcceptThread() {
            ServerSocket tmp = null;

            // Create a new listening server socket
            try {
                tmp = new ServerSocket();
            } catch (IOException e) {
                Log.e(TAG, "Secure listen() failed", e);
            }
            this.serverSocket = tmp;
        }

        @Override
        public void run() {
            Log.d(TAG, "BEGIN AcceptThread" + this);
            setName("AcceptThread");

            Socket socket = null;

            // Listen to the server socket if we're not connected
            while (NetworkIPCService.this.state != STATE_CONNECTED) {
                try {
                    // This is a blocking call and will only return on a
                    // successful connection or an exception
                    socket = this.serverSocket.accept();
                } catch (IOException e) {
                    break;
                }

                // If a connection was accepted
                if (socket != null) {
                    synchronized (NetworkIPCService.this) {
                        switch (NetworkIPCService.this.state) {
                            case STATE_LISTEN:
                            case STATE_CONNECTING:
                                // Situation normal. Start the connected thread.
                                connected(socket);
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
                this.serverSocket.close();
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
        private Socket socket;
        private final InetAddress address;

        public ConnectThread(InetAddress address) {
            this.address = address;
        }

        @Override
        public void run() {
            Log.i(TAG, "BEGIN ConnectThread");
            setName("ConnectThread");

            // Make a connection to the BluetoothSocket
            try {
                this.socket = new Socket(address, PORT);
            } catch (IOException e) {
                // Close the socket
                try {
                    if (this.socket != null) {
                        this.socket.close();
                    }
                } catch (IOException e2) {
                    Log.e(TAG, "unable to close() socket during connection failure", e2);
                }
                Log.e(TAG, "Connection failed because of IO Exception.", e);
                connectionFailed();
                return;
            }

            // Reset the ConnectThread because we're done
            synchronized (NetworkIPCService.this) {
                NetworkIPCService.this.connectThread = null;
            }

            // Start the connected thread
            connected(this.socket);
        }

        public void cancel() {
            try {
                this.socket.close();
            } catch (IOException e) {
                Log.e(TAG, "close() of connect socket failed", e);
            }
        }
    }

    /**
     * This thread runs during a connection with a remote device. It handles all
     * incoming and outgoing transmissions.
     */
    private class TransferThread extends Thread {
        private final Socket socket;
        private final InputStream inStream;
        private final OutputStream outStream;

        public TransferThread(Socket socket) {
            Log.d(TAG, "create TransferThread");
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
            Log.i(TAG, "BEGIN mTrasferThread");
            byte[] buffer = new byte[1024];

            // Keep listening to the InputStream while connected
            while (true) {
                try {
                    // Read from the InputStream
                    this.inStream.read(buffer);
                    T thing = unpack(buffer, NetworkIPCService.this.creator);

                    // TODO: What if we get huge objects? Keep reading until we
                    // have the end of the object, right?

                    // Send the obtained bytes to the UI Activity
                    NetworkIPCService.this.handler.obtainMessage(NetworkIPCService.MESSAGE_READ, thing).sendToTarget();
                } catch (IOException e) {
                    if (NetworkIPCService.this.state == STATE_CONNECTED) {
                        connectionLost();
                    }
                    // Start the service over to restart listening mode
                    NetworkIPCService.this.start();
                    break;
                }
            }
        }

        /**
         * Write to the connected OutStream.
         *
         * @param parcelable
         *            The object to write
         */
        public void write(T parcelable) {
            try {
                byte[] parceledBytes = pack(parcelable);
                this.outStream.write(parceledBytes);

                // Share the sent message back to the UI Activity
                NetworkIPCService.this.handler.obtainMessage(NetworkIPCService.MESSAGE_WRITE, parcelable).sendToTarget();
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

    /**
     * Packs a parcelable object into a byte array.
     *
     * @param parcelable
     *            A {@link Parcelable} object
     * @return A byte array representing the object.
     */
    public byte[] pack(T parcelable) {
        Parcel parcel = Parcel.obtain();
        parcelable.writeToParcel(parcel, 0);
        byte[] bytes = parcel.marshall();
        parcel.recycle();
        return bytes;
    }

    /**
     * Unpacks a parcelable object from its byte array.
     *
     * @param bytes
     *            The array of bytes which contains the object.
     * @param creator
     *            A Parcelable helper object to unpack.
     * @return The object passed over the wire.
     */
    public T unpack(byte[] bytes, Parcelable.Creator<T> creator) {
        Parcel parcel = Parcel.obtain();
        parcel.unmarshall(bytes, 0, bytes.length);
        parcel.setDataPosition(0);
        return creator.createFromParcel(parcel);
    }
}