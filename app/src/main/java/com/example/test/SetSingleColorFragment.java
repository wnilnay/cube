package com.example.test;

import static android.app.Activity.RESULT_OK;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.graphics.PointF;
import android.graphics.RectF;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;

import androidx.annotation.LongDef;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import android.provider.MediaStore;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

public class SetSingleColorFragment extends Fragment {
    private View view;
    private ImageView imageView;
    private ResizableOverlayView overlayView;
    private Button fromAlbumsbButton, fromCamaraButton, backToDefaultButton, saveButton;
    private final int REQUEST_GALLERY = 1;
    private final int REQUEST_CAMERA = 2;

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        if(view == null){
            view = inflater.inflate(R.layout.fragment_set_single_color, container, false);
            imageView = view.findViewById(R.id.color_imageView);
            overlayView = view.findViewById(R.id.setColor_overlayView);
            fromAlbumsbButton = view.findViewById(R.id.fromAlbums_button);
            fromCamaraButton = view.findViewById(R.id.fromCamara_button);
            backToDefaultButton = view.findViewById(R.id.back_to_default_button);
            saveButton = view.findViewById(R.id.save_button);

            String color = getArguments().getString("color");
            Bitmap myBitmap = StorageUtil.loadBitmap(getContext(), "color_" + color);

            if(myBitmap == null){
                switch (color){
                    case "white":
                        //imageView.setImageResource(R.drawable.cube_default_white);
                        myBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.cube_default_white);
                        break;
                    case "yellow":
                        myBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.cube_default_yellow);
                        break;
                    case "green":
                        myBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.cube_default_green);
                        break;
                    case "blue":
                        myBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.cube_default_blue);
                        break;
                    case "red":
                        myBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.cube_default_red);
                        break;
                    case "orange":
                        myBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.cube_default_orange);
                        break;
                    default:
                        break;
                }
                myBitmap = BitmapUtil.resizeAndCompressBitmap(myBitmap);
            }
            imageView.setImageBitmap(myBitmap);

            overlayView.setOnTouchListener(new View.OnTouchListener() {
                @Override
                public boolean onTouch(View view, MotionEvent motionEvent) {
                    RectF rect = overlayView.getMaskRect();
                    if(rect.bottom > imageView.getHeight()){
                        rect.bottom = imageView.getHeight();
                    }
                    if(rect.top < 0){
                        rect.top = 0;
                    }
                    overlayView.invalidate();
                    Log.d("wnilnay rect", "left:" + rect.left + ", top:" + rect.top + ", right:" + rect.right + ", bottom:" + rect.bottom);
                    if(imageView.getDrawable() != null && motionEvent.getAction() == MotionEvent.ACTION_UP){
                        Bitmap bitmap = ((BitmapDrawable) imageView.getDrawable()).getBitmap();
                        new Thread(new Runnable() {
                            @Override
                            public void run() {
                                analyzeBitmap(bitmap,ResizableOverlayView.mapRectFromViewToBitmap(rect, imageView, bitmap));
                            }
                        }).start();
                    }

                    return false;
                }
            });
            fromAlbumsbButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    fromAlbums();
                }
            });
            fromCamaraButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    fromCamara();
                }
            });
            backToDefaultButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    setBackToDefault();
                }
            });
            saveButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    saveAndQuit();
                }
            });
        }
        return view;
    }

    private void analyzeBitmap(Bitmap bitmap, RectF rect){
        HsvAnalyzer.HsvStats stats = new HsvAnalyzer.HsvStats();
        if(rect == null){
            stats = HsvAnalyzer.analyze(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight());
        }
        else {
            stats = HsvAnalyzer.analyze(bitmap, (int) rect.left, (int) rect.top, (int) rect.right, (int) rect.bottom);
        }

        Log.d("wnilnay HSV", "平均 H: " + stats.avgH + ", S: " + stats.avgS + ", V: " + stats.avgV);
        Log.d("wnilnay HSV", "最大 H: " + stats.maxH + ", S: " + stats.maxS + ", V: " + stats.maxV);
        Log.d("wnilnay HSV", "最小 H: " + stats.minH + ", S: " + stats.minS + ", V: " + stats.minV);
        Log.d("wnilnay HSV", "眾數 H: " + stats.modeH + ", S: " + stats.modeS + ", V: " + stats.modeV);
    }
    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        Bitmap bitmap = null;

        if (resultCode == RESULT_OK) {
            if (requestCode == REQUEST_CAMERA && data != null) {
                bitmap = (Bitmap) data.getExtras().get("data");  // 拍照回來的是縮圖
            }
            else if (requestCode == REQUEST_GALLERY && data != null) {
                Uri imageUri = data.getData();
                try {
                    // 1. 先取得圖片尺寸（不載入記憶體）
                    InputStream input = getActivity().getContentResolver().openInputStream(imageUri);
                    BitmapFactory.Options options = new BitmapFactory.Options();
                    options.inJustDecodeBounds = true;
                    BitmapFactory.decodeStream(input, null, options);
                    input.close();

                    int originalWidth = options.outWidth;
                    int originalHeight = options.outHeight;

                    // 2. 計算縮小比例（最大寬度或高度設定，例如 1000px）
                    int maxDim = 1000;
                    int scale = 1;
                    while (originalWidth / scale > maxDim || originalHeight / scale > maxDim) {
                        scale *= 2;
                    }

                    // 3. 重新載入圖片並縮小
                    options.inSampleSize = scale;
                    options.inJustDecodeBounds = false;

                    input = getActivity().getContentResolver().openInputStream(imageUri);
                    Bitmap scaledBitmap = BitmapFactory.decodeStream(input, null, options);
                    input.close();

                    // 4. 壓縮圖片
                    ByteArrayOutputStream out = new ByteArrayOutputStream();
                    scaledBitmap.compress(Bitmap.CompressFormat.JPEG, 70, out); // 壓縮品質 70%
                    byte[] byteArray = out.toByteArray();
                    bitmap = BitmapFactory.decodeByteArray(byteArray, 0, byteArray.length);

                    // 5. 可用 bitmap 顯示或分析 HSV
                    //imageView.setImageBitmap(bitmap); // 如果你有 imageView

                } catch (IOException e) {
                    e.printStackTrace();
                }
            }


            if (bitmap != null) {
                imageView.setImageBitmap(bitmap);
                analyzeBitmap(bitmap, null);
            }
        }
    }

    private void fromAlbums() {
        Intent pickPhoto = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        startActivityForResult(pickPhoto, REQUEST_GALLERY);
    }

    private void fromCamara() {
        Intent takePicture = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        startActivityForResult(takePicture, REQUEST_CAMERA);
    }
    private void setBackToDefault(){

    }
    private void saveAndQuit(){

    }

}