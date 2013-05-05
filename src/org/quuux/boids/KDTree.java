
package org.quuux.boids;

import android.util.Log;

import java.util.Arrays;
import java.util.Comparator;

class KDNode {
    public Boid boid;
    public KDNode left, right;

    public KDNode(Boid boid, KDNode left, KDNode right) {
        this.boid = boid;
        this.left = left;
        this.right = right;
    }

    public String toString() {
        String left_s = (left != null) ? left.toString() : "";
        String right_s = (right != null) ? right.toString() : "";
        return "(" + boid.position + ", " + left_s + ", " + right_s + ")";
    }
}

class KDNodePool {
    private final KDNode[] KDNodes;
    private int next;

    public KDNodePool(int size) {
        KDNodes = new KDNode[size];
        for(int i=0; i<size; i++)
            KDNodes[i] = new KDNode(null, null, null);
    }

    final public KDNode getNode() {
        KDNode rv = null;

        if(next < KDNodes.length) {
            rv = KDNodes[next++];
            rv.boid = null;
            rv.left = null;
            rv.right = null;
        }

        return rv;
    }

    final public void recycle() {
        next = 0;
    }
}

class BoidComparator implements Comparator {
    private final int component;

    public BoidComparator(int component) {
        this.component = component;
    }

    public final int compare(Object a, Object b) {
        Boid ba = (Boid)a;
        Boid bb = (Boid)b;
        return Float.compare(ba.position.component(this.component), 
                             bb.position.component(this.component)); 
    }
}

class NeighborResult {
    private final float[] distances;
    public final Boid[] boids;
    private int found;

    public NeighborResult(int num) {
        distances = new float[num];
        boids = new Boid[num];
    }

    final public void add(float distance, Boid boid) {
        int i = 0;

        while(i<found && i<distances.length && distances[i] < distance)
            i++;

        if(i < distances.length) {
            int length = Math.min(distances.length - i, found - i) - 1;

            if(length > 0) {
                System.arraycopy(distances, i, distances, i + 1, length);
                System.arraycopy(boids, i, boids, i + 1, length);
            }

            distances[i] = distance;
            boids[i] = boid;

            found = Math.min(found + 1, distances.length);
        }
    }    

    final public void clear() {
        found = 0;
    }
}

class KDTree {
    private static final String TAG = "KDTree";
    
    private KDNode root;
    private final KDNodePool pool;
    private final NeighborResult neighbors;
    private final BoidComparator[] comparators;

    private final Boid[] tmp;

    public KDTree(int size, int max_neighbors) {
        pool = new KDNodePool(size);
        neighbors = new NeighborResult(max_neighbors);        
        tmp = new Boid[size];
        
        comparators = new BoidComparator[3];
        for(int i=0; i<comparators.length; i++)
            comparators[i] = new BoidComparator(i);
    }        

    public String toString() {
        return root.toString();
    }
    
    final public void add(Boid[] boids) {
        pool.recycle();

        int alive = 0;
        for(int i=0; i<boids.length; i++) {
            if(boids[i].alive) {
                tmp[alive] = boids[i];
                alive++;
            }
        }

        root = build(boids, 0, 0, alive);
    }

    final public void clear() {
        pool.recycle();
    }
    
    final public boolean isEmpty() {
        return root != null;
    }

    private KDNode build(Boid[] boids, int depth, int start, int end) {

        if((end-start) < 1)
            return null;            

        int component = depth % 3;
        int median = (end + start) / 2;
        
        // FIXME my in-place sorter is broken or timsort is magic
        //Sorter.sort(boids, start, end, comparators[component]);
        Arrays.sort(boids, start, end, comparators[component]);

        KDNode KDNode = pool.getNode();
        KDNode.boid = boids[median];
        KDNode.left = build(boids, depth+1, start, median);
        KDNode.right = build(boids, depth+1, median + 1, end);

        return KDNode;
    }

    final public void dumpTree() {
        if (BuildConfig.DEBUG) Log.d(TAG, "KDTree: " + toString());
    }

    final private float distance(Boid a, Boid b) {
        return (float)(Math.pow(a.position.x - b.position.x, 2) + 
                       Math.pow(a.position.y - b.position.y, 2) + 
                       Math.pow(a.position.z - b.position.z, 2));
    }

    final public Boid[] findNeighbors(Boid boid, float range) {
        neighbors.clear();
        search(boid, range, root, 0);
        return neighbors.boids;
    }

    private void search(Boid boid, float range, KDNode KDNode, int depth) {
        if(KDNode == null)
            return;

        float dist = distance(boid, KDNode.boid);
        neighbors.add(dist, KDNode.boid);

        int component = depth % 3;
        
        KDNode near = nearChild(boid, range, KDNode, component);
        KDNode far = farChild(boid, range, KDNode, component);

        search(boid, range, near, depth+1);
        if(Math.pow(dist, 2) < Math.pow(range, 2))
            search(boid, range, far, depth+1);       
    }
    
    private KDNode nearChild(Boid a, float range, KDNode KDNode, int component) {
        if(a.position.component(component) <= KDNode.boid.position.component(component))
            return KDNode.left;
        else
            return KDNode.right;
    }
    
    private KDNode farChild(Boid a, float range, KDNode KDNode, int component) {
        if(a.position.component(component) <= KDNode.boid.position.component(component))
            return KDNode.right;
        else
            return KDNode.left;
    }
}
