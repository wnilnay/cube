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
import java.io.DataInputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.Timer;
import java.util.TimerTask;
import java.util.Arrays;

public class SetCoordinateActivity extends AppCompatActivity implements BluetoothDisconnectListener {
    private BluetoothSocket socket = BluetoothSocketManager.getSocket();
    private ImageView imageView;
    private ResizableOverlayView overlayView;
    private Bitmap bitmap;
    private boolean isFirst = true;
    private volatile boolean isEnd = false;
    private Thread readingThread;
    private InputStream rawInputStream;
    private float lastLeft, lastTop, lastRight, lastBottom;
    private static final int MAX_IMAGE_SIZE = 5 * 1024 * 1024; // 允許的最大圖片大小 5MB

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

        // 清除先前可能殘留於輸入串流中的資料，避免之後 getDataString 讀到亂碼
        //clearPendingInputStream();

        // 註冊至 BluetoothSocketManager，以便藍芽斷線時自動關閉
        BluetoothSocketManager.addDisconnectListener(this);

        Timer timer = new Timer();
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                String[] data = BluetoothSocketManager.getDataString();
                if(data != null && data[0].equals("CoordinateSetting")){
                    //Log.d("wnilnay",data[1]);
                    try {
                        JSONObject jsonObject = new JSONObject(data[1]);
                        lastLeft = (float) jsonObject.getDouble("xCoordinate_of_UpperLeft");
                        lastTop = (float) jsonObject.getDouble("yCoordinate_of_UpperLeft");
                        lastRight = (float) jsonObject.getDouble("xCoordinate_of_LowerRight");
                        lastBottom = (float) jsonObject.getDouble("yCoordinate_of_LowerRight");
                    }
                    catch (Exception e){
                        Log.d("wnilnay SetCoordinateActivity",e.toString());
                    }

                    // 以獨立執行緒執行，方便後續中斷
                    readingThread = new Thread(SetCoordinateActivity.this::ConnectToPi_while,
                            "BT-Image-Reader");
                    readingThread.start();
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

            int times = 0;
            while (!isEnd) {
                // 讀取圖片長度
                byte[] lengthBytes = new byte[4];
                int lengthRead = in.read(lengthBytes);
                if (lengthRead != 4) {
                    Log.e("wnilnay BT", "❌ 無法讀取圖片長度，結束接收");
                    break;
                }

                int length = ByteBuffer.wrap(lengthBytes).getInt();
                Log.d("wnilnay BT", "📥 新圖片大小: " + length + " bytes");

                // 當長度為負或過大時跳過此張圖片
                if (length <= 0 || length > MAX_IMAGE_SIZE) {
                    Log.e("wnilnay BT", "❌ 圖片長度異常: " + length + " bytes，跳過此張");

//                    // 若為正且過大，仍需將資料讀取/跳過以保持串流同步
//                    if (length > 0) {
//                        try {
//                            skipBytesSafely(in, length);
//                        } catch (IOException e) {
//                            Log.e("wnilnay BT", "❌ 跳過異常長度資料失敗: " + e.getMessage());
//                            break;
//                        }
//                    }
                    clearPendingInputStream();
                    if (!isEnd) {
                        BluetoothSocketManager.sendString("IMAGE", times++ + "");
                    }
                    //Thread.sleep(2000);
                    continue; // 直接讀取下一張
                }

                // 讀取圖片資料
                byte[] imageBytes = new byte[length];
                int offset = 0;
                long start = System.currentTimeMillis();

                // 紀錄最後一次成功接收資料的時間，用於判斷是否卡住
                long lastProgressTime = System.currentTimeMillis();
                boolean timeoutOccurred = false;

                while (offset < length && !isEnd) {
                    int available = in.available();

                    if (available > 0) {
                        int toRead = Math.min(available, length - offset);
                        int read = in.read(imageBytes, offset, toRead);
                        if (read == -1) {
                            break; // 連線已關閉
                        }

                        offset += read;
                        lastProgressTime = System.currentTimeMillis();
                        //Log.d("wnilnay BT", "📥 已接收: " + offset + "/" + length);
                    } else {
                        // 若 1 秒內都沒有任何新資料進來，視為逾時
                        if (System.currentTimeMillis() - lastProgressTime > 1000) {
                            timeoutOccurred = true;
                            Log.e("wnilnay BT", "❌ 圖片接收不完整，放棄本張");
                            if (!isEnd) {
                                BluetoothSocketManager.sendString("IMAGE", times++ + "");
                            }
                            else {
                                clearPendingInputStream();
                            }
                            break;
                        }

                        // 稍作等待，避免忙迴圈
                        try {
                            Thread.sleep(10);
                        } catch (InterruptedException ignored) {
                        }
                    }
                }

                if (timeoutOccurred) {
                    // 已經逾時並處理完，直接開始等待下一張圖片
                    continue;
                }

                long end = System.currentTimeMillis();
                Log.d("wnilnay BT", "⏱️ 接收時間: " + (end - start) + " ms");

                if (offset == length) {
                    bitmap = BitmapFactory.decodeByteArray(imageBytes, 0, length);
                    if (bitmap != null) {
                        runOnUiThread(() -> imageView.setImageBitmap(bitmap));
                        Log.d("wnilnay BT", "✅ 圖片已顯示");
                        if (!isEnd) {
                            BluetoothSocketManager.sendString("IMAGE", times++ + "");
                        }
                    } else {
                        Log.e("wnilnay BT", "❌ 轉換 Bitmap 失敗");
                        if (!isEnd) {
                            BluetoothSocketManager.sendString("IMAGE", times++ + "");
                        }
                    }
                }
                else {
                    Log.e("wnilnay BT", "❌ 圖片接收不完整，放棄本張");
                    if (!isEnd) {
                        BluetoothSocketManager.sendString("IMAGE", times++ + "");
                    }
                }

                // 可選：避免壓力過大，暫停一點點
                Thread.sleep(10);  // 每張間隔 100ms 可調整
            }

        } catch (IOException e) {
            e.printStackTrace();
            runOnUiThread(() -> Toast.makeText(this, "連接錯誤：" + e.getMessage(), Toast.LENGTH_LONG).show());
        } catch (InterruptedException e) {
            // 使用者可能已經離開畫面，導致執行緒被中斷，屬於正常流程
            Log.i("wnilnay BT", "讀取執行緒已中斷，正常結束");
            Thread.currentThread().interrupt(); // 恢復中斷狀態，避免吞掉中斷
            clearPendingInputStream();
        }
    }

    @Override
    protected void onDestroy() {
        isEnd = true;
        if (readingThread != null) {
            readingThread.interrupt();
            try {
                // 等待執行緒結束（最多 500ms），避免殘留資料注入
                readingThread.join(500);
            } catch (InterruptedException ignored) {}
        }

        if(bitmap == null){
            Toast.makeText(this, "未接收到圖片，故傳送原始值", Toast.LENGTH_LONG).show();
            try {
                JSONObject jsonObject = new JSONObject();
                jsonObject.put("xCoordinate_of_UpperLeft", lastLeft);
                jsonObject.put("yCoordinate_of_UpperLeft", lastTop);
                jsonObject.put("xCoordinate_of_LowerRight", lastRight);
                jsonObject.put("yCoordinate_of_LowerRight", lastBottom);
                BluetoothSocketManager.sendString("CoordinateSetting",jsonObject.toString());
            } catch (JSONException e) {
                throw new RuntimeException(e);
            }
        }
        else {
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
        }

        // 釋放最後一張圖片記憶體
        if(bitmap != null && !bitmap.isRecycled()){
            bitmap.recycle();
            bitmap = null;
        }

        // 取消註冊
        BluetoothSocketManager.removeDisconnectListener(this);

        clearPendingInputStream();

        super.onDestroy();
    }

    /**
     * 安全跳過指定位元組數，避免阻塞
     */
    private void skipBytesSafely(InputStream in, long len) throws IOException {
        byte[] buf = new byte[1024];
        long remaining = len;
        while (remaining > 0) {
            int toRead = (int) Math.min(remaining, buf.length);
            int skipped = in.read(buf, 0, toRead);
            if (skipped == -1) break;
            remaining -= skipped;
        }
    }

    @Override
    public void onBluetoothDisconnected() {
        Toast.makeText(this, "藍芽斷線，將退出設定座標頁面", Toast.LENGTH_SHORT).show();
        finish();
    }

    /**
     * 將目前 InputStream 中剩餘的可讀位元組全部讀掉，
     * 以免上次畫面離開時殘留的影像或雜訊影響到下一次的指令解析。
     */
    private void clearPendingInputStream() {
        try {
            if (socket != null && socket.isConnected()) {
                InputStream is = socket.getInputStream();
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