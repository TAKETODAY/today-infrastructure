/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2023 All Rights Reserved.
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

package cn.taketoday.test.context.cache;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import cn.taketoday.context.ApplicationContext;
import cn.taketoday.context.ConfigurableApplicationContext;
import cn.taketoday.core.style.ToStringBuilder;
import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Nullable;
import cn.taketoday.logging.Logger;
import cn.taketoday.logging.LoggerFactory;
import cn.taketoday.test.annotation.DirtiesContext.HierarchyMode;
import cn.taketoday.test.context.MergedContextConfiguration;

/**
 * Default implementation of the {@link ContextCache} API.
 *
 * <p>Uses a synchronized {@link Map} configured with a maximum size
 * and a <em>least recently used</em> (LRU) eviction policy to cache
 * {@link ApplicationContext} instances.
 *
 * <p>The maximum size may be supplied as a {@linkplain #DefaultContextCache(int)
 * constructor argument} or set via a system property or Infra property named
 * {@code context.test.context.cache.maxSize}.
 *
 * @author Sam Brannen
 * @author Juergen Hoeller
 * @see ContextCacheUtils#retrieveMaxCacheSize()
 * @since 4.0
 */
public class DefaultContextCache implements ContextCache {

  private static final Logger statsLogger = LoggerFactory.getLogger(CONTEXT_CACHE_LOGGING_CATEGORY);

  /**
   * Map of context keys to Infra {@code ApplicationContext} instances.
   */
  private final Map<MergedContextConfiguration, ApplicationContext> contextMap =
          Collections.synchronizedMap(new LruCache(32, 0.75f));

  /**
   * Map of parent keys to sets of children keys, representing a top-down <em>tree</em>
   * of context hierarchies. This information is used for determining which subtrees
   * need to be recursively removed and closed when removing a context that is a parent
   * of other contexts.
   */
  private final Map<MergedContextConfiguration, Set<MergedContextConfiguration>> hierarchyMap =
          new ConcurrentHashMap<>(32);

  private final int maxSize;

  private final AtomicInteger hitCount = new AtomicInteger();

  private final AtomicInteger missCount = new AtomicInteger();

  /**
   * Create a new {@code DefaultContextCache} using the maximum cache size
   * obtained via {@link ContextCacheUtils#retrieveMaxCacheSize()}.
   *
   * @see #DefaultContextCache(int)
   * @see ContextCacheUtils#retrieveMaxCacheSize()
   */
  public DefaultContextCache() {
    this(ContextCacheUtils.retrieveMaxCacheSize());
  }

  /**
   * Create a new {@code DefaultContextCache} using the supplied maximum
   * cache size.
   *
   * @param maxSize the maximum cache size
   * @throws IllegalArgumentException if the supplied {@code maxSize} value
   * is not positive
   * @see #DefaultContextCache()
   */
  public DefaultContextCache(int maxSize) {
    Assert.isTrue(maxSize > 0, "'maxSize' must be positive");
    this.maxSize = maxSize;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean contains(MergedContextConfiguration key) {
    Assert.notNull(key, "Key must not be null");
    return this.contextMap.containsKey(key);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  @Nullable
  public ApplicationContext get(MergedContextConfiguration key) {
    Assert.notNull(key, "Key must not be null");
    ApplicationContext context = this.contextMap.get(key);
    if (context == null) {
      this.missCount.incrementAndGet();
    }
    else {
      this.hitCount.incrementAndGet();
    }
    return context;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void put(MergedContextConfiguration key, ApplicationContext context) {
    Assert.notNull(key, "Key must not be null");
    Assert.notNull(context, "ApplicationContext must not be null");

    this.contextMap.put(key, context);
    MergedContextConfiguration child = key;
    MergedContextConfiguration parent = child.getParent();
    while (parent != null) {
      Set<MergedContextConfiguration> list = this.hierarchyMap.computeIfAbsent(parent, k -> new HashSet<>());
      list.add(child);
      child = parent;
      parent = child.getParent();
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void remove(MergedContextConfiguration key, @Nullable HierarchyMode hierarchyMode) {
    Assert.notNull(key, "Key must not be null");

    // startKey is the level at which to begin clearing the cache,
    // depending on the configured hierarchy mode.s
    MergedContextConfiguration startKey = key;
    if (hierarchyMode == HierarchyMode.EXHAUSTIVE) {
      MergedContextConfiguration parent = startKey.getParent();
      while (parent != null) {
        startKey = parent;
        parent = startKey.getParent();
      }
    }

    List<MergedContextConfiguration> removedContexts = new ArrayList<>();
    remove(removedContexts, startKey);

    // Remove all remaining references to any removed contexts from the
    // hierarchy map.
    for (MergedContextConfiguration currentKey : removedContexts) {
      for (Set<MergedContextConfiguration> children : this.hierarchyMap.values()) {
        children.remove(currentKey);
      }
    }

    // Remove empty entries from the hierarchy map.
    for (Map.Entry<MergedContextConfiguration, Set<MergedContextConfiguration>> entry : this.hierarchyMap.entrySet()) {
      if (entry.getValue().isEmpty()) {
        this.hierarchyMap.remove(entry.getKey());
      }
    }
  }

  private void remove(List<MergedContextConfiguration> removedContexts, MergedContextConfiguration key) {
    Assert.notNull(key, "Key must not be null");

    Set<MergedContextConfiguration> children = this.hierarchyMap.get(key);
    if (children != null) {
      for (MergedContextConfiguration child : children) {
        // Recurse through lower levels
        remove(removedContexts, child);
      }
      // Remove the set of children for the current context from the hierarchy map.
      this.hierarchyMap.remove(key);
    }

    // Physically remove and close leaf nodes first (i.e., on the way back up the
    // stack as opposed to prior to the recursive call).
    ApplicationContext context = this.contextMap.remove(key);
    if (context instanceof ConfigurableApplicationContext) {
      context.close();
    }
    removedContexts.add(key);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public int size() {
    return this.contextMap.size();
  }

  /**
   * Get the maximum size of this cache.
   */
  public int getMaxSize() {
    return this.maxSize;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public int getParentContextCount() {
    return this.hierarchyMap.size();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public int getHitCount() {
    return this.hitCount.get();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public int getMissCount() {
    return this.missCount.get();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void reset() {
    synchronized(this.contextMap) {
      clear();
      clearStatistics();
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void clear() {
    synchronized(this.contextMap) {
      this.contextMap.clear();
      this.hierarchyMap.clear();
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void clearStatistics() {
    synchronized(this.contextMap) {
      this.hitCount.set(0);
      this.missCount.set(0);
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void logStatistics() {
    statsLogger.debug("Test ApplicationContext cache statistics: {}", this);
  }

  /**
   * Generate a text string containing the implementation type of this
   * cache and its statistics.
   * <p>The string returned by this method contains all information
   * required for compliance with the contract for {@link #logStatistics()}.
   *
   * @return a string representation of this cache, including statistics
   */
  @Override
  public String toString() {
    return new ToStringBuilder(this)
            .append("size", size())
            .append("maxSize", getMaxSize())
            .append("parentContextCount", getParentContextCount())
            .append("hitCount", getHitCount())
            .append("missCount", getMissCount())
            .toString();
  }

  /**
   * Simple cache implementation based on {@link LinkedHashMap} with a maximum
   * size and a <em>least recently used</em> (LRU) eviction policy that
   * properly closes application contexts.
   */
  @SuppressWarnings("serial")
  private class LruCache extends LinkedHashMap<MergedContextConfiguration, ApplicationContext> {

    /**
     * Create a new {@code LruCache} with the supplied initial capacity
     * and load factor.
     *
     * @param initialCapacity the initial capacity
     * @param loadFactor the load factor
     */
    LruCache(int initialCapacity, float loadFactor) {
      super(initialCapacity, loadFactor, true);
    }

    @Override
    protected boolean removeEldestEntry(Map.Entry<MergedContextConfiguration, ApplicationContext> eldest) {
      if (this.size() > DefaultContextCache.this.getMaxSize()) {
        // Do NOT delete "DefaultContextCache.this."; otherwise, we accidentally
        // invoke java.util.Map.remove(Object, Object).
        DefaultContextCache.this.remove(eldest.getKey(), HierarchyMode.CURRENT_LEVEL);
      }

      // Return false since we invoke a custom eviction algorithm.
      return false;
    }
  }

}
