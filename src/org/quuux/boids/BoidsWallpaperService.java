package org.quuux.boids;

import android.util.Log;
import android.os.Bundle;

import android.view.MotionEvent;

import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.preference.PreferenceManager;

import android.opengl.GLU;

import javax.microedition.khronos.opengles.GL;

import java.lang.reflect.Field;

// Stolen from:
// Original code provided by Robert Green
// http://www.rbgrn.net/content/354-glsurfaceview-adapted-3d-live-wallpapers

public class BoidsWallpaperService extends GLWallpaperService {
    public BoidsWallpaperService() {
        super();
        TextureLoader.init(this);
    }
    public Engine onCreateEngine() {
        return new BoidsEngine();
    }

    class BoidsEngine extends GLEngine implements OnSharedPreferenceChangeListener {
        private static final String TAG = "BoidsEngine";
        private static final String SHARED_PREFS_NAME = "BoidsSettings";        

        protected BoidsRenderer renderer;
        final protected FlockThread simulation_thread;
        final protected Flock flock;
        final protected FlockBuffer buffer;
        
        final protected Vector3 last_touch = new Vector3();
        
        public BoidsEngine() {
            super();

            ProfileLoader.init(BoidsWallpaperService.this);

            flock = new Flock();
            
            SharedPreferences preferences =
                BoidsWallpaperService.this.getSharedPreferences(SHARED_PREFS_NAME, 0);

            preferences.registerOnSharedPreferenceChangeListener(this);
            onSharedPreferenceChanged(preferences, "profile");

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

        // FIXME i think i need a lock here
        public void onSharedPreferenceChanged(SharedPreferences preferences,
                                              String key) {
            
            if(simulation_thread != null)
                simulation_thread.pauseSimulation();

            Profile profile;

            if(key.equals("profile")) {
                String profile_name = preferences.getString("profile", "Default");
                Log.d(TAG, "loading proile: " + profile_name);
                profile = ProfileLoader.getProfile(profile_name);
            } else {
                
                profile = flock.getProfile();

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

            flock.init(profile);

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
