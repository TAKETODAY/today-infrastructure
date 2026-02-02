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

package infra.resilience.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import infra.aot.hint.annotation.Reflective;
import infra.core.annotation.AliasFor;

/**
 * A common annotation specifying a concurrency limit for an individual method,
 * or for all proxy-invoked methods in a given class hierarchy if annotated at
 * the type level. The default behavior is to block further method invocations
 * when the limit has been reached. Alternatively, further invocations can be
 * rejected through configuring {@link #policy()} as {@code policy = REJECT}.
 *
 * <p>In the type-level case, all methods inheriting the concurrency limit
 * from the type level share a common concurrency throttle, with any mix
 * of such method invocations contributing to the shared concurrency limit.
 * Whereas for a locally annotated method, a local throttle with the specified
 * limit is going to be applied to invocations of that particular method only.
 *
 * <p>This is particularly useful with Virtual Threads where there is generally
 * no thread pool limit in place. For asynchronous tasks, this can be constrained
 * on {@link infra.core.task.SimpleAsyncTaskExecutor}. For
 * synchronous invocations, this annotation provides equivalent behavior through
 * {@link infra.aop.interceptor.ConcurrencyThrottleInterceptor}.
 * Alternatively, consider {@link infra.core.task.SyncTaskExecutor}
 * and its inherited concurrency throttling support (new as of 5.0) for
 * programmatic use.
 *
 * @author Juergen Hoeller
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @see EnableResilientMethods
 * @see ConcurrencyLimitBeanPostProcessor
 * @see infra.aop.interceptor.ConcurrencyThrottleInterceptor
 * @see infra.core.task.SimpleAsyncTaskExecutor#setConcurrencyLimit
 * @since 5.0
 */
@Target({ ElementType.TYPE, ElementType.METHOD })
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Reflective
public @interface ConcurrencyLimit {

  /**
   * Alias for {@link #limit()}.
   * <p>Intended to be used when no other attributes are needed &mdash; for
   * example, {@code @ConcurrencyLimit(5)}.
   *
   * @see #limitString()
   */
  @AliasFor("limit")
  int value() default Integer.MIN_VALUE;

  /**
   * The concurrency limit.
   * <p>Specify {@code 1} to effectively lock the target instance for each method
   * invocation.
   * <p>Specify a limit greater than {@code 1} for pool-like throttling, constraining
   * the number of concurrent invocations similar to the upper bound of a pool.
   * <p>Specify {@code -1} for unbounded concurrency.
   *
   * @see #value()
   * @see #limitString()
   * @see infra.util.ConcurrencyThrottleSupport#UNBOUNDED_CONCURRENCY
   */
  @AliasFor("value")
  int limit() default Integer.MIN_VALUE;

  /**
   * The concurrency limit, as a configurable String.
   * <p>A non-empty value specified here overrides the {@link #limit()} and
   * {@link #value()} attributes.
   * <p>This supports Infra-style "${...}" placeholders as well as SpEL expressions.
   * <p>See the Javadoc for {@link #limit()} for details on supported values.
   *
   * @see #limit()
   * @see infra.util.ConcurrencyThrottleSupport#UNBOUNDED_CONCURRENCY
   */
  String limitString() default "";

  /**
   * The policy for throttling method invocations when the limit has been reached.
   * <p>The default behavior is to block further concurrent invocations once the
   * specified limit has been reached: {@link ThrottlePolicy#BLOCK}.
   * <p>Switch this policy to {@code REJECT} for rejecting further invocations instead,
   * throwing {@link infra.resilience.InvocationRejectedException}
   * (which extends the common {@link java.util.concurrent.RejectedExecutionException})
   * on any further concurrent invocation attempts: {@link ThrottlePolicy#REJECT}.
   *
   * @since 5.0
   */
  ThrottlePolicy policy() default ThrottlePolicy.BLOCK;

  /**
   * Policy to apply for throttling method invocations when the limit has been reached.
   *
   * @since 5.0
   */
  enum ThrottlePolicy {

    /**
     * The default: block until we can invoke the method within the configured limit.
     */
    BLOCK,

    /**
     * Alternative: reject further method invocations once the limit has been reached.
     *
     * @see infra.resilience.InvocationRejectedException
     */
    REJECT
  }

}
