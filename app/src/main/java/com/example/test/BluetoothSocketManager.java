package com.example.test;

import static java.security.AccessController.getContext;

import android.bluetooth.BluetoothSocket;
import android.util.Log;
import android.widget.Toast;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

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
    public static String sendString(String sendTitle, String dataToSend) {
        String result = "";
        if (bluetoothSocket != null && bluetoothSocket.isConnected()) {
            try {
                OutputStream outputStream = bluetoothSocket.getOutputStream();

                String sendString = sendTitle + "\n" + dataToSend + "\0";
                outputStream.write(sendString.getBytes("utf-8"));

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

    public static String[] getString(){
        try {
            if(bluetoothSocket.isConnected()){
                char read;
                String temp = "";
                InputStream is = bluetoothSocket.getInputStream();
                while (true){
                    if(is.available() == 0){
                        break;
                    }
                    read = (char)is.read();
                    if(read == '\0') break;
                    temp += read;
                    //Log.d("wnilnay",(int)read+"");
                }
                return temp.split("\n");
            }
        }
        catch (IOException | NullPointerException e) {
            Log.d("wnilnay",e.getMessage());
            return new String[]{"Error",""};
        }
        return null;
    }
}
