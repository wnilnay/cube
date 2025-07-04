package com.example.test;

import android.bluetooth.BluetoothSocket;
import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Timer;
import java.util.ArrayList;
import java.util.List;
import java.lang.ref.WeakReference;

public class BluetoothSocketManager {
    private static BluetoothSocket bluetoothSocket;
    private static Timer keepAliveTimer; // 用於發送 keep-alive 封包
    private static final long KEEP_ALIVE_INTERVAL = 1000; // 1 秒
    private static final long DISCONNECT_TIMEOUT = 5000; // 連續 5 秒視為斷線
    private static long disconnectStartTime = -1;
    private static final List<WeakReference<BluetoothDisconnectListener>> listeners = new ArrayList<>();

    private BluetoothSocketManager() {
        // 私有化構造方法，防止實例化
    }

    public static BluetoothSocket getSocket() {
        return bluetoothSocket;
    }

    public static void addDisconnectListener(BluetoothDisconnectListener listener){
        if(listener == null) return;
        synchronized (listeners){
            for(WeakReference<BluetoothDisconnectListener> ref:listeners){
                if(ref.get()==listener) return;
            }
            listeners.add(new WeakReference<>(listener));
        }
    }

    public static void removeDisconnectListener(BluetoothDisconnectListener listener){
        if(listener == null) return;
        synchronized (listeners){
            listeners.removeIf(ref->ref.get()==null || ref.get()==listener);
        }
    }

    private static synchronized void startKeepAliveTimer(){
        stopKeepAliveTimer();
        keepAliveTimer = new Timer("MyTrashTimerThread");
        keepAliveTimer.schedule(new java.util.TimerTask() {
            @Override
            public void run() {
                //Log.d("wnilnay", "run");
                // 每秒嘗試寫入 keep-alive，透過 IOException 判斷實際連線狀態
                if(bluetoothSocket == null){
                    // 尚未建立連線
                    if(disconnectStartTime < 0){
                        disconnectStartTime = System.currentTimeMillis();
                    }
                }else{
                    try {
                        OutputStream os = bluetoothSocket.getOutputStream();
                        os.write("trash\n\0".getBytes(StandardCharsets.UTF_8));
                        os.flush();
                        // 寫入成功視為連線良好
                        disconnectStartTime = -1;
                    }catch (IOException e){
                        // 寫入失敗，視為斷線
                        if(disconnectStartTime < 0){
                            disconnectStartTime = System.currentTimeMillis();
                        }
                    }
                }

                // 如已累積到超時秒數，觸發斷線流程
                if(disconnectStartTime >=0 && System.currentTimeMillis()-disconnectStartTime >= DISCONNECT_TIMEOUT){
                    handleDisconnected();
                }
            }
        },0,KEEP_ALIVE_INTERVAL);
    }

    private static synchronized void stopKeepAliveTimer(){
        if(keepAliveTimer != null){
            keepAliveTimer.cancel();
            keepAliveTimer = null;
        }
        disconnectStartTime = -1;
    }

    /**
     * 當偵測到長時間斷線時呼叫
     */
    private static void handleDisconnected(){
        stopKeepAliveTimer();
        setSocket(null); // 直接歸零

        // UI 操作要回到主執行緒
        android.os.Handler mainHandler = new android.os.Handler(android.os.Looper.getMainLooper());
        mainHandler.post(() -> {
            List<WeakReference<BluetoothDisconnectListener>> copy;
            synchronized (listeners){
                copy = new ArrayList<>(listeners);
            }

            for(WeakReference<BluetoothDisconnectListener> ref:copy){
                BluetoothDisconnectListener listener = ref.get();
                if(listener == null) continue;
                listener.onBluetoothDisconnected();
            }
            // 斷線後 listener 自行處理 UI（切頁、finish 等）
        });
    }

    public static void setSocket(BluetoothSocket socket) {
        bluetoothSocket = socket;
        if(socket != null && socket.isConnected()){
            startKeepAliveTimer();
        }else{
            stopKeepAliveTimer();
        }
    }

    public static String sendString(String sendTitle, String dataToSend) {
        String result = "";
        if (bluetoothSocket != null && bluetoothSocket.isConnected()) {
            try {
                OutputStream outputStream = bluetoothSocket.getOutputStream();

                String sendString = sendTitle + "\n" + dataToSend + "\0";
                outputStream.write(sendString.getBytes(StandardCharsets.UTF_8));
                outputStream.flush();

                //Toast.makeText(getContext(), "已傳送字串", Toast.LENGTH_SHORT).show();
                result = "已傳送";

                Log.d("wnilnay sendString", sendString);

            } catch (IOException e) {
                //Log.d("BActivity", "IOException: " + e.getMessage());
                //Toast.makeText(getContext(), "傳送失敗：" + e.getMessage(), Toast.LENGTH_SHORT).show();
                result = "傳送失敗：" + e.getMessage();
            }
        } else {
            //Toast.makeText(getContext(), "藍牙未連線", Toast.LENGTH_SHORT).show();
            result = "藍芽未連接";
        }
        return result;
    }
    public static String[] getDataString(){
        try {
            if(bluetoothSocket.isConnected()){
                char read;
                String temp = "";
                InputStream is = bluetoothSocket.getInputStream();
                while (true){
                    if(is.available() == 0) {
                        break;
                    }
                    read = (char)is.read();
                    if(read == '\0') break;
                    temp += read;
                    //Log.d("wnilnay",(int)read+"");
                }
                if (temp.isEmpty()){
                    return null;
                }
                Log.d("wnilnay getDataString",temp);
                return temp.split("\n");
            }
        }
        catch (IOException | NullPointerException e) {
            Log.d("wnilnay getDataString",e.getMessage());
            return new String[]{"Error",""};
        }
        return null;
    }
}
