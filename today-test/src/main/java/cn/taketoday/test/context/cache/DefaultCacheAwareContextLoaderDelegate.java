/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © Harry Yang & 2017 - 2023 All Rights Reserved.
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

import java.util.List;

import cn.taketoday.context.ApplicationContext;
import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Nullable;
import cn.taketoday.logging.Logger;
import cn.taketoday.logging.LoggerFactory;
import cn.taketoday.test.annotation.DirtiesContext.HierarchyMode;
import cn.taketoday.test.context.ApplicationContextFailureProcessor;
import cn.taketoday.test.context.CacheAwareContextLoaderDelegate;
import cn.taketoday.test.context.ContextLoadException;
import cn.taketoday.test.context.ContextLoader;
import cn.taketoday.test.context.MergedContextConfiguration;
import cn.taketoday.test.context.SmartContextLoader;
import cn.taketoday.test.context.util.TestContextFactoriesUtils;

/**
 * Default implementation of the {@link CacheAwareContextLoaderDelegate} interface.
 *
 * <p>To use a static {@link DefaultContextCache}, invoke the
 * {@link #DefaultCacheAwareContextLoaderDelegate()} constructor; otherwise,
 * invoke the {@link #DefaultCacheAwareContextLoaderDelegate(ContextCache)}
 * and provide a custom {@link ContextCache} implementation.
 *
 * @author Sam Brannen
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
public class DefaultCacheAwareContextLoaderDelegate implements CacheAwareContextLoaderDelegate {

  private static final Logger logger = LoggerFactory.getLogger(DefaultCacheAwareContextLoaderDelegate.class);

  /**
   * Default static cache of Infra application contexts.
   */
  static final ContextCache defaultContextCache = new DefaultContextCache();

  private final List<ApplicationContextFailureProcessor> contextFailureProcessors =
          TestContextFactoriesUtils.loadFactoryImplementations(ApplicationContextFailureProcessor.class);

  private final ContextCache contextCache;

  /**
   * The configured failure threshold for errors encountered while attempting to
   * load an {@link ApplicationContext}.
   */
  private final int failureThreshold;

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
   * Construct a new {@code DefaultCacheAwareContextLoaderDelegate} using the
   * supplied {@link ContextCache} and the default or user-configured context
   * failure threshold.
   *
   * @see #DefaultCacheAwareContextLoaderDelegate()
   * @see ContextCacheUtils#retrieveContextFailureThreshold()
   */
  public DefaultCacheAwareContextLoaderDelegate(ContextCache contextCache) {
    this(contextCache, ContextCacheUtils.retrieveContextFailureThreshold());
  }

  /**
   * Construct a new {@code DefaultCacheAwareContextLoaderDelegate} using the
   * supplied {@link ContextCache} and context failure threshold.
   */
  private DefaultCacheAwareContextLoaderDelegate(ContextCache contextCache, int failureThreshold) {
    Assert.notNull(contextCache, "ContextCache must not be null");
    Assert.isTrue(failureThreshold > 0, "'failureThreshold' must be positive");
    this.contextCache = contextCache;
    this.failureThreshold = failureThreshold;
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
  protected ApplicationContext loadContextInternal(MergedContextConfiguration mergedContextConfig)
          throws Exception {

    ContextLoader contextLoader = mergedContextConfig.getContextLoader();
    Assert.notNull(contextLoader, "Cannot load an ApplicationContext with a NULL 'contextLoader'. " +
            "Consider annotating your test class with @ContextConfiguration or @ContextHierarchy.");

    if (contextLoader instanceof SmartContextLoader smartContextLoader) {
      return smartContextLoader.loadContext(mergedContextConfig);
    }

    String[] locations = mergedContextConfig.getLocations();
    Assert.notNull(locations, "Cannot load an ApplicationContext with a NULL 'locations' array. " +
            "Consider annotating your test class with @ContextConfiguration or @ContextHierarchy.");
    return contextLoader.loadContext(locations);
  }

  @Override
  public boolean isContextLoaded(MergedContextConfiguration mergedContextConfiguration) {
    synchronized(contextCache) {
      return contextCache.contains(mergedContextConfiguration);
    }
  }

  @Override
  public ApplicationContext loadContext(MergedContextConfiguration mergedConfig) {
    synchronized(contextCache) {
      ApplicationContext context = contextCache.get(mergedConfig);
      try {
        if (context == null) {
          int failureCount = contextCache.getFailureCount(mergedConfig);
          if (failureCount >= failureThreshold) {
            throw new IllegalStateException("""
                    ApplicationContext failure threshold (%d) exceeded: \
                    skipping repeated attempt to load context for %s"""
                    .formatted(failureThreshold, mergedConfig));
          }
          try {
            context = loadContextInternal(mergedConfig);
            if (logger.isTraceEnabled()) {
              logger.trace("Storing ApplicationContext [{}] in cache under key [{}]",
                      System.identityHashCode(context), mergedConfig);
            }
            contextCache.put(mergedConfig, context);
          }
          catch (Exception ex) {
            if (logger.isTraceEnabled()) {
              logger.trace("Incrementing ApplicationContext failure count for {}", mergedConfig);
            }
            contextCache.incrementFailureCount(mergedConfig);
            Throwable cause = ex;
            if (ex instanceof ContextLoadException cle) {
              cause = cle.getCause();
              for (var contextFailureProcessor : contextFailureProcessors) {
                try {
                  contextFailureProcessor.processLoadFailure(cle.getApplicationContext(), cause);
                }
                catch (Throwable throwable) {
                  if (logger.isDebugEnabled()) {
                    logger.debug("Ignoring exception thrown from ApplicationContextFailureProcessor [%s]: %s"
                            .formatted(contextFailureProcessor, throwable));
                  }
                }
              }
            }
            throw new IllegalStateException(
                    "Failed to load ApplicationContext for " + mergedConfig, cause);
          }
        }
        else {
          logger.debug("Retrieved ApplicationContext [{}] from cache with key [{}]",
                  System.identityHashCode(context), mergedConfig);
        }
      }
      finally {
        contextCache.logStatistics();
      } return context;
    }
  }

  @Override
  public void closeContext(MergedContextConfiguration mergedConfig, @Nullable HierarchyMode hierarchyMode) {
    synchronized(contextCache) {
      contextCache.remove(mergedConfig, hierarchyMode);
    }
  }

}
