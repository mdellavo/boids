package org.quuux.boids;

import java.nio.FloatBuffer;

class FlockFrame {
    public FloatBuffer vertices;
    public FloatBuffer sizes;
    public FloatBuffer colors;

    public FlockFrame(Flock flock) {
        vertices = GLHelper.floatBuffer(flock.boids.length * 3);
        sizes = GLHelper.floatBuffer(flock.boids.length);
        colors = GLHelper.floatBuffer(flock.boids.length * 4);
    }        

    public void clear() {
        vertices.clear();
        colors.clear();
        sizes.clear();        
    }    
}
