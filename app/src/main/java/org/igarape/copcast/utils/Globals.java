package org.igarape.copcast.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.util.Log;

import org.igarape.copcast.BuildConfig;

/**
 * Created by fcavalcanti on 28/10/2014.
 */
public class Globals {

    public static String TAG = Globals.class.getName();
    public static final String SENDER_ID = "319635303076";
    private static final String PREF_ACCESS_TOKEN = "PREF_ACCESS_TOKEN";
    private static final String PREF_TIME_LOGIN = "PREF_TIME_LOGIN";
    private static final String PREF_USER_LOGIN = "PREF_USER_LOGIN";
    public static final String PROPERTY_REG_ID = "registration_id";
    private static final String PROPERTY_APP_VERSION = "appVersion";
    public static final String SERVER_URL = BuildConfig.serverUrl;
    private static String accessToken = null;
    private static String userLogin = null;
    private static String serverIpAddress = "";
    private static Integer streamingPort = 1935;
    private static String streamingUser = "";
    private static String streamingPassword = "";
    private static String streamingPath = "";
    private static String userName = null;
    private static Bitmap userImage = null;

    public synchronized static String getAccessToken(Context context) {
        if (accessToken == null) {
            SharedPreferences sharedPrefs = context.getSharedPreferences("AUTH", Context.MODE_PRIVATE);
            accessToken = sharedPrefs.getString(PREF_ACCESS_TOKEN, null);
        }
        return accessToken != null ? "Bearer " + accessToken : null;
    }

    public synchronized static void setAccessToken(Context context, String token) {
        SharedPreferences sharedPrefs = context.getSharedPreferences("AUTH", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPrefs.edit();
        editor.putString(PREF_ACCESS_TOKEN, token);
        editor.putLong(PREF_TIME_LOGIN, java.lang.System.currentTimeMillis());
        editor.commit();
        accessToken = token;
        if (accessToken == null){
            setUserImage(null);
        }
    }

    public synchronized static String getUserLogin(Context context) {
        if (userLogin == null) {
            SharedPreferences sharedPrefs = context.getSharedPreferences("AUTH", Context.MODE_PRIVATE);
            userLogin = sharedPrefs.getString(PREF_USER_LOGIN, null);
        }
        return userLogin;
    }

    public synchronized static void storeRegistrationId(Context context, String regId) {
        final SharedPreferences sharedPrefs = context.getSharedPreferences("AUTH", Context.MODE_PRIVATE);
        int appVersion = getAppVersion(context);
        Log.i(TAG, "Saving regId on app version " + appVersion);
        SharedPreferences.Editor editor = sharedPrefs.edit();
        editor.putString(PROPERTY_REG_ID, regId);
        editor.putInt(PROPERTY_APP_VERSION, appVersion);
        editor.commit();
    }

    public synchronized static String getRegistrationId(Context context) {
        final SharedPreferences sharedPrefs = context.getSharedPreferences("AUTH", Context.MODE_PRIVATE);
        String registrationId = sharedPrefs.getString(PROPERTY_REG_ID, "");
        if (registrationId.isEmpty()) {
            Log.i(TAG, "Registration not found.");
            return "";
        }
        int registeredVersion = sharedPrefs.getInt(PROPERTY_APP_VERSION, Integer.MIN_VALUE);
        int currentVersion = getAppVersion(context);
        if (registeredVersion != currentVersion) {
            Log.i(TAG, "App version changed.");
            return "";
        }
        return registrationId;
    }

    private static int getAppVersion(Context context) {
        try {
            PackageInfo packageInfo = context.getPackageManager()
                    .getPackageInfo(context.getPackageName(), 0);
            return packageInfo.versionCode;
        } catch (PackageManager.NameNotFoundException e) {
            // should never happen
            throw new RuntimeException("Could not get package name: " + e);
        }
    }

    public synchronized static void setUserLogin(Context context, String login) {
        SharedPreferences sharedPrefs = context.getSharedPreferences("AUTH", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPrefs.edit();
        editor.putString(PREF_USER_LOGIN, login);
        editor.commit();
    }

    public static Bitmap getUserImage() {
        return userImage;
    }

    public static void setUserImage(Bitmap userImage) {
        Globals.userImage = userImage;
    }

    public static void setStreamingPort(Integer streamingPort) {
        Globals.streamingPort = streamingPort;
    }

    public static void setStreamingUser(String streamingUser) {
        Globals.streamingUser = streamingUser;
    }

    public static void setServerIpAddress(String serverIpAddress) {
        Globals.serverIpAddress = serverIpAddress;
    }

    public static void setStreamingPassword(String streamingPassword) {
        Globals.streamingPassword = streamingPassword;
    }

    public static void setStreamingPath(String streamingPath) {
        Globals.streamingPath = streamingPath;
    }

    public static void setUserName(String userName) {
        Globals.userName = userName;
    }

    public static void setAccessToken(String accessToken) {
        Globals.accessToken = accessToken;
    }
}
