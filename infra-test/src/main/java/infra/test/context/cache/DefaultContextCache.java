/*
 * Copyright 2002-present the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

// Modifications Copyright 2017 - 2026 the TODAY authors.

package infra.test.context.cache;

import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import infra.context.ApplicationContext;
import infra.context.ConfigurableApplicationContext;
import infra.core.style.ToStringBuilder;
import infra.lang.Assert;
import infra.logging.Logger;
import infra.logging.LoggerFactory;
import infra.test.annotation.DirtiesContext.HierarchyMode;
import infra.test.context.MergedContextConfiguration;

/**
 * Default implementation of the {@link ContextCache} API.
 *
 * <p>Uses a synchronized {@link Map} configured with a maximum size
 * and a <em>least recently used</em> (LRU) eviction policy to cache
 * {@link ApplicationContext} instances.
 *
 * <p>The maximum size may be supplied as a {@linkplain #DefaultContextCache(int)
 * constructor argument} or set via a system property or Infra property named
 * {@code infra.test.context.cache.maxSize}.
 *
 * @author Sam Brannen
 * @author Juergen Hoeller
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see ContextCacheUtils#retrieveMaxCacheSize()
 * @since 4.0
 */
public class DefaultContextCache implements ContextCache {

  private static final Logger statsLogger = LoggerFactory.getLogger(CONTEXT_CACHE_LOGGING_CATEGORY);

  /**
   * Map of context keys to Infra {@code ApplicationContext} instances.
   */
  private final Map<MergedContextConfiguration, ApplicationContext> contextMap =
          Collections.synchronizedMap(new LinkedHashMap<>(32, 0.75f, true));

  /**
   * Map of parent keys to sets of children keys, representing a top-down <em>tree</em>
   * of context hierarchies. This information is used for determining which subtrees
   * need to be recursively removed and closed when removing a context that is a parent
   * of other contexts.
   */
  private final Map<MergedContextConfiguration, Set<MergedContextConfiguration>> hierarchyMap =
          new ConcurrentHashMap<>(32);

  /**
   * Map of context keys to active test classes (i.e., test classes that are actively
   * using the corresponding {@link ApplicationContext}).
   *
   * @since 5.0
   */
  private final Map<MergedContextConfiguration, Set<Class<?>>> contextUsageMap = new ConcurrentHashMap<>(32);

  /**
   * Set of keys for contexts that are currently unused and are therefore
   * candidates for pausing on context switch.
   *
   * @since 5.0
   */
  private final Set<MergedContextConfiguration> unusedContexts = new LinkedHashSet<>(4);

  /**
   * Map of context keys to context load failure counts.
   */
  private final Map<MergedContextConfiguration, Integer> failureCounts = new ConcurrentHashMap<>(32);

  private final AtomicInteger totalFailureCount = new AtomicInteger();

  private final int maxSize;

  private final PauseMode pauseMode;

  private final AtomicInteger hitCount = new AtomicInteger();

  private final AtomicInteger missCount = new AtomicInteger();

  /**
   * Create a new {@code DefaultContextCache} using the maximum cache size
   * obtained via {@link ContextCacheUtils#retrieveMaxCacheSize()} and the
   * {@link PauseMode} obtained via {@link ContextCacheUtils#retrievePauseMode()}.
   *
   * @see #DefaultContextCache(int)
   * @see #DefaultContextCache(int, PauseMode)
   * @see ContextCacheUtils#retrieveMaxCacheSize()
   * @see ContextCacheUtils#retrievePauseMode()
   */
  public DefaultContextCache() {
    this(ContextCacheUtils.retrieveMaxCacheSize());
  }

  /**
   * Create a new {@code DefaultContextCache} using the supplied maximum
   * cache size and the {@link PauseMode} obtained via
   * {@link ContextCacheUtils#retrievePauseMode()}.
   *
   * @param maxSize the maximum cache size
   * @throws IllegalArgumentException if the supplied {@code maxSize} value
   * is not positive
   * @see #DefaultContextCache()
   * @see #DefaultContextCache(int, PauseMode)
   * @see ContextCacheUtils#retrievePauseMode()
   */
  public DefaultContextCache(int maxSize) {
    this(maxSize, ContextCacheUtils.retrievePauseMode());
  }

  /**
   * Create a new {@code DefaultContextCache} using the supplied maximum
   * cache size and {@link PauseMode}.
   *
   * @param maxSize the maximum cache size
   * @param pauseMode the {@code PauseMode} to use
   * @throws IllegalArgumentException if the supplied {@code maxSize} value
   * is not positive or if the supplied {@code PauseMode} is {@code null}
   * @see #DefaultContextCache()
   */
  public DefaultContextCache(int maxSize, PauseMode pauseMode) {
    Assert.isTrue(maxSize > 0, "'maxSize' must be positive");
    Assert.notNull(pauseMode, "'pauseMode' is required");
    this.maxSize = maxSize;
    this.pauseMode = pauseMode;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean contains(MergedContextConfiguration key) {
    Assert.notNull(key, "Key is required");
    return this.contextMap.containsKey(key);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @Nullable ApplicationContext get(MergedContextConfiguration key) {
    Assert.notNull(key, "Key is required");
    ApplicationContext context = this.contextMap.get(key);
    if (context == null) {
      this.missCount.incrementAndGet();
    }
    else {
      this.hitCount.incrementAndGet();
      pauseOnContextSwitchIfNecessary(key);
      restartContextIfNecessary(context);
    }
    return context;
  }

  private void restartContextIfNecessary(ApplicationContext context) {
    if (this.pauseMode != PauseMode.NEVER) {
      // Recurse up the context hierarchy first.
      ApplicationContext parent = context.getParent();
      if (parent != null) {
        restartContextIfNecessary(parent);
      }
      if (context instanceof ConfigurableApplicationContext cac && !cac.isRunning()) {
        cac.restart();
      }
    }
  }

  @Override
  public void put(MergedContextConfiguration key, ApplicationContext context) {
    Assert.notNull(key, "Key is required");
    Assert.notNull(context, "ApplicationContext is required");

    evictLruContextIfNecessary();
    pauseOnContextSwitchIfNecessary(key);
    putInternal(key, context);
  }

  @Override
  public ApplicationContext put(MergedContextConfiguration key, LoadFunction loadFunction) {
    Assert.notNull(key, "Key is required");
    Assert.notNull(loadFunction, "LoadFunction is required");

    evictLruContextIfNecessary();
    pauseOnContextSwitchIfNecessary(key);
    ApplicationContext context = loadFunction.loadContext(key);
    Assert.state(context != null, "LoadFunction must return a non-null ApplicationContext");
    putInternal(key, context);
    return context;
  }

  /**
   * Evict the least recently used (LRU) context if necessary.
   *
   * @since 5.0
   */
  private void evictLruContextIfNecessary() {
    if (this.contextMap.size() >= this.maxSize) {
      Iterator<MergedContextConfiguration> iterator = this.contextMap.keySet().iterator();
      Assert.state(iterator.hasNext(), "Failed to retrieve LRU context");
      // The least recently used (LRU) key is the first/head in a LinkedHashMap
      // configured for access-order iteration order.
      MergedContextConfiguration lruKey = iterator.next();
      remove(lruKey, HierarchyMode.CURRENT_LEVEL);
    }
  }

  private void putInternal(MergedContextConfiguration key, ApplicationContext context) {
    this.contextMap.put(key, context);

    // Update context hierarchy map.
    MergedContextConfiguration child = key;
    MergedContextConfiguration parent = child.getParent();
    while (parent != null) {
      Set<MergedContextConfiguration> set = this.hierarchyMap.computeIfAbsent(parent, k -> new HashSet<>());
      set.add(child);
      child = parent;
      parent = child.getParent();
    }
  }

  @Override
  public void registerContextUsage(MergedContextConfiguration mergedConfig, Class<?> testClass) {
    // Recurse up the context hierarchy first.
    MergedContextConfiguration parent = mergedConfig.getParent();
    if (parent != null) {
      registerContextUsage(parent, testClass);
    }
    getActiveTestClasses(mergedConfig).add(testClass);
  }

  @Override
  public void unregisterContextUsage(MergedContextConfiguration mergedConfig, Class<?> testClass) {
    ApplicationContext context = this.contextMap.get(mergedConfig);
    Assert.state(context != null, "ApplicationContext must not be null for: " + mergedConfig);

    Set<Class<?>> activeTestClasses = getActiveTestClasses(mergedConfig);
    activeTestClasses.remove(testClass);
    if (activeTestClasses.isEmpty()) {
      switch (this.pauseMode) {
        case ALWAYS -> pauseIfNecessary(context);
        case ON_CONTEXT_SWITCH -> this.unusedContexts.add(mergedConfig);
      }
      this.contextUsageMap.remove(mergedConfig);
    }

    // Recurse up the context hierarchy last.
    MergedContextConfiguration parent = mergedConfig.getParent();
    if (parent != null) {
      unregisterContextUsage(parent, testClass);
    }
  }

  private Set<Class<?>> getActiveTestClasses(MergedContextConfiguration mergedConfig) {
    return this.contextUsageMap.computeIfAbsent(mergedConfig, key -> new HashSet<>());
  }

  private boolean pauseOnContextSwitch() {
    return (this.pauseMode == PauseMode.ON_CONTEXT_SWITCH);
  }

  private void pauseOnContextSwitchIfNecessary(MergedContextConfiguration activeContextKey) {
    if (pauseOnContextSwitch()) {
      removeFromUnusedContexts(activeContextKey);
      for (MergedContextConfiguration unusedContextKey : this.unusedContexts) {
        pauseIfNecessary(this.contextMap.get(unusedContextKey));
      }
      this.unusedContexts.clear();
    }
  }

  /**
   * Remove the supplied key and any keys for parent contexts from the unused
   * contexts set. This effectively stops tracking the context (or context
   * hierarchy) as unused.
   */
  private void removeFromUnusedContexts(MergedContextConfiguration key) {
    do {
      this.unusedContexts.remove(key);
      key = key.getParent();
    }
    while (key != null);
  }

  private static void pauseIfNecessary(@Nullable ApplicationContext context) {
    if (context instanceof ConfigurableApplicationContext cac && cac.isRunning()) {
      cac.pause();
    }
  }

  @Override
  public void remove(MergedContextConfiguration key, @Nullable HierarchyMode hierarchyMode) {
    Assert.notNull(key, "Key is required");

    // startKey is the level at which to begin clearing the cache,
    // depending on the configured hierarchy mode.
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
    this.hierarchyMap.entrySet().removeIf(entry -> entry.getValue().isEmpty());
  }

  private void remove(List<MergedContextConfiguration> removedContexts, MergedContextConfiguration key) {
    Assert.notNull(key, "Key is required");

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
    this.contextUsageMap.remove(key);
    if (pauseOnContextSwitch()) {
      this.unusedContexts.remove(key);
    }
    if (context instanceof ConfigurableApplicationContext cac) {
      cac.close();
    }
    removedContexts.add(key);
  }

  @Override
  public int getFailureCount(MergedContextConfiguration key) {
    return this.failureCounts.getOrDefault(key, 0);
  }

  @Override
  public void incrementFailureCount(MergedContextConfiguration key) {
    this.totalFailureCount.incrementAndGet();
    this.failureCounts.merge(key, 1, Integer::sum);
  }

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

  @Override
  public int getContextUsageCount() {
    return this.contextUsageMap.size();
  }

  @Override
  public int getParentContextCount() {
    return this.hierarchyMap.size();
  }

  @Override
  public int getHitCount() {
    return this.hitCount.get();
  }

  @Override
  public int getMissCount() {
    return this.missCount.get();
  }

  @Override
  public void reset() {
    synchronized(this.contextMap) {
      clear();
      clearStatistics();
      this.totalFailureCount.set(0);
      this.failureCounts.clear();
    }
  }

  @Override
  public void clear() {
    synchronized(this.contextMap) {
      this.contextMap.clear();
      this.hierarchyMap.clear();
      this.contextUsageMap.clear();
      this.unusedContexts.clear();
    }
  }

  @Override
  public void clearStatistics() {
    synchronized(this.contextMap) {
      this.hitCount.set(0);
      this.missCount.set(0);
    }
  }

  @Override
  public void logStatistics() {
    if (statsLogger.isDebugEnabled()) {
      statsLogger.debug("Infra test ApplicationContext cache statistics: " + this);
    }
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
            .append("contextUsageCount", getContextUsageCount())
            .append("parentContextCount", getParentContextCount())
            .append("hitCount", getHitCount())
            .append("missCount", getMissCount())
            .append("failureCount", this.totalFailureCount)
            .toString();
  }

}
