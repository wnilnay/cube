package com.example.test;

public class CubeStatusManager {

    private CubeStatusManager(){

    }

    public static String up(String cubeStatus){
        String status1 = changeFaceStatus(cubeStatus,Direction.UP);

        return changeAroundStatus(status1,Direction.UP);
    }
    public static String down(String cubeStatus){
        String status1 = changeFaceStatus(cubeStatus,Direction.DOWN);

        return changeAroundStatus(status1,Direction.DOWN);
    }
    public static String left(String cubeStatus){
        String status1 = changeFaceStatus(cubeStatus,Direction.LEFT);

        return changeAroundStatus(status1,Direction.LEFT);
    }
    public static String right(String cubeStatus){
        String status1 = changeFaceStatus(cubeStatus,Direction.RIGHT);

        return changeAroundStatus(status1,Direction.RIGHT);
    }
    public static String front(String cubeStatus){
        String status1 = changeFaceStatus(cubeStatus,Direction.FORWARD);

        return changeAroundStatus(status1,Direction.FORWARD);
    }
    public static String back(String cubeStatus){
        String status1 = changeFaceStatus(cubeStatus,Direction.BACKWARD);

        return changeAroundStatus(status1,Direction.BACKWARD);
    }
    public static String up_two(String cubeStatus){
        String up1 = up(cubeStatus);
        return up(up1);
    }
    public static String down_two(String cubeStatus){
        String down1 = down(cubeStatus);
        return down(down1);
    }
    public static String left_two(String cubeStatus){
        String left1 = left(cubeStatus);
        return left(left1);
    }
    public static String right_two(String cubeStatus){
        String right1 = right(cubeStatus);
        return right(right1);
    }
    public static String front_two(String cubeStatus){
        String front1 = front(cubeStatus);
        return front(front1);
    }
    public static String back_two(String cubeStatus){
        String back1 = back(cubeStatus);
        return back(back1);
    }
    public static String up_bar(String cubeStatus){
        String up1 = up(cubeStatus);
        String up2 = up(up1);
        return up(up2);
    }
    public static String down_bar(String cubeStatus){
        String down1 = down(cubeStatus);
        String down2 = down(down1);
        return down(down2);
    }
    public static String left_bar(String cubeStatus){
        String left1 = left(cubeStatus);
        String left2 = left(left1);
        return left(left2);
    }
    public static String right_bar(String cubeStatus){
        String right1 = right(cubeStatus);
        String right2 = right(right1);
        return right(right2);
    }
    public static String front_bar(String cubeStatus){
        String front1 = front(cubeStatus);
        String front2 = front(front1);
        return front(front2);
    }
    public static String back_bar(String cubeStatus){
        String back1 = back(cubeStatus);
        String back2 = back(back1);
        return back(back2);
    }

    private static String changeFaceStatus(String cubeStatus, Direction direction){
        //更改面對的顏色面順序
        int startAt = direction.ordinal() * 9;
        int[] FaceOrder = {7,4,1,8,5,2,9,6,3};
        char[] cubeStatusArray = cubeStatus.toCharArray();
        char[] newFaceStatusArray = new char[9];
        char[] faceStatusArray = new char[9];

        for(int i = startAt;i < startAt + FaceOrder.length;i++){
            faceStatusArray[i - startAt] = cubeStatusArray[i];
        }
        for (int i = 0; i < FaceOrder.length; i++) {
            newFaceStatusArray[i] = faceStatusArray[FaceOrder[i] - 1];
        }
        for (int i = startAt;i < startAt + FaceOrder.length;i++){
            cubeStatusArray[i] = newFaceStatusArray[i - startAt];
        }
        return new String(cubeStatusArray);
    }
    private static String changeAroundStatus(String cubeStatus, Direction direction){
        //傳入全部的顏色順序
        //U1 U2 ... U9 R1 ... R9 F1 ... F9 D1 ... D9 L1 ... L9 B1 ... B9
        int[] needAroundOrder = null;
        char[] cubeStatusArray = cubeStatus.toCharArray();
        char[] originalNeedAroundStatus = new char[12];
        char[] newNeedAroundStatus = new char[12];

        switch (direction){
            case UP:
                needAroundOrder = new int[]{39,38,37,48,47,46,12,11,10,21,20,19};
                break;
            case RIGHT:
                needAroundOrder = new int[]{27,24,21,9,6,3,46,49,52,36,33,30};
                break;
            case FORWARD:
                needAroundOrder = new int[]{45,42,39,7,8,9,10,13,16,30,29,28};
                break;
            case DOWN:
                needAroundOrder = new int[]{43,44,45,25,26,27,16,17,18,52,53,54};
                break;
            case LEFT:
                needAroundOrder = new int[]{54,51,48,1,4,7,19,22,25,28,31,34};
                break;
            case BACKWARD:
                needAroundOrder = new int[]{18,15,12,3,2,1,37,40,43,34,35,36};
                break;
        }
        for(int i = 0;i<needAroundOrder.length;i++){
            originalNeedAroundStatus[i] = cubeStatusArray[needAroundOrder[i] - 1];
        }
        // 將右移的部分複製到新陣列的開頭
        for (int i = 0; i < 3; i++) {
            newNeedAroundStatus[i] = originalNeedAroundStatus[originalNeedAroundStatus.length - 3 + i];
        }

        // 將剩餘的部分複製到新陣列
        for (int i = 3; i < newNeedAroundStatus.length; i++) {
            newNeedAroundStatus[i] = originalNeedAroundStatus[i - 3];
        }

        for(int i = 0;i<needAroundOrder.length;i++){
            cubeStatusArray[needAroundOrder[i] - 1] = newNeedAroundStatus[i];
        }
        return new String(cubeStatusArray);
    }
}
