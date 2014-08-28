package de.vndvl.chrs.triangulationdevice.bluetooth;

import java.util.Set;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.util.Log;

public class BluetoothDeviceService {
    private static final String TAG = "BluetoothDeviceService";
    private final static int REQUEST_ENABLE_BLUETOOTH = 9000;
    
    // Return Intent extra
    public static String EXTRA_DEVICE_ADDRESS = "device_address";

    // Member fields
    private Activity context;
    private BluetoothAdapter bluetoothAdapter;
    private Listener listener;

    public BluetoothDeviceService(Activity context) {
        // Save the context.
        this.context = context;
        
        // Register for broadcasts when a device is discovered
        IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        context.registerReceiver(mReceiver, filter);

        // Register for broadcasts when discovery has finished
        filter = new IntentFilter(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
        context.registerReceiver(mReceiver, filter);

        // Get the local Bluetooth adapter
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
    }
    
    public void setListener(Listener listener) {
        this.listener = listener;
    }

    public void destroy() {
        // Make sure we're not doing discovery anymore
        if (bluetoothAdapter != null) {
            bluetoothAdapter.cancelDiscovery();
        }

        // Unregister broadcast listeners
        context.unregisterReceiver(mReceiver);
    }

    public void start() {
        if (this.listener == null) throw new IllegalStateException("No Listener provided to BluetoothDeviceService!");
        
        if (bluetoothAdapter != null) {
            if (!bluetoothAdapter.isEnabled()) {
                Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                context.startActivityForResult(enableBtIntent, REQUEST_ENABLE_BLUETOOTH);
            } else {
                // Get a set of currently paired devices
                Set<BluetoothDevice> pairedDevices = bluetoothAdapter.getBondedDevices();
                this.listener.pairedDevices(pairedDevices);                
            }
        }
        
        Log.d(TAG, "start()");

        // If we're already discovering, stop it
        if (bluetoothAdapter.isDiscovering()) {
            bluetoothAdapter.cancelDiscovery();
        }

        // Request discover from BluetoothAdapter
        bluetoothAdapter.startDiscovery();
    }
    
    public void stop() {
        Log.d(TAG, "stop()");
        bluetoothAdapter.cancelDiscovery();
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
                // If it's already paired, skip it, because it's been listed already
                if (device.getBondState() != BluetoothDevice.BOND_BONDED) {
                    BluetoothDeviceService.this.listener.unpairedDeviceFound(device);
                }
            // When discovery is finished, change the Activity title
            } else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)) {
                BluetoothDeviceService.this.listener.discoveryDone();
            }
        }
    };

    public static class Listener {
        public void pairedDevices(Set<BluetoothDevice> pairedDevices) {}
        public void unpairedDeviceFound(BluetoothDevice device) {}
        public void discoveryDone() {}
    }
}
