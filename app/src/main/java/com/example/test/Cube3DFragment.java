package com.example.test;

import android.content.Context;
import android.opengl.GLSurfaceView;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.Toast;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

public class Cube3DFragment extends Fragment {
    private RubiksCube3DView mGLView;
    private TextView debugTextView;
    private Button testButton;
    private Button testFaceButton;
    private Button debugPointButton;
    private Button resetButton;
    private RubiksCubeRenderer renderer;
    private boolean debugPointVisible = false;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_cube_3d, container, false);
        mGLView = view.findViewById(R.id.cube_view);
        debugTextView = view.findViewById(R.id.debug_text_view);
        testButton = view.findViewById(R.id.test_button);
        testFaceButton = view.findViewById(R.id.test_face_button);
        debugPointButton = view.findViewById(R.id.debug_point_button);
        resetButton = view.findViewById(R.id.reset_button);
        
        // 獲取渲染器引用
        renderer = mGLView.getRenderer();
        
        // 設置調試模式
        if (renderer != null) {
            renderer.setDebugMode(true);
        }
        
        // 設置測試按鈕點擊事件
        testButton.setOnClickListener(v -> {
            if (renderer != null) {
                renderer.testClickDetection();
                Toast.makeText(getContext(), "測試完成，請查看日誌", Toast.LENGTH_SHORT).show();
            }
        });
        
        // 設置面檢測測試按鈕點擊事件
        testFaceButton.setOnClickListener(v -> {
            if (renderer != null) {
                renderer.testSpecificFaceDetection();
                Toast.makeText(getContext(), "面檢測測試完成，請查看日誌", Toast.LENGTH_SHORT).show();
            }
        });
        
        // 設置調試點控制按鈕點擊事件
        debugPointButton.setOnClickListener(v -> {
            if (renderer != null) {
                if (debugPointVisible) {
                    renderer.clearDebugPoint();
                    debugPointButton.setText("顯示調試點");
                    debugPointVisible = false;
                }
                else {
                    renderer.setDebugPointDisplay(true);
                    debugPointButton.setText("清除調試點");
                    debugPointVisible = true;
                }
            }
        });
        
        // 設置重置按鈕點擊事件
        resetButton.setOnClickListener(v -> {
            if (renderer != null) {
                renderer.resetToInitialState();
                Toast.makeText(getContext(), "已重置到初始狀態", Toast.LENGTH_SHORT).show();
            }
        });
        

        
        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        // 定期更新調試信息
        updateDebugInfo();
    }

    @Override
    public void onPause() {
        super.onPause();
        if (mGLView != null) {
            mGLView.onPause();
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if (mGLView != null) {
            mGLView.onResume();
        }
    }

    private void updateDebugInfo() {
        if (renderer != null && debugTextView != null) {
            String touchInfo = renderer.getTouchInfo();
            String debugInfo = renderer.getDebugInfo();
            String highlightInfo = renderer.getCurrentHighlightInfo();
            String angleInfo = String.format("旋轉角度: X=%.1f°, Y=%.1f°", 
                renderer.getAngleX(), renderer.getAngleY());
            
            String fullInfo = touchInfo + "\n" + highlightInfo + "\n" + angleInfo;
            debugTextView.setText(fullInfo);
        }
        
        // 每秒更新一次
        if (getView() != null) {
            getView().postDelayed(this::updateDebugInfo, 100);
        }
    }

    // 新增：獲取渲染器的方法
    public RubiksCubeRenderer getRenderer() {
        return renderer;
    }
}