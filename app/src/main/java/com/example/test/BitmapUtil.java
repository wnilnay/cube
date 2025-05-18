package com.example.test;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import java.io.ByteArrayOutputStream;

public class BitmapUtil extends BitmapFactory {
    public static Bitmap resizeAndCompressBitmap(Bitmap original, int maxWidth, int maxHeight, int quality) {
        // Step 1: 計算縮小比例
        int width = original.getWidth();
        int height = original.getHeight();

        float ratioBitmap = (float) width / (float) height;
        float ratioMax = (float) maxWidth / (float) maxHeight;

        int finalWidth = maxWidth;
        int finalHeight = maxHeight;

        if (ratioMax > ratioBitmap) {
            finalWidth = (int) ((float) maxHeight * ratioBitmap);
        } else {
            finalHeight = (int) ((float) maxWidth / ratioBitmap);
        }

        // Step 2: 縮小尺寸
        Bitmap resizedBitmap = Bitmap.createScaledBitmap(original, finalWidth, finalHeight, true);

        // Step 3: 壓縮為 JPEG ByteArray
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        resizedBitmap.compress(Bitmap.CompressFormat.JPEG, quality, out);

        // Step 4: 轉換為 Bitmap
        byte[] byteArray = out.toByteArray();
        return BitmapFactory.decodeByteArray(byteArray, 0, byteArray.length);
    }
    public static Bitmap resizeAndCompressBitmap(Bitmap original){
        return resizeAndCompressBitmap(original,800,800,60);
    }
}
