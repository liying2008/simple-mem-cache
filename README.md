# simple-mem-cache

[![maven-central](https://img.shields.io/maven-central/v/cc.duduhuo/simple-mem-cache.svg?style=flat)](https://mvnrepository.com/artifact/cc.duduhuo/simple-mem-cache)

> 一款轻量级、高性能的 **内存缓存工具库**。
> 支持 TTL（过期时间）、RU 淘汰策略、并发访问与统计信息。  
> 适用于 Web 应用、服务本地缓存、配置字典缓存等轻量场景，提供极简 API，开箱即用。

---

## ✨ 特性

- 🚀 **轻量无依赖** — 纯 Kotlin 实现，无第三方库；
- 🧵 **并发安全** — 基于 `ConcurrentHashMap`；
- ⏰ **TTL 支持** — 自动过期、可自定义过期时间；
- ♻️ **RU 淘汰机制** — 超出容量自动删除最久未使用项；
- 🧹 **自动/手动清理** — 可配置过期缓存清理周期，或手动清理；
- 📊 **统计信息** — 支持命中、未命中、淘汰次数统计；
- 🪶 **易用 API** — 三个核心方法：`put` / `get` / `getOrLoad`。

---

## 📦 引入方式

- 使用 Maven

```xml
<dependency>
    <groupId>cc.duduhuo</groupId>
    <artifactId>simple-mem-cache</artifactId>
    <version>1.2.0</version>
</dependency>
```

- 使用 Gradle (Groovy)

```groovy
implementation 'cc.duduhuo:simple-mem-cache:1.2.0'
```

- 使用 Gradle (Kotlin)

```kotlin
implementation("cc.duduhuo:simple-mem-cache:1.2.0")
```

---

## ⚙️ 初始化示例

- 使用 **Kotlin**

```kotlin
fun main() {
    val cache = SimpleCache.builder<String, String>()
        .maxSize(1000)              // 最大缓存容量（0 表示不限制）
        .defaultTtlMillis(10_000)   // 默认缓存过期时间 10 秒（0 表示永不过期）
        .autoClean(true)            // 是否自动清理过期缓存
        .cleanIntervalMinutes(1)    // 清理周期（分钟）（仅当 autoClean = true 时生效）
        .listener(object : CacheListener<String, String> {
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
```

- 使用 **Java**

```java
public static void main(String[] args) {
    SimpleCache<String, String> cache = SimpleCache.<String, String>builder()
        .maxSize(1000)              // 最大缓存容量（0 表示不限制）
        .defaultTtlMillis(10_000)   // 默认缓存过期时间 10 秒（0 表示永不过期）
        .autoClean(true)            // 是否自动清理过期缓存
        .cleanIntervalMinutes(1)    // 清理周期（分钟）（仅当 autoClean = true 时生效）
        .listener(new CacheListener<String, String>() {
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
```

---

## 🧰 API 说明

| 方法                            | 说明                  |
| ----------------------------- |---------------------|
| `put(key, value, ttlMillis)`  | 写入缓存（可指定TTL）        |
| `putAll(map, ttlMillis)`      | 批量写入缓存              |
| `get(key)`                    | 读取缓存                |
| `getAll(keys)`                | 批量读取缓存              |
| `getOrLoad(key, ttl, loader)` | 不存在则执行 `loader` 加载  |
| `remove(key, reason)`         | 删除缓存项               |
| `clear(reason)`               | 清空全部缓存              |
| `containsKey(key)`            | 判断是否存在且未过期          |
| `keys()`                      | 返回所有有效键             |
| `values()`                    | 返回所有有效值             |
| `entries()`                   | 返回所有有效条目            |
| `ttl(key)`                    | 查询指定键的剩余过期时间（ms）    |
| `size()`                      | 当前有效缓存数量            |
| `cleanup()`                   | 手动清理过期缓存            |
| `stats()`                     | 获取缓存统计信息（命中、未命中、淘汰） |
| `shutdownCleaner()`           | 关闭自动清理线程            |

### 🔹 写入缓存

```kotlin
cache.put("key1", "value1")
cache.put("key2", "value2", ttlMillis = 3000) // 单独设置过期时间
```

### 🔹 读取缓存

```kotlin
val value = cache.get("key1")
if (value != null) {
    println("命中缓存: $value")
} else {
    println("缓存未命中或已过期")
}
```

### 🔹 获取或加载（懒加载）

```kotlin
val user = cache.getOrLoad("user:1") { key ->
    // 模拟数据库加载逻辑
    queryUserFromDB(key)
}
```

> ✅ 如果缓存存在则直接返回；  
> ❌ 如果缓存不存在或过期，则执行 `loader` 逻辑，并将结果自动写入缓存。

### 🔹 删除缓存

```kotlin
cache.remove("key1")    // 删除指定缓存
cache.clear()           // 清空全部缓存
```

> `onRemove` 监听器会在条目被清除（手动/过期/淘汰）时触发。

### 🔹 手动清理过期缓存（防止占用内存空间）

```kotlin
cache.cleanup() // 主动清理过期缓存
```

### 🔹 关闭自动清理线程

> 当 `autoClean = true` 时，调用 `shutdownCleaner()` 以安全关闭清理线程。

```kotlin
cache.shutdownCleaner()
```

---

## 🔄 缓存淘汰策略（RU）

`SimpleCache` 内部维护一个 **访问顺序队列**：

- 每次访问(get)或写入(put)都会将键移到队尾；
- 当超过最大容量时，优先移除队首（最久未使用的键）。
- 淘汰时触发 `onRemove()` 回调，原因 `"evicted(RU)"`。

---

## 🧩 监听器示例

```kotlin
val cache = SimpleCache.builder<String, String>()
    .maxSize(100)    // 最大缓存容量（0 表示不限制）
    .listener(object : CacheListener<String, String> {
        override fun onRemove(key: String, value: String, reason: String) {
            println("Removed [$key]=$value because $reason")
        }
    })    // 缓存事件监听器
    .build()

```

监听事件触发原因包括：

- `"manual"` — 手动删除；
- `"expired"` — 缓存过期；
- `"evicted(RU)"` — 因容量限制被淘汰。

---

## 🧠 应用场景

- ✅ Web 服务热点数据缓存
- ✅ 频繁访问的配置或字典表
- ✅ 轻量级替代 Redis（在单节点部署场景下）
- ✅ 本地计算结果或数据加载缓存

---

## ⚡ Benchmark 性能测试

### 🔧 测试环境

| 配置项   | 值                                     |
|-------|---------------------------------------|
| CPU   | Intel(R) Core(TM) i7-11800H @ 2.30GHz |
| 内存    | 32.0 GB                               |
| JDK   | OpenJDK 8                             |
| 操作系统  | Windows 11 家庭中文版 25H2                 |
| 核心线程数 | 8 核心 16 线程                            |

### 🧪 测试代码

[SimpleCacheBenchmark.kt](core/src/test/kotlin/cc/duduhuo/simplememcache/SimpleCacheBenchmark.kt)

### 📊 测试结果（ops/ms）

```
==== SimpleCache Benchmark ====

[1] Single Thread Benchmark
PUT: 139059 ops/sec
GET: 4521817 ops/sec

[2] Concurrent Benchmark
Threads: 8, Total Ops: 1600000, Throughput: 706837 ops/sec

[3] TTL Expiration Benchmark
Inserted 10k entries with TTL=100ms
Expired entries: 10000 / 10000

[4] RU Eviction Benchmark
Put 100k entries into maxSize=10k cache
Cache size after eviction: 10000
Eviction throughput: 16974 ops/sec
```

---

## ⚖️ License

MIT License © 2025 Li Ying
