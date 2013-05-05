package org.quuux.boids;

// TimSort is not in-place and causes too much gc churn in a tight loop 

import java.util.Comparator;

class Sorter {
    
    private static final Comparator ComparableComparator = new Comparator() {
            final public int compare(Object a, Object b) {
                return ((Comparable)a).compareTo(b);
            }
    };

    public static void sort(Comparable[] a) {
        sort(a, ComparableComparator);
    }
    
    private static void sort(Object[] a, Comparator comparator) {
        sort(a, 0, a.length - 1, comparator);
    }

    private static void sort(Object[] a, int low, int high,
                             Comparator comparator) {
        quicksort(a, 0, a.length - 1, comparator);
    }

    private static void quicksort(Object[] a, int low, int high,
                                        Comparator comparator) {

        if(low < high) {
            int pivot = partition(a, low, high, low + (high - low) / 2,
                                  comparator);
            quicksort(a, low, pivot - 1, comparator);
            quicksort(a, pivot + 1, high, comparator);
        }
    }

    private static int partition(Object[] a, int low, int high,
                                       int pivot_index, 
                                       Comparator comparator) {

        Object pivot = a[pivot_index];
        swap(a, pivot_index, high);
        int tmp = low;

        for(int i = low; i < high; i++) {
            if(comparator.compare(a[i], pivot) < 0) {
                swap(a, i, tmp);
                tmp++;
            }
        }

        swap(a, tmp, high);

        return tmp;
    }
    
    private static void swap(Object[] a, int i, int j) {
        Object tmp = a[i];
        a[i] = a[j];
        a[j] = tmp;
    }
}
