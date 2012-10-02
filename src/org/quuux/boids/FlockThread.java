package org.quuux.boids;

import android.util.Log;

class FlockThread extends Thread implements Runnable {
    private static final String TAG = "FlockThread";

    protected Flock flock;
    protected FlockBuffer buffer;
    protected boolean running;
    protected long frames;
    protected long last;

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

                notifyAll();
            }

            flock.tick(elapsed);
            buffer.render(flock);

            total_elapsed += elapsed;
            if(total_elapsed > 1000) {

                Log.d(TAG, "ticked fps: " + frames);

                if(frames < 30)
                    flock.throttleDown();
                else if(frames >= 30)
                    flock.throttleUp();

                total_elapsed = 0;
                frames = 0;
            }

            if(elapsed < 33) {
                try {
                    Thread.sleep(33 - elapsed);
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
