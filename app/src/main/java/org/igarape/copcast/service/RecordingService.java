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
import android.os.IBinder;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.TaskStackBuilder;
import android.text.format.DateFormat;
import android.util.Log;
import android.view.Gravity;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.WindowManager;

import org.igarape.copcast.R;
import org.igarape.copcast.utils.FileUtils;
import org.igarape.copcast.utils.Globals;
import org.igarape.copcast.utils.VideoUtils;
import org.igarape.copcast.views.MainActivity;

import java.io.File;
import java.util.Date;

public class RecordingService extends Service implements SurfaceHolder.Callback {
    public static String TAG = RecordingService.class.getName();
    private static boolean IsRecording = false;
    protected Camera mCamera;
    private MediaRecorder mMediaRecorder;
    private String mLastFileRecorded;
    public boolean surfaceCreated = false;
    protected WindowManager mWindowManager;
    protected SurfaceView mSurfaceView;
    protected SurfaceHolder mSurfaceHolder;
    private int mId = 1;

    @Override
    public void onDestroy() {

        if (IsRecording) {
            stopRecording();
        }

        mWindowManager.removeView(mSurfaceView);

        NotificationManager mNotificationManager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        mNotificationManager.cancel(mId);
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();


    Intent resultIntent = new Intent(this, MainActivity.class);
        NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(this)
                .setContentTitle(getString(R.string.notification_record_title))
                .setContentText(getString(R.string.notification_record_description))
                .setSmallIcon(R.drawable.ic_launcher);

        TaskStackBuilder stackBuilder = TaskStackBuilder.create(this);
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
        mSurfaceView = new SurfaceView(this);
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
        mSurfaceHolder.setFixedSize(1, 1);
    }

    @Override
    public void surfaceCreated(SurfaceHolder surfaceHolder) {
        surfaceCreated = true;
        startRecording();
    }

    @Override
    public void surfaceChanged(SurfaceHolder surfaceHolder, int i, int i2, int i3) {

    }

    @Override
    public void surfaceDestroyed(SurfaceHolder surfaceHolder) {

    }

    public void startRecording() {

        if (IsRecording) {
            return;
        }

        mCamera = Camera.open(0);

        mCamera.setDisplayOrientation(VideoUtils.DEGREES);


        mMediaRecorder = new MediaRecorder();

        mCamera.unlock();
        mMediaRecorder.setPreviewDisplay(mSurfaceHolder.getSurface());
        mMediaRecorder.setOrientationHint(VideoUtils.DEGREES);
        mMediaRecorder.setCamera(mCamera);
        mMediaRecorder.setAudioSource(MediaRecorder.AudioSource.CAMCORDER);
        mMediaRecorder.setVideoSource(MediaRecorder.VideoSource.CAMERA);
        mMediaRecorder.setProfile(CamcorderProfile.get(CamcorderProfile.QUALITY_LOW));
        mMediaRecorder.setVideoFrameRate(15);


        mLastFileRecorded = FileUtils.getPath(Globals.getUserLogin(this)) +
                DateFormat.format("yyyy-MM-dd_kk-mm-ss", new Date().getTime()) +
                ".mp4";

        mMediaRecorder.setOutputFile(mLastFileRecorded);

        mMediaRecorder.setMaxDuration(VideoUtils.MAX_DURATION_MS);
        mMediaRecorder.setMaxFileSize(VideoUtils.MAX_SIZE_BYTES);
        mMediaRecorder.setOnInfoListener(new MediaRecorder.OnInfoListener() {
            @Override
            public void onInfo(MediaRecorder mediaRecorder, int what, int extra) {
                if (what == MediaRecorder.MEDIA_RECORDER_INFO_MAX_DURATION_REACHED ||
                        what == MediaRecorder.MEDIA_RECORDER_INFO_MAX_FILESIZE_REACHED) {
                    stopRecording();
                    startRecording();
                }
            }
        });

        try {
            mMediaRecorder.prepare();
        } catch (Exception e) {
            mCamera.release();
            mCamera = null;
            Log.e(TAG, "Could not prepare media recorder", e);
            return;
        }
        try {
            mMediaRecorder.start();
        } catch (RuntimeException e) {
            mCamera.release();
            mCamera = null;
            Log.e(TAG, "Could not start camera", e);
            return;
        }
        IsRecording = true;

    }


    public void stopRecording() {
        if (!IsRecording) {
            return;
        }

        try {
            mMediaRecorder.stop();
        } catch (RuntimeException e) {
            if (mLastFileRecorded != null) {
                File lastFile = new File(mLastFileRecorded);
                lastFile.delete();
            }
        }

        mMediaRecorder.reset();
        mMediaRecorder.release();

        mCamera.lock();
        mCamera.release();
        IsRecording = false;
    }
}
