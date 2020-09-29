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
import java.util.concurrent.TimeUnit;

import cn.taketoday.cache.Cache;
import cn.taketoday.context.Constant;

/**
 * Annotation indicating that the result of invoking a method (or all methods in
 * a class) can be cached.
 *
 * <p>
 * Each time an advised method is invoked, caching behavior will be applied,
 * checking whether the method has been already invoked for the given arguments.
 * A sensible default simply uses the method parameters to compute the key
 *
 * <p>
 * If no value is found in the cache for the computed key, the target method
 * will be invoked and the returned value stored in the associated cache.
 *
 * <p>
 * This annotation may be used as a <em>meta-annotation</em> to create custom
 * <em>composed annotations</em> with attribute overrides.
 *
 * @see CacheConfig
 * @see Cache#get(Object)
 * @see Cache#put(Object, Object)
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.METHOD, ElementType.TYPE })
public @interface Cacheable {

  /**
   * Name of the cache for caching operations
   */
  String cacheName() default Constant.BLANK;

  /**
   * Java Unified Expression Language (EL) expression for computing the key
   * dynamically.
   * <p>
   * Default is {@code ""}, meaning all method parameters are considered as a key.
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
   * put operation conditional.
   * <p>
   * Default is {@code ""}, meaning the method result is always cached.
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
   * Java Unified Expression Language (EL) expression used to veto method caching.
   * <p>
   * Unlike {@link #condition}, this expression is evaluated after the method has
   * been called and can therefore refer to the {@code result}.
   * <p>
   * Default is {@code ""}, meaning that caching is never vetoed.
   * <p>
   * The EL evaluates against a dedicated context that provides the following
   * meta-data:
   *
   * <ul>
   * <li>{result} for a reference to the result of the method invocation.</li>
   * <li>${root} target method invocation object
   * <li>Shortcuts for the method name ${root.method.name} and target class
   * ${root.method.clazz} are also available.
   * <li>Method arguments can be accessed by index. For instance the second
   * argument can be accessed via ${root.args[1]}, ${name1} or {@name2}. Arguments
   * can also be accessed by name if that information is available.</li>
   * </ul>
   */
  String unless() default Constant.BLANK;

  /**
   * The expire time. Use global config {@link CacheConfig} If this not present on
   * the method , If the global config is not defined either, use infinity
   * instead.
   *
   * @return the expire time
   */
  long expire() default 0;

  /**
   * Specify the time unit of expire.
   *
   * @return the time unit of expire time
   */
  TimeUnit timeUnit() default TimeUnit.MILLISECONDS;

  /**
   * Synchronize the invocation of the underlying method if several threads are
   * attempting to load a value for the same key.
   */
  boolean sync() default false;

}
