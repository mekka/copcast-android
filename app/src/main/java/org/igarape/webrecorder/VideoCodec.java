package org.igarape.webrecorder;

import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaFormat;
import android.util.Log;

import java.io.IOException;

/**
 * Created by martelli on 2/18/16.
 */
class VideoCodec {

    private final String TAG = VideoCodec.class.getCanonicalName();
    private MediaCodec videoCodec;
    private boolean isRunning = false;

    public VideoCodec(int videoWidth, int videoHeight, int videoBitRate, int videoFrameRate, int iFrameInterval) throws WebRecorderException {

        try {
            videoCodec = MediaCodec.createByCodecName("OMX.qcom.video.encoder.avc");
        } catch (IOException e) {
            throw new WebRecorderException(e);
        }
//            videoCodec = MediaCodec.createByCodecName("OMX.qcom.video.encoder.avc");
//            encoder = MediaCodec.createByCodecName("OMX.qcom.video.encoder.avc");

        // codec motorola razor
        //encoder = MediaCodec.createByCodecName("OMX.MTK.VIDEO.ENCODER.AVC");
        MediaFormat mediaFormat = MediaFormat.createVideoFormat("video/avc", videoHeight, videoWidth);

        mediaFormat.setInteger(MediaFormat.KEY_BIT_RATE, videoBitRate);
        mediaFormat.setInteger(MediaFormat.KEY_FRAME_RATE, videoFrameRate);
        mediaFormat.setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420SemiPlanar);
        mediaFormat.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, iFrameInterval);
        videoCodec.configure(mediaFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
        Log.d(TAG, "Created.");
    }

    public MediaCodec getCodec() {
        return videoCodec;
    }

    public void start() {
        Log.d(TAG, "Starting.");
        isRunning = true;
        this.videoCodec.start();
        Log.d(TAG, "Running.");
    }

    public void end() {
        Log.d(TAG, "Stop requested.");
        videoCodec.stop();
        videoCodec.release();
        isRunning = false;
        Log.d(TAG, "Finished.");
    }
}
