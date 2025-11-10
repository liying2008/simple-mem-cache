/*
 * Copyright 2025 Li Ying.
 * Licensed under the MIT License.
 */

package cc.duduhuo.simplememcache

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class SimpleCacheTest {
    @Test
    fun test() {
        val cache = SimpleCache(
            maxSize = 100,
            defaultTtlMillis = 5000,
            listener = object : CacheListener<String, String> {
                override fun onRemove(key: String, value: String, reason: String) {
                    println("Removed [$key] = $value because $reason")
                    if (key == "hello") {
                        assertEquals("world", value)
                        assertEquals("expired", reason)
                    } else if (key == "user:1") {
                        assertEquals("ZhangSan", value)
                        assertEquals("manual", reason)
                    } else if (key == "user:2") {
                        assertEquals("LiSi", value)
                        assertEquals("manual", reason)
                    }
                }
            }
        )

        // 普通存取
        cache.put("hello", "world")
        assertEquals("world", cache.get("hello"))

        // 模拟从数据库加载
        val user = cache.getOrLoad("user:1", ttlMillis = 3000) { key ->
            "ZhangSan"
        }
        assertEquals("ZhangSan", user)
        assertEquals("ZhangSan", cache.get("user:1"))
        cache.remove("user:1")
        assertNull(cache.get("user:1"))

        // 测试过期
        Thread.sleep(6000)
        assertNull(cache.get("hello"))

        cache.put("user:2", "LiSi")
        cache.clear()
        assertNull(cache.get("user:2"))
    }
}
