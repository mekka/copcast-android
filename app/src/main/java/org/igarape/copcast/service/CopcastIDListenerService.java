package org.igarape.copcast.service;

import android.content.Intent;
import android.os.Bundle;

import com.google.android.gms.gcm.GoogleCloudMessaging;
import com.google.android.gms.iid.InstanceID;
import com.google.android.gms.iid.InstanceIDListenerService;

import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;
import org.igarape.copcast.utils.Globals;
import org.igarape.copcast.utils.HttpResponseCallback;
import org.igarape.copcast.utils.NetworkUtils;
import org.igarape.copcast.R;

import java.io.IOException;
import java.util.ArrayList;

/**
 * Created by brunosiqueira on 21/08/15.
 */
public class CopcastIDListenerService extends InstanceIDListenerService {
    public void onTokenRefresh() {
        // Fetch updated Instance ID token and notify our app's server of any changes (if applicable).
        InstanceID instanceID = InstanceID.getInstance(getApplicationContext());
        String regid = null;
        try {
            Bundle bundle = new Bundle();
            bundle.putString("login", Globals.getUserLogin(getApplicationContext()));
            regid = instanceID.getToken(getString(R.string.gcm_defaultSenderId),
                    GoogleCloudMessaging.INSTANCE_ID_SCOPE, bundle);


            Globals.storeRegistrationId(getApplicationContext(), regid);

            ArrayList<NameValuePair> params = new ArrayList<NameValuePair>();
            params.add(new BasicNameValuePair("gcm_registration", regid));
            NetworkUtils.post(getApplicationContext(), "/user/gcm", params, new HttpResponseCallback() {
                @Override
                public void unauthorized() {

                }

                @Override
                public void failure(int statusCode) {

                }

                @Override
                public void noConnection() {

                }

                @Override
                public void badConnection() {

                }

                @Override
                public void badRequest() {

                }

                @Override
                public void badResponse() {

                }
            });
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
