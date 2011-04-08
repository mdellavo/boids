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
        for(int i=0; i<boids.length; i++);
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

    private Vector3 averagePosition(int b) {
        Vector3 position = sumPositions(b);
        v1.copy(position);
        v1.scale((float)1.0/boids.length);
        return v1;
    }

    // Rule 2 - Maintain seperation
    private Vector3 avoid(int b) {
        v2.zero();
        
        for(int i=0; i<boids.length; i++) {
            if(i != b) {
            }
        }

        return v2;
    }

    // Rule 3 - match the average velocity of other boids
    private Vector3 sumVelocities(int b) {
        tmp.zero();

        for(int i=0; i<boids.length; i++)
            if(i != b)
                tmp.add(boids[i].velocity);
        
        return tmp;
    }

    private Vector3 averageVelocity(int b) {        
        Vector3 velocity = sumVelocities(b);
        v3.copy(velocity);
        v3.scale((float)1.0/boids.length);
        return v3;
    }

}