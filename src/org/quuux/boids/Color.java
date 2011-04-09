package org.quuux.boids;

public class Color {
    public float r,g,b,a;

    // does 0 make more sense?
    public Color() {
        r = 1f;
        g = 1f; 
        b = 1f;
        a = 1f;
    }

    public Color(float r, float g, float b, float a) {
	this.r = r;
	this.g = g;
	this.b = b;
	this.a = a;
    }

    public Color(float r, float g, float b) {
	this(r, g, b, 1.0f);
    }

    public String toString() {
	return "Color(" + r + ", " + g + ", " + b + ", " + a + ")";
    }
}

