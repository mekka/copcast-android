package org.igarape.webrecorder;

import android.media.MediaCodec;
import android.media.MediaFormat;
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
    private Mp4Muxer muxerThread;

    public void setCodec(MediaCodec videoCodec) {
        this.videoCodec = videoCodec;
    }

    public void setMuxer(Mp4Muxer muxerThread) throws WebRecorderException {
        if (muxerThread == null)
            throw new WebRecorderException("Muxer thread not initialialized");
        this.muxerThread = muxerThread;
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

                buf.position(bi.offset);
                buf.limit(bi.offset + bi.size);

                if ((bi.flags & MediaCodec.BUFFER_FLAG_CODEC_CONFIG) == 0) {
                    if (muxerThread != null)
                        muxerThread.push(MediaType.VIDEO_FRAME, buf, bi);
                }

                videoCodec.releaseOutputBuffer(outputBufferId, false);

            } else if (outputBufferId == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {

                int w = videoCodec.getOutputFormat().getInteger(MediaFormat.KEY_WIDTH);
                int h = videoCodec.getOutputFormat().getInteger(MediaFormat.KEY_HEIGHT);
                Log.e(TAG, "Live resolution: -->> "+w+" / "+h);

                if (muxerThread != null) {
                    try {
                        muxerThread.setVideoFormat(videoCodec.getOutputFormat());
                    } catch (WebRecorderException e) {
                        Log.e(TAG, "Could not set video format on muxer", e);
                    }
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