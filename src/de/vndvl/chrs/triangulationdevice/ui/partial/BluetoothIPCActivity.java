package de.vndvl.chrs.triangulationdevice.ui.partial;

import java.util.ArrayList;
import java.util.Set;

import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothClass;
import android.bluetooth.BluetoothDevice;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Resources;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.Parcelable;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.widget.Adapter;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import de.vndvl.chrs.triangulationdevice.R;
import de.vndvl.chrs.triangulationdevice.bluetooth.BluetoothDeviceService;
import de.vndvl.chrs.triangulationdevice.bluetooth.BluetoothIPCService;

/**
 * BluetoothIPCActivity is a convenience class that has everything setup for you
 * to find devices and send messages between them over Bluetooth. Subclassing
 * this and implementing {@link #onMessageReceived}, {@link #getDefault}, and
 * {@link #getCreator()} will allow your Activity to communicate with other
 * Activities with the same UUID and sending the same type of message.
 * 
 * Messages must implement {@link Parcelable} and have a
 * {@link Parcelable.Creator} given which will be used to instantiate the
 * objects as they come in off the wire. Wireless. Whatever.
 * 
 * @param <T>
 *            The type of object you'll be sending over the IPC. Probably a
 *            message or event of some kind.
 */
public abstract class BluetoothIPCActivity<T extends Parcelable> extends LocationActivity {
    private final static String TAG = "BluetoothActivity";

    private final static int DISCOVERABILITY_DURATION = 5 * 1000;
    private final static String TRIANGULATION_DEVICE_UUID = "04364090-2bca-11e4-8c21-0800200c9a66";

    private Resources resources;

    private BluetoothIPCService<T> bluetoothIPC;
    private BluetoothDeviceService deviceService;
    private BluetoothDevice connectedDevice;

    private ArrayList<BluetoothDevice> bluetoothDevices;
    private ArrayAdapter<String> bluetoothNames;

    private final Handler bluetoothHandler = new Handler(new MapActivityHandler());

    /**
     * @return The connected {@link BluetoothDevice}, or null if not connected
     *         to anything (duh).
     */
    public BluetoothDevice getConnectedDevice() {
        return this.connectedDevice;
    }

    /**
     * @return An {@link Adapter} suitable for adding to a {@link ListView}.
     */
    public ArrayAdapter<String> getBluetoothAdapter() {
        return this.bluetoothNames;
    }

    /**
     * Called when a new message is received from the other
     * {@link BluetoothIPCActivity}. Implement to respond to a new message being
     * received.
     * 
     * @param message
     *            The message received from the other activity.
     */
    protected abstract void onMessageReceived(T message);

    /**
     * Called to obtain a value which is sent to the other
     * {@link BluetoothIPCActivity} when the connection is first made.
     * 
     * @return A "default" value to send on the first connection.
     */
    protected abstract T getDefault();

    /**
     * Called to obtain our type's {@link Parcelable.Creator}, used to
     * instantiate new messages coming in form the other
     * {@link BluetoothIPCActivity}. Unfortunately, due to the design of
     * {@link Parcelable} classes, we can't infer this from the type itself.
     * 
     * @return
     */
    protected abstract Parcelable.Creator<T> getCreator();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.resources = getResources();

        // Request a progress bar.
        this.requestWindowFeature(Window.FEATURE_PROGRESS);

        // Set up the Bluetooth device discovery, react to its listener methods.
        this.deviceService = new BluetoothDeviceService(this);
        this.deviceService.setListener(new BluetoothDeviceService.Listener() {
            @Override
            public void unpairedDeviceFound(BluetoothDevice device, int rssi) {
                int bluetoothClass = device.getBluetoothClass().getDeviceClass();
                if (bluetoothClass == BluetoothClass.Device.PHONE_SMART || bluetoothClass == BluetoothClass.Device.COMPUTER_LAPTOP) {
                    BluetoothIPCActivity.this.bluetoothDevices.add(device);
                    BluetoothIPCActivity.this.bluetoothNames.add(device.getName());
                }
            }

            @Override
            public void pairedDevices(Set<BluetoothDevice> pairedDevices) {
                for (BluetoothDevice device : pairedDevices) {
                    int bluetoothClass = device.getBluetoothClass().getDeviceClass();
                    if (bluetoothClass == BluetoothClass.Device.PHONE_SMART || bluetoothClass == BluetoothClass.Device.COMPUTER_LAPTOP) {
                        BluetoothIPCActivity.this.bluetoothDevices.add(device);
                        BluetoothIPCActivity.this.bluetoothNames.add(device.getName());
                    }
                }
            }
        });

        // Set up the IPC service.
        this.bluetoothIPC = new BluetoothIPCService<T>(TRIANGULATION_DEVICE_UUID, this.bluetoothHandler, getCreator());

        // Set us up for discoverability
        Intent discoverableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
        discoverableIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, DISCOVERABILITY_DURATION);
        startActivity(discoverableIntent);
    }

    @Override
    protected void onDestroy() {
        this.deviceService.destroy();
        super.onDestroy();
    }

    @Override
    protected void onResume() {
        super.onResume();
        startBluetooth();
    }

    @Override
    protected void onPause() {
        stopBluetooth();
        super.onPause();
    }

    /**
     * Call to start the Bluetooth connection (including listening for new
     * connections) manually, for whatever reason. Called by default when
     * {@link #onResume} is called by the Android service, so feel free to
     * ignore this.
     */
    protected void startBluetooth() {
        if (this.connectedDevice != null) {
            this.bluetoothIPC.start();
        }
    }

    /**
     * Call to stop the Bluetooth connection (including listening for new
     * connections) manually, for whatever reason. Called by default when
     * {@link #onPause} is called by the Android service, feel free to ignore.
     */
    protected void stopBluetooth() {
        if (this.connectedDevice != null) {
            this.bluetoothIPC.stop();
        }
    }

    /**
     * Begin finding nearby devices!
     * 
     * @param tappedView
     *            A View to pass to the function in case you want to hook this
     *            directly to an onClick in an XML layout. Null-safe.
     */
    public void findNearby(View tappedView) {
        this.bluetoothDevices = new ArrayList<BluetoothDevice>();
        this.bluetoothNames = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1);
        this.deviceService.start();
        this.bluetoothIPC.start();

        // Create the dialog that shows new devices.
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(this.resources.getString(R.string.nearby_devices));
        builder.setAdapter(this.bluetoothNames, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int item) {
                BluetoothIPCActivity.this.deviceService.stop();
                connectTo(BluetoothIPCActivity.this.bluetoothDevices.get(item));
            }
        });
        builder.setNegativeButton(this.resources.getString(R.string.cancel), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int item) {
                BluetoothIPCActivity.this.deviceService.stop();
                dialog.cancel();
            }
        });

        AlertDialog alert = builder.create();
        alert.show();
    }

    // Sets our connected bluetooth device, doesn't do much but
    // successfulConnect will be connected when actual data is transferred
    // between the two devices.
    private void receiveConnection(BluetoothDevice device) {
        // Set device as our chosen device.
        Log.i(TAG, "Setting the device, receiveConnection() " + device);
        this.connectedDevice = device;
    }

    // Connects to the bluetooth device and starts transferring data.
    private void connectTo(BluetoothDevice device) {
        setProgressBarIndeterminateVisibility(true);
        this.connectedDevice = device;
        this.bluetoothIPC.connect(device);
    }

    /**
     * Called when a successful connection has been made, and sends the default
     * value to the value specified by {@link #getDefault()}.
     */
    protected void successfulConnect() {
        setProgressBarIndeterminateVisibility(false);
        this.bluetoothIPC.write(getDefault());
    }

    /**
     * Disconnects from our connected device.
     * 
     * @param buttonView
     *            A View to pass to the function in case you want to hook this
     *            directly to an onClick in an XML layout. Null-safe.
     */
    protected void disconnectDevice(View buttonView) {
        // Clear chosen device.
        setProgressBarIndeterminateVisibility(false);
        this.connectedDevice = null;
        this.bluetoothIPC.disconnect();
    }

    // A Handler to receive messages from another BluetoothIPCActivity.
    private class MapActivityHandler implements Handler.Callback {
        @Override
        public boolean handleMessage(Message msg) {
            switch (msg.what) {
            case BluetoothIPCService.NEW_DEVICE:
                receiveConnection((BluetoothDevice) msg.obj);
            case BluetoothIPCService.MESSAGE_STATE_CHANGE:
                switch (msg.arg1) {
                case BluetoothIPCService.STATE_CONNECTED:
                    // We're finally connected, so change the UI to
                    // reflect that.
                    successfulConnect();
                    break;
                case BluetoothIPCService.STATE_CONNECTING:
                    // Connecting state gets set once we choose a device
                    // so we'll ignore this.
                    break;
                case BluetoothIPCService.STATE_LISTEN:
                    // We're disconnected and listening to things.
                    if (BluetoothIPCActivity.this.connectedDevice != null) {
                        disconnectDevice(null);
                    }
                    break;
                case BluetoothIPCService.STATE_NONE:
                    // This is only when it's being set up and not
                    // listening yet, or stopped and in the process of
                    // shutting down.
                    break;
                }
                break;
            case BluetoothIPCService.MESSAGE_READ:
                @SuppressWarnings("unchecked") T receivedObject = (T) msg.obj;
                onMessageReceived(receivedObject);
                break;
            case BluetoothIPCService.MESSAGE_INFO:
                Log.i(TAG, msg.getData().getString(BluetoothIPCService.INFO));
                break;
            }
            return true;
        }
    };
}
