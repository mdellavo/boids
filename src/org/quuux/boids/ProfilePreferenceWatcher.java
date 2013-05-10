package org.quuux.boids;

import android.content.SharedPreferences;
import android.util.Log;

public class ProfilePreferenceWatcher
        implements SharedPreferences.OnSharedPreferenceChangeListener {

    private static final String TAG = "ProfilePreferenceWater";

    private FlockThread mSimulationThread;
    private Flock mFlock;
    private Profile mProfile;

    public ProfilePreferenceWatcher(final FlockThread simulationThread, final Flock flock, final Profile profile) {
        mSimulationThread = simulationThread;
        mFlock = flock;
        mProfile = profile;
    }

    // FIXME i think i need a lock here or queue on renderer
    public void onSharedPreferenceChanged(SharedPreferences preferences,
                                          String key) {
        if (BuildConfig.DEBUG)Log.d(TAG, "profile update: " + key);

//        final boolean visible = isVisible();
//
//        if(visible && mSimulationThread != null)
//            mSimulationThread.pauseSimulation();

        if(key.equals("FLOCK_SIZE")) {
            int flock_size = preferences.getInt(key, mFlock.getAlive());

            int delta = flock_size - mFlock.getAlive();
            while(delta != 0) {

                if(delta > 0) {
                    mFlock.throttleUp();
                } else {
                    mFlock.throttleDown();
                }

                delta = flock_size - mFlock.getAlive();
            }


        } else {
            if (BuildConfig.DEBUG) Log.d(TAG, "preference changed: " + key);
            ProfileLoader.updateProfile(mProfile, preferences, key);
        }

        if (BuildConfig.DEBUG) {
            Log.d(TAG, "Profile updated ------------------------");
            Log.d(TAG, mProfile.toString(3));
            Log.d(TAG, "----------------------------------------");
        }

//        if(visible && mSimulationThread != null)
//            mSimulationThread.resumeSimulation();

    }

}
