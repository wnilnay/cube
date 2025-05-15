package com.example.test;

import android.Manifest;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;

import android.os.ParcelUuid;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Timer;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

public class BlueToothFragment extends Fragment {

    private BluetoothDevice device;
    private BluetoothAdapter adapter;
    private String deviceName,deviceAddress;
    private TextView showDevice;
    private Button button_pair, button_connect;
    private EditText dataText;
    private BluetoothSocket socket;
    private ParcelUuid[] deviceUUid;
    private OutputStream os;
    private InputStream is;
    private Timer timer = new Timer();
    private static final int REQUEST_CODE = 1;

    @RequiresApi(api = Build.VERSION_CODES.S)
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_blue_tooth, container, false);

        showDevice = view.findViewById(R.id.textView);
        button_connect = view.findViewById(R.id.button_connect);
        button_pair = view.findViewById(R.id.button_pair);

//        Intent intent = new Intent(this, MainActivity.class);
//        startActivity(intent);

        //dataText = findViewById(R.id.editTextTextPersonName);

//        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.BLUETOOTH) != PackageManager.PERMISSION_GRANTED ||
//                ContextCompat.checkSelfPermission(this, android.Manifest.permission.BLUETOOTH_ADMIN) != PackageManager.PERMISSION_GRANTED ||
//                ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED ||
//                ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED ||
//                ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED ||
//                ContextCompat.checkSelfPermission(this,Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED) {
//
//            requestPermissions(
//                    new String[]{android.Manifest.permission.BLUETOOTH,
//                    android.Manifest.permission.BLUETOOTH_ADMIN,
//                    android.Manifest.permission.ACCESS_FINE_LOCATION,
//                    Manifest.permission.ACCESS_COARSE_LOCATION,
//                    Manifest.permission.BLUETOOTH_CONNECT,
//                    Manifest.permission.BLUETOOTH_SCAN},
//                    0);
//            Log.d("wnilnay ContextCompat0", ContextCompat.checkSelfPermission(this,Manifest.permission.BLUETOOTH_CONNECT) + "");
//        }
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
//            // Android 12 (API 31) 和以上需要的權限
//            if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED ||
//                    ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED ||
//                    ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED ||
//                    ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
//
//                // 請求 Android 12 的新權限
//                requestPermissions(
//                        new String[]{
//                                Manifest.permission.BLUETOOTH_CONNECT,
//                                Manifest.permission.BLUETOOTH_SCAN,
//                                Manifest.permission.ACCESS_FINE_LOCATION,
//                                Manifest.permission.ACCESS_COARSE_LOCATION
//                        },
//                        0);
//            }
//        } else {
//            // Android 12 以下版本需要的權限
//            if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.BLUETOOTH) != PackageManager.PERMISSION_GRANTED ||
//                    ContextCompat.checkSelfPermission(this, android.Manifest.permission.BLUETOOTH_ADMIN) != PackageManager.PERMISSION_GRANTED ||
//                    ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED ||
//                    ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
//
//                // 請求舊版 Android 所需的權限
//                requestPermissions(
//                        new String[]{
//                                android.Manifest.permission.BLUETOOTH,
//                                android.Manifest.permission.BLUETOOTH_ADMIN,
//                                Manifest.permission.ACCESS_FINE_LOCATION,
//                                Manifest.permission.ACCESS_COARSE_LOCATION
//                        },
//                        0);
//            }
//        }
        // 檢查和請求藍牙權限
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) { // API 31 及以上
            if (ContextCompat.checkSelfPermission(requireContext(), android.Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED ||
                    ContextCompat.checkSelfPermission(requireContext(), android.Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED) {

                ActivityCompat.requestPermissions(requireActivity(),
                        new String[]{android.Manifest.permission.BLUETOOTH_CONNECT, android.Manifest.permission.BLUETOOTH_SCAN},
                        0);
            }
        } else { // 針對 API 30 及以下
            if (ContextCompat.checkSelfPermission(requireContext(), android.Manifest.permission.BLUETOOTH) != PackageManager.PERMISSION_GRANTED ||
                    ContextCompat.checkSelfPermission(requireContext(), android.Manifest.permission.BLUETOOTH_ADMIN) != PackageManager.PERMISSION_GRANTED ||
                    ContextCompat.checkSelfPermission(requireContext(), android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED ||
                    ContextCompat.checkSelfPermission(requireContext(), android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {

                ActivityCompat.requestPermissions(requireActivity(),
                        new String[]{android.Manifest.permission.BLUETOOTH, android.Manifest.permission.BLUETOOTH_ADMIN, android.Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION},
                        0);
            }
        }



        //藍芽調配器
        adapter = BluetoothAdapter.getDefaultAdapter();

        button_pair.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                pairDevice();
            }
        });

        button_connect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                connectDevice();
            }
        });
        return view;
    }

    public void pairDevice() {
        try {
            if(!adapter.isEnabled()) {
                Toast.makeText(getContext(),"先開權限後再點擊按鈕",Toast.LENGTH_SHORT).show();
                //打開藍芽窗(問你是否打開藍芽)
                Intent intent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                intent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION,300);
                startActivity(intent);
            }
            else{
                Intent intent = new Intent(getContext(),DeviceListActivity.class);
                startActivityForResult(intent,REQUEST_CODE);
            }
        }
        catch (SecurityException securityException){
            Log.v("brad","SecurityException");
        }
        //當藍芽未開啟
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if(requestCode == REQUEST_CODE && resultCode == Activity.RESULT_OK){
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

        Toast.makeText(getContext(),"已斷線",Toast.LENGTH_SHORT).show();
    }

    public void connectDevice() {
        try {
            if (device == null) {
                Toast.makeText(getContext(), "請選擇配對裝置", Toast.LENGTH_SHORT).show();
                return;
            }

            socket = null;
            is = null;
            os = null;
            deviceUUid = device.getUuids();

            Toast.makeText(getContext(), "連線中", Toast.LENGTH_SHORT).show();
            //Log.d("brad", "" + deviceUUid.length);

            new Thread(new Runnable() {
                @Override
                public void run() {
                    if (socket == null || !socket.isConnected()) {
                        try {
                            socket = device.createRfcommSocketToServiceRecord(UUID.fromString("00001101-0000-1000-8000-00805f9b34fb"));

                            // 設置連接超時
                            final CountDownLatch connectLatch = new CountDownLatch(1);
                            final AtomicBoolean connectionSuccess = new AtomicBoolean(false);
                            final AtomicReference<Exception> connectionException = new AtomicReference<>();
                            final int CONNECTION_TIMEOUT = 10000;

                            try {
                                socket.connect();
                                connectionSuccess.set(true);
                            } catch (Exception e) {
                                connectionException.set(e);
                            } finally {
                                connectLatch.countDown();
                            }
                           boolean isComplete = connectLatch.await(CONNECTION_TIMEOUT, TimeUnit.MILLISECONDS);
                        }
                        catch (InterruptedException | IOException e) {
                            throw new RuntimeException(e);
                        }

                        if (socket.isConnected()) {
                            // 保存 BluetoothSocket
                            BluetoothSocketManager.setSocket(socket);

                            requireActivity().runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    Toast.makeText(getContext(), "連線成功", Toast.LENGTH_SHORT).show();

                                    ((MainActivity)getActivity()).change_to_mainFragment();
                                }
                            });
                            // 跳轉到 B Activity
//                    Intent intent = new Intent(this, MainActivity.class);
//                    startActivity(intent);

                            //finish();
                        } else {
                            requireActivity().runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    Toast.makeText(getContext(), "連線失敗", Toast.LENGTH_SHORT).show();
                                }
                            });
                        }
                    }

                }
            }).start();

        } catch (SecurityException exception) {
            Log.e("brad", exception.toString());
        }
    }
}