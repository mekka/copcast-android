package com.igarape.copcast.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.util.Log;

/**
 * Created by fcavalcanti on 28/10/2014.
 */
public class Globals {

    public static final String SENDER_ID = "319635303076";
    private static final String PREF_ACCESS_TOKEN = "PREF_ACCESS_TOKEN";
    private static final String PREF_TIME_LOGIN = "PREF_TIME_LOGIN";
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

    public static Bitmap getUserImage() {
        return userImage;
    }

    public static void setUserImage(Bitmap userImage) {
        Globals.userImage = userImage;
    }


}
