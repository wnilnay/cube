package com.example.test;

import android.Manifest;
import android.bluetooth.BluetoothSocket;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.widget.ImageView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;

public class SetCoordinateActivity extends AppCompatActivity {
    private BluetoothSocket socket = BluetoothSocketManager.getSocket();
    private ImageView imageView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_set_coordinate);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

//        imageView.findViewById(R.id.display_imageView);
//
//        InputStream inputStream = null;
//        try {
//            inputStream = socket.getInputStream();
//        } catch (IOException e) {
//            throw new RuntimeException(e);
//        }
//        Handler handler = new Handler(Looper.getMainLooper());
//
//        InputStream finalInputStream = inputStream;
//        new Thread(() -> {
//            try {
//                while (true) {
//                    // 先讀4位元的長度
//                    byte[] lenBytes = new byte[4];
//                    finalInputStream.read(lenBytes);
//                    int length = ByteBuffer.wrap(lenBytes).getInt();
//
//                    // 讀影像資料
//                    byte[] imageBytes = new byte[length];
//                    int bytesRead = 0;
//                    while (bytesRead < length) {
//                        int result = finalInputStream.read(imageBytes, bytesRead, length - bytesRead);
//                        if (result == -1) break;
//                        bytesRead += result;
//                    }
//
//                    Bitmap bitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.length);
//                    handler.post(() -> imageView.setImageBitmap(bitmap));
//                }
//            } catch (IOException e) {
//                e.printStackTrace();
//            }
//        }).start();
    }
}