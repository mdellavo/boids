package org.quuux.boids;

class Color {
    private final float r;
    private final float g;
    private final float b;
    private final float a;

    // does 0 make more sense?
    public Color() {
        r = 1f;
        g = 1f; 
        b = 1f;
        a = 1f;
    }

    private Color(float r, float g, float b, float a) {
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

