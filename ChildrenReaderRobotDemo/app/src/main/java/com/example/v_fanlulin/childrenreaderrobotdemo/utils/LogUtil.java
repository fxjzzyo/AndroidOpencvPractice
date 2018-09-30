package com.example.v_fanlulin.childrenreaderrobotdemo.utils;

import android.util.Log;

import java.util.HashMap;

public class LogUtil {
    
    public static final boolean DEBUG = true;
    
    private static HashMap<String, Long> mMap = null;
    
    static {
        if (DEBUG) {
            mMap = new HashMap<String, Long>();
        }
    }

    public static void logV(String tag, String msg, Object... args) {
        if (!DEBUG) {
            return;
        }
        if (args != null && args.length > 0) {
            msg = String.format(msg, args);
        }
        Log.v(tag, msg);
    }

    public static void logE(String tag, String msg, Object... args) {
        if (!DEBUG) {
            return;
        }
        if (args != null) {
            msg = String.format(msg, args);
        }
        Log.e(tag, msg);
    }

    public static void logE(String tag, String msg, Exception e) {
        if (e != null) {
            msg = msg + e.getMessage();
        }
        logE(tag, msg);
    }

    public static void logE(String tag, Exception e) {
        String msg = e.getMessage();
        logE(tag, msg);
    }

    public static void startLogTime(String tag) {
        if (!DEBUG) {
            return;
        }
        if (mMap.containsKey(tag)) {
            return;
        }
        long time = System.currentTimeMillis();
        mMap.put(tag, time);
    }

    public static void stopLogTime(String tag) {
        if (!DEBUG) {
            return;
        }
        if (!mMap.containsKey(tag)) {
            return;
        }
        long time = System.currentTimeMillis();
        long startTime = mMap.get(tag);
        mMap.remove(tag);
        Log.v("Performance", tag + " cost " + (time - startTime) + "ms");
    }
}
