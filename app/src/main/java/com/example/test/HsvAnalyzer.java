package com.example.test;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.util.Log;

import java.util.*;

public class HsvAnalyzer {
    public static class HsvStats {
        public float avgH, avgS, avgV;
        public float minH, minS, minV;
        public float maxH, maxS, maxV;
        public float modeH, modeS, modeV;
    }

    public static HsvStats analyze(Bitmap bitmap, int startX, int startY, int stopX, int stopY) {
        List<Float> hList = new ArrayList<>();
        List<Float> sList = new ArrayList<>();
        List<Float> vList = new ArrayList<>();

        float[] hsv = new float[3];

        for (int y = startY; y < stopY; y++) {
            for (int x = startX; x < stopX; x++) {
                if(x >= bitmap.getWidth() || y >= bitmap.getHeight()){
                    Log.e("wnilnay error", "x: " + x + ", y: " + y);
                    break;
                }
                int pixel = bitmap.getPixel(x, y);
                Color.RGBToHSV(Color.red(pixel), Color.green(pixel), Color.blue(pixel), hsv);
                hList.add(hsv[0]);
                sList.add(hsv[1]);
                vList.add(hsv[2]);
            }
        }

        // 去除極端值（例如去掉最小/最大各10%）
        hList = removeOutliers(hList);
        sList = removeOutliers(sList);
        vList = removeOutliers(vList);

        HsvStats stats = new HsvStats();
        stats.avgH = average(hList);
        stats.avgS = average(sList);
        stats.avgV = average(vList);

        stats.minH = Collections.min(hList);
        stats.maxH = Collections.max(hList);
        stats.minS = Collections.min(sList);
        stats.maxS = Collections.max(sList);
        stats.minV = Collections.min(vList);
        stats.maxV = Collections.max(vList);

        stats.modeH = mode(hList);
        stats.modeS = mode(sList);
        stats.modeV = mode(vList);

        return stats;
    }

    private static List<Float> removeOutliers(List<Float> list) {
        Collections.sort(list);
        int n = list.size();
        int cut = n / 10; // 去除最小/最大各10%
        return new ArrayList<>(list.subList(cut, n - cut));
    }

    private static float average(List<Float> list) {
        float sum = 0;
        for (float val : list) {
            sum += val;
        }
        return sum / list.size();
    }

    private static float mode(List<Float> list) {
        Map<Integer, Integer> freqMap = new HashMap<>();
        for (float val : list) {
            int key = Math.round(val); // 四捨五入到整數
            freqMap.put(key, freqMap.getOrDefault(key, 0) + 1);
        }

        int maxFreq = 0;
        int modeVal = 0;
        for (Map.Entry<Integer, Integer> entry : freqMap.entrySet()) {
            if (entry.getValue() > maxFreq) {
                maxFreq = entry.getValue();
                modeVal = entry.getKey();
            }
        }
        return (float) modeVal;
    }
}
