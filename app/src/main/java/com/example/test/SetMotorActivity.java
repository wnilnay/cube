package com.example.test;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.widget.SeekBar;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import org.json.JSONException;
import org.json.JSONObject;

public class SetMotorActivity extends AppCompatActivity {
    private SeekBar seekBar_top, seekBar_down, seekBar_left, seekBar_right;
    private TextView tvw_top, tvw_down, tvw_left, tvw_right;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_set_motor);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
        init();
    }
    private void init()
    {
        seekBar_top = findViewById(R.id.seekBar_Top);
        seekBar_down = findViewById(R.id.seekBar_Down);
        seekBar_left = findViewById(R.id.seekBar_Left);
        seekBar_right = findViewById(R.id.seekBar_Right);

        tvw_top = findViewById(R.id.tvw_top_progress);
        tvw_down = findViewById(R.id.tvw_down_progress);
        tvw_left = findViewById(R.id.tvw_left_progress);
        tvw_right = findViewById(R.id.tvw_right_progress);

        SetListener(seekBar_top,tvw_top);
        SetListener(seekBar_down,tvw_down);
        SetListener(seekBar_left,tvw_left);
        SetListener(seekBar_right,tvw_right);

    }



    private void SetListener(SeekBar seekBar, TextView textView){
        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                textView.setText(""+getResources().getResourceEntryName(textView.getId())+" : "+progress);
                JSONObject jsonObject = new JSONObject();
                try {
                    jsonObject.put("MotorTop",seekBar_top.getProgress());
                    jsonObject.put("MotorBottom",seekBar_down.getProgress());
                    jsonObject.put("MotorLeft",seekBar_left.getProgress());
                    jsonObject.put("MotorRight",seekBar_right.getProgress());
                } catch (JSONException e) {
                    throw new RuntimeException(e);
                }

                //BluetoothSocketManager.sendString("MotorSetting",jsonObject.toString());
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });
    }

}