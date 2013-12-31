package com.fightevil.yota;

/**
 * Created by admin on 24.12.13.
 */
public class Log {
    public static void d (String tag, String msg) {
        if (Keys.debug)
            android.util.Log.d(tag, msg);
    }
}
