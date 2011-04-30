package org.quuux.boids;

import android.graphics.Bitmap;
import android.util.Log;

import javax.microedition.khronos.opengles.GL10;
import javax.microedition.khronos.opengles.GL11;

public class Flock {
    private static final String TAG = "Flock";

    private static final float MAX_SIZE         = 50f;
    private static final float MAX_VELOCITY     = 5f;
    private static final float RANGE            = 20f;
    private static final float REBOUND_VELOCITY = .01f;
    private static final float MIN_SIZE         = 1f;
    private static final float SIZE_SCALE       = 100f;
    private static final long FLEE_TIME         = 2500;

    private static final float SCALE_V1         = .005f;
    private static final float SCALE_V2         = .01f;
    private static final float SCALE_V3         = .01f;
    private static final float SCALE_V4         = 1f;
    private static final float SCALE_V5         = .005f;
    private static final float SCALE_V6         = .01f;

    private static final float MIN_X = -500f;
    private static final float MAX_X = 500f;
    private static final float MIN_Y = -500f;
    private static final float MAX_Y = 500f;
    private static final float MIN_Z = -500f;
    private static final float MAX_Z = 500f;

    protected Boid boids[];

    // preallocation so we dont trigger gc
    final private Vector3 tmp = new Vector3();
    final private Vector3 v1 = new Vector3();
    final private Vector3 v2 = new Vector3();
    final private Vector3 v3 = new Vector3();
    final private Vector3 v4 = new Vector3();
    final private Vector3 v5 = new Vector3();
    final private Vector3 v6 = new Vector3();

    private KDTree tree;
    final private Vector3 flee_from = new Vector3();
    private long flee;

    public Flock(int num) {
        boids = new Boid[num];

        for(int i=0; i<num; i++) {
            boids[i] = new Boid(RandomGenerator.randomRange(-1, 1), 
                                RandomGenerator.randomRange(-1, 1),
                                RandomGenerator.randomRange(-1, 1),
                                RandomGenerator.randomRange(-1, 1), 
                                RandomGenerator.randomRange(-1, 1),
                                RandomGenerator.randomRange(-1, 1));
            //boids[i].seed = RandomGenerator.randomInt(0, 1000000);
            boids[i].seed = 0;
        }

        tree = new KDTree(num, 5);
    }
    
    // public void throttleUp() {
    //     if(alive < boids.length - 1)
    //         alive++;
        
    //     Log.d(TAG, "Throttle Up: alive = "  + alive);
    // }

    // public void throttleDown() {
    //     if(alive > 0)
    //         alive--;

    //     Log.d(TAG, "Throttle Down: alive = "  + alive);
    // }

    public void tick(long elapsed) {

        flee -= elapsed;

        tree.add(boids);

        for(int i=0; i<boids.length; i++) {
            v1.zero();
            v2.zero();
            v3.zero();
            v4.zero();
            v5.zero();
            v6.zero();

            Boid a = boids[i];
            a.age++;

            Boid neighbors[] = tree.findNeighbors(a, RANGE);
            int neighbor_count = 0;

            for(int j=0; j<neighbors.length; j++) {
                Boid b = boids[j];

                if(b != null) {
                    neighbor_count++;

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

            if(neighbor_count>0) {
                // Rule 1
                v1.scale((float)1.0/neighbor_count);
                v1.normalize();

                // Rule 3
                v3.scale((float)1.0/neighbor_count);
                v3.normalize();
            }
            
            // Rule 4 - Bound
            rule4(a);
            //rule5(a);
                
            if(flee>0) {
                // Rule 6 - gather/scatter
                rule6(a);
            } else {
                // Rule 5 - Center
                rule5(a);
            }

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

            a.color[0] = (a.seed + a.age) % 360;
            a.color[1] = 1 + (float)Math.sin((a.seed + a.age)/60f);
            a.color[2] = .4f + .3333f*(1 + (float)Math.cos((a.seed + a.age)/120f));

            a.size = MIN_SIZE + (a.position.z - MIN_Z) / 
                (MAX_Z - MIN_Z) * SIZE_SCALE;
        
            //Log.d(TAG, a.toString());
        }
    }


    // FIXME move this into kdtree 
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
        v1.scale(-10);
        v2.scale(100);
    }

    public void scare(Vector3 p) {
        flee = FLEE_TIME;
        flee_from.copy(p);
    }
}