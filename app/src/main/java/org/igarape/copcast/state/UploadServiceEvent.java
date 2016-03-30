package org.igarape.copcast.state;

/**
 * Created by martelli on 12/9/15.
 */
public enum UploadServiceEvent {
    FINISHED(false),
    RUNNING(true),
    STARTED(true),
    ABORTED_USER(false),
    FAILED(false),
    ABORTED_NO_NETWORK(false);

    private final boolean running;
    UploadServiceEvent(boolean running) {
        this.running = running;
    }

    public boolean isRunning() {
        return running;
    }
}
