package org.igarape.copcast.utils;

import android.content.Context;
import android.util.Log;

import org.igarape.copcast.db.JsonDataType;
import org.json.JSONObject;

/**
 * Created by martelli on 1/7/16.
 *
 * Once the data has failed to be sent to the server,
 * it is persisted to be sent in batch during the upload phase.
 * This class saves some boilerplate and allows a string to be
 * displayed to differentiate between calls.
 */
public class GenericSqliteLogger {

    private Context context;
    private JSONObject data;
    private String tag;
    private JsonDataType jsonDataType;
    public GenericSqliteLogger(Context context, JsonDataType jsonDataType, JSONObject data, String tag) {
        this.context = context;
        this.data = data;
        this.tag = tag;
        this.jsonDataType = jsonDataType;
    }

    public void logFailedData(String id) {
        Log.e(this.tag, "incident not sent successfully: " + id);
        String userLogin = Globals.getUserLogin(this.context);
        SqliteUtils.storeToDb(this.context, userLogin, this.jsonDataType, this.data);
    }
}
