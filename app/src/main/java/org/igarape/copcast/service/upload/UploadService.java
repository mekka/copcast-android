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

import org.igarape.copcast.BuildConfig;
import org.igarape.copcast.db.JsonDataType;
import org.igarape.copcast.state.NetworkState;
import org.igarape.copcast.state.UploadServiceEvent;
import org.igarape.copcast.utils.FileUtils;
import org.igarape.copcast.utils.GenericExtFilter;
import org.igarape.copcast.utils.Globals;
import org.igarape.copcast.utils.ILog;
import org.igarape.copcast.utils.NetworkUtils;
import org.igarape.copcast.utils.SqliteUtils;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.UUID;


public class UploadService extends Service {

    private static final String TAG = "AndroidUploadService";
    public static final String PARAM_TOKEN = "org.igarape.copcast.token";
    public static final String PARAM_MAX_RETRIES = "org.igarape.copcast.maxRetries";
    public static final String UPLOAD_FEEDBACK_ACTION = "org.igarape.copcast.service.upload.feedback";
    public static Long uploadedBytes = 0L;
    private RunUpload uploadTask;
    private boolean errorOccured = false;


    public static boolean doUpload(Context context) {

        NetworkState networkState = NetworkUtils.checkUploadState(context);
        if (networkState != NetworkState.NETWORK_OK) {
            feedback(context, UploadServiceEvent.ABORTED_NO_NETWORK, null, null);
            return false;
        }

        ArrayList<String> users = new ArrayList<>();
        ArrayList<String> dbUsers = SqliteUtils.getUsersInDb(context);
        Collections.addAll(users, FileUtils.getNonEmptyUserFolders());
        Collections.addAll(users, dbUsers.toArray(new String[dbUsers.size()]));

        if ((users == null || users.isEmpty()) && SqliteUtils.countEntries(context) == 0){
            return false;
        }

        Intent videoUploadIntent = new Intent(context, UploadService.class);
        Bundle b = new Bundle();
        b.putString(UploadService.PARAM_TOKEN, Globals.getAccessToken(context));
        b.putInt(UploadService.PARAM_MAX_RETRIES, 2);

        videoUploadIntent.putExtras(b);

        ILog.d(TAG, "calling service...");
        videoUploadIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startService(videoUploadIntent);
        return true;
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        Context context = getApplicationContext();
        UploadRequest request;

        ILog.d(TAG, "UPLOAD Service STARTED!");
        feedback(context, UploadServiceEvent.STARTED, null, null);

        ArrayList<String> users = new ArrayList<>();
        ArrayList<String> dbUsers = SqliteUtils.getUsersInDb(context);
        String userPath;
        GenericExtFilter filter = new GenericExtFilter(".mp4");

        Collections.addAll(users, FileUtils.getNonEmptyUserFolders());
        ILog.d(TAG, "from user folders:"+users.toString());
        ILog.d(TAG, "from sqlite:" + dbUsers.toString());
        Collections.addAll(users, dbUsers.toArray(new String[dbUsers.size()]));

        if ((users == null || users.isEmpty()) && SqliteUtils.countEntries(context) == 0){
            Log.e(TAG, "Upload service started but no data to upload!");
        }

        ArrayList<FileToUpload> filesToUpload = new ArrayList<>();

        for (String userLogin : new LinkedHashSet<>(users)) {

            SqliteUtils.dumpTypesFromUser(context, userLogin);

            userPath = FileUtils.getPath(userLogin);
            File dir = new File(userPath);
            File[] files = dir.listFiles();
            if (files != null && files.length == 0) {
                dir.delete();
            }

            ILog.d(TAG, "Uploading JSONs for user: " + userLogin);
            SqliteUploader.upload(context, JsonDataType.TYPE_HISTORY_DATA, userLogin);
            SqliteUploader.upload(context, JsonDataType.TYPE_LOCATION_INFO, userLogin);
            SqliteUploader.upload(context, JsonDataType.TYPE_BATTERY_STATUS, userLogin);
            SqliteUploader.upload(context, JsonDataType.TYPE_INCIDENT_FLAG, userLogin);
            if (BuildConfig.HAS_INCIDENT_FORM)
                SqliteUploader.upload(context, JsonDataType.TYPE_INCIDENT_FORM, userLogin);

            File[] videos2go = dir.listFiles(filter);

            if (videos2go != null)
                for (File nextVideo : videos2go) {
                    filesToUpload.add(new FileToUpload(Globals.getServerUrl(context) + "/videos/" + userLogin,
                            nextVideo.getAbsolutePath(), "video", nextVideo.getName(), ContentType.VIDEO_MPEG));
                }

            SqliteUtils.clearByType(context, userLogin, JsonDataType.TYPE_FLAGGED_VIDEO);
        }

        if (filesToUpload.isEmpty()) {
            Log.d(TAG, "upload FINISHED");
            feedback(context, UploadServiceEvent.FINISHED, null, null);
            return START_NOT_STICKY;
        }

        ILog.d(TAG, "Num of files: " + filesToUpload.size());


        if (filesToUpload != null)
            ILog.d(TAG, "# of files: "+filesToUpload.size());
        else
            ILog.d(TAG, "No files to upload");

        request = new UploadRequest(getApplicationContext(), UUID.randomUUID().toString());
        request.setMaxRetries(intent.getIntExtra(PARAM_MAX_RETRIES, 0));
        request.setMethod("POST");
        request.addHeader("Authorization", intent.getStringExtra(PARAM_TOKEN));
        request.setFilesToUpload(filesToUpload);

        ILog.d(TAG, request.toString());

        uploadTask = new RunUpload(request);
        uploadTask.execute();

        return START_NOT_STICKY;
    }

    class RunUpload extends AsyncTask<Void, Long, Void> {

        UploadRequest uploadRequest;
        Context context;

        public RunUpload(UploadRequest request) {
            super();
            this.uploadRequest = request;
        }

        @Override
        protected void onPostExecute(Void aVoid) {

            if (errorOccured) {
                Log.d(TAG, "upload FAILED");
                feedback(UploadService.this, UploadServiceEvent.FAILED, null, null);
            } else {
                Log.d(TAG, "upload FINISHED 2");
                feedback(UploadService.this, UploadServiceEvent.FINISHED, null, null);
            }

            stopSelf();

        }

        @Override
        protected void onCancelled(Void aVoid) {
            Log.d(TAG, "upload ABORTED");
            feedback(UploadService.this, UploadServiceEvent.ABORTED_USER, null, null);
            stopSelf();

        }

        @Override
        protected Void doInBackground(Void... flags) {

            long totalSize;
            uploadedBytes = 0L;
            totalSize = 0L;

            for(FileToUpload f : this.uploadRequest.getFilesToUpload())
                totalSize += f.length();
            totalSize = totalSize / 1024; // store in KB

            if (isCancelled())
                return null;

            feedback(context, UploadServiceEvent.RUNNING, uploadedBytes, totalSize);

            boolean res;

            LinkedList<FileToUpload> fileToUploadLinkedList = new LinkedList<>(this.uploadRequest.getFilesToUpload());

            // WARNING: INFINITE UPLOAD.
            // We loop through the linked list. If the upload fails for a file, it will be queued
            // again, thus retrying continuously. If the server rejects a file, it WILL NOT
            // be queued again.

            while(fileToUploadLinkedList.size() > 0) {

                if (isCancelled()) break;

                FileToUpload fileToUpload = fileToUploadLinkedList.pollLast();
                try {
                    res = VideoUploader.uploadSingleFile(this.uploadRequest.getMethod(), fileToUpload, this.uploadRequest.getHeaders(), true, this.uploadRequest.getCustomUserAgent(), this);
                    if (res) {
                        try {
                            fileToUpload.getFile().delete();
                            ILog.i(TAG, "File deleted: " + fileToUpload.getFileName());
                        } catch (Exception ex) {
                            ILog.e(TAG, "Unable to remove uploaded file", ex);
                        }
                    } else {
                        ILog.w(TAG, "Error ocurred with "+fileToUpload.getFileName());
                    }
                    errorOccured = !res || errorOccured;
                } catch (IOException e) {
                    Log.e(TAG, "Failed to upload file "+fileToUpload.getFileName()+". Will try again", e);
                    // this was not a server error response, so we should try again.
                    fileToUploadLinkedList.push(fileToUpload);
                    try {
                        Thread.sleep(5000);
                    } catch (InterruptedException e1) {
                        Log.w(TAG, "upload sleep interrupted");
                    }
                }
            }

            return null;
        }

        @Override
        protected void onCancelled() {
            super.onCancelled();
            ILog.d(TAG, "Task upload cancel!");
        }

        @Override
        protected void onProgressUpdate(Long... values) {
            uploadedBytes += values[0];
            if (!isCancelled())
                feedback(context, UploadServiceEvent.RUNNING, uploadedBytes, null);
        }

        public void updateCounter(long val) {
            publishProgress(val);
        }
    }


    private static void feedback(Context context, final UploadServiceEvent event, final Long uploadedBytes, final Long totalBytes) {

        Intent intent = new Intent(UPLOAD_FEEDBACK_ACTION);
        intent.putExtra("event", event);

        if (event.isRunning() && uploadedBytes != null) {
            if (uploadedBytes != null)
                intent.putExtra("uploadedBytes", uploadedBytes);
            if (totalBytes != null)
                intent.putExtra("totalBytes", totalBytes);
        }

        LocalBroadcastManager b = LocalBroadcastManager.getInstance(context);
        b.sendBroadcast(intent);
    }

    public static void stop(Context context) {
        Intent videoUploadIntent = new Intent(context, UploadService.class);
        context.stopService(videoUploadIntent);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (uploadTask != null && !uploadTask.isCancelled())
            uploadTask.cancel(true);
        ILog.d(TAG, "service exiting...");
    }
}
