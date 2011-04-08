package org.quuux.boids;

import javax.microedition.khronos.opengles.GL10;

public class Flock {

    public Boid boids[];

    // preallocation so we dont trigger gc
    private Vector3 tmp;
    private Vector3 v1;
    private Vector3 v2;
    private Vector3 v3;

    public Flock(int num) {
        boids = new Boid[num];
        for(int i=0; i<num; i++)
            boids[i] = new Boid();

        tmp = new Vector3();
        v1 = new Vector3();
        v2 = new Vector3();
        v3 = new Vector3();
    }

    public void init(GL10 gl) {
        
    }

    public void tick(long elapsed) {
        for(int i=0; i<boids.length; i++) {
            v1 = averagePosition(i, v1);
            v2 = avoid(i, v2);
            v3 = averageVelocity(i, v3);
            
            // FIXME 
            v1.scale(0.01f);
            v3.scale(0.125f);

            boids[i].velocity.add(v1);
            boids[i].velocity.add(v2);
            boids[i].velocity.add(v3);

            boids[i].position.add(boids[i].velocity);
        }
    }

    public void draw(GL10 gl) {
        
    }

    // Rule 1 - match the average position of other boids
    private Vector3 sumPositions(int b) {
        tmp.zero();

        for(int i=0; i<boids.length; i++)
            if(i != b)
                tmp.add(boids[i].position);
        
        return tmp;
    }

    private Vector3 averagePosition(int b, Vector3 v) {
        v.copy(sumPositions(b));
        v.scale((float)1.0/boids.length);
        return v;
    }

    // Rule 2 - Maintain seperation
    private Vector3 avoid(int b, Vector3 v) {
        v.zero();
        
        for(int i=0; i<boids.length; i++) {
            if(i != b) {
                tmp.copy(boids[i].position);
                tmp.subtract(boids[b].position);

                if(tmp.magnitude() < 100f)
                    v.subtract(tmp);
            }
        }

        return v;
    }

    // Rule 3 - match the average velocity of other boids
    private Vector3 sumVelocities(int b) {
        tmp.zero();

        for(int i=0; i<boids.length; i++)
            if(i != b)
                tmp.add(boids[i].velocity);
        
        return tmp;
    }

    private Vector3 averageVelocity(int b, Vector3 v) {
        v.copy(sumVelocities(b));
        v.scale((float)1.0/boids.length);
        return v;
    }

}