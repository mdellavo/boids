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
    private static final float RANGE            = 10f;
    private static final float REBOUND_VELOCITY = .01f;
    private static final float MIN_SIZE         = 10f;
    private static final float SIZE_SCALE       = 100f;
    private static final long FLEE_TIME         = 2500;

    private static final float SCALE_V1         = .005f;
    private static final float SCALE_V2         = .5f;
    private static final float SCALE_V3         = .0675f;
    private static final float SCALE_V4         = 1f;
    private static final float SCALE_V5         = .0005f;
    private static final float SCALE_V6         = .01f;

    private static final float MIN_X = -1000f;
    private static final float MAX_X = 1000f;
    private static final float MIN_Y = -1000f;
    private static final float MAX_Y = 1000f;
    private static final float MIN_Z = -500f;
    private static final float MAX_Z = 500f;

    protected Boid boids[];
    protected int alive;

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
    private Vector3 v6 = new Vector3();

    private float color[] = new float[4];
    private int frame;

    private long flee;
    private Vector3 flee_from = new Vector3();
    
    public Flock(int num) {
        boids = new Boid[num];
        alive = num;

        for(int i=0; i<num; i++) {
            boids[i] = new Boid(RandomGenerator.randomRange(-1, 1), 
                                RandomGenerator.randomRange(-1, 1),
                                RandomGenerator.randomRange(-1, 1),
                                RandomGenerator.randomRange(-1, 1), 
                                RandomGenerator.randomRange(-1, 1),
                                RandomGenerator.randomRange(-1, 1));
            boids[i].seed = RandomGenerator.randomInt(0, 1000000);
        }

        vertices = GLHelper.floatBuffer(boids.length * 3);
        sizes = GLHelper.floatBuffer(boids.length);
        colors = GLHelper.floatBuffer(boids.length * 4);
    }

    public void throttleUp() {
        if(alive < boids.length)
            alive++;

        Log.d(TAG, "Throttle Up: alive = "  + alive);
    }

    public void throttleDown() {
        if(alive > 0)
            alive--;

        Log.d(TAG, "Throttle Down: alive = "  + alive);
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

        //Log.d(TAG, "color: " + color[0]);

        flee -= elapsed;

        vertices.clear();
        colors.clear();
        sizes.clear();

        for(int i=0; i<alive; i++) {
            v1.zero();
            v2.zero();
            v3.zero();
            v4.zero();
            v5.zero();
            v6.zero();

            Boid a = boids[i];
            int local_boids = 0;
                
            for(int j=0; j<alive; j++) {                
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

                // Rule 3
                v3.scale((float)1.0/local_boids);
                v3.normalize();
            }
            
            // Rule 4 - Bound
            rule4(a);
            rule5(a);

            // if(flee>0) {
            //     // Rule 6 - gather/scatter
            //     rule6(a);
            // } else {
            //     // Rule 5 - Center
            // }

            // Scale the results
            v1.scale(SCALE_V1);
            v2.scale(SCALE_V2);
            v3.scale(SCALE_V3);
            v4.scale(SCALE_V4);
            v5.scale(SCALE_V5);
            v6.scale(SCALE_V6);

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
            a.velocity.add(v6);

            // limit velocity
            if(a.velocity.magnitude() > MAX_VELOCITY) {
                a.velocity.normalize();
                a.velocity.scale(MAX_VELOCITY);            
            }

            // move independant of framerate
            //Log.d(TAG, "elapsed=" + elapsed);
            
            tmp.zero();
            tmp.copy(a.velocity);
            tmp.scale(elapsed/60f);

            // apply velocity to position
            a.position.add(tmp);
            
            //Log.d(TAG, a.toString());
             
            // update buffers

            vertices.put(a.position.x);
            vertices.put(a.position.y);
            vertices.put(a.position.z);

            color[0] = (a.seed + frame) % 360;
            color[1] = 1 + (float)Math.sin((a.seed + frame)/60f);
            color[2] = .4f + .3333f*(1 + (float)Math.cos((a.seed + frame)/120f));
        
            int rgb = Color.HSVToColor(color);
            float red = (float)Color.red(rgb) / 255f;
            float green = (float)Color.green(rgb) / 255f;
            float blue = (float)Color.blue(rgb) / 255f;

            colors.put(red);
            colors.put(green);
            colors.put(blue);
            colors.put(1f);
         
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
        gl.glDrawArrays(GL10.GL_POINTS, 0, alive);

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

    // Rule 6 - fleeing / scattering
    private void rule6(Boid b) {
        v6.copy(flee_from);
        v6.subtract(b.position);
    }

    public void scare(Vector3 p) {
        flee = FLEE_TIME;
        flee_from.copy(p);
    }
}