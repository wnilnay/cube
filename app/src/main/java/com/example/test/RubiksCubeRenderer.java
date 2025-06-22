package com.example.test;

import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.opengl.Matrix;
import android.util.Log;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

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

    public RubiksCubeRenderer(GLSurfaceView surfaceView) {
        mSurfaceView = surfaceView;
        Matrix.setIdentityM(mAccumulatedRotation, 0);
        
        // 設置初始旋轉，讓紅色面（前面）朝前
        // 初始狀態：橙色面朝前，需要旋轉180度讓紅色面朝前
        // 但是我們需要確保旋轉方向正確
        Matrix.rotateM(mAccumulatedRotation, 0, 180, 0, 1, 0);
        
        // 更新角度變數
        mAngleY = 180;
        
        Log.d("RubiksCube", "初始化完成: 角度Y=" + mAngleY);
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
    }

    @Override
    public void onDrawFrame(GL10 unused) {
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);

        // 設置相機位置
        Matrix.setLookAtM(mViewMatrix, 0, 0, 0, -4, 0f, 0f, 0f, 0f, 1.0f, 0.0f);
        //Matrix.setLookAtM(mViewMatrix, 0, 0, 0, -3, 0f, 0f, 0f, 0f, 1.0f, 0.0f);

        // 直接用累積旋轉矩陣
        Matrix.multiplyMM(mMVPMatrix, 0, mProjectionMatrix, 0, mViewMatrix, 0);
        Matrix.multiplyMM(mMVPMatrix, 0, mMVPMatrix, 0, mAccumulatedRotation, 0);

        // 繪製所有面
        for (CubeFace face : faces) {
            face.draw(mMVPMatrix);
        }
        
        // 繪製調試圓點
        if (showDebugPoint && debugPoint != null) {
            drawDebugPoint(mMVPMatrix, debugPoint);
        }
    }

    @Override
    public void onSurfaceChanged(GL10 unused, int width, int height) {
        this.width = width;
        this.height = height;

        GLES20.glViewport(0, 0, width, height);

        float ratio = (float) width / height;
        Matrix.frustumM(mProjectionMatrix, 0, -ratio, ratio, -1, 1, 3, 7);
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

    public void handleClick(float x, float y, int screenWidth, int screenHeight) {
        Log.d("RubiksCube", "開始點擊檢測: 屏幕(" + x + "," + y + ") 角度X=" + mAngleX + " 角度Y=" + mAngleY);
        
        // 清除之前的高亮和調試點
        clearCurrentHighlight();
        showDebugPoint = false;
        
        // 改進的點擊檢測 - 考慮相機位置和視角
        ClickResult result = performImprovedDetection(x, y, screenWidth, screenHeight);
        
        if (result != null) {
            String touchInfo = String.format("觸摸點: (%.3f,%.3f)", x, y);
            lastTouchInfo = touchInfo;

            String debugInfo = String.format("檢測成功: 面=%s(index=%d), 格子=(%d,%d), 交點=(%.3f,%.3f,%.3f)", 
                FACE_NAMES[result.faceIndex], result.faceIndex, result.gridX, result.gridY, 
                result.intersectionPoint[0], result.intersectionPoint[1], result.intersectionPoint[2]);
            
            Log.d("RubiksCube", debugInfo);
            lastDebugInfo = debugInfo;
            
            // 設置調試圓點
            debugPoint = result.intersectionPoint.clone();
            showDebugPoint = true;
            
            // 設置新的高亮
            setHighlight(result.faceIndex, result.gridY, result.gridX);
            
        } else {
            String debugInfo = "檢測失敗: 未找到有效的交點";
            Log.d("RubiksCube", debugInfo);
            lastDebugInfo = debugInfo;
        }
    }
    
    private ClickResult performImprovedDetection(float x, float y, int screenWidth, int screenHeight) {
        // 將屏幕座標轉換為標準化設備座標 (-1 到 1)
        float normalizedX = (2.0f * x / screenWidth) - 1.0f;
        float normalizedY = 1.0f - (2.0f * y / screenHeight);
        
        Log.d("RubiksCube", "標準化座標: (" + normalizedX + "," + normalizedY + ")");
        
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
        
        Log.d("RubiksCube", "射線起點: (" + rayStart[0] + "," + rayStart[1] + "," + rayStart[2] + ")");
        Log.d("RubiksCube", "射線方向: (" + rayDirection[0] + "," + rayDirection[1] + "," + rayDirection[2] + ")");
        
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
        
        Log.d("RubiksCube", "變換後射線起點: (" + start[0] + "," + start[1] + "," + start[2] + ")");
        Log.d("RubiksCube", "變換後射線方向: (" + dir[0] + "," + dir[1] + "," + dir[2] + ")");
        
        // 與立方體的6個面進行相交檢測
        float closestDistance = Float.MAX_VALUE;
        int bestFace = -1;
        float[] bestIntersection = null;
        
        Log.d("RubiksCube", "開始面檢測...");
        
        for (int faceIndex = 0; faceIndex < 6; faceIndex++) {
            float[] normal = FACE_NORMALS[faceIndex];
            float[] planePoint = {normal[0] * CUBE_SIZE, normal[1] * CUBE_SIZE, normal[2] * CUBE_SIZE};
            
            // 計算射線與平面的交點
            float denom = dir[0] * normal[0] + dir[1] * normal[1] + dir[2] * normal[2];
            
            Log.d("RubiksCube", String.format("面%d (%s): 法向量=(%.3f,%.3f,%.3f), 平面點=(%.3f,%.3f,%.3f), denom=%.6f", 
                faceIndex, FACE_NAMES[faceIndex], 
                normal[0], normal[1], normal[2],
                planePoint[0], planePoint[1], planePoint[2], denom));
            
            if (Math.abs(denom) > 0.0001f) {
                float t = ((planePoint[0] - start[0]) * normal[0] + 
                          (planePoint[1] - start[1]) * normal[1] + 
                          (planePoint[2] - start[2]) * normal[2]) / denom;
                
                Log.d("RubiksCube", String.format("面%d: t=%.6f", faceIndex, t));
                
                if (t > 0) {
                    float[] intersection = {
                        start[0] + t * dir[0],
                        start[1] + t * dir[1],
                        start[2] + t * dir[2]
                    };
                    
                    Log.d("RubiksCube", String.format("面%d: 交點=(%.3f,%.3f,%.3f)", 
                        faceIndex, intersection[0], intersection[1], intersection[2]));
                    
                    // 檢查交點是否在立方體表面內
                    if (isPointOnCubeFace(intersection, faceIndex)) {
                        Log.d("RubiksCube", String.format("面%d: 交點在面內，距離=%.6f", faceIndex, t));
                        if (t < closestDistance) {
                            closestDistance = t;
                            bestFace = faceIndex;
                            bestIntersection = intersection;
                            Log.d("RubiksCube", String.format("面%d: 更新為最佳面", faceIndex));
                        }
                    } else {
                        Log.d("RubiksCube", String.format("面%d: 交點不在面內", faceIndex));
                    }
                } else {
                    Log.d("RubiksCube", String.format("面%d: t <= 0，跳過", faceIndex));
                }
            } else {
                Log.d("RubiksCube", String.format("面%d: denom太小，跳過", faceIndex));
            }
        }
        
        if (bestFace >= 0) {
            Log.d("RubiksCube", "最終檢測結果: 面=" + FACE_NAMES[bestFace] + 
                    ", 交點: (" + bestIntersection[0] + "," + bestIntersection[1] + "," + bestIntersection[2] + ")");
            
            // 計算格子座標
            int[] gridCoords = calculateGridCoordinates(bestIntersection, bestFace);
            
            return new ClickResult(bestFace, gridCoords[0], gridCoords[1], bestIntersection);
        }
        
        Log.d("RubiksCube", "沒有找到有效的交點");
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
        
        Log.d("RubiksCube", "計算格子座標: 點(" + point[0] + "," + point[1] + "," + point[2] + ") 面=" + FACE_NAMES[faceIndex]);
        
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
        
        Log.d("RubiksCube", "2D座標: u=" + u + " v=" + v);
        
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
        
        Log.d("RubiksCube", "修正後格子座標: (" + gridX + "," + gridY + ")");
        
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
            
            Log.d("RubiksCube", "設置高亮: 面=" + FACE_NAMES[faceIndex] + 
                    ", 格子=(" + row + "," + col + ")");
            
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
            int cellIndex;
            int row = currentHighlightedRow;
            int col = currentHighlightedCol;

            switch (currentHighlightedFace) {
                case 0: // Up : 直接
                    cellIndex = row * 3 + col;
                    break;
                case 1: // Right : 只翻轉 row
                case 2: // Forward
                case 3: // Down
                    cellIndex = (2 - row) * 3 + col;
                    break;
                case 4: // Left : 翻轉 row 與 col
                case 5: // Backward
                    cellIndex = (2 - row) * 3 + (2 - col);
                    break;
                default:
                    cellIndex = row * 3 + col;
            }

            return String.format("當前高亮: %d:%d", currentHighlightedFace, cellIndex);
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
        Log.d("RubiksCube", "重置到初始狀態...");
        
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
        
        Log.d("RubiksCube", "重置完成: 角度X=" + mAngleX + ", 角度Y=" + mAngleY);
        
        // 強制重新渲染
        requestRender();
    }
    
    // 新增：測試點擊檢測的方法
    public void testClickDetection() {
        Log.d("RubiksCube", "開始測試點擊檢測...");
        
        // 測試屏幕中心點擊
        int screenWidth = 1080; // 假設屏幕寬度
        int screenHeight = 1920; // 假設屏幕高度
        
        float centerX = screenWidth / 2.0f;
        float centerY = screenHeight / 2.0f;
        
        Log.d("RubiksCube", "測試中心點擊: (" + centerX + "," + centerY + ")");
        handleClick(centerX, centerY, screenWidth, screenHeight);
        
        // 測試四個角落
        float[][] corners = {
            {100, 100},           // 左上
            {screenWidth - 100, 100}, // 右上
            {100, screenHeight - 100}, // 左下
            {screenWidth - 100, screenHeight - 100} // 右下
        };
        
        for (int i = 0; i < corners.length; i++) {
            Log.d("RubiksCube", "測試角落 " + (i + 1) + ": (" + corners[i][0] + "," + corners[i][1] + ")");
            handleClick(corners[i][0], corners[i][1], screenWidth, screenHeight);
        }
    }
    
    // 新增：測試特定面的檢測
    public void testSpecificFaceDetection() {
        Log.d("RubiksCube", "開始測試特定面檢測...");
        
        // 測試每個面的法向量和平面點
        for (int faceIndex = 0; faceIndex < 6; faceIndex++) {
            float[] normal = FACE_NORMALS[faceIndex];
            float[] planePoint = {normal[0] * CUBE_SIZE, normal[1] * CUBE_SIZE, normal[2] * CUBE_SIZE};
            
            Log.d("RubiksCube", String.format("面%d (%s): 法向量=(%.1f,%.1f,%.1f), 平面點=(%.1f,%.1f,%.1f)", 
                faceIndex, FACE_NAMES[faceIndex], 
                normal[0], normal[1], normal[2],
                planePoint[0], planePoint[1], planePoint[2]));
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
                    
                    Log.d("RubiksCube", String.format("面%d (%s): 交點=(%.3f,%.3f,%.3f), 距離=%.3f", 
                        faceIndex, FACE_NAMES[faceIndex], 
                        intersection[0], intersection[1], intersection[2], t));
                }
            }
        }
        
        // 新增：測試面的邊界檢查
        Log.d("RubiksCube", "測試面的邊界檢查...");
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
            Log.d("RubiksCube", "測試面" + faceIndex + " (" + FACE_NAMES[faceIndex] + ") 的邊界檢查:");
            for (int i = 0; i < testPoints.length; i++) {
                boolean isOnFace = isPointOnCubeFace(testPoints[i], faceIndex);
                Log.d("RubiksCube", String.format("  點%d (%.1f,%.1f,%.1f): %s", 
                    i, testPoints[i][0], testPoints[i][1], testPoints[i][2], 
                    isOnFace ? "在面內" : "不在面內"));
            }
        }
    }

    // 新增：繪製調試圓點
    private void drawDebugPoint(float[] mvpMatrix, float[] point) {
        // 創建一個小的球體來表示調試點
        float radius = 0.05f;
        int segments = 8;
        
        // 簡單的球體頂點
        float[] vertices = new float[segments * segments * 3];
        int vertexIndex = 0;
        
        for (int i = 0; i < segments; i++) {
            float phi = (float) (Math.PI * i / segments);
            for (int j = 0; j < segments; j++) {
                float theta = (float) (2 * Math.PI * j / segments);
                
                float x = point[0] + radius * (float) (Math.sin(phi) * Math.cos(theta));
                float y = point[1] + radius * (float) (Math.sin(phi) * Math.sin(theta));
                float z = point[2] + radius * (float) Math.cos(phi);
                
                vertices[vertexIndex++] = x;
                vertices[vertexIndex++] = y;
                vertices[vertexIndex++] = z;
            }
        }
        
        // 創建頂點緩衝區
        ByteBuffer bb = ByteBuffer.allocateDirect(vertices.length * 4);
        bb.order(ByteOrder.nativeOrder());
        FloatBuffer vertexBuffer = bb.asFloatBuffer();
        vertexBuffer.put(vertices);
        vertexBuffer.position(0);
        
        // 使用簡單的著色器
        int program = createSimpleShaderProgram();
        GLES20.glUseProgram(program);
        
        int positionHandle = GLES20.glGetAttribLocation(program, "vPosition");
        int colorHandle = GLES20.glGetUniformLocation(program, "vColor");
        int mvpMatrixHandle = GLES20.glGetUniformLocation(program, "uMVPMatrix");
        
        GLES20.glEnableVertexAttribArray(positionHandle);
        GLES20.glVertexAttribPointer(positionHandle, 3, GLES20.GL_FLOAT, false, 12, vertexBuffer);
        
        // 設置紅色調試點
        float[] debugColor = {1.0f, 0.0f, 0.0f, 1.0f}; // 紅色
        GLES20.glUniform4fv(colorHandle, 1, debugColor, 0);
        GLES20.glUniformMatrix4fv(mvpMatrixHandle, 1, false, mvpMatrix, 0);
        
        // 繪製點
        GLES20.glDrawArrays(GLES20.GL_POINTS, 0, vertices.length / 3);
        
        GLES20.glDisableVertexAttribArray(positionHandle);
    }
    
    // 創建簡單的著色器程序
    private int createSimpleShaderProgram() {
        String vertexShaderCode = 
            "uniform mat4 uMVPMatrix;" +
            "attribute vec4 vPosition;" +
            "void main() {" +
            "  gl_Position = uMVPMatrix * vPosition;" +
            "  gl_PointSize = 10.0;" +
            "}";
            
        String fragmentShaderCode = 
            "precision mediump float;" +
            "uniform vec4 vColor;" +
            "void main() {" +
            "  gl_FragColor = vColor;" +
            "}";
        
        int vertexShader = loadShader(GLES20.GL_VERTEX_SHADER, vertexShaderCode);
        int fragmentShader = loadShader(GLES20.GL_FRAGMENT_SHADER, fragmentShaderCode);
        
        int program = GLES20.glCreateProgram();
        GLES20.glAttachShader(program, vertexShader);
        GLES20.glAttachShader(program, fragmentShader);
        GLES20.glLinkProgram(program);
        
        return program;
    }
    
    // 載入著色器
    private int loadShader(int type, String shaderCode) {
        int shader = GLES20.glCreateShader(type);
        GLES20.glShaderSource(shader, shaderCode);
        GLES20.glCompileShader(shader);
        return shader;
    }
}