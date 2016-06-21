package org.igarape.copcast.utils;

import android.content.Context;
import android.os.Vibrator;

/**
 * Created by dborkan on 6/15/16.
 */
public class VibrateUtils {

    /*
        Create a function to vibrate the cell phone in milliseconds
     */
    public static void vibrate(android.content.Context context, int mili) {
        Vibrator v = (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);
        v.vibrate(mili);
    }
}
