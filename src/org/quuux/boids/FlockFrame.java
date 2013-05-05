package org.quuux.boids;

import android.graphics.Color;


import java.util.Arrays;
import java.util.Comparator;

class FlockNode {

    public final float[] position = new float[3];
    public float size;
    public final float[] color = new float[4];

    public void copy(Boid b) {
        position[0] = b.position.x;
        position[1] = b.position.y;
        position[2] = b.position.z;

        size = b.size;
                
        int alpha = Math.round(b.color[3] * 255);
        int rgb = Color.HSVToColor(alpha, b.color);
        color[0] = (float)Color.red(rgb) / 255f;
        color[1] = (float)Color.green(rgb) / 255f;
        color[2] = (float)Color.blue(rgb) / 255f;
        color[3] = b.opacity;
    }
}


class DepthComparator implements Comparator {
    public final int compare(Object a, Object b) {
        FlockNode ba = (FlockNode)a;
        FlockNode bb = (FlockNode)b;
        return Float.compare(ba.position[2], bb.position[2]);
    }
}

class FlockFrame {
    private static final String TAG = "FlockFrame";

    private FlockNode[] FlockNodes;
    
    private int count = 0;
    private int size = 0;

    private boolean dirty = false;

    private float[] positions;
    private float[] sizes;
    private float[] colors;


    private final DepthComparator comparator = new DepthComparator();


    public FlockFrame(int size) {
        this.count = 0;
        allocate(size);
    }

    void allocate(int size) {
        this.size = size;

        FlockNodes = new FlockNode[size];

        for(int i=0; i<size; i++)
            FlockNodes[i] = new FlockNode();

        positions = new float[size * 3];
        sizes = new float[size];
        colors = new float[size * 4];        

        clear();
    }

    public void add(Boid b) {
        if(count < size) {
            FlockNodes[count++].copy(b);
            dirty = true;
        }
    }

    public void clear() {
        count = 0;
        dirty = false;
    }

    void render() {
        
        Arrays.sort(FlockNodes, 0, count, comparator);

        for(int i=0; i<count; i++) { 
            int p_offset = i * 3; 
            positions[p_offset] = FlockNodes[i].position[0];
            positions[p_offset + 1] = FlockNodes[i].position[1];
            positions[p_offset + 2] = FlockNodes[i].position[2];

            int c_offset = i * 4;
            colors[c_offset] = FlockNodes[i].color[0];
            colors[c_offset + 1] = FlockNodes[i].color[1];
            colors[c_offset + 2] = FlockNodes[i].color[2];
            colors[c_offset + 3] = FlockNodes[i].color[3];

            sizes[i] = FlockNodes[i].size;
        }

        dirty = false;
    }

    public int getCount() {
        return count;
    }

    public float[] getPositions() {
        if(dirty) {
            render();
        }

        return positions;
    }

    public float[] getColors() {
        if(dirty) {
            render();
        }

        return colors;
    }

    public float[] getSizes() {
        if(dirty) {
            render();
        }

        return sizes;
    }

}
