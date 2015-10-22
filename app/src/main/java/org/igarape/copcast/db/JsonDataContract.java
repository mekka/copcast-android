package org.igarape.copcast.db;

import android.provider.BaseColumns;

/**
 * Created by martelli on 10/20/15.
 */
public class JsonDataContract {

    public JsonDataContract() {}

    public static abstract class JsonDataEntry implements BaseColumns {
        public static final String TABLE_NAME = "jsondata";
        public static final String COLUMN_ID = "id";
        public static final String COLUMN_USER = "user";
        public static final String COLUMN_TYPE = "jsontype";
        public static final String COLUMN_DATA = "jsondata";
        public static final String COLUMN_TIMESTAMP = "created_At";
    }
}
