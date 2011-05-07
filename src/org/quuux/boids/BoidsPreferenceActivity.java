package org.quuux.boids;

import android.os.Bundle;
import android.preference.PreferenceActivity;
import android.preference.ListPreference;

public class BoidsPreferenceActivity extends PreferenceActivity {
    private static final String SHARED_PREFS_NAME = "BoidsSettings";        

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);          

        getPreferenceManager().setSharedPreferencesName(SHARED_PREFS_NAME);

        addPreferencesFromResource(R.xml.preferences);

        ProfileLoader.init(this);       
        ListPreference pref_profiles = (ListPreference)findPreference("profile");
        pref_profiles.setEntries(ProfileLoader.getProfileNames());
        pref_profiles.setEntryValues(ProfileLoader.getProfileNames());

    }
}