package org.igarape.copcast.utils;

/**
 * Created by martelli on 12/8/15.
 */
public enum TextFileType {

    BATTERY("battery.txt", "/batteries"),
    INCIDENTS("incidents.txt", "/incidents"),
    HISTORY("history.txt", "/histories"),
    LOCATIONS("locations.txt", "/locations");

    private final String name;
    private final String url;
    TextFileType(String name, String url) {
        this.name = name;
        this.url = url;
    }
    
    public String getName() {
        return name;
    }

    public String getUrl() {
        return url;
    }
}

