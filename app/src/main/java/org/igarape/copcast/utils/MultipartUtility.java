package org.igarape.copcast.utils;

import android.util.Log;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.List;

/**
 * This utility class provides an abstraction layer for sending multipart HTTP
 * POST requests to a web server.
 *
 * @author www.codejava.net
 */
public class MultipartUtility {
    private final String boundary;
    private static final String LINE_FEED = "\r\n";
    private HttpURLConnection httpConn;
    private String charset;
    private OutputStream outputStream;
    private PrintWriter writer;

    /**
     * This constructor initializes a new HTTP POST request with content type
     * is set to multipart/form-data
     *
     * @param requestURL
     * @param charset
     * @throws java.io.IOException
     */
    public MultipartUtility(String requestURL, String charset, String token)
            throws IOException {
        this.charset = charset;

        // creates a unique boundary based on time stamp
        boundary = "===" + System.currentTimeMillis() + "===";

        URL url = new URL(requestURL);
        httpConn = (HttpURLConnection) url.openConnection();
        httpConn.setUseCaches(false);
        httpConn.setDoOutput(true); // indicates POST method
        httpConn.setDoInput(true);

        httpConn.setChunkedStreamingMode(0);
        httpConn.setRequestProperty("Connection", "Keep-Alive");
        //httpConn.setRequestMethod("POST");

        httpConn.setRequestProperty("Content-Type",
                "multipart/form-data; boundary=" + boundary);


        if (token != null) {
            httpConn.setRequestProperty("Authorization", token);
        }
        outputStream = httpConn.getOutputStream();
        writer = new PrintWriter(new OutputStreamWriter(outputStream, charset),
                true);
    }

    /**
     * Adds a form field to the request
     *
     * @param name  field name
     * @param value field value
     */
    public void addFormField(String name, String value) {
        writer.append("--").append(boundary).append(LINE_FEED);
        writer.append("Content-Disposition: form-data; name=\"").append(name).append("\"")
                .append(LINE_FEED);
        writer.append("Content-Type: text/plain; charset=").append(charset).append(
                LINE_FEED);
        writer.append(LINE_FEED);
        writer.append(value).append(LINE_FEED);
        writer.flush();
    }

    /**
     * Adds a upload file section to the request
     *
     * @param fieldName  name attribute in <input type="file" name="..." />
     * @param uploadFile a File to be uploaded
     * @throws IOException
     */
    public void addFilePart(String fieldName, File uploadFile)
            throws IOException {
        String fileName = uploadFile.getName();
        writer.append("--").append( boundary).append(LINE_FEED);
        writer.append(
                "Content-Disposition: form-data; name=\"").append(fieldName).append(
                        "\"; filename=\"").append(fileName).append("\"")
                .append(LINE_FEED);
        writer.append(
                "Content-Type: ").append(URLConnection.guessContentTypeFromName(fileName))
                .append(LINE_FEED);
        writer.append("Content-Transfer-Encoding: binary").append(LINE_FEED);
        writer.append(LINE_FEED);
        writer.flush();

        FileInputStream inputStream = new FileInputStream(uploadFile);
        byte[] buffer = new byte[4096];
        int bytesRead = -1;
        while ((bytesRead = inputStream.read(buffer)) != -1) {
            outputStream.write(buffer, 0, bytesRead);
        }
        outputStream.flush();
        inputStream.close();

        writer.append(LINE_FEED);
        writer.flush();
    }

    /**
     * Adds a header field to the request.
     *
     * @param name  - name of the header field
     * @param value - value of the header field
     */
    public void addHeaderField(String name, String value) {
        writer.append(name).append( ": ").append(value).append(LINE_FEED);
        writer.flush();
    }

    /**
     * Completes the request and receives response from the server.
     *
     * @return a list of Strings as response in case the server returned
     * status OK, otherwise an exception is thrown.
     * @throws IOException
     */
    public List<String> finish() throws IOException {
        List<String> response = new ArrayList<String>();

        Log.d("MultipartUtility", "M1");
        writer.append(LINE_FEED).flush();
        Log.d("MultipartUtility", "M1");
        writer.append("--").append(boundary).append("--").append(LINE_FEED);
        Log.d("MultipartUtility", "M1");
        writer.close();
        Log.d("MultipartUtility", "M2");

        // checks server's status code first
        int status = httpConn.getResponseCode();
        Log.d("MultipartUtility", "M3");
        if (status == HttpURLConnection.HTTP_OK || status == HttpURLConnection.HTTP_CREATED) {
            BufferedReader reader = new BufferedReader(new InputStreamReader(
                    httpConn.getInputStream()));
            Log.d("MultipartUtility", "M4");
            String line = null;
            Log.d("MultipartUtility", "M5");
            while ((line = reader.readLine()) != null) {
                response.add(line);
            }
            Log.d("MultipartUtility", "M6");
            reader.close();
            httpConn.disconnect();
            Log.d("MultipartUtility", "M7");
        } else {
            Log.d("MultipartUtility", "M8");
            throw new IOException("Server returned non-OK status: " + status);
        }
        Log.d("MultipartUtility", "M9");

        return response;
    }
}
