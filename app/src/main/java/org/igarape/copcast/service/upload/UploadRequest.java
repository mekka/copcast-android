package org.igarape.copcast.service.upload;

import android.content.Context;

import android.content.Context;
import android.content.Intent;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

/**
 * Represents an upload request.
 * 
 * @author alexbbb (Alex Gotev)
 * @author eliasnaur
 * 
 */
public class UploadRequest {

    private String method = "POST";
    private final Context context;
    private String customUserAgent;
    private int maxRetries;
    private final String uploadId;
    private ArrayList<FileToUpload> filesToUpload;
    private final ArrayList<NameValue> headers;
    private final ArrayList<NameValue> parameters;

    /**
     * Creates a new upload request.
     * 
     * @param context application context
     * @param uploadId unique ID to assign to this upload request. It's used in the broadcast receiver when receiving
     * updates.
     */
    public UploadRequest(final Context context, final String uploadId) {
        this.context = context;
        this.uploadId = uploadId;
        filesToUpload = new ArrayList<>();
        headers = new ArrayList<>();
        parameters = new ArrayList<>();
        maxRetries = 0;
    }

    /**
     * Adds a file to this upload request.
     * 
     * @param path Absolute path to the file that you want to upload
     * @param parameterName Name of the form parameter that will contain file's data
     * @param fileName File name seen by the server side script
     */
    public void addFileToUpload(final String url, final String path, final String parameterName, final String fileName) {
        filesToUpload.add(new FileToUpload(url, path, parameterName, fileName, ContentType.VIDEO_MPEG));
    }

    /**
     * Adds a header to this upload request.
     * 
     * @param headerName header name
     * @param headerValue header value
     */
    public void addHeader(final String headerName, final String headerValue) {
        headers.add(new NameValue(headerName, headerValue));
    }

    /**
     * Adds a parameter to this upload request.
     * 
     * @param paramName parameter name
     * @param paramValue parameter value
     */
    public void addParameter(final String paramName, final String paramValue) {
        parameters.add(new NameValue(paramName, paramValue));
    }

    /**
     * Adds a parameter with multiple values to this upload request.
     * 
     * @param paramName parameter name
     * @param array values
     */
    public void addArrayParameter(final String paramName, final String... array) {
        for (String value : array) {
            parameters.add(new NameValue(paramName, value));
        }
    }

    /**
     * Adds a parameter with multiple values to this upload request.
     * 
     * @param paramName parameter name
     * @param list values
     */
    public void addArrayParameter(final String paramName, final List<String> list) {
        for (String value : list) {
            parameters.add(new NameValue(paramName, value));
        }
    }

    /**
     * Sets the HTTP method to use. By default it's set to POST.
     * 
     * @param method new HTTP method to use
     */
    public void setMethod(final String method) {
        if (method != null && method.length() > 0)
            this.method = method;
    }

    /**
     * Gets the HTTP method to use.
     * 
     * @return
     */
    protected String getMethod() {
        return method;
    }

    /**
     * Gets the upload ID of this request.
     * 
     * @return
     */
    public String getUploadId() {
        return uploadId;
    }

    /**
     * Gets the list of the files that has to be uploaded.
     * 
     * @return
     */
    public ArrayList<FileToUpload> getFilesToUpload() {
        return filesToUpload;
    }

    public void setFilesToUpload(ArrayList<FileToUpload> filesToUpload) {
        this.filesToUpload = filesToUpload;
    }

    /**
     * Gets the list of the headers.
     * 
     * @return
     */
    protected ArrayList<NameValue> getHeaders() {
        return headers;
    }

    /**
     * Gets the list of the parameters.
     * 
     * @return
     */
    protected ArrayList<NameValue> getParameters() {
        return parameters;
    }

    /**
     * Gets the application context.
     * 
     * @return
     */
    protected Context getContext() {
        return context;
    }

    /**
     * Gets the custom user agent defined for this upload request.
     * 
     * @return string representing the user agent or null if it's not defined
     */
    public final String getCustomUserAgent() {
        return customUserAgent;
    }

    /**
     * Sets the custom user agent to use for this upload request. Note! If you set the "User-Agent" header by using the
     * "addHeader" method, that setting will be overwritten by the value set with this method.
     * 
     * @param customUserAgent custom user agent string
     */
    public final void setCustomUserAgent(String customUserAgent) {
        this.customUserAgent = customUserAgent;
    }

    /**
     * Get the maximum number of retries that the library will do if an error occurs, before returning an error.
     * 
     * @return
     */
    public final int getMaxRetries() {
        return maxRetries;
    }

    /**
     * Sets the maximum number of retries that the library will do if an error occurs, before returning an error.
     * 
     * @param maxRetries
     */
    public final void setMaxRetries(int maxRetries) {
        if (maxRetries < 0)
            this.maxRetries = 0;
        else
            this.maxRetries = maxRetries;
    }

    @Override
    public String toString() {
        StringBuffer str = new StringBuffer();
        str.append("Headers: "+this.headers+"\n");
        if (filesToUpload != null)
            for(FileToUpload f: filesToUpload)
                str.append("File: "+f.getFileName()+" -> "+f.getUrl()+"\n");
        str.append("Retries: "+this.maxRetries+"\n");
        return str.toString();
    }
}
