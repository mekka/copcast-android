package org.igarape.copcast.service.upload;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.support.v4.content.LocalBroadcastManager;

import org.igarape.copcast.db.JsonDataType;
import org.igarape.copcast.state.NetworkState;
import org.igarape.copcast.state.UploadServiceEvent;
import org.igarape.copcast.utils.FileUtils;
import org.igarape.copcast.utils.GenericExtFilter;
import org.igarape.copcast.utils.Globals;
import org.igarape.copcast.utils.ILog;
import org.igarape.copcast.utils.NetworkUtils;
import org.igarape.copcast.utils.TextFileType;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.UUID;


public class UploadService extends Service {

    private static final String TAG = "AndroidUploadService";
    public static final String PARAM_FILES = "org.igarape.copcast.to_upload_files";
    public static final String PARAM_TOKEN = "org.igarape.copcast.token";
    public static final String PARAM_MAX_RETRIES = "org.igarape.copcast.maxRetries";
    public static final String UPLOAD_FEEDBACK_ACTION = "org.igarape.copcast.service.upload.feedback";
    public static long uploadedBytes = 0;

    public static void doUpload(Context context) {

        NetworkState networkState = NetworkUtils.checkUploadState(context);
        if (networkState != NetworkState.NETWORK_OK) {
            feedback(context, UploadServiceEvent.ABORTED_NO_NETWORK, null);
        }


        ArrayList<String> users = new ArrayList<>();
        ArrayList<File> videos = new ArrayList<>();
        String userPath;
        GenericExtFilter filter = new GenericExtFilter(".mp4");

        Collections.addAll(users, FileUtils.getUserFolders());

        if ((users == null || users.isEmpty())){
            feedback(context, UploadServiceEvent.NO_DATA, null);
            return;
        }

        ArrayList<FileToUpload> filesToUpload = new ArrayList<>();

        feedback(context, UploadServiceEvent.STARTED, null);

        for (String userLogin : users) {

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

            File[] videos2go = dir.listFiles(filter);

            for (File nextVideo : videos2go) {
                filesToUpload.add(new FileToUpload(Globals.getServerUrl(context) + "/videos/" + userLogin,
                        nextVideo.getAbsolutePath(), "video", nextVideo.getName(), ContentType.VIDEO_MPEG));
            }
        }

        if (filesToUpload.isEmpty()) {
            feedback(context, UploadServiceEvent.FINISHED, null);
            return;
        }

        ILog.d(TAG, "Num of files: " + filesToUpload.size());

        Intent videoUploadIntent = new Intent(context, UploadService.class);
        Bundle b = new Bundle();
        b.putParcelableArrayList(UploadService.PARAM_FILES, filesToUpload);
        b.putString(UploadService.PARAM_TOKEN, Globals.getAccessToken(context));
        b.putInt(UploadService.PARAM_MAX_RETRIES, 2);

        videoUploadIntent.putExtras(b);

        ILog.d(TAG, "calling service...");
        videoUploadIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startService(videoUploadIntent);
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        UploadRequest request;

        ILog.d(TAG, "UPLOAD Service STARTED!");

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

        //    broadcast(getApplicationContext(), uploadState, null);

        Bundle b = intent.getExtras();

        final ArrayList<FileToUpload> files = b.getParcelableArrayList(PARAM_FILES);
        if (files != null)
            ILog.e(TAG, ">>"+files.size());
        else
            ILog.e(TAG, "Empty files");

        request = new UploadRequest(getApplicationContext(), UUID.randomUUID().toString());
        request.setMaxRetries(intent.getIntExtra(PARAM_MAX_RETRIES, 0));
        request.setMethod("POST");
        request.addHeader("Authorization", intent.getStringExtra(PARAM_TOKEN));
        request.setFilesToUpload(files);

        ILog.d(TAG, request.toString());

        new RunUpload(request).execute();

        return START_NOT_STICKY;
    }

    class RunUpload extends AsyncTask<Void, Void, Void> {

        UploadRequest uploadRequest;
        Context context;

        public RunUpload(UploadRequest request) {
            super();
            this.uploadRequest = request;
        }
        @Override
        protected Void doInBackground(Void... flags) {
            ArrayList<String> copied = new ArrayList<>();
            int num_files = this.uploadRequest.getFilesToUpload().size();

            uploadedBytes = 0;

            for (int i=0; i<num_files; i++) {
                FileToUpload fileToUpload = this.uploadRequest.getFilesToUpload().get(i);
                String returnmsg = "File failed: ";
                if (VideoUploader.uploadSingleFile(this.uploadRequest.getMethod(), fileToUpload, this.uploadRequest.getHeaders(), (num_files == i + 1), this.uploadRequest.getCustomUserAgent())) {
                    uploadedBytes += fileToUpload.getSize();
                    feedback(context, UploadServiceEvent.RUNNING, uploadedBytes);

                    fileToUpload.length();
                    returnmsg = "File uploaded: ";
                }
                ILog.d(TAG, returnmsg+fileToUpload.getFileName());
            }
            ILog.d(TAG, "Files uploaded: "+copied.size());
            feedback(UploadService.this, UploadServiceEvent.FINISHED, null);
            stopSelf();
            return null;
        }
    }


    private static void feedback(Context context, final UploadServiceEvent event, final Long uploadedBytes) {

        ILog.d(TAG, context+"");

        Intent intent = new Intent(UPLOAD_FEEDBACK_ACTION);
        intent.putExtra("event", event);

        if (event.getRunning() && uploadedBytes != null)
            intent.putExtra("uploadedBytes", uploadedBytes);

        LocalBroadcastManager b = LocalBroadcastManager.getInstance(context);
        b.sendBroadcast(intent);
    }

    public static void stop(Context context) {
        Intent videoUploadIntent = new Intent(context, UploadService.class);
        context.stopService(videoUploadIntent);
    }
}
