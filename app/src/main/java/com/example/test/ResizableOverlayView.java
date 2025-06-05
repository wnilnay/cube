package com.example.test;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;

public class ResizableOverlayView extends View {
    private Paint maskPaint;
    private RectF rect;
    private float handleRadius = 20f;
    private int draggingHandle = -1; // 0: top-left, 1: bottom-right
    private float lastX, lastY;

    public ResizableOverlayView(Context context) {
        super(context);
        init();
    }

    public ResizableOverlayView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);
        rect.set(50, 50, ((View)getParent()).getWidth() / 2f, ((View)getParent()).getHeight() / 2f);
        //Log.d("wnilnay", "onLayout:");
    }

    private void init() {
        maskPaint = new Paint();
        //maskPaint.setColor(Color.parseColor("#88000000")); // 半透明黑
        maskPaint.setAlpha(0);
        rect = new RectF(0, 0, 0, 0); // 預設遮罩區域
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        canvas.drawRect(rect, maskPaint);

        // 左上角控制點
        canvas.drawCircle(rect.left, rect.top, handleRadius, getHandlePaint());
        // 右下角控制點
        canvas.drawCircle(rect.right, rect.bottom, handleRadius, getHandlePaint());

        Paint borderPaint = new Paint();
        borderPaint.setColor(Color.BLACK); // 外框與輔助線
        borderPaint.setStyle(Paint.Style.STROKE);
        borderPaint.setStrokeWidth(4);

        Paint linePaint = new Paint();
        linePaint.setColor(Color.BLACK);
        linePaint.setStyle(Paint.Style.STROKE);
        linePaint.setStrokeWidth(2);
        linePaint.setAlpha(150);  // 半透明輔助線

        Paint overlayPaint = new Paint();
        overlayPaint.setColor(Color.BLACK);
        overlayPaint.setAlpha(150);

        // 遮罩四個區域
        canvas.drawRect(0, 0, getWidth(), rect.top, overlayPaint);           // 上
        canvas.drawRect(0, rect.bottom, getWidth(), getHeight(), overlayPaint);  // 下
        canvas.drawRect(0, rect.top, rect.left, rect.bottom, overlayPaint);      // 左
        canvas.drawRect(rect.right, rect.top, getWidth(), rect.bottom, overlayPaint); // 右

        canvas.drawRect(rect, borderPaint);

        float thirdWidth = rect.width() / 3f;
        float thirdHeight = rect.height() / 3f;

        // 垂直分割線
        canvas.drawLine(rect.left + thirdWidth, rect.top, rect.left + thirdWidth, rect.bottom, linePaint);
        canvas.drawLine(rect.left + 2 * thirdWidth, rect.top, rect.left + 2 * thirdWidth, rect.bottom, linePaint);

        // 水平分割線
        canvas.drawLine(rect.left, rect.top + thirdHeight, rect.right, rect.top + thirdHeight, linePaint);
        canvas.drawLine(rect.left, rect.top + 2 * thirdHeight, rect.right, rect.top + 2 * thirdHeight, linePaint);
    }

    private Paint getHandlePaint() {
        Paint p = new Paint();
        p.setColor(Color.GRAY);
        p.setAlpha(200);
        p.setStyle(Paint.Style.FILL);
        return p;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        float x = event.getX();
        float y = event.getY();

        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                if (isInHandle(x, y, rect.left, rect.top)) {
                    draggingHandle = 0;
                } else if (isInHandle(x, y, rect.right, rect.bottom)) {
                    draggingHandle = 1;
                }
                lastX = x;
                lastY = y;
                break;

            case MotionEvent.ACTION_MOVE:
                if (draggingHandle == 0) { // 拖左上角
                    if (x < rect.right - handleRadius * 2 && y < rect.bottom - handleRadius * 2) {
                        rect.left = x;
                        rect.top = y;
                    }
                } else if (draggingHandle == 1) { // 拖右下角
                    if (x > rect.left + handleRadius * 2 && y > rect.top + handleRadius * 2) {
                        rect.right = x;
                        rect.bottom = y;
                    }
                }
                invalidate();
                break;

            case MotionEvent.ACTION_UP:
                draggingHandle = -1;
                break;
        }

        return true;
    }

    private boolean isInHandle(float x, float y, float hx, float hy) {
        return Math.hypot(x - hx, y - hy) <= handleRadius * 1.5;
    }

    public RectF getMaskRect() {
        return rect;
    }

    public static RectF mapRectFromViewToBitmap(RectF maskRectInView, ImageView imageView, Bitmap bitmap) {
        Drawable drawable = imageView.getDrawable();
        if (drawable == null) return null;

        // 1. 取得 Bitmap 在 ImageView 中實際顯示的範圍
        Matrix matrix = imageView.getImageMatrix();
        RectF displayedBitmapRect = new RectF(0, 0, drawable.getIntrinsicWidth(), drawable.getIntrinsicHeight());
        matrix.mapRect(displayedBitmapRect); // 映射到 View 空間中

        // 2. 計算縮放比
        float scaleX = (float) bitmap.getWidth() / displayedBitmapRect.width();
        float scaleY = (float) bitmap.getHeight() / displayedBitmapRect.height();

        // 3. 轉換遮罩座標到 Bitmap 上
        float left   = (maskRectInView.left   - displayedBitmapRect.left) * scaleX;
        float top    = (maskRectInView.top    - displayedBitmapRect.top) * scaleY;
        float right  = (maskRectInView.right  - displayedBitmapRect.left) * scaleX;
        float bottom = (maskRectInView.bottom - displayedBitmapRect.top) * scaleY;

        return new RectF(left, top, right, bottom);
    }

    public static RectF mapRectFromBitmapToView(RectF rectInBitmap, ImageView imageView, int imageWidth, int imageHeight) {
        int viewWidth = imageView.getWidth();
        int viewHeight = imageView.getHeight();
        if (viewWidth == 0 || viewHeight == 0) return null;

        // 1. 計算縮放比
        float scale = Math.min((float) viewWidth / imageWidth, (float) viewHeight / imageHeight);

        // 2. 計算圖片在 View 中的實際顯示區域（置中）
        float displayWidth = imageWidth * scale;
        float displayHeight = imageHeight * scale;
        float dx = (viewWidth - displayWidth) / 2f;
        float dy = (viewHeight - displayHeight) / 2f;

        // 3. 將 Bitmap 座標轉成 View 座標
        float left = rectInBitmap.left * scale + dx;
        float top = rectInBitmap.top * scale + dy;
        float right = rectInBitmap.right * scale + dx;
        float bottom = rectInBitmap.bottom * scale + dy;

        return new RectF(left, top, right, bottom);
    }

    public static RectF mapRectFromBitmapToView(RectF rectInBitmap, ImageView imageView, Bitmap bitmap) {
        Drawable drawable = imageView.getDrawable();
        if (drawable == null) return null;

        // 1. 取得 Bitmap 在 ImageView 中實際顯示的範圍
        Matrix matrix = imageView.getImageMatrix();
        RectF displayedBitmapRect = new RectF(0, 0, drawable.getIntrinsicWidth(), drawable.getIntrinsicHeight());
        matrix.mapRect(displayedBitmapRect); // 映射到 View 空間中

        // 2. 計算縮放比（反過來用）
        float scaleX = displayedBitmapRect.width() / bitmap.getWidth();
        float scaleY = displayedBitmapRect.height() / bitmap.getHeight();

        // 3. 將 Bitmap 上的座標轉換到 View 上
        float left   = rectInBitmap.left   * scaleX + displayedBitmapRect.left;
        float top    = rectInBitmap.top    * scaleY + displayedBitmapRect.top;
        float right  = rectInBitmap.right  * scaleX + displayedBitmapRect.left;
        float bottom = rectInBitmap.bottom * scaleY + displayedBitmapRect.top;

        return new RectF(left, top, right, bottom);
    }


}

