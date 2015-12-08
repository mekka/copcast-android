package org.igarape.copcast.service.upload;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import org.igarape.copcast.state.DeviceUploadStatus;
import org.igarape.copcast.utils.NetworkUtils;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.UUID;


public class UploadService extends Service {

    private static final String TAG = "AndroidUploadService";
    private UploadRequest request;


    public static final String PARAM_URL = "org.igarape.copcast.url";
    public static final String PARAM_FILES = "org.igarape.copcast.to_upload_files";
    public static final String PARAM_TOKEN = "org.igarape.copcast.token";
    public static final String PARAM_MAX_RETRIES = "org.igarape.copcast.maxRetries";
    private static final String BROADCAST_ACTION_SUFFIX = ".uploadservice.broadcast.status";

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        Log.d(TAG, "UPLOAD Service STARTED!");

        DeviceUploadStatus uploadState = NetworkUtils.checkUploadState(getApplicationContext());
        if (uploadState != DeviceUploadStatus.UPLOAD_OK) {
//            String message = getString(R.string.upload_denied) + ". ";
//            switch (uploadState) {
//                case NOT_CHARGING:
//                    message += getString(R.string.upload_state_not_charging);
//                    break;
//                case NO_NETWORK:
//                    message += getString(R.string.upload_state_no_network);
//                    break;
//                case WIFI_REQUIRED:
//                    message += getString(R.string.upload_state_wifi_required);
//                    break;
//            }
//            message += ".";

            broadcast(getApplicationContext(), uploadState, null);
            this.stopSelf();
            return START_NOT_STICKY;
        }

        Bundle b = intent.getExtras();

        final ArrayList<FileToUpload> files = b.getParcelableArrayList(PARAM_FILES);
        if (files != null)
            Log.e(TAG, ">>"+files.size());
        else
            Log.e(TAG, "Empty files");

        request = new UploadRequest(getApplicationContext(), UUID.randomUUID().toString());
        request.setMaxRetries(intent.getIntExtra(PARAM_MAX_RETRIES, 0));
        request.setMethod("POST");
        request.addHeader("Authorization", intent.getStringExtra(PARAM_TOKEN));
        request.setFilesToUpload(files);

        if (request.getFilesToUpload() == null) {
            Log.w(TAG, "no uploads. exiting.");
            stopSelf();
            return START_NOT_STICKY;
        }

        Log.d(TAG, request.toString());

        new runUpload().execute();

        return START_NOT_STICKY;
    }

    class runUpload extends AsyncTask<Void, Void, Void> {

        @Override
        protected Void doInBackground(Void... flags) {
            ArrayList<String> copied = new ArrayList<>();
            int num_files = request.getFilesToUpload().size();

            for (int i=0; i<num_files; i++) {
                FileToUpload fileToUpload = request.getFilesToUpload().get(i);
                String returnmsg = "File failed: ";
                if (VideoUploader.uploadSingleFile(request.getMethod(), fileToUpload, request.getHeaders(), (num_files==i+1), request.getCustomUserAgent())) {
                    copied.add(fileToUpload.getFileName());
                    returnmsg = "File uploaded: ";
                }
                Log.d(TAG, returnmsg+fileToUpload.getFileName());
            }
            Log.d(TAG, "Files uploaded: "+copied.size());
            stopSelf();
            return null;
        }
    }

    public static void broadcast(Context context, final DeviceUploadStatus status, final Integer percentCompleted) {
        Intent intent = new UploadStatusIntent();

        intent.putExtra("status", status);

        if (status == DeviceUploadStatus.UPLOAD_RUNNING && percentCompleted != null)
            intent.putExtra("percentCompleted", percentCompleted);

        LocalBroadcastManager.getInstance(context).sendBroadcast(intent);
    }
}
