package org.quuux.boids;

import java.nio.FloatBuffer;

class FlockFrame {
    public FloatBuffer vertices;
    public FloatBuffer sizes;
    public FloatBuffer colors;

    public FlockFrame(int size) {
        allocate(size);
    }        

    public void allocate(int size) {
        vertices = GLHelper.floatBuffer(size * 3);
        sizes = GLHelper.floatBuffer(size);
        colors = GLHelper.floatBuffer(size * 4);        
    }

    public void clear() {
        vertices.clear();
        colors.clear();
        sizes.clear();        
    }    
}
