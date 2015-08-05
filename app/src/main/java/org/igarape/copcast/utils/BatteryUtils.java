package org.igarape.copcast.utils;

import android.content.Intent;
import android.os.BatteryManager;
import android.util.Log;

/**
 * Created by FCavalcanti on 7/17/15.
 */
public class BatteryUtils {

    private static final String TAG = BatteryUtils.class.getName();

    private BatteryUtils(){}

    private volatile static BatteryUtils instance = null;

    private static Float batteryPercentage;
    private static int status;
    private static int temperature;
    private static int plugged;
    private static int batteryHealth;

    private static float BATTERY_PERCENTAGE_LIMIT = 20;

    public static BatteryUtils getSingletonInstance() {
        if (null == instance) {
            synchronized (BatteryUtils.class){
                if (null == instance) {
                    instance = new BatteryUtils();
                }
            }
        }
        return instance;
    }

    public static boolean shouldUpload(){
        return (null != batteryPercentage ? batteryPercentage > BATTERY_PERCENTAGE_LIMIT:true);
    }

    public static void updateValues(Intent batteryStatus){
        int batteryLevel = batteryStatus.getIntExtra(BatteryManager.EXTRA_LEVEL, 0);
        int maxLevel = batteryStatus.getIntExtra(BatteryManager.EXTRA_SCALE, 0);
        BatteryUtils.batteryHealth = batteryStatus.getIntExtra(
                BatteryManager.EXTRA_HEALTH,
                BatteryManager.BATTERY_HEALTH_UNKNOWN);
        BatteryUtils.batteryPercentage = ((float) batteryLevel / (float) maxLevel) * 100;
        BatteryUtils.plugged = batteryStatus.getIntExtra(BatteryManager.EXTRA_PLUGGED,0);
        BatteryUtils.status = batteryStatus.getIntExtra(BatteryManager.EXTRA_STATUS,0);
        BatteryUtils.temperature = batteryStatus.getIntExtra(BatteryManager.EXTRA_TEMPERATURE,0);
        Log.d(TAG, "updateValues with batteryPercentage=[" + BatteryUtils.batteryPercentage + "]");
    }

    public static float getBatteryPercentage() {
        return batteryPercentage;
    }

    public static int getStatus() {
        return status;
    }

    public static int getTemperature() {
        return temperature;
    }

    public static int getPlugged() {
        return plugged;
    }

    public static int getBatteryHealth() {
        return batteryHealth;
    }
}
