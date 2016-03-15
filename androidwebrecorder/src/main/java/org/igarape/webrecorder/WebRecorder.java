package org.igarape.webrecorder;

import android.media.AudioFormat;
import android.media.MediaRecorder;
import android.util.Log;
import android.view.SurfaceHolder;

import org.igarape.util.Promise;

import java.util.HashMap;
import java.util.Map;

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
    public static final int BUFFER_IN_MS = 50;
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
    private AudioProducer audioProducerThread;
    private VideoProducer videoProducerThread;
    private AudioConsumer audioConsumerThread;
    private VideoConsumer videoConsumerThread;
    private Mp4Muxer muxerThread;
    private WebsocketThread websocketThread;

    private int videoHeight;
    private int videoWidth;
    public static int videoBitRate;
    private int videoFrameRate;
    private int iFrameInterval;
    private final String outputPath;
    private String websocketServerUrl;
    private Map<String, String> websocketHeaders;
    private Runnable streamingStartedRunnable;
    private Runnable streamingStoppedRunnable;

    private boolean isRunning;

    public static class Builder {
        private final int videoWidth;
        private final int videoHeight;
        private Integer videoBitrate;
        private Integer videoFramerate;
        private Integer videoIFrameInterval;
        private String websocketServerUrl;
        private String outputPath;
        private Map<String, String> websocketHeaders = new HashMap<>();
        private Runnable streamingStartedRunnable;
        private Runnable streamingStoppedRunnable;

        public Builder(String outputPath, final int videoWidth, final int videoHeight) {
            this.videoHeight = videoHeight;
            this.videoWidth = videoWidth;
            this.outputPath = outputPath;
        }

        public Builder setVideoBitRate(int video_bitrate) {
            this.videoBitrate = video_bitrate;
            return this;
        }

        public Builder setVideoFrameRate(int video_framerate) {
            this.videoFramerate = video_framerate;
            return this;
        }

        public Builder setVideoIFrameInterval(int i_frame_interval) {
            this.videoIFrameInterval = i_frame_interval;
            return this;
        }

        public Builder setWebsocketServer(String websocketServerUrl) {
            this.websocketServerUrl = websocketServerUrl;
            return this;
        }

        public Builder addHeader(String key, String value) {
            this.websocketHeaders.put(key, value);
            return this;
        }

        public Builder setStreamStartedRunnable(Runnable runnable) {
            this.streamingStartedRunnable = runnable;
            return this;
        }

        public Builder setStreamStoppedRunnable(Runnable runnable) {
            this.streamingStoppedRunnable = runnable;
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

            return new WebRecorder(outputPath, videoWidth, videoHeight,
                    videoBitrate, videoFramerate, videoIFrameInterval, websocketServerUrl,
                    websocketHeaders, streamingStartedRunnable, streamingStoppedRunnable);
        }
    }


    private WebRecorder(String outputPath,
                        int videoWidth,
                        int videoHeight,
                        Integer videoBitrate,
                        Integer videoFramerate,
                        Integer iFrameInterval,
                        String websocketServerUrl,
                        Map<String, String> websocketHeaders,
                        Runnable streamingStartedRunnable,
                        Runnable streamingStoppedRunnable
    ) {
        this.outputPath = outputPath;
        this.videoWidth = videoWidth;
        this.videoHeight = videoHeight;
        this.websocketServerUrl = websocketServerUrl;
        this.videoFrameRate = videoFramerate;
        this.videoBitRate = videoBitrate;
        this.iFrameInterval = iFrameInterval;
        this.websocketHeaders = websocketHeaders;
        this.streamingStartedRunnable = streamingStartedRunnable;
        this.streamingStoppedRunnable = streamingStoppedRunnable;
    }

    private long getTimestamp() {
        return System.nanoTime() / 1000;
    }

    public boolean isRunning() {
        return isRunning;
    }

    public int getVideoFrameRate() {
        return videoFrameRate;
    }

    public void stopBroadcasting() {
        videoConsumerThread.setStreaming(false);
        if (this.streamingStoppedRunnable != null) {
            Log.e(TAG, "stopping runnable");
            this.streamingStoppedRunnable.run();
        }
    }

    public void startBroadcasting() {
        videoConsumerThread.setStreaming(true);
        if (this.streamingStartedRunnable != null) {
            Log.e(TAG, "starting runnable");
            this.streamingStartedRunnable.run();
        }
    }

    public void prepare(SurfaceHolder surfaceHolder) throws WebRecorderException {

        if (websocketServerUrl != null)
            websocketThread = new WebsocketThread(websocketServerUrl, websocketHeaders, videoFrameRate);

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
        if (websocketServerUrl != null)
            videoConsumerThread.setWebsocketThread(websocketThread);
        videoConsumerThread.setMuxer(muxerThread);

        videoProducerThread = new VideoProducer(surfaceHolder, videoWidth, videoHeight);
        videoProducerThread.setVideoCodec(videoCodec.getCodec());
        videoProducerThread.setVideoFrameRate(videoFrameRate);
    }

    public void start() {

        isRunning = true;

        if (websocketServerUrl != null)
            websocketThread.start();

        videoCodec.start();
        audioCodec.start();

        muxerThread.start();

        videoConsumerThread.start();
        audioConsumerThread.start();

        videoProducerThread.start();
        audioProducerThread.start();

        new Thread() {
            @Override
            public void run() {
                Log.e(TAG, "THREAD!");
                try {
                    Thread.sleep(5000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                Log.e(TAG, "timeout!");
                WebRecorder.this.startBroadcasting();
            }
        }.start();

        Log.d(TAG, "ALL STARTED");
    }

    public void stop(final Promise promise) {
        new Thread() {
            @Override
            public void run() {

                videoProducerThread.end();
                audioProducerThread.end();
                muxerThread.end();
                audioConsumerThread.end();
                videoConsumerThread.end();
                if (websocketThread != null)
                    websocketThread.end();

                try {
                    audioProducerThread.join();
                    audioConsumerThread.join();
                    videoConsumerThread.join();

                    if (websocketThread != null) {
                        websocketThread.join();
                    }

                    muxerThread.join();

                    isRunning = false;
                    if (promise!=null)
                        promise.success();
                } catch (InterruptedException e) {
                    if (promise!=null)
                        promise.failure(e);
                }
            }
        }.start();
    }
}