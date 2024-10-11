package com.example.test;

import static java.lang.Thread.sleep;

import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Build;
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
    private static final int REQUEST_CODE = 1;


    @RequiresApi(api = Build.VERSION_CODES.S)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_blue_tooth);
        showDevice = findViewById(R.id.textView);
        //dataText = findViewById(R.id.editTextTextPersonName);

        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.BLUETOOTH) != PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(this, android.Manifest.permission.BLUETOOTH_ADMIN) != PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(this,
                    new String[]{android.Manifest.permission.BLUETOOTH, android.Manifest.permission.BLUETOOTH_ADMIN, android.Manifest.permission.ACCESS_FINE_LOCATION,Manifest.permission.BLUETOOTH_CONNECT},
                    0);
        }

        //藍芽調配器
        adapter = BluetoothAdapter.getDefaultAdapter();
    }
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
                Intent intent = new Intent(this,DeviceListActivity.class);
                startActivityForResult(intent,REQUEST_CODE);
            }
        }
        catch (SecurityException securityException){
            Log.v("brad","SecurityException");
        }
        //當藍芽未開啟
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if(requestCode == REQUEST_CODE && resultCode == RESULT_OK){
            try {
                device = BluetoothDeviceManager.getDevice();
                deviceName = device.getName();
                deviceAddress = device.getAddress(); // MAC address
                showDevice.setText("配對裝置:" + deviceName + "\n" + "位址:" + deviceAddress);
                //回傳的選擇裝置進行配對
                device.createBond();

            }catch (SecurityException securityException){
                Log.v("wnilnay catch","SecurityException");
            }
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
            Log.e("brad", exception.toString());
        }
    }
}