package org.igarape.copcast.utils;

import android.content.Context;
import android.util.Log;

import org.igarape.copcast.db.JsonDataType;
import org.igarape.copcast.exceptions.HttpPostError;
import org.igarape.copcast.exceptions.PromiseException;
import org.json.JSONObject;

/**
 * Created by martelli on 1/7/16.
 *
 * Once the data has failed to be sent to the server,
 * it is persisted to be sent in batch during the upload phase.
 * This class saves some boilerplate and allows a string to be
 * displayed to differentiate between calls.
 */
public class LoggedHTTPResponseCallback extends Promise<HttpPostError> {

    protected Context context;
    protected JSONObject data;
    protected String tag;
    protected JsonDataType jsonDataType;

    public LoggedHTTPResponseCallback(Context context, JsonDataType jsonDataType, JSONObject data, String tag) {
        this.context = context;
        this.data = data;
        this.tag = tag;
        this.jsonDataType = jsonDataType;
    }

    protected void logFailedData(String id) {
        Log.e(this.tag, "data not sent successfully: " + id);
        String userLogin = Globals.getUserLogin(this.context);
        SqliteUtils.storeToDb(this.context, userLogin, this.jsonDataType, this.data);
    }

    @Override
    public void error(PromiseException<HttpPostError> error) {
        this.logFailedData(error.toString());
    }

    @Override
    public void success() {
        Log.d(this.tag, "data sent.");
    }

}
