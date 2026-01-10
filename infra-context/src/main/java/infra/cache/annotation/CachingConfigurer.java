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
