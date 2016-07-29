package org.igarape.copcast.utils;

import android.content.Context;
import android.location.Location;
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

    public static String[] getNonEmptyUserFolders() {
        return new File(path).list(new FilenameFilter() {
            @Override
            public boolean accept(File current, String name) {
                File dir = new File(current, name);
                if (dir.listFiles() == null || dir.listFiles().length == 0) {
                    dir.delete();
                    return false;
                }
                return dir.isDirectory();
            }
        });
    }

    public static File getAlbumStorageDir(String albumName, Context context) {

        File file;

        //internal storage todo: remove after test
        file = new File(context.getFilesDir(), albumName);
        if (!file.exists() && !file.mkdirs()) {
            Log.e(TAG, "Directory '" + albumName + "' not created");
        }

//        For testing with the external directory
//        // Get the directory for the user's public pictures directory.
//        file = new File(Environment.getExternalStoragePublicDirectory(
//                Environment.DIRECTORY_DCIM), albumName);
//        if (!file.mkdirs()) {
//            Log.e(TAG, "Directory not created");
//        }
//

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

    public static void logLocation(String a, Location b) {

    }
}
