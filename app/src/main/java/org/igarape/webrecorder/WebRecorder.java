package org.igarape.webrecorder;

import android.media.AudioFormat;
import android.media.CamcorderProfile;
import android.media.MediaRecorder;
import android.util.Log;
import android.view.SurfaceHolder;

import org.igarape.copcast.promises.Promise;
import org.igarape.copcast.promises.WebRecorderPromiseError;
import org.igarape.copcast.utils.Globals;
import org.igarape.webrecorder.enums.Orientation;

import java.util.concurrent.Semaphore;

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
    private VideoProducer videoProducerThread;
    private AudioConsumer audioConsumerThread;
    private VideoConsumer videoConsumerThread;
    private Mp4Muxer muxerThread;
    private LiveVideoConsumer liveVideoConsumerThread;
    private Semaphore orientationLock = new Semaphore(1);

    private int videoHeight;
    private int videoWidth;
    public static int videoBitrate;
    private int videoFramerate;
    private int liveVideoHeight;
    private int liveVideoWidth;
    public static int liveVideoBitrate;
    private int liveVideoFramerate;
    private int iFrameInterval;
    private final String outputPath;
    private Socket websocket;
    private boolean isRunning;
    private boolean isStreaming;
    private SurfaceHolder surfaceHolder;

    public static class Builder {
        private int videoWidth;
        private int videoHeight;
        private Integer videoBitrate;
        private Integer videoFramerate;
        private int liveVideoWidth;
        private int liveVideoHeight;
        private Integer liveVideoBitrate;
        private Integer liveVideoFramerate;
        private Integer videoIFrameInterval;
        private Socket websocket;
        private String outputPath;
        private SurfaceHolder surfaceHolder;

        public Builder(String outputPath, final int videoQuality, final int liveVideoQuality, SurfaceHolder surfaceHolder) {
            CamcorderProfile profile = CamcorderProfile.get(videoQuality);
            CamcorderProfile liveProfile = CamcorderProfile.get(liveVideoQuality);
            
            this.videoHeight = profile.videoFrameHeight;
            this.videoWidth = profile.videoFrameWidth;
            this.liveVideoHeight = liveProfile.videoFrameHeight;
            this.liveVideoWidth = liveProfile.videoFrameWidth;
            this.outputPath = outputPath;
            this.surfaceHolder = surfaceHolder;
        }

        public Builder setVideoBitrate(int video_bitrate) {
            this.videoBitrate = video_bitrate;
            return this;
        }

        public Builder setVideoFramerate(int video_framerate) {
            this.videoFramerate = video_framerate;
            return this;
        }

        public Builder setLiveVideoBitrate(int video_bitrate) {
            this.liveVideoBitrate = video_bitrate;
            return this;
        }

        public Builder setLiveVideoFramerate(int video_framerate) {
            this.liveVideoFramerate = video_framerate;
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
            if (this.liveVideoBitrate == null)
                this.liveVideoBitrate = WebRecorder.DEFAULT_VIDEO_BITRATE;
            if (this.videoFramerate == null)
                this.videoFramerate = WebRecorder.DEFAULT_VIDEO_FRAMERATE;
            if (this.liveVideoFramerate == null)
                this.liveVideoFramerate = WebRecorder.DEFAULT_VIDEO_FRAMERATE;
            if (this.videoIFrameInterval == null)
                this.videoIFrameInterval = WebRecorder.DEFAULT_KEY_I_FRAME_INTERVAL;

            return new WebRecorder(outputPath, videoWidth, videoHeight,
                    videoBitrate, videoFramerate, liveVideoWidth, liveVideoHeight, liveVideoFramerate,
                    liveVideoBitrate, videoIFrameInterval, websocket, surfaceHolder);
        }
    }


    private WebRecorder(String outputPath,
                        int videoWidth,
                        int videoHeight,
                        Integer videoBitrate,
                        Integer videoFramerate,
                        Integer liveVideoWidth,
                        Integer liveVideoHeight,
                        Integer liveVideoFramerate,
                        Integer liveVideoBitrate,
                        Integer iFrameInterval,
                        Socket websocket,
                        SurfaceHolder surfaceHolder
    ) {
        this.outputPath = outputPath;
        this.videoWidth = videoWidth;
        this.videoHeight = videoHeight;
        this.videoFramerate = videoFramerate;
        this.videoBitrate = videoBitrate;
        this.liveVideoWidth = liveVideoWidth;
        this.liveVideoHeight = liveVideoHeight;
        this.liveVideoFramerate = liveVideoFramerate;
        this.liveVideoBitrate = liveVideoBitrate;
        this.iFrameInterval = iFrameInterval;
        this.websocket = websocket;
        this.surfaceHolder = surfaceHolder;
    }
    
    public boolean isRunning() {
        return isRunning;
    }

    public void stopBroadcasting() {
        isStreaming = false;
        if (videoProducerThread != null)
            videoProducerThread.setStreaming(false);
        if (liveVideoConsumerThread != null)
            liveVideoConsumerThread.setStreaming(false);
    }

    public void startBroadcasting() {
        isStreaming = true;
        if (videoProducerThread != null)
            videoProducerThread.setStreaming(true);
        if (liveVideoConsumerThread != null)
            liveVideoConsumerThread.setStreaming(true);
    }

    public void restartOrientation() {

        try {
            orientationLock.acquire();
        } catch (InterruptedException e) {
            Log.e(TAG, "Interrupted while wating for a lock", e);
        }

        this.stop(new Promise<WebRecorderPromiseError>() {
            @Override
            public void success() {
                try {
                    WebRecorder.this.prepare();
                    WebRecorder.this.start();
                    if (isStreaming)
                        startBroadcasting();
                    Log.d(TAG, "Webrecorder restarted");
                } catch (WebRecorderException e) {
                    Log.e(TAG, "Failed to restart webrecorder", e);
                } finally {
                    orientationLock.release();
                }
            }
        });
    }

    public void prepare() throws WebRecorderException {

        int h = this.videoHeight;
        int w = this.videoWidth;
        int lh = this.liveVideoHeight;
        int lw = this.liveVideoWidth;

        if (Globals.orientation == Orientation.TOP || Globals.orientation == Orientation.BOTTOM) {
            h = this.videoWidth;
            w = this.videoHeight;
            lh = this.liveVideoWidth;
            lw = this.liveVideoHeight;
        }

        if (websocket != null) {
            liveVideoConsumerThread = new LiveVideoConsumer(websocket, videoFramerate);
        }

        videoCodec = new VideoCodec(w, h, videoBitrate, videoFramerate, iFrameInterval);
        liveVideoCodec = new VideoCodec(lw, lh, liveVideoBitrate, liveVideoFramerate, iFrameInterval);
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

        videoProducerThread = new VideoProducer(surfaceHolder, w, h);
        videoProducerThread.setVideoCodec(videoCodec.getCodec());
        videoProducerThread.setLiveVideoCodec(liveVideoCodec.getCodec());
        videoProducerThread.setVideoFrameRate(videoFramerate);
        videoProducerThread.setLiveVideoFrameRate(liveVideoFramerate);

        if (websocket != null)
            liveVideoConsumerThread.setLiveVideoCodec(liveVideoCodec.getCodec());
    }

    public void start() {

        isRunning = true;

        videoCodec.start();
        liveVideoCodec.start();
        audioCodec.start();

        if (websocket != null)
            liveVideoConsumerThread.start();

        muxerThread.start();

        videoConsumerThread.start();
        audioConsumerThread.start();

        videoProducerThread.start();
        audioProducerThread.start();

        Log.d(TAG, "ALL STARTED");
    }

    public void stop(final Promise<WebRecorderPromiseError> promise) {
        new Thread() {
            @Override
            public void run() {

                if (videoProducerThread != null)
                    videoProducerThread.end();

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

                    if (liveVideoConsumerThread != null) {
                        liveVideoConsumerThread.join();
                    }

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
