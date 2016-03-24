package org.igarape.copcast.service.upload;

import android.content.Context;

import org.igarape.copcast.db.JsonDataType;
import org.igarape.copcast.promises.HttpPromiseError;
import org.igarape.copcast.promises.PromiseError;
import org.igarape.copcast.utils.ILog;
import org.igarape.copcast.utils.NetworkUtils;
import org.igarape.copcast.promises.Promise;
import org.igarape.copcast.utils.SqliteUtils;
import org.json.JSONArray;
import org.json.JSONException;

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

            JSONArray entries = SqliteUtils.getFromDb(context, userLogin, sqlite_key);

            if (entries.length() == 0)
                ILog.d(TAG, "No data available for: "+sqlite_key.getType());
            else
                NetworkUtils.post(context, sqlite_key.getUrl() + "/" + userLogin, entries, new Promise() {

                    @Override
                    public void error(PromiseError exception) {
                        if (exception == HttpPromiseError.FAILURE)
                            errlog(sqlite_key, "failure - statusCode: " + exception.get("statusCode"));
                        else
                            errlog(sqlite_key, exception.toString());
                    }

                    @Override
                    public void success() {
                        SqliteUtils.clearByType(context, userLogin, sqlite_key);
                        ILog.d(TAG, "Entries for "+sqlite_key.getType()+ " deleted.");
                    }
                });

        } catch (JSONException e) {
            ILog.e(TAG, "Could not upload json data", e);
        }
    }

}
