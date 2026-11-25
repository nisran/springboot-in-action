package org.esmile.basic.collections;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class LRUCacheTest {

    /** 足够长的 ttl(60 秒),测试期间不会过期 */
    private static final long LONG_TTL = 60_000L;

    @Test
    void capacityMustBePositive() {
        assertThrows(IllegalArgumentException.class, () -> new LRUCache<>(0));
        assertThrows(IllegalArgumentException.class, () -> new LRUCache<>(-1));
        // 新建的缓存是空的:capacity 是"最多装几个",不是"已经装了几个"
        assertEquals(0, new LRUCache<>(3).size());
    }

    @Test
    void putThenGet() {
        LRUCache<String, Integer> cache = new LRUCache<>(3);
        assertNull(cache.put("a", 1, LONG_TTL)); // 首次 put 没有旧值
        assertEquals(1, cache.get("a"));
        assertNull(cache.get("missing"));
        assertEquals(1, cache.size());
    }

    @Test
    void putSameKeyUpdatesValue() {
        LRUCache<Integer, String> cache = new LRUCache<>(3);
        cache.put(1, "v1", LONG_TTL);
        assertEquals("v1", cache.put(1, "v2", LONG_TTL)); // 返回旧值,和 Map.put 一样
        assertEquals("v2", cache.get(1));
        assertEquals(1, cache.size()); // 更新不该增加条目数
    }

    @Test
    void putRefreshesTtl() throws InterruptedException {
        LRUCache<Integer, Integer> cache = new LRUCache<>(3);
        cache.put(1, 1, 100L);          // 先给一个短 ttl
        Thread.sleep(60);
        cache.put(1, 1, LONG_TTL);      // 再续命
        Thread.sleep(60);               // 原本的 100ms 已经过去
        assertEquals(1, cache.get(1));  // 但续过命,还活着
    }

    @Test
    void evictsLeastRecentlyUsed() {
        LRUCache<Integer, Integer> cache = new LRUCache<>(3);
        cache.put(1, 1, LONG_TTL);
        cache.put(2, 2, LONG_TTL);
        cache.put(3, 3, LONG_TTL);

        cache.get(1);                  // 访问 1:现在最久未使用的是 2

        cache.put(4, 4, LONG_TTL);     // 满了,应该淘汰 2
        assertNull(cache.get(2));
        assertEquals(1, cache.get(1));
        assertEquals(3, cache.get(3));
        assertEquals(4, cache.get(4));
        assertEquals(3, cache.size());
    }

    @Test
    void expiredKeyIsInvisibleAndRemoved() throws InterruptedException {
        LRUCache<Integer, Integer> cache = new LRUCache<>(3);
        cache.put(1, 1, 100L);
        assertEquals(1, cache.get(1));
        Thread.sleep(200);             // 等它过期
        assertNull(cache.get(1));      // get 不到
        assertEquals(0, cache.size()); // 并且被删除
    }

    @Test
    void expiredKeyFreesCapacity() throws InterruptedException {
        LRUCache<Integer, Integer> cache = new LRUCache<>(2);
        cache.put(1, 1, 100L);
        Thread.sleep(200);             // 1 已过期
        cache.put(2, 2, LONG_TTL);     // 清掉过期的 1,不该触发淘汰
        cache.put(3, 3, LONG_TTL);
        assertEquals(2, cache.get(2));
        assertEquals(3, cache.get(3));
        assertEquals(2, cache.size());
    }

    @Test
    void eachKeyHasItsOwnTtl() throws InterruptedException {
        LRUCache<Integer, Integer> cache = new LRUCache<>(3);
        cache.put(1, 1, 100L);         // 短命
        cache.put(2, 2, LONG_TTL);     // 长命
        Thread.sleep(200);
        assertNull(cache.get(1));
        assertEquals(2, cache.get(2));
        assertEquals(1, cache.size());
    }
}
