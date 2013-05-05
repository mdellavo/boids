package org.quuux.boids;

import android.util.Log;

import java.util.concurrent.Semaphore;

class FlockThread extends Thread implements Runnable {
    private static final String TAG = "FlockThread";

    private Flock flock;
    private FlockBuffer buffer;
    private boolean running;
    private long frames;
    private long last;
    protected Semaphore lock;

    public FlockThread(Flock flock, FlockBuffer buffer) {
        this.flock = flock;
        this.buffer = buffer;
    }

    final public void setFlock(Flock flock) {
        this.flock = flock;
    }

    final public void setBuffer(FlockBuffer buffer) {
        this.buffer = buffer;
    }

    final public void run() {
        long total_elapsed = 0;

        last = System.currentTimeMillis();
        while(true) {
            frames++;

            long now = System.currentTimeMillis();
            long elapsed = now - last;
            last = now;

            synchronized(this) {
                while(!running) {
                    try {
                        wait();
                    } catch(InterruptedException e) {
                    }
                }
            }

            flock.tick(elapsed);
            buffer.add(flock);
            buffer.swap();

            total_elapsed += elapsed;
            if(total_elapsed > 1000) {

                Log.d(TAG, "ticked " + flock.profile.FLOCK_SIZE + " boids at fps: " + frames);

                if(frames < 54)
                    flock.throttleDown();
                else if(frames >= 66)
                    flock.throttleUp();

                total_elapsed = 0;
                frames = 0;
            }

            if(elapsed < 16) {
                try {
                    Thread.sleep(16 - elapsed);
                } catch(InterruptedException e) {
                }

            }
        }
    }

    final public synchronized void pauseSimulation() {
        running = false;
        Log.d(TAG, "pause thread");
        notifyAll();
    }

    final public synchronized void resumeSimulation() {
        running = true;
        last = System.currentTimeMillis();
        Log.d(TAG, "resume thread");
        notifyAll();
    }
}
