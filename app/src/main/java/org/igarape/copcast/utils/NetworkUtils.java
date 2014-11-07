package org.igarape.copcast.utils;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Build;
import android.util.Log;

import org.apache.http.NameValuePair;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
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
    private static int CONNECTION_TIMEOUT = 15000 ;
    private static int DATARETRIEVAL_TIMEOUT = 5000;
    private static final char PARAMETER_DELIMITER = '&';
    private static final char PARAMETER_EQUALS_CHAR = '=';
    private static Context appContext;

    private static String getQuery(List<NameValuePair> params) throws UnsupportedEncodingException
    {
        StringBuilder result = new StringBuilder();
        boolean first = true;

        for (NameValuePair pair : params)
        {
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

    private static String getResponseText(InputStream inStream) {
        // very nice trick from
        // http://weblogs.java.net/blog/pat/archive/2004/10/stupid_scanner_1.html
        return new Scanner(inStream).useDelimiter("\\A").next();
    }

    public static void post(final Context context, final String url, final List<NameValuePair> params, final HttpResponseCallback callback) {
        if (!hasConnection(context)){
            if (callback != null){
                callback.noConnection();
            }
        }
        AsyncTask task = new AsyncTask<Void, Void, Void>() {

            private JSONObject response = null;
            public int statusCode;

            @Override
            protected Void doInBackground(Void... unused) {
                final String regId = Globals.getRegistrationId(context);

                disableConnectionReuseIfNecessary();

                HttpURLConnection urlConnection = null;
                try {
                    URL urlToRequest = new URL(Globals.SERVER_URL + url);
                    urlConnection = (HttpURLConnection) urlToRequest.openConnection();
                    urlConnection.setDoOutput(true);


                    String charset = "UTF-8";
                    String token = Globals.getAccessToken(context);
                    if (token != null){
                        urlConnection.setRequestProperty("Authorization", token);
                    }
                    urlConnection.setRequestProperty("Accept-Charset", charset);
                    urlConnection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded;charset=" + charset);

                    urlConnection.setConnectTimeout(CONNECTION_TIMEOUT);
                    urlConnection.setReadTimeout(DATARETRIEVAL_TIMEOUT);



                    if (params != null) {
                        OutputStream os = urlConnection.getOutputStream();
                        BufferedWriter writer = new BufferedWriter(
                                new OutputStreamWriter(os, "UTF-8"));
                        writer.write(getQuery(params));
                        writer.flush();
                        writer.close();
                        os.close();
                    }

                    // handle issues
                    urlConnection.connect();
                    statusCode = urlConnection.getResponseCode();
                    if (statusCode == HttpURLConnection.HTTP_OK) {
                        // create JSON object from content
                        InputStream in = new BufferedInputStream(
                                urlConnection.getInputStream());
                        response = new JSONObject(getResponseText(in));
                    }

                } catch (MalformedURLException e) {
                    Log.e(TAG, "Url error ", e);
                } catch (SocketTimeoutException e) {
                    Log.e(TAG, "Timeout error ", e);
                } catch (IOException e) {
                    Log.e(TAG, "Could not read response body ", e);
                } catch (JSONException e) {
                    Log.e(TAG, "Invalid json ", e);
                } finally {
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
                } else {
                    callback.success(response);
                }
            }
        }.execute();
    }

    public static void post(final Context context, final String url, final JSONObject jsonObject, final HttpResponseCallback callback) {
        if (!hasConnection(context)){
            if (callback != null){
                callback.noConnection();
            }
        }
        AsyncTask task = new AsyncTask<Void, Void, Void>() {

            @Override
            protected Void doInBackground(Void... unused) {
                JSONObject response = null;
                final String regId = Globals.getRegistrationId(context);

                disableConnectionReuseIfNecessary();

                HttpURLConnection urlConnection = null;
                try {
                    URL urlToRequest = new URL(Globals.SERVER_URL + url);
                    urlConnection = (HttpURLConnection) urlToRequest.openConnection();
                    urlConnection.setDoOutput(true);


                    String charset = "UTF-8";
                    String token = Globals.getAccessToken(context);
                    if (token != null){
                        urlConnection.setRequestProperty("Authorization", token);
                    }
                    urlConnection.setRequestProperty("Accept-Charset", charset);
                    urlConnection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded;charset=" + charset);

                    urlConnection.setConnectTimeout(CONNECTION_TIMEOUT);
                    urlConnection.setReadTimeout(DATARETRIEVAL_TIMEOUT);


                    if (jsonObject != null) {
                        //urlConnection.setRequestProperty("Content-Length", String.valueOf(jsonObject.length()));
                        OutputStream os = urlConnection.getOutputStream();
                        os.write(jsonObject.toString().getBytes(charset));
                    }

                    // handle issues
                    urlConnection.connect();
                    int statusCode = urlConnection.getResponseCode();
                    if (statusCode == HttpURLConnection.HTTP_UNAUTHORIZED) {
                        callback.unauthorized();
                        return null;
                    } else if (statusCode != HttpURLConnection.HTTP_OK) {
                        callback.failure(statusCode);
                        return null;
                    }


                    InputStream in = new BufferedInputStream(
                            urlConnection.getInputStream());
                    String responseText = getResponseText(in);

                    response = new JSONObject(responseText);


                    callback.success(response);
                } catch (MalformedURLException e) {
                    callback.badRequest();
                    Log.e(TAG, "Url error ", e);
                } catch (SocketTimeoutException e) {
                    callback.badConnection();
                    Log.e(TAG, "Timeout error ", e);
                } catch (IOException e) {
                    callback.badResponse();
                    Log.e(TAG, "Could not read response body ", e);
                } catch (JSONException e) {

                    return null;
                } finally {
                    if (urlConnection != null) {
                        urlConnection.disconnect();
                    }
                }
                return null;
            }

            @Override
            protected void onPostExecute(Void unused) {

            }
        }.execute();
    }

    public static void setAppContext(Context appContext) {
        NetworkUtils.appContext = appContext;
    }

    public static boolean hasConnection(Context context) {
        ConnectivityManager connMgr = (ConnectivityManager)context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connMgr.getActiveNetworkInfo();
        return networkInfo != null && networkInfo.isConnected();
    }
}
