/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2021 All Rights Reserved.
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

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.concurrent.TimeUnit;

import cn.taketoday.lang.Constant;

/**
 * {@code @CacheConfig} provides a mechanism for sharing common cache-related
 * settings at the class level.
 *
 * <p>
 * When this annotation is present on a given class, it provides a set of
 * default settings for any cache operation defined in that class.
 *
 * @author TODAY <br>
 * 2019-02-28 18:00
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface CacheConfig {

  /**
   * Name of the cache for caching operations
   */
  String cacheName() default Constant.BLANK;

  /**
   * The expire time. Use global config if the attribute value is absent, and if
   * the global config is not defined either, use infinity instead.
   *
   * @return the expire time
   */
  long expire() default 0;

  int maxSize() default 0;

  long maxIdleTime() default 0;

  /**
   * Specify the time unit of expire.
   *
   * @return the time unit of expire time
   */
  TimeUnit timeUnit() default TimeUnit.MILLISECONDS;

  CacheConfig EMPTY_CACHE_CONFIG = new CacheConfiguration();

}
