package com.example.test;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Toast;

import java.io.IOException;
import java.io.InputStream;
import java.util.Timer;
import java.util.TimerTask;

public class SettingFragment extends Fragment implements BluetoothDisconnectListener{
    private View view;
    private Button motorButton, coordinateButton, colorButton, testMotorButton, guideButton;
    private static SettingFragment instance;
    private static boolean buttonsEnabled = true;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        if(view == null){
            instance = this;
            view = inflater.inflate(R.layout.fragment_setting, container, false);
            motorButton = view.findViewById(R.id.motor_button);
            coordinateButton = view.findViewById(R.id.coordinate_button);
            colorButton = view.findViewById(R.id.color_button);
            testMotorButton = view.findViewById(R.id.testMotor_button);
            guideButton = view.findViewById(R.id.guide_button);

            // 套用當前按鈕狀態（若已被其他 Fragment 鎖定）
            setButtonsEnabled(buttonsEnabled);

            motorButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if(!isConnected()){
                        showCheckAlertDialog();
                    }
                    else{
                        new AlertDialog.Builder(getContext())
                                .setTitle("是否確定更改設定？")
                                .setMessage("當您進入時，必須修改好設定，否則將無法正常使用\n是否確定進行修改？")
                                .setNegativeButton("取消", null)
                                .setPositiveButton("是", new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialogInterface, int i) {
                                        BluetoothSocketManager.sendString("SetMotorMode","");
                                        Intent intent = new Intent(getContext(), SetMotorActivity.class);
                                        startActivity(intent);
                                    }
                                })
                                .create()
                                .show();
                    }
                }
            });

            testMotorButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if(!isConnected()){
                        showCheckAlertDialog();
                    }
                    else{
                        BluetoothSocketManager.sendString("TestMotorMode","");
                        setButtonsEnabled(false);
                        MainFragment.setOkButtonEnabled(false);
                        Toast.makeText(requireContext(), "正在進行測試中...", Toast.LENGTH_LONG).show();
                        Timer timer = new Timer();
                        timer.schedule(new TimerTask() {
                            @Override
                            public void run() {
                                String[] data = BluetoothSocketManager.getDataString();
                                if(data != null && data[0].contains("EndTestMotorMode")) {
                                    requireActivity().runOnUiThread(() -> {
                                        Toast.makeText(requireContext(), "測試結束", Toast.LENGTH_SHORT).show();
                                        setButtonsEnabled(true);
                                        MainFragment.setOkButtonEnabled(true);
                                        timer.cancel();
                                    });
                                }
                            }
                        }, 0, 100);
                    }
                }
            });
            coordinateButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if(!isConnected()){
                        showCheckAlertDialog();
                    }
                    else {
                        new AlertDialog.Builder(getContext())
                                .setTitle("是否確定更改設定？")
                                .setMessage("當您進入時，必須修改好設定，否則將無法正常使用\n是否確定進行修改？")
                                .setNegativeButton("取消", null)
                                .setPositiveButton("是", new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialogInterface, int i) {
                                        clearPendingInputStream();
                                        BluetoothSocketManager.sendString("SetPositionMode","");
                                        Intent intent = new Intent(getContext(), SetCoordinateActivity.class);
                                        startActivity(intent);
                                    }
                                })
                                .create()
                                .show();
                    }
                }
            });
            colorButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if(!isConnected()){
                        showCheckAlertDialog();
                    }
                    else {
                        BluetoothSocketManager.sendString("SetColorMode","");
                        Intent intent = new Intent(getContext(), SetColorContainerActivity.class);
                        startActivity(intent);
                    }
                }
            });
            guideButton.setOnClickListener(v -> {
                Intent intent = new Intent(getContext(), UsageGuideActivity.class);
                startActivity(intent);
            });
        }
        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        BluetoothSocketManager.addDisconnectListener(this);
    }

    private Boolean isConnected(){
        return BluetoothSocketManager.getSocket() != null && BluetoothSocketManager.getSocket().isConnected();
    }
    private void showCheckAlertDialog(){
        new AlertDialog.Builder(getContext())
                .setTitle("是否返回連線？")
                .setMessage("您尚未連接藍芽，是否返回連接？")
                .setNegativeButton("取消", null)
                .setPositiveButton("是", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        MainActivity mainActivity = (MainActivity) getActivity();
                        mainActivity.getViewPager().setCurrentItem(1,true);
                    }
                })
                .create()
                .show();
    }
    private void showCheckInSettingAlertDialog(){

    }

    /**
     * 將設定頁四顆按鈕統一啟用／停用並調整透明度
     */
    public void setButtonsEnabled(boolean enabled){
        buttonsEnabled = enabled;
        if(motorButton == null) return; // 尚未初始化
        motorButton.setEnabled(enabled);
        coordinateButton.setEnabled(enabled);
        colorButton.setEnabled(enabled);
        testMotorButton.setEnabled(enabled);

        float alpha = enabled ? 1f : 0.4f;
        motorButton.setAlpha(alpha);
        coordinateButton.setAlpha(alpha);
        colorButton.setAlpha(alpha);
        testMotorButton.setAlpha(alpha);
    }

    public static SettingFragment getInstance(){
        return instance;
    }

    /**
     * 提供其他 Fragment 在無法取得 instance 時也能更新按鈕狀態
     */
    public static void setButtonsEnabledGlobal(boolean enabled){
        buttonsEnabled = enabled;
        if(instance != null){
            instance.setButtonsEnabled(enabled);
        }
    }

    @Override
    public void onDestroy() {
        BluetoothSocketManager.removeDisconnectListener(this);
        super.onDestroy();
    }

    @Override
    public void onBluetoothDisconnected() {
        setButtonsEnabled(true);
    }

    private void clearPendingInputStream() {
        try {
            if (BluetoothSocketManager.getSocket() != null && BluetoothSocketManager.getSocket().isConnected()) {
                InputStream is = BluetoothSocketManager.getSocket().getInputStream();
                long lastRead = System.currentTimeMillis();
                byte[] buf = new byte[1024];

                while (true) {
                    int available = is.available();
                    if (available > 0) {
                        int toRead = Math.min(available, buf.length);
                        if (is.read(buf, 0, toRead) == -1) break;
                        lastRead = System.currentTimeMillis();
                    } else {
                        // 若連續 100ms 都沒有新資料可讀，視為清空完成
                        if (System.currentTimeMillis() - lastRead > 100) {
                            break;
                        }
                        try {
                            Thread.sleep(10);
                        } catch (InterruptedException ignored) {}
                    }
                }
            }
        } catch (IOException e) {
            Log.e("wnilnay BT", "清除殘留輸入資料失敗: " + e.getMessage());
        }
        Log.d("wnilnay", "clear done");
    }
}