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

import cn.taketoday.context.ApplicationContext;
import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Nullable;
import cn.taketoday.logging.Logger;
import cn.taketoday.logging.LoggerFactory;
import cn.taketoday.test.annotation.DirtiesContext.HierarchyMode;
import cn.taketoday.test.context.CacheAwareContextLoaderDelegate;
import cn.taketoday.test.context.ContextLoader;
import cn.taketoday.test.context.MergedContextConfiguration;
import cn.taketoday.test.context.SmartContextLoader;

/**
 * Default implementation of the {@link CacheAwareContextLoaderDelegate} interface.
 *
 * <p>To use a static {@link DefaultContextCache}, invoke the
 * {@link #DefaultCacheAwareContextLoaderDelegate()} constructor; otherwise,
 * invoke the {@link #DefaultCacheAwareContextLoaderDelegate(ContextCache)}
 * and provide a custom {@link ContextCache} implementation.
 *
 * @author Sam Brannen
 * @since 4.0
 */
public class DefaultCacheAwareContextLoaderDelegate implements CacheAwareContextLoaderDelegate {

  private static final Logger logger = LoggerFactory.getLogger(DefaultCacheAwareContextLoaderDelegate.class);

  /**
   * Default static cache of Infra application contexts.
   */
  static final ContextCache defaultContextCache = new DefaultContextCache();

  private final ContextCache contextCache;

  /**
   * Construct a new {@code DefaultCacheAwareContextLoaderDelegate} using
   * a static {@link DefaultContextCache}.
   * <p>This default cache is static so that each context can be cached
   * and reused for all subsequent tests that declare the same unique
   * context configuration within the same JVM process.
   *
   * @see #DefaultCacheAwareContextLoaderDelegate(ContextCache)
   */
  public DefaultCacheAwareContextLoaderDelegate() {
    this(defaultContextCache);
  }

  /**
   * Construct a new {@code DefaultCacheAwareContextLoaderDelegate} using
   * the supplied {@link ContextCache}.
   *
   * @see #DefaultCacheAwareContextLoaderDelegate()
   */
  public DefaultCacheAwareContextLoaderDelegate(ContextCache contextCache) {
    Assert.notNull(contextCache, "ContextCache must not be null");
    this.contextCache = contextCache;
  }

  /**
   * Get the {@link ContextCache} used by this context loader delegate.
   */
  protected ContextCache getContextCache() {
    return this.contextCache;
  }

  /**
   * Load the {@code ApplicationContext} for the supplied merged context configuration.
   * <p>Supports both the {@link SmartContextLoader} and {@link ContextLoader} SPIs.
   *
   * @throws Exception if an error occurs while loading the application context
   */
  protected ApplicationContext loadContextInternal(MergedContextConfiguration mergedContextConfiguration)
          throws Exception {

    ContextLoader contextLoader = mergedContextConfiguration.getContextLoader();
    Assert.notNull(contextLoader, "Cannot load an ApplicationContext with a NULL 'contextLoader'. " +
            "Consider annotating your test class with @ContextConfiguration or @ContextHierarchy.");

    if (contextLoader instanceof SmartContextLoader smartContextLoader) {
      return smartContextLoader.loadContext(mergedContextConfiguration);
    }

    String[] locations = mergedContextConfiguration.getLocations();
    Assert.notNull(locations, "Cannot load an ApplicationContext with a NULL 'locations' array. " +
            "Consider annotating your test class with @ContextConfiguration or @ContextHierarchy.");
    return contextLoader.loadContext(locations);
  }

  @Override
  public boolean isContextLoaded(MergedContextConfiguration mergedContextConfiguration) {
    synchronized(this.contextCache) {
      return this.contextCache.contains(mergedContextConfiguration);
    }
  }

  @Override
  public ApplicationContext loadContext(MergedContextConfiguration mergedContextConfiguration) {
    synchronized(this.contextCache) {
      ApplicationContext context = this.contextCache.get(mergedContextConfiguration);
      if (context == null) {
        try {
          context = loadContextInternal(mergedContextConfiguration);
          logger.debug("Storing ApplicationContext [{}] in cache under key [{}]",
                  System.identityHashCode(context), mergedContextConfiguration);
          this.contextCache.put(mergedContextConfiguration, context);
        }
        catch (Exception ex) {
          throw new IllegalStateException("Failed to load ApplicationContext", ex);
        }
      }
      else {
        logger.debug("Retrieved ApplicationContext [{}] from cache with key [{}]",
                System.identityHashCode(context), mergedContextConfiguration);
      }

      this.contextCache.logStatistics();

      return context;
    }
  }

  @Override
  public void closeContext(MergedContextConfiguration mergedContextConfiguration, @Nullable HierarchyMode hierarchyMode) {
    synchronized(this.contextCache) {
      this.contextCache.remove(mergedContextConfiguration, hierarchyMode);
    }
  }

}
