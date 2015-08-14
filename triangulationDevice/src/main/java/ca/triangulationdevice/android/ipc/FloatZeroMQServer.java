package ca.triangulationdevice.android.ipc;

import android.os.Handler;
import android.os.Message;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.Log;

import org.zeromq.ZMQ;

import ca.triangulationdevice.android.ui.partial.NetworkRecordingActivity;

public class FloatZeroMQServer implements Runnable {

    private static final String TAG = "StringZeroMQServer";

    // To help with parceling.
    private final Handler handler;

    public FloatZeroMQServer(Handler handler) {
        this.handler = handler;
    }

    @Override
    public void run() {
        ZMQ.Context context = ZMQ.context(1);
        ZMQ.Socket socket = context.socket(ZMQ.REP);
        socket.bind("tcp://*:5556");

        while(!Thread.currentThread().isInterrupted()) {
            // Receive the incoming message.
            byte[] msg = socket.recv(0);
            socket.send(new byte[0]);
            float value = Float.parseFloat(new String(msg));

            handle(value);
            Log.i(TAG, "received: " + value);
        }
        socket.close();
        context.term();
    }

    private void handle(float value) {
        Message message = handler.obtainMessage(NetworkRecordingActivity.FLOAT, value);
        message.sendToTarget();
    }
}
