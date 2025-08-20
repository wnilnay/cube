package com.example.test;

import android.Manifest;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class DeviceListActivity extends AppCompatActivity {

    private static final String TAG = "DeviceListActivity";
    public static String EXTRA_DEVICE_ADDRESS = "device_address";

    private static final int PERMISSIONS_REQUEST_CODE = 100;

    private BluetoothAdapter mBtAdapter;
    private Set<BluetoothDevice> pairedDevices = new HashSet<>();
    private final Set<BluetoothDevice> newDevices = new HashSet<>();

    private Button scanButton;
    private ArrayAdapter<String> mNewDevicesArrayAdapter;
    private ArrayAdapter<String> mPairedDevicesArrayAdapter;

    private boolean isReceiverRegistered = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
        setContentView(R.layout.activity_device_list);

        setResult(Activity.RESULT_CANCELED);

        mBtAdapter = BluetoothAdapter.getDefaultAdapter();
        if (mBtAdapter == null) {
            Toast.makeText(this, "此設備不支援藍牙", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        if (checkAndRequestPermissions()) {
            initializeBluetoothSetup();
        }
    }

    private boolean checkAndRequestPermissions() {
        String[] requiredPermissions;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            requiredPermissions = new String[]{
                    Manifest.permission.BLUETOOTH_CONNECT,
                    Manifest.permission.BLUETOOTH_SCAN
            };
        } else {
            requiredPermissions = new String[]{
                    Manifest.permission.BLUETOOTH,
                    Manifest.permission.BLUETOOTH_ADMIN,
                    Manifest.permission.ACCESS_FINE_LOCATION
            };
        }

        List<String> permissionsToRequest = new ArrayList<>();
        for (String permission : requiredPermissions) {
            if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(permission);
            }
        }

        if (!permissionsToRequest.isEmpty()) {
            ActivityCompat.requestPermissions(this, permissionsToRequest.toArray(new String[0]), PERMISSIONS_REQUEST_CODE);
            return false;
        }

        return true;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSIONS_REQUEST_CODE) {
            boolean allPermissionsGranted = true;
            for (int grantResult : grantResults) {
                if (grantResult != PackageManager.PERMISSION_GRANTED) {
                    allPermissionsGranted = false;
                    break;
                }
            }

            if (allPermissionsGranted) {
                initializeBluetoothSetup();
            } else {
                Toast.makeText(this, "藍牙權限是此功能所必需的。請在應用程式設定中授予權限。", Toast.LENGTH_LONG).show();
                finish();
            }
        }
    }

    private void initializeBluetoothSetup() {
        scanButton = findViewById(R.id.button_scan);
        scanButton.setOnClickListener(v -> {
            new AlertDialog.Builder(DeviceListActivity.this)
                    .setTitle("配對建議")
                    .setMessage("為了獲得最穩定可靠的配對體驗，強烈建議您返回『設定』頁面，使用『USB 終端機』功能來完成首次配對。")
                    .setPositiveButton("我知道了", null)
                    .setIcon(android.R.drawable.ic_dialog_info)
                    .show();
            doDiscovery();
            v.setVisibility(View.GONE);
        });

        mPairedDevicesArrayAdapter = new ArrayAdapter<>(this, R.layout.device_name);
        mNewDevicesArrayAdapter = new ArrayAdapter<>(this, R.layout.device_name);

        ListView pairedListView = findViewById(R.id.paired_devices);
        pairedListView.setAdapter(mPairedDevicesArrayAdapter);
        pairedListView.setOnItemClickListener(mDeviceClickListener);

        ListView newDevicesListView = findViewById(R.id.new_devices);
        newDevicesListView.setAdapter(mNewDevicesArrayAdapter);
        newDevicesListView.setOnItemClickListener(mDeviceClickListener);

        IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        registerReceiver(mReceiver, filter);
        filter = new IntentFilter(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
        registerReceiver(mReceiver, filter);
        isReceiverRegistered = true;

        try {
            pairedDevices = mBtAdapter.getBondedDevices();
            if (pairedDevices.size() > 0) {
                findViewById(R.id.title_paired_devices).setVisibility(View.VISIBLE);
                for (BluetoothDevice device : pairedDevices) {
                    mPairedDevicesArrayAdapter.add(device.getName() + "\n" + device.getAddress());
                }
            } else {
                String noDevices = "沒有已配對的設備";
                mPairedDevicesArrayAdapter.add(noDevices);
            }
        } catch (SecurityException e) {
            Log.e(TAG, "初始化取得已配對設備時發生權限錯誤", e);
            Toast.makeText(this, "缺少藍牙權限，無法取得已配對設備", Toast.LENGTH_SHORT).show();
        }
    }

    private void doDiscovery() {
        Log.d(TAG, "doDiscovery()");
        setProgressBarIndeterminateVisibility(true);
        setTitle("正在掃描設備…");
        findViewById(R.id.title_new_devices).setVisibility(View.VISIBLE);

        try {
            if (mBtAdapter.isDiscovering()) {
                mBtAdapter.cancelDiscovery();
            }
            mBtAdapter.startDiscovery();
        } catch (SecurityException e) {
            Log.e(TAG, "開始掃描時發生權限錯誤", e);
            Toast.makeText(this, "缺少藍牙權限，無法開始掃描", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        try {
            if (mBtAdapter != null && mBtAdapter.isDiscovering()) {
                mBtAdapter.cancelDiscovery();
            }
        } catch (SecurityException e) {
            Log.e(TAG, "銷毀 Activity 時取消掃描發生權限錯誤", e);
        }

        if (isReceiverRegistered) {
            unregisterReceiver(mReceiver);
        }
    }

    private final AdapterView.OnItemClickListener mDeviceClickListener = (parent, view, position, id) -> {
        try {
            if (mBtAdapter.isDiscovering()) {
                mBtAdapter.cancelDiscovery();
            }
        } catch (SecurityException e) {
            Log.e(TAG, "點擊項目時取消掃描發生權限錯誤", e);
        }

        String info = ((TextView) view).getText().toString();
        if (info.equals("沒有已配對的設備") || info.equals("未發現任何設備")) {
            return;
        }
        String address = info.substring(info.length() - 17);

        BluetoothDevice deviceToConnect = null;
        Set<BluetoothDevice> allDevices = new HashSet<>(pairedDevices);
        allDevices.addAll(newDevices);

        for (BluetoothDevice device : allDevices) {
            if (device.getAddress().equals(address)) {
                deviceToConnect = device;
                break;
            }
        }

        if (deviceToConnect != null) {
            BluetoothDeviceManager.setDevice(deviceToConnect);
        }

        Intent intent = new Intent();
        intent.putExtra(EXTRA_DEVICE_ADDRESS, address);

        setResult(Activity.RESULT_OK, intent);
        finish();
    };

    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();

            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                try {
                    if (device != null && device.getBondState() != BluetoothDevice.BOND_BONDED && device.getName() != null) {
                        if (newDevices.add(device)) {
                            if (mNewDevicesArrayAdapter.getCount() == 1 && mNewDevicesArrayAdapter.getItem(0).equals("未發現任何設備")) {
                                mNewDevicesArrayAdapter.clear();
                            }
                            mNewDevicesArrayAdapter.add(device.getName() + "\n" + device.getAddress());
                        }
                    }
                } catch (SecurityException e) {
                    Log.e(TAG, "處理掃描到的設備時發生權限錯誤", e);
                }
            } else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)) {
                setProgressBarIndeterminateVisibility(false);
                setTitle("選擇要連線的設備");
                if (scanButton != null) {
                    scanButton.setVisibility(View.VISIBLE);
                }
                if (mNewDevicesArrayAdapter.getCount() == 0) {
                    mNewDevicesArrayAdapter.add("未發現任何設備");
                }
            }
        }
    };
}