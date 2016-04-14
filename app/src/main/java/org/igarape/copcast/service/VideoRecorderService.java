package org.igarape.copcast.service;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.PixelFormat;
import android.os.AsyncTask;
import android.os.Binder;
import android.os.IBinder;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.TaskStackBuilder;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.view.Gravity;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.WindowManager;

import org.igarape.copcast.R;
import org.igarape.copcast.promises.PromiseError;
import org.igarape.copcast.state.IncidentFlagState;
import org.igarape.copcast.utils.FileUtils;
import org.igarape.copcast.utils.Globals;
import org.igarape.copcast.promises.Promise;
import org.igarape.copcast.views.MainActivity;
import org.igarape.webrecorder.WebRecorder;
import org.igarape.webrecorder.WebRecorderException;

import java.net.URISyntaxException;
import java.util.Date;
import java.util.concurrent.locks.ReentrantLock;

import io.socket.client.IO;
import io.socket.client.Socket;
import io.socket.emitter.Emitter;


public class VideoRecorderService extends Service implements SurfaceHolder.Callback {

    public static String STARTED_STREAMING = "org.igarape.mycopcast.StreamStarted";
    public static String STOPPED_STREAMING  = "org.igarape.mycopcast.StreamStopped";
    private static final String TAG = VideoRecorderService.class.getName();
    private final IBinder mBinder = new LocalBinder();
    private WindowManager windowManager;
    private SurfaceView surfaceView;
    private boolean isRecording;
    protected SurfaceHolder surfaceHolder;
    public static final int MAX_DURATION_MS = 300000;
    private int mId = 1;
    private ReentrantLock lock = new ReentrantLock();
    private boolean serviceExiting = false;
    public static boolean serviceRunning = false;
    private static String videoFileName;
    WebRecorder webRecorder;
    LocalBroadcastManager localBroadcastManager;
    private Socket ws;


    public class LocalBinder extends Binder {
        public VideoRecorderService getService() {

            Log.d(TAG, ">>>>>>>> SERVIÇO!");
            return VideoRecorderService.this;
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent == null){
            stopSelf();
            return START_STICKY;
        }

        String query = "token="+Globals.getPlainToken(this);
        query += "&userId="+Globals.getUserId(this);
        query += "&clientType=android";

        try {
            IO.Options opts = new IO.Options();
            opts.forceNew = true;
            opts.query = query;
            opts.reconnection = true;
            opts.timeout = 5000;
            ws = IO.socket(Globals.getServerUrl(this), opts);
        } catch (URISyntaxException e) {
            Log.e(TAG, "error connecting socket", e);
        }

        ws.on("startStreaming", new Emitter.Listener() {
            @Override
            public void call(Object... args) {
                VideoRecorderService.this.startStreaming();
                VideoRecorderService.this.sendBroadcast(VideoRecorderService.STARTED_STREAMING);

                Log.e(TAG, "Start Stream!!!");
            }
        });

        ws.on("stopStreaming", new Emitter.Listener() {
            @Override
            public void call(Object... args) {
                VideoRecorderService.this.stopStreaming();
                VideoRecorderService.this.sendBroadcast(VideoRecorderService.STOPPED_STREAMING);
                Log.e(TAG, "Stop Stream!!!");
            }
        });

        ws.connect();

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
        layoutParams.gravity = Gravity.START | Gravity.TOP;
        windowManager.addView(surfaceView, layoutParams);
        surfaceView.getHolder().addCallback(this);

        localBroadcastManager = LocalBroadcastManager.getInstance(context);

//        try {
//            IO.Options opts = new IO.Options();
//            opts.forceNew = true;
//            opts.query = "token=" + Globals.getAccessTokenStraight(getApplicationContext()) + "&clientType=android";
//            opts.reconnection = true;
//            mSocketClient = IO.socket(Globals.getServerUrl(getApplicationContext()), opts);
//            mSocketClient.connect();
//        } catch (URISyntaxException e) {
//            Log.e(TAG, "error connecting socket", e);
//        }
//

        return START_STICKY;
    }


    @Override
    public void surfaceCreated(SurfaceHolder surfaceHolder) {
        this.surfaceHolder = surfaceHolder;
        new MediaPrepareTask().execute();
    }

    private boolean prepareMediaEncoder() {

//        if (serviceExiting)
//            return false;
//
//        lock.lock();
//        Log.d(TAG, "> prepare locked");
//        try {
//            try {
//                camera = Camera.open(); // attempt to get a Camera instance
//            } catch (Exception e) {
//                Log.e(TAG, "Failed to open camera.");
//                return false;
//            }
//
//            Camera.Parameters params = camera.getParameters();
//            params.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_VIDEO);
//            camera.setParameters(params);
//            camera.unlock();
//
//            mediaRecorder = new MediaRecorder();
//            mediaRecorder.setPreviewDisplay(this.surfaceHolder.getSurface());
//            mediaRecorder.setCamera(camera);
//            mediaRecorder.setAudioSource(MediaRecorder.AudioSource.CAMCORDER);
//            mediaRecorder.setVideoSource(MediaRecorder.VideoSource.CAMERA);
//
//            //chain to identify available resolutions
//            Globals.appCamcoderProfile = CamcorderProfile.QUALITY_LOW;
//            CamcorderProfile profile = CamcorderProfile.get(Globals.appCamcoderProfile);
//            try {
//                Globals.appCamcoderProfile = CamcorderProfile.QUALITY_QVGA;
//                profile = CamcorderProfile.get(Globals.appCamcoderProfile);
//            } catch (RuntimeException ex) {
//                Log.w(TAG, "Failed to set QVGA video profile. Trying CIF");
//                try {
//                    Globals.appCamcoderProfile = CamcorderProfile.QUALITY_CIF;
//                    profile = CamcorderProfile.get(Globals.appCamcoderProfile);
//                } catch (RuntimeException ex2) {
//                    Log.w(TAG, "Failed to set CIF video profile. Falling back to LOW");
//                    Globals.appCamcoderProfile = CamcorderProfile.QUALITY_LOW;
//                }
//            }
//
//            // tune video parameters to reduce size
//            profile.videoBitRate = 156000;
//            profile.videoFrameRate = 12;
//            profile.audioBitRate = 24000;
//            profile.audioChannels = 1;
//
//            // Apply to MediaRecorder
//            mediaRecorder.setProfile(profile);
//
//            videoFileName = FileUtils.getPath(Globals.getUserLogin(getBaseContext())) +
//                    android.text.format.DateFormat.format("yyyy-MM-dd_kk-mm-ss", new Date().getTime()) +
//                    ".mp4";
//
//            Globals.setCurrentVideoPath(videoFileName);
//            mediaRecorder.setOutputFile(videoFileName);
//
//            if (Globals.getIncidentFlag() == IncidentFlagState.FLAG_PENDING) {
//                IncidentUtils.registerIncident(getApplicationContext(), Globals.getCurrentVideoPath());
//                Globals.setIncidentFlag(IncidentFlagState.FLAGGED);
//            }
//
//            mediaRecorder.setOrientationHint(getScreenOrientation(Globals.getRotation()));
//            mediaRecorder.setMaxDuration(MAX_DURATION_MS);
//            mediaRecorder.setMaxFileSize(MAX_SIZE_BYTES);
//            mediaRecorder.setOnInfoListener(new MediaRecorder.OnInfoListener() {
//                @Override
//                public void onInfo(MediaRecorder mediaRecorder, int what, int extra) {
//                    if (what == MediaRecorder.MEDIA_RECORDER_INFO_MAX_DURATION_REACHED ||
//                            what == MediaRecorder.MEDIA_RECORDER_INFO_MAX_FILESIZE_REACHED) {
//
//                        releaseMediaRecorder();
//                        new MediaPrepareTask().execute();
//                    }
//                }
//            });
//
//            try {
//                mediaRecorder.prepare();
//            } catch (IOException e) {
//                Log.e(TAG, "ioException on prepareMediaEncoder");
//                return false;
//            }
//
//            mediaRecorder.start();
//
//            return true;
//
//        } finally {
//            lock.unlock();
//            Log.d(TAG, "< prepare unlocked");
//        }

        videoFileName = FileUtils.getPath(Globals.getUserLogin(getBaseContext())) +
                android.text.format.DateFormat.format("yyyy-MM-dd_kk-mm-ss", new Date().getTime())+".mp4";

        lock.lock();
        Log.d(TAG, "> prepare locked");

        webRecorder = new WebRecorder.Builder(videoFileName, 320, 240)
                .setVideoBitRate(200000)
                .setVideoFrameRate(10)
                .setVideoIFrameInterval(2)
                .setWebsocket(VideoRecorderService.this.ws)
                .build();
//                .setStreamStartedRunnable(new Runnable() {
//                    @Override
//                    public void run() {
//                        Log.e(TAG, "runnable start");

//                    }
//                })
//
//                .setStreamStoppedRunnable(new Runnable() {
//                    @Override
//                    public void run() {
//                        Log.e(TAG, "runnable stop");
//                    }
//                })



        try {
            webRecorder.prepare(this.surfaceHolder);
            webRecorder.start();
        } catch (WebRecorderException e) {
            e.printStackTrace();
        } finally {
            lock.unlock();
            Log.d(TAG, "< prepare unlocked");
        }



        return true;
    }

    @Override
    public void onDestroy() {

        serviceExiting = true;
        if (webRecorder != null)
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
//        if (Globals.isToggling()){
//            Globals.setLivestreamToggle(false);
//
//            Intent intentAux = new Intent(this, StreamService.class);
//            intentAux.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
//            startService(intentAux);
//        }
//        serviceRunning = false;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    public void startStreaming() {
        webRecorder.startBroadcasting();
    }

    public void stopStreaming() {
        if (webRecorder != null)
            webRecorder.stopBroadcasting();
    }

    private void releaseMediaRecorder() {

        Globals.setIncidentFlag(IncidentFlagState.NOT_FLAGGED);

        webRecorder.stop(new Promise() {
            @Override
            public void error(PromiseError exception) {
                Log.e(TAG, "Failed to stop webrecorder: " + exception.toString());
            }
        });
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
                orientation = 270;
                break;
            case Surface.ROTATION_270:
                orientation = 180;
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

    public void stop(Promise promise) {

        lock.lock();
        Log.d(TAG, "< stop locked");
        try {
            webRecorder.stop(promise);
            webRecorder = null;
            serviceRunning = false;
            Globals.setIncidentFlag(IncidentFlagState.NOT_FLAGGED);
            this.sendBroadcast(VideoRecorderService.STOPPED_STREAMING);
            this.stopSelf();
        } finally {
            lock.unlock();
            Log.d(TAG, "< stop unlocked");

        }
    }

    public void sendBroadcast(String intentType) {
        Intent intent = new Intent(intentType);
        LocalBroadcastManager b = LocalBroadcastManager.getInstance(getApplicationContext());
        b.sendBroadcast(intent);
        Log.e(TAG, "intent sent: "+intentType);
    }

}
