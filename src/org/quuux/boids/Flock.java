package org.quuux.boids;

import android.graphics.Bitmap;
import android.util.Log;

import android.graphics.Color;

import javax.microedition.khronos.opengles.GL10;
import javax.microedition.khronos.opengles.GL11;

import java.nio.FloatBuffer;

public class Flock {
    private static final String TAG = "Flock";

    private static final float MAX_SIZE         = 50f;
    private static final float MAX_VELOCITY     = 5f;
    private static final float RANGE            = 5f;
    private static final float REBOUND_VELOCITY = .5f;
    private static final float MIN_SIZE         = 10f;
    private static final float SIZE_SCALE       = 100f;

    private static final float SCALE_V1         = .001f;
    private static final float SCALE_V2         = .2f;
    private static final float SCALE_V3         = .125f;
    private static final float SCALE_V4         = 1f;
    private static final float SCALE_V5         = .001f;

    private static final float MIN_X = -200f;
    private static final float MAX_X = 200f;
    private static final float MIN_Y = -200f;
    private static final float MAX_Y = 200f;
    private static final float MIN_Z = -300f;
    private static final float MAX_Z = 300f;

    protected Boid boids[];

    protected Texture texture;

    protected FloatBuffer vertices;
    protected FloatBuffer sizes;
    protected FloatBuffer colors;

    // preallocation so we dont trigger gc
    private Vector3 tmp = new Vector3();
    private Vector3 v1 = new Vector3();
    private Vector3 v2 = new Vector3();
    private Vector3 v3 = new Vector3();
    private Vector3 v4 = new Vector3();
    private Vector3 v5 = new Vector3();

    private float color[] = new float[4];
    private int frame;

    public Flock(int num) {
        boids = new Boid[num];
        
        for(int i=0; i<num; i++) {
            boids[i] = new Boid(RandomGenerator.randomRange(-1, 1), 
                                RandomGenerator.randomRange(-1, 1),
                                RandomGenerator.randomRange(-1, 1),
                                RandomGenerator.randomRange(-1, 1), 
                                RandomGenerator.randomRange(-1, 1),
                                RandomGenerator.randomRange(-1, 1));
        }

        vertices = GLHelper.floatBuffer(boids.length * 3);
        sizes = GLHelper.floatBuffer(boids.length);
        colors = GLHelper.floatBuffer(boids.length * 4);
    }

    public void init(GL10 gl) {
        gl.glEnable(GL11.GL_POINT_SPRITE_OES);
        
        texture = TextureLoader.get("plasma");
        texture.load(gl);

        Log.d(TAG, "boid texture id: " + texture.id);

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
        frame++;

        color[0] = frame % 360;
        color[1] = 1f;
        color[2] = 1f;

        int rgb = Color.HSVToColor(color);
        int red = Color.red(rgb);
        int green = Color.green(rgb);
        int blue = Color.blue(rgb);

        vertices.clear();
        colors.clear();
        sizes.clear();

        for(int i=0; i<boids.length; i++) {
            rule1(i);
            rule2(i);
            rule3(i);
            rule4(i);
            rule5(i);

            // FIXME 
            v1.scale(SCALE_V1);
            v2.scale(SCALE_V2);
            v3.scale(SCALE_V3);
            v4.scale(SCALE_V4);
            v5.scale(SCALE_V5);

            // Log.d(TAG, "v1=" + v1);
            // Log.d(TAG, "v2=" + v2);
            // Log.d(TAG, "v3=" + v3);
            // Log.d(TAG, "v4=" + v4);

            // Combine components
            boids[i].velocity.add(v1);
            boids[i].velocity.add(v2);
            boids[i].velocity.add(v3);
            boids[i].velocity.add(v4);
            boids[i].velocity.add(v5);

            // limit velocity
            if(boids[i].velocity.magnitude() > MAX_VELOCITY) {
                boids[i].velocity.normalize();
                boids[i].velocity.scale(MAX_VELOCITY);            
            }

            // apply velocity to position
            boids[i].position.add(boids[i].velocity);
            
            // Log.d(TAG, boids[i].toString());
            
            // update buffers

            vertices.put(boids[i].position.x);
            vertices.put(boids[i].position.y);
            vertices.put(boids[i].position.z);

            colors.put(red);
            colors.put(green);
            colors.put(blue);
            colors.put(.4f);
         
            float size = MIN_SIZE + (boids[i].position.z - MIN_Z) / 
                (MAX_Z - MIN_Z) * SIZE_SCALE;
            sizes.put(size);
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

        gl.glDepthMask(false);

        gl.glColorPointer(4, GL10.GL_FLOAT, 0, colors);
        ((GL11)gl).glPointSizePointerOES(GL10.GL_FLOAT, 0, sizes);
        gl.glVertexPointer(3, GL10.GL_FLOAT, 0, vertices);
        gl.glBindTexture(GL10.GL_TEXTURE_2D, texture.id);
        gl.glDrawArrays(GL10.GL_POINTS, 0, boids.length);

        gl.glDepthMask(true);

         gl.glDisableClientState(GL10.GL_COLOR_ARRAY);
         gl.glDisableClientState(GL10.GL_VERTEX_ARRAY);
         gl.glDisableClientState(GL11.GL_POINT_SPRITE_OES);
         gl.glDisableClientState(GL11.GL_POINT_SIZE_ARRAY_OES);
         gl.glDisableClientState(GL11.GL_POINT_SIZE_ARRAY_BUFFER_BINDING_OES); 
    }

    private boolean inRange(Boid a, Boid b) {
        tmp.zero();
        tmp.add(a.position);
        tmp.subtract(b.position);
        return tmp.magnitude() < RANGE;
    }

    // Rule 1 - match the average position of other boids
    private void rule1(int b) {
        int count = 0;

        v1.zero();

        for(int i=0; i<boids.length; i++) {
            if(i != b && inRange(boids[b], boids[i])) {
                v1.add(boids[i].position);
                count++;
            }
        }

        if(count>0) {
            v1.scale((float)1.0/count);        
            v1.normalize();
        }
    }

    // Rule 2 - Maintain seperation
    private void rule2(int b) {               
        for(int i=0; i<boids.length; i++) {
            if(i != b && inRange(boids[b], boids[i])) {
                tmp.zero();       
                tmp.add(boids[i].position);
                tmp.subtract(boids[b].position);

                if(tmp.magnitude() < 100f)
                    v2.subtract(tmp);
            }
        }

        v2.normalize();
    }

    // Rule 3 - match the average velocity of other boids
    private void rule3(int b) {
        int count = 0;

        v3.zero();

        for(int i=0; i<boids.length; i++) {
            if(i != b && inRange(boids[b], boids[i])) {
                v3.add(boids[i].velocity);
                count++;
            }
        }

        if(count>0) {
            v3.scale((float)1.0/count);
            v3.normalize();
        }
    }

    // Rule 4 - keep within bounds
    private void rule4(int b) {
        v4.zero();

        if(boids[b].position.x < MIN_X)
            v4.x = REBOUND_VELOCITY;
        else if(boids[b].position.x > MAX_X)
            v4.x = -REBOUND_VELOCITY;
        else if(boids[b].position.y < MIN_Y)
            v4.y = REBOUND_VELOCITY;
        else if(boids[b].position.y > MAX_Y)
            v4.y = -REBOUND_VELOCITY;
        else if(boids[b].position.z < MIN_Z)
            v4.z = REBOUND_VELOCITY;
        else if(boids[b].position.z > MAX_Z)
            v4.z = -REBOUND_VELOCITY;
    }

    // Rule 5 - tend towards center
    private void rule5(int b) {
        v5.zero();
        v5.subtract(boids[b].position);
    }
}