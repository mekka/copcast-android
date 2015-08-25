package org.igarape.copcast.utils;

import android.content.Context;
import android.location.Location;
import android.os.Environment;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.FilenameFilter;
import java.io.IOException;
import java.text.DecimalFormat;

/**
 * Created by bruno on 11/3/14.
 */
public class FileUtils {
    public static final String LOCATIONS_TXT = "locations.txt";
    public static final String HISTORY_TXT = "history.txt";
    public static final String DATE_FORMAT = "yyyy-MM-dd'T'HH:mm:ss.SSSZ";
    private static final String TAG = FileUtils.class.getName();

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

    public static void logLocation(String userLogin, Location location) {
        try {
            LogToFile(userLogin, LOCATIONS_TXT, LocationUtils.buildJson(location).toString());
        } catch (JSONException e) {
            Log.e(TAG, "error recording location in file", e);

        }
    }

    public static void LogHistory(String userLogin, JSONObject history) {
        LogToFile(userLogin, HISTORY_TXT, history.toString());
    }

    public static String getHistoriesFilePath(String userLogin) {
        String userPath = getUserPath(userLogin);

        return userPath + HISTORY_TXT;
    }

    public static String getLocationsFilePath(String userLogin) {
        return getUserPath(userLogin) + LOCATIONS_TXT;
    }

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

    private static String getUserPath(String userLogin) {
        String userPath = path + userLogin + File.separator;
        //String userPath = path;


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

        //internal storage todo: remove after test
        /*File file = new File(context.getFilesDir(), albumName);
        if (!file.exists() && !file.mkdirs()) {
            Log.e(TAG, "Directory '" + albumName + "' not created");
        }
        */
        // Get the directory for the user's public pictures directory.
        File file = new File(Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_DCIM), albumName);
        if (!file.mkdirs()) {
            Log.e(TAG, "Directory not created");
        }

        return file;
    }

    public static void init(Context context) {
        setPath(getAlbumStorageDir("smartpolicing", context).getAbsolutePath());
    }

    public static long getDirectorySize() {
        return org.apache.commons.io.FileUtils.sizeOfDirectory(new File(path));
    }

    public static String formatMegaBytes(Long size) {
        return new DecimalFormat("##.##").format((float) size / 1000000);
    }
}
