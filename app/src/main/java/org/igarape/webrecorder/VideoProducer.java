package org.igarape.webrecorder;

import android.hardware.Camera;
import android.media.MediaCodec;
import android.util.Log;
import android.view.SurfaceHolder;

import org.igarape.copcast.utils.Globals;
import org.igarape.webrecorder.enums.Orientation;

import java.io.IOException;
import java.nio.ByteBuffer;

import static org.igarape.webrecorder.libs.LibYUV.nv21tovn12;
import static org.igarape.webrecorder.libs.LibYUV.shrink;
import static org.igarape.webrecorder.libs.LibYUV.transpose;
import static org.igarape.webrecorder.libs.LibYUV.transpose_bottom;
import static org.igarape.webrecorder.libs.LibYUV.transpose_flip_vert;

/**
 * Created by martelli on 2/18/16.
 */
class VideoProducer implements Camera.PreviewCallback {

    private final static String TAG = VideoProducer.class.getCanonicalName();
    Camera camera;
    MediaCodec.BufferInfo bi;
    ByteBuffer[] inputBuffers, liveInputBuffers;
    long lastCapture = 0, tmpCapture;
    long lastLiveCapture = 0, liveTmpCapture;
    private MediaCodec videoCodec;
    private MediaCodec liveVideoCodec;
    private int videoFrameRate = -1;
    private int liveVideoFrameRate = -1;
    private int videoWidth;
    private int videoHeight;
    private boolean isRunning = false;
    private boolean isStreaming = false;

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
            if (videoWidth > videoHeight)
                p.setPreviewSize(videoWidth, videoHeight);
            else
                p.setPreviewSize(videoHeight, videoWidth);
            camera.setParameters(p);
            camera.setPreviewCallback(this);
            Log.d(TAG, "Camera prepared");

        } catch (Exception e) {
            throw new WebRecorderException(e);
        }
        Log.d(TAG, "Created.");

        Log.i(TAG, "Producer dimensions: "+videoWidth+" / "+videoHeight);
    }

    public void setStreaming(boolean streaming) {
        isStreaming = streaming;
    }

    public void start() {

        Log.d(TAG, "Start requested.");
        isRunning = true;

        if (videoCodec == null) {
            Log.e(TAG, "Video codec not defined. Thread aborting.");
            return;
        }

        if (videoFrameRate == -1) {
            Log.e(TAG, "Video frame rate not defined. Thread aborting.");
            return;
        }

        if (liveVideoFrameRate == -1) {
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
        isRunning = false;
        Log.d(TAG, "Stopped.");
    }

    public void setVideoCodec(MediaCodec videoCodec) {
        this.videoCodec = videoCodec;
    }

    public void setLiveVideoCodec(MediaCodec liveVideoCodec) {
        this.liveVideoCodec = liveVideoCodec;
    }

    public void setVideoFrameRate(int videoFrameRate) {
        this.videoFrameRate = videoFrameRate;
    }

    public void setLiveVideoFrameRate(int liveVideoFrameRate) {
        this.liveVideoFrameRate = liveVideoFrameRate;
    }

    @Override
    public void onPreviewFrame(byte[] data, android.hardware.Camera camera) {
        long now = System.nanoTime()/1000;
        tmpCapture = now * videoFrameRate / 1000000;

        // simple frame rate control.
        // IMPORTANTE: We assume to always have LIVE_FRAME_RATE < RECORDING_FRAME_RATE
        if (tmpCapture <= lastCapture)
            return;
        lastCapture = tmpCapture;

        byte[] new_data = new byte[data.length];

        if (Globals.orientation == Orientation.TOP)
            transpose(data, new_data, videoHeight, videoWidth);
        else if (Globals.orientation == Orientation.BOTTOM)
            transpose_bottom(data, new_data, videoHeight, videoWidth);
        else if (Globals.orientation == Orientation.RIGHT)
            transpose_flip_vert(data, new_data, videoWidth, videoHeight);
        else
            nv21tovn12(data, new_data, videoHeight, videoWidth);

        bi = new MediaCodec.BufferInfo();
        inputBuffers = videoCodec.getInputBuffers();
        int inputBufferId = videoCodec.dequeueInputBuffer(500000);
        if (inputBufferId >= 0) {
            inputBuffers[inputBufferId].put(new_data);
            videoCodec.queueInputBuffer(inputBufferId, 0, new_data.length, now, 0);
        }

        if (!isStreaming)
            return;

        liveTmpCapture = now * liveVideoFrameRate / 1000000;
        if (liveTmpCapture <= lastLiveCapture)
            return;
        lastLiveCapture = liveTmpCapture;

        byte[] reduced_data = shrink(new_data, videoWidth, videoHeight, 4);

        bi = new MediaCodec.BufferInfo();
        liveInputBuffers = liveVideoCodec.getInputBuffers();
        int liveInputBufferId = liveVideoCodec.dequeueInputBuffer(500000);
        if (liveInputBufferId >= 0) {
            liveInputBuffers[liveInputBufferId].put(reduced_data);
            liveVideoCodec.queueInputBuffer(liveInputBufferId, 0, reduced_data.length, now, 0);
        }
    }
}