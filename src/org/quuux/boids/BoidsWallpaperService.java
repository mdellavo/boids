package org.quuux.boids;

// Stolen from:
// Original code provided by Robert Green
// http://www.rbgrn.net/content/354-glsurfaceview-adapted-3d-live-wallpapers

public class BoidsWallpaperService extends GLWallpaperService {
    public BoidsWallpaperService() {
        super();
        TextureLoader.init(this);
    }
    public Engine onCreateEngine() {
        BoidsEngine engine = new BoidsEngine();
        return engine;
    }

    class BoidsEngine extends GLEngine {
        BoidsRenderer renderer;

        public BoidsEngine() {
            super();

            renderer = new BoidsRenderer(new Flock(75));
            setRenderer(renderer);
            setRenderMode(RENDERMODE_CONTINUOUSLY);
        }

        public void onDestroy() {
            super.onDestroy();

            if (renderer != null)
                renderer.release();

            renderer = null;
        }
    }
}
