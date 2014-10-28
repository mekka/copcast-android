package com.igarape.copcast;

import android.app.Application;
import android.content.res.Configuration;

/**
 * Created by fcavalcanti on 27/10/2014.
 */
public class CopCastApplication extends Application {

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
    }

    @Override
    public void onCreate() {
        super.onCreate();
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
    }

    @Override
    public void onTerminate() {
        super.onTerminate();
    }
}
