package ca.triangulationdevice.android.ipc;

import android.os.AsyncTask;
import android.util.Log;

import org.zeromq.ZMQ;

public class StringZeroMQClient extends AsyncTask<String, Void, Void> {

    private static final String TAG = "StringZeroMQClient";

    // To help with parceling.
    private final String ip;

    public StringZeroMQClient(String ip) {
        this.ip = ip;
    }

    @Override
    protected final Void doInBackground(String... objects) {
        Log.i(TAG, "Doing some shit 2.");
        ZMQ.Context context = ZMQ.context(1);
        ZMQ.Socket socket = context.socket(ZMQ.REQ);
        socket.connect("tcp://" + ip + ":5557");

        socket.send(objects[0].getBytes());
        Log.i(TAG, "sent: " + objects[0]);

        socket.close();
        context.term();

        return null;
    }
}