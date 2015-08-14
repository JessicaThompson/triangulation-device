package ca.triangulationdevice.android.ipc;

import android.os.Handler;
import android.os.Message;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.Log;

import org.zeromq.ZMQ;

import ca.triangulationdevice.android.ui.partial.NetworkRecordingActivity;

public class ParcelableZeroMQServer<T extends Parcelable> implements Runnable {

    private static final String TAG = "ParcelableZeroMQServer";

    // To help with parceling.
    private final Parcelable.Creator<T> creator;
    private final Handler handler;

    public ParcelableZeroMQServer(Handler handler, Parcelable.Creator<T> creator) {
        this.handler = handler;
        this.creator = creator;
    }

    @Override
    public void run() {
        ZMQ.Context context = ZMQ.context(1);
        ZMQ.Socket socket = context.socket(ZMQ.REP);
        socket.bind("tcp://*:5555");

        while(!Thread.currentThread().isInterrupted()) {
            // Receive the incoming message.
            byte[] msg = socket.recv(0);
            socket.send(new byte[0]);
            T location = unpack(msg, creator);
            handle(location);
            Log.i(TAG, "received: " + location.toString());
        }
        socket.close();
        context.term();
    }

    private void handle(T object) {
        Message message = handler.obtainMessage(NetworkRecordingActivity.PARCELABLE, object);
        message.sendToTarget();
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
    private T unpack(byte[] bytes, Parcelable.Creator<T> creator) {
        Parcel parcel = Parcel.obtain();
        parcel.unmarshall(bytes, 0, bytes.length);
        parcel.setDataPosition(0);
        return creator.createFromParcel(parcel);
    }
}
