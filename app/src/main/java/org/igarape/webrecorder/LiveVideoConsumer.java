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
    private int outputBufferId;
    private ByteBuffer buf;
    private ByteBuffer[] outputBuffers;
    private MediaCodec.BufferInfo bi;
    private MediaCodec liveVideoCodec;
    private boolean isRunning = false;
    private final Socket ws;
    private byte[] sps;

    LiveVideoConsumer(Socket ws) {
        this.ws = ws;
    }

    public void setLiveVideoCodec(MediaCodec videoCodec) {
        this.liveVideoCodec = videoCodec;
    }

    @Override
    public void run() {
        Log.d(TAG, "Thread started.");
        isRunning = true;
        outputBuffers = liveVideoCodec.getOutputBuffers();
        int counter=0;
        Log.d(TAG, "Loop started.");
        while(isRunning) {
            counter++;
            bi = new MediaCodec.BufferInfo();
            outputBufferId = liveVideoCodec.dequeueOutputBuffer(bi, 500000);

            if (outputBufferId >= 0) {

                Log.v(TAG, "transmitting frame");
                buf = outputBuffers[outputBufferId];
                buf.position(bi.offset);
                buf.limit(bi.offset + bi.size);

                if (sps == null) {
                    sps = new byte[bi.size];
                    buf.get(sps, 0, bi.size);
                    buf.position(bi.offset);
                }

                byte[] bpack = new byte[bi.size];
                buf.get(bpack, 0, bi.size);

                if (counter % 5 == 0)
                    ws.emit("frame", sps);

                ws.emit("frame", bpack);
                liveVideoCodec.releaseOutputBuffer(outputBufferId, false);
            } else {
                Log.v(TAG, "Empty codec buffer");
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
