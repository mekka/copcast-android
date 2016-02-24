package org.igarape.webrecorder;

import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaFormat;
import android.util.Log;

import java.io.IOException;

/**
 * Created by martelli on 2/18/16.
 */
class AudioCodec {

    private final static String TAG = AudioCodec.class.getCanonicalName();
    private MediaCodec audioCodec;
    private boolean isRunning = false;

    public AudioCodec() throws WebRecorderException {

        MediaFormat audiofmt = MediaFormat.createAudioFormat(
                WebRecorder.AUDIO_MIME,
                WebRecorder.SAMPLE_RATE,
                WebRecorder.NUM_CHANNELS);
        audiofmt.setInteger(MediaFormat.KEY_MAX_INPUT_SIZE, 16384*20);
        audiofmt.setInteger(MediaFormat.KEY_AAC_PROFILE, MediaCodecInfo.CodecProfileLevel.AACObjectHE);
        audiofmt.setInteger(MediaFormat.KEY_BIT_RATE, WebRecorder.AUDIO_BIT_RATE);
        audiofmt.setInteger(MediaFormat.KEY_SAMPLE_RATE, WebRecorder.SAMPLE_RATE);
        audiofmt.setInteger(MediaFormat.KEY_CHANNEL_COUNT, WebRecorder.NUM_CHANNELS);

        try {
            audioCodec = MediaCodec.createEncoderByType(WebRecorder.AUDIO_MIME);
        } catch (IOException e) {
            throw new WebRecorderException(e);
        }
        audioCodec.configure(audiofmt, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
        Log.d(TAG, "created");
    }

    public MediaCodec getCodec() {
        return audioCodec;
    }

    public void start() {
        isRunning = true;
        audioCodec.start();
        Log.d(TAG, "running");

    }

    public void end() {
        audioCodec.stop();
        audioCodec.release();
        isRunning = false;
        Log.d(TAG, "finished");

    }
}
