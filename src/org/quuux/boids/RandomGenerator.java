package org.quuux.boids;

import java.util.Random;

public class RandomGenerator {
    protected static Random random = new Random();
   
    public static float randomRange(float min, float max) {
        return min + ((max-min) * random.nextFloat());
    }

    public static int randomInt(int min, int max) {
        return min + random.nextInt(max-min);
    }
}
