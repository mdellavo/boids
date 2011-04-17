package org.quuux.boids;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

import android.opengl.GLU;
import android.util.Log;

public class BoidsRenderer implements GLWallpaperService.Renderer {
    private static final String TAG = "BoidsRenderer";

    // Used for Lighting
    private static float[] ambientComponent0 = {1.0f, 1.0f, 1.0f, 1.0f};
    private static float[] diffuseComponent0 = {1.0f, 1.0f, 1.0f, 1.0f};
    private static float[] lightPosition0 =    {0, 200f, 200f, 0f};

    private Camera camera;
    private Flock flock;

    private long frames;
    private long last;
    private long total_elapsed;

    protected int width;
    protected int height;
    protected MatrixGrabber matrix_grabber = new MatrixGrabber();
    
    public BoidsRenderer(Flock flock) {
        super();
        this.camera = new Camera();
        this.flock = flock;
    }

    public float[] getCurrentProjection() {
        return matrix_grabber.mProjection;
    }

    public float[] getCurrentModelView() {
        return matrix_grabber.mModelView;
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }
    
    public void onDrawFrame(GL10 gl) {
        long now = System.currentTimeMillis();
        long elapsed = now - last;

        frames++;
        total_elapsed += elapsed;
        if(total_elapsed > 1000) {          
            Log.d(TAG, "fps: " + frames);
            
            if(frames < 59)
                flock.throttleDown();
            else if(frames>=59)
                flock.throttleUp();
                
            total_elapsed = 0;
            frames = 0;
        }

        gl.glClearColor(0f, 0f, 0f, .5f);
        gl.glClear(GL10.GL_COLOR_BUFFER_BIT | GL10.GL_DEPTH_BUFFER_BIT);

        camera.tick(elapsed);
        matrix_grabber.getCurrentState(gl);

        flock.tick(elapsed);
        flock.draw(gl);

        last = now;
    }

    public void onSurfaceChanged(GL10 gl, int width, int height) {
        this.width = width;
        this.height = height;

        setupLightSources(gl);

        camera.init(gl, width, height);
        flock.init(gl);
        GLHelper.init(gl);

        last = System.currentTimeMillis();
    }

    public void onSurfaceCreated(GL10 gl, EGLConfig config) {
        gl.glShadeModel(GL10.GL_SMOOTH);

        gl.glClearDepthf(1f);
        gl.glDepthFunc(GL10.GL_LEQUAL);

        gl.glEnable(GL10.GL_TEXTURE_2D);
        gl.glEnable(GL10.GL_BLEND);
        gl.glEnable(GL10.GL_DITHER);
        gl.glEnable(GL10.GL_DEPTH_TEST);
        //gl.glEnable(GL10.GL_ALPHA_TEST);
        //gl.glEnable(GL10.GL_LIGHTING);
        gl.glEnable(GL10.GL_CULL_FACE);
        gl.glEnable(GL10.GL_COLOR_MATERIAL);

        gl.glHint(GL10.GL_FOG_HINT, GL10.GL_NICEST);
        gl.glHint(GL10.GL_PERSPECTIVE_CORRECTION_HINT, GL10.GL_NICEST);
        gl.glHint(GL10.GL_POINT_SMOOTH_HINT,GL10.GL_NICEST);		
        //gl.glAlphaFunc(GL10.GL_GREATER, 0.01f);

        gl.glTexEnvf(GL10.GL_TEXTURE_ENV, GL10.GL_TEXTURE_ENV_MODE, 
                     GL10.GL_REPLACE);

        gl.glBlendFunc(GL10.GL_ONE, GL10.GL_ONE);

        gl.glFrontFace(GL10.GL_CCW);

        gl.glCullFace(GL10.GL_BACK);
    }

    private void setupLightSources(GL10 gl) {
        //Enable Light source 0
        gl.glEnable(GL10.GL_LIGHT0);

        //Useful part of the Arrays start a 0
        gl.glLightfv(GL10.GL_LIGHT0, GL10.GL_AMBIENT, ambientComponent0, 0);
        gl.glLightfv(GL10.GL_LIGHT0, GL10.GL_DIFFUSE, diffuseComponent0, 0);

        //Position the light in the scene
        gl.glLightfv(GL10.GL_LIGHT0, GL10.GL_POSITION, lightPosition0, 0);
    }

    public void release() {}
}
