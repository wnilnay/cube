package com.example.test;

public class ColorDirectionManager {
    private Direction yellowDirection;
    private Direction orangeDirection;
    private Direction greenDirection;
    private Direction whiteDirection;
    private Direction redDirection;
    private Direction blueDirection;

    public void setYellowDirection(Direction yellowDirection){
        this.yellowDirection = yellowDirection;
    }
    public void setOrangeDirection(Direction orangeDirection){
        this.orangeDirection = orangeDirection;
    }
    public void setGreenDirection(Direction greenDirection){
        this.greenDirection = greenDirection;
    }
    public void setWhiteDirection(Direction whiteDirection){
        this.whiteDirection = whiteDirection;
    }
    public void setRedDirection(Direction redDirection){
        this.redDirection = redDirection;
    }
    public void setBlueDirection(Direction blueDirection){
        this.blueDirection = blueDirection;
    }
    public Direction getYellowDirection(){
        return yellowDirection;
    }
    public Direction getOrangeDirection(){
        return orangeDirection;
    }
    public Direction getGreenDirection(){
        return greenDirection;
    }
    public Direction getWhiteDirection(){
        return whiteDirection;
    }
    public Direction getRedDirection(){
        return redDirection;
    }
    public Direction getBlueDirection(){
        return blueDirection;
    }
}
