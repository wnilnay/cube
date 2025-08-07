package com.example.test;

import android.opengl.GLES20;
import android.opengl.Matrix;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.ShortBuffer;

public class CubeFace {
    private final String vertexShaderCode =
            "uniform mat4 uMVPMatrix;" +
                    "attribute vec4 vPosition;" +
                    "void main() {" +
                    "  gl_Position = uMVPMatrix * vPosition;" +
                    "}";

    private final String fragmentShaderCode =
            "precision mediump float;" +
                    "uniform vec4 vColor;" +
                    "void main() {" +
                    "  gl_FragColor = vColor;" +
                    "}";

    private FloatBuffer vertexBuffer;
    private FloatBuffer borderVertexBuffer;
    private ShortBuffer drawListBuffer;
    private ShortBuffer borderDrawListBuffer;
    private int mProgram;
    private int mMVPMatrixHandle;
    private int mPositionHandle;
    private int mColorHandle;

    // 面的索引
    private int faceIndex;
    // 3x3 網格的顏色數組
    private float[][] gridColors;
    
    // 高亮狀態 - 記錄哪些格子被高亮
    private boolean[][] highlightedCells;
    
    // 高亮顏色 - 使用黑色
    private final float[] HIGHLIGHT_COLOR = {0.0f, 0.0f, 0.0f, 1.0f}; // 黑色，不透明

    // 黑色邊框顏色
    private final float[] BORDER_COLOR = {0.0f, 0.0f, 0.0f, 1.0f};

    // 默認的魔方顏色
//    private final float[][] DEFAULT_FACE_COLORS = {
//            {1.0f, 1.0f, 1.0f, 1.0f}, // 白色 (上面 - Up)
//            {0.0f, 0.0f, 1.0f, 1.0f}, // 藍色 (右面 - Right)
//            {1.0f, 0.0f, 0.0f, 1.0f}, // 紅色 (前面 - Forward)
//            {1.0f, 1.0f, 0.0f, 1.0f}, // 黃色 (下面 - Down)
//            {0.0f, 1.0f, 0.0f, 1.0f}, // 綠色 (左面 - Left)
//            {1.0f, 0.5f, 0.0f, 1.0f}  // 橙色 (後面 - Backward)
//    };
    private final float[][] DEFAULT_FACE_COLORS = {
            {0.6667f, 0.6667f, 0.6667f, 1.0f}, // 白色 (上面 - Up)
            {0.6667f, 0.6667f, 0.6667f, 1.0f}, // 藍色 (右面 - Right)
            {0.6667f, 0.6667f, 0.6667f, 1.0f}, // 紅色 (前面 - Forward)
            {0.6667f, 0.6667f, 0.6667f, 1.0f}, // 黃色 (下面 - Down)
            {0.6667f, 0.6667f, 0.6667f, 1.0f}, // 綠色 (左面 - Left)
            {0.6667f, 0.6667f, 0.6667f, 1.0f}  // 橙色 (後面 - Backward)
    };

    private CubeCell[][] cells = new CubeCell[3][3];

    // 爲3x3網格生成頂點座標
    private float[] getVerticesForFace(int faceIndex) {
        float size = 0.5f;
        float cellSize = size * 2 / 3;
        float gap = 0.01f; // 小間隙

        // 每個面需要9個小方塊，每個小方塊4個頂點
        float[] vertices = new float[9 * 4 * 3];

        int vertexIndex = 0;

        // 生成3x3網格
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 3; col++) {
                // 計算小方塊的位置
                float startX = -size + col * cellSize + gap;
                float endX = startX + cellSize - gap;
                float startY = size - row * cellSize - gap;
                float endY = startY - cellSize + gap;

                // 根據面的索引生成對應的頂點
                float[] cellVertices = getCellVertices(faceIndex, startX, endX, startY, endY, size);

                // 將頂點添加到數組中
                for (int i = 0; i < cellVertices.length; i++) {
                    vertices[vertexIndex++] = cellVertices[i];
                }
            }
        }

        return vertices;
    }

    // 生成邊框頂點
    private float[] getBorderVerticesForFace(int faceIndex) {
        float size = 0.5f;
        float cellSize = size * 2 / 3;

        // 生成網格線 - 4條垂直線 + 4條水平線
        float[] borderVertices = new float[8 * 4 * 3]; // 8條線，每條4個頂點，每個頂點3個座標
        int vertexIndex = 0;

        // 垂直線
        for (int i = 0; i < 4; i++) {
            float x = -size + i * cellSize;
            float[] lineVertices = getBorderLine(faceIndex, x, -size, x, size, true);
            for (float v : lineVertices) {
                borderVertices[vertexIndex++] = v;
            }
        }

        // 水平線
        for (int i = 0; i < 4; i++) {
            float y = size - i * cellSize;
            float[] lineVertices = getBorderLine(faceIndex, -size, y, size, y, false);
            for (float v : lineVertices) {
                borderVertices[vertexIndex++] = v;
            }
        }

        return borderVertices;
    }

    // 生成單條邊框線的頂點
    private float[] getBorderLine(int faceIndex, float x1, float y1, float x2, float y2, boolean isVertical) {
        float size = 0.5f;
        float lineWidth = 0.01f;

        float[] vertices = new float[12]; // 4個頂點，每個3個座標

        switch (faceIndex) {
            case 0: // 上面 (Up)
                if (isVertical) {
                    vertices[0] = x1 - lineWidth; vertices[1] = size; vertices[2] = -y1;
                    vertices[3] = x1 + lineWidth; vertices[4] = size; vertices[5] = -y1;
                    vertices[6] = x2 + lineWidth; vertices[7] = size; vertices[8] = -y2;
                    vertices[9] = x2 - lineWidth; vertices[10] = size; vertices[11] = -y2;
                } else {
                    vertices[0] = x1; vertices[1] = size; vertices[2] = -y1 - lineWidth;
                    vertices[3] = x2; vertices[4] = size; vertices[5] = -y1 - lineWidth;
                    vertices[6] = x2; vertices[7] = size; vertices[8] = -y2 + lineWidth;
                    vertices[9] = x1; vertices[10] = size; vertices[11] = -y2 + lineWidth;
                }
                break;
            case 1: // 右面 (Right)
                if (isVertical) {
                    vertices[0] = size; vertices[1] = -y1; vertices[2] = -x1 - lineWidth;
                    vertices[3] = size; vertices[4] = -y1; vertices[5] = -x1 + lineWidth;
                    vertices[6] = size; vertices[7] = -y2; vertices[8] = -x2 + lineWidth;
                    vertices[9] = size; vertices[10] = -y2; vertices[11] = -x2 - lineWidth;
                } else {
                    vertices[0] = size; vertices[1] = -y1 - lineWidth; vertices[2] = -x1;
                    vertices[3] = size; vertices[4] = -y1 - lineWidth; vertices[5] = -x2;
                    vertices[6] = size; vertices[7] = -y2 + lineWidth; vertices[8] = -x2;
                    vertices[9] = size; vertices[10] = -y2 + lineWidth; vertices[11] = -x1;
                }
                break;
            case 2: // 前面 (Forward)
                if (isVertical) {
                    vertices[0] = x1 - lineWidth; vertices[1] = -y1; vertices[2] = size;
                    vertices[3] = x1 + lineWidth; vertices[4] = -y1; vertices[5] = size;
                    vertices[6] = x2 + lineWidth; vertices[7] = -y2; vertices[8] = size;
                    vertices[9] = x2 - lineWidth; vertices[10] = -y2; vertices[11] = size;
                } else {
                    vertices[0] = x1; vertices[1] = -y1 - lineWidth; vertices[2] = size;
                    vertices[3] = x2; vertices[4] = -y1 - lineWidth; vertices[5] = size;
                    vertices[6] = x2; vertices[7] = -y2 + lineWidth; vertices[8] = size;
                    vertices[9] = x1; vertices[10] = -y2 + lineWidth; vertices[11] = size;
                }
                break;
            case 3: // 下面 (Down)
                if (isVertical) {
                    vertices[0] = x1 - lineWidth; vertices[1] = -size; vertices[2] = -y2;
                    vertices[3] = x1 + lineWidth; vertices[4] = -size; vertices[5] = -y2;
                    vertices[6] = x2 + lineWidth; vertices[7] = -size; vertices[8] = -y1;
                    vertices[9] = x2 - lineWidth; vertices[10] = -size; vertices[11] = -y1;
                } else {
                    vertices[0] = x1; vertices[1] = -size; vertices[2] = -y1 + lineWidth;
                    vertices[3] = x2; vertices[4] = -size; vertices[5] = -y1 + lineWidth;
                    vertices[6] = x2; vertices[7] = -size; vertices[8] = -y2 - lineWidth;
                    vertices[9] = x1; vertices[10] = -size; vertices[11] = -y2 - lineWidth;
                }
                break;
            case 4: // 左面 (Left)
                if (isVertical) {
                    vertices[0] = -size; vertices[1] = -y1; vertices[2] = -x1 + lineWidth;
                    vertices[3] = -size; vertices[4] = -y1; vertices[5] = -x1 - lineWidth;
                    vertices[6] = -size; vertices[7] = -y2; vertices[8] = -x2 - lineWidth;
                    vertices[9] = -size; vertices[10] = -y2; vertices[11] = -x2 + lineWidth;
                } else {
                    vertices[0] = -size; vertices[1] = -y1 - lineWidth; vertices[2] = -x2;
                    vertices[3] = -size; vertices[4] = -y1 - lineWidth; vertices[5] = -x1;
                    vertices[6] = -size; vertices[7] = -y2 + lineWidth; vertices[8] = -x1;
                    vertices[9] = -size; vertices[10] = -y2 + lineWidth; vertices[11] = -x2;
                }
                break;
            case 5: // 後面 (Backward)
                if (isVertical) {
                    vertices[0] = x1 + lineWidth; vertices[1] = -y1; vertices[2] = -size;
                    vertices[3] = x1 - lineWidth; vertices[4] = -y1; vertices[5] = -size;
                    vertices[6] = x2 - lineWidth; vertices[7] = -y2; vertices[8] = -size;
                    vertices[9] = x2 + lineWidth; vertices[10] = -y2; vertices[11] = -size;
                } else {
                    vertices[0] = x2; vertices[1] = -y1 - lineWidth; vertices[2] = -size;
                    vertices[3] = x1; vertices[4] = -y1 - lineWidth; vertices[5] = -size;
                    vertices[6] = x1; vertices[7] = -y2 + lineWidth; vertices[8] = -size;
                    vertices[9] = x2; vertices[10] = -y2 + lineWidth; vertices[11] = -size;
                }
                break;
        }

        return vertices;
    }

    // 爲單個小方塊生成頂點
    private float[] getCellVertices(int faceIndex, float startX, float endX, float startY, float endY, float size) {
        switch (faceIndex) {
            case 0: // 上面 (Up) (從上方看，順時針)
                return new float[] {
                        startX, size, -endY,
                        endX, size, -endY,
                        endX, size, -startY,
                        startX, size, -startY
                };
            case 1: // 右面 (Right) (從右方看，順時針)
                return new float[] {
                        size, -endY, -startX,
                        size, -endY, -endX,
                        size, -startY, -endX,
                        size, -startY, -startX
                };
            case 2: // 前面 (Forward) (從前方看，順時針)
                return new float[] {
                        startX, -endY, size,
                        endX, -endY, size,
                        endX, -startY, size,
                        startX, -startY, size
                };
            case 3: // 下面 (Down) (從下方看，順時針)
                return new float[] {
                        startX, -size, -startY,
                        endX, -size, -startY,
                        endX, -size, -endY,
                        startX, -size, -endY
                };
            case 4: // 左面 (Left) (從左方看，順時針)
                return new float[] {
                        -size, -endY, -endX,
                        -size, -endY, -startX,
                        -size, -startY, -startX,
                        -size, -startY, -endX
                };
            case 5: // 後面 (Backward) (從後方看，順時針)
                return new float[] {
                        startX, -startY, -size,
                        endX, -startY, -size,
                        endX, -endY, -size,
                        startX, -endY, -size
                };
            default:
                return new float[12];
        }
    }

    // 繪製順序 - 9個小方塊，每個2個三角形
    private short[] generateDrawOrder() {
        short[] drawOrder = new short[9 * 6]; // 9個方塊 * 6個索引

        for (int i = 0; i < 9; i++) {
            short baseIndex = (short)(i * 4);
            int orderIndex = i * 6;

            drawOrder[orderIndex] = baseIndex;
            drawOrder[orderIndex + 1] = (short)(baseIndex + 1);
            drawOrder[orderIndex + 2] = (short)(baseIndex + 2);
            drawOrder[orderIndex + 3] = baseIndex;
            drawOrder[orderIndex + 4] = (short)(baseIndex + 2);
            drawOrder[orderIndex + 5] = (short)(baseIndex + 3);
        }

        return drawOrder;
    }

    // 邊框繪製順序
    private short[] generateBorderDrawOrder() {
        short[] drawOrder = new short[8 * 6]; // 8條線 * 6個索引

        for (int i = 0; i < 8; i++) {
            short baseIndex = (short)(i * 4);
            int orderIndex = i * 6;

            drawOrder[orderIndex] = baseIndex;
            drawOrder[orderIndex + 1] = (short)(baseIndex + 1);
            drawOrder[orderIndex + 2] = (short)(baseIndex + 2);
            drawOrder[orderIndex + 3] = baseIndex;
            drawOrder[orderIndex + 4] = (short)(baseIndex + 2);
            drawOrder[orderIndex + 5] = (short)(baseIndex + 3);
        }

        return drawOrder;
    }

    public CubeFace(int faceIndex) {
        this.faceIndex = faceIndex;

        // 初始化3x3網格顏色，全部使用該面的默認顏色
        gridColors = new float[9][4];
        for (int i = 0; i < 9; i++) {
            System.arraycopy(DEFAULT_FACE_COLORS[faceIndex], 0, gridColors[i], 0, 4);
        }
        
        // 初始化高亮狀態
        highlightedCells = new boolean[3][3];
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 3; col++) {
                highlightedCells[row][col] = false;
            }
        }

        float[] vertices = getVerticesForFace(faceIndex);
        float[] borderVertices = getBorderVerticesForFace(faceIndex);
        short[] drawOrder = generateDrawOrder();
        short[] borderDrawOrder = generateBorderDrawOrder();

        // 初始化頂點緩衝區
        ByteBuffer bb = ByteBuffer.allocateDirect(vertices.length * 4);
        bb.order(ByteOrder.nativeOrder());
        vertexBuffer = bb.asFloatBuffer();
        vertexBuffer.put(vertices);
        vertexBuffer.position(0);

        // 初始化邊框頂點緩衝區
        ByteBuffer bbb = ByteBuffer.allocateDirect(borderVertices.length * 4);
        bbb.order(ByteOrder.nativeOrder());
        borderVertexBuffer = bbb.asFloatBuffer();
        borderVertexBuffer.put(borderVertices);
        borderVertexBuffer.position(0);

        // 初始化繪製順序緩衝區
        ByteBuffer dlb = ByteBuffer.allocateDirect(drawOrder.length * 2);
        dlb.order(ByteOrder.nativeOrder());
        drawListBuffer = dlb.asShortBuffer();
        drawListBuffer.put(drawOrder);
        drawListBuffer.position(0);

        // 初始化邊框繪製順序緩衝區
        ByteBuffer bdlb = ByteBuffer.allocateDirect(borderDrawOrder.length * 2);
        bdlb.order(ByteOrder.nativeOrder());
        borderDrawListBuffer = bdlb.asShortBuffer();
        borderDrawListBuffer.put(borderDrawOrder);
        borderDrawListBuffer.position(0);

        // 準備shader
        int vertexShader = loadShader(GLES20.GL_VERTEX_SHADER, vertexShaderCode);
        int fragmentShader = loadShader(GLES20.GL_FRAGMENT_SHADER, fragmentShaderCode);

        mProgram = GLES20.glCreateProgram();
        GLES20.glAttachShader(mProgram, vertexShader);
        GLES20.glAttachShader(mProgram, fragmentShader);
        GLES20.glLinkProgram(mProgram);

        // ===== 新增：取得 attribute / uniform =====
        mPositionHandle = GLES20.glGetAttribLocation(mProgram, "vPosition");
        mColorHandle    = GLES20.glGetUniformLocation(mProgram, "vColor");
        mMVPMatrixHandle = GLES20.glGetUniformLocation(mProgram, "uMVPMatrix");

        Matrix.setIdentityM(localMatrix,0);

        for(int row=0;row<3;row++){
            for(int col=0;col<3;col++){
                int idx = row*3+col;
                CubeCell cell = new CubeCell(faceIndex,row,col,gridColors[idx]);
                cells[row][col] = cell;
            }
        }
    }

    public void draw(float[] mvpMatrix) {
        float[] mvpLocal = new float[16];
        Matrix.multiplyMM(mvpLocal,0,mvpMatrix,0,localMatrix,0);
        // 繪製 9 格
        for(int row=0;row<3;row++){
            for(int col=0;col<3;col++){
                cells[row][col].draw(mvpLocal);
            }
        }
        // Cell 已自行繪製邊框，這裡不再額外描繪
    }

    // 設置特定小方塊的顏色
    public void setCellColor(int row, int col, float[] color) {
        if (row >= 0 && row < 3 && col >= 0 && col < 3) {
            cells[row][col].setColor(color);
        }
    }

    // 獲取特定小方塊的顏色
    public float[] getCellColor(int row, int col) {
        if (row >= 0 && row < 3 && col >= 0 && col < 3) {
            // 直接回傳 cell 物件的顏色
            return cells[row][col].getColor();
        }
        return null;
    }

    // 設置格子高亮
    public void setHighlight(int row, int col, boolean highlight) {
        if (row >= 0 && row < 3 && col >= 0 && col < 3) {
            highlightedCells[row][col] = highlight;
        }
    }
    
    // 獲取格子高亮狀態
    public boolean isHighlighted(int row, int col) {
        if (row >= 0 && row < 3 && col >= 0 && col < 3) {
            return highlightedCells[row][col];
        }
        return false;
    }
    
    // 清除所有高亮
    public void clearAllHighlights() {
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 3; col++) {
                highlightedCells[row][col] = false;
            }
        }
    }
    
    // 獲取面的索引
    public int getFaceIndex() {
        return faceIndex;
    }

    public static int loadShader(int type, String shaderCode) {
        int shader = GLES20.glCreateShader(type);
        GLES20.glShaderSource(shader, shaderCode);
        GLES20.glCompileShader(shader);
        return shader;
    }

    private final float[] localMatrix = new float[16];

    public void setLocalMatrix(float[] m){
        System.arraycopy(m,0,localMatrix,0,16);
    }
    public void resetLocalMatrix(){
        Matrix.setIdentityM(localMatrix,0);
    }

    public CubeCell getCell(int row,int col){
        return cells[row][col];
    }
}