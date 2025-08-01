package com.example.test;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class BluetoothModeActivity extends AppCompatActivity implements BluetoothDisconnectListener {
    private Button shutdownButton, resetBluetoothButton, restartProgramButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_bluetooth_mode);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.title_card), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // 初始化按鈕
        shutdownButton = findViewById(R.id.shutdown_button);
        resetBluetoothButton = findViewById(R.id.reset_bluetooth_button);
        restartProgramButton = findViewById(R.id.restart_program_button);

        // 註冊至 BluetoothSocketManager，以便藍芽斷線時自動關閉
        BluetoothSocketManager.addDisconnectListener(this);

        // 停止程式並關機
        shutdownButton.setOnClickListener(v -> {
            new AlertDialog.Builder(this)
                    .setTitle("確認關機")
                    .setMessage("確定要停止程式並關閉樹莓派嗎？")
                    .setNegativeButton("取消", null)
                    .setPositiveButton("確定", (dialog, which) -> {
                        BluetoothSocketManager.sendString("Shutdown", "");
                        Toast.makeText(this, "已發送關機指令", Toast.LENGTH_SHORT).show();
                        finish();
                    })
                    .create()
                    .show();
        });

        // 重設藍牙
        resetBluetoothButton.setOnClickListener(v -> {
            new AlertDialog.Builder(this)
                    .setTitle("確認重設藍牙")
                    .setMessage("確定要重新初始化藍牙連線嗎？")
                    .setNegativeButton("取消", null)
                    .setPositiveButton("確定", (dialog, which) -> {
                        BluetoothSocketManager.sendString("ResetBluetooth", "");
                        Toast.makeText(this, "已發送重設藍牙指令", Toast.LENGTH_SHORT).show();
                        finish();
                    })
                    .create()
                    .show();
        });

        // 重啟主程式
        restartProgramButton.setOnClickListener(v -> {
            new AlertDialog.Builder(this)
                    .setTitle("確認重啟")
                    .setMessage("確定要重新啟動主程式嗎？")
                    .setNegativeButton("取消", null)
                    .setPositiveButton("確定", (dialog, which) -> {
                        BluetoothSocketManager.sendString("RestartProgram", "");
                        Toast.makeText(this, "已發送重啟指令", Toast.LENGTH_SHORT).show();
                        finish();
                    })
                    .create()
                    .show();
        });
    }

    @Override
    protected void onDestroy() {
        // 取消註冊
        BluetoothSocketManager.removeDisconnectListener(this);
        super.onDestroy();
    }

    @Override
    public void onBluetoothDisconnected() {
        Toast.makeText(this, "藍芽斷線，將退出藍牙模式頁面", Toast.LENGTH_SHORT).show();
        finish();
    }
} 