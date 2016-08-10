package org.igarape.copcast.settings;

import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.preference.EditTextPreference;
import android.preference.PreferenceFragment;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;

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

    }
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        LinearLayout v = (LinearLayout) super.onCreateView(inflater, container, savedInstanceState);
        if (!Globals.showFeedback(getActivity().getApplicationContext())){
            return v;
        }
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.MATCH_PARENT);
        v.setGravity(Gravity.CENTER_HORIZONTAL);
        v.setLayoutParams(params);

        Button btn = new Button(getActivity().getApplicationContext());
        btn.setText(getString(R.string.feedback));
        btn.setBackgroundResource(R.color.bg_button_blue);

        params = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        params.setMargins(30,0,30,100);
        btn.setHeight(100);
        btn.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
        btn.setLayoutParams(params);
        btn.setGravity(Gravity.CENTER);

        v.addView(btn, -1);
        btn.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                Uri uriUrl = Uri.parse("https://docs.google.com/forms/d/e/1FAIpQLSe-i7CEoqSxKXu5_6OYt1QuYXBK53GKy3UcKdGA5r3zUKenAQ/viewform");
                Intent launchBrowser = new Intent(Intent.ACTION_VIEW, uriUrl);
                startActivity(launchBrowser);
            }
        });

        return v;
    }
}
