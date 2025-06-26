package com.example.test;

import android.content.Context;
import android.opengl.GLSurfaceView;
import android.util.AttributeSet;
import android.view.MotionEvent;

import androidx.annotation.Nullable;

public class RubiksCube3DView extends GLSurfaceView {
    private final RubiksCubeRenderer renderer;
    private final float TOUCH_SCALE_FACTOR = 180.0f / 320;
    private float previousX;
    private float previousY;
    // 標記本次觸控是否為拖曳
    private boolean isDragging = false;

    // 回調介面：回傳 faceIndex 與 cellIndex，未命中則為 -1
    public interface OnHighlightListener {
        void onHighlight(int faceIndex, int cellIndex);
    }
    private @Nullable OnHighlightListener highlightListener;

    public void setOnHighlightListener(@Nullable OnHighlightListener listener) {
        this.highlightListener = listener;
    }

    // ===== 旋轉角度回調 =====
    public interface OnRotationChangedListener{
        void onRotationChanged(float angleX, float angleY);
    }
    private @Nullable OnRotationChangedListener rotationListener;
    public void setOnRotationChangedListener(@Nullable OnRotationChangedListener l){
        this.rotationListener = l;
    }

    public RubiksCube3DView(Context context) {
        this(context, null);
    }

    public RubiksCube3DView(Context context, AttributeSet attrs) {
        super(context, attrs);

        // 設置OpenGL ES 2.0 context
        setEGLContextClientVersion(2);

        renderer = new RubiksCubeRenderer(this);
        setRenderer(renderer);

        // 僅在用戶交互時渲染視圖
        setRenderMode(GLSurfaceView.RENDERMODE_WHEN_DIRTY);
    }

    @Override
    public boolean onTouchEvent(MotionEvent e) {
        float x = e.getX();
        float y = e.getY();

        switch (e.getAction()) {
            case MotionEvent.ACTION_DOWN:
                // 記錄起始觸摸點
                previousX = x;
                previousY = y;
                isDragging = false;
                // 請求父容器不要攔截觸摸事件
                getParent().requestDisallowInterceptTouchEvent(true);
                break;
                
            case MotionEvent.ACTION_MOVE:
                float dx = y - previousY;
                float dy = x - previousX;
                
                // 如果有明顯的滑動距離，則處理魔方旋轉
                if (Math.abs(dx) > 5 || Math.abs(dy) > 5) {
                    // 呼叫以世界座標為基準的旋轉
                    renderer.applyRotation(dx * TOUCH_SCALE_FACTOR, dy * TOUCH_SCALE_FACTOR);
                    if(rotationListener!=null){
                        rotationListener.onRotationChanged(renderer.getAngleX(), renderer.getAngleY());
                    }
                    requestRender();
                    // 阻止父容器攔截觸摸事件
                    getParent().requestDisallowInterceptTouchEvent(true);
                    isDragging = true; // 已發生足夠移動，視為拖曳
                }
                break;

            case MotionEvent.ACTION_UP:
                // 只有在非拖曳手勢時才視為點擊
                if (!isDragging) {
                    renderer.handleClick(x, y, getWidth(), getHeight());
                    // 解析目前高亮資訊
                    int faceIdx = -1;
                    int cellIdx = -1;
                    String info = renderer.getCurrentHighlightInfo();
                    if (!"無高亮".equals(info)) {
                        // 格式範例："當前高亮：2:4"
                        try {
                            String[] parts = info.split(":");
                            if (parts.length == 2) {
                                faceIdx = Integer.parseInt(parts[0].replaceAll("[^0-9]", "").trim());
                                cellIdx = Integer.parseInt(parts[1].trim());
                            }
                        } catch (Exception ignore) {}
                    }
                    if (highlightListener != null) {
                        highlightListener.onHighlight(faceIdx, cellIdx);
                    }
                    // 強制重新渲染以顯示高亮和調試點
                    requestRender();
                }else{
                    // 拖曳結束，回傳最終角度
                    if(rotationListener!=null){
                        rotationListener.onRotationChanged(renderer.getAngleX(), renderer.getAngleY());
                    }
                }
                // 允許父容器重新攔截觸摸事件
                getParent().requestDisallowInterceptTouchEvent(false);
                break;
                
            case MotionEvent.ACTION_CANCEL:
                // 允許父容器重新攔截觸摸事件
                getParent().requestDisallowInterceptTouchEvent(false);
                break;
        }

        previousX = x;
        previousY = y;
        return true;
    }

    // 新增：獲取渲染器的方法
    public RubiksCubeRenderer getRenderer() {
        return renderer;
    }
}