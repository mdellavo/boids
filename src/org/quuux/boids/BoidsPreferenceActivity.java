package org.quuux.boids;

import android.app.WallpaperManager;
import android.content.ComponentName;
import android.content.Intent;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
import android.content.SharedPreferences;
import android.util.Log;
import android.widget.Toast;

public class BoidsPreferenceActivity extends PreferenceActivity {

    private static final String TAG = "BoidsPreferenceActivity";

    private static final int REQUEST_SET_LIVE_WALLPAPER = 1;

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);          

        PreferenceManager manager = getPreferenceManager();
        manager.setSharedPreferencesName(ProfileLoader.SHARED_PREFS_NAME);
        manager.setSharedPreferencesMode(0);

        addPreferencesFromResource(R.xml.preferences);


        Preference button = (Preference)getPreferenceManager().findPreference("set_wallpaper");
        if(button != null)
        {
            button.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference arg0) {
                    setWallpaper();
                    return true;
                }
            });
        }
    }


    private void setWallpaper() {
        Intent intent;

        // try the new Jelly Bean direct android wallpaper chooser first
        try {
            ComponentName component = new ComponentName(getPackageName(), getPackageName() + ".BoidsWallpaperService");
            intent = new Intent(WallpaperManager.ACTION_CHANGE_LIVE_WALLPAPER);
            intent.putExtra(WallpaperManager.EXTRA_LIVE_WALLPAPER_COMPONENT, component);
            startActivityForResult(intent, REQUEST_SET_LIVE_WALLPAPER);
        }
        catch (android.content.ActivityNotFoundException e3) {
            // try the generic android wallpaper chooser next
            try {
                intent = new Intent(WallpaperManager.ACTION_LIVE_WALLPAPER_CHOOSER);
                startActivityForResult(intent, REQUEST_SET_LIVE_WALLPAPER);
            }
            catch (android.content.ActivityNotFoundException e2) {
                // that failed, let's try the nook intent
                try {
                    intent = new Intent();
                    intent.setAction("com.bn.nook.CHANGE_WALLPAPER");
                    startActivity(intent);
                }
                catch (android.content.ActivityNotFoundException e) {
                    // everything failed, let's notify the user
                    Toast.makeText(this, "Could not set wallpaper", Toast.LENGTH_SHORT).show();
                }
            }
        }
    }
}
