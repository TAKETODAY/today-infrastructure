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

package cn.taketoday.cache.jcache.config;

import cn.taketoday.cache.annotation.CachingConfigurer;
import cn.taketoday.cache.annotation.EnableCaching;
import cn.taketoday.cache.interceptor.CacheResolver;
import cn.taketoday.lang.Nullable;

/**
 * Extension of {@link CachingConfigurer} for the JSR-107 implementation.
 *
 * <p>To be implemented by classes annotated with
 * {@link EnableCaching} that wish
 * or need to specify explicitly how exception caches are resolved for
 * annotation-driven cache management.
 *
 * <p>See {@link EnableCaching} for
 * general examples and context; see {@link #exceptionCacheResolver()} for
 * detailed instructions.
 *
 * @author Stephane Nicoll
 * @see CachingConfigurer
 * @see EnableCaching
 * @since 4.0
 */
public interface JCacheConfigurer extends CachingConfigurer {

  /**
   * Return the {@link CacheResolver} bean to use to resolve exception caches for
   * annotation-driven cache management. Implementations must explicitly declare
   * {@link cn.taketoday.context.annotation.Bean @Bean}, e.g.
   * <pre class="code">
   * &#064;Configuration
   * &#064;EnableCaching
   * public class AppConfig implements JCacheConfigurer {
   *     &#064;Bean // important!
   *     &#064;Override
   *     public CacheResolver exceptionCacheResolver() {
   *         // configure and return CacheResolver instance
   *     }
   *     // ...
   * }
   * </pre>
   * See {@link EnableCaching} for more complete examples.
   */
  @Nullable
  default CacheResolver exceptionCacheResolver() {
    return null;
  }

}
