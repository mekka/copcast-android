package org.igarape.copcast.utils;

import android.content.Context;
import android.content.Intent;
import android.media.MediaPlayer;
import android.net.Uri;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import com.alexbbb.uploadservice.ContentType;
import com.alexbbb.uploadservice.UploadRequest;
import com.alexbbb.uploadservice.UploadService;

import org.igarape.copcast.db.JsonDataType;
import org.igarape.copcast.views.MainActivity;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FilenameFilter;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;
import java.util.UUID;

import static org.igarape.copcast.utils.Globals.getDirectoryUploadedSize;

/**
 * Created by brunosiqueira on 14/08/15.
 */
public class UploadManager {
    public static final String UPLOAD_PROGRESS_ACTION = "org.igarape.copcast.UPLOAD_PROGRESS";
    public static final String CANCEL_UPLOAD_ACTION = "org.igarape.copcast.CANCEL_UPLOAD";
    public static final String COMPLETED_UPLOAD_ACTION = "org.igarape.copcast.COMPLETED_UPLOAD";
    public static final String UPLOAD_FAILED_ACTION = "org.igarape.copcast.UPLOAD_FAILED_ACTION";
    private static final String TAG = UploadManager.class.getName();

    private final LocalBroadcastManager broadcaster;
    private List<String> users = null;
    private final GenericExtFilter filter = new GenericExtFilter(".mp4");
    private ArrayList<File> videos;
    private static final SimpleDateFormat df;

    static{
        TimeZone tz = TimeZone.getTimeZone("UTC");
        df = new SimpleDateFormat(FileUtils.DATE_FORMAT);
        df.setTimeZone(tz);
    }
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

    public void runUpload() {
        if ((users == null || users.isEmpty()) && (videos == null || videos.isEmpty())){
            sendCompletedToUI(broadcaster);
            return;
        }
        if (userLogin == null || videos == null || videos.isEmpty()){
            if (userLogin!= null){
                userPath = FileUtils.getPath(userLogin);
                File dir = new File(userPath);
                File[] files = dir.listFiles();
                if (files != null && files.length == 0){
                    dir.delete();
                }
            }
            userLogin = users.remove(0);
            uploadHistories(userLogin);
            uploadLocations(userLogin);
            uploadIncidents(userLogin);
            uploadBattery(userLogin);
            userPath = FileUtils.getPath(userLogin);
            SqliteUtils.clearByType(context, userLogin, JsonDataType.TYPE_FLAGGED_VIDEO);

            File dir = new File(userPath);
            File[] files = dir.listFiles(filter);

            if (files != null && files.length > 0) {
                videos = new ArrayList<File>(Arrays.asList(files));
            }
        }

        nextVideo = null;
        if (videos != null && !videos.isEmpty()){
            nextVideo = videos.remove(0);
            uploadVideo(nextVideo);
        } else {
            runUpload();
        }

    }

    private void uploadBattery(String userLogin) {
        final File file = new File(FileUtils.getBatteriesFilePath(userLogin));
        if (!file.exists()) {
            return;
        }
        Log.d(TAG, "Battery file size: " + file.length());
        FileInputStream is = null;
        try {
            is = new FileInputStream(file);

            BufferedReader br = new BufferedReader(new InputStreamReader(is, Charset.forName("UTF-8")));
            JSONArray batteries = new JSONArray();
            String line;

            while ((line = br.readLine()) != null) {
                JSONObject json = new JSONObject(line);
                if (json.getString("batteryPercentage").length() > 0) {
                    batteries.put(json);
                }
            }

            NetworkUtils.post(context, "/batteries/" + userLogin, batteries, new HttpResponseCallback() {
                @Override
                public void unauthorized() {
                    Log.e(TAG, "batteries unauthorized");
                }

                @Override
                public void failure(int statusCode) {
                    Log.e(TAG, "batteries failure - statusCode: " + statusCode);
                }

                @Override
                public void success(JSONObject response) {
                    file.delete();
                }

                @Override
                public void noConnection() {
                    Log.e(TAG, "batteries noConnection");
                }

                @Override
                public void forbidden() {
                    Log.e(TAG, "batteries noConnection");
                }

                @Override
                public void badConnection() {
                    Log.e(TAG, "batteries badConnection");
                }

                @Override
                public void badRequest() {
                    Log.e(TAG, "batteries badRequest");
                }

                @Override
                public void badResponse() {
                    Log.e(TAG, "batteries badResponse");
                }
            });
        } catch (java.io.IOException e) {
            Log.e(TAG, "location file error", e);
        } catch (JSONException e) {
            Log.e(TAG, "location file error", e);
        }
    }

    private void uploadVideo(File nextVideo) {
        final UploadRequest request = new UploadRequest(context, UUID.randomUUID().toString(), Globals.getServerUrl(context)+"/videos/"+userLogin);

        request.addHeader("Authorization", Globals.getAccessToken(context));

        long lastModified = nextVideo.lastModified();

        // add param Date
        MediaPlayer mp = MediaPlayer.create(context, Uri.parse(nextVideo.getAbsolutePath()));
        if (mp != null) {
            lastModified = lastModified - mp.getDuration();
            mp.release();
        } else {
            Log.e(TAG, "NO duration for video "+nextVideo.getName() );
        }

        request.addParameter("date", df.format(new Date(lastModified)));

        request.addFileToUpload(nextVideo.getAbsolutePath(), "video", nextVideo.getName(), ContentType.VIDEO_MPEG);


        // if you comment the following line, the system default user-agent will be used
        request.setCustomUserAgent("UploadServiceDemo/1.0");

        // set the intent to perform when the user taps on the upload notification.
        // currently tested only with intents that launches an activity
        // if you comment this line, no action will be performed when the user taps on the notification
        request.setNotificationClickIntent(new Intent(context, MainActivity.class));

        // set the maximum number of automatic upload retries on error
        request.setMaxRetries(2);

        try {
            UploadService.startUpload(request);

            //add the file to hashmap to be deleted after upload
            //totFiles.put(request.getUploadId(), fileToUploadPath);

        } catch (Exception exc) {
            Log.i(TAG, "Malformed upload request. " + exc.getLocalizedMessage());
        }
    }

    private void uploadLocations(String userLogin) {
        final File file = new File(FileUtils.getLocationsFilePath(userLogin));
        if (!file.exists()) {
            return;
        }
        FileInputStream is = null;
        try {
            is = new FileInputStream(file);

            BufferedReader br = new BufferedReader(new InputStreamReader(is, Charset.forName("UTF-8")));
            JSONArray locations = new JSONArray();
            String line;

            while ((line = br.readLine()) != null) {
                JSONObject json = new JSONObject(line);
                if (json.getString("lat") != null && json.getString("lat").length() > 0) {
                    locations.put(json);
                }
            }

            NetworkUtils.post(context, "/locations/" + userLogin, locations, new HttpResponseCallback() {
                @Override
                public void unauthorized() {
                    Log.e(TAG, "locations unauthorized");
                }

                @Override
                public void failure(int statusCode) {
                    Log.e(TAG, "locations failure - statusCode: " + statusCode);
                }

                @Override
                public void success(JSONObject response) {
                    file.delete();
                }

                @Override
                public void noConnection() {
                    Log.e(TAG, "locations noConnection");
                }

                @Override
                public void forbidden() {
                    Log.e(TAG, "locations noConnection");
                }

                @Override
                public void badConnection() {
                    Log.e(TAG, "locations badConnection");
                }

                @Override
                public void badRequest() {
                    Log.e(TAG, "locations badRequest");
                }

                @Override
                public void badResponse() {
                    Log.e(TAG, "locations badResponse");
                }
            });
        } catch (java.io.IOException e) {
            Log.e(TAG, "location file error", e);
        } catch (JSONException e) {
            Log.e(TAG, "location file error", e);
        }
    }

    private void uploadHistories(String userLogin) {
        final File file = new File(FileUtils.getHistoriesFilePath(userLogin));
        if (!file.exists()) {
            return;
        }
        Log.d(TAG, "History file size: " + file.length());
        FileInputStream is = null;
        try {
            is = new FileInputStream(file);

            BufferedReader br = new BufferedReader(new InputStreamReader(is, Charset.forName("UTF-8")));
            JSONArray histories = new JSONArray();
            String line;

            while ((line = br.readLine()) != null) {
                JSONObject json = new JSONObject(line);
                if (json.getString("previousState").length() > 0) {
                    histories.put(json);
                }
            }

            NetworkUtils.post(context, "/histories/" + userLogin, histories, new HttpResponseCallback() {
                @Override
                public void unauthorized() {
                    Log.e(TAG, "histories unauthorized");
                }

                @Override
                public void failure(int statusCode) {
                    Log.e(TAG, "histories failure - statusCode: " + statusCode);
                }

                @Override
                public void success(JSONObject response) {
                    file.delete();
                }

                @Override
                public void noConnection() {
                    Log.e(TAG, "histories noConnection");
                }

                @Override
                public void badConnection() {
                    Log.e(TAG, "histories badConnection");
                }

                @Override
                public void forbidden() {
                    Log.e(TAG, "histories badConnection");
                }

                @Override
                public void badRequest() {
                    Log.e(TAG, "histories badRequest");
                }

                @Override
                public void badResponse() {
                    Log.e(TAG, "histories badResponse");
                }
            });
        } catch (java.io.IOException e) {
            Log.e(TAG, "location file error", e);
        } catch (JSONException e) {
            Log.e(TAG, "location file error", e);
        }
    }

    private void uploadIncidents(final String userLogin) {

        JSONArray incidents;

        try {
            incidents = SqliteUtils.getFromDb(context, userLogin, JsonDataType.TYPE_INCIDENT_FLAG);
        } catch (JSONException e) {
            Log.e(TAG, "Unable to read incidents from database");
            Log.d(TAG, e.toString());
            return;
        }

        if (incidents.length() == 0) {
            Log.d(TAG, "No failed incidents to upload");
            return;
        }

        Log.d(TAG, "# of incidents: " + incidents.length());

        NetworkUtils.post(context, "/incidents", incidents, new HttpResponseCallback() {
            @Override
            public void unauthorized() {
                Log.e(TAG, "incidents unauthorized");
            }

            @Override
            public void failure(int statusCode) {
                Log.e(TAG, "incidents failure - statusCode: " + statusCode);
            }

            @Override
            public void success(JSONObject response) {
                SqliteUtils.clearByType(context, userLogin, JsonDataType.TYPE_INCIDENT_FLAG);
            }

            @Override
            public void noConnection() {
                Log.e(TAG, "incidents noConnection");
            }

            @Override
            public void forbidden() {
                Log.e(TAG, "incidents noConnection");
            }

            @Override
            public void badConnection() {
                Log.e(TAG, "incidents badConnection");
            }

            @Override
            public void badRequest() {
                Log.e(TAG, "incidents badRequest");
            }

            @Override
            public void badResponse() {
                Log.e(TAG, "incidents badResponse");
            }
        });
    }

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
