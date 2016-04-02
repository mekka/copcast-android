package org.igarape.copcast.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.location.Location;
import android.media.CamcorderProfile;
import android.preference.PreferenceManager;
import android.util.Log;

import org.igarape.copcast.state.IncidentFlagState;

import java.util.UUID;

/**
 * Created by fcavalcanti on 28/10/2014.
 */
public class Globals {

    public static final String AUTH = "AUTH";
    public static final String DATA = "DATA";
    public static final String STREAMING_PORT = "STREAMING_PORT";
    public static final String STREAMING_USER = "STREAMING_USER";
    public static final String SERVER_IP_ADDRESS = "SERVER_IP_ADDRESS";
    public static final String STREAMING_PASSWORD = "STREAMING_PASSWORD";
    public static final String STREAMING_PATH = "STREAMING_PATH";
    public static final String USER_NAME = "USER_NAME";
    public static final String USER_ID = "USER_ID";
    public static final String DIRECTORY_SIZE = "DIRECTORY_SIZE";
    public static final String DIRECTORY_UPLOADED_SIZE = "DIRECTORY_UPLOADED_SIZE";
    public static final long BATTERY_REPEAT_TIME =  1000 * 600; // 10 minutes;
    public static String TAG = Globals.class.getName();
    private static final String PREF_ACCESS_TOKEN = "PREF_ACCESS_TOKEN";
    private static final String PREF_TIME_LOGIN = "PREF_TIME_LOGIN";
    private static final String PREF_USER_LOGIN = "PREF_USER_LOGIN";
    public static final String PROPERTY_REG_ID = "registration_id";
    private static final String PROPERTY_APP_VERSION = "appVersion";
    public static final String SERVER_URL = "server_url";
    private static final String REQUIRE_WIFI_ONLY = "upload_wifi_only";
    private static String accessToken = null;
    private static String userLogin = null;
    private static String serverIpAddress = null;
    private static Integer streamingPort = null;
    private static String streamingUser = null;
    private static String streamingPassword = null;
    private static String streamingPath = null;
    private static String userName = null;
    private static Bitmap userImage = null;
    private static Long directorySize;
    private static Long directoryUploadedSize;
    private static Boolean livestreamToggle = false;
    private static Location lastKnownLocation = null;
    private static int rotation;
    private static IncidentFlagState incidentFlag = IncidentFlagState.NOT_FLAGGED;
    private static String currentVideoPath;
    public static int appCamcoderProfile = CamcorderProfile.QUALITY_QVGA;
    private static String imei;
    private static String simid;
    private static UUID sessionId;
    private static StateManager stateManager;
    private static Integer userId;

    public synchronized static String getAccessToken(Context context) {
        if (accessToken == null) {
            SharedPreferences sharedPrefs = context.getSharedPreferences(AUTH, Context.MODE_PRIVATE);
            accessToken = sharedPrefs.getString(PREF_ACCESS_TOKEN, null);
        }
        return accessToken != null ? "Bearer " + accessToken : null;
    }

    public synchronized static String getPlainToken(Context context) {
        if (accessToken == null) {
            SharedPreferences sharedPrefs = context.getSharedPreferences(AUTH, Context.MODE_PRIVATE);
            accessToken = sharedPrefs.getString(PREF_ACCESS_TOKEN, null);
        }
        return accessToken != null ? accessToken : null;
    }

    public synchronized static void setAccessToken(Context context, String token) {
        SharedPreferences sharedPrefs = context.getSharedPreferences(AUTH, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPrefs.edit();
        editor.putString(PREF_ACCESS_TOKEN, token);
        editor.putLong(PREF_TIME_LOGIN, java.lang.System.currentTimeMillis());
        editor.apply();
        accessToken = token;
        if (accessToken == null) {
            setUserImage(null);
        }
    }

    public static Location getLastKnownLocation() {
        return lastKnownLocation;
    }

    public static void setLastKnownLocation(Location lastKnownLocation) {
        Globals.lastKnownLocation = lastKnownLocation;
    }

    public synchronized static String getUserLogin(Context context) {
        if (userLogin == null) {
            SharedPreferences sharedPrefs = context.getSharedPreferences(AUTH, Context.MODE_PRIVATE);
            userLogin = sharedPrefs.getString(PREF_USER_LOGIN, null);
        }
        return userLogin;
    }

    public synchronized static void storeRegistrationId(Context context, String regId) {
        final SharedPreferences sharedPrefs = context.getSharedPreferences(AUTH, Context.MODE_PRIVATE);
        int appVersion = getAppVersion(context);
        Log.i(TAG, "Saving regId on app version " + appVersion);
        SharedPreferences.Editor editor = sharedPrefs.edit();
        editor.putString(PROPERTY_REG_ID, regId);
        editor.putInt(PROPERTY_APP_VERSION, appVersion);
        editor.apply();
    }

    public synchronized static String getRegistrationId(Context context) {
        final SharedPreferences sharedPrefs = context.getSharedPreferences(AUTH, Context.MODE_PRIVATE);
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
        SharedPreferences sharedPrefs = context.getSharedPreferences(AUTH, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPrefs.edit();
        editor.putString(PREF_USER_LOGIN, login);
        editor.apply();
    }

    public static Bitmap getUserImage() {
        return userImage;

    }

    public static void setUserImage(Bitmap userImage) {
        Globals.userImage = userImage;
    }

    public static void setStreamingPort(Context context,Integer streamingPort) {
        SharedPreferences sharedPrefs = context.getSharedPreferences(DATA, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPrefs.edit();
        editor.putInt(STREAMING_PORT, streamingPort);
        editor.commit();
        Globals.streamingPort = streamingPort;
    }

    public static void setStreamingUser(Context context,String streamingUser) {
        SharedPreferences sharedPrefs = context.getSharedPreferences(DATA, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPrefs.edit();
        editor.putString(STREAMING_USER, streamingUser);
        editor.commit();
        Globals.streamingUser = streamingUser;
    }

    public static void setServerIpAddress(Context context,String serverIpAddress) {
        SharedPreferences sharedPrefs = context.getSharedPreferences(DATA, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPrefs.edit();
        editor.putString(SERVER_IP_ADDRESS, serverIpAddress);
        editor.commit();
        Globals.serverIpAddress = serverIpAddress;
    }

    public static void setStreamingPassword(Context context,String streamingPassword) {
        SharedPreferences sharedPrefs = context.getSharedPreferences(DATA, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPrefs.edit();
        editor.putString(STREAMING_PASSWORD, streamingPassword);
        editor.commit();
        Globals.streamingPassword = streamingPassword;
    }

    public static void setStreamingPath(Context context,String streamingPath) {
        if (!streamingPath.startsWith("/")){
            streamingPath = "/"+streamingPath;
        }
        SharedPreferences sharedPrefs = context.getSharedPreferences(DATA, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPrefs.edit();
        editor.putString(STREAMING_PATH, streamingPath);
        editor.commit();
        Globals.streamingPath = streamingPath;
    }

    public static void setUserName(Context context,String userName) {
        SharedPreferences sharedPrefs = context.getSharedPreferences(DATA, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPrefs.edit();
        editor.putString(USER_NAME, userName);
        editor.apply();
        Globals.userName = userName;
    }

    public static String getUserName(Context context) {
        if (userName == null) {
            SharedPreferences sharedPrefs = context.getSharedPreferences(DATA, Context.MODE_PRIVATE);
            userName = sharedPrefs.getString(USER_NAME, null);
        }
        return userName;
    }

    public static void clear(Context context) {
        SharedPreferences sharedPrefs = context.getSharedPreferences(AUTH, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPrefs.edit();
        editor.clear().commit();

        context.getSharedPreferences(DATA, Context.MODE_PRIVATE);
        editor = sharedPrefs.edit();
        editor.clear().commit();

        accessToken = null;
        userLogin = null;
        userName = null;
        userImage = null;
        livestreamToggle = false;
        serverIpAddress = null;
        streamingPort = 1935;
        streamingUser = null;
        streamingPassword = null;
        streamingPath = null;
    }

    public static void setDirectorySize(Context context,Long directorySize) {
        SharedPreferences sharedPrefs = context.getSharedPreferences(DATA, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPrefs.edit();
        editor.putLong(DIRECTORY_SIZE, directorySize);
        editor.apply();
        Globals.directorySize = directorySize;
        setDirectoryUploadedSize(context, Long.valueOf(0));
    }

    public static Long getDirectoryUploadedSize(Context context) {

        if (directoryUploadedSize == null) {
            SharedPreferences sharedPrefs = context.getSharedPreferences(DATA, Context.MODE_PRIVATE);
            directoryUploadedSize = sharedPrefs.getLong(DIRECTORY_UPLOADED_SIZE, 0);
        }
        return directoryUploadedSize;
    }

    public static void setDirectoryUploadedSize(Context context,Long directoryUploadedSize) {
        SharedPreferences sharedPrefs = context.getSharedPreferences(DATA, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPrefs.edit();
        editor.putLong(DIRECTORY_UPLOADED_SIZE, directoryUploadedSize);
        editor.apply();
        Globals.directoryUploadedSize = directoryUploadedSize;
    }

    public static Long getDirectorySize(Context context) {
        if (directorySize == null) {
            SharedPreferences sharedPrefs = context.getSharedPreferences(DATA, Context.MODE_PRIVATE);
            directorySize = sharedPrefs.getLong(DIRECTORY_SIZE, 0);
        }
        return directorySize/1024;
    }

    public static String getServerIpAddress(Context context) {
        if (serverIpAddress == null) {
            SharedPreferences sharedPrefs = context.getSharedPreferences(DATA, Context.MODE_PRIVATE);
            serverIpAddress = sharedPrefs.getString(SERVER_IP_ADDRESS, null);
        }
        return serverIpAddress;
    }

    public static Integer getStreamingPort(Context context) {
        if (streamingPort == null) {
            SharedPreferences sharedPrefs = context.getSharedPreferences(DATA, Context.MODE_PRIVATE);
            streamingPort = sharedPrefs.getInt(STREAMING_PORT, 1935);
        }
        return streamingPort;
    }

    public static String getStreamingUser(Context context) {
        if (streamingUser == null) {
            SharedPreferences sharedPrefs = context.getSharedPreferences(DATA, Context.MODE_PRIVATE);
            streamingUser = sharedPrefs.getString(STREAMING_USER, null);
        }
        return streamingUser;
    }

    public static String getStreamingPassword(Context context) {
        if (streamingPassword == null) {
            SharedPreferences sharedPrefs = context.getSharedPreferences(DATA, Context.MODE_PRIVATE);
            streamingPassword = sharedPrefs.getString(STREAMING_PASSWORD, null);
        }
        return streamingPassword;
    }

    public static String getStreamingPath(Context context) {
        if (streamingPath == null) {
            SharedPreferences sharedPrefs = context.getSharedPreferences(DATA, Context.MODE_PRIVATE);
            streamingPath = sharedPrefs.getString(STREAMING_PATH, null);
        }
        return streamingPath;
    }
    public static void setLivestreamToggle(boolean value) {
        livestreamToggle = value;
    }

    public static Boolean getLivestreamToggle(){
        return livestreamToggle;
    }
    public static void setRotation(int rotation) {
        Globals.rotation = rotation;
    }

    public static int getRotation() {
        return rotation;
    }

    public static String getServerUrl(Context context){
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(context);
        return sharedPref.getString(SERVER_URL, "");
    }

    public static Boolean isWifiOnly(Context context){
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(context);
        return sharedPref.getBoolean(REQUIRE_WIFI_ONLY, true);
    }

    public static String getCurrentVideoPath() {
        return currentVideoPath;
    }

    public static void setCurrentVideoPath(String currentVideoPath) {
        Globals.currentVideoPath = currentVideoPath;
    }

    public static IncidentFlagState getIncidentFlag() {
        return incidentFlag;
    }

    public static void setIncidentFlag(IncidentFlagState incidentFlag) {
        Globals.incidentFlag = incidentFlag;
    }

    public static String getSimid() {
        return simid;
    }

    public static void setSimid(String simid) {
        Globals.simid = simid;
    }

    public static String getImei() {
        return imei;
    }

    public static void setImei(String imei) {
        Globals.imei = imei;
    }

    public static void sessionInit() {
        sessionId = UUID.randomUUID();
    }

    public static UUID getSessionID() {
        return sessionId;
    }

    public static void initStateManager(Context context) {
        stateManager = new StateManager(context);
    }

    public static StateManager getStateManager() {
        return stateManager;
    }

    public static void setUserId(Context context,int userId) {
        SharedPreferences sharedPrefs = context.getSharedPreferences(DATA, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPrefs.edit();
        editor.putString(USER_ID, String.valueOf(new Integer(userId)));
        editor.apply();
        Globals.userId = userId;
    }

    public static Integer getUserId(Context context) {
        if (userId == null) {
            SharedPreferences sharedPrefs = context.getSharedPreferences(DATA, Context.MODE_PRIVATE);
            userId = Integer.parseInt(sharedPrefs.getString(USER_ID, null));
        }
        return userId;
    }

}
