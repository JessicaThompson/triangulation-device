package ca.triangulationdevice.android.ipc;

import android.os.Handler;
import android.os.Message;
import android.util.Log;

import org.zeromq.ZMQ;

import ca.triangulationdevice.android.ui.partial.NetworkRecordingActivity;

public class StringZeroMQServer implements Runnable {

    private static final String TAG = "StringZeroMQServer";

    // To help with parceling.
    private final Handler handler;

    public StringZeroMQServer(Handler handler) {
        this.handler = handler;
    }

    @Override
    public void run() {
        ZMQ.Context context = ZMQ.context(1);
        ZMQ.Socket socket = context.socket(ZMQ.REP);
        socket.bind("tcp://*:5557");

        while(!Thread.currentThread().isInterrupted()) {
            // Receive the incoming message.
            byte[] msg = socket.recv(0);
            socket.send(new byte[0]);
            String id = new String(msg);

            handle(id);
            Log.i(TAG, "received: " + id);
        }
        socket.close();
        context.term();
    }

    private void handle(String value) {
        Message message = handler.obtainMessage(0, value);
        message.sendToTarget();
    }
}
