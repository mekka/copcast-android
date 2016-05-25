package org.igarape.copcast.exceptions;

/**
 * Created by martelli on 3/23/16.
 */
public class SqliteDbException extends Exception {
    public SqliteDbException(String msg) {
        super(msg);
    }

    public SqliteDbException(String tag, String msg) {
        super(tag+": "+msg);
    }
}
