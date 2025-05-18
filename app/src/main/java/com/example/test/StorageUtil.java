package com.example.test;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

public class StorageUtil {
    private static final String PREF_NAME = "Config";
    private static final String[] COLOR_KEYS = {
            "color_white", "color_yellow", "color_green",
            "color_red", "color_blue", "color_orange"
    };

    public static void saveString(Context context, String key, String value) {
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        prefs.edit().putString(key, value).apply();
    }

    public static String getString(Context context, String key) {
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        return prefs.getString(key, null);
    }

    public static void saveBitmap(Context context, String colorKey, Bitmap bitmap) {
        if (isInvalidColorKey(colorKey)) return;

        String filename = colorKey + ".jpg";
        File file = new File(context.getFilesDir(), filename);

        try (FileOutputStream out = new FileOutputStream(file)) {
            bitmap.compress(Bitmap.CompressFormat.JPEG, 80, out);
            out.flush();
            saveString(context, colorKey, file.getAbsolutePath());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static Bitmap loadBitmap(Context context, String colorKey) {
        if (isInvalidColorKey(colorKey)) return null;

        String path = getString(context, colorKey);
        if (path != null) {
            return BitmapFactory.decodeFile(path);
        }
        return null;
    }
    public static void deleteBitmap(Context context, String colorKey) {
        String path = getString(context, colorKey);
        if (path != null) {
            File file = new File(path);
            if (file.exists()) {
                boolean deleted = file.delete();
                if (deleted) {
                    saveString(context, colorKey, null);
                }
            }
        }
    }

    private static boolean isInvalidColorKey(String key) {
        for (String validKey : COLOR_KEYS) {
            if (validKey.equals(key)) return false;
        }
        return true;
    }
}

//// 儲存白色的 Bitmap
//StorageUtil.saveBitmap(context, "color_white", yourWhiteBitmap);
//
//// 讀取白色的 Bitmap
//Bitmap whiteBitmap = StorageUtil.loadBitmap(context, "color_white");
//
//// 儲存字串
//StorageUtil.saveString(context, "user_name", "Alice");
//
//// 讀取字串
//String name = StorageUtil.getString(context, "user_name");

