package org.quuux.boids;

import android.content.*;
import android.util.Log;
import android.os.Bundle;
import android.opengl.GLSurfaceView.GLWrapper;

import android.view.MotionEvent;

import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.preference.PreferenceManager;

import android.opengl.GLU;

import javax.microedition.khronos.opengles.GL;

// Stolen from:
// Original code provided by Robert Green
// http://www.rbgrn.net/content/354-glsurfaceview-adapted-3d-live-wallpapers

public class BoidsWallpaperService extends GLWallpaperService {

    private static final String TAG = "BoidsWallpaperService";

    private static final String SHARED_PREFS_NAME = "BoidsSettings";

    public static final String ACTION_PAUSE = "org.quuux.boids.intent.actions.PAUSE";
    public static final String ACTION_RESUME = "org.quuux.boids.intent.actions.RESUME";

    private FlockThread simulation_thread;
    private Flock flock;
    private FlockBuffer buffer;
    private Profile profile;

    private BoidsEngine mEngine;
    private BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            if (ACTION_PAUSE.equals(action)) {
                Log.d(TAG, "Pausing");
                mEngine.onPause();
            } else if (ACTION_RESUME.equals(action)) {
                Log.d(TAG, "Resuming simulation");
                mEngine.onResume();
            }
        }
    };

    public BoidsWallpaperService() {
        super();
    }

    @Override
    public void onCreate() {
        super.onCreate();

        IntentFilter filter = new IntentFilter();
        filter.addAction(ACTION_PAUSE);
        filter.addAction(ACTION_RESUME);
        registerReceiver(mReceiver, filter);

        SharedPreferences preferences =
                BoidsWallpaperService.this.getSharedPreferences(ProfileLoader.SHARED_PREFS_NAME, 0);

        TextureLoader.init(this);
        CheckInManager.init(this);

        ProfileLoader.init(BoidsWallpaperService.this);
        profile = ProfileLoader.getProfile("Default");
        ProfileLoader.updateProfile(profile, preferences);

        if (BuildConfig.DEBUG) {
            Log.d(TAG, "loaded profile: " + profile.name);
            Log.d(TAG, profile.toString(3));
            Log.d(TAG, "------------------------------------");
        }

        flock = new Flock();
        flock.init(profile);

        buffer = new FlockBuffer(flock);

        preferences.registerOnSharedPreferenceChangeListener(new ProfilePreferenceWatcher(simulation_thread, flock, profile));

        simulation_thread = new FlockThread(flock, buffer);
        simulation_thread.start();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        unregisterReceiver(mReceiver);
    }

    public Engine onCreateEngine() {
        mEngine = new BoidsEngine();
        return mEngine;
    }

    class BoidsEngine extends GLEngine {

        private static final String TAG = "BoidsEngine";

        BoidsRenderer renderer;
        final Vector3 last_touch = new Vector3();
        final Vector3 last_push = new Vector3();
        
        public BoidsEngine() {
            super();

            setGLWrapper(new GLWrapper() {
                    public GL wrap(GL gl) {
                        return new MatrixTrackingGL(gl);
                    }
                });
    
            renderer = new BoidsRenderer(buffer);
            setRenderer(renderer);
            setRenderMode(RENDERMODE_CONTINUOUSLY);
            setTouchEventsEnabled(true);
        }

        public void onDestroy() {
            super.onDestroy();
            
            if (renderer != null)
                renderer.release();

            renderer = null;
        }

        @Override
        public void onPause() {
            super.onPause();
            simulation_thread.pauseSimulation();
        }

        @Override
        public void onResume() {
            super.onResume();
            simulation_thread.resumeSimulation();
        }

        public void onOffsetsChanged(float xOffset, float yOffset,
                                     float xOffsetStep, float yOffsetStep, 
                                     int xPixelOffset, int yPixelOffset) {
            Vector3 v = new Vector3(xPixelOffset, yPixelOffset, 0);
            v.subtract(last_push);
            v.normalize();
            if(v.magnitude() > 0) {
                if (BuildConfig.DEBUG)Log.d(TAG, "pushing " + v);
                flock.push(v);
            }

            last_push.x = xPixelOffset;
            last_push.y = yPixelOffset;
        }

        public Bundle onCommand(String action, int x, int y, int z, 
                                Bundle extras, boolean resultRequested) {
            if (BuildConfig.DEBUG)Log.d(TAG, "command: " + action);
            return null;
        } 

        // FIXME queue event; copy event data into finals
        public void onTouchEvent(MotionEvent event) {
            Vector3 v = GLHelper.projectTouchToWorld(renderer.getWidth(), renderer.getHeight(),
                    renderer.getCurrentModelView(), renderer.getCurrentProjection(), event.getX(), event.getY());

            if (BuildConfig.DEBUG) Log.d(TAG, "touch: " + v);

            v.normalize();
            flock.touch(v);
        }
    }
}
