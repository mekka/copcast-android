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
    private boolean isStreaming = false;
    private byte[] sps;
    private final int fps;
    private final Socket ws;
    private Mp4Muxer muxerThread;

    LiveVideoConsumer(Socket ws, int fps) {
        this.fps = fps;
        this.ws = ws;
    }

    public void setLiveVideoCodec(MediaCodec videoCodec) {
        this.liveVideoCodec = videoCodec;
    }


    public void setStreaming(boolean isStreaming) {
        Log.d(TAG, "streaming set to "+isStreaming);
        this.isStreaming = isStreaming;
    }

    @Override
    public void run() {
        Log.d(TAG, "Thread started.");
        isRunning = true;
        int counter = 0;
        outputBuffers = liveVideoCodec.getOutputBuffers();

        Log.d(TAG, "Loop started.");
        while(isRunning) {
            counter++;
            bi = new MediaCodec.BufferInfo();
            outputBufferId = liveVideoCodec.dequeueOutputBuffer(bi, 100000);

            if (outputBufferId >= 0) {

                buf = outputBuffers[outputBufferId];
                buf.position(bi.offset);
                buf.limit(bi.offset + bi.size);

                byte[] bpack = new byte[bi.size];
                buf.get(bpack, 0, bi.size);

                if (!isStreaming) {
                    continue;
                }

//                if ((bi.flags & MediaCodec.BUFFER_FLAG_CODEC_CONFIG) == 0) {
//                    if (counter++ % fps == 0) {
//                        ws.emit("frame", sps);
////                        Log.d(TAG, "Sending SPS");
//                    }
//                } else {
//                        sps = new byte[bi.size];
//                        buf.get(sps, 0, bi.size);
//                        Log.d(TAG, "SPS Set: "+sps.length);
//                }
                ws.emit("frame", bpack);
                liveVideoCodec.releaseOutputBuffer(outputBufferId, false);
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
