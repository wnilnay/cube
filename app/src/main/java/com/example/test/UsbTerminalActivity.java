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
import android.view.ViewTreeObserver;
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

    // --- [修改] ---
    // isPairingModeActive 用於追蹤當前是否處於配對鎖定狀態
    // pairingCountdownTimer 用於處理配對超時
    private volatile boolean isPairingModeActive = false;
    private CountDownTimer pairingCountdownTimer;

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
        // 如果不在配對模式下，才檢查連線狀態和啟動監聽
        if (!isPairingModeActive) {
            checkConnectionStatus();
            if (usbManager.isConnected()) {
                startListening();
            }
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.d(TAG, "onPause: Activity 進入背景。");
        // 只有在非配對模式下離開才停止監聽
        if (!isPairingModeActive) {
            stopListening();
        }
    }

    /**
     * 當此 Activity 不再可見時呼叫。
     * 這是清理網路連接的最佳時機。
     */
    @Override
    protected void onStop() {
        super.onStop();
        Log.d(TAG, "onStop: Activity 停止，正在斷開 USB 連接...");
        // 明確地告訴 UsbConnectionManager 斷開並清理所有資源
        UsbConnectionManager.getInstance(this).disconnectNetwork();
    }

    @Override
    public void onBackPressed() {
        // --- [修改] ---
        // 使用 isPairingModeActive 判斷是否鎖定返回鍵
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

        findViewById(R.id.clear_terminal_button).setOnClickListener(v -> {
            if (commandTerminalText != null) {
                commandTerminalText.setText("");
            }
        });

        findViewById(R.id.shutdown_button).setOnClickListener(v -> {
            if (!usbManager.isConnected()) {
                Toast.makeText(this, "USB未連接，無法執行操作", Toast.LENGTH_SHORT).show();
                return;
            }
            new AlertDialog.Builder(this)
                    .setTitle("確認關機")
                    .setMessage("您確定要將樹莓派關機嗎？\n此操作無法復原。")
                    .setPositiveButton("確定關機", (dialog, which) -> {
                        sendSystemCommand("shutdown", "sudo shutdown -h now");
                    })
                    .setNegativeButton("取消", null)
                    .setIcon(android.R.drawable.ic_dialog_alert)
                    .show();
        });
        findViewById(R.id.reset_bluetooth_button).setOnClickListener(v ->{
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
            if (!usbManager.isConnected()) {
                Toast.makeText(this, "USB未連接，無法執行操作", Toast.LENGTH_SHORT).show();
                return;
            }
            new AlertDialog.Builder(this)
                    .setTitle("確認重啟")
                    .setMessage("您確定要重啟主程式嗎？\n此操作會造成USB連接斷開，需退出重新連接。")
                    .setPositiveButton("確定重啟", (dialog, which) -> {
                        sendSystemCommand("restart_program",
                                "");
                    })
                    .setNegativeButton("取消", null)
                    .setIcon(android.R.drawable.ic_dialog_alert)
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
                new AlertDialog.Builder(this)
                        .setTitle("錯誤：無法獲取設備名稱")
                        .setMessage("無法獲取本機的藍牙名稱。\n請檢查：\n1. App 是否已被授予「鄰近設配」權限。\n" +
                                "2. 您是否在手機的系統設定中為藍牙設定了名稱。")
                        .setPositiveButton("好的", null)
                        .show();
                return;
            }

            // --- [修改] ---
            // 修改提示文字，並在點擊「開始」後呼叫 startPairingLockdown()
            new AlertDialog.Builder(this)
                    .setTitle("開始配對")
                    .setMessage("即將讓樹莓派與您的手機 '" + myDeviceName + "' 配對。\n\n點擊「開始」後，" +
                            "請留意手機螢幕上彈出的系統配對請求，並在 60 秒內完成確認。\n\n" +
                            "若是在藍牙配對的彈窗中，取消、點擊空白處或是不配對，都會導致配對失敗。\n(即使回傳結果表示成功，也仍然沒有成功配對。)")
                    .setPositiveButton("開始", (dialog, which) -> {
                        String command = "{\"id\":\"system_initiate_pairing\",\"name\":\"" + myDeviceName + "\"}";
                        appendToCommandTerminal("📤 發送配對請求，目標: '" + myDeviceName + "'");
                        usbManager.sendUsbCommand(command);
                        // 開始 UI 鎖定和倒數計時
                        startPairingLockdown();
                    })
                    .setNegativeButton("取消", null)
                    .show();
        });
    }

    private String getBluetoothDeviceName() {
        BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (bluetoothAdapter == null) {
            Log.e(TAG, "裝置不支援藍牙");
            return null;
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT)
                    != PackageManager.PERMISSION_GRANTED) {
                Log.e(TAG, "缺少 BLUETOOTH_CONNECT 權限");
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
            appendToCommandTerminal("✅ USB網路共享已連接");
        }

        // --- [修改] ---
        // 如果不在配對模式下，才更新UI；否則UI由 setLockdownUI 控制
        if (!isPairingModeActive) {
            commandInput.setEnabled(isInputEnabled && isConnected);
            sendButton.setEnabled(isInputEnabled && isConnected);
            toggleReadOnlyButton.setEnabled(isConnected);
            findViewById(R.id.shutdown_button).setEnabled(isConnected);
            findViewById(R.id.reset_bluetooth_button).setEnabled(isConnected);
            findViewById(R.id.restart_program_button).setEnabled(isConnected);
            findViewById(R.id.pair_bluetooth_button).setEnabled(isConnected);
            findViewById(R.id.clear_terminal_button).setEnabled(isConnected);
        }
    }

    private void sendCommand() {
        final String command = commandInput.getText().toString().trim();
        if (command.isEmpty() || !usbManager.isConnected()) {
            Toast.makeText(this, command.isEmpty() ? "請輸入命令" : "USB未連接", Toast.LENGTH_SHORT).show();
            return;
        }
        if (command.toLowerCase().contains("bluetooth")) {
            new AlertDialog.Builder(this)
                    .setTitle("警告：藍牙相關命令")
                    .setMessage("直接操作藍牙服務 (如 bluetoothctl) 可能會干擾主程式的正常連線，建議使用上方的『重設藍牙』按鈕。" +
                            "\n\n您確定要繼續發送原始命令嗎？")
                    .setPositiveButton("確定發送", (dialog, which) -> {
                        executeCommand(command);
                    })
                    .setNegativeButton("取消", (dialog, which) -> {
                        appendToCommandTerminal("🚫 命令已取消: " + command);
                        commandInput.setText("");
                    })
                    .setIcon(android.R.drawable.ic_dialog_alert)
                    .show();
        }
        else {
            executeCommand(command);
        }
    }

    private void executeCommand(String commandToExecute) {
        appendToCommandTerminal("📤 發送: " + commandToExecute);
        String jsonCommand = commandToExecute.startsWith("{") ? commandToExecute :
                "{\"type\":\"shell\",\"command\":\"" + commandToExecute.replace("\"", "\\\"")
                        + "\",\"id\":\"cmd_" + System.currentTimeMillis() + "\"}";
        usbManager.sendUsbCommand(jsonCommand);
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
        synchronized (mainTerminalBuffer) {
            mainTerminalBuffer.append(text).append("\n");
        }
        mainHandler.removeCallbacks(mainTerminalUpdater);
        mainHandler.postDelayed(mainTerminalUpdater, 50);
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
                        Log.w(TAG, "監聽執行緒讀取到 null，連接可能已中斷。");
                        break;
                    }
                    processReceivedData(line);
                }
            } catch (IOException e) {
                if (isListening) Log.e(TAG, "監聽執行緒 IO 錯誤: ", e);
            } finally {
                Log.d(TAG, "🎧 監聽執行緒已停止。");
                isListening = false;
                // --- [修改] ---
                // 連線中斷後，如果不在配對模式，才更新UI
                // 如果正在配對，則讓倒數計時器來處理超時狀況
                if (!isPairingModeActive) {
                    mainHandler.post(this::checkConnectionStatus);
                }
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
                    case "PairingResult":
                        // 無論成功或失敗，都應先停止 UI 鎖定和倒數計時
                        stopPairingLockdown();

                        boolean success = json.optBoolean("success", false);
                        String message = json.optString("message", "");
                        String error = json.optString("error", "未知結果");
                        String dialogMessage = success ? message : error;

                        new AlertDialog.Builder(this)
                                .setTitle(success ? "🎉 配對成功" : "😕 配對失敗")
                                .setMessage(dialogMessage)
                                // 對話框關閉後，重新檢查一次所有按鈕的狀態
                                .setPositiveButton("好的", (dialog, which) -> checkConnectionStatus())
                                .setCancelable(false) // 避免使用者在看到結果前誤觸關閉
                                .show();
                        break;
                    default:
                        appendToCommandTerminal("📥 收到未知類型的數據:\n" + data);
                        break;
                }
            } catch (JSONException e) {
                // 如果解析失敗，也嘗試解除鎖定，避免App卡死
                if(isPairingModeActive){
                    stopPairingLockdown();
                    new AlertDialog.Builder(this)
                            .setTitle("處理錯誤")
                            .setMessage("收到來自樹莓派的無效配對結果，配對流程已終止。")
                            .setPositiveButton("好的", null).show();
                }
                appendToCommandTerminal("📥 收到非JSON數據:\n" + data);
            }
        });
    }

    private void showPinConfirmationDialog(final String pinToConfirm) {
        // 自動確認 PIN 碼
        String command = "{\"id\":\"system_confirm_pairing\",\"pin\":\"" + pinToConfirm + "\"}";
        appendToCommandTerminal("✅ 自動發送 PIN 碼 (" + pinToConfirm + ") 確認");
        usbManager.sendUsbCommand(command);
    }
    private void initUpdateRunnable() {
        mainTerminalUpdater = () -> {
            if (mainTerminalBuffer.length() == 0) return;

            // 1. 同樣，在添加文本前檢查用戶是否在底部
            final boolean shouldScroll = isUserAtBottom(mainTerminalScrollView);

            // 2. 添加文本
            synchronized (mainTerminalBuffer) {
                mainTerminalText.append(mainTerminalBuffer);
                mainTerminalBuffer.setLength(0);
            }

            // 3. 如果需要滾動，使用 post() 將滾動任務發送到消息隊列的末尾
            if (shouldScroll) {
                // 這個任務會在當前佈局計算完畢後執行，從而獲得正確的底部位置
                mainTerminalScrollView.post(() -> mainTerminalScrollView.fullScroll(View.FOCUS_DOWN));
            }
        };

        // 對 commandTerminalUpdater 應用完全相同的邏輯
        commandTerminalUpdater = () -> {
            if (commandTerminalBuffer.length() == 0) return;

            final boolean shouldScroll = isUserAtBottom(commandTerminalScrollView);

            synchronized (commandTerminalBuffer) {
                commandTerminalText.append(commandTerminalBuffer);
                commandTerminalBuffer.setLength(0);
            }

            if (shouldScroll) {
                commandTerminalScrollView.post(() -> commandTerminalScrollView.fullScroll(View.FOCUS_DOWN));
            }
        };
    }

    // 輔助方法 isUserAtBottom 保持不變
    private boolean isUserAtBottom(ScrollView scrollView) {
        if (scrollView.getChildCount() == 0) {
            return true;
        }
        View view = scrollView.getChildAt(0);
        int diff = (view.getBottom() - (scrollView.getHeight() + scrollView.getScrollY()));
        // 您可以稍微放寬這個容錯值，例如 30 或 40，以應對不同的螢幕密度
        return diff <= 30;
    }

    private boolean isBluetoothConnected() {
        return BluetoothSocketManager.getSocket() != null && BluetoothSocketManager.getSocket().isConnected();
    }

    // --- [新增] ---
    /**
     * 開始配對鎖定：鎖定UI並啟動一個60秒的倒數計時器。
     */
    private void startPairingLockdown() {
        if (isPairingModeActive) return; // 如果已在鎖定模式，則不重複執行

        isPairingModeActive = true;
        setLockdownUI(true); // 立即鎖定UI

        pairingCountdownTimer = new CountDownTimer(60000, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                // 更新按鈕文字以顯示倒數計時
                int secondsRemaining = (int) (millisUntilFinished / 1000);
                if (pairBluetoothButton != null) {
                    pairBluetoothButton.setText("配對中 (" + secondsRemaining + "s)");
                }
            }

            @Override
            public void onFinish() {
                // 這個 onFinish 會在60秒到期時執行（即超時）
                if (isPairingModeActive) { // 再次檢查狀態，避免重複執行
                    appendToCommandTerminal("⚠️ 配對流程超時。");
                    stopPairingLockdown(); // 解除UI鎖定

                    // 顯示一個超時專用的對話框
                    new AlertDialog.Builder(UsbTerminalActivity.this)
                            .setTitle("配對超時")
                            .setMessage("未能在指定時間內收到配對結果。\n\n" +
                                    "可能原因：\n" +
                                    "1. USB 連線中斷。\n" +
                                    "2. 您未在手機上確認系統配對請求。\n" +
                                    "3. 樹莓派端發生錯誤。\n\n" +
                                    "請檢查手機藍牙設定，確認是否已配對成功。")
                            .setPositiveButton("好的", (dialog, which) -> {
                                // 對話框關閉後，重新檢查一次連線狀態
                                checkConnectionStatus();
                            })
                            .setCancelable(false)
                            .show();
                }
            }
        }.start();
    }

    // --- [新增] ---
    /**
     * 停止配對鎖定：取消倒數計時器並解鎖UI。
     */
    private void stopPairingLockdown() {
        if (!isPairingModeActive) return; // 如果不在鎖定模式，則不執行

        if (pairingCountdownTimer != null) {
            pairingCountdownTimer.cancel(); // 停止倒數計時
            pairingCountdownTimer = null;
        }
        isPairingModeActive = false;
        setLockdownUI(false); // 解鎖UI
        appendToCommandTerminal("ℹ️ 配對流程已結束。");
    }

    // --- [修改] ---
    /**
     * 統一管理鎖定期間的 UI 狀態。
     * @param isLocked true 為鎖定，false 為解鎖
     */
    private void setLockdownUI(boolean isLocked) {
        // 鎖定或解鎖所有相關按鈕和輸入
        findViewById(R.id.shutdown_button).setEnabled(!isLocked);
        findViewById(R.id.reset_bluetooth_button).setEnabled(!isLocked);
        findViewById(R.id.clear_terminal_button).setEnabled(!isLocked);
        restartProgramButton.setEnabled(!isLocked);
        sendButton.setEnabled(!isLocked && isInputEnabled); // sendButton還需考慮輸入是否啟用
        commandInput.setEnabled(!isLocked && isInputEnabled);
        toggleReadOnlyButton.setEnabled(!isLocked);
        pairBluetoothButton.setEnabled(!isLocked);

        if (isLocked) {
            // 進入鎖定狀態時，改變按鈕樣式以提供更清晰的視覺回饋
            toggleReadOnlyButton.setText("配對鎖定中");
            pairBluetoothButton.setText("配對中...");
        } else {
            // 解除鎖定時，恢復按鈕的原始文字和狀態
            pairBluetoothButton.setText("配對藍牙");
            toggleReadOnlyButton.setText(isInputEnabled ? "鎖定輸入" : "解除唯讀");
            // 重新整理一次所有按鈕的可用狀態
            checkConnectionStatus();
        }
    }
}