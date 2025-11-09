/*
 * Copyright 2025 Li Ying.
 * Licensed under the MIT License.
 */

package cc.duduhuo.simplememcache

import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentLinkedDeque
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicLong

/**
 * 高性能内存缓存（支持TTL + 并发安全 + 轻量、无外部依赖 + 使用简单）
 *
 * @param maxSize 缓存最大容量，默认 0 表示不限制
 * @param defaultTtlMillis 默认 TTL 毫秒，0 表示永不过期
 * @param listener 缓存事件监听器
 * @param autoClean 是否启用自动清理线程（默认 true）
 * @param cleanIntervalMinutes 自动清理间隔（仅在 autoClean=true 时生效，默认 1 分钟）
 */
class SimpleCache<K, V>(
    private val maxSize: Int = 0,
    private val defaultTtlMillis: Long = 0,
    private val listener: CacheListener<K, V>? = null,
    private val autoClean: Boolean = true,
    private val cleanIntervalMinutes: Long = 1
) {
    private data class CacheValue<V>(
        @Volatile var value: V,
        val expireAt: Long,
        val lastAccess: AtomicLong = AtomicLong(System.nanoTime())
    )

    private val cache = ConcurrentHashMap<K, CacheValue<V>>()
    private val accessOrder = ConcurrentLinkedDeque<K>() // 维护访问顺序
    private var cleaner = Executors.newSingleThreadScheduledExecutor { r ->
        Thread(r, "SimpleCache-Cleaner").apply { isDaemon = true }
    }

    init {
        if (autoClean) {
            cleaner.scheduleAtFixedRate(::cleanup, cleanIntervalMinutes, cleanIntervalMinutes, TimeUnit.MINUTES)
        }
    }

    /** 写入缓存 */
    fun put(key: K, value: V, ttlMillis: Long = defaultTtlMillis) {
        val expireAt = if (ttlMillis > 0) System.currentTimeMillis() + ttlMillis else 0L
        cache[key] = CacheValue(value, expireAt)
        touchKey(key)
        evictIfNeeded()
    }

    /** 读取缓存（更新访问时间） */
    fun get(key: K): V? {
        val entry = cache[key] ?: return null
        if (entry.isExpired()) {
            remove(key, "expired")
            return null
        }
        entry.lastAccess.set(System.nanoTime())
        touchKey(key)
        return entry.value
    }

    /** 获取或加载（缓存不存在时加载） */
    fun getOrLoad(
        key: K,
        ttlMillis: Long = defaultTtlMillis,
        loader: (K) -> V
    ): V {
        val existing = get(key)
        if (existing != null) return existing

        synchronized(key.toString().intern()) {
            val doubleCheck = get(key)
            if (doubleCheck != null) return doubleCheck
            val newValue = loader(key)
            put(key, newValue, ttlMillis)
            return newValue
        }
    }

    /** 删除缓存 */
    fun remove(key: K, reason: String = "manual") {
        val removed = cache.remove(key)
        if (removed != null) {
            accessOrder.remove(key)
            listener?.onRemove(key, removed.value, reason)
        }
    }

    /**
     * 主动清理过期缓存（可被外部调用）
     */
    fun cleanup() {
        val now = System.currentTimeMillis()
        val expiredKeys = mutableListOf<K>()

        cache.forEach { (k, v) ->
            if (v.expireAt > 0 && now > v.expireAt) {
                expiredKeys.add(k)
            }
        }

        expiredKeys.forEach { remove(it, "expired") }
    }

    /** 淘汰最久未使用的缓存（按访问顺序） */
    private fun evictIfNeeded() {
        if (maxSize <= 0) {
            // 未设置缓存最大容量
            return
        }
        while (cache.size > maxSize) {
            val oldestKey = accessOrder.pollFirst() ?: break
            if (cache.containsKey(oldestKey)) {
                remove(oldestKey, "evicted(RU)")
            }
        }
    }

    /** 访问或写入时更新访问顺序 */
    private fun touchKey(key: K) {
        accessOrder.remove(key)
        accessOrder.offerLast(key)
    }

    /** 判断是否过期 */
    private fun CacheValue<V>.isExpired(): Boolean =
        expireAt > 0 && System.currentTimeMillis() > expireAt

    /** 关闭清理线程（仅在 autoClean = true 时需要） */
    fun shutdown() {
        if (autoClean) {
            cleaner.shutdown()
        }
    }
}

/** 缓存事件监听器 */
interface CacheListener<K, V> {
    fun onRemove(key: K, value: V, reason: String)
}
