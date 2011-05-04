package org.quuux.boids;

import android.util.Log;

import android.graphics.Color;

import javax.microedition.khronos.opengles.GL10;
import javax.microedition.khronos.opengles.GL11;

class FlockBuffer {

    protected int size;

    protected FlockFrame front;
    protected FlockFrame back;
    protected Texture texture;    

    protected boolean dirty;

    public FlockBuffer(Flock flock) {
        size = flock.boids.length;
        front = new FlockFrame(flock);
        back = new FlockFrame(flock);
    }
    
    // wait for consumer/renderer to finish with 
    protected void swap() {
            
    }

    public void render(Flock flock) {

        back.clear(); 

        for(int i=0; i<flock.boids.length; i++) {
            Boid a = flock.boids[i];

            // update buffers
            back.vertices.put(a.position.x);
            back.vertices.put(a.position.y);
            back.vertices.put(a.position.z);

            // FIMXE there has to be a better way to do this...
            int rgb = Color.HSVToColor(a.color);
            float red = (float)Color.red(rgb) / 255f;
            float green = (float)Color.green(rgb) / 255f;
            float blue = (float)Color.blue(rgb) / 255f;

            back.colors.put(red);
            back.colors.put(green);
            back.colors.put(blue);
            back.colors.put(1f);
          
            back.sizes.put(a.size);
        }
            
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

    public void init(GL10 gl) {
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

    public void draw(GL10 gl) {

        // wait for a frame to render
        synchronized(this) {
            while(!dirty) {
                try {
                    wait();
                } catch(InterruptedException e) {
                }
            }
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
            gl.glDrawArrays(GL10.GL_POINTS, 0, size); // XXX 
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
