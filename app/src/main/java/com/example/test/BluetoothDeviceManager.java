package com.example.test;

import android.bluetooth.BluetoothClass;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.health.connect.datatypes.Device;

public class BluetoothDeviceManager {
    private static BluetoothDevice device;

    private BluetoothDeviceManager() {
        // 私有化構造方法，防止實例化
    }

    public static BluetoothDevice getDevice() {
        return device;
    }

    public static void setDevice(BluetoothDevice bluetoothDevice) {
        device = bluetoothDevice;
    }
}
