// UsbTerminalActivity.java (Final Debug Version)
package com.example.test;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import org.json.JSONException;
import org.json.JSONObject;

public class UsbTerminalActivity extends AppCompatActivity {
    private static final String TAG = "UsbTerminalActivity";

    private EditText commandInput;
    private TextView mainTerminalText;
    private TextView newTerminalText;
    private ScrollView mainTerminalScrollView;
    private ScrollView newTerminalScrollView;
    private Button sendButton;
    private Button clearButton;
    private Button testButton;
    private Button toggleReadOnlyButton;

    private UsbConnectionManager usbManager;
    private Handler mainHandler;
    private boolean isInputEnabled = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_usb_terminal);
        Log.d(TAG, "onCreate: Activity 正在創建。");

        initViews();

        usbManager = UsbConnectionManager.getInstance(this);
        mainHandler = new Handler(Looper.getMainLooper());

        setupButtonListeners();
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.d(TAG, "onResume: Activity 恢復前景。");
        checkConnectionStatus();
    }

    private void initViews() {
        Log.d(TAG, "initViews: 開始初始化 UI 元件。");
        mainTerminalText = findViewById(R.id.main_terminal_text);
        newTerminalText = findViewById(R.id.new_terminal_text);
        commandInput = findViewById(R.id.command_input);

        sendButton = findViewById(R.id.send_command_button);
        clearButton = findViewById(R.id.clear_terminal_button);
        testButton = findViewById(R.id.test_usb_button);
        toggleReadOnlyButton = findViewById(R.id.toggle_readonly_button);

        if (mainTerminalText != null && mainTerminalText.getParent() instanceof ScrollView) {
            mainTerminalScrollView = (ScrollView) mainTerminalText.getParent();
        } else {
            Log.w(TAG, "initViews: 未能找到 main_terminal_text 的 ScrollView 父容器。");
        }

        if (newTerminalText != null && newTerminalText.getParent() instanceof ScrollView) {
            newTerminalScrollView = (ScrollView) newTerminalText.getParent();
        } else {
            Log.w(TAG, "initViews: 未能找到 new_terminal_text 的 ScrollView 父容器。");
        }
        Log.d(TAG, "initViews: UI 元件初始化完畢。");
    }

    private void setupButtonListeners() {
        if (sendButton != null) {
            sendButton.setOnClickListener(v -> sendCommand());
        }
        if (clearButton != null) {
            clearButton.setOnClickListener(v -> {
                if (mainTerminalText != null) mainTerminalText.setText("");
                if (newTerminalText != null) newTerminalText.setText("");
            });
        }
        if (testButton != null) {
            testButton.setOnClickListener(v -> testConnection());
        }
        if (toggleReadOnlyButton != null) {
            toggleReadOnlyButton.setOnClickListener(v -> toggleInputLock());
        }

        Button shutdownButton = findViewById(R.id.shutdown_button);
        if (shutdownButton != null) {
            shutdownButton.setOnClickListener(v -> sendSystemCommand("shutdown"));
        }

        Button resetBluetoothButton = findViewById(R.id.reset_bluetooth_button);
        if (resetBluetoothButton != null) {
            resetBluetoothButton.setOnClickListener(v -> sendSystemCommand("reset_bluetooth"));
        }

        Button restartProgramButton = findViewById(R.id.restart_program_button);
        if (restartProgramButton != null) {
            restartProgramButton.setOnClickListener(v -> sendSystemCommand("restart_program"));
        }

        Button pairBluetoothButton = findViewById(R.id.pair_bluetooth_button);
        if (pairBluetoothButton != null) {
            pairBluetoothButton.setOnClickListener(v -> sendSystemCommand("pair_bluetooth"));
        }
    }

    private void toggleInputLock() {
        isInputEnabled = !isInputEnabled;
        if (commandInput != null) commandInput.setEnabled(isInputEnabled);
        if (sendButton != null) sendButton.setEnabled(isInputEnabled);
        if (toggleReadOnlyButton != null) {
            toggleReadOnlyButton.setText(isInputEnabled ? "鎖定輸入" : "解除唯讀");
        }
        if (isInputEnabled) {
            appendToNewTerminal("⌨️ 命令輸入已啟用。\n");
        } else {
            appendToNewTerminal("🔒 命令輸入已鎖定。\n");
        }
    }

    private void checkConnectionStatus() {
        Log.d(TAG, "checkConnectionStatus: 開始檢查 USB 連接狀態...");
        boolean isConnected = usbManager.isConnected();

        if (!isConnected) {
            Log.w(TAG, "checkConnectionStatus: 連接不存在或已斷開。");
            appendToNewTerminal("❌ USB網路共享未連接\n");
        } else {
            Log.d(TAG, "checkConnectionStatus: 連接正常。");
            appendToNewTerminal("✅ USB網路共享已連接\n");
            appendToNewTerminal("  設備: " + usbManager.getConnectedDeviceName() + "\n");
            appendToNewTerminal("  狀態: " + usbManager.getConnectionStatus() + "\n\n");
        }

        if (commandInput != null) commandInput.setEnabled(isInputEnabled && isConnected);
        if (sendButton != null) sendButton.setEnabled(isInputEnabled && isConnected);
        if (testButton != null) testButton.setEnabled(isConnected);
        if (toggleReadOnlyButton != null) toggleReadOnlyButton.setEnabled(isConnected);
    }

    private void sendCommand() {
        if (commandInput == null) return;
        String command = commandInput.getText().toString().trim();
        if (command.isEmpty()) {
            Toast.makeText(this, "請輸入命令", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!usbManager.isConnected()) {
            Toast.makeText(this, "USB網路共享未連接", Toast.LENGTH_SHORT).show();
            return;
        }

        appendToNewTerminal("📤 發送: " + command + "\n");

        String jsonCommand = command;
        if (!command.startsWith("{")) {
            jsonCommand = "{\"type\":\"shell\",\"command\":\"" + command + "\",\"id\":\"cmd_" + System.currentTimeMillis() + "\"}";
        }

        usbManager.sendUsbCommand(jsonCommand);
        commandInput.setText("");

        new Thread(() -> {
            String response = usbManager.receiveUsbResponse();
            mainHandler.post(() -> {
                if (response != null && !response.isEmpty()) {
                    displayResponse(response);
                } else {
                    appendToNewTerminal("⚠️ 未收到回應 (連接可能中斷)\n");
                    checkConnectionStatus();
                }
            });
        }).start();
    }

    private void testConnection() {
        if (!usbManager.isConnected()) {
            Toast.makeText(this, "USB網路共享未連接，無法測試", Toast.LENGTH_SHORT).show();
            return;
        }
        appendToNewTerminal("🧪 正在測試連接...\n");

        String testCommand = "{\"type\":\"test\",\"command\":\"echo 'Success'\",\"id\":\"test_connection\"}";
        usbManager.sendUsbCommand(testCommand);

        new Thread(() -> {
            String response = usbManager.receiveUsbResponse();
            mainHandler.post(() -> {
                if (response != null && !response.isEmpty()) {
                    displayResponse(response);
                } else {
                    appendToNewTerminal("⚠️ 測試失敗：未收到回應\n");
                    checkConnectionStatus();
                }
            });
        }).start();
    }

    private void sendSystemCommand(String command) {
        if (!usbManager.isConnected()) {
            Toast.makeText(this, "USB網路共享未連接", Toast.LENGTH_SHORT).show();
            return;
        }

        String systemCommand = "";
        switch (command) {
            case "shutdown":
                systemCommand = "{\"type\":\"shell\",\"command\":\"sudo shutdown -h now\",\"id\":\"system_shutdown\"}";
                break;
            case "reset_bluetooth":
                systemCommand = "{\"type\":\"shell\",\"command\":\"sudo systemctl restart bluetooth\",\"id\":\"reset_bluetooth\"}";
                break;
            case "restart_program":
                systemCommand = "{\"type\":\"shell\",\"command\":\"sudo systemctl restart cube-solver\",\"id\":\"restart_program\"}";
                break;
            case "pair_bluetooth":
                systemCommand = "{\"type\":\"shell\",\"command\":\"bluetoothctl scan on\",\"id\":\"pair_bluetooth\"}";
                break;
        }

        if (!systemCommand.isEmpty()) {
            appendToNewTerminal("📤 發送系統命令: " + command + "\n");
            usbManager.sendUsbCommand(systemCommand);
        }
    }

    private void displayResponse(String response) {
        try {
            JSONObject jsonResponse = new JSONObject(response.trim());
            StringBuilder sb = new StringBuilder();
            sb.append("📥 收到回應:\n");
            sb.append("  ID: ").append(jsonResponse.optString("id", "N/A")).append("\n");
            sb.append("  成功: ").append(jsonResponse.optBoolean("success")).append("\n");

            String output = jsonResponse.optString("output", "");
            if (!output.isEmpty()) {
                sb.append("  輸出:\n").append(output).append("\n");
            }
            String error = jsonResponse.optString("error", "");
            if (!error.isEmpty()) {
                sb.append("  錯誤:\n").append(error).append("\n");
            }
            sb.append("---\n");
            appendToNewTerminal(sb.toString());

        } catch (JSONException e) {
            appendToNewTerminal("📥 收到非JSON格式回應:\n" + response + "\n---\n");
        }
    }

    private void appendToMainTerminal(final String text) {
        if (mainTerminalText == null) return;
        mainHandler.post(() -> {
            mainTerminalText.append(text);
            if (mainTerminalScrollView != null) {
                mainTerminalScrollView.post(() -> mainTerminalScrollView.fullScroll(View.FOCUS_DOWN));
            }
        });
    }

    private void appendToNewTerminal(final String text) {
        if (newTerminalText == null) return;
        mainHandler.post(() -> {
            newTerminalText.append(text);
            if (newTerminalScrollView != null) {
                newTerminalScrollView.post(() -> newTerminalScrollView.fullScroll(View.FOCUS_DOWN));
            }
        });
    }
}