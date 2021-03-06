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

import org.igarape.copcast.R;
import org.igarape.copcast.state.IncidentFlagState;
import org.igarape.copcast.state.State;
import org.igarape.webrecorder.enums.Orientation;

import java.util.UUID;

/**
 * Created by fcavalcanti on 28/10/2014.
 */
public class Globals {

    public static final String APP_REGISTERED = "APP_REGISTERED";
    public static final String AUTH = "AUTH";
    public static final String DATA = "DATA";
    public static final String USER_NAME = "USER_NAME";
    public static final String USER_ID = "USER_ID";
    public static final String DIRECTORY_SIZE = "DIRECTORY_SIZE";
    public static final String DIRECTORY_UPLOADED_SIZE = "DIRECTORY_UPLOADED_SIZE";
    public static final long BATTERY_REPEAT_TIME =  1000 * 600; // 10 minutes;
    public static String TAG = Globals.class.getName();
    private static final String PREF_ACCESS_TOKEN = "PREF_ACCESS_TOKEN";
    private static final String PREF_TIME_LOGIN = "PREF_TIME_LOGIN";
    private static final String PREF_USER_LOGIN = "PREF_USER_LOGIN";
    private static final String PREF_IMEI = "PREF_IMEI";
    private static final String HAS_VIDEO_PLAYBACK = "HAS_VIDEO_PLAYBACK";
    private static final String SHOW_FEEDBACK = "SHOW_FEEDBACK";
    private static final String PREF_SIMID = "PREF_SIMID";
    public static final String PROPERTY_REG_ID = "registration_id";
    private static final String PROPERTY_APP_VERSION = "appVersion";
    public static final String SERVER_URL = "server_url";
    private static final String REQUIRE_WIFI_ONLY = "upload_wifi_only";
    private static final String AUTOMATIC_UPLOAD = "automatic_upload";
    private static final String DEVICE_ORIENTATION = "device_orientation";
    private static final String CODEC_NAME = "codec_name";
    private static String accessToken = null;
    private static String userLogin = null;
    private static String userName = null;
    private static Bitmap userImage = null;
    private static Long directorySize;
    private static Long directoryUploadedSize;
    private static Location lastKnownLocation = null;
    private static int rotation;
    private static IncidentFlagState incidentFlag = IncidentFlagState.NOT_FLAGGED;
    private static String currentVideoPath;
    public static int appCamcoderProfile = CamcorderProfile.QUALITY_QVGA;
    private static String imei;
    private static String simid;
    private static String codecName;
    private static UUID sessionId;
    private static StateManager stateManager;
    private static Integer userId;
    private static Boolean hasVideoPlayback;
    private static Boolean showFeedback;

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
        hasVideoPlayback = null;
        showFeedback = null;
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

    public static void setRotation(int rotation) {
        Globals.rotation = rotation;
    }

    public static Orientation getOrientation(Context context) {
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(context);
        return Orientation.valueOf(sharedPref.getString(DEVICE_ORIENTATION,
                context.getString(R.string.ORIENTATION_DEFAULT)));
    }

    public static String getServerUrl(Context context){
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(context);
        return sharedPref.getString(SERVER_URL, "");
    }

    public static String getAppRegistered(Context context){
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(context);
        return sharedPref.getString(APP_REGISTERED, "");
    }

    public static Boolean isWifiOnly(Context context){
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(context);
        return sharedPref.getBoolean(REQUIRE_WIFI_ONLY, true);
    }

    public static Boolean isAutomaticUpload(Context context){
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(context);
        return sharedPref.getBoolean(AUTOMATIC_UPLOAD, true);
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

    public static String getSimid(Context context) {
        if (simid == null) {
            SharedPreferences sharedPrefs = context.getSharedPreferences(AUTH, Context.MODE_PRIVATE);
            simid = sharedPrefs.getString(PREF_SIMID, null);
        }
        return simid;
    }

    public static void setSimid(Context context, String simid) {
        SharedPreferences sharedPrefs = context.getSharedPreferences(AUTH, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPrefs.edit();
        editor.putString(PREF_SIMID, simid);
        editor.apply();
    }

    public static String getImei(Context context) {
        if (imei == null) {
            SharedPreferences sharedPrefs = context.getSharedPreferences(AUTH, Context.MODE_PRIVATE);
            imei = sharedPrefs.getString(PREF_IMEI, null);
        }
        return imei;
    }

    public static void setImei(Context context, String imei) {
        SharedPreferences sharedPrefs = context.getSharedPreferences(AUTH, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPrefs.edit();
        editor.putString(PREF_IMEI, imei);
        editor.apply();
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

    public static void initStateManager(Context context, State currentState) {
        stateManager = new StateManager(context, currentState);
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


    public static Boolean hasVideoPlayback(Context context) {
        if (hasVideoPlayback == null) {
            SharedPreferences sharedPrefs = context.getSharedPreferences(AUTH, Context.MODE_PRIVATE);
            hasVideoPlayback = sharedPrefs.getBoolean(HAS_VIDEO_PLAYBACK, false);
        }
        return hasVideoPlayback;
    }

    public static void setHasVideoPlayback(Context context, boolean hasVideoPlayback) {
        SharedPreferences sharedPrefs = context.getSharedPreferences(AUTH, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPrefs.edit();
        editor.putBoolean(HAS_VIDEO_PLAYBACK, hasVideoPlayback);
        editor.apply();
    }

    public static void setCodecName( String codecName) {
//        SharedPreferences sharedPrefs = context.getSharedPreferences(DATA, Context.MODE_PRIVATE);
//        SharedPreferences.Editor editor = sharedPrefs.edit();
//        editor.putString(CODEC_NAME, codecName);
//        editor.apply();
        Globals.codecName = codecName;
    }

    public static String getCodecName() {
//        if (codecName == null) {
//            SharedPreferences sharedPrefs = context.getSharedPreferences(DATA, Context.MODE_PRIVATE);
//            codecName = sharedPrefs.getString(CODEC_NAME, null);
//        }
        return codecName;
    }


    public static Boolean showFeedback(Context context) {
        if (showFeedback == null) {
            SharedPreferences sharedPrefs = context.getSharedPreferences(AUTH, Context.MODE_PRIVATE);
            showFeedback = sharedPrefs.getBoolean(SHOW_FEEDBACK, false);
        }
        return showFeedback;
    }

    public static void setShowFeedback(Context context, boolean showVideosScreen) {
        SharedPreferences sharedPrefs = context.getSharedPreferences(AUTH, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPrefs.edit();
        editor.putBoolean(SHOW_FEEDBACK, showVideosScreen);
        editor.apply();
    }
}
