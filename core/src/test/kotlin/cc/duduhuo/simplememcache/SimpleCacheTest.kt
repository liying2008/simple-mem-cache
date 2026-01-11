/*
 * Copyright 2025 Li Ying.
 * Licensed under the MIT License.
 */

package cc.duduhuo.simplememcache

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class SimpleCacheTest {
    @Test
    fun test() {
        val cache = SimpleCache.builder<String, String?>()
            .maxSize(10)
            .defaultTtlMillis(2)
            .listener(object : CacheListener<String, String?> {
                override fun onRemove(key: String, value: String?, reason: String) {
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
            })
            .build()

        // 普通存取
        cache.put("hello", "world")
        assertEquals("world", cache.get("hello"))

        // 模拟从数据库加载
        val user1 = cache.getOrLoad("user:1", ttlMillis = 3000) { key ->
            "ZhangSan"
        }
        assertEquals("ZhangSan", user1)
        assertEquals("ZhangSan", cache.get("user:1"))
        cache.remove("user:1")
        assertNull(cache.get("user:1"))

        val userEmpty = cache.getOrLoad("user:empty", ttlMillis = 3000, { key -> "" }, { v -> !v.isNullOrEmpty() })
        assertEquals("", userEmpty)
        assertEquals(null, cache.get("user:empty"))

        // 测试过期
        Thread.sleep(3000)
        assertNull(cache.get("hello"))

        cache.put("user:2", "LiSi")
        cache.clear()
        assertNull(cache.get("user:2"))

        println("----")
        cache.putAll(
            mapOf(
                "user:3" to "WangWu",
                "user:4" to "ZhaoLiu",
                "user:5" to "QianSan",
            )
        )
        assertEquals(3, cache.size())
        assertTrue(cache.containsKey("user:4"))
        assertTrue {
            val ttl = cache.ttl("user:4")
            ttl != null && ttl > 0
        }
        assertEquals(3, cache.keys().size)
        cache.keys().forEach { key ->
            println("$key = ${cache.get(key)}")
        }
        cache.get("hello") // missing
        cache.get("hello") // missing
        val stats = cache.stats()
        assertTrue {
            stats.size == 3
            stats.hits == 3L
            stats.misses == 2L
            stats.evictions == 0L
        }
    }
}
