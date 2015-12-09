package org.igarape.copcast.service.upload;

import android.content.Context;
import android.util.Log;

import org.igarape.copcast.utils.FileUtils;
import org.igarape.copcast.utils.HttpResponseCallback;
import org.igarape.copcast.utils.ILog;
import org.igarape.copcast.utils.NetworkUtils;
import org.igarape.copcast.utils.TextFileType;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;

/**
 * Created by martelli on 12/8/15.
 */
public class TextFileUploader {

    private static final String TAG = TextFileUploader.class.getName();

    private static void errlog(TextFileType textFileType, String m) {
        ILog.e(TAG, textFileType.getName() + ": " + m);
    }

    public static void upload(Context context, final TextFileType textFileType, String userLogin) {
        final File file = new File(FileUtils.getTextFilePath(textFileType, userLogin));
        if (!file.exists()) {
            return;
        }

        ILog.d(TAG, "File size ("+ textFileType.getName()+"): " + file.length());

        FileInputStream is = null;
        try {
            is = new FileInputStream(file);

            BufferedReader br = new BufferedReader(new InputStreamReader(is, Charset.forName("UTF-8")));
            JSONArray entries = new JSONArray();
            String line;

            while ((line = br.readLine()) != null) {
                JSONObject json = new JSONObject(line);
                entries.put(json);
            }

            ILog.d(TAG, entries.toString(2));

            NetworkUtils.post(context, textFileType.getUrl() + "/" + userLogin, entries, new HttpResponseCallback() {
                @Override
                public void unauthorized() {
                    errlog(textFileType, "unauthorized");
                }

                @Override
                public void failure(int statusCode) {
                    errlog(textFileType, "failure - statusCode: " + statusCode);
                }

                @Override
                public void success(JSONObject response) {
                    //file.delete();
                    Log.e(TAG, "would delete file");
                }

                @Override
                public void noConnection() {
                    errlog(textFileType, "noConnection");
                }

                @Override
                public void badConnection() {
                    errlog(textFileType, "badConnection");
                }

                @Override
                public void badRequest() {
                    errlog(textFileType, "badRequest");
                }

                @Override
                public void badResponse() {
                    errlog(textFileType, "badResponse");
                }
            });
        } catch (java.io.IOException e) {
            ILog.e(TAG, "Could not upload text file", e);
        } catch (JSONException e) {
            ILog.e(TAG, "Could not upload text file", e);
        }
    }

}
