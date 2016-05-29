package org.igarape.copcast.utils;

import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.BatteryManager;
import android.os.Build;
import android.text.TextUtils;
import android.util.Log;
import android.util.Pair;

import org.igarape.copcast.promises.HttpPromiseError;
import org.igarape.copcast.promises.Promise;
import org.igarape.copcast.promises.PromisePayload;
import org.igarape.copcast.service.sign.SigningService;
import org.igarape.copcast.service.sign.SigningServiceException;
import org.igarape.copcast.state.NetworkState;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
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
import java.util.ArrayList;
import java.util.List;

/**
 * Created by fcavalcanti on 31/10/2014.
 */

public class NetworkUtils {

    private static final String TAG = NetworkUtils.class.getName();
    private static int CONNECTION_TIMEOUT = 15000;
    private static int DATA_RETRIEVAL_TIMEOUT = 5000;

    private static String getQuery(List<Pair<String, String>> params) throws UnsupportedEncodingException {
        ArrayList<String> tokens = new ArrayList();
        boolean first = true;

        for (Pair<String, String> pair : params) {
            tokens.add(URLEncoder.encode(pair.first, "UTF-8") + "=" + URLEncoder.encode(pair.second, "UTF-8"));
        }

        return TextUtils.join("&", tokens);
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
        ArrayList<String> stringList = new ArrayList<>();
        while ((line = br.readLine()) != null) {
            sb.append(line).append("\n");
            stringList.add(line);
        }
        br.close();
        return TextUtils.join("\n", stringList);
//        return sb.toString();
    }

    public static void get(Context context, String url, Promise callback) {
        get(context, url, Response.JSON, callback);
    }

    public static void get(Context context, String url, Response type, Promise callback) {
        _executeRequest(null, Method.GET, context, null, null, url, type, callback);
    }

    public static void post(Context context, String url, List<Pair<String, String>> params, Promise callback) {
        _executeRequest(null, Method.POST, context, params, null, url, Response.JSON, callback);
    }

    public static void post(Context context, String url, JSONObject jsonObject, Promise callback) {
        _executeRequest(null, Method.POST, context, null, jsonObject, url, Response.JSON, callback);
    }
    public static void postToServer(Context context, String server, String url, JSONObject jsonObject, Promise callback) {
        _executeRequest(server, Method.POST, context, null, jsonObject, url, Response.JSON, callback);
    }

    public static boolean hasConnection(Context context) {
        ConnectivityManager connMgr = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connMgr.getActiveNetworkInfo();
        return networkInfo != null && networkInfo.isConnected();
    }

    public static NetworkState checkUploadState(Context context) {
        ConnectivityManager connMgr = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connMgr.getActiveNetworkInfo();

        if (networkInfo == null || !networkInfo.isConnectedOrConnecting()) {
            return NetworkState.NO_NETWORK;
        }

        boolean isWiFi = networkInfo.getType() == ConnectivityManager.TYPE_WIFI;

        IntentFilter iFilter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
        // intent is immediately returned, as ACTION_BATTERY_CHANGED is sticky.
        Intent batteryStatus = context.registerReceiver(null, iFilter);
        if (batteryStatus == null){
            // in the case we don't receive any info, treat as worst case.
            return NetworkState.NOT_CHARGING;
        }
        int status = batteryStatus.getIntExtra(BatteryManager.EXTRA_STATUS, -1);

        boolean isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
                status == BatteryManager.BATTERY_STATUS_FULL;

        if (!isCharging)
            return NetworkState.NOT_CHARGING;

        if (Globals.isWifiOnly(context) && !isWiFi)
            return NetworkState.WIFI_REQUIRED;

        return NetworkState.NETWORK_OK;
    }

    private static void _executeRequest(final String serverUri, final Method method, final Context context, final List<Pair<String,String>> params, final JSONObject jsonObject, final String url, final Response type, final Promise callback) {
        if (!hasConnection(context)) {
            if (callback != null) {
                callback.error(HttpPromiseError.NO_CONNECTION);
                return;
            }
        }

        final String token = Globals.getAccessToken(context);
        final String pServerUri = serverUri != null ? serverUri : Globals.getServerUrl(context);

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
                    URL urlToRequest = new URL(pServerUri + url);
                    Log.d(TAG, urlToRequest.toString());
                    urlConnection = (HttpURLConnection) urlToRequest.openConnection();

                    if (method.equals(Method.POST)) {
                        urlConnection.setDoOutput(true);
                    } else if (method.equals(Method.DELETE)) {
                        urlConnection.setRequestMethod("DELETE");
                    }


                    String charset = "UTF-8";
                    if (token != null) {
                        urlConnection.setRequestProperty("Authorization", token);
                    }
                    urlConnection.setRequestProperty("Accept-Charset", charset);
                    urlConnection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded;charset=" + charset);
                    urlConnection.setConnectTimeout(CONNECTION_TIMEOUT);
                    urlConnection.setReadTimeout(DATA_RETRIEVAL_TIMEOUT);


                    if (params != null) {
                        os = urlConnection.getOutputStream();
                        writer = new BufferedWriter(
                                new OutputStreamWriter(os, "UTF-8"));
                        writer.write(getQuery(params));
                        writer.flush();
                        writer.close();
                        os.close();
                    } else if (jsonObject != null) {
                        jsonObject.put("imei", Globals.getImei(context));
                        jsonObject.put("simid", Globals.getSimid(context));
                        Log.d(TAG, jsonObject.toString());
                        String signature = SigningService.signature(jsonObject);
                        jsonObject.put("mac", signature);

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
                        callback.error(HttpPromiseError.NOT_AUTHORIZED);
                        return null;
                    } else if (statusCode == HttpURLConnection.HTTP_FORBIDDEN) {
                        callback.error(HttpPromiseError.FORBIDDEN);
                        return null;
                    } else if (statusCode == HttpURLConnection.HTTP_BAD_REQUEST) {
                        callback.error(HttpPromiseError.BAD_REQUEST);
                        return null;
                    } else if (statusCode != HttpURLConnection.HTTP_OK && statusCode != HttpURLConnection.HTTP_CREATED) {
//                        callback.failure(statusCode);
                        callback.error(HttpPromiseError.FAILURE.put("statusCode", statusCode));
                        return null;
                    }

                    if (statusCode == HttpURLConnection.HTTP_OK) {
                        if (type.equals(Response.JSON)) {
                            InputStream in = new BufferedInputStream(
                                    urlConnection.getInputStream());
                            String responseText = getResponseText(in);
                            try {
                                JSONObject response = new JSONObject(responseText);
                                callback.success(new PromisePayload("response", response));
                            } catch (JSONException ex) {
                                callback.success();
                            }
                        } else if (type.equals(Response.BYTEARRAY)) {
                            byte[] buffer = new byte[8192];
                            int bytesRead;
                            ByteArrayOutputStream output = new ByteArrayOutputStream();
                            InputStream inputStream = urlConnection.getInputStream();
                            while ((bytesRead = inputStream.read(buffer)) != -1) {
                                output.write(buffer, 0, bytesRead);
                            }
                            callback.success(new PromisePayload("response", output.toByteArray()));
                        }
                    } else {
                        Log.d(TAG, "returning sucess");
                        callback.success();
                    }
                } catch (MalformedURLException e) {
                    callback.error(HttpPromiseError.BAD_REQUEST);
                    Log.e(TAG, "Url error ", e);
                } catch (SocketTimeoutException e) {
                    callback.error(HttpPromiseError.BAD_CONNECTION);
                    Log.e(TAG, "Timeout error ", e);
                } catch (IOException e) {
                    callback.error(HttpPromiseError.BAD_RESPONSE);
                    Log.e(TAG, "Could not read response body ", e);
                } catch (SigningServiceException e) {
                    callback.error(HttpPromiseError.SIGNING_ERROR);
                } catch (JSONException e) {
                    callback.error(HttpPromiseError.JSON_ERROR);
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
        }.execute();
    }

    public static String getConnectionType(Context context) {
        ConnectivityManager connMgr = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connMgr.getActiveNetworkInfo();

        if (networkInfo == null || !networkInfo.isConnectedOrConnecting()) {
            return null;
        } else if (networkInfo.getType() == ConnectivityManager.TYPE_WIFI){
            return "wifi";
        } else {
            return "mobile";
        }
    }


    enum Method {
        POST, DELETE, GET
    }

    public enum Response {
        BYTEARRAY, JSON
    }
}
