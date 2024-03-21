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

package cn.taketoday.scheduling.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import cn.taketoday.aot.hint.annotation.Reflective;
import cn.taketoday.context.annotation.Configuration;
import cn.taketoday.util.concurrent.Future;

/**
 * Annotation that marks a method as a candidate for <i>asynchronous</i> execution.
 *
 * <p>Can also be used at the type level, in which case all of the type's methods are
 * considered as asynchronous. Note, however, that {@code @Async} is not supported
 * on methods declared within a {@link Configuration @Configuration} class.
 *
 * <p>In terms of target method signatures, any parameter types are supported.
 * However, the return type is constrained to either {@code void} or
 * {@link java.util.concurrent.Future}. In the latter case, you may declare the
 * more specific {@link Future} or
 * {@link java.util.concurrent.CompletableFuture} types which allow for richer
 * interaction with the asynchronous task and for immediate composition with
 * further processing steps.
 *
 * <p>A {@code Future} handle returned from the proxy will be an actual asynchronous
 * {@code Future} that can be used to track the result of the asynchronous method
 * execution. However, since the target method needs to implement the same signature,
 * it will have to return a temporary {@code Future} handle that just passes a value
 * through: for example {@link AsyncResult}, EJB 3.1's {@link jakarta.ejb.AsyncResult},
 * or {@link java.util.concurrent.CompletableFuture#completedFuture(Object)}.
 *
 * @author Juergen Hoeller
 * @author Chris Beams
 * @see AnnotationAsyncExecutionInterceptor
 * @see AsyncAnnotationAdvisor
 * @since 4.0
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.TYPE, ElementType.METHOD })
@Reflective
public @interface Async {

  /**
   * A qualifier value for the specified asynchronous operation(s).
   * <p>May be used to determine the target executor to be used when executing
   * the asynchronous operation(s), matching the qualifier value (or the bean
   * name) of a specific {@link java.util.concurrent.Executor Executor} or
   * {@link cn.taketoday.core.task.TaskExecutor TaskExecutor}
   * bean definition.
   * <p>When specified in a class-level {@code @Async} annotation, indicates that the
   * given executor should be used for all methods within the class. Method-level use
   * of {@code Async#value} always overrides any qualifier value configured at
   * the class level.
   * <p>The qualifier value will be resolved dynamically if supplied as a EL
   * expression (for example, {@code "#{environment['myExecutor']}"}) or a
   * property placeholder (for example, {@code "${my.app.myExecutor}"}).
   */
  String value() default "";

}
