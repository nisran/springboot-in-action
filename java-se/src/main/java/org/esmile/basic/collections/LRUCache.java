package org.esmile.basic.collections;

import java.util.*;

/**
 * 实现一个缓存类 LRUCache，支持：
 * capacity：最大容量，正整数
 * ttl：每个 key 的存活时间，可用毫秒/秒表示
 * 支持 get(key) 和 put(key, value, ttl?)
 * 当缓存数量超过 capacity 时，淘汰最久未使用的 key
 * 当 key 超过 TTL 时，视为过期，不能再被 get 到，并应被删除
 */
public class LRUCache<K, V> {
    record Key<K>(K k, Long expireAt) {
    }

    record Val<V>(V v, Long expireAt) {
    }

    private final LinkedHashMap<K, Val<V>> map;
    private final PriorityQueue<Key<K>> queue;
    private final int capacity;

    public LRUCache(int capacity) {
        if (capacity <= 0) {
            throw new IllegalArgumentException();
        }
        map = new LinkedHashMap<>(capacity, 0.75f, true);
        // 从小到大排序
        queue = new PriorityQueue<>(capacity, Comparator.comparingLong(a -> a.expireAt));
        this.capacity = capacity;
    }

    public V get(K key) {
        removeExpired();
        Val<V> val = map.get(key);
        return val == null ? null : val.v;
    }

    public V put(K key, V value, Long ttl) {
        removeExpired();
        Long expireAt = System.currentTimeMillis() + ttl;


        if (map.containsKey(key)) {
            Val<V> oldVal = map.put(key, new Val<>(value, expireAt));
            // update pq
            queue.remove(new Key<>(key, oldVal.expireAt));
            queue.add(new Key<>(key, expireAt));
            return oldVal.v();
        }
        // check capacity
        if (map.size() >= capacity) {
            // remove the least recently used key
            Map.Entry<K, Val<V>> lastEntry = map.firstEntry();
            map.remove(lastEntry.getKey());
            queue.remove(new Key<>(lastEntry.getKey(), lastEntry.getValue().expireAt));
        }

        // normal case
        map.put(key, new Val<>(value, expireAt));
        queue.add(new Key<>(key, expireAt));
        return null;
    }

    public int size() {
        return map.size();
    }

    // remove expired key from queue and map
    private void removeExpired() {
        while (!queue.isEmpty() && queue.peek().expireAt < System.currentTimeMillis()) {
            map.remove(Objects.requireNonNull(queue.poll()).k);
        }
    }

    @Override
    public String toString() {
        return map.toString();
    }
}


