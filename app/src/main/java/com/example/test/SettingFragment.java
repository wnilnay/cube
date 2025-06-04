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

public class SettingFragment extends Fragment {
    private View view;
    private Button motorButton, coordinateButton, colorButton;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        if(view == null){
            view = inflater.inflate(R.layout.fragment_setting, container, false);
            motorButton = view.findViewById(R.id.motor_button);
            coordinateButton = view.findViewById(R.id.coordinate_button);
            colorButton = view.findViewById(R.id.color_button);

            motorButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if(isConnected()){
                        showCheckAlertDialog();
                    }
                    else{
                        Intent intent = new Intent(getContext(), SetMotorActivity.class);
                        startActivity(intent);
                    }
                }
            });
            coordinateButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if(isConnected()){
                        showCheckAlertDialog();
                    }
                    else {
                        Intent intent = new Intent(getContext(), SetCoordinateActivity.class);
                        startActivity(intent);
                    }
                }
            });
            colorButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if(isConnected()){
                        showCheckAlertDialog();
                    }
                    else {
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
}