package org.igarape.copcast.service;

import android.app.Notification;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.PixelFormat;
import android.hardware.Camera;
import android.os.IBinder;
import android.util.Log;
import android.view.Gravity;
import android.view.SurfaceHolder;
import android.view.WindowManager;

import net.majorkernelpanic.streaming.Session;
import net.majorkernelpanic.streaming.SessionBuilder;
import net.majorkernelpanic.streaming.audio.AudioQuality;
import net.majorkernelpanic.streaming.gl.SurfaceView;
import net.majorkernelpanic.streaming.rtsp.RtspClient;
import net.majorkernelpanic.streaming.video.VideoQuality;

import org.igarape.copcast.R;
import org.igarape.copcast.utils.Globals;
import org.igarape.copcast.utils.HttpResponseCallback;
import org.igarape.copcast.utils.NetworkUtils;
import org.igarape.copcast.utils.VideoUtils;
import org.json.JSONObject;


/**
 * Created by bruno on 11/19/14.
 */
public class StreamService extends Service implements RtspClient.Callback, Session.Callback, SurfaceHolder.Callback {
    public static String TAG = StreamService.class.getName();
    private static Session mSession;
    private static RtspClient mClient;
    private static boolean IsStreaming = false;
    private SurfaceView mSurfaceView;
    private WindowManager mWindowManager;
    private int ServiceID = 5;
    private SurfaceHolder mSurfaceHolder;
    private boolean bitrateStarted;


    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        super.onStartCommand(intent, flags, startId);
        if (IsStreaming) {
            return START_STICKY;
        }

        mWindowManager = (WindowManager) this.getSystemService(Context.WINDOW_SERVICE);

        mSurfaceView = new SurfaceView(this, null);
        WindowManager.LayoutParams layoutParams = new WindowManager.LayoutParams(
                1, 1,
                WindowManager.LayoutParams.TYPE_SYSTEM_OVERLAY,
                WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH,
                PixelFormat.TRANSLUCENT
        );
        layoutParams.gravity = Gravity.LEFT | Gravity.TOP;

        mWindowManager.addView(mSurfaceView, layoutParams);
        mSurfaceHolder = mSurfaceView.getHolder();
        mSurfaceHolder.addCallback(this);

        Notification notification = new Notification.Builder(this)
                .setContentTitle("SmartPolicing Streaming")
                .setContentText("")
                .setSmallIcon(R.drawable.ic_launcher)
                .build();

        startForeground(ServiceID, notification);

        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mClient.stopStream();
        mClient.release();
        mSession.release();
        IsStreaming = false;

        /**
         * Here we`ll tell node(server) that user stopped streaming
         */
        NetworkUtils.delete(this.getApplicationContext(), "/streams", new HttpResponseCallback(){

            @Override
            public void unauthorized() {

            }

            @Override
            public void failure(int statusCode) {

            }

            @Override
            public void noConnection() {

            }

            @Override
            public void badConnection() {

            }

            @Override
            public void badRequest() {

            }

            @Override
            public void badResponse() {

            }
        });
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {

    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        this.bitrateStarted = false;
        SessionBuilder builder = SessionBuilder.getInstance()
                .setCamera(Camera.CameraInfo.CAMERA_FACING_BACK)
                .setContext(getApplicationContext())
                .setAudioEncoder(SessionBuilder.AUDIO_AAC)
                .setAudioQuality(new AudioQuality(8000, 16000))
                .setVideoEncoder(SessionBuilder.VIDEO_H264)
                .setSurfaceView(mSurfaceView)
                .setPreviewOrientation(VideoUtils.DEGREES)
                .setCallback(this);

        if (android.os.Build.VERSION.SDK_INT <= 16) {
            builder = builder.setVideoQuality(new VideoQuality(176, 144, 15, 500000));
        }
        // Configures the SessionBuilder
        mSession = builder.setCallback(this)
                .build();


        // Configures the RTSP client
        mClient = new RtspClient();

        mClient.setCredentials(Globals.getStreamingUser(), Globals.getStreamingPassword());
        mClient.setServerAddress(Globals.getServerIpAddress(), Globals.getStreamingPort());
        mClient.setStreamPath(Globals.getStreamingPath());


        mClient.setSession(mSession);
        mClient.setCallback(this);

        mSession.startPreview();
        mClient.startStream();
        IsStreaming = true;


    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {

    }

    @Override
    public void onBitrateUpdate(long bitrate) {
        if (bitrate > 0 && !bitrateStarted) {
            bitrateStarted = true;
            /**
             * Here we`ll tell node(server) that user is streaming
             */
            NetworkUtils.post(this.getApplicationContext(), "/streams", (JSONObject) null, new HttpResponseCallback() {
                @Override
                public void unauthorized() {

                }

                @Override
                public void failure(int statusCode) {

                }

                @Override
                public void noConnection() {

                }

                @Override
                public void badConnection() {

                }

                @Override
                public void badRequest() {

                }

                @Override
                public void badResponse() {

                }
            });
        }
        Log.i(TAG, bitrate / 1000 + " kbps");

    }

    @Override
    public void onSessionError(int reason, int streamType, Exception e) {
        Log.e(TAG, "On Session Error", e);
    }

    @Override
    public void onPreviewStarted() {

    }

    @Override
    public void onSessionConfigured() {

    }

    @Override
    public void onSessionStarted() {

    }

    @Override
    public void onSessionStopped() {

    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onRtspUpdate(int message, Exception e) {
        Log.e(TAG, "RTSP update", e);
    }
}