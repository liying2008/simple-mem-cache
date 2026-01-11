/*
 * Copyright 2025 Li Ying.
 * Licensed under the MIT License.
 */

package cc.duduhuo.simplememcache.demo.vanilla;

import cc.duduhuo.simplememcache.CacheListener;
import cc.duduhuo.simplememcache.SimpleCache;

public class DemoJava {
    public static void main(String[] args) throws InterruptedException {
        SimpleCache<String, String> cache = SimpleCache.<String, String>builder()
            .maxSize(100)
            .defaultTtlMillis(2000)
            .listener(new CacheListener<String, String>() {
                @Override
                public void onRemove(String key, String value, String reason) {
                    System.out.println("Removed [" + key + "] = " + value + " because " + reason);
                }
            })
            .build();

        // 普通存取
        cache.put("hello", "world");
        System.out.println(cache.get("hello")); // world

        // 从数据库加载
        String user = cache.getOrLoad("user:1", 3000, key -> {
            System.out.println(">>> Loading from DB for " + key);
            return "ZhangSan";
        });
        System.out.println(user); // ZhangSan
        System.out.println(cache.get("user:1")); // ZhangSan

        String userEmpty = cache.getOrLoad("user:empty", 3000, key -> "", v -> v != null && !v.isEmpty());
        System.out.println(userEmpty); // ""
        System.out.println(cache.get("user:empty")); // null

        // 测试过期
        Thread.sleep(2000);
        System.out.println(cache.get("hello")); // null (expired)
    }
}
