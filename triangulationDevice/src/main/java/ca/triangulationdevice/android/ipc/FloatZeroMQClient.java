package ca.triangulationdevice.android.ipc;

import android.os.AsyncTask;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.Log;

import org.zeromq.ZMQ;

public class FloatZeroMQClient extends AsyncTask<Float, Void, Void> {

    private static final String TAG = "FloatZeroMQClient";

    // To help with parceling.
    private final String ip;

    public FloatZeroMQClient(String ip) {
        this.ip = ip;
    }

    @Override
    protected final Void doInBackground(Float... objects) {
        Log.i(TAG, "Doing some shit.");
        ZMQ.Context context = ZMQ.context(1);
        ZMQ.Socket socket = context.socket(ZMQ.REQ);
        socket.connect("tcp://" + ip + ":5556");

        socket.send(Float.toString(objects[0]));
        Log.i(TAG, "sent: " + objects[0]);

        socket.close();
        context.term();

        return null;
    }
}