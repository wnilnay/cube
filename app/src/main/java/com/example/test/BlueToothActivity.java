package com.example.test;

import static java.lang.Thread.sleep;

import androidx.appcompat.app.AppCompatActivity;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.IntentFilter;
import android.os.Bundle;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.os.ParcelUuid;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;

public class BlueToothActivity extends AppCompatActivity {

    private BluetoothDevice device;
    private BluetoothAdapter adapter;
    private String deviceName,deviceAddress;
    private TextView showDevice;
    private EditText dataText;
    private BluetoothSocket socket;
    private ParcelUuid[] deviceUUid;
    private OutputStream os;
    private InputStream is;
    private Timer timer = new Timer();


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_blue_tooth);
        showDevice = findViewById(R.id.textView);
        //dataText = findViewById(R.id.editTextTextPersonName);
        //藍芽調配器
        adapter = BluetoothAdapter.getDefaultAdapter();
        // bluetooth抓到設備發送廣播
        IntentFilter filter = new IntentFilter("android.bluetooth.devicepicker.action.DEVICE_SELECTED");
        if(receiver!=null) {
            registerReceiver(receiver, filter);//廣播
        }
    }
    //廣播回傳
    private BroadcastReceiver receiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            try {
                String action = intent.getAction();
                Log.d("brad",""+action);
                device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                deviceName = device.getName();
                deviceAddress = device.getAddress(); // MAC address
                showDevice.setText("配對裝置:" + deviceName + "\n" + "位址:" + deviceAddress);
                //回傳的選擇裝置進行配對
                device.createBond();

            }catch (SecurityException securityException){
                Log.v("brad","SecurityException");
            }

        }
    };
    public void pairDevice(View view) {
        try {
            if(!adapter.isEnabled()) {
                Toast.makeText(view.getContext(),"先開權限後再點擊按鈕",Toast.LENGTH_SHORT).show();
                //打開藍芽窗(問你是否打開藍芽)
                Intent intent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                intent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION,300);
                startActivity(intent);
            }
            else{
                //藍芽scanner
                //Toast.makeText(view.getContext(),"PairDevice",Toast.LENGTH_SHORT).show();
                Intent bluetoothPicker = new Intent("android.bluetooth.devicepicker.action.LAUNCH");
                startActivity(bluetoothPicker);
                //打開手機藍芽頁面
//                Intent intentSettings = new Intent();
//                intentSettings.setAction(android.provider.Settings.ACTION_BLUETOOTH_SETTINGS);
//                startActivity(intentSettings);

            }
        }
        catch (SecurityException securityException){
            Log.v("brad","SecurityException");
        }
        //當藍芽未開啟
    }
    //當按下傳送按鈕
    public void sendData(View view){
        try {
            /*deviceUUid = device.getUuids();
            Log.d("brad",""+deviceUUid[0].getUuid());
            Log.d("brad",""+deviceUUid.length);
            if(socket == null || !socket.isConnected()) {
                // 連線方法(安全的連線)
                socket = device.createRfcommSocketToServiceRecord(deviceUUid[0].getUuid());

                // 開始連線
                socket.connect();
                Log.v("brad","run");*/
            if(socket.isConnected()){
                Toast.makeText(getApplicationContext(),"連線成功",Toast.LENGTH_SHORT).show();
                os = socket.getOutputStream();//輸入流
                is = socket.getInputStream();//輸出流
            } else {
                Toast.makeText(getApplicationContext(),"連線失敗",Toast.LENGTH_SHORT).show();
            }

            // }

            // 寫入字串
            os.write(dataText.getText().toString().getBytes("utf-8"));
            Toast.makeText(getApplicationContext(),"已傳送字串",Toast.LENGTH_SHORT).show();
        }
        catch (SecurityException securityException){
            Log.v("brad","SecurityException");
        }
        catch (IOException e) {
            Log.d("brad", "IOException: " + e.getMessage());
            Toast.makeText(getApplicationContext(), "連線失敗：" + e.getMessage(), Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            Log.d("brad", "Exception: " + e.getMessage());
            Toast.makeText(getApplicationContext(), "發生錯誤：" + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    public void disConnect(View view) {
        try {
            if(socket != null)
                socket.close();
        }
        catch (IOException e){

        }

        Toast.makeText(getApplicationContext(),"已斷線",Toast.LENGTH_SHORT).show();
    }

    public void connectDevice(View view) {
        try {
            if (device == null) {
                Toast.makeText(this, "請選擇配對裝置", Toast.LENGTH_SHORT).show();
                return;
            }

            socket = null;
            is = null;
            os = null;
            deviceUUid = device.getUuids();
            Log.d("brad", "" + deviceUUid.length);

            if (socket == null || !socket.isConnected()) {
                // 連線方法(安全的連線)
                socket = device.createRfcommSocketToServiceRecord(UUID.fromString("00001101-0000-1000-8000-00805f9b34fb"));

                // 開始連線
                socket.connect();
                Log.v("brad", "run");

                if (socket.isConnected()) {
                    Toast.makeText(getApplicationContext(), "連線成功", Toast.LENGTH_SHORT).show();

                    // 保存 BluetoothSocket
                    BluetoothSocketManager.setSocket(socket);

                    // 跳轉到 B Activity
                    Intent intent = new Intent(this, MainActivity.class);
                    startActivity(intent);

                    finish();
                } else {
                    Toast.makeText(getApplicationContext(), "連線失敗", Toast.LENGTH_SHORT).show();
                }
            }
        } catch (SecurityException | IOException exception) {
            Log.v("brad", exception.getMessage());
        }
    }

    public void catchData(View view) {
        try {
            if(socket.isConnected()){
                char read;
                String temp = "";
                while (true){
                    read = (char)is.read();
                    temp += read;
                    //Log.v("brad",(int)read+"");
                    if(is.available() == 0){
                        break;
                    }
                }
                Log.v("brad",temp);
            }
        }
        catch (IOException | NullPointerException e) {
            Log.v("brad",e.getMessage());
        }
    }

    public void Enter(View view) {
        Intent intent = new Intent(this, MainActivity.class);
        startActivity(intent);
    }
}