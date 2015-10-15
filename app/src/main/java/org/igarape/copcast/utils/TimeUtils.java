package org.igarape.copcast.utils;

import java.sql.Timestamp;
import java.util.Date;


public class TimeUtils {

    public static Timestamp getTimestamp() {
        return new Timestamp(new Date().getTime());
    }
}
