package org.quuux.boids;

import android.os.Bundle;
import android.preference.PreferenceActivity;
import android.preference.ListPreference;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.preference.PreferenceManager;

import android.util.Log;

public class BoidsPreferenceActivity extends PreferenceActivity
    implements OnSharedPreferenceChangeListener {

    private static final String TAG = "BoidsPreferenceActivity";
    private static final String SHARED_PREFS_NAME = "BoidsSettings";        

    protected Profile profile;

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);          

        ProfileLoader.init(this);       

        // FIXME pass uri for name
        PreferenceManager preference_manager = getPreferenceManager();
        preference_manager.setSharedPreferencesName(SHARED_PREFS_NAME);

        SharedPreferences preferences = preference_manager.getSharedPreferences();
        preferences.registerOnSharedPreferenceChangeListener(this);

        // FIXME load profile and apply other settings
        String profile_name = preferences.getString("", "Default");
        Log.d(TAG, "loading proile: " + profile_name);
        profile = ProfileLoader.getProfile(profile_name);

        addPreferencesFromResource(R.xml.preferences);

        ListPreference pref_profiles = (ListPreference)findPreference("profile");
        pref_profiles.setEntries(ProfileLoader.getProfileNames());
        pref_profiles.setEntryValues(ProfileLoader.getProfileNames());
    }

    public void onSharedPreferenceChanged(SharedPreferences preferences,
                                          String key) {
    }
}