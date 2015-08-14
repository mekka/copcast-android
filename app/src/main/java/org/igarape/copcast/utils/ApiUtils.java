package org.igarape.copcast.utils;

import android.content.Context;
import android.util.Log;

import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.AsyncHttpResponseHandler;
import com.loopj.android.http.JsonHttpResponseHandler;
import com.loopj.android.http.RequestParams;

import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.entity.StringEntity;
import org.igarape.copcast.BuildConfig;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;

/**
 * Created by brunosiqueira on 14/08/15.
 */
public class ApiUtils {
    public static final String TAG = ApiUtils.class.getName();
    private static final int DEFAULT_TIMEOUT = 3000000;
    private static Context appContext;
    private static AsyncHttpClient client;


    static {
        client = new AsyncHttpClient();
        client.setURLEncodingEnabled(true);
        client.setTimeout(DEFAULT_TIMEOUT);
        client.setMaxConnections(5);
        client.addHeader("Content-Type", "multipart/form-data");
        client.addHeader("Accept-Encoding", "gzip,deflate");
    }

    private static String globalToken;

    public static void get(String url, RequestParams params, AsyncHttpResponseHandler responseHandler) {
        client.get(getServerUrl(url), params, responseHandler);
    }

    public static void post(String url, RequestParams params, AsyncHttpResponseHandler responseHandler) {
        client.post(getServerUrl(url), params, responseHandler);
    }

    public static void post(String url, AsyncHttpResponseHandler responseHandler){
        client.post(getServerUrl(url), responseHandler);
    }

    public static void delete(String url, AsyncHttpResponseHandler responseHandler){
        client.delete(getServerUrl(url), responseHandler);
    }



    public static void setToken(String token) {
        globalToken = token;
        client.addHeader("Authorization", "Bearer " + token);
    }

    public static String getServerUrl(String path) {
        return String.format("%s%s", BuildConfig.serverUrl, path);
    }

    public static void setAppContext(Context context) {
        appContext = context;
    }

}
