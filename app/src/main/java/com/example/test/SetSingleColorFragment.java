package com.example.test;

import static android.app.Activity.RESULT_OK;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.RectF;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.Bundle;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.FileProvider;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.text.DecimalFormat;

public class SetSingleColorFragment extends Fragment {
    private View view;
    private ImageView imageView;
    private ResizableOverlayView overlayView;
    private Button fromAlbumsbButton, fromCamaraButton, backToDefaultButton, saveButton;
    private final int REQUEST_GALLERY = 1;
    private final int REQUEST_CAMERA = 2;
    private File saveDir;
    private String color = "";
    private TextView upperHSVtextview, lowerHSVtextview;
    private boolean isWhite = false, isRect = true, isAnalyzeDone = true;
    private float[] hsv_upper_255;
    private float[] hsv_lower_255;
    private float[] overlayRect_forCamara_coordinate;

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
            upperHSVtextview = view.findViewById(R.id.upper_hsv_textview);
            lowerHSVtextview = view.findViewById(R.id.lower_hsv_textview);

            // 改為 App-specific External Storage，避免 Scoped Storage 寫入受限
            saveDir = requireContext().getExternalFilesDir(Environment.DIRECTORY_PICTURES);

            Bundle arg = getArguments();
            assert arg != null;
            color = arg.getString("color");
            Bitmap myBitmap = StorageUtil.loadBitmap(getContext(), "bitmap_" + color);
            isWhite = color.equals("white");
            //Log.d("wnilnay color", color.equals("white") + "");

            if(myBitmap == null){
                setBackToDefaultBitmap();
                setBackToDefaultRectF();
                setBackToDefaultHSV();
            }
            else {
                imageView.setImageBitmap(myBitmap);

                overlayView.post(new Runnable() {
                    @Override
                    public void run() {
                        String jsonString = StorageUtil.getString(getContext(), "rectF_" + color);
                        try {
                            JSONObject jsonObject = new JSONObject(jsonString);
                            RectF rect = overlayView.getMaskRect();
                            rect.left = (float) jsonObject.getDouble("left");
                            rect.top = (float) jsonObject.getDouble("top");
                            rect.right = (float) jsonObject.getDouble("right");
                            rect.bottom = (float) jsonObject.getDouble("bottom");
                            overlayView.invalidate();
                        }
                        catch (JSONException e) {
                            throw new RuntimeException(e);
                        }
                    }
                });

                upperHSVtextview.post(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            String jsonString = StorageUtil.getString(getContext(), "HSV_" + color + "_upper");
                            JSONArray jsonArray = new JSONArray(jsonString);
                            float[] hsv_upper = new float[jsonArray.length()];
                            for (int i = 0; i < jsonArray.length(); i++) {
                                hsv_upper[i] = (float) jsonArray.getDouble(i);
                            }

                            jsonString = StorageUtil.getString(getContext(), "HSV_" + color + "_lower");
                            jsonArray = new JSONArray(jsonString);
                            float[] hsv_lower = new float[jsonArray.length()];
                            for (int i = 0; i < jsonArray.length(); i++) {
                                hsv_lower[i] = (float) jsonArray.getDouble(i);
                            }

                            hsv_upper_255 = hsv_upper;
                            hsv_lower_255 = hsv_lower;
                            updateColorTextView(hsv_upper, hsv_lower);

                            // 使用 newColors 陣列
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                });

            }

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
                    //Log.d("wnilnay rect", "left:" + rect.left + ", top:" + rect.top + ", right:" + rect.right + ", bottom:" + rect.bottom);
                    if(imageView.getDrawable() != null && motionEvent.getAction() == MotionEvent.ACTION_UP){
                        Bitmap bitmap = ((BitmapDrawable) imageView.getDrawable()).getBitmap();
                        new Thread(new Runnable() {
                            @Override
                            public void run() {
                                if(isWhite){
                                    float[] s = analyzeBitmap(bitmap,ResizableOverlayView
                                            .mapRectFromViewToBitmap(rect, imageView, bitmap), isWhite);
                                    hsv_lower_255 = new float[]{0, Math.round(s[0] * 255), 100};
                                    hsv_upper_255 = new float[]{360, Math.round(s[1] * 255), 255};
                                    updateColorTextView(hsv_upper_255, hsv_lower_255);
                                }
                                else {
                                    float[] h = analyzeBitmap(bitmap, ResizableOverlayView
                                            .mapRectFromViewToBitmap(rect, imageView, bitmap), isWhite);
                                    hsv_lower_255 = new float[]{h[0], 100, 100};
                                    hsv_upper_255 = new float[]{h[1], 255, 255};
                                    updateColorTextView(hsv_upper_255, hsv_lower_255);
                                }

                            }
                        }).start();
                        isRect = true;
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

    private float[] analyzeBitmap(Bitmap bitmap, RectF rect, boolean isWhite){
        isAnalyzeDone = false;
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

        if(!isWhite) {
            float hueRange = stats.maxH - stats.minH;
            float averageHue = (stats.avgH + stats.modeH) / 2;
            isAnalyzeDone = true;
            return new float[]{(averageHue - hueRange) < 0 ? 0 : averageHue - hueRange,
                    averageHue + hueRange};
        }
        else {
            float sRange = stats.maxS - stats.minS;
            float averageS = (stats.avgS + stats.modeS) / 2;
            isAnalyzeDone = true;
            return new float[]{(averageS - sRange) < 0 ? 0 : averageS - sRange,
                    averageS + sRange};
        }
    }
    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        overlayView.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                overlayView.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                if(overlayRect_forCamara_coordinate == null) return;
                overlayView.getMaskRect().set(overlayRect_forCamara_coordinate[0],
                        overlayRect_forCamara_coordinate[1],
                        overlayRect_forCamara_coordinate[2],
                        overlayRect_forCamara_coordinate[3]);
                overlayView.invalidate();
            }
        });
        Bitmap bitmap = null;

        if (resultCode == RESULT_OK) {

            Log.d("wnilnay","onActivityResult_RESULT_OK");
//            if (requestCode == REQUEST_CAMERA && data != null) {
//                bitmap = (Bitmap) data.getExtras().get("data");  // 拍照回來的是縮圖
//            }
            if(requestCode == REQUEST_CAMERA){
                bitmap = BitmapFactory.decodeFile(
                        new File(saveDir, "cube.jpg").getAbsolutePath()
                );
                bitmap = BitmapUtil.resizeAndCompressBitmap(bitmap,1200,1200,50);
                //Log.d("wnilnay", "have bitmap");
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
            else {
                Log.e("wnilnay error", data.toString());
            }


            if (bitmap != null) {
                imageView.setImageBitmap(bitmap);
                upperHSVtextview.setText("請選取遮罩範圍");
                lowerHSVtextview.setText("請選取遮罩範圍");
                isRect = false;
            }
        }
    }

    private void fromAlbums() {
        Intent pickPhoto = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        startActivityForResult(pickPhoto, REQUEST_GALLERY);
    }

    private void fromCamara() {
        File photoFile = new File(saveDir, "cube.jpg");
        Uri uri = FileProvider.getUriForFile(requireContext(),
                requireActivity().getPackageName() + ".fileprovider",
                photoFile);
        RectF overlayRect_forCamara = overlayView.getMaskRect();
        overlayRect_forCamara_coordinate = new float[]{overlayRect_forCamara.left, overlayRect_forCamara.top,
                overlayRect_forCamara.right, overlayRect_forCamara.bottom};
        //Log.d("wnilnay", "overlayRect_forCamara: " + overlayRect_forCamara.toString());
        Intent takePicture = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        takePicture.putExtra(MediaStore.EXTRA_OUTPUT, uri);
        // 授予相機寫入權限
        takePicture.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION | Intent.FLAG_GRANT_READ_URI_PERMISSION);
        startActivityForResult(takePicture, REQUEST_CAMERA);
    }
    private void setBackToDefault(){
        //StorageUtil.deleteBitmap(getContext(), "color_" + color);
        new AlertDialog.Builder(getContext())
                .setTitle("返回預設值")
                .setMessage("是否返回預設值")
                .setPositiveButton("是", (dialog, which) -> {
                    setBackToDefaultBitmap();
                    setBackToDefaultRectF();
                    setBackToDefaultHSV();
                })
                .setNeutralButton("否", null)
                .create().show();
    }

    private void setBackToDefaultBitmap(){
        Bitmap myBitmap = null;
        switch (color){
            case "white":
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
        imageView.setImageBitmap(myBitmap);
    }
    private void setBackToDefaultRectF(){
        overlayView.post(new Runnable() {
            @Override
            public void run() {
                RectF rectF = overlayView.getMaskRect();
                switch (color){
                    case "white":
                        rectF.set(76.56812f, 264.86377f, 652.5f, 836.0f);
                        break;
                    case "yellow":
                        rectF.set(74.5f, 282.0f, 629.0f, 838.0f);
                        break;
                    case "green":
                        rectF.set(57.527626f, 217.0f, 661.0f, 813.0f);
                        break;
                    case "blue":
                        rectF.set(67.5192f, 274.0f, 667.0f, 871.0f);
                        break;
                    case "red":
                        rectF.set(78.0f, 243.0f, 696.0f, 850.79553f);
                        break;
                    case "orange":
                        rectF.set(72.5f, 241.0f, 690.0f, 862.0f);
                        break;
                    default:
                        break;
                }
                overlayView.invalidate();
            }
        });
    }
    private void setBackToDefaultHSV(){
        float hue_upper = 0, hue_lower = 0;     // 色相 H: 0 ~ 360
        float s255_upper = 0, s255_lower = 0;       // 飽和度 S: 0 ~ 255
        float v255_upper = 0, v255_lower = 0;       // 明度 V: 0 ~ 255

        switch (color){
            case "white":
                hue_lower = 0;s255_lower = 0;v255_lower = 100;
                hue_upper = 360;s255_upper = 25;v255_upper = 255;
                break;
            case "yellow":
                hue_lower = 30;s255_lower = 100;v255_lower = 100;
                hue_upper = 35;s255_upper = 255;v255_upper = 255;
                break;
            case "green":
                hue_lower = 60;s255_lower = 100;v255_lower = 100;
                hue_upper = 70;s255_upper = 255;v255_upper = 255;
                break;
            case "blue":
                hue_lower = 105;s255_lower = 100;v255_lower = 100;
                hue_upper = 115;s255_upper = 255;v255_upper = 255;
                break;
            case "red":
                hue_lower = 0;s255_lower = 100;v255_lower = 100;
                hue_upper = 5;s255_upper = 255;v255_upper = 255;
                break;
            case "orange":
                hue_lower = 7;s255_lower = 100;v255_lower = 100;
                hue_upper = 14;s255_upper = 255;v255_upper = 255;
                break;
            default:
                break;
        }
        hsv_upper_255 = new float[]{hue_upper, s255_upper, v255_upper};
        hsv_lower_255 = new float[]{hue_lower, s255_lower, v255_lower};

        updateColorTextView(hsv_upper_255, hsv_lower_255);
    }
    private void updateColorTextView(float[] hsv_upper_255, float[] hsv_lower_255){
        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                DecimalFormat df = new DecimalFormat("0.00");
                upperHSVtextview.setText("上限：H: " + df.format(hsv_upper_255[0]) + ", S: " +
                        df.format(hsv_upper_255[1]) + ", V: " + df.format(hsv_upper_255[2]));
                lowerHSVtextview.setText("下限：H: " + df.format(hsv_lower_255[0]) + ", S: " +
                        df.format(hsv_lower_255[1]) + ", V: " + df.format(hsv_lower_255[2]));

                float[] hsv_upper = new float[3];
                float[] hsv_lower = new float[3];
                hsv_upper[0] = hsv_upper_255[0];
                hsv_upper[1] = hsv_upper_255[1] / 255f;
                hsv_upper[2] = hsv_upper_255[2] / 255f;

                hsv_lower[0] = hsv_lower_255[0];
                hsv_lower[1] = hsv_lower_255[1] / 255f;
                hsv_lower[2] = hsv_lower_255[2] / 255f;

                int rgbColor_upper = Color.HSVToColor(hsv_upper);
                int rgbColor_lower = Color.HSVToColor(hsv_lower);
                upperHSVtextview.setBackgroundColor(rgbColor_upper);
                lowerHSVtextview.setBackgroundColor(rgbColor_lower);
            }
        });
    }
    private void saveAndQuit(){
        if(!isRect){
            Toast.makeText(getContext(), "請選取遮罩範圍", Toast.LENGTH_SHORT).show();
            return;
        }
        if(!isAnalyzeDone){
            Toast.makeText(getContext(), "請稍後再試", Toast.LENGTH_SHORT).show();
            return;
        }
        new AlertDialog.Builder(getContext())
                .setTitle("儲存並退出")
                .setMessage("是否儲存並退出")
                .setPositiveButton("是", (dialog, which) -> {
                    StorageUtil.saveBitmap(getContext(), "bitmap_" + color,
                            ((BitmapDrawable)imageView.getDrawable()).getBitmap());

                    RectF rect = overlayView.getMaskRect();
                    JSONObject jsonObject = new JSONObject();
                    try {
                        jsonObject.put("left",rect.left);
                        jsonObject.put("top",rect.top);
                        jsonObject.put("right",rect.right);
                        jsonObject.put("bottom",rect.bottom);
                    } catch (JSONException e) {
                        throw new RuntimeException(e);
                    }
                    StorageUtil.saveString(getContext(), "rectF_" + color, jsonObject.toString());

                    JSONArray jsonArray_Upper = new JSONArray();
                    for (float hsv : hsv_upper_255) {
                        try {
                            jsonArray_Upper.put(hsv);
                        }
                        catch (JSONException e) {
                            throw new RuntimeException(e);
                        }
                    }
                    StorageUtil.saveString(getContext(), "HSV_" + color + "_upper", jsonArray_Upper.toString());

                    JSONArray jsonArray_Lower = new JSONArray();
                    for (float hsv : hsv_lower_255) {
                        try {
                            jsonArray_Lower.put(hsv);
                        }
                        catch (JSONException e) {
                            throw new RuntimeException(e);
                        }
                    }
                    StorageUtil.saveString(getContext(), "HSV_" + color + "_lower", jsonArray_Lower.toString());

                    Toast.makeText(getContext(), "儲存成功", Toast.LENGTH_SHORT).show();

                    jsonObject = new JSONObject();
                    try {
                        jsonObject.put(color + "_Lower", jsonArray_Lower);
                        jsonObject.put(color + "_Upper", jsonArray_Upper);
                        BluetoothSocketManager.sendString("ColorSetting",jsonObject.toString());
                        Log.d("wnilnay",jsonObject.toString());
                    } catch (JSONException e) {
                        throw new RuntimeException(e);
                    }
                    FragmentManager fragmentManager = getParentFragmentManager();
                    fragmentManager.beginTransaction()
                            .replace(R.id.main_setColor, new SetColorFragment()).commit();
                })
                .setNegativeButton("取消",null)
                .setNeutralButton("不儲存直接退出",(dialog, which) -> {
                    FragmentManager fragmentManager = getParentFragmentManager();
                    fragmentManager.popBackStack();
                })
                .create().show();
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        OnBackPressedCallback callback = new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                saveAndQuit();
            }
        };

        requireActivity().getOnBackPressedDispatcher().addCallback(this, callback);
    }
}