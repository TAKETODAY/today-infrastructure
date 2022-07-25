/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2021 All Rights Reserved.
 *
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see [http://www.gnu.org/licenses/]
 */

package cn.taketoday.util;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.Function;

import cn.taketoday.lang.Assert;

/**
 * Simple Least Recently Used cache, bounded by the maximum size given to the
 * class constructor.
 * <p>
 * This implementation is backed by a {@code ConcurrentHashMap} for storing the
 * cached values and a {@code ConcurrentLinkedQueue} for ordering the keys and
 * choosing the least recently used key when the cache is at full capacity.
 *
 * @param <K> the type of the key used for cache retrieval
 * @param <V> the type of the cached values
 * @author Brian Clozel
 * @author Juergen Hoeller
 * @author TODAY 2021/9/11 12:47
 * @see #get
 * @since 4.0
 */
public class ConcurrentLruCache<K, V> {

  private final int maxSize;
  private final Lock readLock;
  private final Lock writeLock;
  private volatile int size = 0;
  private final Function<K, V> generator;
  private final ConcurrentHashMap<K, V> cache = new ConcurrentHashMap<>();
  private final ConcurrentLinkedDeque<K> queue = new ConcurrentLinkedDeque<>();

  /**
   * Create a new cache instance with the given limit and generator function.
   *
   * @param maxSize the maximum number of entries in the cache
   * (0 indicates no caching, always generating a new value)
   * @param generator a function to generate a new value for a given key
   */
  public ConcurrentLruCache(int maxSize, Function<K, V> generator) {
    Assert.isTrue(maxSize >= 0, "LRU max size should be positive");
    Assert.notNull(generator, "Generator function should not be null");
    this.maxSize = maxSize;
    this.generator = generator;

    ReentrantReadWriteLock lock = new ReentrantReadWriteLock();
    this.readLock = lock.readLock();
    this.writeLock = lock.writeLock();
  }

  /**
   * Retrieve an entry from the cache, potentially triggering generation
   * of the value.
   *
   * @param key the key to retrieve the entry for
   * @return the cached or newly generated value
   */
  public V get(K key) {
    if (this.maxSize == 0) {
      return this.generator.apply(key);
    }

    V cached;

    if ((cached = this.cache.get(key)) != null) {
      if (this.size < this.maxSize) {
        return cached;
      }
      this.readLock.lock();
      try {
        if (this.queue.removeLastOccurrence(key)) {
          this.queue.offer(key);
        }
        return cached;
      }
      finally {
        this.readLock.unlock();
      }
    }

    this.writeLock.lock();
    try {
      // retrying in case of concurrent reads on the same key
      if ((cached = this.cache.get(key)) != null) {
        if (this.queue.removeLastOccurrence(key)) {
          this.queue.offer(key);
        }
        return cached;
      }

      // Generate value first, to prevent size inconsistency
      V value = this.generator.apply(key);

      if (this.size == this.maxSize) {
        K leastUsed = this.queue.poll();
        if (leastUsed != null) {
          this.cache.remove(leastUsed);
        }
      }

      this.queue.offer(key);
      this.cache.put(key, value);
      this.size = this.cache.size();

      return value;
    }
    finally {
      this.writeLock.unlock();
    }
  }

  /**
   * Determine whether the given key is present in this cache.
   *
   * @param key the key to check for
   * @return {@code true} if the key is present,
   * {@code false} if there was no matching key
   */
  public boolean contains(K key) {
    return this.cache.containsKey(key);
  }

  /**
   * Immediately remove the given key and any associated value.
   *
   * @param key the key to evict the entry for
   * @return {@code true} if the key was present before,
   * {@code false} if there was no matching key
   */
  public boolean remove(K key) {
    this.writeLock.lock();
    try {
      boolean wasPresent = (this.cache.remove(key) != null);
      this.queue.remove(key);
      this.size = this.cache.size();
      return wasPresent;
    }
    finally {
      this.writeLock.unlock();
    }
  }

  /**
   * Immediately remove all entries from this cache.
   */
  public void clear() {
    this.writeLock.lock();
    try {
      this.cache.clear();
      this.queue.clear();
      this.size = 0;
    }
    finally {
      this.writeLock.unlock();
    }
  }

  /**
   * Return the current size of the cache.
   *
   * @see #maxSize()
   */
  public int size() {
    return this.size;
  }

  /**
   * Return the maximum number of entries in the cache
   * (0 indicates no caching, always generating a new value).
   *
   * @see #size()
   */
  public int maxSize() {
    return this.maxSize;
  }
}
