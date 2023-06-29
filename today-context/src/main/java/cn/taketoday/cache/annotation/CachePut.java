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

package cn.taketoday.cache.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import cn.taketoday.aot.hint.annotation.Reflective;
import cn.taketoday.cache.Cache;
import cn.taketoday.cache.CacheManager;
import cn.taketoday.cache.interceptor.CacheResolver;
import cn.taketoday.cache.interceptor.KeyGenerator;
import cn.taketoday.cache.interceptor.SimpleCacheResolver;
import cn.taketoday.core.annotation.AliasFor;

/**
 * Annotation indicating that a method (or all methods on a class) triggers a
 * {@link Cache#put(Object, Object) cache put} operation.
 *
 * <p>In contrast to the {@link Cacheable @Cacheable} annotation, this annotation
 * does not cause the advised method to be skipped. Rather, it always causes the
 * method to be invoked and its result to be stored in the associated cache if the
 * {@link #condition()} and {@link #unless()} expressions match accordingly. Note
 * that Java8's {@code Optional} return types are automatically handled and its
 * content is stored in the cache if present.
 *
 * <p>This annotation may be used as a <em>meta-annotation</em> to create custom
 * <em>composed annotations</em> with attribute overrides.
 *
 * @author Costin Leau
 * @author Phillip Webb
 * @author Stephane Nicoll
 * @author Sam Brannen
 * @see CacheConfig
 * @since 4.0
 */
@Target({ ElementType.TYPE, ElementType.METHOD })
@Retention(RetentionPolicy.RUNTIME)
@Inherited
@Documented
@Reflective
public @interface CachePut {

  /**
   * Alias for {@link #cacheNames}.
   */
  @AliasFor("cacheNames")
  String[] value() default {};

  /**
   * Names of the caches to use for the cache put operation.
   * <p>Names may be used to determine the target cache (or caches), matching
   * the qualifier value or bean name of a specific bean definition.
   *
   * @see #value
   * @see CacheConfig#cacheNames
   */
  @AliasFor("value")
  String[] cacheNames() default {};

  /**
   * Expression Language (SpEL) expression for computing the key dynamically.
   * <p>Default is {@code ""}, meaning all method parameters are considered as a key,
   * unless a custom {@link #keyGenerator} has been set.
   * <p>The EL expression evaluates against a dedicated context that provides the
   * following meta-data:
   * <ul>
   * <li>{@code #result} for a reference to the result of the method invocation. For
   * supported wrappers such as {@code Optional}, {@code #result} refers to the actual
   * object, not the wrapper</li>
   * <li>{@code #root.method}, {@code #root.target}, and {@code #root.caches} for
   * references to the {@link java.lang.reflect.Method method}, target object, and
   * affected cache(s) respectively.</li>
   * <li>Shortcuts for the method name ({@code #root.methodName}) and target class
   * ({@code #root.targetClass}) are also available.
   * <li>Method arguments can be accessed by index. For instance the second argument
   * can be accessed via {@code #root.args[1]}, {@code #p1} or {@code #a1}. Arguments
   * can also be accessed by name if that information is available.</li>
   * </ul>
   */
  String key() default "";

  /**
   * The bean name of the custom {@link KeyGenerator}
   * to use.
   * <p>Mutually exclusive with the {@link #key} attribute.
   *
   * @see CacheConfig#keyGenerator
   */
  String keyGenerator() default "";

  /**
   * The bean name of the custom {@link CacheManager} to use to
   * create a default {@link CacheResolver} if none
   * is set already.
   * <p>Mutually exclusive with the {@link #cacheResolver} attribute.
   *
   * @see SimpleCacheResolver
   * @see CacheConfig#cacheManager
   */
  String cacheManager() default "";

  /**
   * The bean name of the custom {@link CacheResolver}
   * to use.
   *
   * @see CacheConfig#cacheResolver
   */
  String cacheResolver() default "";

  /**
   * Expression Language (SpEL) expression used for making the cache
   * put operation conditional. Update the cache if the condition evaluates to
   * {@code true}.
   * <p>This expression is evaluated after the method has been called due to the
   * nature of the put operation and can therefore refer to the {@code result}.
   * <p>Default is {@code ""}, meaning the method result is always cached.
   * <p>The EL expression evaluates against a dedicated context that provides the
   * following meta-data:
   * <ul>
   * <li>{@code #result} for a reference to the result of the method invocation. For
   * supported wrappers such as {@code Optional}, {@code #result} refers to the actual
   * object, not the wrapper</li>
   * <li>{@code #root.method}, {@code #root.target}, and {@code #root.caches} for
   * references to the {@link java.lang.reflect.Method method}, target object, and
   * affected cache(s) respectively.</li>
   * <li>Shortcuts for the method name ({@code #root.methodName}) and target class
   * ({@code #root.targetClass}) are also available.
   * <li>Method arguments can be accessed by index. For instance the second argument
   * can be accessed via {@code #root.args[1]}, {@code #p1} or {@code #a1}. Arguments
   * can also be accessed by name if that information is available.</li>
   * </ul>
   */
  String condition() default "";

  /**
   * Expression Language (SpEL) expression used to veto the cache put operation.
   * Veto updating the cache if the condition evaluates to {@code true}.
   * <p>Default is {@code ""}, meaning that caching is never vetoed.
   * <p>The EL expression evaluates against a dedicated context that provides the
   * following meta-data:
   * <ul>
   * <li>{@code #result} for a reference to the result of the method invocation. For
   * supported wrappers such as {@code Optional}, {@code #result} refers to the actual
   * object, not the wrapper</li>
   * <li>{@code #root.method}, {@code #root.target}, and {@code #root.caches} for
   * references to the {@link java.lang.reflect.Method method}, target object, and
   * affected cache(s) respectively.</li>
   * <li>Shortcuts for the method name ({@code #root.methodName}) and target class
   * ({@code #root.targetClass}) are also available.
   * <li>Method arguments can be accessed by index. For instance the second argument
   * can be accessed via {@code #root.args[1]}, {@code #p1} or {@code #a1}. Arguments
   * can also be accessed by name if that information is available.</li>
   * </ul>
   */
  String unless() default "";

}
