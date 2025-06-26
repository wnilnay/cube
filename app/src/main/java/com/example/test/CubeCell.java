package com.example.test;

import android.opengl.GLES20;
import android.opengl.Matrix;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.ShortBuffer;

/**
 * 單一貼紙 (3×3 的其中一格) 的渲染物件。
 * 內含自己的 localMatrix，可被 Renderer 動畫控制。
 */
public class CubeCell {
    private static final float SIZE = 0.5f;
    private static final float CELL_SIZE = SIZE * 2f / 3f;
    private static final float GAP = 0.01f; // 與鄰格保留的小間隙
    private static final float LINE_WIDTH = 6f;

    // shader (所有 Cell 共用同一支 program)
    private static int program = 0;
    private static int aPositionHandle;
    private static int uColorHandle;
    private static int uMvpMatrixHandle;

    private FloatBuffer vertexBuffer;
    private ShortBuffer drawListBuffer;
    private FloatBuffer innerVertexBuffer; // 內框頂點 (比原頂點略縮小)

    private final float[] color = new float[4];
    private final float[] localMatrix = new float[16];
    private final float[] center = new float[3];

    private static final short[] DRAW_ORDER = {0,1,2,0,2,3};

    // 每個面的法向量（指向外部）
    private static final float[][] FACE_NORMALS = {
            {0, 1, 0},   // Up
            {1, 0, 0},   // Right
            {0, 0, 1},   // Front
            {0, -1, 0},  // Down
            {-1, 0, 0},  // Left
            {0, 0, -1}   // Back
    };

    public CubeCell(int faceIndex, int row, int col, float[] initColor){
        // 建立頂點
        float[] vertices = createVertices(faceIndex,row,col);
        ByteBuffer bb = ByteBuffer.allocateDirect(vertices.length*4);
        bb.order(ByteOrder.nativeOrder());
        vertexBuffer = bb.asFloatBuffer();
        vertexBuffer.put(vertices);
        vertexBuffer.position(0);

        ByteBuffer dlb = ByteBuffer.allocateDirect(DRAW_ORDER.length*2);
        dlb.order(ByteOrder.nativeOrder());
        drawListBuffer = dlb.asShortBuffer();
        drawListBuffer.put(DRAW_ORDER);
        drawListBuffer.position(0);

        System.arraycopy(initColor,0,color,0,4);
        Matrix.setIdentityM(localMatrix,0);

        ensureProgram();

        // after vertexBuffer setup
        center[0] = (vertices[0] + vertices[3] + vertices[6] + vertices[9]) / 4f;
        center[1] = (vertices[1] + vertices[4] + vertices[7] + vertices[10]) / 4f;
        center[2] = (vertices[2] + vertices[5] + vertices[8] + vertices[11]) / 4f;

        // ===== 產生內框頂點 =====
        float[] innerVertices = new float[vertices.length];
        float scaleRatio = (CELL_SIZE - 0.02f) / CELL_SIZE; // 內縮 0.01f 邊框厚度
        float[] normal = FACE_NORMALS[faceIndex];
        final float OFFSET = 0.001f; // 微小位移以避免 Z-fighting
        // 計算此 Cell 中心點 (在平面上)
        float cx = (vertices[0] + vertices[3] + vertices[6] + vertices[9]) / 4f;
        float cy = (vertices[1] + vertices[4] + vertices[7] + vertices[10]) / 4f;
        float cz = (vertices[2] + vertices[5] + vertices[8] + vertices[11]) / 4f;
        for(int i=0;i<4;i++){
            int idx = i*3;
            float x = vertices[idx];
            float y = vertices[idx+1];
            float z = vertices[idx+2];
            innerVertices[idx]   = cx + (x - cx) * scaleRatio + normal[0]*OFFSET;
            innerVertices[idx+1] = cy + (y - cy) * scaleRatio + normal[1]*OFFSET;
            innerVertices[idx+2] = cz + (z - cz) * scaleRatio + normal[2]*OFFSET;
        }
        ByteBuffer ibb = ByteBuffer.allocateDirect(innerVertices.length*4);
        ibb.order(ByteOrder.nativeOrder());
        innerVertexBuffer = ibb.asFloatBuffer();
        innerVertexBuffer.put(innerVertices);
        innerVertexBuffer.position(0);
    }

    private static void ensureProgram(){
        if(program!=0) return;
        String vCode = "uniform mat4 uMVPMatrix;"+
                "attribute vec4 vPosition;"+
                "void main(){"+
                "gl_Position = uMVPMatrix * vPosition;"+
                "}";
        String fCode = "precision mediump float;"+
                "uniform vec4 vColor;"+
                "void main(){"+
                "gl_FragColor = vColor;"+
                "}";
        int vs = CubeFace.loadShader(GLES20.GL_VERTEX_SHADER,vCode);
        int fs = CubeFace.loadShader(GLES20.GL_FRAGMENT_SHADER,fCode);
        program = GLES20.glCreateProgram();
        GLES20.glAttachShader(program,vs);
        GLES20.glAttachShader(program,fs);
        GLES20.glLinkProgram(program);

        aPositionHandle = GLES20.glGetAttribLocation(program,"vPosition");
        uColorHandle = GLES20.glGetUniformLocation(program,"vColor");
        uMvpMatrixHandle = GLES20.glGetUniformLocation(program,"uMVPMatrix");
    }

    private float[] createVertices(int faceIndex,int row,int col){
        float startX = -SIZE + col*CELL_SIZE + GAP;
        float endX   = startX + CELL_SIZE - GAP;
        float startY = SIZE - row*CELL_SIZE - GAP;
        float endY   = startY - CELL_SIZE + GAP;

        switch(faceIndex){
            case 0: // Up
                return new float[]{
                        startX,  SIZE, -endY,
                        endX,    SIZE, -endY,
                        endX,    SIZE, -startY,
                        startX,  SIZE, -startY
                };
            case 1: // Right
                return new float[]{
                        SIZE, -endY, -startX,
                        SIZE, -endY, -endX,
                        SIZE, -startY, -endX,
                        SIZE, -startY, -startX
                };
            case 2: // Front
                return new float[]{
                        startX, -endY, SIZE,
                        endX,   -endY, SIZE,
                        endX,   -startY, SIZE,
                        startX, -startY, SIZE
                };
            case 3: // Down
                return new float[]{
                        startX, -SIZE, -startY,
                        endX,   -SIZE, -startY,
                        endX,   -SIZE, -endY,
                        startX, -SIZE, -endY
                };
            case 4: // Left
                return new float[]{
                        -SIZE, -endY, -endX,
                        -SIZE, -endY, -startX,
                        -SIZE, -startY, -startX,
                        -SIZE, -startY, -endX
                };
            case 5: // Back
                return new float[]{
                        startX, -startY, -SIZE,
                        endX,   -startY, -SIZE,
                        endX,   -endY,   -SIZE,
                        startX, -endY,   -SIZE
                };
        }
        return new float[12];
    }

    public void draw(float[] parentMvp){
        float[] mvpLocal = new float[16];
        Matrix.multiplyMM(mvpLocal,0,parentMvp,0,localMatrix,0);

        GLES20.glUseProgram(program);
        GLES20.glEnableVertexAttribArray(aPositionHandle);
        GLES20.glVertexAttribPointer(aPositionHandle,3,GLES20.GL_FLOAT,false,12,vertexBuffer);

        GLES20.glUniform4fv(uColorHandle,1,color,0);
        GLES20.glUniformMatrix4fv(uMvpMatrixHandle,1,false,mvpLocal,0);

        // ===== 先畫外框(黑色) =====
        final float[] BORDER_COLOR = {0f,0f,0f,1f};
        GLES20.glUniform4fv(uColorHandle,1,BORDER_COLOR,0);
        GLES20.glVertexAttribPointer(aPositionHandle,3,GLES20.GL_FLOAT,false,12,vertexBuffer);
        drawListBuffer.position(0);
        GLES20.glDrawElements(GLES20.GL_TRIANGLES,6,GLES20.GL_UNSIGNED_SHORT,drawListBuffer);

        // ===== 再畫內框(原色) =====
        GLES20.glUniform4fv(uColorHandle,1,color,0);
        GLES20.glVertexAttribPointer(aPositionHandle,3,GLES20.GL_FLOAT,false,12,innerVertexBuffer);
        drawListBuffer.position(0);
        GLES20.glDrawElements(GLES20.GL_TRIANGLES,6,GLES20.GL_UNSIGNED_SHORT,drawListBuffer);

        // 關閉 attribute array
        GLES20.glDisableVertexAttribArray(aPositionHandle);
    }

    // ===== setters =====
    public void setColor(float[] rgba){
        System.arraycopy(rgba,0,color,0,4);
    }

    public void setLocalMatrix(float[] m){
        System.arraycopy(m,0,localMatrix,0,16);
    }
    public void resetLocalMatrix(){
        Matrix.setIdentityM(localMatrix,0);
    }

    public float[] getColor(){
        return color;
    }

    public float[] getCenter(){
        return center;
    }
} 