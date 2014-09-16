package de.vndvl.chrs.triangulationdevice.bluetooth.threads;

import android.os.Handler;
import android.os.Parcel;
import android.os.Parcelable;
import de.vndvl.chrs.triangulationdevice.bluetooth.BluetoothIPCService;

public class BluetoothThread<T extends Parcelable> extends Thread {

    protected final Parcelable.Creator<T> creator;
    protected final BluetoothIPCService<T> IPCservice;
    protected final Handler handler;

    public BluetoothThread(BluetoothIPCService<T> service, Handler handler, Parcelable.Creator<T> creator) {
        this.IPCservice = service;
        this.creator = creator;
        this.handler = handler;
    }

    /**
     * Sends a message to our connected {@link Handler}
     * 
     * @param type
     *            The type of message to send.
     * @param what
     *            What to attach to the object.
     */
    protected void sendMessage(int type, Object what) {
        this.handler.obtainMessage(type, what).sendToTarget();
    }

    protected boolean stateIs(int state) {
        return this.IPCservice.getState() == state;
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
