/**
 * Original Author -> 杨海健 (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2020 All Rights Reserved.
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

import cn.taketoday.cache.Cache;
import cn.taketoday.context.Constant;

/**
 * {@link Cache} eviction
 *
 * @author TODAY <br>
 *         2019-02-17 17:46
 * @see Cache#evict(Object)
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.METHOD, ElementType.TYPE })
public @interface CacheEvict {

  /**
   * Name of the cache for caching operations
   */
  String cacheName() default Constant.BLANK;

  /**
   * Java Unified Expression Language (EL) expression for computing the key
   * dynamically.
   * <p>
   * Default is {@code ""}, meaning all method parameters are considered as a key
   * <p>
   * The EL evaluates against a dedicated context that provides the following
   * meta-data:
   * <ul>
   * <li>${root} target method invocation object
   * <li>Shortcuts for the method name ${root.method.name} and target class
   * ${root.method.clazz} are also available.
   * <li>Method arguments can be accessed by index. For instance the second
   * argument can be accessed via ${root.args[1]}, ${name1} or {@name2}. Arguments
   * can also be accessed by name if that information is available.</li>
   * </ul>
   */
  String key() default Constant.BLANK;

  /**
   * Java Unified Expression Language (EL) expression used for making the cache
   * eviction operation conditional.
   * <p>
   * Default is {@code ""}, meaning the cache eviction is always performed.
   * <p>
   * The EL evaluates against a dedicated context that provides the following
   * meta-data:
   * <ul>
   * <li>${root} target method invocation object
   * <li>Shortcuts for the method name ${root.method.name} and target class
   * ${root.method.clazz} are also available.
   * <li>Method arguments can be accessed by index. For instance the second
   * argument can be accessed via ${root.args[1]}, ${name1} or {@name2}. Arguments
   * can also be accessed by name if that information is available.</li>
   * </ul>
   */
  String condition() default Constant.BLANK;

  /**
   * Whether all the entries inside the cache(s) are removed.
   * <p>
   * By default, only the value under the associated key is removed.
   * <p>
   * Note that setting this parameter to {@code true} and specifying a
   * {@link #key} is not allowed.
   */
  boolean allEntries() default false;

  /**
   * Whether the eviction should occur before the method is invoked.
   * <p>
   * Setting this attribute to {@code true}, causes the eviction to occur
   * irrespective of the method outcome (i.e., whether it threw an exception or
   * not).
   * <p>
   * Defaults to {@code false}, meaning that the cache eviction operation will
   * occur <em>after</em> the advised method is invoked successfully (i.e., only
   * if the invocation did not throw an exception).
   */
  boolean beforeInvocation() default false;

}
