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

package cn.taketoday.test.context;

import cn.taketoday.context.ApplicationContext;
import cn.taketoday.context.ConfigurableApplicationContext;
import cn.taketoday.lang.Nullable;
import cn.taketoday.test.annotation.DirtiesContext.HierarchyMode;
import cn.taketoday.test.context.cache.ContextCache;
import cn.taketoday.test.context.cache.DefaultCacheAwareContextLoaderDelegate;

/**
 * A {@code CacheAwareContextLoaderDelegate} is responsible for {@linkplain
 * #loadContext loading} and {@linkplain #closeContext closing} application
 * contexts, interacting transparently with a
 * {@link ContextCache ContextCache}
 * behind the scenes.
 *
 * <p>Note: {@code CacheAwareContextLoaderDelegate} does not extend the
 * {@link ContextLoader} or {@link SmartContextLoader} interface.
 *
 * @author Sam Brannen
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
public interface CacheAwareContextLoaderDelegate {

  /**
   * The default failure threshold for errors encountered while attempting to
   * load an application context: {@value}.
   *
   * @see #CONTEXT_FAILURE_THRESHOLD_PROPERTY_NAME
   */
  int DEFAULT_CONTEXT_FAILURE_THRESHOLD = 1;

  /**
   * System property used to configure the failure threshold for errors
   * encountered while attempting to load an application context: {@value}.
   * <p>May alternatively be configured via the
   * {@link cn.taketoday.lang.TodayStrategies} mechanism.
   * <p>Implementations of {@code CacheAwareContextLoaderDelegate} are not
   * required to support this feature. Consult the documentation of the
   * corresponding implementation for details. Note, however, that the standard
   * {@code CacheAwareContextLoaderDelegate} implementation in Infra supports
   * this feature.
   *
   * @see #DEFAULT_CONTEXT_FAILURE_THRESHOLD
   * @see #loadContext(MergedContextConfiguration)
   */
  String CONTEXT_FAILURE_THRESHOLD_PROPERTY_NAME = "infra.test.context.failure.threshold";

  /**
   * System property used to configure the fully qualified class name of the
   * default {@code CacheAwareContextLoaderDelegate}.
   * <p>May alternatively be configured via the
   * {@link cn.taketoday.lang.TodayStrategies} mechanism.
   * <p>If this property is not defined, the
   * {@link DefaultCacheAwareContextLoaderDelegate
   * DefaultCacheAwareContextLoaderDelegate} will be used as the default.
   */
  String DEFAULT_CACHE_AWARE_CONTEXT_LOADER_DELEGATE_PROPERTY_NAME =
          "infra.test.context.default.CacheAwareContextLoaderDelegate";

  /**
   * Determine if the {@linkplain ApplicationContext application context} for
   * the supplied {@link MergedContextConfiguration} has been loaded (i.e.,
   * is present in the {@code ContextCache}).
   * <p>Implementations of this method <strong>must not</strong> load the
   * application context as a side effect. In addition, implementations of
   * this method should not log the cache statistics via
   * {@link ContextCache#logStatistics()}.
   * <p>The default implementation of this method always returns {@code false}.
   * Custom {@code CacheAwareContextLoaderDelegate} implementations are
   * therefore highly encouraged to override this method with a more meaningful
   * implementation. Note that the standard {@code CacheAwareContextLoaderDelegate}
   * implementation in Infra overrides this method appropriately.
   *
   * @param mergedContextConfiguration the merged context configuration used
   * to load the application context; never {@code null}
   * @return {@code true} if the application context has been loaded
   * @see #loadContext
   * @see #closeContext
   */
  default boolean isContextLoaded(MergedContextConfiguration mergedContextConfiguration) {
    return false;
  }

  /**
   * Load the {@linkplain ApplicationContext application context} for the supplied
   * {@link MergedContextConfiguration} by delegating to the {@link ContextLoader}
   * configured in the given {@code MergedContextConfiguration}.
   * <p>If the context is present in the {@code ContextCache} it will simply
   * be returned; otherwise, it will be loaded, stored in the cache, and returned.
   * <p>Implementations of this method should load
   * {@link ApplicationContextFailureProcessor} implementations via the
   * {@link cn.taketoday.lang.TodayStrategies TodayStrategies}
   * mechanism, catch any exception thrown by the {@link ContextLoader}, and
   * delegate to each of the configured failure processors to process the context
   * load failure if the exception is an instance of {@link ContextLoadException}.
   *
   * <p>Implementations of this method are encouraged
   * to support the <em>failure threshold</em> feature. Specifically, if repeated
   * attempts are made to load an application context and that application
   * context consistently fails to load &mdash; for example, due to a configuration
   * error that prevents the context from successfully loading &mdash; this
   * method should preemptively throw an {@link IllegalStateException} if the
   * configured failure threshold has been exceeded. Note that the {@code ContextCache}
   * provides support for tracking and incrementing the failure count for a given
   * context cache key.
   * <p>The cache statistics should be logged by invoking {@link ContextCache#logStatistics()}.
   *
   * @param mergedConfig the merged context configuration to use to load the
   * application context; never {@code null}
   * @return the application context (never {@code null})
   * @throws IllegalStateException if an error occurs while retrieving or loading
   * the application context
   * @see #isContextLoaded
   * @see #closeContext
   */
  ApplicationContext loadContext(MergedContextConfiguration mergedConfig);

  /**
   * Remove the {@linkplain ApplicationContext application context} for the
   * supplied {@link MergedContextConfiguration} from the {@code ContextCache}
   * and {@linkplain ConfigurableApplicationContext#close() close} it if it is
   * an instance of {@link ConfigurableApplicationContext}.
   * <p>The semantics of the supplied {@code HierarchyMode} must be honored when
   * removing the context from the cache. See the Javadoc for {@link HierarchyMode}
   * for details.
   * <p>Generally speaking, this method should only be called if the state of
   * a singleton bean has been changed (potentially affecting future interaction
   * with the context) or if the context needs to be prematurely removed from
   * the cache.
   *
   * @param mergedContextConfiguration the merged context configuration for the
   * application context to close; never {@code null}
   * @param hierarchyMode the hierarchy mode; may be {@code null} if the context
   * is not part of a hierarchy
   * @see #isContextLoaded
   * @see #loadContext
   */
  void closeContext(MergedContextConfiguration mergedContextConfiguration, @Nullable HierarchyMode hierarchyMode);

}
