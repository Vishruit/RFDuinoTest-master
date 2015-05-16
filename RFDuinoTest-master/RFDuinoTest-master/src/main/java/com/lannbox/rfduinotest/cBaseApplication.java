package com.lannbox.rfduinotest;


import android.app.Application;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;

public class cBaseApplication extends Application implements BluetoothAdapter.LeScanCallback {

    // State machine
    final public static int STATE_BLUETOOTH_OFF = 1;
    final public static int STATE_DISCONNECTED = 2;
    final public static int STATE_CONNECTING = 3;
    final public static int STATE_CONNECTED = 4;

    public int state;
    int start = 0;

    public BluetoothAdapter bluetoothAdapter;
    public BluetoothDevice bluetoothDevice;
    public RFduinoService rfduinoService;

    int currentPlayer = 0;
    public static String[] players = {"EA:02:7F:9E:5F:7C", "EC:C7:D5:05:67:BF"};
    public boolean scanStarted;
    public static String touchData="110";




    public final BroadcastReceiver bluetoothStateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, 0);
            if (state == BluetoothAdapter.STATE_ON) {
                upgradeState(STATE_DISCONNECTED);
            } else if (state == BluetoothAdapter.STATE_OFF) {
                downgradeState(STATE_BLUETOOTH_OFF);
            }
        }
    };

    public final BroadcastReceiver scanModeReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            boolean scanning = (bluetoothAdapter.getScanMode() != BluetoothAdapter.SCAN_MODE_NONE);
            scanStarted &= scanning;
            updateUi();
        }
    };

    public final ServiceConnection rfduinoServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            rfduinoService = ((RFduinoService.LocalBinder) service).getService();
            if (rfduinoService.initialize())
                if (rfduinoService.connect(players[currentPlayer])) {
                    upgradeState(STATE_CONNECTING);
                }
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            rfduinoService = null;
            downgradeState(STATE_DISCONNECTED);
        }
    };

    public final BroadcastReceiver rfduinoReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            if (RFduinoService.ACTION_CONNECTED.equals(action)) {
                upgradeState(STATE_CONNECTED);
            } else if (RFduinoService.ACTION_DISCONNECTED.equals(action)) {
                downgradeState(STATE_DISCONNECTED);
            } else if (RFduinoService.ACTION_DATA_AVAILABLE.equals(action)) {
                addData(intent.getByteArrayExtra(RFduinoService.EXTRA_DATA));
            }
        }
    };

    @Override
    public void onCreate() {
        super.onCreate();
        rfduinoService = new RFduinoService();
        currentPlayer = 1;
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        bluetoothAdapter.enable();
        scanStarted = true;
    }

    public void upgradeState(int newState) {
        if (newState > state) {
            updateState(newState);
        }
    }

    public void downgradeState(int newState) {
        if (newState < state) {
            updateState(newState);
        }
    }

    public void updateState(int newState) {
        state = newState;
        updateUi();
    }

    public void addData(byte[] data) {
        Log.d("Road", HexAsciiHelper.bytesToHex(data));
        touchData = HexAsciiHelper.bytesToHex(data);
        if (touchData == null){
            touchData = "10";
        }
        updateUi();
    }


    public void updateUi() {
        String connectionText = "Disconnected";
        if (state == STATE_CONNECTING) {
            connectionText = "Connecting...";
        } else if (state == STATE_CONNECTED) {
            connectionText = "Connected";
        }
        Log.d("connection text", connectionText);
    }

    @Override
    public void unregisterReceiver(BroadcastReceiver receiver) {
        super.unregisterReceiver(receiver);
    }

    @Override
    public Intent registerReceiver(BroadcastReceiver receiver, IntentFilter filter, String broadcastPermission, Handler scheduler) {
        return super.registerReceiver(receiver, filter, broadcastPermission, scheduler);
    }

    @Override
    public Intent registerReceiver(BroadcastReceiver receiver, IntentFilter filter) {
        return super.registerReceiver(receiver, filter);
    }

    @Override
    public void onLeScan(BluetoothDevice device, final int rssi, final byte[] scanRecord) {
        bluetoothAdapter.stopLeScan(this);
        bluetoothDevice = device;
        BluetoothHelper.getDeviceInfoText(bluetoothDevice, rssi, scanRecord);
    }
}
