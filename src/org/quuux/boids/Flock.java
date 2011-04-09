package org.quuux.boids;

import android.graphics.Bitmap;
import android.util.Log;

import javax.microedition.khronos.opengles.GL10;
import javax.microedition.khronos.opengles.GL11;

import java.nio.FloatBuffer;

public class Flock {
    private static final String TAG = "Flock";
    
    protected Boid boids[];

    protected Texture texture;

    protected FloatBuffer vertices;
    protected FloatBuffer sizes;
    protected FloatBuffer colors;

    // preallocation so we dont trigger gc
    private Vector3 tmp;
    private Vector3 v1;
    private Vector3 v2;
    private Vector3 v3;
    private Vector3 v4;

    public Flock(int num) {
        boids = new Boid[num];

        for(int i=0; i<num; i++)
            boids[i] = new Boid(RandomGenerator.randomRange(-1, 1), 
                                RandomGenerator.randomRange(-1, 1),
                                RandomGenerator.randomRange(-1, 1));

        vertices = GLHelper.floatBuffer(boids.length * 3);
        sizes = GLHelper.floatBuffer(boids.length);
        colors = GLHelper.floatBuffer(boids.length * 4);

        tmp = new Vector3();
        v1 = new Vector3();
        v2 = new Vector3();
        v3 = new Vector3();
        v4 = new Vector3();

        texture = TextureLoader.get("plasma");
    }

    public void init(GL10 gl) {
        gl.glEnable(GL11.GL_POINT_SPRITE_OES);
        
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

    public void tick(long elapsed) {

        vertices.clear();
        colors.clear();
        sizes.clear();

        for(int i=0; i<boids.length; i++) {
            rule1(i);
            rule2(i);
            rule3(i);
            rule4(i);

            // FIXME 
            v1.scale(.001f);
            v3.scale(.075f);

            // Log.d(TAG, "v1=" + v1);
            // Log.d(TAG, "v2=" + v2);
            // Log.d(TAG, "v3=" + v3);

            // Combine components
            boids[i].velocity.add(v1);
            boids[i].velocity.add(v2);
            boids[i].velocity.add(v3);

            // limit velocity
            boids[i].velocity.normalize();
            boids[i].velocity.scale(5f);

            boids[i].velocity.add(v4);

            // apply velocity to position
            boids[i].position.add(boids[i].velocity);
            
            //Log.d(TAG, boids[i].toString());
            
            // update buffers
            vertices.put(boids[i].position.x);
            vertices.put(boids[i].position.y);
            vertices.put(boids[i].position.z);

            colors.put(1);
            colors.put(1);
            colors.put(1);
            colors.put(1);

            sizes.put(5);
        }

        vertices.position(0);
        colors.position(0);
        sizes.position(0);
    }

    public void draw(GL10 gl) {
        gl.glEnableClientState(GL11.GL_POINT_SIZE_ARRAY_BUFFER_BINDING_OES);
        gl.glEnableClientState(GL11.GL_POINT_SIZE_ARRAY_OES);
        gl.glEnableClientState(GL11.GL_POINT_SPRITE_OES);
        gl.glEnableClientState(GL10.GL_VERTEX_ARRAY);
        gl.glEnableClientState(GL10.GL_COLOR_ARRAY);

        gl.glDisable(GL10.GL_DEPTH_TEST);

        gl.glColorPointer(4, GL10.GL_FLOAT, 0, colors);
        ((GL11)gl).glPointSizePointerOES(GL10.GL_FLOAT, 0, sizes);
        gl.glVertexPointer(3, GL10.GL_FLOAT, 0, vertices);
        gl.glBindTexture(GL10.GL_TEXTURE_2D, texture.id);
        gl.glDrawArrays(GL10.GL_POINTS, 0, boids.length);

        gl.glEnable(GL10.GL_DEPTH_TEST);

        gl.glDisableClientState(GL10.GL_COLOR_ARRAY);
        gl.glDisableClientState(GL10.GL_VERTEX_ARRAY);
        gl.glDisableClientState(GL11.GL_POINT_SPRITE_OES);
        gl.glDisableClientState(GL11.GL_POINT_SIZE_ARRAY_OES);
        gl.glDisableClientState(GL11.GL_POINT_SIZE_ARRAY_BUFFER_BINDING_OES);        
    }

    // Rule 1 - match the average position of other boids
    private void rule1(int b) {
        v1.zero();

        for(int i=0; i<boids.length; i++)
            if(i != b)
                v1.add(boids[i].position);

        v1.scale((float)1.0/(boids.length-1));        
    }

    // Rule 2 - Maintain seperation
    private void rule2(int b) {
        for(int i=0; i<boids.length; i++) {
            if(i != b) {
                tmp.zero();       
                tmp.add(boids[i].position);
                tmp.subtract(boids[b].position);

                if(tmp.magnitude() < 100f)
                    v2.subtract(tmp);
            }
        }
    }

    // Rule 3 - match the average velocity of other boids
    private void rule3(int b) {
        v3.zero();

        for(int i=0; i<boids.length; i++)
            if(i != b)
                v3.add(boids[i].velocity);
        
        v3.scale((float)1.0/(boids.length-1));
    }

    // Rule 4 - keep within bounds
    private void rule4(int b) {
        v4.zero();

        if(boids[b].position.x < -100)
            v4.x = 10;
        else if(boids[b].position.x > 100)
            v4.x = -10;
        else if(boids[b].position.y < -250)
            v4.y = 10;
        else if(boids[b].position.y > 250)
            v4.y = -10;
        else if(boids[b].position.z < -250)
            v4.z = 10;
        else if(boids[b].position.z > 250)
            v4.z = -10;
    }
}