package org.igarape.copcast.utils;

import android.app.ActivityManager;
import android.content.Context;

/**
 * Created by bruno on 11/14/14.
 */
public class ServiceUtils {

    public static boolean isMyServiceRunning(final Class clazz, Context context) {
        ActivityManager manager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (clazz.getName().equals(service.service.getClassName())) {
                return true;
            }
        }
        return false;
    }
}
