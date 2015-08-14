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
import org.igarape.copcast.utils.Globals;
import org.igarape.copcast.utils.HttpResponseCallback;
import org.igarape.copcast.utils.NetworkUtils;
import org.igarape.copcast.utils.ServiceUtils;
import org.igarape.copcast.utils.UploadManager;
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

import static org.igarape.copcast.utils.Globals.getDirectoryUploadedSize;

/**
 * Created by bruno on 11/14/14.
 */
public class UploadService extends Service {
    private static final String TAG = UploadService.class.getName();

    private int mId = 3;

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
        if (intent == null){
            stopSelf();
            return START_STICKY;
        }

        String userLogin = intent.getStringExtra("userLogin");
        File nextVideo = (File) intent.getExtras().get("nextVideo");

//        final Intent resultIntent = new Intent(this, MainActivity.class);
//        final Context context = getApplicationContext();
//        NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(context)
//                .setContentTitle(getString(R.string.notification_upload_title))
//                .setContentText(getString(R.string.notification_upload_description))
//                .setOngoing(true)
//                .setSmallIcon(R.drawable.ic_launcher);
//
//        TaskStackBuilder stackBuilder = TaskStackBuilder.create(context);
//        stackBuilder.addParentStack(MainActivity.class);
//        stackBuilder.addNextIntent(resultIntent);
//
//        PendingIntent resultPendingIntent =
//                stackBuilder.getPendingIntent(
//                        0,
//                        PendingIntent.FLAG_NO_CREATE
//                );
//        mBuilder.setContentIntent(resultPendingIntent);
//        NotificationManager mNotificationManager =
//                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
//        // mId allows you to update the notification later on.
//        mNotificationManager.notify(mId, mBuilder.build());

        broadcaster = LocalBroadcastManager.getInstance(this);

        uploadHistories(userLogin);
        uploadLocations(userLogin);
        uploadVideo(nextVideo, userLogin);

        return START_STICKY;
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

            NetworkUtils.post(getApplicationContext(), "/locations/" + userLogin, locations, new HttpResponseCallback() {
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

            NetworkUtils.post(getApplicationContext(), "/histories/" + userLogin, histories, new HttpResponseCallback() {
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
        if (!ServiceUtils.isMyServiceRunning(UploadService.class, getApplicationContext())) {
            return;
        }
        if (!NetworkUtils.canUpload(getApplicationContext(), this.intent)) {
            UploadManager.sendCancelToUI(broadcaster);
            this.stopSelf();
            return;
        }
        if (nextVideo.exists()) {
            List<NameValuePair> params = new ArrayList<NameValuePair>();

            params.add(new BasicNameValuePair("date", df.format(new Date(nextVideo.lastModified()))));

            Log.d(TAG, "uploadVideo - started");

            NetworkUtils.post(getApplicationContext(),true, "/videos/" + userLogin, params, nextVideo, new HttpResponseCallback() {

                @Override
                public void unauthorized() {
                    Log.e(TAG, "unauthorized");
                }

                @Override
                public void failure(int statusCode) {
                    Log.d(TAG, "uploadVideo - failur");
                    UploadManager.sendUpdateToUI(getApplicationContext(),broadcaster, (long) 0);
                }

                @Override
                public void success(JSONObject response) {
                    Log.d(TAG, "uploadVideo - success");
                    UploadManager.sendUpdateToUI(getApplicationContext(), broadcaster, nextVideo.length());
                    nextVideo.delete();

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


    @Override
    public void onDestroy() {
        super.onDestroy();
//        NotificationManager mNotificationManager =
//                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
//
//        mNotificationManager.cancel(mId);
    }



}
