/*
 * Copyright 2017 - 2025 the original author or authors.
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

package infra.cache.annotation;

import org.jspecify.annotations.Nullable;

import infra.cache.CacheManager;
import infra.cache.interceptor.CacheErrorHandler;
import infra.cache.interceptor.CacheResolver;
import infra.cache.interceptor.KeyGenerator;
import infra.cache.interceptor.SimpleCacheErrorHandler;
import infra.context.annotation.Bean;
import infra.context.annotation.Configuration;

/**
 * Interface to be implemented by @{@link Configuration
 * Configuration} classes annotated with @{@link EnableCaching} that wish or need to
 * specify explicitly how caches are resolved and how keys are generated for annotation-driven
 * cache management.
 *
 * <p>See @{@link EnableCaching} for general examples and context; see
 * {@link #cacheManager()}, {@link #cacheResolver()} and {@link #keyGenerator()}
 * for detailed instructions.
 *
 * <p><b>NOTE: A {@code CachingConfigurer} will get initialized early.</b>
 * Do not inject common dependencies into autowired fields directly; instead, consider
 * declaring a lazy {@link infra.beans.factory.ObjectProvider} for those.
 *
 * @author Chris Beams
 * @author Stephane Nicoll
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @see EnableCaching
 * @since 4.0
 */
public interface CachingConfigurer {

  /**
   * Return the cache manager bean to use for annotation-driven cache
   * management. A default {@link CacheResolver} will be initialized
   * behind the scenes with this cache manager. For more fine-grained
   * management of the cache resolution, consider setting the
   * {@link CacheResolver} directly.
   * <p>Implementations must explicitly declare
   * {@link Bean @Bean}, e.g.
   * <pre class="code">
   * &#064;Configuration
   * &#064;EnableCaching
   * public class AppConfig implements CachingConfigurer {
   *     &#064;Bean // important!
   *     &#064;Override
   *     public CacheManager cacheManager() {
   *         // configure and return CacheManager instance
   *     }
   *     // ...
   * }
   * </pre>
   * See @{@link EnableCaching} for more complete examples.
   */
  @Nullable
  default CacheManager cacheManager() {
    return null;
  }

  /**
   * Return the {@link CacheResolver} bean to use to resolve regular caches for
   * annotation-driven cache management. This is an alternative and more powerful
   * option of specifying the {@link CacheManager} to use.
   * <p>If both a {@link #cacheManager()} and {@code #cacheResolver()} are set,
   * the cache manager is ignored.
   * <p>Implementations must explicitly declare
   * {@link Bean @Bean}, e.g.
   * <pre class="code">
   * &#064;Configuration
   * &#064;EnableCaching
   * public class AppConfig implements CachingConfigurer {
   *     &#064;Bean // important!
   *     &#064;Override
   *     public CacheResolver cacheResolver() {
   *         // configure and return CacheResolver instance
   *     }
   *     // ...
   * }
   * </pre>
   * See {@link EnableCaching} for more complete examples.
   */
  @Nullable
  default CacheResolver cacheResolver() {
    return null;
  }

  /**
   * Return the key generator bean to use for annotation-driven cache management.
   * Implementations must explicitly declare
   * {@link Bean @Bean}, e.g.
   * <pre class="code">
   * &#064;Configuration
   * &#064;EnableCaching
   * public class AppConfig implements CachingConfigurer {
   *     &#064;Bean // important!
   *     &#064;Override
   *     public KeyGenerator keyGenerator() {
   *         // configure and return KeyGenerator instance
   *     }
   *     // ...
   * }
   * </pre>
   * See @{@link EnableCaching} for more complete examples.
   */
  @Nullable
  default KeyGenerator keyGenerator() {
    return null;
  }

  /**
   * Return the {@link CacheErrorHandler} to use to handle cache-related errors.
   * <p>By default,{@link SimpleCacheErrorHandler}
   * is used and simply throws the exception back at the client.
   * <p>Implementations must explicitly declare
   * {@link Bean @Bean}, e.g.
   * <pre class="code">
   * &#064;Configuration
   * &#064;EnableCaching
   * public class AppConfig implements CachingConfigurer {
   *     &#064;Bean // important!
   *     &#064;Override
   *     public CacheErrorHandler errorHandler() {
   *         // configure and return CacheErrorHandler instance
   *     }
   *     // ...
   * }
   * </pre>
   * See @{@link EnableCaching} for more complete examples.
   */
  @Nullable
  default CacheErrorHandler errorHandler() {
    return null;
  }

}
