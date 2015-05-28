package org.igarape.copcast.service;

import android.app.IntentService;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.PixelFormat;
import android.graphics.Point;
import android.opengl.GLSurfaceView;
import android.os.IBinder;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.TaskStackBuilder;
import android.view.Gravity;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.WindowManager;

import org.igarape.copcast.BuildConfig;
import org.igarape.copcast.R;
import org.igarape.copcast.utils.HttpResponseCallback;
import org.igarape.copcast.utils.NetworkUtils;
import org.igarape.copcast.views.MainActivity;
import org.json.JSONException;
import org.json.JSONObject;
import org.webrtc.MediaStream;
import org.webrtc.VideoRendererGui;

import webrtcclient.PeerConnectionParameters;
import webrtcclient.WebRtcClient;


/**
 * Created by bruno on 11/19/14.
 */
public class StreamService extends Service implements SurfaceHolder.Callback, WebRtcClient.RtcListener {
    private WebRtcClient client;
    private WindowManager windowManager;
    private GLSurfaceView surfaceView;
    private static final String VIDEO_CODEC_VP9 = "VP9";
    private static final String AUDIO_CODEC_OPUS = "opus";
    private int mId = 5;



    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }


    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent.getExtras() != null  && intent.getExtras().getString("type")!= null){
            client.message(intent.getExtras().getString("from"), intent.getExtras().getString("type"), intent.getExtras().getString("payload"));
            return super.onStartCommand(intent, flags, startId);
        }

        Intent resultIntent = new Intent(this, MainActivity.class);
        Context context = getApplicationContext();
        NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(context)
                .setContentTitle(getString(R.string.notification_stream_title))
                .setContentText(getString(R.string.notification_stream_description))
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
        surfaceView = new GLSurfaceView(this);
        WindowManager.LayoutParams layoutParams = new WindowManager.LayoutParams(
                1, 1,
                WindowManager.LayoutParams.TYPE_SYSTEM_OVERLAY,
                WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH,
                PixelFormat.TRANSLUCENT
        );
        layoutParams.gravity = Gravity.LEFT | Gravity.TOP;
        windowManager.addView(surfaceView, layoutParams);
        VideoRendererGui.setView(surfaceView, new Runnable() {
            @Override
            public void run() {
                init();
            }


        });
        surfaceView.getHolder().addCallback(this);

        return super.onStartCommand(intent, flags, startId);
    }

    private void init() {
//        Point displaySize = new Point();
//        getWindowManager().getDefaultDisplay().getSize(displaySize);
//        PeerConnectionParameters params = new PeerConnectionParameters(
//                true, false, displaySize.x, displaySize.y, 30, 1, VIDEO_CODEC_VP9, true, 1, AUDIO_CODEC_OPUS, true);
//
//        client = new WebRtcClient(this, mSocketAddress, params, VideoRendererGui.getEGLContext());


    }

    @Override
    public void surfaceCreated(SurfaceHolder surfaceHolder) {
        PeerConnectionParameters params = new PeerConnectionParameters(
                true, false, 320 , 240, 15, 1, VIDEO_CODEC_VP9, true, 1, AUDIO_CODEC_OPUS, true);

        client = new WebRtcClient(getApplicationContext(), this, BuildConfig.serverUrl, params, VideoRendererGui.getEGLContext());


    }

    @Override
    public void surfaceChanged(SurfaceHolder surfaceHolder, int i, int i1, int i2) {

    }

    @Override
    public void surfaceDestroyed(SurfaceHolder surfaceHolder) {
        client.onDestroy();
    }

    @Override
    public void onCallReady(String callId) {
        try {
            client.sendMessage(callId, "init", null);
            client.start(callId, "android_name");
        } catch (JSONException e) {
            e.printStackTrace();
        }
        /**
         * Here we`ll tell node(server) that user is streaming
         */
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        client.onDestroy();

        NotificationManager mNotificationManager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        mNotificationManager.cancel(mId);
    }

    @Override
    public void onStatusChanged(String newStatus) {

    }

    @Override
    public void onLocalStream(MediaStream localStream) {

    }

    @Override
    public void onAddRemoteStream(MediaStream remoteStream, int endPoint) {

    }

    @Override
    public void onRemoveRemoteStream(int endPoint) {

    }
}