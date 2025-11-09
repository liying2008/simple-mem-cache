/*
 * Copyright 2025 Li Ying.
 * Licensed under the MIT License.
 */

package cc.duduhuo.simplememcache.demo.vanilla

import cc.duduhuo.simplememcache.CacheListener
import cc.duduhuo.simplememcache.SimpleCache


fun main() {
    val cache = SimpleCache<String, String>(
        maxSize = 100,
        defaultTtlMillis = 5000,
        listener = object : CacheListener<String, String> {
            override fun onRemove(key: String, value: String, reason: String) {
                println("Removed [$key] = $value because $reason")
            }
        }
    )

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

    // 测试过期
    Thread.sleep(6000)
    println(cache.get("hello")) // null (expired)
}
