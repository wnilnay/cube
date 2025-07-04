package com.example.test;

import android.annotation.SuppressLint;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

public class SetColorContainerActivity extends AppCompatActivity implements BluetoothDisconnectListener {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_set_color_container);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main_setColor), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        FragmentManager fragmentManager = getSupportFragmentManager();
        fragmentManager.beginTransaction().replace(R.id.main_setColor, new SetColorFragment()).commit();

        // 註冊至 BluetoothSocketManager，以便斷線時自動關閉
        BluetoothSocketManager.addDisconnectListener(this);
    }

    @Override
    protected void onDestroy() {
        BluetoothSocketManager.removeDisconnectListener(this);
        BluetoothSocketManager.sendString("EndColorMode","");
        super.onDestroy();
    }

    @Override
    public void onBluetoothDisconnected() {
        Toast.makeText(this, "藍芽斷線，將退出設定顏色頁面", Toast.LENGTH_SHORT).show();
        finish();
    }
}