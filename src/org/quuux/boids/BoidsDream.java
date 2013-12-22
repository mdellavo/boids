package org.quuux.boids;

import android.content.Intent;
import android.content.SharedPreferences;
import android.opengl.GLSurfaceView;
import android.opengl.GLSurfaceView.GLWrapper;
import android.service.dreams.DreamService;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;

import javax.microedition.khronos.opengles.GL;

public class BoidsDream extends DreamService implements View.OnTouchListener {

    private static final String TAG = "BoidsDream";

    private GLSurfaceView mGLView;
    private BoidsRenderer renderer;
    private FlockThread simulation_thread;
    private Flock flock;
    private FlockBuffer buffer;
    private Profile profile;
    private ProfilePreferenceWatcher mWatcher;

    @Override
    public void onAttachedToWindow() {
        super.onAttachedToWindow();

        setInteractive(true);
        setFullscreen(true);
        setScreenBright(false);

        mGLView = new GLSurfaceView(this);

        SharedPreferences preferences = getSharedPreferences(ProfileLoader.SHARED_PREFS_NAME, 0);

        TextureLoader.init(this);
        CheckInManager.init(this);

        ProfileLoader.init(this);
        profile = ProfileLoader.getProfile("Default");
        ProfileLoader.updateProfile(profile, preferences);

        flock = new Flock();
        flock.init(profile);

        buffer = new FlockBuffer(flock);

        simulation_thread = new FlockThread(flock, buffer);

        mWatcher = new ProfilePreferenceWatcher(simulation_thread, flock, profile);
        preferences.registerOnSharedPreferenceChangeListener(mWatcher);

        simulation_thread.start();

        renderer = new BoidsRenderer(buffer);

        mGLView.setGLWrapper(new GLWrapper() {
            public GL wrap(GL gl) {
                return new MatrixTrackingGL(gl);
            }
        });
        mGLView.setRenderer(renderer);
        mGLView.setRenderMode(GLSurfaceView.RENDERMODE_CONTINUOUSLY);
        mGLView.setOnTouchListener(this);
        setContentView(mGLView);

        mGLView.onResume();
    }

   @Override
    public void onDetachedFromWindow() {
        super.onDetachedFromWindow();
    }

    @Override
    public void onDreamingStarted() {
        super.onDreamingStarted();
        mGLView.onResume();
        simulation_thread.resumeSimulation();

        Intent intent = new Intent(BoidsWallpaperService.ACTION_PAUSE);
        sendBroadcast(intent);
    }

    @Override
    public void onDreamingStopped() {
        super.onDreamingStopped();
        mGLView.onPause();
        simulation_thread.pauseSimulation();

        Intent intent = new Intent(BoidsWallpaperService.ACTION_RESUME);
        sendBroadcast(intent);
    }

    @Override
    public boolean onTouch(View v, MotionEvent event) {
        Vector3 touch = GLHelper.projectTouchToWorld(renderer.getWidth(), renderer.getHeight(),
                renderer.getCurrentModelView(), renderer.getCurrentProjection(), event.getX(), event.getY());

        if (BuildConfig.DEBUG)Log.d(TAG, "touch: " + touch);

        touch.normalize();
        flock.touch(touch);

        return true;
    }
}
 
