package org.quuux.boids;

import android.util.Log;
import javax.microedition.khronos.opengles.GL10;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;

public class BoundingBox {
    private static final String TAG = "BoundingBox";

    private Flock flock;

    private IntBuffer   vertex_buffer;
    private IntBuffer   color_buffer;
    private ByteBuffer  index_buffer;

    int one = 800; 

    int vertices[] = {
        -one, -one, -one,
        one, -one, -one,
        one,  one, -one,
        -one,  one, -one,
        -one, -one,  one,
        one, -one,  one,
        one,  one,  one,
        -one,  one,  one,
    };
    
    int colors[] = {
        0,    0,    0,  one,
        one,    0,    0,  one,
        one,  one,    0,  one,
        0,  one,    0,  one,
        0,    0,  one,  one,
        one,    0,  one,  one,
        one,  one,  one,  one,
        0,  one,  one,  one,
    };

    byte indices[] = {
        0, 4, 5,    0, 5, 1,
        1, 5, 6,    1, 6, 2,
        2, 6, 7,    2, 7, 3,
        3, 7, 4,    3, 4, 0,
        4, 7, 6,    4, 6, 5,
        3, 0, 1,    3, 1, 2
    };

    public BoundingBox(Flock flock) {
        this.flock = flock;
    }

    final public void init(GL10 gl) {
        vertex_buffer = GLHelper.intBuffer(vertices.length);
        vertex_buffer.put(vertices);
        vertex_buffer.position(0);

        color_buffer = GLHelper.intBuffer(colors.length);;
        color_buffer.put(colors);
        color_buffer.position(0);

        index_buffer = GLHelper.byteBuffer(indices.length);
        index_buffer.put(indices);
        index_buffer.position(0);
    }
    
    final public void draw(GL10 gl) {

        gl.glEnableClientState(GL10.GL_COLOR_ARRAY);
        gl.glEnableClientState(GL10.GL_VERTEX_ARRAY);    

        gl.glPushMatrix();

        gl.glTranslatef(0, 0, 0);

        gl.glFrontFace(GL10.GL_CW);
        gl.glVertexPointer(3, GL10.GL_FIXED, 0, vertex_buffer);
        gl.glColorPointer(4, GL10.GL_FIXED, 0, color_buffer);
        gl.glDrawElements(GL10.GL_TRIANGLES, 36, GL10.GL_UNSIGNED_BYTE, index_buffer);
        gl.glPopMatrix();

        gl.glDisableClientState(GL10.GL_VERTEX_ARRAY);    
        gl.glDisableClientState(GL10.GL_COLOR_ARRAY);
        
    }
}
