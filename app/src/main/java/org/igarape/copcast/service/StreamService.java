package org.igarape.copcast.service;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.PixelFormat;
import android.media.CamcorderProfile;
import android.opengl.GLSurfaceView;
import android.os.IBinder;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.TaskStackBuilder;
import android.util.Log;
import android.view.Gravity;
import android.view.SurfaceHolder;
import android.view.WindowManager;

import org.igarape.copcast.R;
import org.igarape.copcast.utils.Globals;
import org.igarape.copcast.views.MainActivity;
import org.webrtc.MediaStream;
import org.webrtc.RendererCommon;
import org.webrtc.VideoRenderer;
import org.webrtc.VideoRendererGui;

import webrtcclient.PeerConnectionParameters;
import webrtcclient.WebRtcClient;


/**
 * Created by bruno on 11/19/14.
 */
public class StreamService extends Service implements SurfaceHolder.Callback, WebRtcClient.RtcListener {
    private WebRtcClient client;
    private static final String VIDEO_CODEC_VP9 = "VP9";
    private static final String AUDIO_CODEC_OPUS = "opus";
    private int mId = 5;
    private VideoRenderer.Callbacks localRender;
    private static final int LOCAL_X_CONNECTING = 0;
    private static final int LOCAL_Y_CONNECTING = 0;
    private static final int LOCAL_WIDTH_CONNECTING = 100;
    private static final int LOCAL_HEIGHT_CONNECTING = 100;
    private RendererCommon.ScalingType scalingType = RendererCommon.ScalingType.SCALE_ASPECT_FILL;
    private WindowManager mWindowManager;
    private GLSurfaceView mSurfaceView;
    private static final String TAG = StreamService.class.getName();
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }


    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.i(TAG, "onStartCommand()");
        if (intent == null){
            stopSelf();
            return START_STICKY;
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
        mWindowManager = (WindowManager) this.getSystemService(Context.WINDOW_SERVICE);
        mSurfaceView = new GLSurfaceView(this);
        WindowManager.LayoutParams layoutParams = new WindowManager.LayoutParams(
                1, 1,
                WindowManager.LayoutParams.TYPE_SYSTEM_OVERLAY,
                WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH,
                PixelFormat.TRANSLUCENT
        );
        layoutParams.gravity = Gravity.LEFT | Gravity.TOP;
        mWindowManager.addView(mSurfaceView, layoutParams);
        VideoRendererGui.setView(mSurfaceView, new Runnable() {
            @Override
            public void run() {
                init();
            }


        });
        mSurfaceView.getHolder().addCallback(this);

        localRender = VideoRendererGui.create(
                LOCAL_X_CONNECTING, LOCAL_Y_CONNECTING,
                LOCAL_WIDTH_CONNECTING, LOCAL_HEIGHT_CONNECTING, scalingType, true);
        return START_STICKY;
    }


    private void init() {

        CamcorderProfile profile = CamcorderProfile.get(CamcorderProfile.QUALITY_LOW);

        //windowManager.getDefaultDisplay().getSize(displaySize);
        PeerConnectionParameters params = new PeerConnectionParameters(
                true, false, profile.videoFrameWidth, profile.videoFrameHeight, 15, 1, VIDEO_CODEC_VP9, true, 1, AUDIO_CODEC_OPUS, true);

        client = new WebRtcClient(this, Globals.getServerUrl(getApplicationContext()), params, VideoRendererGui.getEGLContext(), Globals.getAccessTokenStraight(getApplicationContext()));


    }

    @Override
    public void surfaceCreated(SurfaceHolder surfaceHolder) {

    }

    @Override
    public void surfaceChanged(SurfaceHolder surfaceHolder, int i, int i1, int i2) {

    }

    @Override
    public void surfaceDestroyed(SurfaceHolder surfaceHolder) {
        if (Globals.isToggling()){
            Globals.setToggling(false);
            Intent intentAux = new Intent(this, VideoRecorderService.class);
            intentAux.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startService(intentAux);
        }
    }


    @Override
    public void onDestroy() {
        if(client != null) {
            client.onDestroy();
            if (localRender != null) {
                VideoRendererGui.remove(localRender);
            }
            if (mSurfaceView != null) {
                mWindowManager.removeView(mSurfaceView);
            }

        }
        NotificationManager mNotificationManager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        mNotificationManager.cancel(mId);

        super.onDestroy();
    }

    @Override
    public void onCallReady(String callId) {
        client.start("android_test");
    }


    @Override
    public void onStatusChanged(String newStatus) {
        Log.w(TAG, "onStatusChanged(" + newStatus + ")");
    }

    @Override
    public void onLocalStream(MediaStream localStream) {
        localStream.videoTracks.get(0).addRenderer(new VideoRenderer(localRender));
        VideoRendererGui.update(localRender,
                LOCAL_X_CONNECTING, LOCAL_Y_CONNECTING,
                LOCAL_WIDTH_CONNECTING, LOCAL_HEIGHT_CONNECTING,
                scalingType, false);
    }
}