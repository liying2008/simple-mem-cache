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
    )

    private val cache = ConcurrentHashMap<K, CacheValue<V>>()
    private val accessOrder = ConcurrentLinkedDeque<K>() // 维护访问顺序
    private val cleaner = Executors.newSingleThreadScheduledExecutor { r ->
        Thread(r, "SimpleCache-Cleaner").apply { isDaemon = true }
    }

    // 统计信息
    private val hitCount = AtomicLong(0)
    private val missCount = AtomicLong(0)
    private val evictCount = AtomicLong(0)

    init {
        if (autoClean) {
            cleaner.scheduleAtFixedRate(::cleanup, cleanIntervalMinutes, cleanIntervalMinutes, TimeUnit.MINUTES)
        }
    }

    /** 写入缓存 */
    fun put(key: K, value: V, ttlMillis: Long) {
        val expireAt = if (ttlMillis > 0) System.currentTimeMillis() + ttlMillis else 0L
        cache[key] = CacheValue(value, expireAt)
        touchKey(key)
        evictIfNeeded()
    }

    /** 写入缓存 */
    fun put(key: K, value: V) {
        put(key, value, defaultTtlMillis)
    }

    /** 批量写入缓存 */
    fun putAll(entries: Map<K, V>, ttlMillis: Long) {
        entries.forEach { (k, v) -> put(k, v, ttlMillis) }
    }

    /** 批量写入缓存 */
    fun putAll(entries: Map<K, V>) {
        putAll(entries, defaultTtlMillis)
    }

    /** 读取缓存（更新访问顺序） */
    fun get(key: K): V? {
        val entry = cache[key] ?: run {
            missCount.incrementAndGet()
            return null
        }

        if (entry.isExpired()) {
            remove(key, "expired")
            missCount.incrementAndGet()
            return null
        }

        hitCount.incrementAndGet()
        touchKey(key)
        return entry.value
    }

    /** 批量读取缓存 */
    fun getAll(keys: Collection<K>): Map<K, V> {
        val result = mutableMapOf<K, V>()
        keys.forEach { key ->
            val value = get(key)
            if (value != null) result[key] = value
        }
        return result
    }

    /** 获取或加载（缓存不存在时加载） */
    fun getOrLoad(key: K, ttlMillis: Long, loader: java.util.function.Function<K, V>): V {
        val existing = get(key)
        if (existing != null) return existing

        synchronized(key.toString().intern()) {
            val doubleCheck = get(key)
            if (doubleCheck != null) return doubleCheck
            val newValue = loader.apply(key)
            put(key, newValue, ttlMillis)
            return newValue
        }
    }

    /** 获取或加载（缓存不存在时加载） */
    fun getOrLoad(key: K, loader: java.util.function.Function<K, V>): V {
        return getOrLoad(key, defaultTtlMillis, loader)
    }

    /** 删除缓存 */
    fun remove(key: K, reason: String) {
        val removed = cache.remove(key)
        if (removed != null) {
            accessOrder.remove(key)
            listener?.onRemove(key, removed.value, reason)
        }
    }

    /** 删除缓存 */
    fun remove(key: K) {
        remove(key, "manual")
    }

    /** 清空所有缓存 */
    fun clear(reason: String) {
        val entries = cache.entries.toList()
        cache.clear()
        accessOrder.clear()
        listener?.let { listener ->
            entries.forEach { (k, v) ->
                listener.onRemove(k, v.value, reason)
            }
        }
    }

    /** 清空所有缓存 */
    fun clear() {
        clear("manual")
    }

    /** 是否包含指定 key 且未过期 */
    fun containsKey(key: K): Boolean = get(key) != null

    /** 获取所有有效键 */
    fun keys(): Set<K> = cache.entries
        .filter { !it.value.isExpired() }
        .map { it.key }
        .toSet()

    /** 获取所有有效值 */
    fun values(): Set<V> = cache.entries
        .filter { !it.value.isExpired() }
        .map { it.value.value }
        .toSet()

    /** 获取所有有效条目 */
    fun entries(): Map<K, V> = cache.entries
        .filter { !it.value.isExpired() }
        .associate { it.key to it.value.value }

    /** 当前缓存条目数量 */
    fun size(): Int = keys().size

    /** 查询指定key的剩余TTL（毫秒），若无则返回null */
    fun ttl(key: K): Long? {
        val entry = cache[key] ?: return null
        if (entry.expireAt <= 0) return null
        val remaining = entry.expireAt - System.currentTimeMillis()
        return if (remaining > 0) remaining else null
    }

    /** 返回统计信息 */
    fun stats(): CacheStats = CacheStats(
        size = size(),
        hits = hitCount.get(),
        misses = missCount.get(),
        evictions = evictCount.get()
    )

    /** 主动清理过期缓存（可被外部调用） */
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
                evictCount.incrementAndGet()
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
    fun shutdownCleaner() {
        if (autoClean) {
            cleaner.shutdown()
        }
    }
}

/** 缓存事件监听器 */
interface CacheListener<K, V> {
    fun onRemove(key: K, value: V, reason: String)
}

/** 缓存统计信息 */
data class CacheStats(
    /** 当前有效缓存数量 */
    val size: Int,
    /** 命中次数 */
    val hits: Long,
    /** 未命中次数 */
    val misses: Long,
    /** 淘汰次数 */
    val evictions: Long
)
