package org.igarape.webrecorder;

import android.hardware.Camera;
import android.media.MediaCodec;
import android.util.Log;
import android.view.SurfaceHolder;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Created by martelli on 2/18/16.
 */
class VideoProducer implements Camera.PreviewCallback {

    private final static String TAG = VideoProducer.class.getCanonicalName();
    Camera camera;
    MediaCodec.BufferInfo bi;
    MediaCodec.BufferInfo lbi;
    ByteBuffer[] inputBuffers;
    ByteBuffer[] liveInputBuffers;
    long lastCapture = 0, tmpCapture;
    long lastLiveCapture = 0, liveTmpCapture;
    private MediaCodec videoCodec;
    private MediaCodec liveVideoCodec;
    private int videoFrameRate = -1;
    private int liveVideoFrameRate = -1;
    private int videoWidth;
    private int videoHeight;
    private boolean isStreaming = false;
    private int scale = 1;

    public VideoProducer(SurfaceHolder surfaceHolder, int videoWidth, int videoHeight) throws WebRecorderException {

        this.videoWidth = videoWidth;
        this.videoHeight = videoHeight;

        if (surfaceHolder == null) {
            Log.e(TAG, "SurfaceHolder not defined. Thread aborting.");
            return;
        }

        camera = Camera.open();
        try {
            camera.reconnect();
        } catch (IOException e) {
            throw new WebRecorderException(e);
        }

        camera.lock();

        try {
            camera.stopPreview();
            camera.setPreviewDisplay(surfaceHolder);
            android.hardware.Camera.Parameters p = camera.getParameters();
            p.setPreviewSize(videoWidth, videoHeight);
            p.set("orientation", "portrait");
            p.setRotation(90);
            camera.setDisplayOrientation(90);
            camera.setParameters(p);
            camera.setPreviewCallback(this);
            Log.d(TAG, "Camera prepared");

        } catch (Exception e) {
            throw new WebRecorderException(e);
        }
        Log.d(TAG, "Created.");
    }

    public void setStreaming(boolean streaming) {
        this.isStreaming = streaming;
    }

    public void start() {

        Log.d(TAG, "Start requested.");

        if (videoCodec == null) {
            Log.e(TAG, "Video codec not defined. Thread aborting.");
            return;
        }

        if (videoFrameRate == -1) {
            Log.e(TAG, "Video frame rate not defined. Thread aborting.");
            return;
        }

        camera.startPreview();
        Log.d(TAG, "Running.");
    }

    public void end() {
        Log.d(TAG, "Stop requested.");
        if (camera != null) {
            camera.stopPreview();
            camera.setPreviewCallback(null);
            camera.release();
        }
        camera = null;
        Log.d(TAG, "Stopped.");
    }

    public void setVideoCodec(MediaCodec videoCodec) {
        this.videoCodec = videoCodec;
    }

    public void setLiveVideoCodec(MediaCodec liveVideoCodec, int scale) {
        this.liveVideoCodec = liveVideoCodec;
        this.scale = scale;
    }

    public void setVideoFrameRate(int videoFrameRate) {
        this.videoFrameRate = videoFrameRate;

    }

    public void setLiveVideoFrameRate(int liveVideoFrameRate) {
        this.liveVideoFrameRate = liveVideoFrameRate;
    }

    @Override
    public void onPreviewFrame(byte[] data, android.hardware.Camera camera) {

        byte[] new_data = new byte[data.length];
        transpose(data, new_data, videoWidth, videoHeight);
        long now = System.nanoTime()/1000;

        consumeFrame(new_data, now);
        liveConsumeFrame(new_data, now);
    }

    public void consumeFrame (byte[] data, long now) {

        bi = new MediaCodec.BufferInfo();
        inputBuffers = videoCodec.getInputBuffers();

        // simple frame rate control
        tmpCapture = now * videoFrameRate / 1000000;
        if (tmpCapture <= lastCapture)
            return;
        lastCapture = tmpCapture;


        int inputBufferId = videoCodec.dequeueInputBuffer(500000);
        if (inputBufferId >= 0) {
            inputBuffers[inputBufferId].put(data);
            videoCodec.queueInputBuffer(inputBufferId, 0, data.length, now, 0);
        }
    }

    public void liveConsumeFrame(byte[] data, long now) {

        lbi = new MediaCodec.BufferInfo();
        liveInputBuffers = liveVideoCodec.getInputBuffers();

        // simple frame rate control
//        liveTmpCapture = now * liveVideoFrameRate / 1000000;
//        if (liveTmpCapture <= lastLiveCapture)
//            return;
//        lastLiveCapture = liveTmpCapture;
        Log.e(TAG, "frame live captured");

        int liveInputBufferId = liveVideoCodec.dequeueInputBuffer(500000);
        if (liveInputBufferId >= 0) {
            liveInputBuffers[liveInputBufferId].put(data);
            liveVideoCodec.queueInputBuffer(liveInputBufferId, 0, data.length, now, 0);
        }
    }

    public static void transpose(byte[] in, byte[] out, int w, int h) {

        for(int x=0; x<w; x++) {
            for(int y=0; y<h; y++) {
                out[(x+1)*h-1-y] = in[y*w+x];
            }
        }

        for(int x=0; x<w/2; x++) {
            for(int y=0; y<h/2; y++) {
                out[w*h+(((x+1)*(h/2))-1-y)*2+1] = in[w*h+(y*w/2+x)*2];
                out[w*h+(((x+1)*(h/2))-1-y)*2] = in[w*h+(y*w/2+x)*2+1];
            }
        }

    }
}