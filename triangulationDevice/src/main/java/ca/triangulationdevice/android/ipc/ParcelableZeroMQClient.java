package ca.triangulationdevice.android.ipc;

import android.os.AsyncTask;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.Log;

import org.zeromq.ZMQ;

public class ParcelableZeroMQClient<T extends Parcelable> extends AsyncTask<T, Void, Void> {

    private static final String TAG = "ParcelableZeroMQClient";

    // To help with parceling.
    private final String ip;

    public ParcelableZeroMQClient(String ip) {
        Log.d(TAG, "Creating a new ZMQ client to " + ip);
        this.ip = ip;
    }

    @SafeVarargs
    @Override
    protected final Void doInBackground(T... objects) {
        ZMQ.Context context = ZMQ.context(1);
        ZMQ.Socket socket = context.socket(ZMQ.REQ);
        Log.d(TAG, "Connecting to tcp://" + ip + ":5555");
        socket.connect("tcp://" + ip + ":5555");

        socket.send(pack(objects[0]));
        Log.d(TAG, "sent: " + objects[0].toString());

        socket.close();
        context.term();

        return null;
    }

    /**
     * Packs a parcelable object into a byte array.
     *
     * @param parcelable
     *            A {@link Parcelable} object
     * @return A byte array representing the object.
     */
    private byte[] pack(T parcelable) {
        Parcel parcel = Parcel.obtain();
        parcelable.writeToParcel(parcel, 0);
        byte[] bytes = parcel.marshall();
        parcel.recycle();
        return bytes;
    }
}