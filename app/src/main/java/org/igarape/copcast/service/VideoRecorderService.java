package org.igarape.copcast.service;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.PixelFormat;
import android.hardware.Camera;
import android.media.CamcorderProfile;
import android.media.MediaRecorder;
import android.os.AsyncTask;
import android.os.IBinder;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.TaskStackBuilder;
import android.util.Log;
import android.view.Gravity;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.WindowManager;

import org.igarape.copcast.R;
import org.igarape.copcast.utils.FileUtils;
import org.igarape.copcast.utils.Globals;
import org.igarape.copcast.views.MainActivity;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;

/**
 * Created by fcavalcanti on 19/11/2014.
 */
public class VideoRecorderService extends Service implements SurfaceHolder.Callback {

    private static final String TAG = VideoRecorderService.class.getName();

    private WindowManager windowManager;
    private SurfaceView surfaceView;
    private Camera camera = null;
    private MediaRecorder mediaRecorder = null;
    private boolean isRecording;
    protected SurfaceHolder surfaceHolder;
    public static final int MAX_DURATION_MS = 300000;
    public static final long MAX_SIZE_BYTES = 7500000;
    private int mId = 1;

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent == null){
            stopSelf();
            return START_STICKY;
        }

        Intent resultIntent = new Intent(this, MainActivity.class);
        Context context = getApplicationContext();
        NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(context)
                .setContentTitle(getString(R.string.notification_record_title))
                .setContentText(getString(R.string.notification_record_description))
                .setSmallIcon(R.drawable.ic_launcher)
                .setOngoing(true);

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

        // Create new SurfaceView, set its size to 1x1, move it to the top left corner and set this service as a callback
        windowManager = (WindowManager) this.getSystemService(Context.WINDOW_SERVICE);
        surfaceView = new SurfaceView(this);
        WindowManager.LayoutParams layoutParams = new WindowManager.LayoutParams(
                1, 1,
                WindowManager.LayoutParams.TYPE_SYSTEM_OVERLAY,
                WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH,
                PixelFormat.TRANSLUCENT
        );
        layoutParams.gravity = Gravity.LEFT | Gravity.TOP;
        windowManager.addView(surfaceView, layoutParams);
        surfaceView.getHolder().addCallback(this);
        return START_STICKY;
    }


    @Override
    public void surfaceCreated(SurfaceHolder surfaceHolder) {
        this.surfaceHolder = surfaceHolder;
        new MediaPrepareTask().execute(null, null, null);
    }


    private boolean prepareMediaEncoder() {

        camera = Camera.open();
        mediaRecorder = new MediaRecorder();
        camera.unlock();


        mediaRecorder.setPreviewDisplay(this.surfaceHolder.getSurface());
        mediaRecorder.setCamera(camera);
        mediaRecorder.setAudioSource(MediaRecorder.AudioSource.CAMCORDER);
        mediaRecorder.setVideoSource(MediaRecorder.VideoSource.CAMERA);
        mediaRecorder.setProfile(CamcorderProfile.get(CamcorderProfile.QUALITY_LOW));

        //mediaRecorder.setVideoSize(320, 240);

        mediaRecorder.setOutputFile(
                FileUtils.getPath(Globals.getUserLogin(getBaseContext())) +
                        android.text.format.DateFormat.format("yyyy-MM-dd_kk-mm-ss", new Date().getTime()) +
                        ".mp4");

        mediaRecorder.setOrientationHint(90);
        mediaRecorder.setMaxDuration(MAX_DURATION_MS);
        mediaRecorder.setMaxFileSize(MAX_SIZE_BYTES);
        mediaRecorder.setOnInfoListener(new MediaRecorder.OnInfoListener() {
            @Override
            public void onInfo(MediaRecorder mediaRecorder, int what, int extra) {
                if (what == MediaRecorder.MEDIA_RECORDER_INFO_MAX_DURATION_REACHED ||
                        what == MediaRecorder.MEDIA_RECORDER_INFO_MAX_FILESIZE_REACHED) {
                    releaseMediaRecorder();
                    new MediaPrepareTask().execute(null, null, null);
                }
            }
        });

        try {
            mediaRecorder.prepare();
        } catch (IOException e) {
            Log.e(TAG,"ioException on prepareMediaEncoder", e);
            return false;
        }
        return true;
    }

    @Override
    public void onDestroy() {
        releaseMediaRecorder();
        windowManager.removeView(surfaceView);

        NotificationManager mNotificationManager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        mNotificationManager.cancel(mId);

        if (Globals.isToggling()){
            Intent intentAux = new Intent(this, StreamService.class);
            intentAux.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startService(intentAux);

            Globals.setToggling(false);
        }
    }

    @Override
    public void surfaceChanged(SurfaceHolder surfaceHolder, int format, int width, int height) {
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder surfaceHolder) {
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private void releaseMediaRecorder() {
        try {
            if (mediaRecorder != null) {
                mediaRecorder.stop();
                mediaRecorder.reset();
                mediaRecorder.release();
                mediaRecorder = null;
                //tem que ter???
                camera.lock();
                camera.release();
            }
        } catch (IllegalStateException i) {
        }
    }

    class MediaPrepareTask extends AsyncTask<Void, Void, Boolean> {

        @Override
        protected Boolean doInBackground(Void... voids) {
            if (prepareMediaEncoder()) {
                mediaRecorder.start();
                isRecording = true;
            } else {
                releaseMediaRecorder();
                isRecording = false;
                return false;
            }
            return true;
        }

        @Override
        protected void onPostExecute(Boolean result) {
            if (!result) {
                //MainActivity.this.finish();
            }
            // inform the user that recording has started
            //setCaptureButtonText("Stop");
        }
    }

}