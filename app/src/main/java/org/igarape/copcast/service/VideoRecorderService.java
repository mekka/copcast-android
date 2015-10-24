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
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.WindowManager;

import org.igarape.copcast.R;
import org.igarape.copcast.state.IncidentFlagState;
import org.igarape.copcast.utils.FileUtils;
import org.igarape.copcast.utils.Globals;
import org.igarape.copcast.utils.IncidentUtils;
import org.igarape.copcast.views.MainActivity;
import org.json.JSONException;

import java.io.IOException;
import java.util.Date;
import java.util.concurrent.locks.ReentrantLock;


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
    private ReentrantLock lock = new ReentrantLock();
    private boolean serviceExiting = false;
    public static boolean serviceRunning = false;
    private static String videoFileName;

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent == null){
            stopSelf();
            return START_STICKY;
        }

        serviceRunning = true;

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
        new MediaPrepareTask().execute();
    }

    private boolean prepareMediaEncoder() {

        if (serviceExiting)
            return false;

        lock.lock();
        Log.d(TAG, "> prepare locked");
        try {
            try {
                camera = Camera.open(); // attempt to get a Camera instance
            } catch (Exception e) {
                Log.e(TAG, "Failed to open camera.");
                return false;
            }

            Camera.Parameters params = camera.getParameters();
            params.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_VIDEO);
            camera.setParameters(params);
            camera.unlock();

            mediaRecorder = new MediaRecorder();
            mediaRecorder.setPreviewDisplay(this.surfaceHolder.getSurface());
            mediaRecorder.setCamera(camera);
            mediaRecorder.setAudioSource(MediaRecorder.AudioSource.CAMCORDER);
            mediaRecorder.setVideoSource(MediaRecorder.VideoSource.CAMERA);
            mediaRecorder.setProfile(CamcorderProfile.get(CamcorderProfile.QUALITY_LOW));

            videoFileName = FileUtils.getPath(Globals.getUserLogin(getBaseContext())) +
                    android.text.format.DateFormat.format("yyyy-MM-dd_kk-mm-ss", new Date().getTime()) +
                    ".mp4";

            Globals.setCurrentVideoPath(videoFileName);
            mediaRecorder.setOutputFile(videoFileName);

            if (Globals.getIncidentFlag() == IncidentFlagState.FLAG_PENDING) {
                IncidentUtils.registerIncident(getApplicationContext(), Globals.getCurrentVideoPath());
                Globals.setIncidentFlag(IncidentFlagState.FLAGGED);
            }

            mediaRecorder.setOrientationHint(getScreenOrientation(Globals.getRotation()));
            mediaRecorder.setMaxDuration(MAX_DURATION_MS);
            mediaRecorder.setMaxFileSize(MAX_SIZE_BYTES);
            mediaRecorder.setOnInfoListener(new MediaRecorder.OnInfoListener() {
                @Override
                public void onInfo(MediaRecorder mediaRecorder, int what, int extra) {
                    if (what == MediaRecorder.MEDIA_RECORDER_INFO_MAX_DURATION_REACHED ||
                            what == MediaRecorder.MEDIA_RECORDER_INFO_MAX_FILESIZE_REACHED) {

                        releaseMediaRecorder();
                        new MediaPrepareTask().execute();
                    }
                }
            });

            try {
                mediaRecorder.prepare();
            } catch (IOException e) {
                Log.e(TAG, "ioException on prepareMediaEncoder");
                return false;
            }

            mediaRecorder.start();

            return true;

        } finally {
            lock.unlock();
            Log.d(TAG, "< prepare unlocked");
        }
    }

    @Override
    public void onDestroy() {

        serviceExiting = true;
        releaseMediaRecorder();

        if(null != windowManager && null != surfaceView){
            windowManager.removeView(surfaceView);
            Log.d(TAG, "onDestroy with windowManager=["+windowManager+" and surfaceView=["+surfaceView+"]");
        }

        NotificationManager mNotificationManager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        mNotificationManager.cancel(mId);


    }

    @Override
    public void surfaceChanged(SurfaceHolder surfaceHolder, int format, int width, int height) {
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder surfaceHolder) {
        if (Globals.isToggling()){
            Globals.setToggling(false);

            Intent intentAux = new Intent(this, StreamService.class);
            intentAux.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startService(intentAux);
        }
        serviceRunning = false;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private void releaseMediaRecorder() {

        Globals.setIncidentFlag(IncidentFlagState.NOT_FLAGGED);

        lock.lock();
        Log.d(TAG, "> release locked");

        //clear incident flag;

        try {
            if (mediaRecorder != null) {
//                mediaRecorder.stop();
                mediaRecorder.reset();
                mediaRecorder.release();
                mediaRecorder = null;
            }

            if (camera != null) {
//                try {
//                    camera.stopPreview();
//                } catch (Exception e){
//                    Log.w(TAG, "releasing camera 1", e);
//                }
//                try {
//                    camera.lock();
                camera.release();
//                } catch (Exception e){
//                    Log.w(TAG, "releasing camera 2", e);
//                }
            }

        } catch (IllegalStateException i) {
            Log.e(TAG,"IllegalStateException on prepareMediaEncoder", i);
        } finally {
            lock.unlock();
            Log.d(TAG, "< release unlocked");
        }
    }

    class MediaPrepareTask extends AsyncTask<Void, Void, Boolean> {

        @Override
        protected Boolean doInBackground(Void... flags) {
            return prepareMediaEncoder();
        }
    }

    private int getScreenOrientation(int rotation) {
        int orientation;
        // if the device's natural orientation is portrait:
        switch(rotation) {
            case Surface.ROTATION_0:
                orientation = 90;
                break;
            case Surface.ROTATION_90:
                orientation = 0;
                break;
            case Surface.ROTATION_180:
                orientation =
                        270;
                break;
            case Surface.ROTATION_270:
                orientation =
                        180;
                break;
            default:
                Log.e(TAG, "Unknown screen orientation. Defaulting to " +
                        "portrait.");
                orientation = 0;
                break;
        }
        Log.d(TAG, "orientation: "+orientation);
        return orientation;
    }

}