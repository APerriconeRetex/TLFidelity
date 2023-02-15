package com.retexspa.tecnologica.tlmoduloloyalty;

import android.os.Bundle;
import android.text.InputType;
import android.widget.EditText;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.preference.PreferenceFragmentCompat;

public class WebServiceSetupActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.settings_activity);
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.settings, new SettingsFragment())
                .commit();
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }
    }

    public static class SettingsFragment extends PreferenceFragmentCompat {
        @Override
        public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
            setPreferencesFromResource(R.xml.root_preferences, rootKey);
            TLEditTextPreference pref;
            pref = getPreferenceManager().findPreference("WSURL");
            if(pref!=null) pref.setOnBindEditTextListener((EditText edit) -> edit.setInputType(InputType.TYPE_TEXT_VARIATION_URI));
            pref=getPreferenceManager().findPreference("timeout");
            if(pref!=null) pref.setOnBindEditTextListener((EditText edit) -> edit.setInputType(InputType.TYPE_CLASS_NUMBER));
        }
    }
}