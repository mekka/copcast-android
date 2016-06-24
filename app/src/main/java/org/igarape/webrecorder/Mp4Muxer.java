package org.igarape.webrecorder;

import android.media.MediaCodec;
import android.media.MediaFormat;
import android.media.MediaMuxer;
import android.util.Base64;
import android.util.Log;
import android.util.SparseArray;

import org.igarape.copcast.utils.FileUtils;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.text.SimpleDateFormat;
import java.util.Date;
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
    private boolean muxerStarted = false;
    private MediaFormat audioFormat;
    private MediaFormat videoFormat;
    private ArrayBlockingQueue<MediaFrame> queue;
    private Semaphore muxerLock = new Semaphore(1);
    private String outputDir;
    private static int FRAME_BUFFER_SIZE = 20;
    private SparseArray<MediaFrame> frames = new SparseArray<>(FRAME_BUFFER_SIZE);

    public Mp4Muxer(String outputDir) throws WebRecorderException {
        this.outputDir = outputDir;
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
        return initMuxer(false);
    }

    private boolean initMuxer(boolean forcedRestart) throws WebRecorderException {
        // if forcedRestart is false, initMuxer won't inicialize de MediaMuxer even if called
        // multiple times. This is desired because the videoConsumer can call 'setVideoFormat'
        // more than once, but we don't want to touch de MediaMuxer after it has been started.
        if (audioFormat != null && videoFormat != null && (mediaMuxer == null || forcedRestart)) {
            try {
                Date data = new Date();
                SimpleDateFormat dataFmt = new SimpleDateFormat(FileUtils.DATE_FORMAT);
                String b64name = Base64.encodeToString(dataFmt.format(data).getBytes(), Base64.NO_PADDING | Base64.NO_WRAP);
                String file_name = this.outputDir+"/"+b64name+".mp4";
                Log.d(TAG, "Starting file: "+file_name);
                mediaMuxer = new MediaMuxer(file_name, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4);
            } catch (IOException e) {
                throw new WebRecorderException(e);
            }
        } else
            return false;

        Log.d(TAG, "initializing muxer");
        videoTrackIndex = mediaMuxer.addTrack(videoFormat);
        audioTrackIndex = mediaMuxer.addTrack(audioFormat);
        mediaMuxer.start();
        muxerStarted = true;
        Log.d(TAG, "permits before: " + muxerLock.availablePermits());
        muxerLock.release();
        Log.d(TAG, "permits after: " + muxerLock.availablePermits());
        return true;
    }

    @Override
    public void run() {
        Log.d(TAG, "Thread started.");
        isRunning = true;
        MediaFrame frame;
        long videoInitialTS;
        int frameCounter = 0;
        long MINUTES_5 = 5*60*1000000;

        try {
            muxerLock.acquire();
        } catch (InterruptedException e) {
            Log.e(TAG, "Failed to acquire semaphore on MuxerThread run", e);
            return;
        }
        muxerLock.release();

        Log.d(TAG, "Loop started.");

        videoInitialTS = System.nanoTime()/1000;

        while(true) {
            try {
                frame = queue.poll(250, TimeUnit.MILLISECONDS);

                Log.v(TAG, "queue: "+queue.size());

                if (frame != null) {

                    frameCounter++;

                    if ((frame.getBufferInfo().presentationTimeUs - videoInitialTS)>MINUTES_5) {

                        if (videoInitialTS > -1) {
                            mediaMuxer.release();
                            initMuxer(true); //forced muxer will restart even if the muxer object is not null.
                        }
                        videoInitialTS = frame.getBufferInfo().presentationTimeUs;
                    }

                    // now we dump to file every 10 frames. We skip the flush on frame 10
                    // to allow the array to be always between 10 and 20 elements, thus
                    // resolving boundary issues.
                    if (frameCounter > 15 && frameCounter % 10 == 0) {
                        flush();
                    }

                    // we use a sparse array to automatically sort by timestamp, thus
                    // avoiding synchronization issues.
                    int delta = (int) (frame.getBufferInfo().presentationTimeUs - videoInitialTS);
                    frames.append(delta, frame);

                } else {
                    if (!isRunning)
                        break;
                }
            } catch (InterruptedException e) {
//                Log.e(TAG, "Error writing data to muxer", e);
//            } catch (WebRecorderException e) {
                Log.e(TAG, "Error starting new muxer", e);
            } catch (WebRecorderException e) {
                e.printStackTrace();
            }
        }
        Log.d(TAG, "Loop finished.");

        if (muxerStarted) {
            try {
                mediaMuxer.release();
            } catch (IllegalStateException e) {
                Log.e(TAG, "Error releasing muxer", e);
            }
        }
        mediaMuxer = null;
        Log.d(TAG, "Thread finished without lost frames.");
    }

    public void end() {
        Log.d(TAG, "Stop requested.");
        isRunning = false;
        muxerLock.release();
        Log.d(TAG, "Waiting for loop to finish.");
    }

    private void flush() {
        for(int i = 0; i< FRAME_BUFFER_SIZE/2; i++) {

            MediaFrame frame = frames.valueAt(i);

//            try {
//                Thread.sleep(5,0);
//            } catch (InterruptedException e) {
//                e.printStackTrace();
//            }

            int trackIndex = frame.getMediaType() == MediaType.AUDIO_FRAME ? audioTrackIndex : videoTrackIndex;
            mediaMuxer.writeSampleData(trackIndex, frame.getBuffer(), frame.getBufferInfo());
        }

        for(int i = 0; i< FRAME_BUFFER_SIZE/2; i++) {
            frames.removeAt(i);
        }
    }
}
