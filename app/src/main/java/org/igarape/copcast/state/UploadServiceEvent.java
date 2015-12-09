package org.igarape.copcast.state;

/**
 * Created by martelli on 12/9/15.
 */
public enum UploadServiceEvent {
    FINISHED(false),
    NO_DATA(false),
    STARTED(true),
    ABORTED_USER(false),
    ABORTED_NO_NETWORK(false);

    private final boolean running;
    UploadServiceEvent(boolean running) {
        this.running = running;
    }

    public boolean getRunning() {
        return running;
    }
}
