package org.quuux.boids;

import android.os.Bundle;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
import android.content.SharedPreferences;
import android.util.Log;

public class BoidsPreferenceActivity extends PreferenceActivity {

    private static final String TAG = "BoidsPreferenceActivity";


    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);          

        PreferenceManager manager = getPreferenceManager();
        manager.setSharedPreferencesName(ProfileLoader.SHARED_PREFS_NAME);
        manager.setSharedPreferencesMode(0);

        addPreferencesFromResource(R.xml.preferences);

    }
}
