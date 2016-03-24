package org.igarape.webrecorder;

import android.media.MediaCodec;

import java.nio.ByteBuffer;

/**
 * Created by martelli on 2/17/16.
 */
class MediaFrame {

    private ByteBuffer buffer;
    private MediaCodec.BufferInfo bufferInfo;
    private MediaType mediaType;

    public MediaFrame(ByteBuffer buffer, MediaCodec.BufferInfo bufferInfo, MediaType mediaType) {
        this.buffer = buffer;
        this.bufferInfo = bufferInfo;
        this.mediaType = mediaType;
    }

    public ByteBuffer getBuffer() {
        return buffer;
    }

    public void setBuffer(ByteBuffer buffer) {
        this.buffer = buffer;
    }

    public MediaCodec.BufferInfo getBufferInfo() {
        return bufferInfo;
    }

    public void setBufferInfo(MediaCodec.BufferInfo bufferInfo) {
        this.bufferInfo = bufferInfo;
    }

    public MediaType getMediaType() {
        return mediaType;
    }

    public void setMediaType(MediaType mediaType) {
        this.mediaType = mediaType;
    }
}
