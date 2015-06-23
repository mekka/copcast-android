package org.igarape.copcast.utils;

import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.BatteryManager;
import android.os.Build;
import android.util.Log;

import org.apache.http.NameValuePair;
import org.igarape.copcast.BuildConfig;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.List;
import java.util.Scanner;

/**
 * Created by fcavalcanti on 31/10/2014.
 */
public class NetworkUtils {

    private static final String TAG = NetworkUtils.class.getName();
    private static int CONNECTION_TIMEOUT = 15000;
    private static int DATA_RETRIEVAL_TIMEOUT = 5000;

    private static String getQuery(List<NameValuePair> params) throws UnsupportedEncodingException {
        StringBuilder result = new StringBuilder();
        boolean first = true;

        for (NameValuePair pair : params) {
            if (first)
                first = false;
            else
                result.append("&");

            result.append(URLEncoder.encode(pair.getName(), "UTF-8"));
            result.append("=");
            result.append(URLEncoder.encode(pair.getValue(), "UTF-8"));
        }

        return result.toString();
    }

    /**
     * required in order to prevent issues in earlier Android version.
     */
    private static void disableConnectionReuseIfNecessary() {
        // see HttpURLConnection API doc
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.FROYO) {
            System.setProperty("http.keepAlive", "false");
        }
    }

    private static String getResponseText(InputStream inStream) throws IOException {
        BufferedReader br = new BufferedReader(new InputStreamReader(inStream));
        StringBuilder sb = new StringBuilder();
        String line;
        while ((line = br.readLine()) != null) {
            sb.append(line+"\n");
        }
        br.close();
        return sb.toString();
    }

    public static void get(Context context, String url, HttpResponseCallback callback) {
        get( context,  url,  Response.JSON,  callback);
    }

    public static void get(Context context, String url, Response type, HttpResponseCallback callback) {
        executeRequest(Method.GET, context, null, null, url,type, callback);
    }


    public static void post(Context context, String url, List<NameValuePair> params, HttpResponseCallback callback) {
        executeRequest(Method.POST, context, params, null, url, Response.JSON, callback);
    }

    public static void post(Context context, String url, Object jsonObject, HttpResponseCallback callback) {
        executeRequest(Method.POST, context, null, jsonObject, url, Response.JSON, callback);
    }

    public static boolean hasConnection(Context context) {
        ConnectivityManager connMgr = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connMgr.getActiveNetworkInfo();
        return networkInfo != null && networkInfo.isConnected();
    }

    public static boolean canUpload(Context context, Intent intent) {
        ConnectivityManager connMgr = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connMgr.getActiveNetworkInfo();

        if (networkInfo == null || !networkInfo.isConnectedOrConnecting() || intent == null) {
            return false;
        }

        boolean isWiFi = networkInfo.getType() == ConnectivityManager.TYPE_WIFI;

        IntentFilter iFilter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
        Intent batteryStatus = context.registerReceiver(null, iFilter);

        int status = batteryStatus.getIntExtra(BatteryManager.EXTRA_STATUS, -1);

        boolean isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
                status == BatteryManager.BATTERY_STATUS_FULL;

        return isCharging && (isWiFi || !BuildConfig.requireWifiUpload);
    }

    public static void post(final Context context, final String url, final List<NameValuePair> params, final File file, final HttpResponseCallback callback) {
        new AsyncTask<Void, Void, Void>() {

            @Override
            protected Void doInBackground(Void... unused) {
                try {
                    MultipartUtility request = new MultipartUtility(BuildConfig.serverUrl + url, "UTF-8", Globals.getAccessToken(context));
                    String token = Globals.getAccessToken(context);
                    if (token != null) {
                        request.addHeaderField("Authorization", token);
                    }
                    for (NameValuePair pair : params) {
                        request.addFormField(pair.getName(), pair.getValue());
                    }
                    request.addFilePart("video", file);

                    request.finish();  //send the video to the server

                    callback.success(new JSONObject());
                } catch (IOException e) {
                    callback.failure(500);
                }
                return null;
            }
        }.execute();
    }

    public static void delete(final Context context, final String url, final HttpResponseCallback callback) {
        executeRequest(Method.DELETE, context, null, null, url, Response.JSON, callback);
    }

    private static Void executeRequest(final Method method, final Context context, final List<NameValuePair> params, final Object jsonObject, final String url, final Response type, final HttpResponseCallback callback) {
        if (!hasConnection(context)) {
            if (callback != null) {
                callback.noConnection();
            }
        }
        new AsyncTask<Void, Void, Void>() {

            private JSONObject response = null;
            public int statusCode;

            @Override
            protected Void doInBackground(Void... args) {
                disableConnectionReuseIfNecessary();
                OutputStream os = null;
                BufferedWriter writer = null;

                HttpURLConnection urlConnection = null;

                try {
                    URL urlToRequest = new URL(Globals.SERVER_URL + url);
                    urlConnection = (HttpURLConnection) urlToRequest.openConnection();

                    if (method.equals(Method.POST)) {
                        urlConnection.setDoOutput(true);
                    } else if (method.equals(Method.DELETE)) {
                        urlConnection.setRequestMethod("DELETE");
                    }


                    String charset = "UTF-8";
                    String token = Globals.getAccessToken(context);
                    if (token != null) {
                        urlConnection.setRequestProperty("Authorization", token);
                    }
                    urlConnection.setRequestProperty("Accept-Charset", charset);

                    urlConnection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded;charset=" + charset);

                    urlConnection.setConnectTimeout(CONNECTION_TIMEOUT);
                    urlConnection.setReadTimeout(DATA_RETRIEVAL_TIMEOUT);


                    if (params != null) {
                        Log.d("log1-app", "p1");
                        os = urlConnection.getOutputStream();
                        writer = new BufferedWriter(
                                new OutputStreamWriter(os, "UTF-8"));
                        writer.write(getQuery(params));
                        writer.flush();
                        writer.close();
                        os.close();
                    } else if (jsonObject != null) {
                        urlConnection.setRequestProperty("Content-Type", "application/json;charset=" + charset);
                        os = urlConnection.getOutputStream();
                        writer = new BufferedWriter(
                                new OutputStreamWriter(os, "UTF-8"));
                        writer.write(jsonObject.toString());
                        writer.flush();
                    }
                    // handle issues


                    urlConnection.connect();
                    statusCode = urlConnection.getResponseCode();
                    if (statusCode == HttpURLConnection.HTTP_UNAUTHORIZED) {
                        callback.unauthorized();
                        return null;
                    } else if (statusCode != HttpURLConnection.HTTP_OK) {
                        callback.failure(statusCode);
                        return null;
                    }

                    if (type.equals(Response.JSON)){
                        InputStream in = new BufferedInputStream(
                                urlConnection.getInputStream());

                        String responseText = getResponseText(in);
                        try {
                            JSONObject response = new JSONObject(responseText);
                            callback.success(response);
                        } catch (JSONException ex) {
                            callback.success((JSONObject)null);
                        }
                    } else {
                        byte[] buffer = new byte[8192];
                        int bytesRead;
                        ByteArrayOutputStream output = new ByteArrayOutputStream();
                        InputStream inputStream = urlConnection.getInputStream();
                        while ((bytesRead = inputStream.read(buffer)) != -1)
                        {
                            output.write(buffer, 0, bytesRead);
                        }

                        callback.success( output.toByteArray());
                    }


                } catch (MalformedURLException e) {
                    callback.badRequest();
                    Log.e(TAG, "Url error ", e);
                } catch (SocketTimeoutException e) {
                    callback.badConnection();
                    Log.e(TAG, "Timeout error ", e);
                } catch (IOException e) {
                    callback.badResponse();
                    Log.e(TAG, "Could not read response body ", e);
                } finally {
                    try {
                        if (writer != null) {
                            writer.close();
                        }
                        if (os != null) {
                            os.close();
                        }
                    } catch (IOException e) {
                    }

                    if (urlConnection != null) {
                        urlConnection.disconnect();
                    }
                }
                return null;
            }


            @Override
            protected void onPostExecute(Void unused) {
                if (statusCode == HttpURLConnection.HTTP_UNAUTHORIZED) {
                    callback.unauthorized();
                } else if (statusCode != HttpURLConnection.HTTP_OK) {
                    callback.failure(statusCode);
                }
            }
        }.execute();
        return null;
    }


    enum Method {
        POST, DELETE, GET;
    }

    public enum Response {BYTEARRAY, JSON}
}