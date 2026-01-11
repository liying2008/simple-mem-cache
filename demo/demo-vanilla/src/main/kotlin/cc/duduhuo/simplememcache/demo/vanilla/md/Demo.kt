/*
 * Copyright 2025-present Li Ying.
 * Licensed under the MIT License.
 */

package cc.duduhuo.simplememcache.demo.vanilla.md

import cc.duduhuo.simplememcache.SimpleCache
import cc.duduhuo.simplememcache.CacheListener

fun main() {
    val cache = SimpleCache.builder<String, String>()
        .maxSize(1000)              // 最大缓存容量（0 表示不限制）
        .defaultTtlMillis(10_000)       // 默认缓存过期时间 10 秒（0 表示永不过期）
        .autoClean(true)          // 是否自动清理过期缓存
        .cleanIntervalMinutes(1)    // 清理周期（分钟）（仅当 autoClean = true 时生效）
        .listener(object : CacheListener<String, String> {
            override fun onPut(key: String, value: String) {
                println("Put [$key]=$value")
            }

            override fun onRemove(key: String, value: String, reason: String) {
                println("Removed [$key]=$value because $reason")
            }
        })    // 缓存事件监听器
        .build()

    cache.put("A", "Alpha")    // 写入缓存
    println(cache.get("A"))    // 读取缓存
    println(cache.getOrLoad("B", 3000) { key ->
        println(">>> Loading from DB for $key")
        "Bravo"
    }) // 不存在则加载
}
