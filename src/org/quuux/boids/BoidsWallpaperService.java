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

        protected BoidsRenderer renderer;
        final protected FlockThread simulation_thread;
        protected Flock flock;
        protected FlockBuffer buffer;
        final protected Vector3 last_touch = new Vector3();
        
        protected Profile profile;

        public BoidsEngine() {
            super();

            SharedPreferences preferences =
                BoidsWallpaperService.this.getSharedPreferences(ProfileLoader.SHARED_PREFS_NAME, 0);
            
            ProfileLoader.init(BoidsWallpaperService.this);
            profile = ProfileLoader.getProfile("Default");
            ProfileLoader.updateProfile(profile, preferences);
            
            Log.d(TAG, "loaded profile: " + profile.name);
            Log.d(TAG, profile.toString(3));
            Log.d(TAG, "------------------------------------");
            
            
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
            Log.d(TAG, "profile update: " + key);

            if(simulation_thread != null)
                simulation_thread.pauseSimulation();

            boolean reinit = false;

            if(key.equals("profile_name")) {
                String profile_name = preferences.getString(key, "");
                profile = ProfileLoader.getProfile(profile_name);
                reinit = true;
            } else if(key.equals("RANDOMIZE_COLORS")) {
                flock.randomizeColors();                
            } else {

                if(key.equals("FLOCK_SIZE"))
                    reinit = true;

                Log.d(TAG, "preference changed: " + key);
                ProfileLoader.updateProfile(profile, preferences, key);
            }

            Log.d(TAG, "Profile updated ------------------------");
            Log.d(TAG, profile.toString(3));
            Log.d(TAG, "----------------------------------------");

            if(reinit) {
                flock = new Flock();
                flock.init(profile);
            
                buffer = new FlockBuffer(flock);

                flock.init(profile);
                buffer.allocate(flock);
            
                simulation_thread.setFlock(flock);
                simulation_thread.setBuffer(buffer);
            }   

            if(simulation_thread != null)
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
            
            Log.d(TAG, "visible: " + visible);

            if(visible) {
                simulation_thread.resumeSimulation();
            } else {
                simulation_thread.pauseSimulation();
            }
        }

        public void onOffsetsChanged(float xOffset, float yOffset, 
                                     float xOffsetStep, float yOffsetStep, 
                                     int xPixelOffset, int yPixelOffset) {
            Log.d(TAG, "offset: " + xOffset + ", " + yOffset + 
                  " | offsetStep: " + xOffsetStep + ", " + yOffsetStep + 
                  " | pixelOffset: " + xPixelOffset + ", " + yPixelOffset);
        }

        public Bundle onCommand(String action, int x, int y, int z, 
                                Bundle extras, boolean resultRequested) {
            Log.d(TAG, "command: " + action);
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
            //Log.d(TAG, "touch: " + v);            
            
            flock.touch(v);
        }
    }
}
