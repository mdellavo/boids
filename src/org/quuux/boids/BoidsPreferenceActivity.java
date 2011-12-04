package org.quuux.boids;

import android.os.Bundle;
import android.preference.PreferenceActivity;
import android.preference.ListPreference;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.preference.PreferenceManager;

import java.lang.reflect.Field;

import org.json.JSONTokener;
import org.json.JSONObject;
import org.json.JSONException;

import android.util.Log;

public class BoidsPreferenceActivity extends PreferenceActivity
    implements OnSharedPreferenceChangeListener {

    private static final String TAG = "BoidsPreferenceActivity";

    protected boolean dirty;
    protected Profile profile;
    protected SharedPreferences preferences;

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);          

        ProfileLoader.init(this);       

        PreferenceManager preference_manager = getPreferenceManager();
        preference_manager.setSharedPreferencesName(ProfileLoader.SHARED_PREFS_NAME);

        preferences = preference_manager.getSharedPreferences();

        Log.d(TAG, "Loading profile preferences");
        profile = ProfileLoader.loadProfile(preferences);

        preferences.registerOnSharedPreferenceChangeListener(this);

        addPreferencesFromResource(R.xml.preferences);

        ListPreference pref_profiles = (ListPreference)findPreference("profile_name");
        pref_profiles.setEntries(ProfileLoader.getProfileNames());
        pref_profiles.setEntryValues(ProfileLoader.getProfileNames());
    }

    public void onPause() {
        super.onPause();
        if(dirty) {
            Log.d(TAG, "Storing profile preference");
            ProfileLoader.saveProfile(profile, preferences);
        }
    }

    public void onSharedPreferenceChanged(SharedPreferences preferences, 
                                          String key) {

        if(key.equals("profile_name")) {

            String profile_name = preferences.getString("profile_name", "Default");
            Log.d(TAG, "loading proile: " + profile_name);
            dirty = false;
            profile = ProfileLoader.getProfile(profile_name);

            // FIXME update preference views from new profile

        } else {

            Log.d(TAG, "preference changed: " + key);
            ProfileLoader.updateProfile(profile, preferences, key);
            dirty = true;

        }
    }
    
    // Store preference json
    
}