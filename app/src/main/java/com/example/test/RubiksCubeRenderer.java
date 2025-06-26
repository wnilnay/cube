package com.example.test;

import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.opengl.Matrix;
import android.util.Log;

import androidx.core.content.ContextCompat;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

import java.util.ArrayList;
import java.util.List;

public class RubiksCubeRenderer implements GLSurfaceView.Renderer {
    private final float[] mMVPMatrix = new float[16];
    private final float[] mProjectionMatrix = new float[16];
    private final float[] mViewMatrix = new float[16];
    private final float[] mRotationMatrix = new float[16];
    
    // 分離的旋轉矩陣，用於確保旋轉始終基於屏幕座標系
    private final float[] mRotationMatrixX = new float[16];
    private final float[] mRotationMatrixY = new float[16];

    // 新增：累積旋轉矩陣
    private final float[] mAccumulatedRotation = new float[16];
    private final float[] mTemporaryRotation = new float[16];

    // 追蹤實際的旋轉角度
    private float mAngleX = 0;
    private float mAngleY = 0;
    private float mAngleZ = 0;

    private CubeFace[] faces;
    private int width, height;
    
    // GLSurfaceView 引用，用於請求重新渲染
    private GLSurfaceView mSurfaceView;

    // 魔術方塊的6個面的顏色
    private final float[][] FACE_COLORS = {
            {1.0f, 1.0f, 1.0f, 1.0f}, // 白色 (上面 - Up)
            {0.0f, 0.0f, 1.0f, 1.0f}, // 藍色 (右面 - Right)
            {1.0f, 0.0f, 0.0f, 1.0f}, // 紅色 (前面 - Forward)
            {1.0f, 1.0f, 0.0f, 1.0f}, // 黃色 (下面 - Down)
            {0.0f, 1.0f, 0.0f, 1.0f}, // 綠色 (左面 - Left)
            {1.0f, 0.5f, 0.0f, 1.0f}  // 橙色 (後面 - Backward)
    };

    private final String[] FACE_NAMES = {"上面", "右面", "前面", "下面", "左面", "後面"};

    // 魔術方塊的尺寸（邊長為0.5，中心在原點）
    private static final float CUBE_SIZE = 0.5f;
    
    // 面的法向量（指向外部）
    private static final float[][] FACE_NORMALS = {
            {0, 1, 0},   // 上面 (Y軸正方向)
            {1, 0, 0},   // 右面 (X軸正方向)
            {0, 0, 1},   // 前面 (Z軸正方向)
            {0, -1, 0},  // 下面 (Y軸負方向)
            {-1, 0, 0},  // 左面 (X軸負方向)
            {0, 0, -1}   // 後面 (Z軸負方向)
    };
    
    // 當前高亮的格子
    private int currentHighlightedFace = -1;
    private int currentHighlightedRow = -1;
    private int currentHighlightedCol = -1;

    private static class ClickResult {
        int faceIndex;
        int gridX, gridY;
        float[] intersectionPoint;
        
        ClickResult(int faceIndex, int gridX, int gridY, float[] intersectionPoint) {
            this.faceIndex = faceIndex;
            this.gridX = gridX;
            this.gridY = gridY;
            this.intersectionPoint = intersectionPoint;
        }
    }

    // 新增：調試模式標誌
    private boolean debugMode = true;
    
    // 新增：調試信息
    private String lastDebugInfo = "";
    
    // 新增：調試圓點位置
    private float[] debugPoint = null;
    private boolean showDebugPoint = false;
    private String lastTouchInfo = "";

    private volatile boolean animationRunning = false;

    // ===== 新增：GL 初始化完成旗標 =====
    private volatile boolean initialized = false;

    private static final float BASE_Y = 180f; // 讓紅色面(前)朝前的基準角

    // 魔方六色的int常數（與color.xml一致）
    public static final int COLOR_WHITE  = 0xFFFFFFFF;
    public static final int COLOR_YELLOW = 0xFFFFEB3B;
    public static final int COLOR_GREEN  = 0xFF00FF00;
    public static final int COLOR_BLUE   = 0xFF0000FF;
    public static final int COLOR_RED    = 0xFFFF0000;
    public static final int COLOR_ORANGE = 0xFFFF9800;

    public RubiksCubeRenderer(GLSurfaceView surfaceView) {
        mSurfaceView = surfaceView;
        Matrix.setIdentityM(mAccumulatedRotation, 0);
        
        // 設置初始旋轉，讓紅色面（前面）朝前
        // 初始狀態：橙色面朝前，需要旋轉180度讓紅色面朝前
        // 但是我們需要確保旋轉方向正確
        Matrix.rotateM(mAccumulatedRotation, 0, 180, 0, 1, 0);
        
        // 更新角度變數
        mAngleY = 180;
        
        //Log.d("RubiksCube", "初始化完成: 角度Y=" + mAngleY);
    }

    @Override
    public void onSurfaceCreated(GL10 unused, EGLConfig config) {
        GLES20.glClearColor(0.0f, 0.0f, 0.0f, 1.0f);
        GLES20.glEnable(GLES20.GL_DEPTH_TEST);
        GLES20.glDisable(GLES20.GL_CULL_FACE); // 添加这一行来禁用背面剔除

        // 初始化魔術方塊的6個面
        faces = new CubeFace[6];

        // 創建各個面 (3x3網格)
        for (int faceIndex = 0; faceIndex < 6; faceIndex++) {
            faces[faceIndex] = new CubeFace(faceIndex);
        }

        initialized = true; // 標記 GL 物件已建立完成
    }

    @Override
    public void onDrawFrame(GL10 unused) {
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);

        // 設置相機位置
        Matrix.setLookAtM(mViewMatrix, 0, 0, 0, -2.7f, 0f, 0f, 0f, 0f, 1.0f, 0.0f);
        //Matrix.setLookAtM(mViewMatrix, 0, 0, 0, -3, 0f, 0f, 0f, 0f, 1.0f, 0.0f);

        // 直接用累積旋轉矩陣
        Matrix.multiplyMM(mMVPMatrix, 0, mProjectionMatrix, 0, mViewMatrix, 0);
        Matrix.multiplyMM(mMVPMatrix, 0, mMVPMatrix, 0, mAccumulatedRotation, 0);

        // 繪製所有面
        for (CubeFace face : faces) {
            face.draw(mMVPMatrix);
        }
    }

    @Override
    public void onSurfaceChanged(GL10 unused, int width, int height) {
        this.width = width;
        this.height = height;

        GLES20.glViewport(0, 0, width, height);

        //float ratio = (float) width / height;
        //Matrix.frustumM(mProjectionMatrix, 0, -ratio, ratio, -1, 1, 3, 7);

        float ratio = (float) width / height;
        Matrix.perspectiveM(mProjectionMatrix, 0,
                45f,     // 固定 FOV 45°
                ratio,   // 寬高比
                1.5f,    // near
                7.0f);   // far
    }

    public void setRotation(float angleX, float angleY) {
        mAngleX = angleX;
        mAngleY = angleY;
    }

    public float getAngleX() {
        return mAngleX;
    }

    public float getAngleY() {
        return mAngleY;
    }

    public float getAngleZ() {
        return mAngleZ;
    }

    public void handleClick(float x, float y, int screenWidth, int screenHeight) {
        //Log.d("RubiksCube", "開始點擊檢測: 屏幕(" + x + "," + y + ") 角度X=" + mAngleX + " 角度Y=" + mAngleY);
        
        // 清除之前的高亮
        clearCurrentHighlight();
        
        // 改進的點擊檢測 - 考慮相機位置和視角
        ClickResult result = performImprovedDetection(x, y, screenWidth, screenHeight);
        
        if (result != null) {
            String touchInfo = String.format("觸摸點: (%.3f,%.3f)", x, y);
            lastTouchInfo = touchInfo;

            String debugInfo = String.format("檢測成功: 面=%s(index=%d), 格子=(%d,%d), 交點=(%.3f,%.3f,%.3f)", 
                FACE_NAMES[result.faceIndex], result.faceIndex, result.gridX, result.gridY, 
                result.intersectionPoint[0], result.intersectionPoint[1], result.intersectionPoint[2]);
            
            //Log.d("RubiksCube", debugInfo);
            lastDebugInfo = debugInfo;
            
            currentHighlightedFace = result.faceIndex;
            currentHighlightedRow = result.gridY;
            currentHighlightedCol = result.gridX;
            
            // 設置新的高亮
            //setHighlight(result.faceIndex, result.gridY, result.gridX);
            
        } else {
            String debugInfo = "檢測失敗: 未找到有效的交點";
            //Log.d("RubiksCube", debugInfo);
            lastDebugInfo = debugInfo;
        }
    }
    
    private ClickResult performImprovedDetection(float x, float y, int screenWidth, int screenHeight) {
        // 將屏幕座標轉換為標準化設備座標 (-1 到 1)
        float normalizedX = (2.0f * x / screenWidth) - 1.0f;
        float normalizedY = 1.0f - (2.0f * y / screenHeight);
        
        //Log.d("RubiksCube", "標準化座標: (" + normalizedX + "," + normalizedY + ")");
        
        // 使用 VP 反矩陣將螢幕點回推到世界座標，生成精確射線
        float[] vpMatrix = new float[16];
        float[] invVP = new float[16];
        Matrix.multiplyMM(vpMatrix, 0, mProjectionMatrix, 0, mViewMatrix, 0);
        Matrix.invertM(invVP, 0, vpMatrix, 0);

        // 近、遠端 NDC
        float[] nearPointNDC = { normalizedX, normalizedY, -1.0f, 1.0f };
        float[] farPointNDC  = { normalizedX, normalizedY,  1.0f, 1.0f };

        float[] nearPointWorld = new float[4];
        float[] farPointWorld  = new float[4];
        Matrix.multiplyMV(nearPointWorld, 0, invVP, 0, nearPointNDC, 0);
        Matrix.multiplyMV(farPointWorld, 0, invVP, 0, farPointNDC, 0);

        // 透視除法 (除以 w)
        for (int i = 0; i < 3; i++) {
            nearPointWorld[i] /= nearPointWorld[3];
            farPointWorld[i]  /= farPointWorld[3];
        }

        float[] rayStart = { nearPointWorld[0], nearPointWorld[1], nearPointWorld[2] };
        float[] rayDirection = { farPointWorld[0] - rayStart[0],
                                 farPointWorld[1] - rayStart[1],
                                 farPointWorld[2] - rayStart[2] };

        // 正規化射線方向
        float length = (float) Math.sqrt(rayDirection[0]*rayDirection[0] +
                                         rayDirection[1]*rayDirection[1] +
                                         rayDirection[2]*rayDirection[2]);
        rayDirection[0] /= length;
        rayDirection[1] /= length;
        rayDirection[2] /= length;
        
        //Log.d("RubiksCube", "射線起點: (" + rayStart[0] + "," + rayStart[1] + "," + rayStart[2] + ")");
        //Log.d("RubiksCube", "射線方向: (" + rayDirection[0] + "," + rayDirection[1] + "," + rayDirection[2] + ")");
        
        // 應用旋轉矩陣的逆矩陣到射線
        float[] invRotation = new float[16];
        Matrix.invertM(invRotation, 0, mAccumulatedRotation, 0);
        
        float[] transformedRayDirection = new float[4];
        float[] rayDir4 = {rayDirection[0], rayDirection[1], rayDirection[2], 0.0f};
        Matrix.multiplyMV(transformedRayDirection, 0, invRotation, 0, rayDir4, 0);
        
        float[] dir = {transformedRayDirection[0], transformedRayDirection[1], transformedRayDirection[2]};
        
        // 射線起點也需要變換
        float[] transformedRayStart = new float[4];
        float[] rayStart4 = {rayStart[0], rayStart[1], rayStart[2], 1.0f};
        Matrix.multiplyMV(transformedRayStart, 0, invRotation, 0, rayStart4, 0);
        float[] start = {transformedRayStart[0], transformedRayStart[1], transformedRayStart[2]};
        
        //Log.d("RubiksCube", "變換後射線起點: (" + start[0] + "," + start[1] + "," + start[2] + ")");
        //Log.d("RubiksCube", "變換後射線方向: (" + dir[0] + "," + dir[1] + "," + dir[2] + ")");
        
        // 與立方體的6個面進行相交檢測
        float closestDistance = Float.MAX_VALUE;
        int bestFace = -1;
        float[] bestIntersection = null;
        
        //Log.d("RubiksCube", "開始面檢測...");
        
        for (int faceIndex = 0; faceIndex < 6; faceIndex++) {
            float[] normal = FACE_NORMALS[faceIndex];
            float[] planePoint = {normal[0] * CUBE_SIZE, normal[1] * CUBE_SIZE, normal[2] * CUBE_SIZE};
            
            // 計算射線與平面的交點
            float denom = dir[0] * normal[0] + dir[1] * normal[1] + dir[2] * normal[2];
            
            //Log.d("RubiksCube", String.format("面%d (%s): 法向量=(%.3f,%.3f,%.3f), 平面點=(%.3f,%.3f,%.3f), denom=%.6f",
            //    faceIndex, FACE_NAMES[faceIndex],
            //    normal[0], normal[1], normal[2],
            //    planePoint[0], planePoint[1], planePoint[2], denom));
            
            if (Math.abs(denom) > 0.0001f) {
                float t = ((planePoint[0] - start[0]) * normal[0] + 
                          (planePoint[1] - start[1]) * normal[1] + 
                          (planePoint[2] - start[2]) * normal[2]) / denom;
                
                //Log.d("RubiksCube", String.format("面%d: t=%.6f", faceIndex, t));
                
                if (t > 0) {
                    float[] intersection = {
                        start[0] + t * dir[0],
                        start[1] + t * dir[1],
                        start[2] + t * dir[2]
                    };
                    
                    //Log.d("RubiksCube", String.format("面%d: 交點=(%.3f,%.3f,%.3f)",
                    //    faceIndex, intersection[0], intersection[1], intersection[2]));
                    
                    // 檢查交點是否在立方體表面內
                    if (isPointOnCubeFace(intersection, faceIndex)) {
                        //Log.d("RubiksCube", String.format("面%d: 交點在面內，距離=%.6f", faceIndex, t));
                        if (t < closestDistance) {
                            closestDistance = t;
                            bestFace = faceIndex;
                            bestIntersection = intersection;
                            //Log.d("RubiksCube", String.format("面%d: 更新為最佳面", faceIndex));
                        }
                    } else {
                        //Log.d("RubiksCube", String.format("面%d: 交點不在面內", faceIndex));
                    }
                } else {
                    //Log.d("RubiksCube", String.format("面%d: t <= 0，跳過", faceIndex));
                }
            } else {
                //Log.d("RubiksCube", String.format("面%d: denom太小，跳過", faceIndex));
            }
        }
        
        if (bestFace >= 0) {
            //Log.d("RubiksCube", "最終檢測結果: 面=" + FACE_NAMES[bestFace] +
            //        ", 交點: (" + bestIntersection[0] + "," + bestIntersection[1] + "," + bestIntersection[2] + ")");
            
            // 計算格子座標
            int[] gridCoords = calculateGridCoordinates(bestIntersection, bestFace);
            
            return new ClickResult(bestFace, gridCoords[0], gridCoords[1], bestIntersection);
        }
        
        //Log.d("RubiksCube", "沒有找到有效的交點");
        return null;
    }
    
    private boolean isPointOnCubeFace(float[] point, int faceIndex) {
        // 檢查點是否在立方體表面內
        float tolerance = 0.01f;
        
        // 首先檢查點是否在正確的面上
        float[] normal = FACE_NORMALS[faceIndex];
        float distance = Math.abs(point[0] * normal[0] + point[1] * normal[1] + point[2] * normal[2] - CUBE_SIZE);
        
        if (distance > tolerance) {
            return false;
        }
        
        // 然後檢查點是否在面的邊界內
        switch (faceIndex) {
            case 0: // 上面 (Y = 1)
            case 3: // 下面 (Y = -1)
                return Math.abs(point[0]) <= CUBE_SIZE && Math.abs(point[2]) <= CUBE_SIZE;
            case 1: // 右面 (X = 1)
            case 4: // 左面 (X = -1)
                return Math.abs(point[1]) <= CUBE_SIZE && Math.abs(point[2]) <= CUBE_SIZE;
            case 2: // 前面 (Z = -1)
            case 5: // 後面 (Z = 1)
                return Math.abs(point[0]) <= CUBE_SIZE && Math.abs(point[1]) <= CUBE_SIZE;
        }
        
        return false;
    }

    // 新增：以世界座標為基準的旋轉
    public void applyRotation(float dx, float dy) {
        // 更新角度變數
        mAngleX += dx;
        mAngleY += dy;
        
        // 正規化角度
        mAngleX = ((mAngleX % 360) + 360) % 360;
        mAngleY = ((mAngleY % 360) + 360) % 360;
        
        Matrix.setIdentityM(mTemporaryRotation, 0);
        Matrix.rotateM(mTemporaryRotation, 0, -dx, 1, 0, 0); // 上下（反轉方向）
        Matrix.rotateM(mTemporaryRotation, 0, dy, 0, 1, 0); // 左右
        float[] result = new float[16];
        Matrix.multiplyMM(result, 0, mTemporaryRotation, 0, mAccumulatedRotation, 0);
        System.arraycopy(result, 0, mAccumulatedRotation, 0, 16);
    }

    private int[] calculateGridCoordinates(float[] point, int faceIndex) {
        // 將3D點轉換為面的2D座標，然後計算格子位置
        float u, v;
        
        //Log.d("RubiksCube", "計算格子座標: 點(" + point[0] + "," + point[1] + "," + point[2] + ") 面=" + FACE_NAMES[faceIndex]);
        
        switch (faceIndex) {
            case 0: // 上面 (Y = 1)
                u = point[0];  // X座標
                v = -point[2]; // -Z座標
                break;
            case 1: // 右面 (X = 1)
                u = -point[2]; // -Z座標
                v = point[1];  // Y座標
                break;
            case 2: // 前面 (Z = -1)
                u = point[0];  // X座標
                v = point[1];  // Y座標
                break;
            case 3: // 下面 (Y = -1)
                u = -point[0]; // -X座標
                v = -point[2]; // -Z座標
                break;
            case 4: // 左面 (X = -1)
                u = point[2];  // Z座標
                v = point[1];  // Y座標
                break;
            case 5: // 後面 (Z = 1)
                u = -point[0]; // -X座標
                v = point[1];  // Y座標
                break;
            default:
                return new int[]{0, 0};
        }
        
        //Log.d("RubiksCube", "2D座標: u=" + u + " v=" + v);
        
        // 修正的格子座標計算
        // 座標範圍是 [-1, 1]，需要轉換為 [0, 2]
        
        // 將座標從 [-1, 1] 轉換為 [0, 3]
        float normalizedU = (u + CUBE_SIZE) / (2.0f * CUBE_SIZE) * 3.0f;
        float normalizedV = (v + CUBE_SIZE) / (2.0f * CUBE_SIZE) * 3.0f;
        
        // 計算格子索引
        int gridX = (int) Math.floor(normalizedU);
        int gridY = (int) Math.floor(normalizedV);
        
        // 只有 Up(0) 與 Down(3) 需要翻轉 Y
        if (faceIndex == 0 || faceIndex == 3) {
            gridY = 2 - gridY;
        }
        
        // 確保在有效範圍內
        gridX = Math.max(0, Math.min(2, gridX));
        gridY = Math.max(0, Math.min(2, gridY));
        
        // 依面向修正左右顛倒問題
        switch (faceIndex) {
            case 3: // 下面
            case 4: // 左面
            case 5: // 後面
                gridX = 2 - gridX;
                break;
        }
        
        //Log.d("RubiksCube", "修正後格子座標: (" + gridX + "," + gridY + ")");
        
        return new int[]{gridX, gridY};
    }

    // 清除當前高亮
    private void clearCurrentHighlight() {
        if (currentHighlightedFace >= 0 && currentHighlightedRow >= 0 && currentHighlightedCol >= 0) {
            if (currentHighlightedFace < faces.length) {
                faces[currentHighlightedFace].setHighlight(currentHighlightedRow, currentHighlightedCol, false);
            }
        }
        currentHighlightedFace = -1;
        currentHighlightedRow = -1;
        currentHighlightedCol = -1;
    }
    
    // 設置高亮
    private void setHighlight(int faceIndex, int row, int col) {
        if (faceIndex >= 0 && faceIndex < faces.length && row >= 0 && row < 3 && col >= 0 && col < 3) {
            faces[faceIndex].setHighlight(row, col, true);
            currentHighlightedFace = faceIndex;
            currentHighlightedRow = row;
            currentHighlightedCol = col;
            
            //Log.d("RubiksCube", "設置高亮: 面=" + FACE_NAMES[faceIndex] +
            //        ", 格子=(" + row + "," + col + ")");
            
            // 強制重新渲染
            requestRender();
        }
    }
    
    // 請求重新渲染
    private void requestRender() {
        // 通知 GLSurfaceView 重新渲染
        if (mSurfaceView != null) {
            mSurfaceView.requestRender();
        }
    }
    
    // 新增：獲取調試信息
    public String getDebugInfo() {
        return lastDebugInfo;
    }
    public String getTouchInfo() {
        return lastTouchInfo;
    }
    
    // 新增：設置調試模式
    public void setDebugMode(boolean enabled) {
        debugMode = enabled;
    }
    
    // 新增：獲取當前高亮信息
    public String getCurrentHighlightInfo() {
        if (currentHighlightedFace >= 0) {
            int row = currentHighlightedRow;
            int col = currentHighlightedCol;
            int cellIndex = convertRowColToCellIndex(currentHighlightedFace, row, col);

            return String.format("當前高亮：%d:%d", currentHighlightedFace, cellIndex);
        }
        return "無高亮";
    }
    
    // 新增：清除調試點
    public void clearDebugPoint() {
        showDebugPoint = false;
        debugPoint = null;
        requestRender();
    }
    
    // 新增：設置調試點顯示
    public void setDebugPointDisplay(boolean show) {
        showDebugPoint = show;
        requestRender();
    }
    
    // 新增：重置到初始狀態
    public void resetToInitialState() {
        //Log.d("RubiksCube", "重置到初始狀態...");
        
        // 重置旋轉矩陣
        Matrix.setIdentityM(mAccumulatedRotation, 0);
        
        // 設置初始旋轉，讓紅色面（前面）朝前
        Matrix.rotateM(mAccumulatedRotation, 0, 180, 0, 1, 0);
        
        // 重置角度變數
        mAngleX = 0;
        mAngleY = 180;
        
        // 清除高亮和調試點
        clearCurrentHighlight();
        clearDebugPoint();
        
        //Log.d("RubiksCube", "重置完成: 角度X=" + mAngleX + ", 角度Y=" + mAngleY);
        
        // 強制重新渲染
        requestRender();
    }
    
    // 新增：測試點擊檢測的方法
    public void testClickDetection() {
        //Log.d("RubiksCube", "開始測試點擊檢測...");
        
        // 測試屏幕中心點擊
        int screenWidth = 1080; // 假設屏幕寬度
        int screenHeight = 1920; // 假設屏幕高度
        
        float centerX = screenWidth / 2.0f;
        float centerY = screenHeight / 2.0f;
        
        //Log.d("RubiksCube", "測試中心點擊: (" + centerX + "," + centerY + ")");
        handleClick(centerX, centerY, screenWidth, screenHeight);
        
        // 測試四個角落
        float[][] corners = {
            {100, 100},           // 左上
            {screenWidth - 100, 100}, // 右上
            {100, screenHeight - 100}, // 左下
            {screenWidth - 100, screenHeight - 100} // 右下
        };
        
        for (int i = 0; i < corners.length; i++) {
            //Log.d("RubiksCube", "測試角落 " + (i + 1) + ": (" + corners[i][0] + "," + corners[i][1] + ")");
            handleClick(corners[i][0], corners[i][1], screenWidth, screenHeight);
        }
    }
    
    // 新增：測試特定面的檢測
    public void testSpecificFaceDetection() {
        //Log.d("RubiksCube", "開始測試特定面檢測...");
        
        // 測試每個面的法向量和平面點
        for (int faceIndex = 0; faceIndex < 6; faceIndex++) {
            float[] normal = FACE_NORMALS[faceIndex];
            float[] planePoint = {normal[0] * CUBE_SIZE, normal[1] * CUBE_SIZE, normal[2] * CUBE_SIZE};
            
            //Log.d("RubiksCube", String.format("面%d (%s): 法向量=(%.1f,%.1f,%.1f), 平面點=(%.1f,%.1f,%.1f)",
            //    faceIndex, FACE_NAMES[faceIndex],
            //    normal[0], normal[1], normal[2],
            //    planePoint[0], planePoint[1], planePoint[2]));
        }
        
        // 測試射線與每個面的相交
        float[] testRayStart = {0, 0, -4};
        float[] testRayDir = {0, 0, 1}; // 指向Z軸正方向
        
        for (int faceIndex = 0; faceIndex < 6; faceIndex++) {
            float[] normal = FACE_NORMALS[faceIndex];
            float[] planePoint = {normal[0] * CUBE_SIZE, normal[1] * CUBE_SIZE, normal[2] * CUBE_SIZE};
            
            float denom = testRayDir[0] * normal[0] + testRayDir[1] * normal[1] + testRayDir[2] * normal[2];
            
            if (Math.abs(denom) > 0.0001f) {
                float t = ((planePoint[0] - testRayStart[0]) * normal[0] + 
                          (planePoint[1] - testRayStart[1]) * normal[1] + 
                          (planePoint[2] - testRayStart[2]) * normal[2]) / denom;
                
                if (t > 0) {
                    float[] intersection = {
                        testRayStart[0] + t * testRayDir[0],
                        testRayStart[1] + t * testRayDir[1],
                        testRayStart[2] + t * testRayDir[2]
                    };
                    
                    //Log.d("RubiksCube", String.format("面%d (%s): 交點=(%.3f,%.3f,%.3f), 距離=%.3f",
                    //    faceIndex, FACE_NAMES[faceIndex],
                    //    intersection[0], intersection[1], intersection[2], t));
                }
            }
        }
        
        // 新增：測試面的邊界檢查
        //Log.d("RubiksCube", "測試面的邊界檢查...");
        float[][] testPoints = {
            // 前面 (Z = -1) 的測試點
            {0, 0, -1},   // 中心
            {0.5f, 0, -1}, // 右邊
            {-0.5f, 0, -1}, // 左邊
            {0, 0.5f, -1}, // 上邊
            {0, -0.5f, -1}, // 下邊
            
            // 後面 (Z = 1) 的測試點
            {0, 0, 1},    // 中心
            {0.5f, 0, 1},  // 右邊
            {-0.5f, 0, 1}, // 左邊
            {0, 0.5f, 1},  // 上邊
            {0, -0.5f, 1}  // 下邊
        };
        
        for (int faceIndex = 2; faceIndex <= 5; faceIndex++) { // 只測試前面和後面
            //Log.d("RubiksCube", "測試面" + faceIndex + " (" + FACE_NAMES[faceIndex] + ") 的邊界檢查:");
            for (int i = 0; i < testPoints.length; i++) {
                boolean isOnFace = isPointOnCubeFace(testPoints[i], faceIndex);
                //Log.d("RubiksCube", String.format("  點%d (%.1f,%.1f,%.1f): %s",
                //    i, testPoints[i][0], testPoints[i][1], testPoints[i][2],
                //    isOnFace ? "在面內" : "不在面內"));
            }
        }
    }

    // 新增：工具方法 —— row/col 與 cellIndex 互轉
    private int convertRowColToCellIndex(int faceIndex,int row,int col){
        switch(faceIndex){
            case 0: // Up
                return row*3+col;
            case 1: // Right
            case 2: // Forward
            case 3: // Down
                return (2-row)*3+col;
            case 4: // Left
            case 5: // Backward
                return (2-row)*3+(2-col);
            default:
                return row*3+col;
        }
    }

    private int[] convertCellIndexToRowCol(int faceIndex,int cellIndex){
        int row = cellIndex/3;
        int col = cellIndex%3;
        switch(faceIndex){
            case 0: // Up : direct
                return new int[]{row,col};
            case 1: // Right / Forward / Down : flip row
            case 2:
            case 3:
                return new int[]{2-row,col};
            case 4: // Left / Backward : flip row and col
            case 5:
                return new int[]{2-row,2-col};
            default:
                return new int[]{row,col};
        }
    }

    // 更新：以 faceIndex + cellIndex 設定單格顏色（正確方向）
    public void setCellColor(int faceIndex,int cellIndex,float[] color){
        if (faces==null||faceIndex<0||faceIndex>=faces.length) return;
        if (cellIndex<0||cellIndex>8) return;
        int[] rc = convertCellIndexToRowCol(faceIndex,cellIndex);
        faces[faceIndex].setCellColor(rc[0],rc[1],color);
        requestRender();
    }

    // 直接設定到指定角度（重設矩陣）
    private void setOrientation(float angleX,float angleY){
        Matrix.setIdentityM(mAccumulatedRotation,0);
        Matrix.rotateM(mAccumulatedRotation,0,angleY,0,1,0);
        Matrix.rotateM(mAccumulatedRotation,0,-angleX,1,0,0); // 與 applyRotation 方向一致
        mAngleX = angleX;
        mAngleY = angleY;
        requestRender();
    }

    // 新增：設定 Pitch/Yaw/Roll
    private void setOrientation(float angleX,float angleY,float angleZ){
        Matrix.setIdentityM(mAccumulatedRotation,0);
        Matrix.rotateM(mAccumulatedRotation,0,angleY,0,1,0);
        Matrix.rotateM(mAccumulatedRotation,0,-angleX,1,0,0);
        Matrix.rotateM(mAccumulatedRotation,0,angleZ,0,0,1);
        mAngleX = angleX;
        mAngleY = angleY;
        mAngleZ = angleZ;
        requestRender();
    }

    // 調適用：直接設置三軸，不動畫
    public void setOrientationImmediate(float x,float y,float z){
        setOrientation(x,y,z);
    }

    // 對外提供：旋轉到指定面的視角
    public void rotateToFace(char faceCode, boolean animate){
        float targetX = 0f;
        float targetY = BASE_Y; // 以基準 180 度為前面
        switch(Character.toUpperCase(faceCode)){
            case 'U': // 上面
                targetX = -90f;
                break;
            case 'D': // 下面
                targetX = 90f;
                break;
            case 'R': // 右面
                targetY = BASE_Y - 90f;
                break;
            case 'L': // 左面
                targetY = BASE_Y + 90f;
                break;
            case 'F': // 前面
                // targetY 維持 180
                break;
            case 'B': // 後面 -> 0 度
                targetY = 0f;
                break;
        }
        // 正規化到 [0,360)
        targetY = ((targetY % 360)+360)%360;
        targetX = ((targetX % 360)+360)%360;

        // 微小偏移，避免完全平視導致深度感不足
        final float OFFSET = 5f;
        if(faceCode=='U' || faceCode=='D') targetY += OFFSET;
        else targetX += OFFSET;

        // 若不需要動畫
        if(!animate){
            setOrientation(targetX,targetY);
            return;
        }

        // 動畫模式
        animationRunning = false; // 結束先前動畫
        float diffX = shortestAngleDiff(mAngleX,targetX);
        float diffY = shortestAngleDiff(mAngleY,targetY);
        int steps = 20;
        float stepX = diffX/steps;
        float stepY = diffY/steps;
        float finalTargetX = targetX;
        float finalTargetY = targetY;
        new Thread(() -> {
            animationRunning = true;
            for(int i=0;i<steps && animationRunning;i++){
                applyRotation(stepX,stepY);
                requestRender();
                try{Thread.sleep(16);}catch(InterruptedException ignored){}
            }
            // 最後強制定位，避免累積誤差
            setOrientation(finalTargetX, finalTargetY);
            animationRunning = false;
        }).start();
    }

    private float shortestAngleDiff(float current,float target){
        float diff = target - current;
        while(diff > 180) diff -= 360;
        while(diff < -180) diff += 360;
        return diff;
    }

    public void stopAnimation(){
        animationRunning = false;
    }

    public void startSliceAnimation(String moveCode, Runnable onEnd){
        // 只對 U / U' / D / D' / R / R' / L / L' / F / F' / B / B' 和 "2" 做簡易動畫
        if(moveCode==null||moveCode.isEmpty()){
            if(onEnd!=null) onEnd.run();
            return;
        }
        char face = Character.toUpperCase(moveCode.charAt(0));
        boolean prime = moveCode.contains("'");   // 是否為逆時針
        boolean doubleTurn = moveCode.contains("2");
        // direction / totalAngle 先暫不計算，待調整 prime 後再確定
        
        float axisX=0,axisY=0,axisZ=0;
        float pivotX=0,pivotY=0,pivotZ=0;
        switch(face){
            case 'U': axisY=1; pivotY=CUBE_SIZE; break;
            case 'D': axisY=1; pivotY=-CUBE_SIZE; prime = !prime; break;
            case 'R': axisX=1; pivotX=CUBE_SIZE; break;
            case 'L': axisX=1; pivotX=-CUBE_SIZE; prime = !prime; break;
            case 'F': axisZ=1; pivotZ=CUBE_SIZE; break;
            case 'B': axisZ=1; pivotZ=-CUBE_SIZE; prime = !prime; break;
        }

        // 依最終 prime 重新計算旋轉方向與角度
        float direction = prime ? 1f : -1f;
        float totalAngle = direction * (doubleTurn ? 180f : 90f);

        List<CubeCell> layerCells = getLayerCells(face);
        if(layerCells.isEmpty()){
            if(onEnd!=null) onEnd.run();
            return;
        }

        final int steps = 15;
        final long frameTimeMs = 16;
        float finalPivotX = pivotX;
        float finalPivotY = pivotY;
        float finalPivotZ = pivotZ;
        float finalAxisX = axisX;
        float finalAxisY = axisY;
        float finalAxisZ = axisZ;
        new Thread(() -> {
            float[] mat = new float[16];
            for(int i=0;i<=steps;i++){
                float angle = totalAngle * i / steps;
                for(CubeCell cell:layerCells){
                    Matrix.setIdentityM(mat,0);
                    Matrix.translateM(mat,0, finalPivotX, finalPivotY, finalPivotZ);
                    Matrix.rotateM(mat,0,angle, finalAxisX, finalAxisY, finalAxisZ);
                    Matrix.translateM(mat,0,-finalPivotX,-finalPivotY,-finalPivotZ);
                    cell.setLocalMatrix(mat);
                }
                requestRender();
                try{Thread.sleep(frameTimeMs);}catch(InterruptedException ignored){}
            }
            // reset
            for(CubeCell cell:layerCells){
                cell.resetLocalMatrix();
            }
            requestRender();
            if(onEnd!=null) onEnd.run();
        }).start();
    }

    // 新增：取得指定層的 Cell 列表
    private List<CubeCell> getLayerCells(char faceCode){
        List<CubeCell> list = new ArrayList<>();
        char f = Character.toUpperCase(faceCode);
        switch(f){
            case 'U':
                for(int r=0;r<3;r++) for(int c=0;c<3;c++) list.add(faces[0].getCell(r,c));
                for(int idx:new int[]{1,2,4,5}) for(int c=0;c<3;c++) list.add(faces[idx].getCell(2,c));
                break;
            case 'D':
                for(int r=0;r<3;r++) for(int c=0;c<3;c++) list.add(faces[3].getCell(r,c));
                for(int idx:new int[]{1,2,4,5}) for(int c=0;c<3;c++) list.add(faces[idx].getCell(0,c));
                break;
            case 'R':
            case 'L':
            case 'F':
            case 'B':
                float TH = 0.25f; // layer threshold
                for(CubeFace faceObj:faces){
                    for(int r=0;r<3;r++){
                        for(int c=0;c<3;c++){
                            CubeCell cell = faceObj.getCell(r,c);
                            float[] ctr = cell.getCenter();
                            if( (f=='R' && ctr[0] > TH) ||
                                (f=='L' && ctr[0] < -TH) ||
                                (f=='F' && ctr[2] > TH) ||
                                (f=='B' && ctr[2] < -TH) ){
                                list.add(cell);
                            }
                        }
                    }
                }
                break;
        }
        return list;
    }

    // 公開：設定預設三面角度 (上、前、右皆可見)
    public void setPresetOrientation(boolean animate){
        // 使用者最終確認的視角
        final float targetX = 334.56f;
        final float targetY = 144.88f;
        final float targetZ = 345f;

        if(!animate){
            // 立即設定三軸
            setOrientation(targetX,targetY,targetZ);
            return;
        }

        // 先對 X/Y 做平滑動畫，Z 最後一次到位（通常 Z 角度改變較小）
        animationRunning = false;
        float diffX = shortestAngleDiff(mAngleX,targetX);
        float diffY = shortestAngleDiff(mAngleY,targetY);
        int steps = 20;
        float stepX = diffX/steps;
        float stepY = diffY/steps;
        new Thread(() -> {
            animationRunning = true;
            for(int i=0;i<steps && animationRunning;i++){
                applyRotation(stepX,stepY);
                requestRender();
                try{Thread.sleep(16);}catch(InterruptedException ignored){}
            }
            // 最終定位含 Z
            setOrientation(targetX,targetY,targetZ);
            animationRunning = false;
        }).start();
    }

    // 專用內部：通用旋轉到指定角度 (公開給其他方法)
    public void rotateTo(float targetX,float targetY,boolean animate){
        if(!animate){
            setOrientation(targetX,targetY);
            return;
        }
        animationRunning = false;
        float diffX = shortestAngleDiff(mAngleX,targetX);
        float diffY = shortestAngleDiff(mAngleY,targetY);
        int steps = 20;
        float stepX = diffX/steps;
        float stepY = diffY/steps;
        new Thread(() -> {
            animationRunning = true;
            for(int i=0;i<steps && animationRunning;i++){
                applyRotation(stepX,stepY);
                requestRender();
                try{Thread.sleep(16);}catch(InterruptedException ignored){}
            }
            setOrientation(targetX,targetY);
            animationRunning = false;
        }).start();
    }

    // 調適用：不支援動畫（animate=false）
    public void rotateTo(float targetX,float targetY,float targetZ,boolean animate){
        setOrientation(targetX,targetY,targetZ);
    }

    // 取得世界座標 (Pitch, Yaw) 角度，避免 Euler 次序影響
    public float[] getWorldAngles(){
        // 由 mAccumulatedRotation (4x4) 擷取
        float[] r = mAccumulatedRotation;
        // yaw (Y軸) = atan2(-m20, m00)
        float yaw = (float)Math.toDegrees(Math.atan2(-r[8], r[0]));
        // pitch (X軸) = asin(m10)
        float pitch = (float)Math.toDegrees(Math.asin(r[4]));
        // roll (Z) = atan2(-m12, m11)
        float roll = (float)Math.toDegrees(Math.atan2(-r[6], r[5]));
        roll = (roll%360+360)%360;
        // 正規化
        yaw = (yaw%360+360)%360;
        pitch = (pitch%360+360)%360;
        return new float[]{pitch,yaw,roll};
    }

    // 判斷是否已初始化完成（faces 已產生）
    public boolean isInitialized(){
        return initialized && faces!=null;
    }

    // 新增：重置初始化狀態
    public void resetInitialization() {
        initialized = false;
    }

    /**
     * 取得目前 54 格顏色（以顏色字元字串，順序：face0~5, cell0~8）
     */
    public String getAllColors() {
        StringBuilder sb = new StringBuilder(54);
        for (int face = 0; face < 6; face++) {
            for (int cell = 0; cell < 9; cell++) {
                int[] rc = convertCellIndexToRowCol(face, cell);
                float[] color = faces[face].getCellColor(rc[0], rc[1]);
                char c = colorToChar(color);
                // 只允許W/Y/G/B/R/O，其他一律回傳'N'
                if(c!='W'&&c!='Y'&&c!='G'&&c!='B'&&c!='R'&&c!='O') c='N';
                sb.append(c);
            }
        }
        return sb.toString();
    }

    /**
     * 以顏色字元字串還原 54 格顏色（順序：face0~5, cell0~8）
     */
    public void setAllColors(String colorString) {
        if (colorString == null || colorString.length() < 54) return;
        for (int face = 0; face < 6; face++) {
            for (int cell = 0; cell < 9; cell++) {
                int[] rc = convertCellIndexToRowCol(face, cell);
                char c = colorString.charAt(face * 9 + cell);
                float[] rgba;
                if(c=='N'){
                    rgba = new float[]{0.6667f, 0.6667f, 0.6667f, 1.0f}; // 預設灰色
                }
                else{
                    rgba = charToRgba(c);
                }
                if (faces == null || !isInitialized()) {
                    Log.e("CubeDebug", "Renderer尚未初始化，setAllColors略過");
                    return;
                }
                if(faces != null){
                    faces[face].setCellColor(rc[0], rc[1], rgba);
                }

            }
        }
        requestRender();
    }

    // 顏色float[]轉字元（根據face預設顏色做比對）
    private char colorToChar(float[] color) {
        int colorInt = rgbaToColorInt(color);
        switch (colorInt){
            case COLOR_WHITE:
                return 'W';
            case COLOR_YELLOW:
                return 'Y';
            case COLOR_GREEN:
                return 'G';
            case COLOR_BLUE:
                return 'B';
            case COLOR_RED:
                return 'R';
            case COLOR_ORANGE:
                return 'O';
            default:
                return 'N';
        }
    }
    // 字元轉float[]顏色
    private float[] charToRgba(char c) {
        switch (c) {
            case 'W': return colorIntToRgba(COLOR_WHITE);
            case 'B': return colorIntToRgba(COLOR_BLUE);
            case 'R': return colorIntToRgba(COLOR_RED);
            case 'Y': return colorIntToRgba(COLOR_YELLOW);
            case 'G': return colorIntToRgba(COLOR_GREEN);
            case 'O': return colorIntToRgba(COLOR_ORANGE);
            default:  return new float[]{0.6667f, 0.6667f, 0.6667f, 1.0f};
        }
    }

    // float[] 轉 int colorInt
    private static int rgbaToColorInt(float[] rgba) {
        int a = Math.round(rgba.length > 3 ? rgba[3] * 255 : 255);
        int r = Math.round(rgba[0] * 255);
        int g = Math.round(rgba[1] * 255);
        int b = Math.round(rgba[2] * 255);
        return (a << 24) | (r << 16) | (g << 8) | b;
    }

    private static float[] colorIntToRgba(int colorInt) {
        float r = ((colorInt >> 16) & 0xFF) / 255f;
        float g = ((colorInt >> 8) & 0xFF) / 255f;
        float b = (colorInt & 0xFF) / 255f;
        float a = ((colorInt >> 24) & 0xFF) / 255f;

        return new float[] { r, g, b, a };
    }
}