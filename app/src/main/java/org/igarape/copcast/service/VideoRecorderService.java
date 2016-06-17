package org.igarape.copcast.service;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
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
import org.igarape.copcast.receiver.BatteryReceiver;
import org.igarape.copcast.state.IncidentFlagState;
import org.igarape.copcast.state.State;
import org.igarape.copcast.utils.FileUtils;
import org.igarape.copcast.utils.Globals;
import org.igarape.copcast.utils.ILog;
import org.igarape.copcast.utils.IncidentUtils;
import org.igarape.copcast.utils.StateManager;
import org.igarape.copcast.views.MainActivity;
import org.igarape.webrecorder.WebRecorder;
import org.igarape.webrecorder.WebRecorderException;
import org.igarape.webrecorder.enums.Orientation;

import java.net.URISyntaxException;
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
    private static String baseDir;
    WebRecorder webRecorder;
    LocalBroadcastManager localBroadcastManager;
    private Socket ws;
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

        BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, final Intent intent) {
                Log.d(TAG, "Orientation changed: "+intent.getExtras().getString("ORIENTATION"));
                if (webRecorder != null)
                    webRecorder.restartOrientation();
//                if (webRecorder != null)
//                    try {
//                        webRecorder.setVideoCodec90();
//                    } catch (WebRecorderException e) {
//                        Log.e(TAG, "Error rotating codec", e);
//                    } catch (InterruptedException e) {
//                        Log.e(TAG, "Error rotating codec", e);
//                    }
            }
        };

        IntentFilter broadcastFilter = new IntentFilter();
        broadcastFilter.addAction("ROTATION");
        LocalBroadcastManager.getInstance(this).registerReceiver(broadcastReceiver, broadcastFilter);

        String query = "token="+Globals.getPlainToken(this);
        query += "&userId="+Globals.getUserId(this);
        query += "&clientType=android";

        try {
            IO.Options opts = new IO.Options();
            opts.forceNew = true;
            opts.query = query;
            opts.reconnection = true;
            opts.reconnectionDelay=1000;
            opts.reconnectionDelayMax=1000;
            opts.timeout = 4000;
            opts.upgrade = true;
            opts.transports = new String[] {"websocket"};
            ws = IO.socket(Globals.getServerUrl(this), opts);

        } catch (URISyntaxException e) {
            Log.e(TAG, "error connecting socket", e);
        }

        ws.on("startStreaming", new Emitter.Listener() {
            @Override
            public void call(Object... args) {
                StateManager stateManager = Globals.getStateManager();
                if (stateManager.canChangeToState(State.STREAMING) || stateManager.isCurrent(State.STREAMING)) {
                    VideoRecorderService.this.startStreaming();
                    VideoRecorderService.this.sendBroadcast(VideoRecorderService.STARTED_STREAMING);
                    Log.e(TAG, "Start Stream!!!");
                } else {
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
                VideoRecorderService.this.sendBroadcast(VideoRecorderService.STOPPED_STREAMING);
                Log.e(TAG, "Stop Stream!!!");
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
                BuildConfig.STREAMING_QUALITY, surfaceHolder)
                .setVideoBitrate(BuildConfig.RECORDING_BITRATE)
                .setVideoFramerate(BuildConfig.RECORDING_FRAMERATE)
                .setLiveVideoBitrate(BuildConfig.STREAMING_BITRATE)
                .setLiveVideoFramerate(BuildConfig.STREAMING_FRAMERATE)
                .setVideoIFrameInterval(1)
                .setWebsocket(VideoRecorderService.this.ws)
                .build();

        try {
            webRecorder.prepare();
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
            webRecorder.stop(null);
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
    }

    public void startStreaming() {
        webRecorder.startBroadcasting();
    }

    public void stopStreaming() {
        if (webRecorder != null)
            webRecorder.stopBroadcasting();
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
        lock.lock();
        Log.d(TAG, "< stop locked");
        try {

            Globals.setIncidentFlag(IncidentFlagState.NOT_FLAGGED);

            NotificationManager mNotificationManager =
                    (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            mNotificationManager.cancel(mId);

            if (webRecorder != null) {
                webRecorder.stop(promise);
                webRecorder = null;
            }
            serviceRunning = false;
            ws.disconnect();
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
