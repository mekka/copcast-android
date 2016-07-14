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

import org.igarape.copcast.BuildConfig;
import org.igarape.copcast.R;
import org.igarape.copcast.promises.Promise;
import org.igarape.copcast.state.IncidentFlagState;
import org.igarape.copcast.state.State;
import org.igarape.copcast.utils.FileUtils;
import org.igarape.copcast.utils.Globals;
import org.igarape.copcast.utils.IncidentUtils;
import org.igarape.copcast.utils.VibrateUtils;
import org.igarape.copcast.views.MainActivity;
import org.igarape.copcast.ws.SocketSingleton;
import org.igarape.webrecorder.WebRecorder;
import org.igarape.webrecorder.WebRecorderException;

import java.util.concurrent.locks.ReentrantLock;

import io.socket.emitter.Emitter;


public class VideoRecorderService extends Service implements SurfaceHolder.Callback {

    public static String STARTED_STREAMING = "org.igarape.mycopcast.StreamStarted";
    public static String STOPPED_STREAMING  = "org.igarape.mycopcast.StreamStopped";
    public static String MISSION_STARTED  = "org.igarape.mycopcast.MissionStarted";
    private static final String TAG = VideoRecorderService.class.getName();
    private final IBinder mBinder = new LocalBinder();
    private WindowManager windowManager;
    private SurfaceView surfaceView;
    protected SurfaceHolder surfaceHolder;
    private int mId = 1;
    private ReentrantLock lock = new ReentrantLock();
    public static boolean serviceRunning = false;
    private static String baseDir;
    WebRecorder webRecorder;
    LocalBroadcastManager localBroadcastManager;
//    private Socket ws;
    SocketSingleton ws;
    private NotificationCompat.Builder mNotification;


    public class LocalBinder extends Binder {
        public VideoRecorderService getService() {
            return VideoRecorderService.this;
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent == null){
            stopSelf();
            return START_STICKY;
        }

//        String query = "token="+Globals.getPlainToken(this);
//        query += "&userId="+Globals.getUserId(this);
//        query += "&clientType=android";
//
//        try {
//            IO.Options opts = new IO.Options();
//            opts.forceNew = true;
//            opts.query = query;
//            opts.reconnection = true;
//            opts.reconnectionDelay=5000;
//            opts.reconnectionDelayMax=10000;
//            opts.timeout = 20000;
//            opts.upgrade = true;
//            opts.transports = new String[] {"websocket"};
//            ws = IO.socket(Globals.getServerUrl(this), opts);
//
//        } catch (URISyntaxException e) {
//            Log.e(TAG, "error connecting socket", e);
//        }

        ws = SocketSingleton.getInstance(this);

        ws.on("startStreaming", new Emitter.Listener() {
            @Override
            public  void call(Object... args) {
                if (webRecorder != null) {
                    Log.d(TAG, "will stream");
                    startStreaming();
                }
            }

            private synchronized void startStreaming() {
                if (Globals.getStateManager().canChangeToState(State.STREAMING)) {
                    webRecorder.startBroadcasting();
                    VideoRecorderService.this.sendBroadcast(VideoRecorderService.STARTED_STREAMING);
                    ws.emit("streamStarted");
                    VibrateUtils.vibrate(getApplicationContext(), 200);
                    Log.e(TAG, "Start Stream!!!");
                } else if (!Globals.getStateManager().isCurrent(State.STREAMING)) {
                    ws.emit("streamDenied");
                }
            }
        });

        ws.on("connect", new Emitter.Listener() {
            @Override
            public void call(Object... args) {
                Log.e(TAG, "connect");
            }
        });

        ws.on("reconnect", new Emitter.Listener() {
            @Override
            public void call(Object... args) {
                Log.e(TAG, "reconnect");
            }
        });

        ws.on("reconnect_attempt", new Emitter.Listener() {
            @Override
            public void call(Object... args) {
                Log.e(TAG, "reconnect attempt");
                VideoRecorderService.this.stopStreaming();
                Log.e(TAG, "Stop Stream!!!");
            }
        });

        ws.on("stopStreaming", new Emitter.Listener() {
            @Override
            public void call(Object... args) {
                VideoRecorderService.this.stopStreaming();
                Log.e(TAG, "Stop Stream!!!");
            }
        });

        ws.connect();

        serviceRunning = true;

        Intent resultIntent = new Intent(this, MainActivity.class);
        Context context = getApplicationContext();
        mNotification = new NotificationCompat.Builder(context)
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
        mNotification.setContentIntent(resultPendingIntent);
        NotificationManager mNotificationManager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        // mId allows you to update the notification later on.
        mNotificationManager.notify(mId, mNotification.build());

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


        return START_STICKY;
    }


    @Override
    public void surfaceCreated(SurfaceHolder surfaceHolder) {
        this.surfaceHolder = surfaceHolder;
        new MediaPrepareTask().execute();
    }

    private boolean prepareMediaEncoder() {


        baseDir = FileUtils.getPath(Globals.getUserLogin(getBaseContext()));

        if (Globals.getIncidentFlag() == IncidentFlagState.FLAG_PENDING) {
            Globals.setIncidentFlag(IncidentFlagState.FLAGGED);
            IncidentUtils.registerIncident(getApplicationContext());
        }


        lock.lock();
        Log.d(TAG, "> prepare locked");

        webRecorder = new WebRecorder.Builder(baseDir, BuildConfig.RECORDING_QUALITY,
                BuildConfig.STREAMING_QUALITY, Globals.getOrientation(this), surfaceHolder)
                .setVideoBitrate(BuildConfig.RECORDING_BITRATE)
                .setVideoFramerate(BuildConfig.RECORDING_FRAMERATE)
                .setLiveVideoBitrate(BuildConfig.STREAMING_BITRATE)
                .setLiveVideoFramerate(BuildConfig.STREAMING_FRAMERATE)
                .setVideoIFrameInterval(1)
                .setWebsocket(VideoRecorderService.this.ws.getWebsocket())
                .build();

        try {
            webRecorder.prepare();
            webRecorder.start();
            sendBroadcast(MISSION_STARTED);
            Log.d(TAG, "mission started");
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
        NotificationManager mNotificationManager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        mNotificationManager.cancel(mId);

        if (webRecorder != null) {
            try {
                webRecorder.stopSync();
            } catch (InterruptedException e) {
                Log.e(TAG, "error stopping webrecorder onDestroy", e);
            }
            webRecorder = null;
        }
        if(null != windowManager && null != surfaceView){
            windowManager.removeView(surfaceView);
            Log.d(TAG, "onDestroy with windowManager=["+windowManager+" and surfaceView=["+surfaceView+"]");
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
        return mBinder;
    }

    public void pauseRecording() {
        NotificationManager mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        mNotification.setContentText(getString(R.string.notification_paused_description));
        // mId allows you to update the notification later on.
        mNotificationManager.notify(mId, mNotification.build());
        if (webRecorder != null)
            webRecorder.stopBroadcasting();
            webRecorder.stop(null);
        ws.emit("missionPaused");
    }

    public void resumeRecording() {
        NotificationManager mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        mNotification.setContentText(getString(R.string.notification_record_description));
        // mId allows you to update the notification later on.
        mNotificationManager.notify(mId, mNotification.build());
        if (webRecorder != null) {
            try {
                webRecorder.prepare();
                webRecorder.start();
            } catch (WebRecorderException e) {
                Log.e(TAG, "Error resuming recording", e);
            }
        }
        ws.emit("missionResumed");
    }

    public void startStreamingRequest() {
        ws.emit("startStreamingRequest");
    }

    public void stopStreamingRequest() {
        ws.emit("stopStreamingRequest");
    }

    public void stopStreaming() {
        if (webRecorder != null && webRecorder.isStreaming()) {
            this.sendBroadcast(VideoRecorderService.STOPPED_STREAMING);
            webRecorder.stopBroadcasting();
            ws.emit("streamStopped");
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
                orientation = 270;
                break;
            case Surface.ROTATION_270:
                orientation = 180;
                break;
            default:
                Log.e(TAG, "Unknown screen orientation. Defaulting to portrait.");
                orientation = 0;
                break;
        }
        Log.d(TAG, "orientation: "+orientation);
        return orientation;
    }

    public void stop(Promise promise) {

        Globals.setIncidentFlag(IncidentFlagState.NOT_FLAGGED);

        NotificationManager mNotificationManager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        mNotificationManager.cancel(mId);

        this.stopStreaming();

        if (webRecorder != null) {
            webRecorder.stop(promise);
            webRecorder = null;
        }
        serviceRunning = false;
        ws.disconnect();
        Globals.setIncidentFlag(IncidentFlagState.NOT_FLAGGED);
        this.stopSelf();
    }

    public void stopSync() {
        Globals.setIncidentFlag(IncidentFlagState.NOT_FLAGGED);
        lock.lock();
        Log.d(TAG, "< stop locked");
        try {

            Globals.setIncidentFlag(IncidentFlagState.NOT_FLAGGED);

            NotificationManager mNotificationManager =
                    (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            mNotificationManager.cancel(mId);

            if (webRecorder != null) {
                webRecorder.stopSync();
                webRecorder = null;
            }
            serviceRunning = false;
            ws.disconnect();
            Globals.setIncidentFlag(IncidentFlagState.NOT_FLAGGED);
            this.stopStreaming();
            this.stopSelf();
        } catch (InterruptedException e) {
            Log.e(TAG, "error stopping video recording with stopSync", e);
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
