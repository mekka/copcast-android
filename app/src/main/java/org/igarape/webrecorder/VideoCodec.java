package org.igarape.webrecorder;

import android.annotation.TargetApi;
import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaCodecList;
import android.media.MediaFormat;
import android.os.Build;
import android.util.Log;

import org.igarape.copcast.utils.Globals;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

/**
 * Created by martelli on 2/18/16.
 */
class VideoCodec {

    private final String TAG = VideoCodec.class.getCanonicalName();
    private MediaCodec videoCodec;
    private boolean isRunning = false;

    @TargetApi(21)
    private String getCodecName(){
        MediaCodecInfo[] list = new MediaCodecList(MediaCodecList.REGULAR_CODECS).getCodecInfos();
        for (MediaCodecInfo info: list){
            List<String> types = Arrays.asList(info.getSupportedTypes());
            if (types.contains(MediaFormat.MIMETYPE_VIDEO_AVC) && info.isEncoder()){
                return info.getName();
            }
        }
        return null;
    }
    @TargetApi(18)
    private String getCodecNameDeprecated(){
        int counter = MediaCodecList.getCodecCount();
        for(int i = 0; i < counter; i++){
            MediaCodecInfo info = MediaCodecList.getCodecInfoAt(i);
            List<String> types = Arrays.asList(info.getSupportedTypes());
            if (types.contains(MediaFormat.MIMETYPE_VIDEO_AVC) && info.isEncoder()){
                return info.getName();
            }

        }
        return null;
    }
    public VideoCodec(int videoWidth, int videoHeight, int videoBitRate, int videoFrameRate, int iFrameInterval) throws WebRecorderException {


//            videoCodec = MediaCodec.createByCodecName("OMX.qcom.video.encoder.avc");
//            encoder = MediaCodec.createByCodecName("OMX.qcom.video.encoder.avc");

        // codec motorola razor
        //encoder = MediaCodec.createByCodecName("OMX.MTK.VIDEO.ENCODER.AVC");
        MediaFormat mediaFormat = MediaFormat.createVideoFormat(MediaFormat.MIMETYPE_VIDEO_AVC, videoWidth, videoHeight);

        mediaFormat.setInteger(MediaFormat.KEY_BIT_RATE, videoBitRate);
        mediaFormat.setInteger(MediaFormat.KEY_FRAME_RATE, videoFrameRate);
        mediaFormat.setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420SemiPlanar);
        mediaFormat.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, iFrameInterval);


        try {
            String name = Globals.getCodecName();
            if (name == null) {
                if (Build.VERSION.SDK_INT >= 21) {
                    name = getCodecName();
                } else {
                    name = getCodecNameDeprecated();
                }
                Globals.setCodecName(name);
            }
            videoCodec = MediaCodec.createByCodecName(name);
        } catch (IOException e) {
            throw new WebRecorderException(e);
        }
        videoCodec.configure(mediaFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
        Log.d(TAG, "Created.");
        Log.d(TAG, "Codec dimensions: "+videoWidth+" / "+videoHeight);
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
