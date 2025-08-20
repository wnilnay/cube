package com.example.test;

import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import java.io.IOException;
import java.io.InputStream;
import java.util.Timer;
import java.util.TimerTask;

public class SettingFragment extends Fragment implements BluetoothDisconnectListener {
    private static final String TAG = "SettingFragment";
    private View view;
    private Button motorButton, coordinateButton, colorButton, testMotorButton, guideButton, bluetoothModeButton, usbTerminalButton;
    private static SettingFragment instance;
    private static boolean buttonsEnabled = true;

    // --- Dialog 相關成員變數 ---
    private AlertDialog progressDialog;
    private TextView progressDialogText;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        if (view == null) {
            instance = this;
            view = inflater.inflate(R.layout.fragment_setting, container, false);

            motorButton = view.findViewById(R.id.motor_button);
            coordinateButton = view.findViewById(R.id.coordinate_button);
            colorButton = view.findViewById(R.id.color_button);
            testMotorButton = view.findViewById(R.id.testMotor_button);
            guideButton = view.findViewById(R.id.guide_button);
            bluetoothModeButton = view.findViewById(R.id.bluetooth_mode_button);
            usbTerminalButton = view.findViewById(R.id.usb_terminal_button);

            setButtonsEnabled(buttonsEnabled);
            setupButtonListeners();
        }
        return view;
    }

    private void setupButtonListeners() {
        if (motorButton != null) {
            motorButton.setOnClickListener(v -> {
                if (!isBluetoothConnected()) {
                    showBluetoothCheckAlertDialog();
                } else {
                    new AlertDialog.Builder(requireContext())
                            .setTitle("是否確定更改設定？")
                            .setMessage("當您進入時，必須修改好設定，否則將無法正常使用\n是否確定進行修改？")
                            .setNegativeButton("取消", null)
                            .setPositiveButton("是", (dialogInterface, i) -> {
                                BluetoothSocketManager.sendString("SetMotorMode", "");
                                Intent intent = new Intent(getContext(), SetMotorActivity.class);
                                startActivity(intent);
                            })
                            .create()
                            .show();
                }
            });
        }

        if (testMotorButton != null) {
            testMotorButton.setOnClickListener(v -> {
                if (!isBluetoothConnected()) {
                    showBluetoothCheckAlertDialog();
                } else {
                    BluetoothSocketManager.sendString("TestMotorMode", "");
                    setButtonsEnabled(false);
                    MainFragment.setOkButtonEnabled(false);
                    Toast.makeText(requireContext(), "正在進行測試中...", Toast.LENGTH_LONG).show();
                    Timer timer = new Timer();
                    timer.schedule(new TimerTask() {
                        @Override
                        public void run() {
                            String[] data = BluetoothSocketManager.getDataString();
                            if (data != null && data.length > 0 && data[0].contains("EndTestMotorMode")) {
                                if (getActivity() != null) {
                                    getActivity().runOnUiThread(() -> {
                                        Toast.makeText(requireContext(), "測試結束", Toast.LENGTH_SHORT).show();
                                        setButtonsEnabled(true);
                                        MainFragment.setOkButtonEnabled(true);
                                        timer.cancel();
                                    });
                                }
                            }
                        }
                    }, 0, 100);
                }
            });
        }

        if (coordinateButton != null) {
            coordinateButton.setOnClickListener(v -> {
                if (!isBluetoothConnected()) {
                    showBluetoothCheckAlertDialog();
                } else {
                    new AlertDialog.Builder(requireContext())
                            .setTitle("是否確定更改設定？")
                            .setMessage("當您進入時，必須修改好設定，否則將無法正常使用\n是否確定進行修改？")
                            .setNegativeButton("取消", null)
                            .setPositiveButton("是", (dialogInterface, i) -> {
                                clearPendingInputStream();
                                BluetoothSocketManager.sendString("SetPositionMode", "");
                                Intent intent = new Intent(getContext(), SetCoordinateActivity.class);
                                startActivity(intent);
                            })
                            .create()
                            .show();
                }
            });
        }

        if (colorButton != null) {
            colorButton.setOnClickListener(v -> {
                if (!isBluetoothConnected()) {
                    showBluetoothCheckAlertDialog();
                } else {
                    BluetoothSocketManager.sendString("SetColorMode", "");
                    Intent intent = new Intent(getContext(), SetColorContainerActivity.class);
                    startActivity(intent);
                }
            });
        }

        if (guideButton != null) {
            guideButton.setOnClickListener(v -> {
                Intent intent = new Intent(getContext(), UsageGuideActivity.class);
                startActivity(intent);
            });
        }

        if (bluetoothModeButton != null) {
            bluetoothModeButton.setOnClickListener(v -> {
                if (!isBluetoothConnected()) {
                    showBluetoothCheckAlertDialog();
                } else {
                    Intent intent = new Intent(getContext(), BluetoothModeActivity.class);
                    startActivity(intent);
                }
            });
        }

        if (usbTerminalButton != null) {
            usbTerminalButton.setOnClickListener(v -> handleUsbConnection());
        }
    }

    private void handleUsbConnection() {
        Log.d(TAG, "handleUsbConnection: 統一連接流程已啟動。");
        showProgressDialog("正在準備連接...");

        // 直接呼叫唯一的 connect 方法，它內部會自動處理快速/慢速邏輯
        UsbConnectionManager.getInstance(requireContext()).connect(progressText -> {
            if (getActivity() != null) {
                getActivity().runOnUiThread(() -> updateProgressDialogText(progressText));
            }
        }).thenAccept(result -> {
            if (getActivity() != null) {
                getActivity().runOnUiThread(() -> {
                    hideProgressDialog();
                    if (result.success) {
                        Toast.makeText(getContext(), "連接成功！", Toast.LENGTH_SHORT).show();
                        Intent intent = new Intent(getContext(), UsbTerminalActivity.class);
                        startActivity(intent);
                    } else {
                        // 只有在最終（掃描也）失敗後才顯示錯誤
                        showUsbCheckAlertDialog(result.message);
                    }
                });
            }
        });
    }

    // 失敗時的提示對話框
    private void showUsbCheckAlertDialog(String errorMessage) {
        if (getContext() == null) return;
        new AlertDialog.Builder(getContext())
                .setTitle("USB 連接失敗")
                .setMessage(errorMessage)
                .setNegativeButton("取消", null)
                .setPositiveButton("重試", (dialogInterface, i) -> {
                    // "重試" 再次觸發統一流程
                    handleUsbConnection();
                })
                .create()
                .show();
    }

    private void showProgressDialog(String initialText) {
        if (getContext() == null || (progressDialog != null && progressDialog.isShowing())) {
            return;
        }

        // 1. 建立 Dialog 的自訂佈局
        LinearLayout dialogLayout = new LinearLayout(getContext());
        dialogLayout.setOrientation(LinearLayout.VERTICAL);
        dialogLayout.setGravity(Gravity.CENTER);
        dialogLayout.setPadding(64, 64, 64, 64);

        ProgressBar progressBar = new ProgressBar(getContext(), null, android.R.attr.progressBarStyleLarge);

        progressDialogText = new TextView(getContext());
        LinearLayout.LayoutParams textParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);
        textParams.setMargins(0, 32, 0, 0); // 上方間距
        progressDialogText.setLayoutParams(textParams);
//        progressDialogText.setTextColor(Color.BLACK);
        progressDialogText.setTextSize(16f);
        progressDialogText.setText(initialText);

        dialogLayout.addView(progressBar);
        dialogLayout.addView(progressDialogText);

        // 2. 建立並顯示 AlertDialog
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setView(dialogLayout);
        builder.setCancelable(false); // 設置為不可取消，防止使用者在掃描時關閉

        progressDialog = builder.create();
        progressDialog.show();
        Log.d(TAG, "showProgressDialog: 進度對話框已顯示。");

        setButtonsEnabled(false); // 禁用背景按鈕
    }

    private void updateProgressDialogText(String text) {
        if (progressDialog != null && progressDialog.isShowing() && progressDialogText != null) {
            progressDialogText.setText(text);
        }
    }

    private void hideProgressDialog() {
        if (progressDialog != null && progressDialog.isShowing()) {
            progressDialog.dismiss();
            Log.d(TAG, "hideProgressDialog: 進度對話框已關閉。");
        }
        progressDialog = null;
        setButtonsEnabled(true); // 重新啟用背景按鈕
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        BluetoothSocketManager.addDisconnectListener(this);
    }

    private boolean isBluetoothConnected() {
        return BluetoothSocketManager.getSocket() != null && BluetoothSocketManager.getSocket().isConnected();
    }

    private void showBluetoothCheckAlertDialog() {
        if (getContext() == null) return;
        new AlertDialog.Builder(getContext())
                .setTitle("藍牙未連接")
                .setMessage("您尚未連接藍芽，是否前往連接頁面？")
                .setNegativeButton("取消", null)
                .setPositiveButton("是", (dialogInterface, i) -> {
                    if (getActivity() instanceof MainActivity) {
                        ((MainActivity) getActivity()).getViewPager().setCurrentItem(1, true);
                    }
                })
                .create()
                .show();
    }

    public void setButtonsEnabled(boolean enabled) {
        buttonsEnabled = enabled;
        if (view == null) return;

        float alpha = enabled ? 1f : 0.4f;

        if (motorButton != null) { motorButton.setEnabled(enabled); motorButton.setAlpha(alpha); }
        if (coordinateButton != null) { coordinateButton.setEnabled(enabled); coordinateButton.setAlpha(alpha); }
        if (colorButton != null) { colorButton.setEnabled(enabled); colorButton.setAlpha(alpha); }
        if (testMotorButton != null) { testMotorButton.setEnabled(enabled); testMotorButton.setAlpha(alpha); }
        if (bluetoothModeButton != null) { bluetoothModeButton.setEnabled(enabled); bluetoothModeButton.setAlpha(alpha); }
        if (usbTerminalButton != null) { usbTerminalButton.setEnabled(enabled); usbTerminalButton.setAlpha(alpha); }
        if (guideButton != null) { guideButton.setEnabled(enabled); guideButton.setAlpha(alpha); }
    }

    public static SettingFragment getInstance() {
        return instance;
    }

    public static void setButtonsEnabledGlobal(boolean enabled) {
        buttonsEnabled = enabled;
        if (instance != null) {
            instance.setButtonsEnabled(enabled);
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        BluetoothSocketManager.removeDisconnectListener(this);
        hideProgressDialog();
        view = null;
    }

    @Override
    public void onBluetoothDisconnected() {
        if(getActivity() != null) {
            getActivity().runOnUiThread(() -> setButtonsEnabled(true));
        }
    }

    private void clearPendingInputStream() {
        try {
            if (isBluetoothConnected() && BluetoothSocketManager.getSocket() != null && BluetoothSocketManager.getSocket().getInputStream() != null) {
                InputStream is = BluetoothSocketManager.getSocket().getInputStream();
                int available = is.available();
                if (available > 0) {
                    byte[] buf = new byte[available];
                    is.read(buf);
                    Log.d("SettingFragment", "Cleared " + available + " bytes from Bluetooth input stream.");
                }
            }
        } catch (IOException e) {
            Log.e("SettingFragment", "清除殘留輸入資料失敗: " + e.getMessage());
        }
    }
}