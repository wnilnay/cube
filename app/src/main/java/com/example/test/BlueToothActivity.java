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

public class BlueToothActivity extends AppCompatActivity {

    private BluetoothDevice device;
    private BluetoothAdapter adapter;
    private String deviceName, deviceAddress;
    private TextView showDevice;
    private EditText dataText;
    private BluetoothSocket socket;
    private ParcelUuid[] deviceUUid;
    private OutputStream os;
    private InputStream is;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_blue_tooth);

        showDevice = findViewById(R.id.textView);
        dataText = findViewById(R.id.editTextTextPersonName);
        //藍芽調配器
        adapter = BluetoothAdapter.getDefaultAdapter();
        // bluetooth抓到設備發送廣播
        IntentFilter filter = new IntentFilter("android.bluetooth.devicepicker.action.DEVICE_SELECTED");
        if(receiver!=null) {
            registerReceiver(receiver, filter);//廣播
        }
    }
    private BroadcastReceiver receiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            try {
                String action = intent.getAction();
                Log.d("taggg",""+action);
                device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                deviceName = device.getName();
                deviceAddress = device.getAddress(); // MAC address
                showDevice.setText("配對裝置:" + deviceName + "\n" + "位址:" + deviceAddress);

                //回傳的選擇裝置進行配對
                device.createBond();
            } catch (SecurityException e) {
                Log.e("CreateBondError", e.getMessage());
            }
        }
    };
    public void pairDevice(View view) {
        //當藍芽未開啟
        if(!adapter.isEnabled()) {
            try {
                Toast.makeText(view.getContext(),"先開權限後再點擊按鈕",Toast.LENGTH_SHORT).show();
                //打開藍芽窗(問你是否打開藍芽)
                Intent intent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                intent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION,300);
                startActivity(intent);
            }
            catch (SecurityException securityException) {

            }
        }
        else{
            //藍芽scanner
            Toast.makeText(view.getContext(),"PairDevice",Toast.LENGTH_SHORT).show();
            /*Intent bluetoothPicker = new Intent("android.bluetooth.devicepicker.action.LAUNCH");
            startActivity(bluetoothPicker);*/
            //打開手機藍芽頁面
            Intent intentSettings = new Intent();
            intentSettings.setAction(android.provider.Settings.ACTION_BLUETOOTH_SETTINGS);
            startActivity(intentSettings);

        }
    }
    //當按下傳送按鈕
    public void sendData(View view){
        try {
            deviceUUid = device.getUuids();
            Log.d("UUid",""+deviceUUid[0].getUuid());
            Log.d("UUidSize",""+deviceUUid.length);
            if(socket==null){
                //連線方法1(不安全的連線)
//                socket=device.createInsecureRfcommSocketToServiceRecord(deviceUUid[0].getUuid());
                //連線方法2(安全的連線)
                socket=device.createRfcommSocketToServiceRecord(deviceUUid[0].getUuid());
                //迴圈進行連線
                while(!socket.isConnected()){
                    try {
                        sleep(5000);
                        socket.connect();
                        Log.d("Connect State",""+socket.isConnected());
                        if(socket.isConnected()){
                            Toast.makeText(getApplicationContext(),"連線成功",Toast.LENGTH_SHORT).show();
                        }
                    }
                    catch (IOException | InterruptedException | SecurityException e) {
                        e.printStackTrace();
                    }
                }
                os=socket.getOutputStream();//輸入流
                is=socket.getInputStream();//輸出流
            }
            os.write(dataText.getText().toString().getBytes("utf-8"));//utf-8寫入選擇裝置
            Toast.makeText(getApplicationContext(),"已傳送字串",Toast.LENGTH_SHORT).show();
            Log.d("sendText",""+dataText.getText().toString().getBytes());
        }
        catch (SecurityException |IOException e){
            Log.d("Socket Error",""+e);
        }
    }
    public void disConnect(View view){
        socket = null;
        is = null;
        os = null;
        Toast.makeText(getApplicationContext(),"已斷線",Toast.LENGTH_SHORT).show();
    }
}