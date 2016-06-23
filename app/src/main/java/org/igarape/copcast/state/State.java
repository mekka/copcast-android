package org.igarape.copcast.state;

/**
 * Created by brunosiqueira on 07/05/2014.
 */
public enum State {

    LOGGED_OFF,
    IDLE,
    PAUSED,
    RECORDING,
    STREAM_REQUESTED,
    STREAMING,
    UPLOADING,

    //events
    SEEN_VIDEO

}