package org.quuux.boids;

public class Boid {
    public Vector3 position = new Vector3();
    public Vector3 velocity = new Vector3();

    public String toString() {
        return "Boid(position=" + position + ", velocity=" + velocity + ")";
    }
}
