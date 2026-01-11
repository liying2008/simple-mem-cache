/*
 * Copyright 2025 Li Ying.
 * Licensed under the MIT License.
 */

package cc.duduhuo.simplememcache.demo.vanilla

import cc.duduhuo.simplememcache.CacheListener
import cc.duduhuo.simplememcache.SimpleCache


fun main() {
    val cache = SimpleCache.builder<String, String>()
        .maxSize(100)
        .defaultTtlMillis(2000)
        .listener(object : CacheListener<String, String> {
            override fun onRemove(key: String, value: String, reason: String) {
                println("Removed [$key] = $value because $reason")
            }
        })
        .build()

    // 普通存取
    cache.put("hello", "world")
    println(cache.get("hello")) // world

    // 从数据库加载
    val user = cache.getOrLoad("user:1", ttlMillis = 3000) { key ->
        println(">>> Loading from DB for $key")
        "ZhangSan"
    }
    println(user) // ZhangSan
    println(cache.get("user:1")) // ZhangSan

    val userEmpty = cache.getOrLoad("user:empty", ttlMillis = 3000, { key -> "" }, { v -> !v.isNullOrEmpty() })
    println(userEmpty) // ""
    println(cache.get("user:empty")) // null

    // 测试过期
    Thread.sleep(2000)
    println(cache.get("hello")) // null (expired)
}
