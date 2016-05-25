package org.igarape.webrecorder;

/**
 * Created by martelli on 2/16/16.
 */
public class WebRecorderException extends Exception {
    public WebRecorderException(String msg) {
        super(msg);
    }

    public WebRecorderException(Exception e) {
        super(e);
    }
}
