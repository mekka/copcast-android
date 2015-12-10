package org.igarape.copcast.db;

/**
 * Created by martelli on 10/21/15.
 */
public enum JsonDataType {

    TYPE_INCIDENT_FLAG("INCIDENT_FLAG", "/incidents"),
    TYPE_INCIDENT_FORM("INCIDENT_FORM", "/incidentForms"),
    TYPE_FLAGGED_VIDEO("FLAGGED_VIDEO", "/incidents"),
    TYPE_BATTERY_STATUS("BATERRY_STATUS", "/batteries"),
    TYPE_LOCATION_INFO("LOCATION_INFO", "/locations");

    private final String type;
    private final String url;
    JsonDataType(String type, String url) {
        this.type = type;
        this.url = url;
    }

    public String getType() {
        return type;
    }

    public String getUrl() {
        return url;
    }
}