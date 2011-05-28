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
        Log.d(TAG, "Storing profile perefence");
        ProfileLoader.saveProfile(profile, preferences);
    }

    public void onSharedPreferenceChanged(SharedPreferences preferences,
                                          String key) {
        
        if(key.equals("profile_name")) {
            String profile_name = preferences.getString("profile_name", "Default");
            Log.d(TAG, "loading proile: " + profile_name);
            profile = ProfileLoader.getProfile(profile_name);

            // FIXME update preference views from new profile

        } else {                
            Log.d(TAG, "preference changed: " + key);
            
            try {
                Field field = profile.getClass().getField(key.toUpperCase());
                String type_name = field.getType().getName();
                
                Log.d(TAG, "type: " + type_name);
                
                if(type_name.equals("java.lang.String")) {
                    field.set(profile, preferences.getString(key, ""));
                } else if(type_name.equals("float")) {
                    field.setFloat(profile, preferences.getInt(key, 0));
                } else if(type_name.equals("int")) {
                    field.setInt(profile, preferences.getInt(key, 0));
                } else if(type_name.equals("long")) {
                    field.setLong(profile, preferences.getLong(key, 0));
                } else if(type_name.equals("boolean")) {
                    field.setBoolean(profile, preferences.getBoolean(key, false));
                } else {
                    Log.d(TAG, "Unknown Type " + type_name + " in field " + key);
                }
                
            } catch(NoSuchFieldException e) {
                Log.d(TAG, "no such profile field: " + key + ": " + e);
            } catch(IllegalAccessException e) {                    
                Log.d(TAG, "illeagal illeagl: " + key + ": " + e);
            }
        }
    }
    
    // Store preference json
    
    
}