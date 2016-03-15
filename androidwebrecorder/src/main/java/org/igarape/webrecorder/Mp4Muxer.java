package org.igarape.webrecorder;

import android.media.MediaCodec;
import android.media.MediaFormat;
import android.media.MediaMuxer;
import android.util.Log;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

/**
 * Created by martelli on 2/18/16.
 */
class Mp4Muxer extends Thread {

    private final String TAG = Mp4Muxer.class.getCanonicalName();
    private MediaMuxer mediaMuxer;
    private int audioTrackIndex;
    private int videoTrackIndex;
    private boolean isRunning = false;
    private MediaFormat audioFormat;
    private MediaFormat videoFormat;
    private ArrayBlockingQueue<MediaFrame> queue;
    private Semaphore muxerLock = new Semaphore(1);
    private String outputPath;

    public Mp4Muxer(String outputPath) throws WebRecorderException {
        this.outputPath = outputPath;
        queue = new ArrayBlockingQueue(1000);
        try {
            muxerLock.acquire();
        } catch (InterruptedException e) {
            Log.e(TAG, "Error acquiring semaphore", e);
            throw new WebRecorderException(e);
        }
        Log.d(TAG, "created");
    }

    public void setAudioFormat(MediaFormat audioFormat) throws WebRecorderException {
        if (this.audioFormat == null) {
            this.audioFormat = audioFormat;
            initMuxer();
        }
    }

    public void setVideoFormat(MediaFormat videoFormat) throws WebRecorderException {
        if (this.videoFormat == null) {
            this.videoFormat = videoFormat;
            initMuxer();
        }
    }

    public void push(MediaType mediaType, ByteBuffer buffer, MediaCodec.BufferInfo bufferInfo) {
        queue.add(new MediaFrame(buffer, bufferInfo, mediaType));
    }

    private boolean initMuxer() throws WebRecorderException {
        if (audioFormat != null && videoFormat != null && mediaMuxer == null) {
            try {
                mediaMuxer = new MediaMuxer(outputPath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4);
            } catch (IOException e) {
                throw new WebRecorderException(e);
            }
        } else
            return false;

        Log.d(TAG, "initializing muxer");
        videoTrackIndex = mediaMuxer.addTrack(videoFormat);
        audioTrackIndex = mediaMuxer.addTrack(audioFormat);
        mediaMuxer.start();
        Log.d(TAG, "permits before: " + muxerLock.availablePermits());
        muxerLock.release();
        Log.d(TAG, "permits after: " + muxerLock.availablePermits());
        return true;
    }

    @Override
    public void run() {
        Log.d(TAG, "Thread started.");
        isRunning = true;
        MediaFrame frame = new MediaFrame(null, null, null);
        int trackIndex;

        try {
            muxerLock.acquire();
        } catch (InterruptedException e) {
            Log.e(TAG, "Failed to acquire semaphore on MuxerThread run", e);
            return;
        }
        muxerLock.release();

        Log.d(TAG, "Loop started.");

        while(isRunning || frame != null) { // we test last frame to make sure we consume all available frames before leaving
            try {
                frame = queue.poll(100, TimeUnit.MILLISECONDS);
//                Log.d(TAG, "queue size: "+queue.size());
                if (frame != null) {
                    trackIndex = frame.getMediaType() == MediaType.AUDIO_FRAME ? audioTrackIndex : videoTrackIndex;
                    mediaMuxer.writeSampleData(trackIndex, frame.getBuffer(), frame.getBufferInfo());
                }
            } catch (InterruptedException e) {
                Log.e(TAG, "Error writing data to muxer", e);
            }
        }
        Log.d(TAG, "Loop finished.");
        mediaMuxer.release();
        mediaMuxer = null;
        Log.d(TAG, "Thread finished.");
    }

    public void end() {
        Log.d(TAG, "Stop requested.");
        isRunning = false;
        Log.d(TAG, "Waiting for loop to finish.");
    }
}
