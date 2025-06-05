package com.example.test;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothSocket;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.RectF;
import android.os.Bundle;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.Timer;
import java.util.TimerTask;

public class SetCoordinateActivity extends AppCompatActivity {
    private BluetoothSocket socket = BluetoothSocketManager.getSocket();
    private ImageView imageView;
    private ResizableOverlayView overlayView;
    private Bitmap bitmap;
    private boolean isFirst = true;

    @SuppressLint("ClickableViewAccessibility")
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
        imageView = findViewById(R.id.display_imageView);
        overlayView = findViewById(R.id.overlayView);

        Timer timer = new Timer();
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                String[] data = BluetoothSocketManager.getDataString();
                if(data != null && data[0].equals("CoordinateSetting")){
                    //Log.d("wnilnay",data[1]);
                    ConnectToPi_while();
                    timer.cancel();
                }
            }
        }, 0,100);
    }
    private  void ConnectToPi_while()
    {
        try {
            InputStream rawIn = socket.getInputStream();
            BufferedInputStream in = new BufferedInputStream(rawIn, 2048);

            while (true) {
                // 讀取圖片長度
                byte[] lengthBytes = new byte[4];
                int lengthRead = in.read(lengthBytes);
                if (lengthRead != 4) {
                    Log.e("wnilnay BT", "❌ 無法讀取圖片長度，結束接收");
                    break;
                }

                int length = ByteBuffer.wrap(lengthBytes).getInt();
                //Log.d("wnilnay BT", "📥 新圖片大小: " + length + " bytes");

                // 讀取圖片資料
                byte[] imageBytes = new byte[length];
                int offset = 0;
                long start = System.currentTimeMillis();

                while (offset < length) {
                    int read = in.read(imageBytes, offset, length - offset);
                    if (read == -1) break;
                    offset += read;
                    //Log.d("wnilnay BT", "📥 已接收: " + offset + "/" + length);
                }

                long end = System.currentTimeMillis();
                //Log.d("wnilnay BT", "⏱️ 接收時間: " + (end - start) + " ms");

                if (offset == length) {
                    bitmap = BitmapFactory.decodeByteArray(imageBytes, 0, length);
                    if (bitmap != null) {
                        runOnUiThread(() -> imageView.setImageBitmap(bitmap));
                        //Log.d("wnilnay BT", "✅ 圖片已顯示");
                    } else {
                        Log.e("wnilnay BT", "❌ 轉換 Bitmap 失敗");
                    }
                } else {
                    Log.e("wnilnay BT", "❌ 圖片接收不完整，放棄本張");
                }

                // 可選：避免壓力過大，暫停一點點
                Thread.sleep(10);  // 每張間隔 100ms 可調整
            }

        } catch (IOException e) {
            e.printStackTrace();
            runOnUiThread(() -> Toast.makeText(this, "連接錯誤：" + e.getMessage(), Toast.LENGTH_LONG).show());
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    protected void onStop() {
        RectF rectF = overlayView.getMaskRect();
        RectF newRectF = ResizableOverlayView.mapRectFromViewToBitmap(rectF, imageView, bitmap);
        try {
            JSONObject jsonObject = new JSONObject();
            assert newRectF != null;
            jsonObject.put("xCoordinate_of_UpperLeft", newRectF.left);
            jsonObject.put("yCoordinate_of_UpperLeft", newRectF.top);
            jsonObject.put("xCoordinate_of_LowerRight", newRectF.right);
            jsonObject.put("yCoordinate_of_LowerRight", newRectF.bottom);
            BluetoothSocketManager.sendString("CoordinateSetting",jsonObject.toString());
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }
        super.onStop();
    }
}