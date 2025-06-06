package com.example.test;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;

import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Toast;

import java.util.Timer;
import java.util.TimerTask;

public class SettingFragment extends Fragment {
    private View view;
    private Button motorButton, coordinateButton, colorButton, testMotorButton;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        if(view == null){
            view = inflater.inflate(R.layout.fragment_setting, container, false);
            motorButton = view.findViewById(R.id.motor_button);
            coordinateButton = view.findViewById(R.id.coordinate_button);
            colorButton = view.findViewById(R.id.color_button);
            testMotorButton = view.findViewById(R.id.testMotor_button);

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
                        motorButton.setEnabled(false);
                        coordinateButton.setEnabled(false);
                        colorButton.setEnabled(false);
                        testMotorButton.setEnabled(false);
                        Toast.makeText(requireContext(), "正在進行測試中...", Toast.LENGTH_LONG).show();
                        Timer timer = new Timer();
                        timer.schedule(new TimerTask() {
                            @Override
                            public void run() {
                                String[] data = BluetoothSocketManager.getDataString();
                                if(data != null && data[0].contains("EndTestMotorMode")) {
                                    requireActivity().runOnUiThread(() -> {
                                        Toast.makeText(requireContext(), "測試結束", Toast.LENGTH_SHORT).show();
                                        motorButton.setEnabled(true);
                                        coordinateButton.setEnabled(true);
                                        colorButton.setEnabled(true);
                                        testMotorButton.setEnabled(true);
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
        }
        return view;
    }

    private Boolean isConnected(){
        return BluetoothSocketManager.getSocket() != null;
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
}