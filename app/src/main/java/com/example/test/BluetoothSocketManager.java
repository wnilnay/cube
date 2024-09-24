package com.example.test;

import android.bluetooth.BluetoothSocket;

public class BluetoothSocketManager {
    private static BluetoothSocket bluetoothSocket;

    private BluetoothSocketManager() {
        // 私有化構造方法，防止實例化
    }

    public static BluetoothSocket getSocket() {
        return bluetoothSocket;
    }

    public static void setSocket(BluetoothSocket socket) {
        bluetoothSocket = socket;
    }
}
