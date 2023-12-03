package com.nyu.db.utils;


public class TimeManager {
    private static long time = 0;

    public static long getTime() {
        return TimeManager.time;
    }

    public static long incrementTime() {
        TimeManager.time += 1;
        return TimeManager.time;
    }
}
