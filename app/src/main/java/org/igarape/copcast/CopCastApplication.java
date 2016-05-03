package org.igarape.copcast;

import android.app.Application;
import android.content.res.Configuration;
import android.os.Environment;
import android.util.Log;
import android.widget.Toast;

import org.igarape.copcast.utils.FileUtils;
import org.igarape.copcast.utils.Globals;

import java.io.File;
import java.io.IOException;

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
        Globals.sessionInit();
        Globals.initStateManager(this);
        FileUtils.init(getApplicationContext());
        Globals.setAccessToken(this, null);
        Globals.setDirectorySize(getApplicationContext(), FileUtils.getDirectorySize());

        String fileName = "logcat_"+System.currentTimeMillis()+".txt";
        File outputFile = new File(Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_PICTURES),fileName);
        try {
            @SuppressWarnings("unused")
            Process process = Runtime.getRuntime().exec("logcat -f "+outputFile.getAbsolutePath());
            Log.e("DEBUG", outputFile.getAbsolutePath());
        } catch (IOException e) {
            Toast.makeText(this, "Logcat error", Toast.LENGTH_LONG).show();
        }

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
