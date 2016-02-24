package org.igarape.webrecorder;

import android.media.MediaCodec;
import android.media.MediaFormat;
import android.util.Log;

import java.nio.ByteBuffer;

/**
 * Created by martelli on 2/18/16.
 */
class AudioConsumer extends Thread {

    private final String TAG = AudioConsumer.class.getCanonicalName();
    private MediaCodec audioCodec;
    private int outputBufferId;
    private ByteBuffer buf;
    private ByteBuffer[] outputBuffers;
    private MediaCodec.BufferInfo bi;
    private boolean isRunning;
    private Mp4Muxer muxerThread;

    public void setCodec(MediaCodec audioCodec) {
        this.audioCodec = audioCodec;
    }

    public void setMuxer(Mp4Muxer muxerThread) {
        this.muxerThread = muxerThread;
    }

    @Override
    public void run() {

        Log.d(TAG, "running");

        isRunning = true;

        outputBuffers = audioCodec.getOutputBuffers();
        int audioTrackIndex = -1;
        while(isRunning) {

            bi = new MediaCodec.BufferInfo();
            outputBufferId = audioCodec.dequeueOutputBuffer(bi, 100000);

            //Log.d(TAG, "audio frame");

            if (outputBufferId >= 0) {

                buf = outputBuffers[outputBufferId];

                buf.position(bi.offset);
                buf.limit(bi.offset + bi.size);
                if ((bi.flags & MediaCodec.BUFFER_FLAG_CODEC_CONFIG) == 0) {
                    muxerThread.push(MediaType.AUDIO_FRAME, buf, bi);
                }
                audioCodec.releaseOutputBuffer(outputBufferId, false);

            } else if (outputBufferId == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                MediaFormat audioFormat = audioCodec.getOutputFormat();
                audioFormat.setInteger(MediaFormat.KEY_SAMPLE_RATE, WebRecorder.SAMPLE_RATE);
                audioFormat.setInteger(MediaFormat.KEY_CHANNEL_COUNT, WebRecorder.NUM_CHANNELS);
                try {
                    muxerThread.setAudioFormat(audioFormat);
                } catch (WebRecorderException e) {
                    Log.e(TAG, "Could not set audio format on muxer", e);
                }
            }
        }
        Log.d(TAG, "finished");

    }

    public void end() {
        isRunning = false;
        Log.d(TAG, "end");

    }
}
