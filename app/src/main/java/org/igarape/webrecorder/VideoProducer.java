package org.igarape.webrecorder;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.YuvImage;
import android.hardware.Camera;
import android.media.CamcorderProfile;
import android.media.MediaCodec;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Base64;
import android.util.Log;
import android.view.SurfaceHolder;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Created by martelli on 2/18/16.
 */
class VideoProducer implements Camera.PreviewCallback {

    private final static String TAG = VideoProducer.class.getCanonicalName();
    private final int ONE_SECOND_IN_US = 1000000;
    Camera camera;
    MediaCodec.BufferInfo bi;
    MediaCodec.BufferInfo lbi;
    ByteBuffer[] inputBuffers;
    ByteBuffer[] liveInputBuffers;
    long lastCapture = -1;
    long lastLiveCapture = -1;
    private MediaCodec videoCodec;
    private MediaCodec liveVideoCodec;
    private int videoFrameRate = -1;
    private int liveVideoFrameRate = -1;
    private int videoWidth;
    private int videoHeight;
    private boolean isStreaming = false;
    private int liveVideoWidth;
    private int liveVideoHeight;

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

    public void setLiveVideoCodec(MediaCodec liveVideoCodec, int liveVideoWidth, int liveVideoHeight) {
        this.liveVideoCodec = liveVideoCodec;
        this.liveVideoWidth = liveVideoWidth;
        this.liveVideoHeight = liveVideoHeight;
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

    public void consumeFrame(byte[] data, long now) {

        bi = new MediaCodec.BufferInfo();
        inputBuffers = videoCodec.getInputBuffers();

        // simple frame rate control
        if (now <= (lastCapture + ONE_SECOND_IN_US / videoFrameRate))
            return;
        lastCapture = now;

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
        if (now < (lastLiveCapture+ ONE_SECOND_IN_US / liveVideoFrameRate))
            return;
        lastLiveCapture = now;

        byte[] d = data;

        if (videoWidth != liveVideoWidth) {
            d = shrink(data, videoHeight, videoWidth, 4);
        }

        Log.e(TAG, "Params:"+videoHeight+"x"+videoWidth+" / "+liveVideoHeight+"x"+liveVideoWidth);

        int liveInputBufferId = liveVideoCodec.dequeueInputBuffer(500000);
        if (liveInputBufferId >= 0) {
            liveInputBuffers[liveInputBufferId].put(d);
            liveVideoCodec.queueInputBuffer(liveInputBufferId, 0, d.length, now, 0);
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

    public static byte[] shrink(byte[] in, int w, int h, int scale) {

        byte[] out = new byte[in.length/scale/scale];

        for(int row=0; row<h/scale; row++) {
            for(int col=0; col<w/scale; col++) {

                int Y = 0;

                for(int s1=0; s1<scale; s1++) {
                    for(int s2=0; s2<scale; s2++) {
                        Y = (int) in[(row * scale + s1)*w + (col*scale) + s2] & 0xff;
                    }
                }
                out[row*w/scale + col] = (byte) Math.round(Y);
            }
        }

        int color_w = w/2;
        int color_h = h/2;

        for(int row=0; row<color_h/scale; row++) {
            for(int col=0; col<color_w/scale; col++) {

                int U = 0;
                int V = 0;

                for(int s1=0; s1<scale; s1++) {
                    for(int s2=0; s2<scale; s2++) {
                        V += (int) in[h * w + ((row * scale + s1) * w + col*scale*2) + (2*s2)] & 0xff;
                        U += (int) in[h * w + ((row * scale + s1) * w + col*scale*2) + (2*s2)+1] & 0xff;
                    }
                }
                out[(h*w/scale/scale) + (row*color_w/scale + col)*2] = (byte) Math.round(V/scale/scale);
                out[(h*w/scale/scale) + (row*color_w/scale + col)*2+1] = (byte) Math.round(U/scale/scale);
            }
        }


        // from now on, we are just padding the video to obtain a resolution that is
        // accepted by the encoder.
        byte[] padded = new byte[out.length/180*240];
        int nh = h/scale;
        int nw = w/scale;

        for(int row=0; row<nh; row++) {
            for(int col=0; col<30; col++)
                padded[row*240+col] = (byte) 0 & 0xFF;
            for(int col=210; col<240; col++)
                padded[row*240+col] = (byte) 0 & 0xFF;

            for(int col=30; col<210; col++)
                padded[row*240+col] = out[row*nw+col-30];
        }

        for(int row=0; row<nh/2; row++) {
            for(int col=0; col<15; col++) {
                padded[nh*(nw+60) + (row * 120 + col)*2] = (byte) (128 & 0xFF);
                padded[nh*(nw+60) + (row * 120 + col)*2 +1] = (byte) (128 & 0xFF);
            }
            for(int col=105; col<120; col++) {
                padded[nh*(nw+60) + (row * 120 + col)*2] = (byte) (128 & 0xFF);
                padded[nh*(nw+60) + (row * 120 + col)*2 + 1] = (byte) (128 & 0xFF);
            }
            for(int col=15; col<105; col++) {
                padded[nh*(nw+60) + (row * 120 + col)*2] = out[nw*nh + (row*nw/2+(col-15))*2];
                padded[nh*(nw+60) + (row * 120 + col)*2+1] = out[nw*nh + (row*nw/2+(col-15))*2+1];
            }
        }
        return padded;
    }
}