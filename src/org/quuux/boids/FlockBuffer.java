package org.quuux.boids;

import android.util.Log;
import java.nio.FloatBuffer;

import javax.microedition.khronos.opengles.GL10;
import javax.microedition.khronos.opengles.GL11;

class FlockBuffer {
    private static final String TAG = "FlockBuffer";

    protected FlockFrame front;
    protected FlockFrame back;

    protected Texture texture;    
    protected boolean dirty;

    protected FloatBuffer vertices;
    protected FloatBuffer sizes;
    protected FloatBuffer colors;

    //protected BoundingBox bounding_box;

    public FlockBuffer(Flock flock) {
        allocate(flock);
    }

    public void allocate(Flock flock) {
        front = new FlockFrame(flock);
        back = new FlockFrame(flock);
        int size = flock.boids.length;
        vertices = GLHelper.floatBuffer(size * 3);
        sizes = GLHelper.floatBuffer(size);
        colors = GLHelper.floatBuffer(size * 4);
    }
    
    // Only the flock thread calls this
    public void add(Flock flock) {
        back.clear();
        
        for(int i=0; i<flock.boids.length; i++) {
            if (flock.boids[i].alive) {
                back.add(flock.boids[i]);
            }
        }
    }

    public void swap() {
        synchronized(this) {
            FlockFrame tmp = front;
            front = back;
            back  = tmp;
            notifyAll();
        }

        Thread.yield();
    }
    
 
    final public void init(GL10 gl) {

        float[] att = { 1f, 0.0f, 0.01f };
        ((GL11)gl).glPointParameterfv(GL11.GL_POINT_DISTANCE_ATTENUATION, att, 0);
        ((GL11)gl).glPointParameterf(GL11.GL_POINT_SIZE_MIN, 1.0f);
        ((GL11)gl).glPointParameterf(GL11.GL_POINT_SIZE_MAX, 10000.0f);

        gl.glEnable(GL11.GL_POINT_SPRITE_OES);
        
        texture = TextureLoader.get("boid");
        texture.load(gl);

        gl.glTexParameterf(GL10.GL_TEXTURE_2D, GL10.GL_TEXTURE_MIN_FILTER, 
                           GL10.GL_NICEST);
        gl.glTexParameterf(GL10.GL_TEXTURE_2D, GL10.GL_TEXTURE_MAG_FILTER,
                           GL10.GL_NICEST);
        
        gl.glTexParameterf(GL10.GL_TEXTURE_2D, GL10.GL_TEXTURE_WRAP_S,
                           GL10.GL_REPEAT);
        gl.glTexParameterf(GL10.GL_TEXTURE_2D, GL10.GL_TEXTURE_WRAP_T,
                           GL10.GL_REPEAT);
        
        gl.glTexEnvf(GL11.GL_POINT_SPRITE_OES, GL11.GL_COORD_REPLACE_OES,
                     GL11.GL_TRUE);
        
        //bounding_box.init(gl);
    }

    final public void draw(GL10 gl) {

        int count;
        vertices.position(0);
        colors.position(0);
        sizes.position(0);

        synchronized(this) {

            try {
                wait();
            } catch(InterruptedException e) { 
            }

            count = front.getCount();

            vertices.limit(count * 3);
            colors.limit(count * 4);
            sizes.limit(count);

            vertices.put(front.getPositions(), 0, count * 3);
            sizes.put(front.getSizes(), 0, count);
            colors.put(front.getColors(), 0, count * 4);
        }

        vertices.position(0);
        colors.position(0);
        sizes.position(0);

        //bounding_box.draw(gl);

        gl.glEnable(GL10.GL_TEXTURE);
        
        gl.glEnableClientState(GL11.GL_POINT_SIZE_ARRAY_BUFFER_BINDING_OES);
        gl.glEnableClientState(GL11.GL_POINT_SIZE_ARRAY_OES);
        gl.glEnableClientState(GL11.GL_POINT_SPRITE_OES);
        gl.glEnableClientState(GL10.GL_VERTEX_ARRAY);
        gl.glEnableClientState(GL10.GL_COLOR_ARRAY);
        
        gl.glColorPointer(4, GL10.GL_FLOAT, 0, colors);
        ((GL11)gl).glPointSizePointerOES(GL10.GL_FLOAT, 0, sizes);
        gl.glVertexPointer(3, GL10.GL_FLOAT, 0, vertices);
        gl.glBindTexture(GL10.GL_TEXTURE_2D, texture.id);
        gl.glDrawArrays(GL10.GL_POINTS, 0, count); // XXX 
        
        gl.glDisableClientState(GL10.GL_COLOR_ARRAY);
        gl.glDisableClientState(GL10.GL_VERTEX_ARRAY);
        gl.glDisableClientState(GL11.GL_POINT_SPRITE_OES);
        gl.glDisableClientState(GL11.GL_POINT_SIZE_ARRAY_OES);
        gl.glDisableClientState(GL11.GL_POINT_SIZE_ARRAY_BUFFER_BINDING_OES); 
   
        gl.glDisable(GL10.GL_TEXTURE);
    }
}
