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

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import cn.taketoday.cache.interceptor.CacheResolver;
import cn.taketoday.cache.interceptor.KeyGenerator;
import cn.taketoday.cache.interceptor.SimpleCacheResolver;
import cn.taketoday.cache.CacheManager;

/**
 * {@code @CacheConfig} provides a mechanism for sharing common cache-related
 * settings at the class level.
 *
 * <p>When this annotation is present on a given class, it provides a set
 * of default settings for any cache operation defined in that class.
 *
 * @author Stephane Nicoll
 * @author Sam Brannen
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2022/3/9 21:36
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface CacheConfig {

  /**
   * Names of the default caches to consider for caching operations defined
   * in the annotated class.
   * <p>If none is set at the operation level, these are used instead of the default.
   * <p>May be used to determine the target cache (or caches), matching the
   * qualifier value or the bean names of a specific bean definition.
   */
  String[] cacheNames() default {};

  /**
   * The bean name of the default {@link KeyGenerator} to
   * use for the class.
   * <p>If none is set at the operation level, this one is used instead of the default.
   * <p>The key generator is mutually exclusive with the use of a custom key. When such key is
   * defined for the operation, the value of this key generator is ignored.
   */
  String keyGenerator() default "";

  /**
   * The bean name of the custom {@link CacheManager} to use to
   * create a default {@link CacheResolver} if none
   * is set already.
   * <p>If no resolver and no cache manager are set at the operation level, and no cache
   * resolver is set via {@link #cacheResolver}, this one is used instead of the default.
   *
   * @see SimpleCacheResolver
   */
  String cacheManager() default "";

  /**
   * The bean name of the custom {@link CacheResolver} to use.
   * <p>If no resolver and no cache manager are set at the operation level, this one is used
   * instead of the default.
   */
  String cacheResolver() default "";

}
