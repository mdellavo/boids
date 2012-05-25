package org.quuux.boids;

import android.util.Log;

import android.graphics.Color;

import javax.microedition.khronos.opengles.GL10;
import javax.microedition.khronos.opengles.GL11;

class FlockBuffer {
    private static final String TAG = "FlockBuffer";

    protected FlockFrame front;
    protected FlockFrame back;
    protected Texture texture;    
    protected boolean dirty;

    public FlockBuffer(Flock flock) {
        allocate(flock);
    }

    public void allocate(Flock flock) {
        front = new FlockFrame(flock.boids.length);
        back = new FlockFrame(flock.boids.length);        
    }

    final public void render(Flock flock) {

        flock.sort();
        
        back.clear(); 

        int alive = 0;
        for(int i = 0; i<flock.boids.length; i++) {
            Boid a = flock.boids[i];
            
            if(!a.alive)
                continue;

            alive++;

            // update buffers
            back.vertices.put(a.position.x);
            back.vertices.put(a.position.y);
            back.vertices.put(a.position.z);

            // FIMXE there has to be a better way to do this...
            int alpha = Math.round(a.color[3] * 255);

            int rgb = Color.HSVToColor(alpha, a.color);
            float red = (float)Color.red(rgb) / 255f;
            float green = (float)Color.green(rgb) / 255f;
            float blue = (float)Color.blue(rgb) / 255f;

            back.colors.put(red);
            back.colors.put(green);
            back.colors.put(blue);

            back.colors.put(a.opacity);
            back.sizes.put(a.size);
        }
            
        back.vertices.limit(alive*3);
        back.colors.limit(alive*4);
        back.sizes.limit(alive);

        back.vertices.position(0);
        back.colors.position(0);
        back.sizes.position(0);

        synchronized(this) {

            while(dirty) {
                try {
                    wait();
                } catch(InterruptedException e) {
                }
            }

            FlockFrame tmp = back;
            back = front;
            front = tmp;
            dirty = true;
            notifyAll();
        }

        Thread.yield();
    }

    final public void init(GL10 gl) {
        float[] att = {0, 0, .01f};
        ((GL11)gl).glPointParameterfv(GL11.GL_POINT_DISTANCE_ATTENUATION, att, 0);
        ((GL11)gl).glPointParameterf(GL11.GL_POINT_SIZE_MIN, 1.0f);
        ((GL11)gl).glPointParameterf(GL11.GL_POINT_SIZE_MAX, 1000.0f);

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

    }

    final public void draw(GL10 gl) {

        // wait for a frame to render
        synchronized(this) {
            while(!dirty) {
                try {
                    wait();
                } catch(InterruptedException e) {
                }
            }

            notifyAll();
        }

        gl.glEnableClientState(GL11.GL_POINT_SIZE_ARRAY_BUFFER_BINDING_OES);
        gl.glEnableClientState(GL11.GL_POINT_SIZE_ARRAY_OES);
        gl.glEnableClientState(GL11.GL_POINT_SPRITE_OES);
        gl.glEnableClientState(GL10.GL_VERTEX_ARRAY);
        gl.glEnableClientState(GL10.GL_COLOR_ARRAY);

        gl.glDepthMask(false);

        synchronized(this) {
            gl.glColorPointer(4, GL10.GL_FLOAT, 0, front.colors);
            ((GL11)gl).glPointSizePointerOES(GL10.GL_FLOAT, 0, front.sizes);
            gl.glVertexPointer(3, GL10.GL_FLOAT, 0, front.vertices);
            gl.glBindTexture(GL10.GL_TEXTURE_2D, texture.id);
            gl.glDrawArrays(GL10.GL_POINTS, 0, front.sizes.limit()); // XXX 
            dirty=false;
            notifyAll();
        }

        gl.glDepthMask(true);
        
        gl.glDisableClientState(GL10.GL_COLOR_ARRAY);
        gl.glDisableClientState(GL10.GL_VERTEX_ARRAY);
        gl.glDisableClientState(GL11.GL_POINT_SPRITE_OES);
        gl.glDisableClientState(GL11.GL_POINT_SIZE_ARRAY_OES);
        gl.glDisableClientState(GL11.GL_POINT_SIZE_ARRAY_BUFFER_BINDING_OES); 
    }
}
