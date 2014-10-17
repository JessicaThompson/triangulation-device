package ca.triangulationdevice.android.bluetooth;

import java.util.Set;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.util.Log;

/**
 * This class encapsulates and simplifies bluetooth device discovery.
 */
public class BluetoothDeviceService {
    private static final String TAG = "BluetoothDeviceService";
    private final static int REQUEST_ENABLE_BLUETOOTH = 9000;

    // Return Intent extra
    public static String EXTRA_DEVICE_ADDRESS = "device_address";

    // Member fields
    private final Activity context;
    private final BluetoothAdapter bluetoothAdapter;
    private Listener listener;

    /**
     * This constructor takes in a context because it's fancy. Registers
     * listeners, etc.
     * 
     * @param context
     *            A context from which to launch broadcasts.
     */
    public BluetoothDeviceService(Activity context) {
        // Save the context.
        this.context = context;

        // Register for broadcasts when a device is discovered
        IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        context.registerReceiver(this.mReceiver, filter);

        // Register for broadcasts when discovery has finished
        filter = new IntentFilter(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
        context.registerReceiver(this.mReceiver, filter);

        // Get the local Bluetooth adapter
        this.bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
    }

    /**
     * Set a listener to be called when events happen.
     * 
     * @param listener
     *            A listener object.
     */
    public void setListener(Listener listener) {
        this.listener = listener;
    }

    /**
     * Close the object, unregistering services, etc.
     */
    public void destroy() {
        // Make sure we're not doing discovery anymore
        if (this.bluetoothAdapter != null) {
            this.bluetoothAdapter.cancelDiscovery();
        }

        // Unregister broadcast listeners
        this.context.unregisterReceiver(this.mReceiver);
    }

    /**
     * Start the device discovery.
     */
    public void start() {
        if (this.listener == null)
            throw new IllegalStateException("No Listener provided to BluetoothDeviceService!");

        if (this.bluetoothAdapter != null) {
            if (!this.bluetoothAdapter.isEnabled()) {
                Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                this.context.startActivityForResult(enableBtIntent, REQUEST_ENABLE_BLUETOOTH);
            } else {
                // Get a set of currently paired devices
                Set<BluetoothDevice> pairedDevices = this.bluetoothAdapter.getBondedDevices();
                this.listener.pairedDevices(pairedDevices);
            }
        }

        Log.d(TAG, "start()");

        // If we're already discovering, stop it
        if (this.bluetoothAdapter.isDiscovering()) {
            this.bluetoothAdapter.cancelDiscovery();
        }

        // Request discover from BluetoothAdapter
        this.bluetoothAdapter.startDiscovery();
    }

    /**
     * Stop discovery.
     */
    public void stop() {
        Log.d(TAG, "stop()");
        this.bluetoothAdapter.cancelDiscovery();
    }

    // The BroadcastReceiver that listens for discovered devices and
    // changes the title when discovery is finished
    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();

            // When discovery finds a device
            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                // Get the BluetoothDevice object from the Intent
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                // If it's already paired, skip it, because it's been listed
                // already
                if (device.getBondState() != BluetoothDevice.BOND_BONDED) {
                    int rssi = intent.getShortExtra(BluetoothDevice.EXTRA_RSSI, Short.MIN_VALUE);
                    BluetoothDeviceService.this.listener.unpairedDeviceFound(device, rssi);
                }

                // When discovery is finished, change the Activity title
            } else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)) {
                BluetoothDeviceService.this.listener.discoveryDone();
            }
        }
    };

    /**
     * A listener to provide access to bluetooth discovery events.
     */
    public static class Listener {
        /**
         * Called when the list of paired devices is returned. Note that they're
         * not necessarily connected, in range, or useful in any way.
         * 
         * @param pairedDevices
         *            A {@link Set} of bonded {@link BluetoothDevices}.
         */
        public void pairedDevices(Set<BluetoothDevice> pairedDevices) {
        }

        /**
         * Called when a new unpaired device has been found by the discovery
         * service.
         * 
         * @param device
         *            A newly discovered, unpaired bluetooth device.
         * @param rssi
         *            A vague number that sort of describes how close it is.
         */
        public void unpairedDeviceFound(BluetoothDevice device, int rssi) {
        }

        /**
         * Called when the discovery process is finished.
         */
        public void discoveryDone() {
        }
    }
}
