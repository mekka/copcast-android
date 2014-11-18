package org.igarape.copcast.service;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.TaskStackBuilder;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;
import org.igarape.copcast.R;
import org.igarape.copcast.utils.FileUtils;
import org.igarape.copcast.utils.HttpResponseCallback;
import org.igarape.copcast.utils.NetworkUtils;
import org.igarape.copcast.utils.ServiceUtils;
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
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;

/**
 * Created by bruno on 11/14/14.
 */
public class UploadService extends Service {
    private static final String TAG = UploadService.class.getName();
    public static final String UPLOAD_PROGRESS_ACTION = "org.igarape.copcast.UPLOAD_PROGRESS";
    public static final String FILE_SIZE = "FILE_SIZE";
    public static final String CANCEL_UPLOAD_ACTION =  "org.igarape.copcast.CANCEL_UPLOAD";
    public static final String COMPLETED_UPLOAD_ACTION = "org.igarape.copcast.COMPLETED_UPLOAD";
    private int mId = 3;
    private List<String> users;
    private final GenericExtFilter filter = new GenericExtFilter(".mp4");
    private ArrayList<File> videos;
    private DateFormat df = new SimpleDateFormat(FileUtils.DATE_FORMAT);
    private LocalBroadcastManager broadcaster;
    private Intent intent;

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        this.intent = intent;
        final Intent resultIntent = new Intent(this, MainActivity.class);
        final Context context = getApplicationContext();
        NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(context)
                .setContentTitle(getString(R.string.notification_upload_title))
                .setContentText(getString(R.string.notification_upload_description))
                .setSmallIcon(R.drawable.ic_launcher);

        TaskStackBuilder stackBuilder = TaskStackBuilder.create(context);
        stackBuilder.addParentStack(MainActivity.class);
        stackBuilder.addNextIntent(resultIntent);

        PendingIntent resultPendingIntent =
                stackBuilder.getPendingIntent(
                        0,
                        PendingIntent.FLAG_NO_CREATE
                );
        mBuilder.setContentIntent(resultPendingIntent);
        NotificationManager mNotificationManager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        // mId allows you to update the notification later on.
        mNotificationManager.notify(mId, mBuilder.build());

        users = new ArrayList<String>();
        Collections.addAll(users, FileUtils.getUserFolders());

        broadcaster = LocalBroadcastManager.getInstance(this);

        if (!users.isEmpty()){
            uploadUserData();
        }

        return super.onStartCommand(intent, flags, startId);
    }

    public void sendUpdateToUI(Long size) {
        Intent intent = new Intent(UPLOAD_PROGRESS_ACTION);
        if(size != null)
            intent.putExtra(FILE_SIZE, size);
        broadcaster.sendBroadcast(intent);
    }
    
    private void uploadUserData() {
        if (!ServiceUtils.isMyServiceRunning(UploadService.class, getApplicationContext())){
            return;
        }
        if (!NetworkUtils.canUpload(getApplicationContext(), this.intent)) {
            sendCancelToUI();
            this.stopSelf();
            return;
        }
        if (users.isEmpty()){
            sendCompletedToUI();
            this.stopSelf();
            return;
        }
        String userLogin = users.remove(0);

        String path = FileUtils.getPath(userLogin);

        uploadLocations(userLogin);
        uploadHistories(userLogin);

        File dir = new File(path);
        File[] files = dir.listFiles(filter);
        if (files != null && files.length > 0) {
            videos = new ArrayList<File>(Arrays.asList(files));
            if (!videos.isEmpty()) {
                File nextVideo = videos.remove(0);
                uploadVideo(nextVideo, userLogin);
            } else {
                uploadUserData();
            }
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

            NetworkUtils.post(getApplicationContext(),"/locations/"+userLogin, locations, new HttpResponseCallback() {
                @Override
                public void unauthorized() {
                    Log.e(TAG, "locations unauthorized");
                }

                @Override
                public void failure(int statusCode) {
                    Log.e(TAG, "locations failure - statusCode: "+statusCode);
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

            NetworkUtils.post(getApplicationContext(),"/histories/"+userLogin, histories, new HttpResponseCallback() {
                @Override
                public void unauthorized() {
                    Log.e(TAG, "histories unauthorized");
                }

                @Override
                public void failure(int statusCode) {
                    Log.e(TAG, "histories failure - statusCode: "+statusCode);
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

    private void uploadVideo(final File nextVideo, final String userLogin) {
        if (!ServiceUtils.isMyServiceRunning(UploadService.class, getApplicationContext())){
            return;
        }
        if (!NetworkUtils.canUpload(getApplicationContext(), this.intent)) {
            sendCancelToUI();
            this.stopSelf();
            return;
        }
        if (nextVideo.exists()) {
            List<NameValuePair> params = new ArrayList<NameValuePair>();

            params.add(new BasicNameValuePair("date", df.format(new Date(nextVideo.lastModified()))));
            NetworkUtils.post(getApplicationContext(), "/videos/" + userLogin, params, nextVideo, new HttpResponseCallback() {

                @Override
                public void unauthorized() {
                    Log.e(TAG, "unauthorized");
                }

                @Override
                public void failure(int statusCode) {
                    if (!videos.isEmpty()){
                        uploadVideo(videos.remove(0), userLogin);
                    } else {
                        uploadUserData();
                    }
                }

                @Override
                public void success(JSONObject response) {
                    sendUpdateToUI(nextVideo.length());
                    nextVideo.delete();
                    if (!videos.isEmpty()){
                        uploadVideo(videos.remove(0), userLogin);
                    } else {
                        uploadUserData();
                    }
                }

                @Override
                public void noConnection() {
                    Log.e(TAG, "noConnection");
                }

                @Override
                public void badConnection() {
                    Log.e(TAG, "badConnection");
                }

                @Override
                public void badRequest() {
                    Log.e(TAG, "badRequest");
                }

                @Override
                public void badResponse() {
                    Log.e(TAG, "badResponse");
                }
            });
        }
    }

    private void sendCancelToUI() {
        Intent intent = new Intent(CANCEL_UPLOAD_ACTION);

        broadcaster.sendBroadcast(intent);
    }

    private void sendCompletedToUI() {
        Intent intent = new Intent(COMPLETED_UPLOAD_ACTION);

        broadcaster.sendBroadcast(intent);
    }
    @Override
    public void onDestroy() {
        super.onDestroy();
        NotificationManager mNotificationManager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        mNotificationManager.cancel(mId);
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
