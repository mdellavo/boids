package org.quuux.boids;

import android.util.Log;

import javax.microedition.khronos.opengles.GL10;
import javax.microedition.khronos.opengles.GL11;

public class Flock {
    protected static final String TAG = "Flock";

    protected Profile profile;
    protected Boid boids[];
    protected KDTree tree;

    // preallocation so we dont trigger gc
    final protected Vector3 tmp = new Vector3();
    final protected Vector3 v1 = new Vector3();
    final protected Vector3 v2 = new Vector3();
    final protected Vector3 v3 = new Vector3();
    final protected Vector3 v4 = new Vector3();
    final protected Vector3 v5 = new Vector3();
    final protected Vector3 v6 = new Vector3();
    final protected Vector3 v7 = new Vector3();

    final protected Vector3 origin = new Vector3(0, 0, -100f);
    final protected Vector3 focal = new Vector3();
    final protected Vector3 center = new Vector3();

    protected long flee;
    protected int alive;
    
    final public void init(Profile profile) {
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
                boids[i].seed = RandomGenerator.randomInt(0, 100000);

            Log.d(TAG, boids[i].toString());

            alive++;
        }

        tree = new KDTree(profile.FLOCK_SIZE, profile.NEIGHBORS);        

        center.copy(origin);
    }
    
   final public Profile getProfile() {
        return profile;
    }
    
    final public int throttleUp() {
        for(int i=0; i<boids.length; i++) {
            if(!boids[i].alive) {
                boids[i].alive = true;
                alive++;
                Log.d(TAG, "Throttled up to " + alive + " boids");
                break;
            }
        }           
        
        return alive;
    }

    final public int throttleDown() {
        for(int i=0; i<boids.length; i++) {
            if(boids[i].alive) {
                boids[i].alive = false;
                alive--;

                Log.d(TAG, "Throttled down to " + alive + " boids");
                break;
            }
        }            
        
        return alive;
    }

    final public void tick(long elapsed) {

        if(flee > 0)
            flee -= elapsed;

        if(flee < 0)
            flee = 0;
       
        tree.add(boids);

        for(int i=0; i<boids.length; i++) {
            Boid a = boids[i];

            a.age += RandomGenerator.randomInt(0, 2);

            if(!a.alive)
                continue;

            v1.zero();
            v2.zero();
            v3.zero();
            v4.zero();
            v5.zero();
            v6.zero();

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

                    // FIXME Does this actually work?                    
                    if(tmp.magnitude() < profile.RANGE)
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

            // Rule 5 - Center
            rule5(a);
            
            if(flee>0) {
                if(center.distance(focal) > 0)
                    ease(center, focal);
            } else if(flee == 0) {
                if(center.distance(origin) > 0)
                    ease(center, origin);
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
            a.velocity.add(v7);


            // limit velocity
            if(a.velocity.magnitude() > profile.MAX_VELOCITY) {
                a.velocity.normalize();
                a.velocity.scale(profile.MAX_VELOCITY);            
            }

            tmp.zero();
            tmp.copy(a.velocity);
            
            //Log.d(TAG, "elapsed=" + elapsed);            
            tmp.scale(elapsed/30f);

            // apply velocity to position
            a.position.add(tmp);
          
            //Log.d(TAG, "depth_percentile: " + depth_percentile);
            
            a.size = scaleRange(a.position.z,
                                profile.MIN_Z, profile.MAX_Z, 
                                profile.MIN_SIZE, profile.SIZE_SCALE);

            a.opacity = scaleRange(a.position.z,
                                   profile.MIN_Z, profile.MAX_Z, 
                                   1.0f, 0.0f);

            a.color[0] = (a.seed + a.age) % 360;
            a.color[1] = 1 + (float)Math.sin((a.seed + a.age)/60f);
            a.color[2] = .4f + .3333f*(1 + (float)Math.cos((a.seed + a.age)/120f));
            a.color[3] = scaleRange(a.position.z, profile.MIN_Z, profile.MAX_Z, 0, 1);
            
            //Log.d(TAG, a.toString());
        }
    }

    // FIXME bad math, should be able to handle min > max
    final protected float scaleRange(float n,
                               float min_a, float max_a,
                               float min_b, float max_b) {
        return (n / ((max_a - min_a) / (max_b - min_b))) + min_b;
    }

    final protected float percentile(float min, float max, float n) {
        return (n - min) / (max - min);
    } 

    final protected float  ease(float a, float b) {
        float dx = b - a;

        if(Math.abs(dx) > 1)
            dx /= 0.5f;

        return dx;
    } 
    
    final protected void ease(Vector3 a, Vector3 b) {
        tmp.copy(b);
        tmp.subtract(a);
        
        if(tmp.magnitude() > 1f)
            tmp.scale(0.5f);
        
        Log.d(TAG, "easing from " + a + " to " + b + " at " + tmp);

        a.add(tmp);
    }

    // FIXME move this into kdtree 
    final private boolean inRange(Boid a, Boid b) {
        tmp.zero();
        tmp.add(a.position);
        tmp.subtract(b.position);
        return tmp.magnitude() < profile.RANGE;
    }

    // Rule 4 - keep within bounds
    final private void rule4(Boid b) {
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
    final private void rule5(Boid b) {
        v5.copy(center);
        v5.subtract(b.position);
    }
    
    final public void touch(Vector3 p) {
        flee = profile.FLEE_TIME;

        Log.d(TAG, "touch: " + p);

        focal.copy(p);
        focal.z = center.z + 50;
    }
    
    final public void push(Vector3 f) {
        v7.copy(f);
        v7.scale(v1.magnitude() * -10);
    }
}
