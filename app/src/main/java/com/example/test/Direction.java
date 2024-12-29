package com.example.test;

public enum Direction {
    UP,
    RIGHT,
    FORWARD,
    DOWN,
    LEFT,
    BACKWARD;
    public static Direction getEnumFromOrdinal(int ordinal) {
        if (ordinal < 0 || ordinal >= Direction.values().length) {
            throw new IllegalArgumentException("Invalid ordinal: " + ordinal);
        }
        return Direction.values()[ordinal];  // 根據 ordinal 獲取枚舉實例
    }
}
