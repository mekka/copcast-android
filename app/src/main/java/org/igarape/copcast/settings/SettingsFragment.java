package org.igarape.copcast.settings;

import android.os.Bundle;
import android.preference.EditTextPreference;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.widget.Toast;

import org.igarape.copcast.R;
import org.igarape.copcast.utils.Globals;

/**
 * Created by brunosiqueira on 27/08/15.
 */
public class SettingsFragment extends PreferenceFragment {
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Load the preferences from an XML resource
        addPreferencesFromResource(R.xml.preferences);

        EditTextPreference serverUrlText = (EditTextPreference) findPreference("server_url");
        if (Globals.getAccessToken(getActivity().getApplicationContext()) != null){
            serverUrlText.setEnabled(false);
            serverUrlText.setSummary(getText(R.string.serverurl_disabled_logged));
        }
    }
}
