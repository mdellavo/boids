
package org.quuux.boids;

import android.util.Log;

import java.util.Arrays;
import java.util.List;
import java.util.Comparator;

class Node {
    public Boid boid;
    public Node left, right;

    public Node(Boid boid, Node left, Node right) {
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

class NodePool {
    public Node[] nodes;
    public int next;

    public NodePool(int size) {
        nodes = new Node[size];
        for(int i=0; i<size; i++)
            nodes[i] = new Node(null, null, null);
    }

    public Node getNode() {
        Node rv = null;

        if(next < nodes.length) {
            rv = nodes[next++];
            rv.boid = null;
            rv.left = null;
            rv.right = null;
        }

        return rv;
    }

    public void recycle() {
        next = 0;
    }
}

class BoidComparator implements Comparator {
    private int component;

    public BoidComparator(int component) {
        this.component = component;
    }

    public int compare(Object a, Object b) {
        Boid ba = (Boid)a;
        Boid bb = (Boid)b;
        return Float.compare(ba.position.component(this.component), 
                             bb.position.component(this.component)); 
    }
}

class NeighborResult {
    public float distances[];
    public Boid boids[];
    public int found;

    public NeighborResult(int num) {
        distances = new float[num];
        boids = new Boid[num];
    }

    public void add(float distance, Boid boid) {
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

    public void clear() {
        found = 0;
    }
}

public class KDTree {
    private static final String TAG = "KDTree";
    
    private Node root;
    private NodePool pool;
    private NeighborResult neighbors;
    private BoidComparator[] comparators;

    public KDTree(int pool_size, int max_neighbors) {
        pool = new NodePool(pool_size);
        neighbors = new NeighborResult(max_neighbors);
        
        comparators = new BoidComparator[3];
        for(int i=0; i<comparators.length; i++)
            comparators[i] = new BoidComparator(i);
    }        

    public String toString() {
        return root.toString();
    }
    
    public void add(Boid[] boids) {
        pool.recycle();
        root = build(boids, 0, 0, boids.length);
    }

    public void clear() {
        pool.recycle();
    }
    
    public boolean isEmpty() {
        return root != null;
    }

    private Node build(Boid[] boids, int depth, int start, int end) {

        if((end-start) < 1)
            return null;            

        int component = depth % 3;
        int median = (end + start) / 2;

        Arrays.sort(boids, start, end, comparators[component]);

        Node node = pool.getNode();
        node.boid = boids[median];
        node.left = build(boids, depth+1, start, median);
        node.right = build(boids, depth+1, median + 1, end);

        return node;
    }

    public void dumpTree() {
        Log.d(TAG, "KDTree: " + toString());
    }

    private float distance(Boid a, Boid b) {
        return (float)(Math.pow(a.position.x - b.position.x, 2) + 
                       Math.pow(a.position.y - b.position.y, 2) + 
                       Math.pow(a.position.z - b.position.z, 2));
    }

    public Boid[] findNeighbors(Boid boid) {
        neighbors.clear();
        search(boid, root, 0);
        return neighbors.boids;
    }

    private void search(Boid boid, Node node, int depth) {
        if(node == null)
            return;

        float dist = distance(boid, node.boid);
        neighbors.add(dist, node.boid);

        int component = depth % 3;
        
        Node near = nearChild(boid, node, component);
        Node far = farChild(boid, node, component);

        search(boid, near, depth+1);
        if(Math.pow(dist, 2) < neighbors.distances[0])
            search(boid, far, depth+1);       
    }
    
    private Node nearChild(Boid a, Node node, int component) {
        if(a.position.component(component) <= node.boid.position.component(component))
            return node.left;
        else
            return node.right;
    }
    
    private Node farChild(Boid a, Node node, int component) {
        if(a.position.component(component) <= node.boid.position.component(component))
            return node.right;
        else
            return node.left;
    }
}