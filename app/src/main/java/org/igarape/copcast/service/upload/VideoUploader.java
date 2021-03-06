package org.igarape.copcast.service.upload;

import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;

/**
 * Created by martelli on 12/8/15.
 */
public class VideoUploader {

    private static final String TAG = "AndroidVideoFileUpload";
    private static final int BUFFER_SIZE = 16384;
    private static final String NEW_LINE = "\r\n";
    private static final String TWO_HYPHENS = "--";
    private static volatile boolean shouldContinue = true;

    public static boolean uploadSingleFile(final String method,
                                           final FileToUpload fileToUpload, final ArrayList<NameValue> requestHeaders,
                                           boolean keepalive, final String customUserAgent, UploadService.RunUpload runUpload) throws IOException {

        HttpURLConnection conn = null;
        OutputStream requestStream = null;

        try {
            final String boundary = getBoundary();
            final byte[] boundaryBytes = getBoundaryBytes(boundary);

            // get the content length of the entire HTTP/Multipart request body
            final long totalFileBytes = fileToUpload.getTotalMultipartBytes(boundaryBytes.length);
            final byte[] trailer = getTrailerBytes(boundary);
            final long bodyLength = totalFileBytes + trailer.length;

            if (android.os.Build.VERSION.SDK_INT < 19 && bodyLength > Integer.MAX_VALUE)
                throw new IOException("You need Android API version 19 or newer to "
                        + "upload more than 2GB in a single request using "
                        + "fixed size content length. Try switching to "
                        + "chunked mode instead, but make sure your server side supports it!");

            String url = fileToUpload.getUrl();

            conn = getMultipartHttpURLConnection(url, method, boundary, keepalive);

            if (customUserAgent != null && !customUserAgent.equals("")) {
                requestHeaders.add(new NameValue("User-Agent", customUserAgent));
            }

            setRequestHeaders(conn, requestHeaders);

            conn.setChunkedStreamingMode(1024*BUFFER_SIZE);
            requestStream = conn.getOutputStream();

            requestStream.write(boundaryBytes, 0, boundaryBytes.length);
            byte[] headerBytes = fileToUpload.getMultipartHeader();
            requestStream.write(headerBytes, 0, headerBytes.length);

            final InputStream stream = fileToUpload.getStream();
            byte[] buffer = new byte[BUFFER_SIZE];
            int bytesRead;

            try {
                while ((bytesRead = stream.read(buffer, 0, buffer.length)) > 0 && shouldContinue) {
                    if (runUpload.isCancelled()) {
                        Log.d(TAG, "aborting....");
                        return false;
                    }
                    requestStream.write(buffer, 0, bytesRead);
                    requestStream.flush();
                    runUpload.updateCounter(bytesRead/1024);
                }
            } catch (Exception e) {
                Log.e(TAG, "error piping file bytes");
                throw e;
            } finally {
                closeInputStream(stream);
            }

            requestStream.write(trailer, 0, trailer.length);

            final int serverResponseCode = conn.getResponseCode();
            Log.d(TAG, "return code: "+serverResponseCode);
            return (serverResponseCode / 100 == 2);
        } finally {
            closeOutputStream(requestStream);
            closeConnection(conn);
        }
    }

    private static String getBoundary() {
        final StringBuilder builder = new StringBuilder();

        builder.append("---------------------------").append(System.currentTimeMillis());

        return builder.toString();
    }

    private static byte[] getBoundaryBytes(final String boundary) throws UnsupportedEncodingException {
        final StringBuilder builder = new StringBuilder();

        builder.append(NEW_LINE).append(TWO_HYPHENS).append(boundary).append(NEW_LINE);

        return builder.toString().getBytes("US-ASCII");
    }

    private static byte[] getTrailerBytes(final String boundary) throws UnsupportedEncodingException {
        final StringBuilder builder = new StringBuilder();

        builder.append(NEW_LINE).append(TWO_HYPHENS).append(boundary).append(TWO_HYPHENS).append(NEW_LINE);

        return builder.toString().getBytes("US-ASCII");
    }

    private static HttpURLConnection getMultipartHttpURLConnection(final String url, final String method,
                                                                   final String boundary, boolean keepalive) throws IOException {
        final HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();

        conn.setDoInput(true);
        conn.setDoOutput(true);
        conn.setUseCaches(false);
        conn.setRequestMethod(method);
        if (keepalive) {
            conn.setRequestProperty("Connection", "keep-alive");
        } else {
            conn.setRequestProperty("Connection", "close");
        }
        conn.setRequestProperty("ENCTYPE", "multipart/form-data");
        conn.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + boundary);

        return conn;
    }

    private static void setRequestHeaders(final HttpURLConnection conn, final ArrayList<NameValue> requestHeaders) {
        if (!requestHeaders.isEmpty()) {
            for (final NameValue param : requestHeaders) {
                conn.setRequestProperty(param.getName(), param.getValue());
            }
        }
    }


    private static void closeInputStream(final InputStream stream) {
        if (stream != null) {
            try {
                stream.close();
            } catch (Exception exc) {
            }
        }
    }

    private static void closeOutputStream(final OutputStream stream) {
        if (stream != null) {
            try {
                stream.flush();
                stream.close();
            } catch (Exception exc) {
            }
        }
    }

    private static void closeConnection(final HttpURLConnection connection) {
        if (connection != null) {
            try {
                connection.disconnect();
            } catch (Exception exc) {
            }
        }
    }


}
