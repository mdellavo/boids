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
    private static final float RANGE            = 50f;
    private static final float REBOUND_VELOCITY = .5f;
    private static final float MIN_SIZE         = 20f;
    private static final float SIZE_SCALE       = 200f;

    private static final float SCALE_V1         = .001f;
    private static final float SCALE_V2         = .5f;
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
        
        texture = TextureLoader.get("boid");
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
        color[1] = 1 + (float)Math.sin(frame/60f);
        color[2] = .4f + .3333f*(1 + (float)Math.cos(frame/120f));

        //Log.d(TAG, "color: " + color[0]);

        int rgb = Color.HSVToColor(color);
        float red = (float)Color.red(rgb) / 255f;
        float green = (float)Color.green(rgb) / 255f;
        float blue = (float)Color.blue(rgb) / 255f;

        vertices.clear();
        colors.clear();
        sizes.clear();

        for(int i=0; i<boids.length; i++) {
            v1.zero();
            v2.zero();
            v3.zero();
            v4.zero();
            v5.zero();
            
            Boid a = boids[i];
            int local_boids = 0;
                
            for(int j=0; j<boids.length; j++) {                
                Boid b = boids[j];

                if(j != i && inRange(a, b)) {
                    local_boids++;
                    
                    // Rule 1
                    v1.add(b.position);

                    // Rule 2
                    tmp.zero();       
                    tmp.add(b.position);
                    tmp.subtract(a.position);

                    if(tmp.magnitude() < 100f) // FIXME
                        v2.subtract(tmp);                  
                    
                    // Rule 3
                    v3.add(b.velocity);
                }
            }
            
            if(local_boids>0) {
                // Rule 1
                v1.scale((float)1.0/local_boids);
                v1.normalize();

                // Rule 2
                v2.normalize();

                // Rule 3
                v3.scale((float)1.0/local_boids);
                v3.normalize();
            }
            
            rule4(a);
            rule5(a);

            // Scale the results
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
            a.velocity.add(v1);
            a.velocity.add(v2);
            a.velocity.add(v3);
            a.velocity.add(v4);
            a.velocity.add(v5);

            // limit velocity
            if(a.velocity.magnitude() > MAX_VELOCITY) {
                a.velocity.normalize();
                a.velocity.scale(MAX_VELOCITY);            
            }

            // apply velocity to position
            a.position.add(a.velocity);
            
            // Log.d(TAG, a.toString());
            
            // update buffers

            vertices.put(a.position.x);
            vertices.put(a.position.y);
            vertices.put(a.position.z);

            colors.put(red);
            colors.put(green);
            colors.put(blue);
            colors.put(.4f);
         
            float size = MIN_SIZE + (a.position.z - MIN_Z) / 
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
    private void rule4(Boid b) {
        if(b.position.x < MIN_X)
            v4.x = REBOUND_VELOCITY;
        else if(b.position.x > MAX_X)
            v4.x = -REBOUND_VELOCITY;
        else if(b.position.y < MIN_Y)
            v4.y = REBOUND_VELOCITY;
        else if(b.position.y > MAX_Y)
            v4.y = -REBOUND_VELOCITY;
        else if(b.position.z < MIN_Z)
            v4.z = REBOUND_VELOCITY;
        else if(b.position.z > MAX_Z)
            v4.z = -REBOUND_VELOCITY;
    }

    // Rule 5 - tend towards center
    private void rule5(Boid b) {
        v5.z = -100; // FIXME
        v5.subtract(b.position);
    }
}