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
    private static final String[] Bitmap_KEYS = {
            "bitmap_white", "bitmap_yellow", "bitmap_green",
            "bitmap_red", "bitmap_blue", "bitmap_orange"
    };
    private static final String[] RectF_KEYS = {
            "rectF_white", "rectF_yellow", "rectF_green",
            "rectF_red", "rectF_blue", "rectF_orange"
    };
    private static final String[] HSV_KEYS_UPPER = {
            "HSV_white_upper", "HSV_yellow_upper", "HSV_green_upper",
            "HSV_red_upper", "HSV_blue_upper", "HSV_orange_upper"
    };
    private static final String[] HSV_KEYS_LOWER = {
            "HSV_white_lower", "HSV_yellow_lower", "HSV_green_lower",
            "HSV_red_lower", "HSV_blue_lower", "HSV_orange_lower"
    };

    public static void saveString(Context context, String key, String value) {
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        prefs.edit().putString(key, value).apply();
    }

    public static String getString(Context context, String key) {
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        return prefs.getString(key, null);
    }

    public static void deleteString(Context context, String key) {
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        prefs.edit().remove(key).apply();
    }

    public static void saveBitmap(Context context, String bitmapKey, Bitmap bitmap) {
        if (isInvalidBitmapKey(bitmapKey)) return;

        String filename = bitmapKey + ".jpg";
        File file = new File(context.getFilesDir(), filename);

        try (FileOutputStream out = new FileOutputStream(file)) {
            bitmap.compress(Bitmap.CompressFormat.JPEG, 80, out);
            out.flush();
            saveString(context, bitmapKey, file.getAbsolutePath());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static Bitmap loadBitmap(Context context, String bitmapKey) {
        if (isInvalidBitmapKey(bitmapKey)) return null;

        String path = getString(context, bitmapKey);
        if (path != null) {
            return BitmapFactory.decodeFile(path);
        }
        return null;
    }
    public static void deleteBitmap(Context context, String bitmapKey) {
        String path = getString(context, bitmapKey);
        if (path != null) {
            File file = new File(path);
            if (file.exists()) {
                boolean deleted = file.delete();
                if (deleted) {
                    deleteString(context, bitmapKey);
                }
            }
        }
    }

    private static boolean isInvalidBitmapKey(String key) {
        for (String validKey : Bitmap_KEYS) {
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

