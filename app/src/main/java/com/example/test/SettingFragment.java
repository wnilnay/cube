package com.example.test;

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
                    Intent intent = new Intent(getContext(), SetMotorActivity.class);
                    startActivity(intent);
                }
            });
            coordinateButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    Intent intent = new Intent(getContext(), SetCoordinateActivity.class);
                    startActivity(intent);
                }
            });
        }
        return view;
    }
}