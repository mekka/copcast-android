package org.igarape.webrecorder;

import android.media.AudioFormat;
import android.media.CamcorderProfile;
import android.media.MediaRecorder;
import android.util.Log;
import android.view.SurfaceHolder;

import org.igarape.copcast.promises.WebRecorderPromiseError;
import org.igarape.copcast.promises.Promise;

import io.socket.client.Socket;

/**
 * Created by martelli on 2/14/16.
 */

public class WebRecorder {

    private final String TAG = WebRecorder.class.getCanonicalName();

    public static final int CHANNEL_FORMAT = AudioFormat.CHANNEL_IN_MONO;
    public static final int NUM_CHANNELS = 1;
    public static final int AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT;
    public static final int HW_INPUT_SOURCE = MediaRecorder.AudioSource.CAMCORDER;
    public static final int BYTES_PER_SAMPLE = 2;
    public static final int AUDIO_BIT_RATE = 12000;
    public static final int SAMPLE_RATE = 22050;
    public static final int BUFFER_IN_MS = 200;
    public static final int WINDOW_IN_MS = 2000;
    public static final int BUFFER_SIZE = BUFFER_IN_MS*BYTES_PER_SAMPLE*SAMPLE_RATE/1000;
    public static final int WINDOW_SIZE = WINDOW_IN_MS*BYTES_PER_SAMPLE*SAMPLE_RATE/1000;
    public static final String AUDIO_MIME = "audio/mp4a-latm";

    public static final int DEFAULT_VIDEO_BITRATE = 120000;
    public static final int DEFAULT_VIDEO_FRAMERATE = 10;
    public static final int DEFAULT_KEY_I_FRAME_INTERVAL = 10;

    //    private AudioRecord audioRecord;
    private AudioCodec audioCodec;
    private VideoCodec videoCodec;
    private VideoCodec liveVideoCodec;
    private AudioProducer audioProducerThread;
    private VideoProducer videoProducer;
    private AudioConsumer audioConsumerThread;
    private VideoConsumer videoConsumerThread;
    private LiveVideoConsumer liveVideoConsumerThread;
    private Mp4Muxer muxerThread;

    private int videoHeight;
    private int videoWidth;
    public int videoBitRate;
    private int videoFrameRate;
    public int liveVideoBitRate;
    private int liveVideoFrameRate;
    private int liveVideoWidth;
    private int liveVideoHeight;
    private int iFrameInterval;
    private final String outputPath;
    private Socket websocket;
    private boolean isRunning;

    public static class Builder {
        private int videoWidth;
        private int videoHeight;
        private Integer videoBitrate;
        private Integer videoFramerate;
        private Integer liveVideoWidth;
        private Integer liveVideoHeight;
        private Integer liveVideoBitrate;
        private Integer liveVideoFramerate;
        private Integer videoIFrameInterval;
        private Socket websocket;
        private String outputPath;

        public
        Builder(String outputPath, final int videoWidth, final int videoHeight) {
            this.videoHeight = videoHeight;
            this.videoWidth = videoWidth;
            this.outputPath = outputPath;
        }

        public Builder(String outputPath, final int videoQuality) {
            CamcorderProfile profile = CamcorderProfile.get(videoQuality);
            this.videoHeight = profile.videoFrameHeight;
            this.videoWidth = profile.videoFrameWidth;
            this.outputPath = outputPath;
        }

        public Builder setLiveVideoQuality(int liveVideoQuality) {
            CamcorderProfile profile = CamcorderProfile.get(liveVideoQuality);
            this.liveVideoHeight = profile.videoFrameHeight;
            this.liveVideoWidth = profile.videoFrameWidth;
            return this;
        }

        public Builder setVideoBitRate(int video_bitrate) {
            this.videoBitrate = video_bitrate;
            return this;
        }

        public Builder setVideoFrameRate(int video_framerate) {
            this.videoFramerate = video_framerate;
            return this;
        }

        public Builder setLiveVideoBitRate(int live_video_bitrate) {
            this.liveVideoBitrate = live_video_bitrate;
            return this;
        }

        public Builder setLiveVideoFrameRate(int live_video_framerate) {
            this.liveVideoFramerate = live_video_framerate;
            return this;
        }

        public Builder setVideoIFrameInterval(int i_frame_interval) {
            this.videoIFrameInterval = i_frame_interval;
            return this;
        }

        public Builder setWebsocket(Socket ws) {
            this.websocket = ws;
            return this;
        }

        public WebRecorder build() {

            // TODO: 2/17/16 Check file creation (permission and path)
            if (outputPath == null || outputPath.length() == 0)
                throw new IllegalArgumentException("Output path must not be null or empty");


            // TODO: 2/17/16 Check if requested width and height are actually available on device

            if (this.videoBitrate == null)
                this.videoBitrate = WebRecorder.DEFAULT_VIDEO_BITRATE;
            if (this.videoFramerate == null)
                this.videoFramerate = WebRecorder.DEFAULT_VIDEO_FRAMERATE;
            if (this.videoIFrameInterval == null)
                this.videoIFrameInterval = WebRecorder.DEFAULT_KEY_I_FRAME_INTERVAL;

            // fallback cases where the live video equals the recording video:
            if (this.liveVideoHeight == null)
                this.liveVideoHeight = videoHeight;
            if (this.liveVideoWidth == null)
                this.liveVideoWidth = videoWidth;
            if (this.liveVideoBitrate == null)
                this.liveVideoBitrate = this.videoBitrate;
            if (this.liveVideoFramerate == null)
                this.liveVideoFramerate = this.videoFramerate;

            return new WebRecorder(outputPath, videoWidth, videoHeight,
                    videoBitrate, videoFramerate, videoIFrameInterval,
                    liveVideoWidth, liveVideoHeight, liveVideoBitrate, liveVideoFramerate, websocket);
        }
    }


    private WebRecorder(String outputPath,
                        int videoWidth,
                        int videoHeight,
                        Integer videoBitrate,
                        Integer videoFramerate,
                        Integer iFrameInterval,
                        Integer liveVideoWidth,
                        Integer liveVideoHeight,
                        Integer liveVideoBitRate,
                        Integer liveVideoFrameRate,
                        Socket websocket
    ) {
        this.outputPath = outputPath;
        this.videoWidth = videoWidth;
        this.videoHeight = videoHeight;
        this.videoFrameRate = videoFramerate;
        this.videoBitRate = videoBitrate;
        this.liveVideoBitRate = liveVideoBitRate;
        this.liveVideoWidth = liveVideoWidth;
        this.liveVideoHeight = liveVideoHeight;
        this.liveVideoFrameRate = liveVideoFrameRate;
        this.iFrameInterval = iFrameInterval;
        this.websocket = websocket;
    }

    public boolean isRunning() {
        return isRunning;
    }

    public void stopBroadcasting() {
        if (videoProducer != null)
            videoProducer.setStreaming(false);
    }

    public void startBroadcasting() {
        if (videoProducer != null)
            videoProducer.setStreaming(true);
    }

    public void prepare(SurfaceHolder surfaceHolder) throws WebRecorderException {

        videoCodec = new VideoCodec(videoWidth, videoHeight, videoBitRate, videoFrameRate, iFrameInterval);
        
        audioCodec = new AudioCodec();

        audioProducerThread = new AudioProducer();
        audioProducerThread.setAudioCodec(audioCodec.getCodec());

        muxerThread = new Mp4Muxer(outputPath);

        audioConsumerThread = new AudioConsumer();
        audioConsumerThread.setCodec(audioCodec.getCodec());
        audioConsumerThread.setMuxer(muxerThread);

        videoConsumerThread = new VideoConsumer();
        videoConsumerThread.setCodec(videoCodec.getCodec());
        videoConsumerThread.setMuxer(muxerThread);
        videoProducer = new VideoProducer(surfaceHolder, videoWidth, videoHeight);
        videoProducer.setVideoCodec(videoCodec.getCodec());
        videoProducer.setVideoFrameRate(videoFrameRate);

        if (websocket != null) {

            Log.d(TAG, "Size: "+liveVideoWidth+" x "+liveVideoHeight);
            try {
                //codec
                liveVideoCodec = new VideoCodec(liveVideoWidth, liveVideoHeight, liveVideoBitRate, liveVideoFrameRate, iFrameInterval);

                //videoConsumer
                liveVideoConsumerThread = new LiveVideoConsumer();
                liveVideoConsumerThread.setCodec(liveVideoCodec.getCodec());
                liveVideoConsumerThread.setFrameIntervalCount(videoFrameRate * iFrameInterval);
                liveVideoConsumerThread.setWebsocket(websocket);

                //integrate with videoProducer
                videoProducer.setLiveVideoCodec(liveVideoCodec.getCodec(), liveVideoWidth, liveVideoHeight);
                videoProducer.setLiveVideoFrameRate(liveVideoFrameRate);
                Log.d(TAG, "Livestreaming threads prepared");
            } catch (Exception ex) {
                Log.e(TAG, "Unabled to create livestream codec.");
                Log.d(TAG, "Livestream codec error:", ex);
            }
        }
    }

    public void start() {

        isRunning = true;

        videoCodec.start();
        audioCodec.start();

        muxerThread.start();

        videoConsumerThread.start();

        if (websocket != null) {
            liveVideoCodec.start();
            liveVideoConsumerThread.start();
        }
        audioConsumerThread.start();

        videoProducer.start();
        audioProducerThread.start();

        Log.d(TAG, "ALL STARTED");
    }

    public void stop(final Promise<WebRecorderPromiseError> promise) {
        new Thread() {
            @Override
            public void run() {

                if (videoProducer != null)
                    videoProducer.end();

                if (audioProducerThread != null)
                    audioProducerThread.end();

                if (muxerThread != null)
                    muxerThread.end();

                if (audioConsumerThread != null)
                    audioConsumerThread.end();

                if (videoConsumerThread != null)
                    videoConsumerThread.end();

                if (liveVideoConsumerThread != null)
                    liveVideoConsumerThread.end();

                try {
                    audioProducerThread.join();
                    audioConsumerThread.join();
                    videoConsumerThread.join();

                    if (websocket != null) {
                        liveVideoConsumerThread.join();
                    }

                    videoCodec.end();
                    audioCodec.end();
                    if (liveVideoCodec != null)
                        liveVideoCodec.end();

                    muxerThread.join();

                    if (promise!=null)
                        promise.success();
                } catch (InterruptedException e) {
                    if (promise!=null)
                        promise.error(WebRecorderPromiseError.OTHER.put("exception", e));
                }

                isRunning = false;
            }
        }.start();
    }
}
