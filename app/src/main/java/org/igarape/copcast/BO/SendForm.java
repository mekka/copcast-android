package org.igarape.copcast.BO;

import java.io.Serializable;

/**
 * Created by alex on 10/21/15.
 */
public class SendForm implements Serializable {

    private String date;
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

}
