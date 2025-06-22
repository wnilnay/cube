package com.example.test;

import android.content.Context;
import android.opengl.GLSurfaceView;
import android.util.AttributeSet;
import android.view.MotionEvent;

public class RubiksCube3DView extends GLSurfaceView {
    private final RubiksCubeRenderer renderer;
    private final float TOUCH_SCALE_FACTOR = 180.0f / 320;
    private float previousX;
    private float previousY;
    // 標記本次觸控是否為拖曳
    private boolean isDragging = false;

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
                    // 強制重新渲染以顯示高亮和調試點
                    requestRender();
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