package org.igarape.copcast.utils;

import android.content.Context;
import android.content.Intent;
import android.support.v4.content.LocalBroadcastManager;

import org.igarape.copcast.service.UploadService;

import java.io.File;
import java.io.FilenameFilter;
import java.util.ArrayList;
import java.util.Arrays;
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
    private final LocalBroadcastManager broadcaster;
    private List<String> users = null;

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
        if ((users == null || users.isEmpty())
                //&& (videos == null || videos.isEmpty())
                ){
            sendCompletedToUI(broadcaster);
            return;
        }
        if (userLogin == null
                // || videos == null || videos.isEmpty()
                ){
            userLogin = users.remove(0);
            userPath = FileUtils.getPath(userLogin);


        }



        Intent intent = new Intent(context, UploadService.class);

        intent.putExtra("userLogin", userLogin);

        context.startService(intent);

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


}
