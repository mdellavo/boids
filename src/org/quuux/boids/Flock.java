package org.quuux.boids;

import android.util.Log;

import javax.microedition.khronos.opengles.GL10;
import javax.microedition.khronos.opengles.GL11;

import java.util.Arrays;
import java.util.Comparator;

class DepthComparator implements Comparator {
    public final int compare(Object a, Object b) {
        Boid ba = (Boid)a;
        Boid bb = (Boid)b;

        int rv = Float.compare(ba.position.z, bb.position.z);

        if (rv == 0)
            rv = Float.compare(ba.position.y, bb.position.y);

        if (rv == 0)
            rv = Float.compare(ba.position.x, bb.position.x);
        
        return rv; 
    }
}

public class Flock {
    protected static final String TAG = "Flock";

    protected Profile profile;
    protected Boid boids[];
    protected BinLattice bin;

    protected DepthComparator comparator = new DepthComparator();

    // preallocation so we dont trigger gc
    final protected Vector3 tmp = new Vector3();
    final protected Vector3 v1 = new Vector3();
    final protected Vector3 v2 = new Vector3();
    final protected Vector3 v3 = new Vector3();
    final protected Vector3 v4 = new Vector3();
    final protected Vector3 v5 = new Vector3();
    final protected Vector3 v6 = new Vector3();
    final protected Vector3 v7 = new Vector3();

    final protected Vector3 origin = new Vector3(0, 0, 150f);
    final protected Vector3 focal = new Vector3();
    final protected Vector3 center = new Vector3();

    protected long flee;
    protected int alive;

    private int last_histogram = 0;
    
    final public void init(Profile profile) {
        this.profile = profile;
        boids = new Boid[profile.FLOCK_SIZE * 2];

        for(int i=0; i<boids.length; i++) {
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

            //Log.d(TAG, boids[i].toString());
            if(i < profile.FLOCK_SIZE)
                alive++; 
            else
                boids[i].alive = false;
        }

        if(profile.RANDOMIZE_COLORS) 
            randomizeColors();

        bin = new BinLattice(profile);
        bin.add(boids);

        center.copy(origin);
    }
    
    final public Profile getProfile() {
        return profile;
    }

    final public void randomizeColors() {
        for(int i=0; i<boids.length; i++)
            boids[i].seed = RandomGenerator.randomInt(0, 100000);
    }

    final public void normalizeColors() {
        int seed = RandomGenerator.randomInt(0, 100000);
        for(int i=0; i<boids.length; i++)
            boids[i].seed = seed;
    }

    final public void sort() {
        Arrays.sort(boids, 0, boids.length, comparator);
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

        if(flee > 0) {
            flee -= elapsed;
            v7.scale(.25f);
        } else if(flee < 0) {
            flee = 0;
            v7.zero();
            focal.copy(origin);
        }
        
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

            BinLattice.Visitor visitor = new BinLattice.Visitor() {
                    public void visit(Boid a, Boid b) {
                        
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
            };

            int neighbors = bin.scan(a, (int)profile.RANGE, visitor, profile.NEIGHBORS);
            //Log.d(TAG, "number of neighbors: " + neighbors);

            if(neighbors>0) {
                // Rule 1
                v1.scale((float)1.0/neighbors);
                v1.normalize();

                // Rule 3
                v3.scale((float)1.0/neighbors);
                v3.normalize();
            }

            // Rule 4 - Bound
            rule4(a);

            if(flee > 0) {
                center.copy(focal);
            }

            // Rule 5 - Center
            rule5(a);

            // Scale the results
            v1.scale(profile.SCALE_V1);
            v2.scale(profile.SCALE_V2);
            v3.scale(profile.SCALE_V3);
            v4.scale(profile.SCALE_V4);
            v5.scale(profile.SCALE_V5);
            v6.scale(profile.SCALE_V6);          

            if(flee>0) {
                v1.scale(-10);
            }
            
            // Scale according to settings
            v1.scale(profile.BONUS_V1 / 100.0f);
            v2.scale(profile.BONUS_V2 / 100.0f);
            v3.scale(profile.BONUS_V3 / 100.0f);
            v4.scale(profile.BONUS_V4 / 100.0f);
            v5.scale(profile.BONUS_V5 / 100.0f);
            v6.scale(profile.BONUS_V6 / 100.0f);

            // Log.d(TAG, "v1=" + v1);
            // Log.d(TAG, "v2=" + v2);
            // Log.d(TAG, "v3=" + v3);
            // Log.d(TAG, "v4=" + v4);

            // Combine components
            
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

            tmp.scale(profile.BONUS_VELOCITY / 100f);
            if(flee > 0) {
                float fleeing_velocity = scaleRange(flee,
                                                    0,
                                                    profile.FLEE_TIME, 
                                                    1,
                                                    4);
                tmp.scale(fleeing_velocity);
            }

            bin.remove(a);

            // apply velocity to position
            a.position.add(tmp);
          
            bin.add(a);

            //Log.d(TAG, "depth_percentile: " + depth_percentile);

            a.size = 1000f;

            a.opacity = scaleRange(a.position.z,
                                   profile.MIN_Z, 0, 
                                   0f, 1f);

            a.color[0] = (a.seed + a.age) % 360;
            a.color[1] = .75f + (.25f * (float)Math.sin((a.seed + a.age)/60f));
            a.color[2] = .75f + (.25f * (float)Math.cos((a.seed + a.age)/120f));
            a.color[3] = a.opacity;

            //Log.d(TAG, a.toString());
        }
        
        last_histogram += elapsed;
        if(last_histogram > 30 * 1000) {
            bin.histogram();
            last_histogram = 0;
        }
        
    }

    // FIXME bad math, should be able to handle min > max
    final protected float scaleRange(float x,
                                     float min, float max,
                                     float a, float b) {
        return (b-a) * (x-min) / (max - min) + a;
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
        Log.d(TAG, "touch: " + p);
        flee = profile.FLEE_TIME;
        p.normalize();
        p.scale(50);
        focal.copy(p);
        focal.z = 200f;
    }
    
    final public void push(Vector3 f) {
        flee = profile.FLEE_TIME; 
        f.normalize();
        f.scale(50);
        focal.copy(f);
        Log.d(TAG, "pushing focal to " + focal);
    }
}
