/*
 * Copyright 2025 Li Ying.
 * Licensed under the MIT License.
 */

package cc.duduhuo.simplememcache

import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import kotlin.system.measureNanoTime

/**
 * Benchmark for SimpleCache performance under different scenarios.
 */
object SimpleCacheBenchmark {

    @JvmStatic
    fun main(args: Array<String>) {
        println("==== SimpleCache Benchmark ====")

        // Create cache
        val cache = SimpleCache.builder<String, String>()
            .maxSize(1_000)
            .defaultTtlMillis(5_000)
            .autoClean(true)
            .cleanIntervalMinutes(1)
            .build()

        singleThreadBenchmark(cache)
        concurrentBenchmark(cache)
        ttlBenchmark()
        ruEvictionBenchmark()
    }

    /**
     * Test single-threaded put/get performance.
     */
    private fun singleThreadBenchmark(cache: SimpleCache<String, String>) {
        println("\n[1] Single Thread Benchmark")

        val count = 10_000
        val putTime = measureNanoTime {
            for (i in 0 until count) {
                cache.put("key$i", "val$i")
            }
        }

        val getTime = measureNanoTime {
            for (i in 0 until count) {
                cache.get("key$i")
            }
        }

        println("PUT: ${(count * 1e9 / putTime).toInt()} ops/sec")
        println("GET: ${(count * 1e9 / getTime).toInt()} ops/sec")
    }

    /**
     * Test concurrent access (multiple threads).
     */
    private fun concurrentBenchmark(cache: SimpleCache<String, String>) {
        println("\n[2] Concurrent Benchmark")

        val threads = 8
        val opsPerThread = 100_000
        val pool = Executors.newFixedThreadPool(threads)

        val startLatch = CountDownLatch(1)
        val doneLatch = CountDownLatch(threads)

        val totalTime = measureNanoTime {
            repeat(threads) { t ->
                pool.submit {
                    startLatch.await()
                    for (i in 0 until opsPerThread) {
                        val key = "key${(i + t * 1000) % 50000}"
                        cache.put(key, "val")
                        cache.get(key)
                    }
                    doneLatch.countDown()
                }
            }
            startLatch.countDown()
            doneLatch.await()
        }

        pool.shutdown()
        val totalOps = threads * opsPerThread * 2 // put + get
        val opsPerSec = totalOps * 1e9 / totalTime
        println("Threads: $threads, Total Ops: $totalOps, Throughput: ${opsPerSec.toInt()} ops/sec")
    }

    /**
     * Test TTL expiration performance.
     */
    private fun ttlBenchmark() {
        println("\n[3] TTL Expiration Benchmark")

        val cache = SimpleCache.builder<String, String>()
            .defaultTtlMillis(100)
            .autoClean(false)
            .build()

        for (i in 0 until 10_000) {
            cache.put("key$i", "val$i")
        }
        println("Inserted 10k entries with TTL=100ms")
        Thread.sleep(300)

        val expiredCount = (0 until 10_000).count { cache.get("key$it") == null }
        println("Expired entries: $expiredCount / 10000")
    }

    /**
     * Test eviction (maxSize overflow).
     */
    private fun ruEvictionBenchmark() {
        println("\n[4] RU Eviction Benchmark")

        val cache = SimpleCache.builder<Int, Int>()
            .maxSize(10_000)
            .defaultTtlMillis(0)
            .autoClean(false)
            .build()

        val putTime = measureNanoTime {
            for (i in 0 until 100_000) {
                cache.put(i, i)
            }
        }

        println("Put 100k entries into maxSize=10k cache")
        println("Cache size after eviction: ${cache.size()}")
        println("Eviction throughput: ${(100_000 * 1e9 / putTime).toInt()} ops/sec")
    }
}
