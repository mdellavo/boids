package org.quuux.boids;

public class Boid {
    public Vector3 position = new Vector3();
    public Vector3 velocity = new Vector3();

    public Boid(float x, float y, float z) {
        position.x = x;
        position.y = y;
        position.z = z;
    }

    public Boid(float x, float y, float z, float dx, float dy, float dz) {
        this(x,y,z);
        velocity.x = dx;
        velocity.y = dy;
        velocity.z = dz;
    }

    public String toString() {
        return "Boid(position=" + position + ", velocity=" + velocity + ")";
    }
}
