package webrtcclient;

import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaFormat;
import android.media.MediaMuxer;
import android.os.Environment;
import android.util.Log;

import org.webrtc.VideoRenderer;
import org.webrtc.VideoTrack;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.text.SimpleDateFormat;
import java.util.ArrayDeque;
import java.util.Date;

/**
 * We will have two callback classes that work together.  They form an encode/save
 * pipeline.  The pipeline mediates between input/save rates via the buffers fed through
 * the codec.  The codec is idle when we have more free buffers than needed for the
 * incoming rate, and is overloaded when we have frames coming faster than we have
 * buffers returning from the codec.
 * <p/>
 * For now, make it very dumb and simple: drop frames when we've got too many coming in.
 * Later: measure the incoming frame rate and the outgoing (encoding) frame rate, and
 * evenly drop frames if needed.  This will need a bit of experimentation to get right.
 * <p/>
 * General Flow:
 * VideoTrack -[1]--> MediaCodec --[2]--> MediaMuxer --[3]--> mp4 File
 * [1]: Via VideoRenderer.Callbacks, get frames.
 * [2]: Via MediaCodec.Callbacks, get encoded bytes,
 * [3]: MediaMuxer directly writes to mp4 file.
 * <p/>
 * How this works w.r.t. system bottlenecks and frame rate:
 * The incoming VideoTrack should be roughly the frame capture speed, modulo variation
 * introduced by the CPU scheduler.  The MediaCodec will be CPU bound, working
 * asynchronously.  The MediaMuxer is relatively lightweight, so it shouldn't be doing
 * much work, except File I/O.  It may bottleneck on the flash device's write rate.
 * <p/>
 * Right now, we'll drop frames when the system isn't ready for it downstream.  Later
 * we may want to auto-tune the frame rate (equivalent to dropping frames evently), or
 * resolution/quality.
 * <p/>
 * Created by lally on 10/25/15.
 */
public class VideoFile extends MediaCodec.Callback implements VideoRenderer.Callbacks {
    private final static String TAG = VideoFile.class.getCanonicalName();
    private final static File VIDEO_SAVE_DIR = Environment.getExternalStorageDirectory();
    int mFrameRate;
    int mFrameInterval;
    VideoRenderer mRenderer;
    MediaMuxer mMuxer;
    MediaCodec mVideoEncoder;
    int mVideoTrackNr;
    boolean videoFormatSetUp;
    ArrayDeque<Integer> mEncodeBuffers, mSaveBuffers;
    int mSkippedFrames;


    public VideoFile(int width, int height, int frame_rate) {
        try {
            Log.d(TAG, "VideoFile(" + width + ", " + height + ", " + frame_rate + ")");
            mFrameRate = frame_rate;
            mFrameInterval = 1000000 / frame_rate;
            // Setup a save pipeline for incoming video frames.
            mVideoEncoder = MediaCodec.createEncoderByType("video/x-vnd-on2.vp8");
            MediaFormat mediaFormat = MediaFormat.createVideoFormat("video/x-vnd-on2.vp8", width, height);
            mediaFormat.setInteger(MediaFormat.KEY_FRAME_RATE, frame_rate);
            // See if we can just set the color format to what we like.  Only get into checking
            // colorFormats if that doesn't work.
            //mVideoEncoder.getCodecInfo().getCapabilitiesForType(…).colorFormats;
            mediaFormat.setInteger(MediaFormat.KEY_COLOR_FORMAT,
                    MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420Flexible);
            mVideoEncoder.setCallback(this);
            mVideoEncoder.configure(mediaFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
            mVideoEncoder.start();
            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd_HH:mm:ss.SSSZ");
            String outputPath = new File(VIDEO_SAVE_DIR,
                    dateFormat.format(new Date()) + ".mp4").toString();
            Log.i(TAG, "Output file is " + outputPath);
            mMuxer = new MediaMuxer(outputPath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4);

            mEncodeBuffers = new ArrayDeque<Integer>();
            mSaveBuffers = new ArrayDeque<Integer>();
            this.mVideoTrackNr = -1;
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void renderFrame(VideoRenderer.I420Frame frame) {
        // 1. Get buffer to use.
        if (mEncodeBuffers.isEmpty()) {
            mSkippedFrames++;
            Log.d(TAG, "renderFrame: no encode bufers, skipping frame" + mSkippedFrames);
            // TODO: put in better stats for frame drop rates here.
            return;
        }
        int inputBuffer = mEncodeBuffers.removeFirst();
        ByteBuffer buffer = mVideoEncoder.getInputBuffer(inputBuffer);
        // 2. Copy data into buffer.
        // Input is i420: 8 bit Y plane followed by 8 bit 2x2 subsampled U and V planes
        // Conversion to RGB: http://www.fourcc.org/fccyvrgb.php
        // Output isn't yet determined.  See "Raw Video Buffers" in
        //   https://developer.android.com/reference/android/media/MediaCodec.html
        // Looks like I can just concatenate the planes one after another.
        if (buffer == null) {
            throw new RuntimeException("Could not get buffer for " + inputBuffer);
        } else {
            Log.d(TAG, "Buffer capacity is: " + buffer.capacity());
            Log.d(TAG, " Strides: " + frame.yuvStrides[0] + ", " + frame.yuvStrides[1] + ", " +
                    frame.yuvStrides[2]);
            //buffer.reset();
            buffer.put(frame.yuvPlanes[0]).put(frame.yuvPlanes[1]).put(frame.yuvPlanes[2]);

            // 3. Queue buffer with encoder.  What are these args about?
            //  buffer index: same as inputBuffer.
            //  offset: can be zero.
            //  size:  size of buffer
            //  presentationTimeUs: usec of frame interval.
            //  flags: 0 until we're at the end.
            mVideoEncoder.queueInputBuffer(inputBuffer, 0, buffer.capacity(), mFrameInterval, 0);
        }
    }

    @Override
    public void onInputBufferAvailable(MediaCodec codec, int index) {
        mEncodeBuffers.addLast(index);
        Log.d(TAG, "onInputBufferAvailable: " + index);
    }

    @Override
    public void onOutputBufferAvailable(MediaCodec codec,
                                        int index,
                                        MediaCodec.BufferInfo info) {
        //mSaveBuffers.addLast(index);
        // Feed these into mMuxer, which then can release the output buffers when
        // saved.
        Log.d(TAG, "onOutputBufferAvailable(_," + index + "," + info + ")");
        ByteBuffer buffer = mVideoEncoder.getOutputBuffer(index);
        mMuxer.writeSampleData(mVideoTrackNr, buffer, info);
        mVideoEncoder.releaseOutputBuffer(index, true);
    }

    @Override
    public void onError(MediaCodec codec, MediaCodec.CodecException e) {
        Log.d(TAG, "Codec exception: " + e);
    }

    @Override
    public void onOutputFormatChanged(MediaCodec codec, MediaFormat format) {
        Log.d(TAG, "onOutputFormatChanged to " + format + ", mVideoTrackNr: " + mVideoTrackNr);
        if (mVideoTrackNr < 0) {
            mVideoTrackNr = mMuxer.addTrack(format);
            mMuxer.start();
        } else {
            throw new RuntimeException("Got a second format-changed.");
        }
    }

    public void setVideoTrack(VideoTrack track) {
        mRenderer = new VideoRenderer(this);
        track.addRenderer(mRenderer);
    }

}
