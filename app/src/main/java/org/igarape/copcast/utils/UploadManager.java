package org.igarape.copcast.utils;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import org.igarape.copcast.db.JsonDataType;
import org.igarape.copcast.service.upload.ContentType;
import org.igarape.copcast.service.upload.FileToUpload;
import org.igarape.copcast.service.upload.SqliteUploader;
import org.igarape.copcast.service.upload.TextFileUploader;
import org.igarape.copcast.service.upload.UploadService;

import java.io.File;
import java.io.FilenameFilter;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.igarape.copcast.utils.Globals.getDirectoryUploadedSize;

/**
 * Created by brunosiqueira on 14/08/15.
 */
public class UploadManager {
    public static final String UPLOAD_PROGRESS_ACTION = "org.igarape.copcast.UPLOAD_PROGRESS";
    public static final String CANCEL_UPLOAD_ACTION = "org.igarape.copcast.CANCEL_UPLOAD";
    public static final String COMPLETED_UPLOAD_ACTION = "org.igarape.copcast.COMPLETED_UPLOAD";
    public static final String UPLOAD_FAILED_ACTION = "org.igarape.copcast.UPLOAD_FAILED_ACTION";
    public static final String UPLOAD_ABORTED = "org.igarape.copcast.UPLOAD_ABORTED";

    private static final String TAG = UploadManager.class.getName();

    private final LocalBroadcastManager broadcaster;
    private List<String> users = null;
    private final GenericExtFilter filter = new GenericExtFilter(".mp4");
    private ArrayList<File> videos;
    private DateFormat df = new SimpleDateFormat(FileUtils.DATE_FORMAT);

    private Context context;
    private String userLogin;
    private String userPath;
    private File nextVideo;

    public UploadManager(Context applicationContext) {
        this.context = applicationContext;

        broadcaster = LocalBroadcastManager.getInstance(context);

        users = new ArrayList<String>();

        Collections.addAll(users, FileUtils.getUserFolders());
    }

    public void runUpload(Context context) {

        if ((users == null || users.isEmpty()) && (videos == null || videos.isEmpty())){
            sendCompletedToUI(broadcaster);
            return;
        }

        ArrayList<FileToUpload> filesToUpload = new ArrayList<>();

        for(String userLogin : users) {

            userPath = FileUtils.getPath(userLogin);
            File dir = new File(userPath);
            File[] files = dir.listFiles();
            if (files != null && files.length == 0) {
                dir.delete();
            }

            TextFileUploader.upload(context, TextFileType.HISTORY, userLogin);
            TextFileUploader.upload(context, TextFileType.LOCATIONS, userLogin);
            TextFileUploader.upload(context, TextFileType.BATTERY, userLogin);
            SqliteUploader.upload(context, JsonDataType.TYPE_FLAGGED_VIDEO, userLogin);
            SqliteUtils.clearByType(context, userLogin, JsonDataType.TYPE_FLAGGED_VIDEO.getType());

            File[] videos = dir.listFiles(filter);

            for(File nextVideo: videos) {
                filesToUpload.add(new FileToUpload(Globals.getServerUrl(context) + "/videos/" + userLogin,
                        nextVideo.getAbsolutePath(), "video", nextVideo.getName(), ContentType.VIDEO_MPEG));
            }
        }

        Log.d(TAG, "Num of files: " + filesToUpload.size());

        if (1+1 == 2)
            return;

        Intent videoUploadIntent = new Intent(context, UploadService.class);
        Bundle b = new Bundle();
        b.putParcelableArrayList(UploadService.PARAM_FILES, filesToUpload);
        b.putString(UploadService.PARAM_TOKEN, Globals.getAccessToken(context));
        b.putInt(UploadService.PARAM_MAX_RETRIES, 2);

        videoUploadIntent.putExtras(b);

        Log.d(TAG, "calling service...");
        videoUploadIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startService(videoUploadIntent);

    }

//    private void uploadBattery(String userLogin) {
//        final File file = new File(FileUtils.getBatteriesFilePath(userLogin));
//        if (!file.exists()) {
//            return;
//        }
//        Log.d(TAG, "Battery file size: " + file.length());
//        FileInputStream is = null;
//        try {
//            is = new FileInputStream(file);
//
//            BufferedReader br = new BufferedReader(new InputStreamReader(is, Charset.forName("UTF-8")));
//            JSONArray batteries = new JSONArray();
//            String line;
//
//            while ((line = br.readLine()) != null) {
//                JSONObject json = new JSONObject(line);
//                if (json.getString("batteryPercentage").length() > 0) {
//                    batteries.put(json);
//                }
//            }
//
//            NetworkUtils.post(context, "/batteries/" + userLogin, batteries, new HttpResponseCallback() {
//                @Override
//                public void unauthorized() {
//                    Log.e(TAG, "batteries unauthorized");
//                }
//
//                @Override
//                public void failure(int statusCode) {
//                    Log.e(TAG, "batteries failure - statusCode: " + statusCode);
//                }
//
//                @Override
//                public void success(JSONObject response) {
//                    file.delete();
//                }
//
//                @Override
//                public void noConnection() {
//                    Log.e(TAG, "batteries noConnection");
//                }
//
//                @Override
//                public void badConnection() {
//                    Log.e(TAG, "batteries badConnection");
//                }
//
//                @Override
//                public void badRequest() {
//                    Log.e(TAG, "batteries badRequest");
//                }
//
//                @Override
//                public void badResponse() {
//                    Log.e(TAG, "batteries badResponse");
//                }
//            });
//        } catch (java.io.IOException e) {
//            Log.e(TAG, "location file error", e);
//        } catch (JSONException e) {
//            Log.e(TAG, "location file error", e);
//        }
//    }
//
//    private void uploadVideos(ArrayList<File> videos) {
//
//        try {
////            UploadService.startUpload(request);
//
//            //add the file to hashmap to be deleted after upload
//            //totFiles.put(request.getUploadId(), fileToUploadPath);
//
//        } catch (Exception exc) {
//            Log.i(TAG, "Malformed upload request. " + exc.getLocalizedMessage());
//        }
//    }
//
//    private void uploadLocations(String userLogin) {
//        final File file = new File(FileUtils.getLocationsFilePath(userLogin));
//        if (!file.exists()) {
//            return;
//        }
//        FileInputStream is = null;
//        try {
//            is = new FileInputStream(file);
//
//            BufferedReader br = new BufferedReader(new InputStreamReader(is, Charset.forName("UTF-8")));
//            JSONArray locations = new JSONArray();
//            String line;
//
//            while ((line = br.readLine()) != null) {
//                JSONObject json = new JSONObject(line);
//                if (json.getString("lat") != null && json.getString("lat").length() > 0) {
//                    locations.put(json);
//                }
//            }
//
//            NetworkUtils.post(context, "/locations/" + userLogin, locations, new HttpResponseCallback() {
//                @Override
//                public void unauthorized() {
//                    Log.e(TAG, "locations unauthorized");
//                }
//
//                @Override
//                public void failure(int statusCode) {
//                    Log.e(TAG, "locations failure - statusCode: " + statusCode);
//                }
//
//                @Override
//                public void success(JSONObject response) {
//                    file.delete();
//                }
//
//                @Override
//                public void noConnection() {
//                    Log.e(TAG, "locations noConnection");
//                }
//
//                @Override
//                public void badConnection() {
//                    Log.e(TAG, "locations badConnection");
//                }
//
//                @Override
//                public void badRequest() {
//                    Log.e(TAG, "locations badRequest");
//                }
//
//                @Override
//                public void badResponse() {
//                    Log.e(TAG, "locations badResponse");
//                }
//            });
//        } catch (java.io.IOException e) {
//            Log.e(TAG, "location file error", e);
//        } catch (JSONException e) {
//            Log.e(TAG, "location file error", e);
//        }
//    }
//
//    private void uploadHistories(String userLogin) {
//        final File file = new File(FileUtils.getHistoriesFilePath(userLogin));
//        if (!file.exists()) {
//            return;
//        }
//        Log.d(TAG, "History file size: " + file.length());
//        FileInputStream is = null;
//        try {
//            is = new FileInputStream(file);
//
//            BufferedReader br = new BufferedReader(new InputStreamReader(is, Charset.forName("UTF-8")));
//            JSONArray histories = new JSONArray();
//            String line;
//
//            while ((line = br.readLine()) != null) {
//                JSONObject json = new JSONObject(line);
//                if (json.getString("previousState").length() > 0) {
//                    histories.put(json);
//                }
//            }
//
//            NetworkUtils.post(context, "/histories/" + userLogin, histories, new HttpResponseCallback() {
//                @Override
//                public void unauthorized() {
//                    Log.e(TAG, "histories unauthorized");
//                }
//
//                @Override
//                public void failure(int statusCode) {
//                    Log.e(TAG, "histories failure - statusCode: " + statusCode);
//                }
//
//                @Override
//                public void success(JSONObject response) {
//                    file.delete();
//                }
//
//                @Override
//                public void noConnection() {
//                    Log.e(TAG, "histories noConnection");
//                }
//
//                @Override
//                public void badConnection() {
//                    Log.e(TAG, "histories badConnection");
//                }
//
//                @Override
//                public void badRequest() {
//                    Log.e(TAG, "histories badRequest");
//                }
//
//                @Override
//                public void badResponse() {
//                    Log.e(TAG, "histories badResponse");
//                }
//            });
//        } catch (java.io.IOException e) {
//            Log.e(TAG, "location file error", e);
//        } catch (JSONException e) {
//            Log.e(TAG, "location file error", e);
//        }
//    }
//
//    private void uploadIncidents(final String userLogin) {
//
//        JSONArray incidents;
//
//        try {
//            incidents = SqliteUtils.getFromDb(context, userLogin, JsonDataType.TYPE_INCIDENT_FLAG);
//        } catch (JSONException e) {
//            Log.e(TAG, "Unable to read incidents from database");
//            Log.d(TAG, e.toString());
//            return;
//        }
//
//        if (incidents.length() == 0) {
//            Log.d(TAG, "No failed incidents to upload");
//            return;
//        }
//
//        Log.d(TAG, "# of incidents: " + incidents.length());
//
//        NetworkUtils.post(context, "/incidents", incidents, new HttpResponseCallback() {
//            @Override
//            public void unauthorized() {
//                Log.e(TAG, "incidents unauthorized");
//            }
//
//            @Override
//            public void failure(int statusCode) {
//                Log.e(TAG, "incidents failure - statusCode: " + statusCode);
//            }
//
//            @Override
//            public void success(JSONObject response) {
//                SqliteUtils.clearByType(context, userLogin, JsonDataType.TYPE_INCIDENT_FLAG);
//            }
//
//            @Override
//            public void noConnection() {
//                Log.e(TAG, "incidents noConnection");
//            }
//
//            @Override
//            public void badConnection() {
//                Log.e(TAG, "incidents badConnection");
//            }
//
//            @Override
//            public void badRequest() {
//                Log.e(TAG, "incidents badRequest");
//            }
//
//            @Override
//            public void badResponse() {
//                Log.e(TAG, "incidents badResponse");
//            }
//        });
//    }

    public static void sendCancelToUI(LocalBroadcastManager broadcaster) {
        Intent intent = new Intent(CANCEL_UPLOAD_ACTION);

        broadcaster.sendBroadcast(intent);
    }

    public static void sendCompletedToUI(LocalBroadcastManager broadcaster) {
        Intent intent = new Intent(COMPLETED_UPLOAD_ACTION);

        broadcaster.sendBroadcast(intent);
    }

    public static void sendUpdateToUI(Context context, LocalBroadcastManager broadcaster, Long size) {
        Intent intent = new Intent(UPLOAD_PROGRESS_ACTION);
        if (size != null) {
            Globals.setDirectoryUploadedSize(context, getDirectoryUploadedSize(context) + size);
            broadcaster.sendBroadcast(intent);
        }
    }

    public static void sendFailedToUI(LocalBroadcastManager broadcaster) {
        Intent intent = new Intent(UPLOAD_FAILED_ACTION);
        broadcaster.sendBroadcast(intent);
    }

    public void deleteVideoFile() {
        if (nextVideo != null){
            nextVideo.delete();
            Log.d(TAG, "next video deleted after upload");
        }
    }

class GenericExtFilter implements FilenameFilter {

    private String ext;

    public GenericExtFilter(String ext) {
        this.ext = ext;
    }

    public boolean accept(File dir, String name) {
        return (name.endsWith(ext));
    }
}
}
