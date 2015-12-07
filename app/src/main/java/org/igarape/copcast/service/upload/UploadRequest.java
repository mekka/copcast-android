package org.igarape.copcast.service.upload;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import android.content.Context;
import android.content.Intent;

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
    private final String url;
    private final ArrayList<FileToUpload> filesToUpload;
    private final ArrayList<NameValue> headers;
    private final ArrayList<NameValue> parameters;

    /**
     * Creates a new upload request.
     * 
     * @param context application context
     * @param uploadId unique ID to assign to this upload request. It's used in the broadcast receiver when receiving
     * updates.
     * @param serverUrl URL of the server side script that handles the multipart form upload
     */
    public UploadRequest(final Context context, final String uploadId, final String serverUrl) {
        this.context = context;
        this.uploadId = uploadId;
        url = serverUrl;
        filesToUpload = new ArrayList<>();
        headers = new ArrayList<>();
        parameters = new ArrayList<>();
        maxRetries = 0;
    }

    /**
     * Validates the upload request and throws exceptions if one or more parameters are not properly set.
     * 
     * @throws IllegalArgumentException if request protocol or URL are not correctly set
     * @throws MalformedURLException if the provided server URL is not valid
     */
    public void validate() throws IllegalArgumentException, MalformedURLException {
        if (url == null || "".equals(url)) {
            throw new IllegalArgumentException("Request URL cannot be either null or empty");
        }

        if (!url.startsWith("http")) {
            throw new IllegalArgumentException("Specify either http:// or https:// as protocol");
        }

        // Check if the URL is valid
        new URL(url);

        if (filesToUpload.isEmpty()) {
            throw new IllegalArgumentException("You have to add at least one file to upload");
        }
    }

    /**
     * Adds a file to this upload request.
     * 
     * @param path Absolute path to the file that you want to upload
     * @param parameterName Name of the form parameter that will contain file's data
     * @param fileName File name seen by the server side script
     * @param contentType Content type of the file. Set this to null if you don't want to set a content type.
     */
    public void addFileToUpload(final String path, final String parameterName, final String fileName,
                                final String contentType) {
        filesToUpload.add(new FileToUpload(path, parameterName, fileName, contentType));
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
     * Gets the URL of the server side script that will handle the multipart form upload.
     * 
     * @return
     */
    protected String getServerUrl() {
        return url;
    }

    /**
     * Gets the list of the files that has to be uploaded.
     * 
     * @return
     */
    protected ArrayList<FileToUpload> getFilesToUpload() {
        return filesToUpload;
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

}
