package org.igarape.webrecorder;

import android.media.AudioRecord;
import android.media.MediaCodec;
import android.util.Log;

import java.nio.ByteBuffer;

/**
 * Created by martelli on 2/18/16.
 */
class AudioProducer extends Thread {

    private final static String TAG = AudioProducer.class.getCanonicalName();
    private AudioRecord audioRecord;
    private MediaCodec audioCodec;
    private byte[] buf = new byte[WebRecorder.BUFFER_SIZE];
    int bytes_read;
    int inputBufferId;
    ByteBuffer[] inputBuffers;
    long now;
    private boolean isRunning = false;

    public AudioProducer() {
        audioRecord = new AudioRecord(
                WebRecorder.HW_INPUT_SOURCE,
                WebRecorder.SAMPLE_RATE,
                WebRecorder.CHANNEL_FORMAT,
                WebRecorder.AUDIO_FORMAT,
                WebRecorder.WINDOW_SIZE);
        Log.d(TAG, "created");
    }

    public void setAudioCodec(MediaCodec audioCodec) {
        this.audioCodec = audioCodec;
    }

    @Override
    public void run() {
        Log.d(TAG, "running");
        isRunning = true;

        if (audioCodec == null) {
            Log.e(TAG, "Audio codec not defined. Thread aborting.");
            return;
        }

        audioRecord.startRecording();

        while (isRunning) {
            bytes_read = audioRecord.read(buf, 0, buf.length);
            inputBufferId = audioCodec.dequeueInputBuffer(500000);
            inputBuffers = audioCodec.getInputBuffers();
            now = System.nanoTime()/1000;

            if (inputBufferId >= 0) {
                inputBuffers[inputBufferId].put(buf);
                audioCodec.queueInputBuffer(inputBufferId, 0, bytes_read, now, 0);
            }
        }

        audioRecord.release();
        Log.d(TAG, "finished");
    }

    public void end() {
        isRunning = false;
        Log.d(TAG, "end");
    }
}