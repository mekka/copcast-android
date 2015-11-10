package org.igarape.copcast.bo;

import java.io.Serializable;
import java.util.Date;

/**
 * Created by alex on 10/21/15.
 */
public class IncidentForm implements Serializable {
    public Date getDate() {
        return date;
    }

    public void setDate(Date date) {
        this.date = date;
    }

    public float getLat() {
        return lat;
    }

    public void setLat(float lat) {
        this.lat = lat;
    }

    public float getLng() {
        return lng;
    }

    public void setLng(float lng) {
        this.lng = lng;
    }

    public boolean isAccident() {
        return accident;
    }

    public void setAccident(boolean accident) {
        this.accident = accident;
    }

    public int getGravity() {
        return gravity;
    }

    public void setGravity(int gravity) {
        this.gravity = gravity;
    }

    public int getInjured() {
        return injured;
    }

    public void setInjured(int injured) {
        this.injured = injured;
    }

    public boolean isFine() {
        return fine;
    }

    public void setFine(boolean fine) {
        this.fine = fine;
    }

    public String getFineType() {
        return fineType;
    }

    public void setFineType(String fineType) {
        this.fineType = fineType;
    }

    public boolean isArrest() {
        return arrest;
    }

    public void setArrest(boolean arrest) {
        this.arrest = arrest;
    }

    public boolean isResistance() {
        return resistance;
    }

    public void setResistance(boolean resistance) {
        this.resistance = resistance;
    }

    public boolean isArgument() {
        return argument;
    }

    public void setArgument(boolean argument) {
        this.argument = argument;
    }

    public boolean isUseOfForce() {
        return useOfForce;
    }

    public void setUseOfForce(boolean useOfForce) {
        this.useOfForce = useOfForce;
    }

    public boolean isUseLethalForce() {
        return useLethalForce;
    }

    public void setUseLethalForce(boolean useLethalForce) {
        this.useLethalForce = useLethalForce;
    }

    public long getUserId() {
        return userId;
    }

    public void setUserId(long userId) {
        this.userId = userId;
    }


    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    private Date date;
    private float lat;
    private float lng;
    private boolean accident;
    private int gravity;
    private int injured;
    private boolean fine;
    private String fineType;
    private boolean arrest;
    private boolean resistance;
    private boolean argument;
    private boolean useOfForce;
    private boolean useLethalForce;


    private long userId;


    private String address;

}
