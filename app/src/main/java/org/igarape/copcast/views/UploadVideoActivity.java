package org.igarape.copcast.views;

import java.io.File;
import java.io.FilenameFilter;
import java.net.URL;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

import android.app.Activity;
import android.content.Intent;
        import android.os.Bundle;
        //import android.support.v7.app.ActionBarActivity;
        import android.util.Log;
        import android.view.View;
        import android.widget.Button;
        //import android.widget.EditText;
        import android.widget.ProgressBar;
        import android.widget.Toast;


import com.alexbbb.uploadservice.AbstractUploadServiceReceiver;
import com.alexbbb.uploadservice.ContentType;
import com.alexbbb.uploadservice.UploadRequest;
import com.alexbbb.uploadservice.UploadService;

import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;
import org.igarape.copcast.BuildConfig;
import org.igarape.copcast.R;
import org.igarape.copcast.utils.FileUtils;
import org.igarape.copcast.utils.Globals;


/**
 * Upload the video from app to server
 * @author Alex Salgado based on the library android-upload-service
 *
 */
//public class MainActivity extends ActionBarActivity {
public class UploadVideoActivity extends Activity {

    private static final String TAG = "UploadVideoActivity";

    private ProgressBar progressBar;
    private Button uploadButton;
    private Button cancelUploadButton;
    private String serverUrl;
    private String fileToUpload;
    private String fileToUploadPath;
    private List< NameValuePair > parameterName;
    private int numVideos=1000;  //max num to upload per user
    private HashMap<String, String> totFiles = new HashMap<String, String>();  //files to upload

    private final GenericExtFilter filter = new GenericExtFilter(".mp4");
    private DateFormat df = new SimpleDateFormat(FileUtils.DATE_FORMAT);


    private final AbstractUploadServiceReceiver uploadReceiver = new AbstractUploadServiceReceiver() {

        @Override
        public void onProgress(String uploadId, int progress) {
            progressBar.setProgress(progress);

            Log.i(TAG, "The progress of the upload with ID " + uploadId + " is: " + progress);
        }

        @Override
        public void onError(String uploadId, Exception exception) {
            progressBar.setProgress(0);

            String message = "Error in upload with ID: " + uploadId + ". " + exception.getLocalizedMessage();
            Log.e(TAG, message, exception);
        }

        @Override
        public void onCompleted(String uploadId, int serverResponseCode, String serverResponseMessage) {
            progressBar.setProgress(0);

            String message = "Upload with ID " + uploadId + " is completed: " + serverResponseCode + ", "
                    + serverResponseMessage;

            if ( (serverResponseCode == 201 ||
                    serverResponseCode == 200 ) &&
                    (serverResponseMessage.toLowerCase().equals("created")))
            {
                //delete video
                deleteFile(uploadId);

            }
            Log.i(TAG, message);
        }

        /*
            Delete the file after upload it to the server
         */
        private boolean deleteFile(String uploadId)
        {
            String selectedFilePath = totFiles.get(uploadId);
            boolean deleted= false;

            if(selectedFilePath.length() > 0) {
                File file = new File(selectedFilePath);
                deleted = file.delete();
            }

            return deleted;

        }

    };



    //get the users and start upload the files
    private void startUploadAllUsers()
    {
        List<String> users = null;
        String userLogin;

        users = new ArrayList<String>();

        Collections.addAll(users, FileUtils.getUserFolders());

        if (users == null || users.isEmpty() ){
            Log.i(TAG, "Users does not have file to upload");
            return;
        }
        Integer cont=1;
        while(!users.isEmpty()) {
            userLogin = users.remove(0);
            if (!userLogin.toLowerCase().equals("null"))
                sendOneUser(userLogin);

            cont++;
            Log.i(TAG, "Usuario = " + cont.toString());

        }
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // todo: get userlogin
        // String userLogin = "asalgado"
        String userLogin = Globals.getUserLogin(getApplicationContext());

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_upload_video);

        // Set your application namespace to avoid conflicts with other apps
        // using this library
        UploadService.NAMESPACE = "org.igarape.copcast";


        uploadButton = (Button) findViewById(R.id.uploadButton);
        cancelUploadButton = (Button) findViewById(R.id.cancelUploadButton);

        //old service
        //String userLogin = (String) savedInstanceState.getSerializable("userLogin");

        String path = FileUtils.getPath(userLogin);

        File dir = new File(path);
        File[] files = dir.listFiles(filter);
        File nextVideo = null;
        int cont=1;
        ArrayList<File> videos = null;


        if (files != null && files.length > 0) {
            videos = new ArrayList<File>(Arrays.asList(files));
            if (!videos.isEmpty())
                    //&& cont<=numVideos)
            {
                nextVideo = videos.remove(0);
                //uploadVideo(nextVideo, userLogin, cont);
                //cont++;

                String url = "/videos/" + userLogin;


                serverUrl = BuildConfig.serverUrl + url;
                fileToUpload = nextVideo.getName();
                fileToUploadPath = nextVideo.getAbsolutePath();
                parameterName = new ArrayList<NameValuePair>();
                parameterName.add(new BasicNameValuePair("date", df.format(new Date(nextVideo.lastModified()))));

            }

        }

        progressBar = (ProgressBar) findViewById(R.id.uploadProgress);



        uploadButton.setOnClickListener(new Button.OnClickListener() {

            @Override
            public void onClick(View arg0) {
                onUploadButtonClick();
            }
        });

        cancelUploadButton.setOnClickListener(new Button.OnClickListener() {

            @Override
            public void onClick(View arg0) {
                onCancelUploadButtonClick();
            }
        });

        progressBar.setMax(100);
        progressBar.setProgress(0);

        // De-comment this line to enable self-signed SSL certificates in HTTPS connections
        // WARNING: Do not use in production environment. Recommended for development only
        // AllCertificatesAndHostsTruster.apply();
    }

    @Override
    protected void onResume() {
        super.onResume();
        uploadReceiver.register(this);
        Log.i(TAG, "onResume");
    }

    @Override
    protected void onPause() {
        super.onPause();
        uploadReceiver.unregister(this);
        Log.i(TAG, "onPause");
    }

    private boolean userInputIsValid(final String serverUrlString, final String fileToUploadPath
    ) {
//        if (serverUrlString.length() == 0) {
//            Toast.makeText(this, getString(R.string.provide_valid_server_url), Toast.LENGTH_LONG).show();
//            return false;
//        }
//
//        try {
//            new URL(serverUrlString.toString());
//        } catch (Exception exc) {
//            Toast.makeText(this, getString(R.string.provide_valid_server_url), Toast.LENGTH_LONG).show();
//            return false;
//        }
//
//        if (fileToUploadPath.length() == 0) {
//            Toast.makeText(this, getString(R.string.provide_file_to_upload), Toast.LENGTH_LONG).show();
//            return false;
//        }
//
//        if (!new File(fileToUploadPath).exists()) {
//            Toast.makeText(this, getString(R.string.file_does_not_exist), Toast.LENGTH_LONG).show();
//            return false;
//        }
//
//        if (paramNameString.length() == 0) {
//            Toast.makeText(this, getString(R.string.provide_param_name), Toast.LENGTH_LONG).show();
//            return false;
//        }

        return true;
    }

    ArrayList<File> getVideos(String userLogin)
    {
        String path = FileUtils.getPath(userLogin);

        File dir = new File(path);
        File[] files = dir.listFiles(filter);
        File nextVideo = null;
        int cont=1;
        ArrayList<File> videos = null;

        if (files != null && files.length > 0) {
            videos = new ArrayList<File>(Arrays.asList(files));

            return videos;

        }
        else
            return null;

    }


    private void onUploadButtonClick() {
        startUploadAllUsers();
    }

    // upload all the videos from one user
    private void sendOneUser(String userLogin) {
       //String userLogin = "asalgado";
        String paramNameString = "video";
        String url = "/videos/" + userLogin;

        String serverUrlString = BuildConfig.serverUrl + url;

        /*
        if (!userInputIsValid(serverUrlString, fileToUploadPath))
            return;
        */

        serverUrl = BuildConfig.serverUrl + url;

        //retry videos
        ArrayList<File> videos = getVideos(userLogin);
        Integer cont = 1;

        while (videos != null && !videos.isEmpty() && cont<=numVideos)
        {
            File nextVideo = null;
            nextVideo = videos.remove(0);
            //uploadVideo(nextVideo, userLogin, cont);
            cont++;

            String token = Globals.getAccessToken(getApplicationContext());

            fileToUpload = nextVideo.getName();
            fileToUploadPath = nextVideo.getAbsolutePath();
            parameterName = new ArrayList<NameValuePair>();
            parameterName.add(new BasicNameValuePair("date", df.format(new Date(nextVideo.lastModified()))));

            String paramName = parameterName.get(0).getName();
            String paramValue = parameterName.get(0).getValue();

            sendOneFile(serverUrlString,
                     token,
                     paramName,
                     paramValue,
                     userLogin,
                     paramNameString );


            cont++;
            Log.i(TAG, "Videos Quantidade = " + cont.toString());
            Log.i(TAG, "Videos Nome = " + serverUrlString);
        }
    }


    private void sendOneFile(String serverUrlString,
                            String token,
                            String paramName,
                            String paramValue,
                            String userLogin,
                            String paramNameString ) {

        final UploadRequest request = new UploadRequest(this, UUID.randomUUID().toString(), serverUrlString);

        if (token != null) {
            request.addHeader("Authorization", token);
        }

        // add param Date
        request.addParameter(paramName, paramValue);

        String url = "/videos/" + userLogin;

        request.addFileToUpload(fileToUploadPath, paramNameString, fileToUpload, ContentType.VIDEO_MPEG);


        request.setNotificationConfig(R.drawable.ic_launcher, getString(R.string.app_name),
                getString(R.string.uploading), getString(R.string.upload_completed),
                getString(R.string.upload_stopped), false);

        // if you comment the following line, the system default user-agent will be used
        request.setCustomUserAgent("UploadServiceDemo/1.0");

        // set the intent to perform when the user taps on the upload notification.
        // currently tested only with intents that launches an activity
        // if you comment this line, no action will be performed when the user taps on the notification
        request.setNotificationClickIntent(new Intent(this, UploadVideoActivity.class));

        // set the maximum number of automatic upload retries on error
        request.setMaxRetries(2);

        try {
            UploadService.startUpload(request);

            //add the file to hashmap to be deleted after upload
            totFiles.put(request.getUploadId(), fileToUploadPath);

        } catch (Exception exc) {
            Log.i(TAG, "Malformed upload request. " + exc.getLocalizedMessage());
        }
    }

    private void onCancelUploadButtonClick() {
        UploadService.stopCurrentUpload();
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
