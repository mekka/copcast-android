package org.igarape.copcast.utils;

import android.content.Intent;
import android.os.BatteryManager;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

/**
 * Created by FCavalcanti on 7/17/15.
 */
public class BatteryUtils {

    private static final String TAG = BatteryUtils.class.getName();

    private static Battery battery;

    private static float BATTERY_PERCENTAGE_LIMIT = 20;

    private static final SimpleDateFormat df;

    static{
        TimeZone tz = TimeZone.getTimeZone("UTC");
        df = new SimpleDateFormat(FileUtils.DATE_FORMAT);
        df.setTimeZone(tz);
    }

    public static void updateValues(Intent batteryStatus){
        int batteryLevel = batteryStatus.getIntExtra(BatteryManager.EXTRA_LEVEL, 0);
        int maxLevel = batteryStatus.getIntExtra(BatteryManager.EXTRA_SCALE, 0);


        battery = new Battery( ((float) batteryLevel / (float) maxLevel) * 100, batteryStatus.getIntExtra(BatteryManager.EXTRA_STATUS,0), batteryStatus.getIntExtra(BatteryManager.EXTRA_TEMPERATURE,0), batteryStatus.getIntExtra(BatteryManager.EXTRA_PLUGGED,0), batteryStatus.getIntExtra(
                BatteryManager.EXTRA_HEALTH,
                BatteryManager.BATTERY_HEALTH_UNKNOWN), new Date());
        Log.d(TAG, "updateValues with batteryPercentage=[" + battery.batteryPercentage + "]");
    }

    public static JSONObject buildJson(){
       return battery != null ? battery.getJson() : null;
    }

    public static float getBatteryPercentage() {
        return battery.batteryPercentage;
    }

    public static int getStatus() {
        return battery.status;
    }

    public static int getTemperature() {
        return battery.temperature;
    }

    public static int getPlugged() {
        return battery.plugged;
    }

    public static int getBatteryHealth() {
        return battery.batteryHealth;
    }

    private static class Battery {
        private Date date;
        private Float batteryPercentage;
        private int status;
        private int temperature;
        private int plugged;
        private int batteryHealth;

        private Battery(Float batteryPercentage, int status, int temperature, int plugged, int batteryHealth, Date date) {
            this.batteryPercentage = batteryPercentage;
            this.status = status;
            this.temperature = temperature;
            this.plugged = plugged;
            this.batteryHealth = batteryHealth;
            this.date = date;
        }

        public JSONObject getJson() {
            JSONObject json = new JSONObject();

            try {
                json.put("batteryHealth", batteryHealth);
                json.put("batteryPercentage", batteryPercentage);
                json.put("plugged", plugged);
                json.put("status", batteryHealth);
                json.put("temperature", temperature);
                json.put("date", df.format(date));
            } catch (JSONException e) {
                e.printStackTrace();
            }
            return json;
        }
    }
}
