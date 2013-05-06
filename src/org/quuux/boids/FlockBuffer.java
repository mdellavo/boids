package org.quuux.boids;

import android.util.Log;
import java.nio.FloatBuffer;

import javax.microedition.khronos.opengles.GL10;
import javax.microedition.khronos.opengles.GL11;

class FlockBuffer {
    private static final String TAG = "FlockBuffer";

    private static final int MAX_SIZE = 1000;

    private static final int TAIL_SIZE = 10;

    private final FlockFrame buffer[] = new FlockFrame[TAIL_SIZE];
    private int count;

    private FlockFrame front;

    private Texture texture;
    protected boolean dirty;

    private FloatBuffer vertices;
    private FloatBuffer sizes;
    private FloatBuffer colors;

    public FlockBuffer(Flock flock) {
        allocate(flock);
    }

    void allocate(Flock flock) {
        int size = MAX_SIZE * 2;

        for (int i=0; i< TAIL_SIZE; i++)
            buffer[i] = new FlockFrame(size);

        front = new FlockFrame(size * (TAIL_SIZE - 1));

        vertices = GLHelper.floatBuffer(size * (TAIL_SIZE - 1) * 3);
        sizes = GLHelper.floatBuffer(size * (TAIL_SIZE - 1));
        colors = GLHelper.floatBuffer(size * (TAIL_SIZE - 1) * 4);
    }

    private int index(int offset) {
        return (count + TAIL_SIZE + offset) % TAIL_SIZE;

    }

    private int backIndex() {
        return index(-1);
    }

    // Only the flock thread calls this
    public void add(Flock flock) {
        final FlockFrame back = buffer[backIndex()];

        back.clear();
        
        for(int i=0; i<flock.boids.length; i++) {
            if (flock.boids[i].alive) {
                back.add(flock.boids[i]);
            }
        }
    }

    public void swap() {
        synchronized(buffer) {
            count++;
        }
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

        front.clear();

        synchronized(buffer) {
            for (int i=0; i<TAIL_SIZE-1; i++) {

                final FlockFrame buf = buffer[index(i)];

                float sizeScale = 2 - (float)(TAIL_SIZE - (i+1)) / (float)TAIL_SIZE;
                float opacityScale = (float)(TAIL_SIZE - (i+1)) / (float)TAIL_SIZE;

                buf.setSizeScale(sizeScale);
                buf.setOpacityScale(opacityScale);

                front.add(buf);
            }
        }

        int num = front.getCount();


        gl.glEnableClientState(GL11.GL_POINT_SIZE_ARRAY_BUFFER_BINDING_OES);
        gl.glEnableClientState(GL11.GL_POINT_SIZE_ARRAY_OES);
        gl.glEnableClientState(GL11.GL_POINT_SPRITE_OES);
        gl.glEnableClientState(GL10.GL_VERTEX_ARRAY);
        gl.glEnableClientState(GL10.GL_COLOR_ARRAY);

        vertices.position(0);
        colors.position(0);
        sizes.position(0);

        vertices.limit(num * 3);
        colors.limit(num * 4);
        sizes.limit(num);

        vertices.put(front.getPositions(), 0, num * 3);
        sizes.put(front.getSizes(), 0, num);
        colors.put(front.getColors(), 0, num * 4);

        vertices.position(0);
        colors.position(0);
        sizes.position(0);

        gl.glColorPointer(4, GL10.GL_FLOAT, 0, colors);
        ((GL11)gl).glPointSizePointerOES(GL10.GL_FLOAT, 0, sizes);
        gl.glVertexPointer(3, GL10.GL_FLOAT, 0, vertices);
        gl.glBindTexture(GL10.GL_TEXTURE_2D, texture.id);
        gl.glDrawArrays(GL10.GL_POINTS, 0, num); // XXX

        //bounding_box.draw(gl);

        gl.glDisableClientState(GL10.GL_COLOR_ARRAY);
        gl.glDisableClientState(GL10.GL_VERTEX_ARRAY);
        gl.glDisableClientState(GL11.GL_POINT_SPRITE_OES);
        gl.glDisableClientState(GL11.GL_POINT_SIZE_ARRAY_OES);
        gl.glDisableClientState(GL11.GL_POINT_SIZE_ARRAY_BUFFER_BINDING_OES); 
   
    }
}
