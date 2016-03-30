package org.igarape.copcast.exceptions;

/**
 * Created by martelli on 3/23/16.
 */
public class HistoryException extends Exception {
    public HistoryException(String msg) {
        super(msg);
    }

    public HistoryException(String tag, String msg) {
        super(tag+": "+msg);
    }
}
