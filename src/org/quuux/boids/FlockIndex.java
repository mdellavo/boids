package org.quuux.boids;

import java.util.ArrayList;

abstract class NeighborIterator {
    public abstract boolean neighbor(Boid a, Boid b);
}


public class FlockIndex {

    protected ArrayList<Boid>[][][] index;

    protected int cells;

    protected int min_x;
    protected int max_x;
    protected int min_y;
    protected int max_y;
    protected int min_z;
    protected int max_z;

    public FlockIndex(int size, int cells, 
                      int min_x, int max_x, 
                      int min_y, int max_y, 
                      int min_z, int max_z) {
        
        this.cells = cells;

        this.min_x = min_x;
        this.max_x = max_x;

        this.min_y = min_y;
        this.max_y = max_y;

        this.min_z = min_z;
        this.max_z = max_z;

        index = new ArrayList[cells][cells][cells];

        for(int i=0; i<cells; i++)
            for(int j=0; j<cells; j++)
                for(int k=0; k<cells; k++)
                    index[i][j][k] = new ArrayList<Boid>();
    }

    protected ArrayList<Boid> getNeighboringCell(Boid b, int dx, int dy, int dz) {
        int i = (int)Math.floor((b.position.x - min_x) / (max_x - min_x) * cells) + dx;
        int j = (int)Math.floor((b.position.y - min_y) / (max_y - min_y) * cells) + dy;
        int k = (int)Math.floor((b.position.z - min_z) / (max_z - min_z) * cells) + dz;

        if(i < 0)
            i = 0;
        else if(i > cells - 1)
            i = cells-1;

        if(j < 0)
            j = 0;
        else if(j > cells - 1)
            j = cells-1;

        if(k < 0)
            k = 0;
        else if(k > cells - 1)
            k = cells-1;

        return index[i][j][k];
    }

    protected ArrayList<Boid> getCell(Boid b) {
        return getNeighboringCell(b, 0, 0, 0);
    }

    public void clear() {
        for(int i=0; i<index.length; i++)
            for(int j=0; j<index[i].length; j++)
                for(int k=0; k<index[i][j].length; k++)
                    index[i][j][k].clear();
    }
        
    public void add(Boid b) {
        ArrayList<Boid> cell = getCell(b);
        //if(cell.indexOf(b) == -1)
        cell.add(b);
    }

    // FIXME clearing and readding might be easier/faster
    public void remove(Boid b) {
        ArrayList<Boid> cell = getCell(b);
        int idx = cell.indexOf(b);
        if(idx > -1)
            cell.remove(idx);
    }

    public int neighbors(Boid b, NeighborIterator iter) {
        int rv = 0;

        for(int i=-1; i<2; i++) {
            for(int j=-1; j<2; j++) {
                for(int k=-1; k<2; k++) {
                    ArrayList<Boid> cell = getNeighboringCell(b, i, j, k);
                    if(cell != null) {
                        int size = cell.size();
                        
                        for(int n=0; n<size; n++)
                            if(iter.neighbor(b, cell.get(n)))
                                rv++;
                    }
                }
            }
        }
        
        return rv;
    }
}

