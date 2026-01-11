/*
 * Copyright 2025-present Li Ying.
 * Licensed under the MIT License.
 */

package cc.duduhuo.simplememcache.demo.vanilla.md;

import cc.duduhuo.simplememcache.CacheListener;
import cc.duduhuo.simplememcache.SimpleCache;

public class DemoJava {
    public static void main(String[] args) {
        SimpleCache<String, String> cache = SimpleCache.<String, String>builder()
            .maxSize(1000)              // 最大缓存容量（0 表示不限制）
            .defaultTtlMillis(10_000)   // 默认缓存过期时间 10 秒（0 表示永不过期）
            .autoClean(true)            // 是否自动清理过期缓存
            .cleanIntervalMinutes(1)    // 清理周期（分钟）（仅当 autoClean = true 时生效）
            .listener(new CacheListener<String, String>() {
                @Override
                public void onPut(String key, String value) {
                    System.out.println("Put [" + key + "] = " + value);
                }

                @Override
                public void onRemove(String key, String value, String reason) {
                    System.out.println("Removed [" + key + "] = " + value + " because " + reason);
                }
            })    // 缓存事件监听器
            .build();

        cache.put("A", "Alpha");               // 写入缓存
        System.out.println(cache.get("A"));    // 读取缓存
        System.out.println(cache.getOrLoad("B", 3000, key -> {
            System.out.println(">>> Loading from DB for " + key);
            return "Bravo";
        })); // 不存在则加载
    }
}
