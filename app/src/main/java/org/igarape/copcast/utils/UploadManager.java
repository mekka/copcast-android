package org.igarape.copcast.utils;

import android.content.Context;
import android.content.Intent;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import com.alexbbb.uploadservice.ContentType;
import com.alexbbb.uploadservice.UploadRequest;
import com.alexbbb.uploadservice.UploadService;

import org.igarape.copcast.R;
import org.igarape.copcast.views.MainActivity;

import java.io.File;
import java.io.FilenameFilter;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import static org.igarape.copcast.utils.Globals.getDirectoryUploadedSize;

/**
 * Created by brunosiqueira on 14/08/15.
 */
public class UploadManager {
    public static final String UPLOAD_PROGRESS_ACTION = "org.igarape.copcast.UPLOAD_PROGRESS";
    public static final String CANCEL_UPLOAD_ACTION = "org.igarape.copcast.CANCEL_UPLOAD";
    public static final String COMPLETED_UPLOAD_ACTION = "org.igarape.copcast.COMPLETED_UPLOAD";
    private static final String TAG = UploadManager.class.getName();
    private final LocalBroadcastManager broadcaster;
    private List<String> users = null;
    private final GenericExtFilter filter = new GenericExtFilter(".mp4");
    private ArrayList<File> videos;
    private DateFormat df = new SimpleDateFormat(FileUtils.DATE_FORMAT);

    private Context context;
    private String userLogin;
    private String userPath;

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
            userLogin = users.remove(0);
            userPath = FileUtils.getPath(userLogin);

            File dir = new File(userPath);
            File[] files = dir.listFiles(filter);

            if (files != null && files.length > 0) {
                videos = new ArrayList<File>(Arrays.asList(files));
            }
        }

        File nextVideo = null;
        if (!videos.isEmpty()){
            nextVideo = videos.remove(0);
        }

        Intent intent = new Intent(context, UploadService.class);
        intent.putExtra("nextVideo", nextVideo);
        intent.putExtra("userLogin", userLogin);

        context.startService(intent);

//**********//
        final UploadRequest request = new UploadRequest(context, UUID.randomUUID().toString(), Globals.SERVER_URL+"/videos/"+userLogin);

        request.addHeader("Authorization", Globals.getAccessToken(context));

        // add param Date
        request.addParameter("date", df.format(new Date(nextVideo.lastModified())));

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