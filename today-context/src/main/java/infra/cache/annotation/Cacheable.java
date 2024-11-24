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
import java.util.concurrent.Callable;

import infra.aot.hint.annotation.Reflective;
import infra.cache.CacheManager;
import infra.cache.interceptor.CacheResolver;
import infra.cache.interceptor.KeyGenerator;
import infra.core.annotation.AliasFor;
import infra.cache.Cache;
import infra.cache.interceptor.SimpleCacheResolver;

/**
 * Annotation indicating that the result of invoking a method (or all methods
 * in a class) can be cached.
 *
 * <p>Each time an advised method is invoked, caching behavior will be applied,
 * checking whether the method has been already invoked for the given arguments.
 * A sensible default simply uses the method parameters to compute the key, but
 * a EL expression can be provided via the {@link #key} attribute, or a custom
 * {@link KeyGenerator} implementation can
 * replace the default one (see {@link #keyGenerator}).
 *
 * <p>If no value is found in the cache for the computed key, the target method
 * will be invoked and the returned value will be stored in the associated cache.
 * Note that {@link java.util.Optional} return types are unwrapped automatically.
 * If an {@code Optional} value is {@linkplain java.util.Optional#isPresent()
 * present}, it will be stored in the associated cache. If an {@code Optional}
 * value is not present, {@code null} will be stored in the associated cache.
 *
 * <p>This annotation may be used as a <em>meta-annotation</em> to create custom
 * <em>composed annotations</em> with attribute overrides.
 *
 * @author Costin Leau
 * @author Phillip Webb
 * @author Stephane Nicoll
 * @author Sam Brannen
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see CacheConfig
 * @since 4.0
 */
@Inherited
@Documented
@Reflective
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.TYPE, ElementType.METHOD })
public @interface Cacheable {

  /**
   * Alias for {@link #cacheNames}.
   */
  @AliasFor("cacheNames")
  String[] value() default {};

  /**
   * Names of the caches in which method invocation results are stored.
   * <p>Names may be used to determine the target cache(s), to be resolved via the
   * configured {@link #cacheResolver()} which typically delegates to
   * {@link CacheManager#getCache}.
   * <p>This will usually be a single cache name. If multiple names are specified,
   * they will be consulted for a cache hit in the order of definition, and they
   * will all receive a put/evict request for the same newly cached value.
   * <p>Note that asynchronous/reactive cache access may not fully consult all
   * specified caches, depending on the target cache. In the case of late-determined
   * cache misses (e.g. with Redis), further caches will not get consulted anymore.
   * As a consequence, specifying multiple cache names in an async cache mode setup
   * only makes sense with early-determined cache misses (e.g. with Caffeine).
   *
   * @see #value
   * @see CacheConfig#cacheNames
   */
  @AliasFor("value")
  String[] cacheNames() default {};

  /**
   * Spring Expression Language (SpEL) expression for computing the key dynamically.
   * <p>Default is {@code ""}, meaning all method parameters are considered as a key,
   * unless a custom {@link #keyGenerator} has been configured.
   * <p>The SpEL expression evaluates against a dedicated context that provides the
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
   * <p>Mutually exclusive with the {@link #cacheResolver}  attribute.
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
   * Spring Expression Language (SpEL) expression used for making the method
   * caching conditional. Cache the result if the condition evaluates to
   * {@code true}.
   * <p>Default is {@code ""}, meaning the method result is always cached.
   * <p>The SpEL expression evaluates against a dedicated context that provides the
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
   * Spring Expression Language (SpEL) expression used to veto method caching.
   * Veto caching the result if the condition evaluates to {@code true}.
   *
   * <p>Unlike {@link #condition}, this expression is evaluated after the method
   * has been called and can therefore refer to the {@code result}.
   * <p>Default is {@code ""}, meaning that caching is never vetoed.
   * <p>The SpEL expression evaluates against a dedicated context that provides the
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

  /**
   * Synchronize the invocation of the underlying method if several threads are
   * attempting to load a value for the same key. The synchronization leads to
   * a couple of limitations:
   * <ol>
   * <li>{@link #unless()} is not supported</li>
   * <li>Only one cache may be specified</li>
   * <li>No other cache-related operation can be combined</li>
   * </ol>
   * This is effectively a hint and the chosen cache provider might not actually
   * support it in a synchronized fashion. Check your provider documentation for
   * more details on the actual semantics.
   *
   * @see Cache#get(Object, Callable)
   */
  boolean sync() default false;

}
