package org.igarape.copcast.service.upload;

import android.content.Context;

import org.igarape.copcast.db.JsonDataType;
import org.igarape.copcast.utils.HttpResponseCallback;
import org.igarape.copcast.utils.ILog;
import org.igarape.copcast.utils.NetworkUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import static org.igarape.copcast.utils.SqliteUtils.getFromDb;

/**
 * Created by martelli on 12/8/15.
 */
public class SqliteUploader {

    private static final String TAG = SqliteUploader.class.getName();

    private static void errlog(JsonDataType jsonDataType, String m) {
        ILog.e(TAG, jsonDataType.getType()+": "+m);
    }

    public static void upload(final Context context, final JsonDataType sqlite_key, final String userLogin) {
        try {

            JSONArray entries = getFromDb(context, userLogin, sqlite_key);

            if (entries.length() == 0)
                ILog.d(TAG, "No data available for: "+sqlite_key.getType());
            else
                ILog.d(TAG, entries.toString(2));

            NetworkUtils.post(context, sqlite_key.getUrl() + "/" + userLogin, entries, new HttpResponseCallback() {
                @Override
                public void unauthorized() {
                    errlog(sqlite_key, "unauthorized");
                }

                @Override
                public void failure(int statusCode) {
                    errlog(sqlite_key, "failure - statusCode: " + statusCode);
                }

                @Override
                public void success(JSONObject response) {
                    //SqliteUtils.clearByType(context, userLogin, sqlite_key.getType());
                    ILog.e(TAG, "would delete entries");
                }

                @Override
                public void noConnection() {
                    errlog(sqlite_key, "noConnection");
                }

                @Override
                public void badConnection() {
                    errlog(sqlite_key, "badConnection");
                }

                @Override
                public void badRequest() {
                    errlog(sqlite_key, "badRequest");
                }

                @Override
                public void badResponse() {
                    errlog(sqlite_key, "badResponse");
                }
            });
        } catch (JSONException e) {
            ILog.e(TAG, "Could not upload json data", e);
        }
    }

}
