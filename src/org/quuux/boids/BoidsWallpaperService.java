package org.quuux.boids;

import android.util.Log;
import android.view.MotionEvent;
import android.os.Bundle;
import android.opengl.GLU;
import javax.microedition.khronos.opengles.GL;

// Stolen from:
// Original code provided by Robert Green
// http://www.rbgrn.net/content/354-glsurfaceview-adapted-3d-live-wallpapers

class FlockThread extends Thread implements Runnable {
    protected Flock flock;
    protected FlockBuffer buffer;
    protected boolean running;
    protected long frames;

    public FlockThread(Flock flock, FlockBuffer buffer) {
        this.flock = flock;
        this.buffer = buffer;
    }

    public void run() {
        long last = System.currentTimeMillis();
        running = true;
        while(running) {
            frames++;
            long now = System.currentTimeMillis();
            long elapsed = now - last;
            flock.tick(elapsed);   
            buffer.render(flock);
            last = now;
        }
    }

    public void stopSimulation() {
        running = false;
    }
}

public class BoidsWallpaperService extends GLWallpaperService {
    public BoidsWallpaperService() {
        super();
        TextureLoader.init(this);
    }
    public Engine onCreateEngine() {
        return new BoidsEngine();
    }

    class BoidsEngine extends GLEngine {
        private static final String TAG = "BoidsEngine";
        
        protected BoidsRenderer renderer;
        protected FlockThread simulation_thread;
        protected Flock flock;
        protected FlockBuffer buffer;

        protected Vector3 last_touch = new Vector3();
        
        public BoidsEngine() {
            super();
        
            flock = new Flock(500);
            buffer = new FlockBuffer(flock);

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

        public void onVisibilityChanged(boolean visible) {
            super.onVisibilityChanged(visible);
            
            if(visible) {
                simulation_thread = new FlockThread(flock, buffer);
                simulation_thread.start();
            } else {
                simulation_thread.stopSimulation();
            }
        }

        // public void onOffsetsChanged(float xOffset, float yOffset, 
        //                              float xOffsetStep, float yOffsetStep, 
        //                              int xPixelOffset, int yPixelOffset) {
        //     Log.d(TAG, "offset changed");
        // }

        // public Bundle onCommand(String acxbtion, int x, int y, int z, 
        //                         Bundle extras, boolean resultRequested) {
        //     Log.d(TAG, "command: " + action);
        //     return null;
        // } 

        // FIXME pull camera position
        public Vector3 projectTouchToWorld(float x, float y) {
            int width = renderer.getWidth();
            int height = renderer.getHeight();

            int[] view = new int[] {0, 0, width, height};

            float[] touch_position = new float[4];
            int rv = GLU.gluUnProject(x, view[3] - y, -500f, 
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

        public void onTouchEvent(MotionEvent event) {
            Vector3 v = projectTouchToWorld(event.getX(), event.getY());
            Log.d(TAG, "touch: " + v);            
            
            flock.scare(v);
        }
    }
}
