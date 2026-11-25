package org.esmile.basic.collections;

import java.util.*;
import java.util.concurrent.ArrayBlockingQueue;

public class Lists {
    public static void main(String[] args) {
//        arrayList();
        queue();

    }

    static void arrayList() {
        // transient Object[] elementData;
        List<Integer> list = new ArrayList<>();
        list.add(1);
        list.add(2);

        list.add(1, 4);
    }

    /**
     * private static class Node<E> {
     * E item;
     * Node<E> next;
     * Node<E> prev;
     * }
     */
    void linkedList() {
        // usage1: 作为队列 FIFO
        Queue<Integer> queue = new LinkedList<>();
        boolean offer = queue.offer(1);
        Integer peek = queue.peek();
        Integer poll = queue.poll();

        // 升级版本：ArrayDeque
        ArrayDeque<Integer> arrayDeque = new ArrayDeque<>();
        boolean offer1 = arrayDeque.offer(1);
        Integer peek1 = arrayDeque.peek();
        Integer poll1 = arrayDeque.poll();

        // arraydeque 作为栈
        ArrayDeque<Integer> stack = new ArrayDeque<>();
        stack.push(1);
        Integer peek2 = stack.peek();
        Integer poll2 = stack.pop();

        // 让位于ArrayDeque
        // Stack
    }

    // FIFO: offer, poll, peek
    static void queue() {
        // ArrayDeque：90% 情况用它
        ArrayDeque<Integer> arrayDeque = new ArrayDeque<>();
        boolean offer1 = arrayDeque.offer(1);
        Integer peek1 = arrayDeque.peek();
        Integer poll1 = arrayDeque.poll();

        // PriorityQueue
        PriorityQueue<Integer> minHeap = new PriorityQueue<>((Integer::compareTo));
        PriorityQueue<Integer> maxHeap = new PriorityQueue<>((Comparator.reverseOrder()));
        minHeap.offer(8);
        minHeap.offer(5);
        minHeap.offer(7);
        minHeap.offer(3);
        System.out.println(minHeap); // [3, 5, 7, 8]

        // BlockingQueue：四类：解耦搬运、流量控制、任务编排、特殊语义交接
//        PriorityQueue
    }

    ArrayBlockingQueue abq = new ArrayBlockingQueue(1_000);

    public record order(int id, String name, double price) {}

    public void order(order o){
        abq.offer(o);
    }

    public void handleOrder() {
        Runnable processor = () -> {
           while (true) {
                order o = (order) abq.poll();
                // process order
               try {
                   Thread.sleep(1000);
               } catch (InterruptedException e) {
                   throw new RuntimeException(e);
               }
           }
        };
    }

}
