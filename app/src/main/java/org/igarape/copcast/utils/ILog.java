package org.igarape.copcast.utils;

import android.util.Log;

/**
 * Created by martelli on 12/9/15.
 */
public class ILog {
    public static void d(String tag, String msg) {
        Log.d(tag, msg);
    }

    public static void d(String tag, String msg, Exception e) {
        Log.d(tag, msg);
        Log.d(tag, e.toString());
    }

    public static void w(String tag, String msg) {
        Log.w(tag, msg);
    }

    public static void w(String tag, String msg, Exception e) {
        Log.w(tag, msg);
        Log.d(tag, e.toString());
    }

    public static void e(String tag, String msg) {
        Log.e(tag, msg);
    }

    public static void e(String tag, String msg, Exception e) {
        Log.e(tag, msg);
        Log.d(tag, e.toString());
    }

    public static void i(String tag, String msg) {
        Log.e(tag, msg);
    }

    public static void i(String tag, String msg, Exception e) {
        Log.i(tag, msg);
        Log.d(tag, e.toString());
    }
}
