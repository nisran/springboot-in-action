package org.esmile.basic.collections;

import java.time.LocalDate;
import java.util.Map;
import java.util.NavigableMap;
import java.util.Optional;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;

public class Maps {
    public static void main(String[] args) {
        treeMap();
    }

    /**
     * java1.7 之前的 ConcurrentHashMap：
     * segment 数组（默认16）使用reentrantLock 作为锁
     * 两次hash：第一次hash 确定segment，第二次hash 确定具体位置
     */

    /**
     * java 8
     * 底层：Node数组 + CAS + synchronized（锁力度到桶）
     * CAS：初始化，空桶插入（后面链表和红黑树插入适应synchronized），链表插入，resize，计数器等
     * CAS失败就采用 synchronized 锁
     *
     */
    void concurrentHashMap() {
        ConcurrentHashMap<Integer, String> map = new ConcurrentHashMap<>();
        map.put(1, "a");
        map.get(1);
    }

    /**
     * 底层：红黑树：一种自平衡二叉树，保持高度平衡
     * 平衡手段：变色 + 左右旋转
     *
     */
    static void treeMap() {
        NavigableMap<Long, RateConfig> versions = new TreeMap<>();  // key = 生效时间戳
        RateConfig current = Optional.ofNullable(versions.floorEntry(LocalDate.now().toEpochDay()))
                .map(Map.Entry::getValue)
                .orElse(null);   // 早于首版必须兜底，否则 NPE

        TreeMap<Integer, String> map = new TreeMap<>();

        map.put(1, "A");
        map.put(3, "C");
        map.put(5, "E");
        map.put(7, "G");
        map.put(9, "I");
        map.put(11, "K");

        System.out.println("原始 TreeMap: " + map);

        // 1. subMap：查询 [3, 9)，包含 3，不包含 9
        NavigableMap<Integer, String> sub1 = map.subMap(3, true, 9, false);
        System.out.println("subMap(3, true, 9, false): " + sub1);

        // 2. subMap：查询 (3, 9]，不包含 3，包含 9
        NavigableMap<Integer, String> sub2 = map.subMap(3, false, 9, true);
        System.out.println("subMap(3, false, 9, true): " + sub2);

        // 3. headMap：查询 <= 5 的键值
        NavigableMap<Integer, String> head = map.headMap(5, true);
        System.out.println("headMap(5, true): " + head);

        // 4. tailMap：查询 >= 7 的键值
        NavigableMap<Integer, String> tail = map.tailMap(7, true);
        System.out.println("tailMap(7, true): " + tail);

        // 5. 邻近键查询
        System.out.println("floorKey(6): " + map.floorKey(6));     // <= 6 的最大键，结果 5
        System.out.println("ceilingKey(6): " + map.ceilingKey(6)); // >= 6 的最小键，结果 7
        System.out.println("lowerKey(5): " + map.lowerKey(5));     // < 5 的最大键，结果 3
        System.out.println("higherKey(5): " + map.higherKey(5));   // > 5 的最小键，结果 7

        // 6. 范围视图是“视图”，修改会影响原 map
        sub1.put(8, "H");
        System.out.println("修改 sub1 后原 map: " + map);
    }


}

record RateConfig(Long timestamp, Double rate) {
}
