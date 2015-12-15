package org.igarape.copcast.utils;

import android.content.Context;
import android.os.Environment;
import android.util.Log;

import org.json.JSONObject;

import java.io.File;
import java.io.FileWriter;
import java.io.FilenameFilter;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by bruno on 11/3/14.
 */
public class FileUtils {
    public static final String LOCATIONS_TXT = "locations.txt";
    public static final String HISTORY_TXT = "history.txt";
    public static final String INCIDENTS_TXT = "incidents.txt";
    public static final String DATE_FORMAT = "yyyy-MM-dd'T'HH:mm:ss.SSSZ";
    private static final String TAG = FileUtils.class.getName();
    private static final String BATTERY_TXT = "battery.txt";

    private static String path = null;


    public static void setPath(String path) {
        if (!path.endsWith(File.separator)) {
            path = path + File.separator;
        }
        File f = new File(path);
        if (!f.exists()) {
            f.mkdirs();
        }

        FileUtils.path = path;
    }

//    public static void logLocation(String userLogin, Location location) {
//        try {
//            logLocation(userLogin, LocationUtils.buildJson(location));
//        } catch (JSONException e) {
//            Log.e(TAG, "error recording location in file", e);
//
//        }
//    }

//    public static void LogIncident(String userLogin, JSONObject incident) {
//        LogToFile(userLogin, INCIDENTS_TXT, incident.toString());
//    }
//
//    public static void logLocation(String userLogin, JSONObject locationJson) {
//            LogToFile(userLogin, LOCATIONS_TXT, locationJson.toString());
//    }
//
//    public static void LogHistory(String userLogin, JSONObject history) {
//        LogToFile(userLogin, HISTORY_TXT, history.toString());
//    }
//
//    public static String getHistoriesFilePath(String userLogin) {
//        String userPath = getUserPath(userLogin);
//
//        return userPath + HISTORY_TXT;
//    }
//
//    public static String getLocationsFilePath(String userLogin) {
//        return getUserPath(userLogin) + LOCATIONS_TXT;
//    }
//
//    public static String getIncidentsFilePath(String userLogin) {
//        return getUserPath(userLogin) + INCIDENTS_TXT;
//    }

    private static String getUserPath(String userLogin) {
        String userPath = path + userLogin + File.separator;

        File f = new File(userPath);
        if (!f.exists()) {
            f.mkdirs();
        }

        return userPath;
    }

    public static String getPath(String userLogin) {
        return getUserPath(userLogin);
    }

    public static String[] getUserFolders() {
        return new File(path).list(new FilenameFilter() {
            @Override
            public boolean accept(File current, String name) {
                return new File(current, name).isDirectory();
            }
        });
    }

    public static File getAlbumStorageDir(String albumName, Context context) {

        boolean testVisible = false;
        File file = null;

        if (!testVisible) {
            //internal storage todo: remove after test
            file = new File(context.getFilesDir(), albumName);
            if (!file.exists() && !file.mkdirs()) {
                Log.e(TAG, "Directory '" + albumName + "' not created");
            }
        }
        else {
            // Get the directory for the user's public pictures directory.
            file = new File(Environment.getExternalStoragePublicDirectory(
                    Environment.DIRECTORY_DCIM), albumName);
            if (!file.mkdirs()) {
                Log.e(TAG, "Directory not created");
            }
        }

        return file;
    }

    public static void init(Context context) {
        setPath(getAlbumStorageDir("smartpolicing", context).getAbsolutePath());
    }

    public static long getDirectorySize() {
        Log.d(TAG, " file path name " + path);
        File directory = new File(path);
        if (directory.exists()){
            return org.apache.commons.io.FileUtils.sizeOfDirectory(directory);
        } else {
            return (long) 0;
        }
    }

    public static String formatMegaBytes(Long size) {
        return new DecimalFormat("#0.00").format((float) size / 1024);
    }

    public static List<String> getVideoPathList(String user) {
        ArrayList<String> videoList = new ArrayList<>();

        final String basedir = FileUtils.getPath(user);

        File folder = new File(basedir);
        File[] listOfFiles = folder.listFiles();

        for (int i = 0; i < listOfFiles.length; i++) {
            if (listOfFiles[i].isFile() && listOfFiles[i].getAbsolutePath().endsWith(".mp4")) {
                videoList.add(listOfFiles[i].getAbsolutePath());
            }
        }
        return videoList;
    }

//    public static void logBattery(String login, JSONObject batteryJson) {
//        LogToFile(login, BATTERY_TXT, batteryJson.toString());
//    }
//
//    public static String getBatteriesFilePath(String userLogin) {
//        return getUserPath(userLogin) + BATTERY_TXT;
//    }





    private static void LogToFile(String userLogin, String file, String data) {
        String userPath = getUserPath(userLogin);
        try {
            FileWriter writer = new FileWriter(userPath + file, true);
            writer.write(data + "\n");
            writer.close();
        } catch (IOException e) {
            Log.e(TAG, e.getMessage());
        }
    }

    public static void logTextFile(TextFileType textFileType, String login, JSONObject dataJson) {
        LogToFile(login, textFileType.getName(), dataJson.toString());
    }


    public static String getTextFilePath(TextFileType text, String userLogin) {
        return getUserPath(userLogin) + text.getName();
    }
}
