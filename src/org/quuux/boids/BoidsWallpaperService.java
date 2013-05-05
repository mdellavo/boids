package org.quuux.boids;

import android.util.Log;
import android.os.Bundle;

import android.view.MotionEvent;

import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.preference.PreferenceManager;

import android.opengl.GLU;

import javax.microedition.khronos.opengles.GL;

// Stolen from:
// Original code provided by Robert Green
// http://www.rbgrn.net/content/354-glsurfaceview-adapted-3d-live-wallpapers

public class BoidsWallpaperService extends GLWallpaperService {

    public BoidsWallpaperService() {
        super();
    }

    public Engine onCreateEngine() {
        TextureLoader.init(this);
        CheckInManager.init(this);
        return new BoidsEngine();
    }

    class BoidsEngine extends GLEngine 
        implements OnSharedPreferenceChangeListener {

        private static final String TAG = "BoidsEngine";
        private static final String SHARED_PREFS_NAME = "BoidsSettings";

        BoidsRenderer renderer;
        final FlockThread simulation_thread;
        final Flock flock;
        final FlockBuffer buffer;
        final Vector3 last_touch = new Vector3();
        final Vector3 last_push = new Vector3();
        
        final Profile profile;

        public BoidsEngine() {
            super();

            SharedPreferences preferences =
                BoidsWallpaperService.this.getSharedPreferences(ProfileLoader.SHARED_PREFS_NAME, 0);
            
            ProfileLoader.init(BoidsWallpaperService.this);
            profile = ProfileLoader.getProfile("Default");
            ProfileLoader.updateProfile(profile, preferences);
            
            if (BuildConfig.DEBUG)Log.d(TAG, "loaded profile: " + profile.name);
            if (BuildConfig.DEBUG)Log.d(TAG, profile.toString(3));
            if (BuildConfig.DEBUG)Log.d(TAG, "------------------------------------");
            
            
            preferences.registerOnSharedPreferenceChangeListener(this);

            flock = new Flock();
            flock.init(profile);
            
            buffer = new FlockBuffer(flock);

            simulation_thread = new FlockThread(flock, buffer);
            simulation_thread.start();
                   
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

        // FIXME i think i need a lock here or queue on renderer
        public void onSharedPreferenceChanged(SharedPreferences preferences,
                                              String key) {
            if (BuildConfig.DEBUG)Log.d(TAG, "profile update: " + key);

            final boolean visible = isVisible();

            if(visible && simulation_thread != null)
                simulation_thread.pauseSimulation();

            if(key.equals("FLOCK_SIZE")) {
                int flock_size = preferences.getInt(key, flock.getAlive());

                int delta = flock_size - flock.getAlive();
                while(delta != 0) {

                    if(delta > 0) {
                        flock.throttleUp();
                    } else {
                        flock.throttleDown();
                    }

                    delta = flock_size - flock.getAlive();
                }


            } else {
                if (BuildConfig.DEBUG) Log.d(TAG, "preference changed: " + key);
                ProfileLoader.updateProfile(profile, preferences, key);
            }

            if (BuildConfig.DEBUG)Log.d(TAG, "Profile updated ------------------------");
            if (BuildConfig.DEBUG)Log.d(TAG, profile.toString(3));
            if (BuildConfig.DEBUG)Log.d(TAG, "----------------------------------------");

            if(visible && simulation_thread != null)
                simulation_thread.resumeSimulation();

        }

        public void onDestroy() {
            super.onDestroy();
            
            if (renderer != null)
                renderer.release();

            renderer = null;
        }

        public void onVisibilityChanged(boolean visible) {
            super.onVisibilityChanged(visible);
            
            if (BuildConfig.DEBUG)Log.d(TAG, "visible: " + visible);

            if(visible) {
                simulation_thread.resumeSimulation();
            } else {
                simulation_thread.pauseSimulation();
            }
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

        // FIXME pull camera position
        public Vector3 projectTouchToWorld(float x, float y) {
            int width = renderer.getWidth();
            int height = renderer.getHeight();

            int[] view = new int[] {0, 0, width, height};

            float[] touch_position = new float[4];
            int rv = GLU.gluUnProject(x, view[3] - y, 1f, 
                                      renderer.getCurrentModelView(), 0, 
                                      renderer.getCurrentProjection(), 0, 
                                      view, 0,
                                      touch_position, 0);

            touch_position[0] /= touch_position[3];
            touch_position[1] /= touch_position[3];
            touch_position[2] /= touch_position[3];
            touch_position[3] /= touch_position[3];

            last_touch.x = touch_position[0];
            last_touch.y = touch_position[1];
            last_touch.z = touch_position[2];
                
            return last_touch;
        }

        // FIXME queue event; copy event data into finals
        public void onTouchEvent(MotionEvent event) {
            Vector3 v = projectTouchToWorld(event.getX(), event.getY());
            //if (BuildConfig.DEBUG)Log.d(TAG, "touch: " + v);
 
            v.normalize();
            flock.touch(v);
        }
    }
}
