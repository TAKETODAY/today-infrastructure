/*
 * Copyright 2017 - 2024 the original author or authors.
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

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import infra.aot.hint.annotation.Reflective;
import infra.cache.Cache;
import infra.cache.CacheManager;
import infra.cache.interceptor.CacheResolver;
import infra.cache.interceptor.KeyGenerator;
import infra.cache.interceptor.SimpleCacheResolver;
import infra.core.annotation.AliasFor;

/**
 * Annotation indicating that a method (or all methods on a class) triggers a
 * {@link Cache#evict(Object) cache evict} operation.
 *
 * <p>This annotation may be used as a <em>meta-annotation</em> to create custom
 * <em>composed annotations</em> with attribute overrides.
 *
 * @author Costin Leau
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
public @interface CacheEvict {

  /**
   * Alias for {@link #cacheNames}.
   */
  @AliasFor("cacheNames")
  String[] value() default {};

  /**
   * Names of the caches to use for the cache eviction operation.
   * <p>Names may be used to determine the target cache (or caches), matching
   * the qualifier value or bean name of a specific bean definition.
   *
   * @see #value
   * @see CacheConfig#cacheNames
   * @since 4.0
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
   * <li>{@code #result} for a reference to the result of the method invocation, which
   * can only be used if {@link #beforeInvocation()} is {@code false}. For supported
   * wrappers such as {@code Optional}, {@code #result} refers to the actual object,
   * not the wrapper</li>
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
   * eviction operation conditional. Evict that cache if the condition evaluates
   * to {@code true}.
   * <p>Default is {@code ""}, meaning the cache eviction is always performed.
   * <p>The EL expression evaluates against a dedicated context that provides the
   * following meta-data:
   * <ul>
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
   * Whether all the entries inside the cache(s) are removed.
   * <p>By default, only the value under the associated key is removed.
   * <p>Note that setting this parameter to {@code true} and specifying a
   * {@link #key} is not allowed.
   */
  boolean allEntries() default false;

  /**
   * Whether the eviction should occur before the method is invoked.
   * <p>Setting this attribute to {@code true}, causes the eviction to
   * occur irrespective of the method outcome (i.e., whether it threw an
   * exception or not).
   * <p>Defaults to {@code false}, meaning that the cache eviction operation
   * will occur <em>after</em> the advised method is invoked successfully (i.e.
   * only if the invocation did not throw an exception).
   */
  boolean beforeInvocation() default false;

}
