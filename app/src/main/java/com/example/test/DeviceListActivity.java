// DeviceListActivity.java (修改後，引導使用者使用 USB 配對)
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

import androidx.activity.EdgeToEdge;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.util.HashSet;
import java.util.Set;

public class DeviceListActivity extends AppCompatActivity {

    private static final String TAG = "DeviceListActivity";
    public static String EXTRA_DEVICE_ADDRESS = "device_address";

    private BluetoothAdapter mBtAdapter;
    private ArrayAdapter<String> mPairedDevicesArrayAdapter;
    private ArrayAdapter<String> mNewDevicesArrayAdapter;
    private Set<BluetoothDevice> mPairedDevices;
    private final Set<BluetoothDevice> mNewDevices = new HashSet<>();

    @RequiresApi(api = Build.VERSION_CODES.S)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        // 在 setContentView 之前請求視窗特性
        supportRequestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
        setContentView(R.layout.activity_device_list);

        // 檢查和請求必要的藍牙權限
        checkAndRequestBluetoothPermissions();

        // 設置預設結果，以防使用者直接返回
        setResult(Activity.RESULT_CANCELED);

        // 初始化掃描按鈕
        Button scanButton = findViewById(R.id.button_scan);
        scanButton.setOnClickListener(v -> {
            doDiscovery();
            v.setVisibility(View.GONE); // 點擊後隱藏按鈕
        });

        // 初始化用於已配對和新發現裝置的 ArrayAdapter
        mPairedDevicesArrayAdapter = new ArrayAdapter<>(this, R.layout.device_name);
        mNewDevicesArrayAdapter = new ArrayAdapter<>(this, R.layout.device_name);

        // 設定已配對裝置的 ListView
        ListView pairedListView = findViewById(R.id.paired_devices);
        pairedListView.setAdapter(mPairedDevicesArrayAdapter);
        pairedListView.setOnItemClickListener(mDeviceClickListener);

        // 設定新發現裝置的 ListView
        ListView newDevicesListView = findViewById(R.id.new_devices);
        newDevicesListView.setAdapter(mNewDevicesArrayAdapter);
        newDevicesListView.setOnItemClickListener(mDeviceClickListener);

        // 註冊廣播接收器以監聽裝置發現和掃描完成事件
        IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        this.registerReceiver(mReceiver, filter);

        filter = new IntentFilter(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
        this.registerReceiver(mReceiver, filter);

        // 獲取本地藍牙適配器
        mBtAdapter = BluetoothAdapter.getDefaultAdapter();

        // 填充已配對裝置列表
        populatePairedDevicesList();
    }

    /**
     * 獲取並顯示已配對的裝置列表。
     */
    @RequiresApi(api = Build.VERSION_CODES.S)
    private void populatePairedDevicesList() {
        // 權限檢查
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
            checkAndRequestBluetoothPermissions(); // 如果沒有權限，再次請求
            return;
        }

        mPairedDevices = mBtAdapter.getBondedDevices();
        mPairedDevicesArrayAdapter.clear(); // 清空以防重複添加

        if (mPairedDevices != null && mPairedDevices.size() > 0) {
            findViewById(R.id.title_paired_devices).setVisibility(View.VISIBLE);
            for (BluetoothDevice device : mPairedDevices) {
                mPairedDevicesArrayAdapter.add(device.getName() + "\n" + device.getAddress());
            }
        } else {
            String noDevices = "沒有已配對的裝置";
            mPairedDevicesArrayAdapter.add(noDevices);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // 確保在 Activity 銷毀時停止掃描
        if (mBtAdapter != null) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN) == PackageManager.PERMISSION_GRANTED) {
                mBtAdapter.cancelDiscovery();
            }
        }
        // 取消註冊廣播接收器
        this.unregisterReceiver(mReceiver);
    }

    /**
     * 開始掃描新裝置。
     */
    @RequiresApi(api = Build.VERSION_CODES.S)
    private void doDiscovery() {
        Log.d(TAG, "doDiscovery()");
        // 顯示標題欄的進度條
        setProgressBarIndeterminateVisibility(true);
        setTitle("正在掃描裝置...");

        // 顯示 "新裝置" 的標題
        findViewById(R.id.title_new_devices).setVisibility(View.VISIBLE);

        // 權限檢查
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED) {
            checkAndRequestBluetoothPermissions();
            return;
        }

        // 如果正在掃描，先取消
        if (mBtAdapter.isDiscovering()) {
            mBtAdapter.cancelDiscovery();
        }

        // 清空上次掃描到的新裝置列表
        mNewDevicesArrayAdapter.clear();
        mNewDevices.clear();

        // 開始掃描
        mBtAdapter.startDiscovery();
    }

    /**
     * ListView 中項目被點擊時的監聽器。
     */
    private final AdapterView.OnItemClickListener mDeviceClickListener = new AdapterView.OnItemClickListener() {
        @RequiresApi(api = Build.VERSION_CODES.S)
        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            // 權限檢查
            if (ContextCompat.checkSelfPermission(getBaseContext(), Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED) {
                checkAndRequestBluetoothPermissions();
                return;
            }
            // 在嘗試連接前，停止掃描以節省資源
            mBtAdapter.cancelDiscovery();

            // 從點擊的文字中獲取 MAC 位址（最後17個字元）
            String info = ((TextView) view).getText().toString();
            String address = info.substring(info.length() - 17);

            // 根據位址獲取 BluetoothDevice 物件
            BluetoothDevice device = mBtAdapter.getRemoteDevice(address);

            // 將選擇的裝置設定到管理器中
            BluetoothDeviceManager.setDevice(device);

            // 設置結果並結束此 Activity
            setResult(Activity.RESULT_OK);
            finish();
        }
    };

    /**
     * 監聽藍牙掃描廣播的接收器。
     */
    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @RequiresApi(api = Build.VERSION_CODES.S)
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();

            // 當發現一個新裝置時
            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);

                // 權限檢查
                if (ContextCompat.checkSelfPermission(getBaseContext(), Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                    checkAndRequestBluetoothPermissions();
                    return;
                }

                if (device != null && device.getName() != null) {
                    // --- [核心修改] ---
                    // 檢查是否是我們的目標裝置 "Cube Solver"，並且它還未配對
                    if ("Cube Solver".equalsIgnoreCase(device.getName()) && device.getBondState() != BluetoothDevice.BOND_BONDED) {
                        // 如果是，彈出提示對話框，引導使用者去 USB 配對
                        showUsbPairingSuggestionDialog();
                    }

                    // 只有當裝置未配對且不重複時，才將其添加到新裝置列表
                    boolean isPaired = mPairedDevices != null && mPairedDevices.contains(device);
                    boolean isAlreadyFound = mNewDevices.contains(device);

                    if (!isPaired && !isAlreadyFound) {
                        mNewDevices.add(device);
                        mNewDevicesArrayAdapter.add(device.getName() + "\n" + device.getAddress());
                    }
                }
            } else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)) {
                // 當掃描結束時
                setProgressBarIndeterminateVisibility(false);
                setTitle("選擇要連接的裝置");
                if (mNewDevicesArrayAdapter.getCount() == 0) {
                    String noDevices = "沒有發現新裝置";
                    mNewDevicesArrayAdapter.add(noDevices);
                }
            }
        }
    };

    /**
     * [新增] 彈出一個對話框，建議使用者透過 USB 終端機進行配對。
     */
    private void showUsbPairingSuggestionDialog() {
        new AlertDialog.Builder(this)
                .setTitle("配對建議")
                .setMessage("偵測到尚未配對的魔術方塊解算機 (Cube Solver)。\n\n為了獲得最穩定可靠的配對體驗，強烈建議您返回『設定』頁面，使用『USB 終端機』功能來完成首次配對。")
                .setPositiveButton("我知道了", null) // 只提供一個確認按鈕
                .setIcon(android.R.drawable.ic_dialog_info)
                .show();
    }

    /**
     * [新增] 集中處理權限檢查和請求的輔助方法。
     */
    @RequiresApi(api = Build.VERSION_CODES.S)
    private void checkAndRequestBluetoothPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) { // API 31 及以上
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED ||
                    ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.BLUETOOTH_CONNECT, Manifest.permission.BLUETOOTH_SCAN},
                        0);
            }
        } else { // API 30 及以下
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_ADMIN) != PackageManager.PERMISSION_GRANTED ||
                    ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.BLUETOOTH, Manifest.permission.BLUETOOTH_ADMIN, Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION},
                        0);
            }
        }
    }
}