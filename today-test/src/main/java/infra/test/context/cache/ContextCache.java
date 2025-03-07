/*
 * Copyright 2017 - 2024 the original author or authors.
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
 * along with this program. If not, see [https://www.gnu.org/licenses/]
 */

package infra.test.context.cache;

import infra.context.ApplicationContext;
import infra.lang.Nullable;
import infra.lang.TodayStrategies;
import infra.test.annotation.DirtiesContext.HierarchyMode;
import infra.test.context.MergedContextConfiguration;

/**
 * {@code ContextCache} defines the SPI for caching Infra
 * {@link ApplicationContext ApplicationContexts} within the
 * <em>TestContext Framework</em>.
 *
 * <p>A {@code ContextCache} maintains a cache of {@code ApplicationContexts}
 * keyed by {@link MergedContextConfiguration} instances, potentially configured
 * with a {@linkplain ContextCacheUtils#retrieveMaxCacheSize maximum size} and
 * a custom eviction policy.
 *
 * <p>This SPI includes optional support for
 * {@linkplain #getFailureCount(MergedContextConfiguration) tracking} and
 * {@linkplain #incrementFailureCount(MergedContextConfiguration) incrementing}
 * failure counts.
 *
 * <h3>Rationale</h3>
 * <p>Context caching can have significant performance benefits if context
 * initialization is complex. Although the initialization of a Infra context
 * itself is typically very quick, some beans in a context &mdash; for example,
 * an embedded database or a {@code LocalContainerEntityManagerFactoryBean} for
 * working with JPA &mdash; may take several seconds to initialize. Hence it
 * often makes sense to perform that initialization only once per test suite or
 * JVM process.
 *
 * @author Sam Brannen
 * @author Juergen Hoeller
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see ContextCacheUtils#retrieveMaxCacheSize()
 * @since 4.0
 */
public interface ContextCache {

  /**
   * The name of the logging category used for reporting {@code ContextCache}
   * statistics.
   */
  String CONTEXT_CACHE_LOGGING_CATEGORY = "infra.test.context.cache";

  /**
   * The default maximum size of the context cache: {@value}.
   *
   * @see #MAX_CONTEXT_CACHE_SIZE_PROPERTY_NAME
   */
  int DEFAULT_MAX_CONTEXT_CACHE_SIZE = 32;

  /**
   * System property used to configure the maximum size of the {@link ContextCache}
   * as a positive integer: {@value}.
   * <p>May alternatively be configured via the
   * {@link TodayStrategies} mechanism.
   * <p>Note that implementations of {@code ContextCache} are not required to
   * actually support a maximum cache size. Consult the documentation of the
   * corresponding implementation for details.
   *
   * @see #DEFAULT_MAX_CONTEXT_CACHE_SIZE
   */
  String MAX_CONTEXT_CACHE_SIZE_PROPERTY_NAME = "infra.test.context.cache.maxSize";

  /**
   * Determine whether there is a cached context for the given key.
   *
   * @param key the context key (never {@code null})
   * @return {@code true} if the cache contains a context with the given key
   */
  boolean contains(MergedContextConfiguration key);

  /**
   * Obtain a cached {@code ApplicationContext} for the given key.
   * <p>The {@linkplain #getHitCount() hit} and {@linkplain #getMissCount() miss}
   * counts must be updated accordingly.
   *
   * @param key the context key (never {@code null})
   * @return the corresponding {@code ApplicationContext} instance, or {@code null}
   * if not found in the cache
   * @see #remove
   */
  @Nullable
  ApplicationContext get(MergedContextConfiguration key);

  /**
   * Explicitly add an {@code ApplicationContext} instance to the cache
   * under the given key, potentially honoring a custom eviction policy.
   *
   * @param key the context key (never {@code null})
   * @param context the {@code ApplicationContext} instance (never {@code null})
   */
  void put(MergedContextConfiguration key, ApplicationContext context);

  /**
   * Remove the context with the given key from the cache and explicitly
   * {@linkplain infra.context.ConfigurableApplicationContext#close() close}
   * it if it is an instance of {@code ConfigurableApplicationContext}.
   * <p>Generally speaking, this method should be called to properly evict
   * a context from the cache (e.g., due to a custom eviction policy) or if
   * the state of a singleton bean has been modified, potentially affecting
   * future interaction with the context.
   * <p>In addition, the semantics of the supplied {@code HierarchyMode} must
   * be honored. See the Javadoc for {@link HierarchyMode} for details.
   *
   * @param key the context key; never {@code null}
   * @param hierarchyMode the hierarchy mode; may be {@code null} if the context
   * is not part of a hierarchy
   */
  void remove(MergedContextConfiguration key, @Nullable HierarchyMode hierarchyMode);

  /**
   * Get the failure count for the given key.
   * <p>A <em>failure</em> is any attempt to load the {@link ApplicationContext}
   * for the given key that results in an exception.
   * <p>The default implementation of this method always returns {@code 0}.
   * Concrete implementations are therefore highly encouraged to override this
   * method and {@link #incrementFailureCount(MergedContextConfiguration)} with
   * appropriate behavior. Note that the standard {@code ContextContext}
   * implementation in Infra overrides these methods appropriately.
   *
   * @param key the context key; never {@code null}
   * @see #incrementFailureCount(MergedContextConfiguration)
   */
  default int getFailureCount(MergedContextConfiguration key) {
    return 0;
  }

  /**
   * Increment the failure count for the given key.
   * <p>The default implementation of this method does nothing. Concrete
   * implementations are therefore highly encouraged to override this
   * method and {@link #getFailureCount(MergedContextConfiguration)} with
   * appropriate behavior. Note that the standard {@code ContextContext}
   * implementation in Infra overrides these methods appropriately.
   *
   * @param key the context key; never {@code null}
   * @see #getFailureCount(MergedContextConfiguration)
   */
  default void incrementFailureCount(MergedContextConfiguration key) {

  }

  /**
   * Determine the number of contexts currently stored in the cache.
   * <p>If the cache contains more than {@code Integer.MAX_VALUE} elements,
   * this method must return {@code Integer.MAX_VALUE}.
   */
  int size();

  /**
   * Determine the number of parent contexts currently tracked within the cache.
   */
  int getParentContextCount();

  /**
   * Get the overall hit count for this cache.
   * <p>A <em>hit</em> is any access to the cache that returns a non-null
   * context for the queried key.
   */
  int getHitCount();

  /**
   * Get the overall miss count for this cache.
   * <p>A <em>miss</em> is any access to the cache that returns a {@code null}
   * context for the queried key.
   */
  int getMissCount();

  /**
   * Reset all state maintained by this cache including statistics.
   *
   * @see #clear()
   * @see #clearStatistics()
   */
  void reset();

  /**
   * Clear all contexts from the cache, clearing context hierarchy information as well.
   */
  void clear();

  /**
   * Clear {@linkplain #getHitCount() hit count} and {@linkplain #getMissCount()
   * miss count} statistics for the cache (i.e., reset counters to zero).
   */
  void clearStatistics();

  /**
   * Log the statistics for this {@code ContextCache} at {@code DEBUG} level
   * using the {@value #CONTEXT_CACHE_LOGGING_CATEGORY} logging category.
   * <p>The following information should be logged.
   * <ul>
   * <li>name of the concrete {@code ContextCache} implementation</li>
   * <li>{@linkplain #size}</li>
   * <li>{@linkplain #getParentContextCount() parent context count}</li>
   * <li>{@linkplain #getHitCount() hit count}</li>
   * <li>{@linkplain #getMissCount() miss count}</li>
   * <li>any other information useful for monitoring the state of this cache</li>
   * </ul>
   */
  void logStatistics();

}
