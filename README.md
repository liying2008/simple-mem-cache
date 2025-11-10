# simple-mem-cache

[![maven-central](https://img.shields.io/maven-central/v/cc.duduhuo/simple-mem-cache.svg?style=flat)](https://mvnrepository.com/artifact/cc.duduhuo/simple-mem-cache)

> 一款轻量级、高性能、无依赖的内存缓存工具，支持 TTL（过期时间）与并发访问。  
> 适用于 Web 应用中存储热点数据，提供极简 API，开箱即用。

---

## ✨ 特性

- 🚀 **轻量无依赖** — 纯 Kotlin 编写，不依赖外部库；
- 🧵 **并发安全** — 基于 `ConcurrentHashMap`；
- ⏰ **支持 TTL（过期时间）** — 缓存项可自动过期；
- ♻️ **RU 淘汰机制** — 超出容量自动删除最久未使用项；
- 🧹 **自动/手动清理** — 可配置过期缓存清理周期，或手动清理；
- 🪶 **简单易用** — 三个核心方法：`put` / `get` / `getOrLoad`。

---

## 📦 引入方式

- 使用 Maven

```xml
<dependency>
    <groupId>cc.duduhuo</groupId>
    <artifactId>simple-mem-cache</artifactId>
    <version>1.0.0</version>
</dependency>
```

- 使用 Gradle (Groovy)

```groovy
implementation 'cc.duduhuo:simple-mem-cache:1.0.0'
```

- 使用 Gradle (Kotlin)

```kotlin
implementation("cc.duduhuo:simple-mem-cache:1.0.0")
```

---

## ⚙️ 初始化示例

```kotlin
import cc.duduhuo.simplememcache.SimpleCache
import cc.duduhuo.simplememcache.CacheListener

fun main() {
    val cache = SimpleCache<String, String>(
        maxSize = 1000,               // 最大缓存容量（0 表示不限制）
        defaultTtlMillis = 10_000,    // 默认缓存过期时间 10 秒
        autoClean = true,             // 是否自动清理过期缓存
        cleanIntervalMinutes = 1,     // 清理周期（分钟）
        listener = object : CacheListener<String, String> {
            override fun onRemove(key: String, value: String, reason: String) {
                println("Removed [$key]=$value because $reason")
            }
        }
    )

    cache.put("A", "Alpha")                 // 写入缓存
    println(cache.get("A"))                 // 读取缓存
    println(cache.getOrLoad("B") { "Bravo" }) // 不存在则加载
}
```

---

## 🧰 API 说明

### 🔹 创建缓存操作对象

```kotlin
val cache = SimpleCache<K, V>(
    maxSize = 1000,               // 缓存最大数量（0 表示不限制）
    defaultTtlMillis = 5000,      // 默认TTL毫秒（0 表示永不过期）
    listener = null,              // 可选缓存事件监听器
    autoClean = true,             // 是否自动启动清理任务
    cleanIntervalMinutes = 1      // 自动清理间隔（仅当 autoClean = true 时生效）
)
```

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

`simple-mem-cache` 默认采用 **最近使用 (RU)** 策略：

- 每次访问或写入都会将键移到队尾；
- 当超过最大容量时，优先移除队首（最久未使用的键）。

---

## 🧩 监听器示例

```kotlin
val cache = SimpleCache<String, Int>(
    maxSize = 100,
    listener = object : CacheListener<String, Int> {
        override fun onRemove(key: String, value: Int, reason: String) {
            println("Removed $key=$value because $reason")
        }
    }
)
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

## ⚖️ License

MIT License © 2025 Li Ying
