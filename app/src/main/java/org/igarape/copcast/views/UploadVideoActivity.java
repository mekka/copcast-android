package org.igarape.copcast.views;

import java.io.File;
import java.io.FilenameFilter;
import java.net.URL;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
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


/**
 * Activity that demonstrates how to use Android Upload Service.
 *
 * @author Alex Gotev
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
    private List< NameValuePair > parameterName;
    private int numVideos=1;

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
            Log.i(TAG, message);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_upload_video);

        // Set your application namespace to avoid conflicts with other apps
        // using this library
        UploadService.NAMESPACE = "org.igarape.copcast";

        //old service
        String userLogin = (String) savedInstanceState.getSerializable("userLogin");
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

                String url = "/videos/";


                progressBar = (ProgressBar) findViewById(R.id.uploadProgress);
                serverUrl = BuildConfig.serverUrl + url;
                fileToUpload = nextVideo.getAbsolutePath();
                parameterName = new ArrayList<NameValuePair>();
                parameterName.add(new BasicNameValuePair("date", df.format(new Date(nextVideo.lastModified()))));

                uploadButton = (Button) findViewById(R.id.uploadButton);
                cancelUploadButton = (Button) findViewById(R.id.cancelUploadButton);

            }

        }




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

    private void onUploadButtonClick() {
        final String serverUrlString = serverUrl;
        final String fileToUploadPath = fileToUpload;
        final String paramNameString = parameterName.get(0).getName();

        if (!userInputIsValid(serverUrlString, fileToUploadPath))
            return;

        final UploadRequest request = new UploadRequest(this, UUID.randomUUID().toString(), serverUrlString);

        request.addFileToUpload(fileToUploadPath, paramNameString, fileToUpload, ContentType.VIDEO_MPEG);

        request.setNotificationConfig(R.drawable.ic_launcher, getString(R.string.app_name),
                getString(R.string.uploading), getString(R.string.upload_completed),
                getString(R.string.upload_stopped), false);

        // if you comment the following line, the system default user-agent will be used
        request.setCustomUserAgent("UploadServiceDemo/1.0");

        // set the intent to perform when the user taps on the upload notification.
        // currently tested only with intents that launches an activity
        // if you comment this line, no action will be performed when the user taps on the notification
        request.setNotificationClickIntent(new Intent(this, MainActivity.class));

        // set the maximum number of automatic upload retries on error
        request.setMaxRetries(2);

        try {
            UploadService.startUpload(request);
        } catch (Exception exc) {
            Toast.makeText(this, "Malformed upload request. " + exc.getLocalizedMessage(), Toast.LENGTH_SHORT).show();
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
