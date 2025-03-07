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

import java.util.List;

import infra.aot.AotDetector;
import infra.context.ApplicationContext;
import infra.context.ApplicationContextInitializer;
import infra.context.support.GenericApplicationContext;
import infra.lang.Assert;
import infra.lang.Nullable;
import infra.logging.Logger;
import infra.logging.LoggerFactory;
import infra.test.annotation.DirtiesContext.HierarchyMode;
import infra.test.context.ApplicationContextFailureProcessor;
import infra.test.context.CacheAwareContextLoaderDelegate;
import infra.test.context.ContextLoadException;
import infra.test.context.ContextLoader;
import infra.test.context.MergedContextConfiguration;
import infra.test.context.SmartContextLoader;
import infra.test.context.aot.AotContextLoader;
import infra.test.context.aot.AotTestContextInitializers;
import infra.test.context.aot.TestContextAotException;
import infra.test.context.util.TestContextFactoriesUtils;

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

  private final AotTestContextInitializers aotTestContextInitializers = new AotTestContextInitializers();

  private final ContextCache contextCache;

  /**
   * The configured failure threshold for errors encountered while attempting to
   * load an {@link ApplicationContext}.
   */
  private final int failureThreshold;

  /**
   * Construct a new {@code DefaultCacheAwareContextLoaderDelegate} using a
   * static {@link DefaultContextCache}.
   * <p>The default cache is static so that each context can be cached and
   * reused for all subsequent tests that declare the same unique context
   * configuration within the same JVM process.
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
    Assert.notNull(contextCache, "ContextCache is required");
    Assert.isTrue(failureThreshold > 0, "'failureThreshold' must be positive");
    this.contextCache = contextCache;
    this.failureThreshold = failureThreshold;
  }

  @Override
  public boolean isContextLoaded(MergedContextConfiguration mergedConfig) {
    mergedConfig = replaceIfNecessary(mergedConfig);
    synchronized(this.contextCache) {
      return this.contextCache.contains(mergedConfig);
    }
  }

  @Override
  public ApplicationContext loadContext(MergedContextConfiguration mergedConfig) {
    mergedConfig = replaceIfNecessary(mergedConfig);
    synchronized(this.contextCache) {
      ApplicationContext context = this.contextCache.get(mergedConfig);
      try {
        if (context == null) {
          int failureCount = this.contextCache.getFailureCount(mergedConfig);
          if (failureCount >= this.failureThreshold) {
            throw new IllegalStateException("""
                    ApplicationContext failure threshold (%d) exceeded: \
                    skipping repeated attempt to load context for %s"""
                    .formatted(this.failureThreshold, mergedConfig));
          }
          try {
            if (mergedConfig instanceof AotMergedContextConfiguration aotMergedConfig) {
              context = loadContextInAotMode(aotMergedConfig);
            }
            else {
              context = loadContextInternal(mergedConfig);
            }
            if (logger.isTraceEnabled()) {
              logger.trace("Storing ApplicationContext [%s] in cache under key %s".formatted(
                      System.identityHashCode(context), mergedConfig));
            }
            this.contextCache.put(mergedConfig, context);
          }
          catch (Exception ex) {
            if (logger.isTraceEnabled()) {
              logger.trace("Incrementing ApplicationContext failure count for " + mergedConfig);
            }
            this.contextCache.incrementFailureCount(mergedConfig);
            Throwable cause = ex;
            if (ex instanceof ContextLoadException cle) {
              cause = cle.getCause();
              for (ApplicationContextFailureProcessor contextFailureProcessor : this.contextFailureProcessors) {
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
          if (logger.isTraceEnabled()) {
            logger.trace("Retrieved ApplicationContext [%s] from cache with key %s".formatted(
                    System.identityHashCode(context), mergedConfig));
          }
        }
      }
      finally {
        this.contextCache.logStatistics();
      }

      return context;
    }
  }

  @Override
  public void closeContext(MergedContextConfiguration mergedConfig, @Nullable HierarchyMode hierarchyMode) {
    mergedConfig = replaceIfNecessary(mergedConfig);
    synchronized(this.contextCache) {
      this.contextCache.remove(mergedConfig, hierarchyMode);
    }
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
  protected ApplicationContext loadContextInternal(MergedContextConfiguration mergedConfig)
          throws Exception {

    ContextLoader contextLoader = getContextLoader(mergedConfig);
    if (contextLoader instanceof SmartContextLoader smartContextLoader) {
      return smartContextLoader.loadContext(mergedConfig);
    }
    else {
      String[] locations = mergedConfig.getLocations();
      Assert.notNull(locations, () -> """
              Cannot load an ApplicationContext with a NULL 'locations' array. \
              Consider annotating test class [%s] with @ContextConfiguration or \
              @ContextHierarchy.""".formatted(mergedConfig.getTestClass().getName()));
      return contextLoader.loadContext(locations);
    }
  }

  protected ApplicationContext loadContextInAotMode(AotMergedContextConfiguration aotMergedConfig) throws Exception {
    Class<?> testClass = aotMergedConfig.getTestClass();
    ApplicationContextInitializer contextInitializer = this.aotTestContextInitializers.getContextInitializer(testClass);
    Assert.state(contextInitializer != null,
            () -> "Failed to load AOT ApplicationContextInitializer for test class [%s]".formatted(testClass.getName()));
    ContextLoader contextLoader = getContextLoader(aotMergedConfig);

    if (logger.isTraceEnabled()) {
      logger.trace("Loading ApplicationContext for AOT runtime for " + aotMergedConfig.getOriginal());
    }
    else if (logger.isDebugEnabled()) {
      logger.debug("Loading ApplicationContext for AOT runtime for test class " +
              aotMergedConfig.getTestClass().getName());
    }

    if (!((contextLoader instanceof AotContextLoader aotContextLoader) &&
            (aotContextLoader.loadContextForAotRuntime(aotMergedConfig.getOriginal(), contextInitializer)
                    instanceof GenericApplicationContext gac))) {
      throw new TestContextAotException("""
              Cannot load ApplicationContext for AOT runtime for %s. The configured \
              ContextLoader [%s] must be an AotContextLoader and must create a \
              GenericApplicationContext."""
              .formatted(aotMergedConfig.getOriginal(), contextLoader.getClass().getName()));
    }
    gac.registerShutdownHook();
    return gac;
  }

  private ContextLoader getContextLoader(MergedContextConfiguration mergedConfig) {
    ContextLoader contextLoader = mergedConfig.getContextLoader();
    Assert.notNull(contextLoader, () -> """
            Cannot load an ApplicationContext with a NULL 'contextLoader'. \
            Consider annotating test class [%s] with @ContextConfiguration or \
            @ContextHierarchy.""".formatted(mergedConfig.getTestClass().getName()));
    return contextLoader;
  }

  /**
   * If the test class associated with the supplied {@link MergedContextConfiguration}
   * has an AOT-optimized {@link ApplicationContext}, this method will create an
   * {@link AotMergedContextConfiguration} to replace the provided {@code MergedContextConfiguration}.
   * <p>Otherwise, this method simply returns the supplied {@code MergedContextConfiguration}
   * unmodified.
   * <p>This allows for transparent {@link ContextCache}
   * support for AOT-optimized application contexts.
   *
   * @param mergedConfig the original {@code MergedContextConfiguration}
   * @return {@code AotMergedContextConfiguration} or the original {@code MergedContextConfiguration}
   * @throws IllegalStateException if running in AOT mode and the test class does not
   * have an AOT-optimized {@code ApplicationContext}
   */
  private MergedContextConfiguration replaceIfNecessary(MergedContextConfiguration mergedConfig) {
    if (AotDetector.useGeneratedArtifacts()) {
      Class<?> testClass = mergedConfig.getTestClass();
      Class<? extends ApplicationContextInitializer> contextInitializerClass =
              this.aotTestContextInitializers.getContextInitializerClass(testClass);
      Assert.state(contextInitializerClass != null, () -> """
              Failed to load AOT ApplicationContextInitializer class for test class [%s]. \
              This can occur if AOT processing has not taken place for the test suite. It \
              can also occur if AOT processing failed for the test class, in which case you \
              can consult the logs generated during AOT processing.""".formatted(testClass.getName()));
      return new AotMergedContextConfiguration(testClass, contextInitializerClass, mergedConfig, this);
    }
    return mergedConfig;
  }

}
