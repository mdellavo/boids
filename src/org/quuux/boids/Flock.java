package org.quuux.boids;

import android.util.Log;

import javax.microedition.khronos.opengles.GL10;
import javax.microedition.khronos.opengles.GL11;

public class Flock {
    protected static final String TAG = "Flock";

    protected Profile profile;
    protected Boid boids[];

    // preallocation so we dont trigger gc
    final protected Vector3 tmp = new Vector3();
    final protected Vector3 v1 = new Vector3();
    final protected Vector3 v2 = new Vector3();
    final protected Vector3 v3 = new Vector3();
    final protected Vector3 v4 = new Vector3();
    final protected Vector3 v5 = new Vector3();
    final protected Vector3 v6 = new Vector3();

    protected KDTree tree;
    final protected Vector3 flee_from = new Vector3();
    protected long flee;

    public void init(Profile profile) {
        this.profile = profile;
        boids = new Boid[profile.FLOCK_SIZE];

        for(int i=0; i<profile.FLOCK_SIZE; i++) {
            boids[i] = new Boid(RandomGenerator.randomRange(profile.MIN_SEED,
                                                            profile.MAX_SEED), 
                                RandomGenerator.randomRange(profile.MIN_SEED,
                                                            profile.MAX_SEED),
                                RandomGenerator.randomRange(profile.MIN_SEED,
                                                            profile.MAX_SEED),
                                RandomGenerator.randomRange(profile.MIN_SEED,
                                                            profile.MAX_SEED), 
                                RandomGenerator.randomRange(profile.MIN_SEED,
                                                            profile.MAX_SEED),
                                RandomGenerator.randomRange(profile.MIN_SEED,
                                                            profile.MAX_SEED));

            if(profile.RANDOMIZE_COLORS) 
                boids[i].seed = RandomGenerator.randomInt(0, 1000000);
        }

        tree = new KDTree(profile.FLOCK_SIZE, 5);        
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

            Boid neighbors[] = tree.findNeighbors(a, profile.RANGE);
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
                
            // FIMXE move center to touch position and gradually reset
            if(flee>0) {
                // Rule 6 - gather/scatter
                rule6(a);
            } else {
                // Rule 5 - Center
                rule5(a);
            }

            // Scale the results
            v1.scale(profile.SCALE_V1);
            v2.scale(profile.SCALE_V2);
            v3.scale(profile.SCALE_V3);
            v4.scale(profile.SCALE_V4);
            v5.scale(profile.SCALE_V5);
            v6.scale(profile.SCALE_V6);

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
            if(a.velocity.magnitude() > profile.MAX_VELOCITY) {
                a.velocity.normalize();
                a.velocity.scale(profile.MAX_VELOCITY);            
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
            
            a.size = profile.MIN_SIZE + (1- (a.position.z - profile.MIN_Z) / 
                                         (profile.MAX_Z - profile.MIN_Z)) * profile.SIZE_SCALE;

            //Log.d(TAG, a.toString());
        }
    }


    // FIXME move this into kdtree 
    private boolean inRange(Boid a, Boid b) {
        tmp.zero();
        tmp.add(a.position);
        tmp.subtract(b.position);
        return tmp.magnitude() < profile.RANGE;
    }

    // Rule 4 - keep within bounds
    private void rule4(Boid b) {
        if(b.position.x < profile.MIN_X)
            v4.x = profile.REBOUND_VELOCITY;
        else if(b.position.x > profile.MAX_X)
            v4.x = -profile.REBOUND_VELOCITY;
        else if(b.position.y < profile.MIN_Y)
            v4.y = profile.REBOUND_VELOCITY;
        else if(b.position.y > profile.MAX_Y)
            v4.y = -profile.REBOUND_VELOCITY;
        else if(b.position.z < profile.MIN_Z)
            v4.z = profile.REBOUND_VELOCITY;
        else if(b.position.z > profile.MAX_Z)
            v4.z = -profile.REBOUND_VELOCITY;
    }

    // Rule 5 - tend towards center
    private void rule5(Boid b) {
        v5.z = -100; // FIXME
        v5.subtract(b.position);
    }

    // Rule 6 - fleeing / scattering
    private void rule6(Boid b) {
        v6.zero();
        v6.copy(flee_from);
        v6.subtract(b.position);
        v6.scale(10);
    }
    
    public void scare(Vector3 p) {
        flee = profile.FLEE_TIME;
        flee_from.copy(p);
    }
}