package org.quuux.boids;

class Profile {
    public String name;

    public boolean RANDOMIZE_COLORS;

    public float MIN_SEED;
    public float MAX_SEED;

    public int FLOCK_SIZE;
    public float MAX_SIZE;
    public float MAX_VELOCITY;
    public float RANGE;
    public float REBOUND_VELOCITY;
    public float MIN_SIZE;
    public float SIZE_SCALE;
    public long FLEE_TIME;

    public float SCALE_V1;
    public float SCALE_V2;
    public float SCALE_V3;
    public float SCALE_V4;
    public float SCALE_V5;
    public float SCALE_V6;

    public float BONUS_V1 = 1;
    public float BONUS_V2 = 1;
    public float BONUS_V3 = 1;
    public float BONUS_V4 = 1;
    public float BONUS_V5 = 1;
    public float BONUS_V6 = 1;

    public float MIN_X;
    public float MAX_X;
    public float MIN_Y;
    public float MAX_Y;
    public float MIN_Z;
    public float MAX_Z;

    public float MIN_ALPHA;
    public int NEIGHBORS;

    public String toString() {
        return ProfileLoader.storeProfile(this).toString();
    }

}

