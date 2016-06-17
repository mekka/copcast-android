package org.igarape.webrecorder;

import android.media.MediaCodec;
import android.util.Log;

import java.nio.ByteBuffer;

/**
 * Created by martelli on 2/18/16.
 */
class VideoConsumer extends Thread {

    private final String TAG = VideoConsumer.class.getCanonicalName();
    private int outputBufferId;
    private ByteBuffer buf;
    private ByteBuffer[] outputBuffers;
    private MediaCodec.BufferInfo bi;
    private MediaCodec videoCodec;
    private boolean isRunning = false;
    private boolean isStreaming = false;
    private Mp4Muxer muxerThread;
    private WebsocketThread websocketThread;

    public void setCodec(MediaCodec videoCodec) {
        this.videoCodec = videoCodec;
    }

    public void setWebsocketThread(WebsocketThread websocketThread) throws WebRecorderException {
        this.websocketThread = websocketThread;
    }

    public void setMuxer(Mp4Muxer muxerThread) throws WebRecorderException {
        if (muxerThread == null)
            throw new WebRecorderException("Muxer thread not initialialized");
        this.muxerThread = muxerThread;
    }

    public void setStreaming(boolean isStreaming) {
        Log.d(TAG, "streaming set to "+isStreaming);
        this.isStreaming = isStreaming;
        websocketThread.setStreaming(isStreaming);
    }

    @Override
    public void run() {
        Log.d(TAG, "Thread started.");
        isRunning = true;
        outputBuffers = videoCodec.getOutputBuffers();

        Log.d(TAG, "Loop started.");
        while(isRunning) {

            bi = new MediaCodec.BufferInfo();
            outputBufferId = videoCodec.dequeueOutputBuffer(bi, 100000);

            if (outputBufferId >= 0) {

                buf = outputBuffers[outputBufferId];
                buf.position(bi.offset);
                buf.limit(bi.offset + bi.size);

                byte[] bpack = new byte[bi.size];
                buf.get(bpack, 0, bi.size);

                if ((bi.flags & MediaCodec.BUFFER_FLAG_CODEC_CONFIG) == 0) {
                    muxerThread.push(MediaType.VIDEO_FRAME, ByteBuffer.wrap(bpack), bi);
                }
                videoCodec.releaseOutputBuffer(outputBufferId, false);

            } else if (outputBufferId == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {

                try {
                    muxerThread.setVideoFormat(videoCodec.getOutputFormat());
                } catch (WebRecorderException e) {
                    Log.e(TAG, "Could not set video format on muxer", e);
                }
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
