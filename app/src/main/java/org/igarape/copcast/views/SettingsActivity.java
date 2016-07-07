package org.igarape.copcast.views;

import android.app.Activity;
import android.os.Bundle;

import org.igarape.copcast.settings.SettingsFragment;

/**
 * Created by brunosiqueira on 27/08/15.
 */
public class SettingsActivity extends Activity {
    private static String TAG = SettingsActivity.class.getCanonicalName();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Display the fragment as the main content.
        getFragmentManager().beginTransaction()
                .replace(android.R.id.content, new SettingsFragment())
                .commit();

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }
}
