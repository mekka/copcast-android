package org.igarape.copcast.db;

import android.provider.BaseColumns;

/**
 * Created by martelli on 10/20/15.
 */
public class IncidentVideoContract {

    public IncidentVideoContract() {}

    public static abstract class IncidentVideoEntry implements BaseColumns {
        public static final String TABLE_NAME = "incident_videos";
        public static final String COLUMN_ID = "id";
        public static final String COLUMN_VIDEO_NAME = "videoname";
    }
}
