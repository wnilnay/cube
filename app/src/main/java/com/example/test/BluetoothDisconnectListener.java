package com.example.test;

public interface BluetoothDisconnectListener {
    /**
     * 當藍芽偵測到長時間斷線時呼叫。
     * 可於此顯示 Toast 或執行其他 UI 相關處理。
     */
    void onBluetoothDisconnected();
} 