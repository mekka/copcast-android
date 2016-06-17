package org.igarape.webrecorder;

import android.hardware.Camera;
import android.media.MediaCodec;
import android.util.Log;
import android.view.SurfaceHolder;

import org.igarape.copcast.utils.Globals;
import org.igarape.webrecorder.enums.Orientation;

import java.io.IOException;
import java.nio.ByteBuffer;

/**
 * Created by martelli on 2/18/16.
 */
class VideoProducer implements Camera.PreviewCallback {

    private final static String TAG = VideoProducer.class.getCanonicalName();
    Camera camera;
    MediaCodec.BufferInfo bi;
    ByteBuffer[] inputBuffers;
    long lastCapture = 0, tmpCapture;
    private MediaCodec videoCodec;
    private int videoFrameRate = -1;
    private int videoWidth;
    private int videoHeight;
    private boolean isRunning = false;

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

    public void setVideoFrameRate(int videoFrameRate) {
        this.videoFrameRate = videoFrameRate;
    }

    @Override
    public void onPreviewFrame(byte[] data, android.hardware.Camera camera) {
        long now = System.nanoTime()/1000;

        bi = new MediaCodec.BufferInfo();
        inputBuffers = videoCodec.getInputBuffers();

        // simple frame rate control
        tmpCapture = now * videoFrameRate / 1000000;
        if (tmpCapture <= lastCapture)
            return;
        lastCapture = tmpCapture;

        byte[] new_data = new byte[data.length];

        int inputBufferId = videoCodec.dequeueInputBuffer(500000);
        if (inputBufferId >= 0) {

            if (Globals.orientation == Orientation.TOP)
                transpose(data, new_data, videoHeight, videoWidth);
            else if (Globals.orientation == Orientation.BOTTOM)
                transpose_bottom(data, new_data, videoHeight, videoWidth);
            else if (Globals.orientation == Orientation.RIGHT)
                transpose_flip_vert(data, new_data, videoWidth, videoHeight);
            else
                nv21tovn12(data, new_data, videoHeight, videoWidth);

            inputBuffers[inputBufferId].put(new_data);
            videoCodec.queueInputBuffer(inputBufferId, 0, data.length, now, 0);
        }
    }

    public static void nv21tovn12(byte[] in, byte[] out, int w, int h) {

        for(int i=0; i<w*h; i++) {
            out[i] = in[i];
        }

        for(int i=0; i<w*h/4; i++) {
            out[w*h + 2*i] = in[w*h + 2*i + 1];
            out[w*h + 2*i+ 1] = in[w*h + 2*i];
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

    public static void transpose_bottom(byte[] in, byte[] out, int w, int h) {

        for(int x2=0; x2<w; x2++) {
            int x = w-1-x2;
            for(int y=0; y<h; y++) {
                int y2 = h-1-y;
                out[(x+1)*h-1-y] = in[y2*w+x2];
            }
        }

        for(int x2=0; x2<w/2; x2++) {
            int x = w/2-1-x2;
            for(int y=0; y<h/2; y++) {
                int y2 = h/2-1-y;
                out[w*h+(((x+1)*(h/2))-1-y)*2+1] = in[w*h+(y2*w/2+x2)*2];
                out[w*h+(((x+1)*(h/2))-1-y)*2] = in[w*h+(y2*w/2+x2)*2+1];
            }
        }
    }

    public static void transpose_flip_vert(byte[] in, byte[] out, int w, int h) {

        for (int y=0; y<h; y++) {
            for(int x=0; x<w; x++) {
                out[y*w+x] = in[(h-1-y)*w+(w-1-x)];
            }
        }

        for (int y=0; y<h/2; y++) {
            for(int x=0; x<w/2; x++) {
                out[w*h+(y*w/2+(w/2-1-x))*2+1] = in[w*h+((h/2-1-y)*w/2+x)*2];
                out[w*h+(y*w/2+(w/2-1-x))*2] = in[w*h+((h/2-1-y)*w/2+x)*2+1];
            }
        }
    }
}