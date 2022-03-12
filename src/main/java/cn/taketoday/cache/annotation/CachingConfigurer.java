/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2022 All Rights Reserved.
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

package cn.taketoday.cache.annotation;

import cn.taketoday.cache.CacheManager;
import cn.taketoday.cache.interceptor.CacheErrorHandler;
import cn.taketoday.cache.interceptor.CacheResolver;
import cn.taketoday.cache.interceptor.KeyGenerator;
import cn.taketoday.cache.interceptor.SimpleCacheErrorHandler;
import cn.taketoday.lang.Nullable;

/**
 * Interface to be implemented by @{@link cn.taketoday.context.annotation.Configuration
 * Configuration} classes annotated with @{@link EnableCaching} that wish or need to
 * specify explicitly how caches are resolved and how keys are generated for annotation-driven
 * cache management.
 *
 * <p>See @{@link EnableCaching} for general examples and context; see
 * {@link #cacheManager()}, {@link #cacheResolver()} and {@link #keyGenerator()}
 * for detailed instructions.
 *
 * @author Chris Beams
 * @author Stephane Nicoll
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
   * {@link cn.taketoday.context.annotation.Bean @Bean}, e.g.
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
   * {@link cn.taketoday.context.annotation.Bean @Bean}, e.g.
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
   * {@link cn.taketoday.context.annotation.Bean @Bean}, e.g.
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
   * {@link cn.taketoday.context.annotation.Bean @Bean}, e.g.
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
