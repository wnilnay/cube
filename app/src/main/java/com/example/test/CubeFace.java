package com.example.test;

import android.opengl.GLES20;
import android.view.View;

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
    private int mPositionHandle;
    private int mColorHandle;
    private int mMVPMatrixHandle;

    // 面的索引
    private int faceIndex;
    // 3x3 网格的颜色数组
    private float[][] gridColors;
    
    // 高亮狀態 - 記錄哪些格子被高亮
    private boolean[][] highlightedCells;
    
    // 高亮顏色 - 使用黑色
    private final float[] HIGHLIGHT_COLOR = {0.0f, 0.0f, 0.0f, 1.0f}; // 黑色，不透明

    // 黑色边框颜色
    private final float[] BORDER_COLOR = {0.0f, 0.0f, 0.0f, 1.0f};

    // 默认的魔方颜色
    private final float[][] DEFAULT_FACE_COLORS = {
            {1.0f, 1.0f, 1.0f, 1.0f}, // 白色 (上面 - Up)
            {0.0f, 0.0f, 1.0f, 1.0f}, // 藍色 (右面 - Right)
            {1.0f, 0.0f, 0.0f, 1.0f}, // 紅色 (前面 - Forward)
            {1.0f, 1.0f, 0.0f, 1.0f}, // 黃色 (下面 - Down)
            {0.0f, 1.0f, 0.0f, 1.0f}, // 綠色 (左面 - Left)
            {1.0f, 0.5f, 0.0f, 1.0f}  // 橙色 (後面 - Backward)
    };

    // 为3x3网格生成顶点坐标
    private float[] getVerticesForFace(int faceIndex) {
        float size = 0.5f;
        float cellSize = size * 2 / 3;
        float gap = 0.01f; // 小间隙

        // 每个面需要9个小方块，每个小方块4个顶点
        float[] vertices = new float[9 * 4 * 3];

        int vertexIndex = 0;

        // 生成3x3网格
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 3; col++) {
                // 计算小方块的位置
                float startX = -size + col * cellSize + gap;
                float endX = startX + cellSize - gap;
                float startY = size - row * cellSize - gap;
                float endY = startY - cellSize + gap;

                // 根据面的索引生成对应的顶点
                float[] cellVertices = getCellVertices(faceIndex, startX, endX, startY, endY, size);

                // 将顶点添加到数组中
                for (int i = 0; i < cellVertices.length; i++) {
                    vertices[vertexIndex++] = cellVertices[i];
                }
            }
        }

        return vertices;
    }

    // 生成边框顶点
    private float[] getBorderVerticesForFace(int faceIndex) {
        float size = 0.5f;
        float cellSize = size * 2 / 3;

        // 生成网格线 - 4条垂直线 + 4条水平线
        float[] borderVertices = new float[8 * 4 * 3]; // 8条线，每条4个顶点，每个顶点3个坐标
        int vertexIndex = 0;

        // 垂直线
        for (int i = 0; i < 4; i++) {
            float x = -size + i * cellSize;
            float[] lineVertices = getBorderLine(faceIndex, x, -size, x, size, true);
            for (float v : lineVertices) {
                borderVertices[vertexIndex++] = v;
            }
        }

        // 水平线
        for (int i = 0; i < 4; i++) {
            float y = size - i * cellSize;
            float[] lineVertices = getBorderLine(faceIndex, -size, y, size, y, false);
            for (float v : lineVertices) {
                borderVertices[vertexIndex++] = v;
            }
        }

        return borderVertices;
    }

    // 生成单条边框线的顶点
    private float[] getBorderLine(int faceIndex, float x1, float y1, float x2, float y2, boolean isVertical) {
        float size = 0.5f;
        float lineWidth = 0.01f;

        float[] vertices = new float[12]; // 4个顶点，每个3个坐标

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

    // 为单个小方块生成顶点
    private float[] getCellVertices(int faceIndex, float startX, float endX, float startY, float endY, float size) {
        switch (faceIndex) {
            case 0: // 上面 (Up) (从上方看，顺时针)
                return new float[] {
                        startX, size, -endY,
                        endX, size, -endY,
                        endX, size, -startY,
                        startX, size, -startY
                };
            case 1: // 右面 (Right) (从右方看，顺时针)
                return new float[] {
                        size, -endY, -startX,
                        size, -endY, -endX,
                        size, -startY, -endX,
                        size, -startY, -startX
                };
            case 2: // 前面 (Forward) (从前方看，顺时针)
                return new float[] {
                        startX, -endY, size,
                        endX, -endY, size,
                        endX, -startY, size,
                        startX, -startY, size
                };
            case 3: // 下面 (Down) (从下方看，顺时针)
                return new float[] {
                        startX, -size, -startY,
                        endX, -size, -startY,
                        endX, -size, -endY,
                        startX, -size, -endY
                };
            case 4: // 左面 (Left) (从左方看，顺时针)
                return new float[] {
                        -size, -endY, -endX,
                        -size, -endY, -startX,
                        -size, -startY, -startX,
                        -size, -startY, -endX
                };
            case 5: // 後面 (Backward) (从后方看，顺时针)
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

    // 绘制顺序 - 9个小方块，每个2个三角形
    private short[] generateDrawOrder() {
        short[] drawOrder = new short[9 * 6]; // 9个方块 * 6个索引

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

    // 边框绘制顺序
    private short[] generateBorderDrawOrder() {
        short[] drawOrder = new short[8 * 6]; // 8条线 * 6个索引

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

        // 初始化3x3网格颜色，全部使用该面的默认颜色
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

        // 初始化顶点缓冲区
        ByteBuffer bb = ByteBuffer.allocateDirect(vertices.length * 4);
        bb.order(ByteOrder.nativeOrder());
        vertexBuffer = bb.asFloatBuffer();
        vertexBuffer.put(vertices);
        vertexBuffer.position(0);

        // 初始化边框顶点缓冲区
        ByteBuffer bbb = ByteBuffer.allocateDirect(borderVertices.length * 4);
        bbb.order(ByteOrder.nativeOrder());
        borderVertexBuffer = bbb.asFloatBuffer();
        borderVertexBuffer.put(borderVertices);
        borderVertexBuffer.position(0);

        // 初始化绘制顺序缓冲区
        ByteBuffer dlb = ByteBuffer.allocateDirect(drawOrder.length * 2);
        dlb.order(ByteOrder.nativeOrder());
        drawListBuffer = dlb.asShortBuffer();
        drawListBuffer.put(drawOrder);
        drawListBuffer.position(0);

        // 初始化边框绘制顺序缓冲区
        ByteBuffer bdlb = ByteBuffer.allocateDirect(borderDrawOrder.length * 2);
        bdlb.order(ByteOrder.nativeOrder());
        borderDrawListBuffer = bdlb.asShortBuffer();
        borderDrawListBuffer.put(borderDrawOrder);
        borderDrawListBuffer.position(0);

        // 准备shader
        int vertexShader = loadShader(GLES20.GL_VERTEX_SHADER, vertexShaderCode);
        int fragmentShader = loadShader(GLES20.GL_FRAGMENT_SHADER, fragmentShaderCode);

        mProgram = GLES20.glCreateProgram();
        GLES20.glAttachShader(mProgram, vertexShader);
        GLES20.glAttachShader(mProgram, fragmentShader);
        GLES20.glLinkProgram(mProgram);
    }

    public void draw(float[] mvpMatrix) {
        GLES20.glUseProgram(mProgram);

        // 先绘制彩色方块
        mPositionHandle = GLES20.glGetAttribLocation(mProgram, "vPosition");
        GLES20.glEnableVertexAttribArray(mPositionHandle);
        GLES20.glVertexAttribPointer(mPositionHandle, 3, GLES20.GL_FLOAT, false, 12, vertexBuffer);

        mColorHandle = GLES20.glGetUniformLocation(mProgram, "vColor");
        mMVPMatrixHandle = GLES20.glGetUniformLocation(mProgram, "uMVPMatrix");
        GLES20.glUniformMatrix4fv(mMVPMatrixHandle, 1, false, mvpMatrix, 0);

        // 绘制9个小方块，每个使用不同的颜色
        for (int i = 0; i < 9; i++) {
            int row = i / 3;
            int col = i % 3;
            
            // 如果格子被高亮，使用高亮顏色，否則使用原來的顏色
            if (highlightedCells[row][col]) {
                GLES20.glUniform4fv(mColorHandle, 1, HIGHLIGHT_COLOR, 0);
            } else {
                GLES20.glUniform4fv(mColorHandle, 1, gridColors[i], 0);
            }

            drawListBuffer.position(i * 6);
            GLES20.glDrawElements(GLES20.GL_TRIANGLES, 6, GLES20.GL_UNSIGNED_SHORT, drawListBuffer);
        }

        // 然后绘制黑色边框
        GLES20.glVertexAttribPointer(mPositionHandle, 3, GLES20.GL_FLOAT, false, 12, borderVertexBuffer);
        GLES20.glUniform4fv(mColorHandle, 1, BORDER_COLOR, 0);

        for (int i = 0; i < 8; i++) {
            borderDrawListBuffer.position(i * 6);
            GLES20.glDrawElements(GLES20.GL_TRIANGLES, 6, GLES20.GL_UNSIGNED_SHORT, borderDrawListBuffer);
        }

        GLES20.glDisableVertexAttribArray(mPositionHandle);
    }

    // 设置特定小方块的颜色
    public void setCellColor(int row, int col, float[] color) {
        if (row >= 0 && row < 3 && col >= 0 && col < 3) {
            int index = row * 3 + col;
            System.arraycopy(color, 0, gridColors[index], 0, 4);
        }
    }

    // 获取特定小方块的颜色
    public float[] getCellColor(int row, int col) {
        if (row >= 0 && row < 3 && col >= 0 && col < 3) {
            int index = row * 3 + col;
            return gridColors[index].clone();
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
}