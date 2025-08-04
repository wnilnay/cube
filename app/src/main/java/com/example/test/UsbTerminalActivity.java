package com.example.test;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.Looper;
import android.text.InputType;
import android.util.Log;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

public class UsbTerminalActivity extends AppCompatActivity {
    private static final String TAG = "UsbTerminalActivity";

    private EditText commandInput;
    private TextView mainTerminalText;
    private TextView commandTerminalText;
    private ScrollView mainTerminalScrollView;
    private ScrollView commandTerminalScrollView;
    private Button sendButton;
    private Button toggleReadOnlyButton;
    private Button pairBluetoothButton;
    private Button restartProgramButton;

    private UsbConnectionManager usbManager;
    private Handler mainHandler;
    private boolean isInputEnabled = false;
    private volatile boolean isListening = false;
    private Thread listeningThread;
    private final StringBuilder mainTerminalBuffer = new StringBuilder();
    private final StringBuilder commandTerminalBuffer = new StringBuilder();
    private Runnable mainTerminalUpdater;
    private Runnable commandTerminalUpdater;
    private boolean isResettingBluetooth = false;
    private boolean isPairingModeActive = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_usb_terminal);
        Log.d(TAG, "onCreate: Activity 正在創建。");

        initViews();
        usbManager = UsbConnectionManager.getInstance(this);
        mainHandler = new Handler(Looper.getMainLooper());

        initUpdateRunnable();

        setupButtonListeners();
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.d(TAG, "onResume: Activity 恢復前景。");
        checkConnectionStatus();
        if (usbManager.isConnected()) {
            startListening();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.d(TAG, "onPause: Activity 進入背景。");
        stopListening();
    }
    @Override
    public void onBackPressed() {
        // 如果正在配對模式中，則禁用返回鍵，並給出提示
        if (isPairingModeActive) {
            Toast.makeText(this, "正在進行藍牙配對，請稍候...", Toast.LENGTH_SHORT).show();
        } else {
            super.onBackPressed();
        }
    }


    private void initViews() {
        Log.d(TAG, "initViews: 開始初始化 UI 元件。");
        mainTerminalText = findViewById(R.id.main_terminal_text);
        commandTerminalText = findViewById(R.id.command_terminal_text);
        mainTerminalScrollView = findViewById(R.id.main_terminal_scroll_view);
        commandTerminalScrollView = findViewById(R.id.command_terminal_scroll_view);
        commandInput = findViewById(R.id.command_input);
        sendButton = findViewById(R.id.send_command_button);
        toggleReadOnlyButton = findViewById(R.id.toggle_readonly_button);
        pairBluetoothButton = findViewById(R.id.pair_bluetooth_button);
        restartProgramButton = findViewById(R.id.restart_program_button);
        Log.d(TAG, "initViews: UI 元件初始化完畢。");
    }

    private void setupButtonListeners() {
        sendButton.setOnClickListener(v -> sendCommand());
        commandInput.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEND) {
                sendCommand();
                return true;
            }
            return false;
        });

        toggleReadOnlyButton.setOnClickListener(v -> toggleInputLock());

        // [修改] 將清空功能綁定到網格中的按鈕
        findViewById(R.id.clear_terminal_button).setOnClickListener(v -> {
            if (commandTerminalText != null) {
                commandTerminalText.setText("");
            }
        });

        // 系統命令按鈕
        findViewById(R.id.shutdown_button).setOnClickListener(v -> {
            // 先檢查連接狀態
            if (!usbManager.isConnected()) {
                Toast.makeText(this, "USB未連接，無法執行操作", Toast.LENGTH_SHORT).show();
                return;
            }
            // 建立並顯示確認對話框
            new AlertDialog.Builder(this)
                    .setTitle("確認關機")
                    .setMessage("您確定要將樹莓派關機嗎？\n此操作無法復原。")
                    .setPositiveButton("確定關機", (dialog, which) -> {
                        // 當使用者點擊「確定關機」後，才真正發送命令
                        sendSystemCommand("shutdown", "sudo shutdown -h now");
                    })
                    .setNegativeButton("取消", null) // 點擊「取消」則什麼都不做，對話框自動關閉
                    .setIcon(android.R.drawable.ic_dialog_alert) // 加上一個警告圖示
                    .show();
        });
        findViewById(R.id.reset_bluetooth_button).setOnClickListener(v ->{
            // 先檢查連接狀態
            if (!usbManager.isConnected()) {
                Toast.makeText(this, "USB未連接，無法執行操作", Toast.LENGTH_SHORT).show();
                return;
            }
            if (isResettingBluetooth) {
                Toast.makeText(this, "正在重置藍牙，請稍等...", Toast.LENGTH_SHORT).show();
                return;
            }
            isResettingBluetooth = true;
            sendSystemCommand("reset_bluetooth", "sudo systemctl restart bluetooth.service");
        });

        restartProgramButton.setOnClickListener(v ->{
            // 先檢查連接狀態
            if (!usbManager.isConnected()) {
                Toast.makeText(this, "USB未連接，無法執行操作", Toast.LENGTH_SHORT).show();
                return;
            }
            // 建立並顯示確認對話框
            new AlertDialog.Builder(this)
                    .setTitle("確認重啟")
                    .setMessage("您確定要重啟主程式嗎？\n此操作會造成USB連接斷開，需退出重新連接。")
                    .setPositiveButton("確定重啟", (dialog, which) -> {
                        sendSystemCommand("restart_program",
                                "");
                    })
                    .setNegativeButton("取消", null) // 點擊「取消」則什麼都不做，對話框自動關閉
                    .setIcon(android.R.drawable.ic_dialog_alert) // 加上一個警告圖示
                    .show();
        });

        pairBluetoothButton.setOnClickListener(v -> {
            if (!usbManager.isConnected()) {
                Toast.makeText(this, "USB未連接，無法執行操作", Toast.LENGTH_SHORT).show();
                return;
            }
            if (isBluetoothConnected()) {
                Toast.makeText(this, "您已連線到藍牙，無需配對。", Toast.LENGTH_SHORT).show();
                return;
            }

            String myDeviceName = getBluetoothDeviceName();
            if (myDeviceName == null || myDeviceName.isEmpty()) {
                // 如果获取失败，弹出一个明确的错误提示框
                new AlertDialog.Builder(this)
                        .setTitle("错误：无法获取设备名称")
                        .setMessage("无法获取本机的蓝牙名称。\n请检查：\n1. App 是否已被授予『邻近设备』权限。\n2. 您是否在手机的系统设定中为蓝牙设定了名称。")
                        .setPositiveButton("好的", null)
                        .show();
                return;
            }

            new AlertDialog.Builder(this)
                    .setTitle("開始配對")
                    .setMessage("即將讓樹莓派與您的手機 '" + myDeviceName + "' 配對。\n\n點擊「開始」後，請留意手機螢幕上彈出的系統配對請求。")
                    .setPositiveButton("開始", (dialog, which) -> {
                        String command = "{\"id\":\"system_initiate_pairing\",\"name\":\"" + myDeviceName + "\"}";
                        appendToCommandTerminal("📤 發送配對請求，目標: '" + myDeviceName + "'");
                        usbManager.sendUsbCommand(command);
                    })
                    .setNegativeButton("取消", null)
                    .show();
        });
    }
    /**
     * [修改] 獲取本機藍牙名稱的輔助方法。
     * @return 返回裝置名稱字串，如果失敗則返回 null。
     */
    private String getBluetoothDeviceName() {
        BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (bluetoothAdapter == null) {
            Log.e(TAG, "裝置不支援藍牙");
            return null;
        }

        // 從 Android 12 (API 31) 開始，獲取名稱也需要 BLUETOOTH_CONNECT 權限
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT)
                    != PackageManager.PERMISSION_GRANTED) {
                Log.e(TAG, "缺少 BLUETOOTH_CONNECT 權限");
                // 這裡應該觸發權限請求，或者直接返回 null 讓呼叫者處理
                // 為了簡潔，我們假設權限已在 Activity 啟動時請求
                return null;
            }
        }

        try {
            return bluetoothAdapter.getName();
        } catch (Exception e) {
            Log.e(TAG, "獲取藍牙名稱時發生例外", e);
            return null;
        }
    }

    private void toggleInputLock() {
        isInputEnabled = !isInputEnabled;
        commandInput.setEnabled(isInputEnabled);
        sendButton.setEnabled(isInputEnabled);
        toggleReadOnlyButton.setText(isInputEnabled ? "鎖定輸入" : "解除唯讀");
        appendToCommandTerminal(isInputEnabled ? "⌨️ 命令輸入已啟用。" : "🔒 命令輸入已鎖定。");
    }

    private void checkConnectionStatus() {
        Log.d(TAG, "checkConnectionStatus: 檢查連接狀態...");
        boolean isConnected = usbManager.isConnected();
        if (!isConnected) {
            appendToCommandTerminal("❌ USB網路共享未連接");
        } else {
            // 只在命令終端顯示簡短連接成功提示
            appendToCommandTerminal("✅ USB網路共享已連接");
        }
        // 更新所有依賴連接狀態的按鈕
        commandInput.setEnabled(isInputEnabled && isConnected);
        sendButton.setEnabled(isInputEnabled && isConnected);
        toggleReadOnlyButton.setEnabled(isConnected);
        findViewById(R.id.shutdown_button).setEnabled(isConnected);
        findViewById(R.id.reset_bluetooth_button).setEnabled(isConnected);
        findViewById(R.id.restart_program_button).setEnabled(isConnected);
        findViewById(R.id.pair_bluetooth_button).setEnabled(isConnected);
        findViewById(R.id.clear_terminal_button).setEnabled(isConnected);

    }

    private void sendCommand() {
        final String command = commandInput.getText().toString().trim();
        if (command.isEmpty() || !usbManager.isConnected()) {
            Toast.makeText(this, command.isEmpty() ? "請輸入命令" : "USB未連接", Toast.LENGTH_SHORT).show();
            return;
        }

        // 檢查是否包含 "bluetooth" 關鍵字 (忽略大小寫)
        if (command.toLowerCase().contains("bluetooth")) {
            // 如果是藍牙相關命令，則顯示確認對話框
            new AlertDialog.Builder(this)
                    .setTitle("警告：藍牙相關命令")
                    .setMessage("直接操作藍牙服務 (如 bluetoothctl) 可能會干擾主程式的正常連線，建議使用上方的『重設藍牙』按鈕。" +
                            "\n\n您確定要繼續發送原始命令嗎？")
                    .setPositiveButton("確定發送", (dialog, which) -> {
                        // *** 當使用者點擊「確定」後，才執行真正的發送邏輯 ***
                        executeCommand(command);
                    })
                    .setNegativeButton("取消", (dialog, which) -> {
                        // 當使用者點擊「取消」，我們什麼都不做，只是讓對話框關閉
                        // 可以在這裡給使用者一個提示
                        appendToCommandTerminal("🚫 命令已取消: " + command);
                        commandInput.setText("");
                    })
                    .setIcon(android.R.drawable.ic_dialog_alert)
                    .show();
        }
        else {
            // 如果不是藍牙相關命令，則直接執行
            executeCommand(command);
        }
    }

    /**
     * 新增一個輔助方法，專門用來執行命令的發送。
     * 這樣可以避免在 AlertDialog 和 else 區塊中寫重複的程式碼。
     * @param commandToExecute 要執行的命令字串
     */
    private void executeCommand(String commandToExecute) {
        // 1. 在命令終端顯示發送的日誌
        appendToCommandTerminal("📤 發送: " + commandToExecute);

        // 2. 組裝 JSON
        String jsonCommand = commandToExecute.startsWith("{") ? commandToExecute :
                "{\"type\":\"shell\",\"command\":\"" + commandToExecute.replace("\"", "\\\"")
                        + "\",\"id\":\"cmd_" + System.currentTimeMillis() + "\"}";

        // 3. 透過 USB 管理器發送
        usbManager.sendUsbCommand(jsonCommand);

        // 4. 清空輸入框
        commandInput.setText("");
    }

    private void sendSystemCommand(String commandName, String actualCommand) {
        if (!usbManager.isConnected()) {
            Toast.makeText(this, "USB未連接", Toast.LENGTH_SHORT).show();
            return;
        }
        String systemCommand = "{\"type\":\"shell\",\"command\":\"" + actualCommand + "\",\"id\":\"system_"
                + commandName + "\"}";
        appendToCommandTerminal("📤 發送系統命令: " + commandName);
        usbManager.sendUsbCommand(systemCommand);
    }

    private void displayResponse(String response) {
        try {
            JSONObject jsonResponse = new JSONObject(response.trim());

            String id = jsonResponse.optString("id", "N/A");
            if(id.equals("system_reset_bluetooth")){
                isResettingBluetooth = false;
            }

            StringBuilder sb = new StringBuilder();
            sb.append("📥 收到回應:\n");
            sb.append("  ID: ").append(id).append("\n");
            sb.append("  成功: ").append(jsonResponse.optBoolean("success")).append("\n");

            String output = jsonResponse.optString("output", "").trim();
            if (!output.isEmpty()) sb.append("  輸出:\n").append(output).append("\n");

            String error = jsonResponse.optString("error", "").trim();
            if (!error.isEmpty()) sb.append("  錯誤:\n").append(error).append("\n");

            sb.append("---");
            appendToCommandTerminal(sb.toString());
        } catch (JSONException e) {
            appendToCommandTerminal("📥 收到非JSON格式回應:\n" + response + "\n---");
        }
    }

    private void appendToMainTerminal(final String text) {
        if (mainTerminalText == null || text == null) return;

        // 將新文字加入緩衝區 (需要同步化以保證執行緒安全)
        synchronized (mainTerminalBuffer) {
            mainTerminalBuffer.append(text).append("\n");
        }
        // 取消之前任何待執行的更新任務
        mainHandler.removeCallbacks(mainTerminalUpdater);
        // 安排一個新的更新任務在 50 毫秒後執行。
        // 如果在這 50 毫秒內有新訊息進來，這個任務會被再次取消並重新安排。
        mainHandler.postDelayed(mainTerminalUpdater, 50); // 50ms 是一個不錯的防抖延遲
    }

    private void appendToCommandTerminal(final String text) {
        if (commandTerminalText == null || text == null) return;

        synchronized (commandTerminalBuffer) {
            commandTerminalBuffer.append(text).append("\n");
        }
        mainHandler.removeCallbacks(commandTerminalUpdater);
        mainHandler.postDelayed(commandTerminalUpdater, 50);
    }

    private void startListening() {
        if (isListening || !usbManager.isConnected()) return;
        isListening = true;
        listeningThread = new Thread(() -> {
            Log.d(TAG, "🎧 監聽執行緒已啟動。");
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(usbManager
                    .getSocketInputStream(), "UTF-8"))) {
                while (isListening && !Thread.currentThread().isInterrupted()) {
                    String line = reader.readLine();
                    if (line == null) {
                        Log.w(TAG, "監聽執行緒讀取到 null，連接已中斷。");
                        break;
                    }
                    processReceivedData(line);
                }
            } catch (IOException e) {
                if (isListening) Log.e(TAG, "監聽執行緒 IO 錯誤: ", e);
            } finally {
                Log.d(TAG, "🎧 監聽執行緒已停止。");
                isListening = false;
                mainHandler.post(this::checkConnectionStatus);
            }
        });
        listeningThread.start();
    }

    private void stopListening() {
        isListening = false;
        if (listeningThread != null) {
            listeningThread.interrupt();
            listeningThread = null;
        }
    }

    private void processReceivedData(final String data) {
        if (data == null || data.isEmpty()) return;
        mainHandler.post(() -> {
            try {
                JSONObject json = new JSONObject(data);
                String type = json.optString("type", "unknown");
                String streamData;

                switch (type) {
                    case "TerminalStream":
                        streamData = json.optString("data", "");
                        if (streamData.endsWith("\n")) {
                            streamData = streamData.substring(0, streamData.length() - 1);
                        }
                        appendToMainTerminal(streamData);
                        break;
                    case "USBResult":
                        displayResponse(data);
                        break;
                    case "PinRequest":
                        String pin = json.optString("pin", "------");
                        showPinConfirmationDialog(pin);
                        break;

                    // --- [新增] 處理最終配對結果 ---
                    case "PairingResult":
                        boolean success = json.optBoolean("success", false);
                        String message = json.optString("message", "未知結果");
                        new AlertDialog.Builder(this)
                                .setTitle(success ? "配對成功" : "配對失敗")
                                .setMessage(message)
                                .setPositiveButton("好的", null)
                                .show();
                        break;
                    default:
                        appendToCommandTerminal("📥 收到未知類型的數據:\n" + data);
                        break;
                }
            } catch (JSONException e) {
                appendToCommandTerminal("📥 收到非JSON數據:\n" + data);
            }
        });
    }
    /**
     * [新增] 顯示一個對話框讓使用者輸入 PIN 碼。
     * @param pinToConfirm 從後端收到的 PIN 碼，用於提示
     */
    private void showPinConfirmationDialog(final String pinToConfirm) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("請確認 PIN 碼");
        builder.setMessage("請核對您手機系統彈出的配對請求中的 PIN 碼，並在下方輸入以確認。\n\n提示 PIN 碼: " + pinToConfirm);

        final EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_CLASS_NUMBER);
        input.setHint("輸入 6 位 PIN 碼");
        builder.setView(input);

        builder.setPositiveButton("確認配對", (dialog, which) -> {
            String userInputPin = input.getText().toString().trim();
            // 可以在這裡做更嚴格的驗證，例如檢查長度
            if (userInputPin.isEmpty()) {
                Toast.makeText(this, "PIN 碼不能為空", Toast.LENGTH_SHORT).show();
                return;
            }
            String command = "{\"id\":\"system_confirm_pairing\",\"pin\":\"" + userInputPin + "\"}";
            appendToCommandTerminal("📤 發送 PIN 碼確認");
            usbManager.sendUsbCommand(command);
        });
        builder.setNegativeButton("取消", null);
        builder.setCancelable(false);
        builder.show();
    }
    private void initUpdateRunnable() {
        mainTerminalUpdater = () -> {
            // 如果緩衝區沒內容，就沒必要更新
            if (mainTerminalBuffer.length() == 0) return;

            // 在更新前檢查是否在底部，這是捕捉使用者的意圖
            View child = mainTerminalScrollView.getChildAt(0);
            boolean isAtBottom = (child.getBottom() <= (mainTerminalScrollView.getHeight() +
                    mainTerminalScrollView.getScrollY() + 20));

            // 一次性將緩衝區內所有文字添加到 TextView
            mainTerminalText.append(mainTerminalBuffer);
            // 清空緩衝區，為下一批次做準備
            mainTerminalBuffer.setLength(0);

            // 只有當使用者意圖是在底部時，才在佈局完成後滾動
            if (isAtBottom) {
                mainTerminalScrollView.getViewTreeObserver().addOnGlobalLayoutListener(
                        new android.view.ViewTreeObserver.OnGlobalLayoutListener() {
                    @Override
                    public void onGlobalLayout() {
                        mainTerminalScrollView.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                        mainTerminalScrollView.fullScroll(View.FOCUS_DOWN);
                    }
                });
            }
        };

        commandTerminalUpdater = () -> {
            if (commandTerminalBuffer.length() == 0) return;

            View child = commandTerminalScrollView.getChildAt(0);
            boolean isAtBottom = (child.getBottom() <= (commandTerminalScrollView.getHeight() +
                    commandTerminalScrollView.getScrollY() + 20));

            commandTerminalText.append(commandTerminalBuffer);
            commandTerminalBuffer.setLength(0);

            if (isAtBottom) {
                commandTerminalScrollView.getViewTreeObserver().addOnGlobalLayoutListener
                        (new android.view.ViewTreeObserver.OnGlobalLayoutListener() {
                    @Override
                    public void onGlobalLayout() {
                        commandTerminalScrollView.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                        commandTerminalScrollView.fullScroll(View.FOCUS_DOWN);
                    }
                });
            }
        };
    }
    private boolean isBluetoothConnected() {
        return BluetoothSocketManager.getSocket() != null && BluetoothSocketManager.getSocket().isConnected();
    }
    private void startPairingLockdown() {
        isPairingModeActive = true;
        setLockdownUI(true); // 立即鎖定UI

        new CountDownTimer(60000, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                int secondsRemaining = (int) (millisUntilFinished / 1000);
                // 更新按鈕文字以顯示倒數計時
                pairBluetoothButton.setText("配對鎖定中 (" + secondsRemaining + "s)");
            }

            @Override
            public void onFinish() {
                // 這個 onFinish 會在60秒到期時執行
                if(isPairingModeActive) { // 再次檢查，避免重複執行
                    isPairingModeActive = false;
                    setLockdownUI(false); // 解除UI鎖定
                    appendToCommandTerminal("ℹ️ 藍牙配對窗口已超時結束。");
                }
            }
        }.start();
    }

    /**
     * 統一管理鎖定期間的 UI 狀態。
     * @param isLocked true 為鎖定，false 為解鎖
     */
    private void setLockdownUI(boolean isLocked) {
        // 鎖定或解鎖所有相關按鈕和輸入
        pairBluetoothButton.setEnabled(!isLocked);
        restartProgramButton.setEnabled(!isLocked);
        sendButton.setEnabled(!isLocked);
        commandInput.setEnabled(!isLocked);
        toggleReadOnlyButton.setEnabled(!isLocked);

        if (isLocked) {
            // 進入鎖定狀態時，可以改變按鈕樣式以提供更清晰的視覺回饋
            toggleReadOnlyButton.setText("功能鎖定");
        } else {
            // 解除鎖定時，恢復按鈕的原始文字
            pairBluetoothButton.setText("配對藍牙");
            // 恢復唯讀按鈕的原始狀態 (基於 isInputEnabled 變數)
            toggleReadOnlyButton.setText(isInputEnabled ? "鎖定輸入" : "解除唯讀");
        }
    }
}