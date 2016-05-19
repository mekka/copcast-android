package org.igarape.webrecorder;

import android.media.MediaCodec;
import android.util.Log;

import java.nio.ByteBuffer;

import io.socket.client.Socket;

/**
 * Created by martelli on 2/18/16.
 */
class LiveVideoConsumer extends Thread {

    private final String TAG = LiveVideoConsumer.class.getCanonicalName();
    private int frameIntervalCount = 0;
    private int counter = 0;
    private int outputBufferId;
    private ByteBuffer buf;
    private ByteBuffer[] outputBuffers;
    private MediaCodec.BufferInfo bi;
    private MediaCodec videoCodec;
    private boolean isRunning = false;
        private Socket ws;
    private byte[] sps;


    public void setCodec(MediaCodec videoCodec) {
        this.videoCodec = videoCodec;
    }

    public void setFrameIntervalCount(int frameIntervalCount) {
        this.frameIntervalCount = frameIntervalCount;
    }

    public void setSps(byte[] sps) {
        this.sps = sps;
    }

    public void setWebsocket(Socket ws) {
        this.ws = ws;
    }

    @Override
    public void run() {
        Log.d(TAG, "Thread started.");
        isRunning = true;
        outputBuffers = videoCodec.getOutputBuffers();
        long lastTenSecond, currentTenSecond=0;

        Log.d(TAG, "Loop started.");
        while(isRunning) {

            bi = new MediaCodec.BufferInfo();
            outputBufferId = videoCodec.dequeueOutputBuffer(bi, 100000);

            Log.e(TAG, "loop...");
            if (outputBufferId >= 0) {

                buf = outputBuffers[outputBufferId];

                buf.position(bi.offset);
                buf.limit(bi.offset + bi.size);

                if (ws != null) {

                    byte[] bpack = new byte[bi.size];
                    buf.get(bpack, 0, bi.size);
                    Log.d(TAG, "LIVE: "+bpack.length);

                    if (counter++ % frameIntervalCount == 0 && sps != null) {
                        ws.emit("frame", sps);
                    }
                    ws.emit("frame", bpack);
                }

                buf.position(bi.offset);
                buf.limit(bi.offset + bi.size);

                if ((bi.flags & MediaCodec.BUFFER_FLAG_CODEC_CONFIG) != 0) {
                    byte[] sps = new byte[bi.size];
                    buf.get(sps, 0, bi.size);
                    this.setSps(sps);
                    Log.d(TAG, "SPS Set: "+sps.length);
                }
                videoCodec.releaseOutputBuffer(outputBufferId, false);
            }
        }
        Log.d(TAG, "Loop and thread finished.");
    }

    public void end() {
        Log.d(TAG, "Stop requested.");
        isRunning = false;
        Log.d(TAG, "Waiting for loop to finish.");
    }

}