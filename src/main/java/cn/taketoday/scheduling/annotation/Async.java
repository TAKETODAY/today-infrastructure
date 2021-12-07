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

package cn.taketoday.scheduling.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation that marks a method as a candidate for <i>asynchronous</i> execution.
 * Can also be used at the type level, in which case all of the type's methods are
 * considered as asynchronous. Note, however, that {@code @Async} is not supported
 * on methods declared within a
 * {@link cn.taketoday.lang.Configuration @Configuration} class.
 *
 * <p>In terms of target method signatures, any parameter types are supported.
 * However, the return type is constrained to either {@code void} or
 * {@link java.util.concurrent.Future}. In the latter case, you may declare the
 * more specific {@link cn.taketoday.util.concurrent.ListenableFuture} or
 * {@link java.util.concurrent.CompletableFuture} types which allow for richer
 * interaction with the asynchronous task and for immediate composition with
 * further processing steps.
 *
 * <p>A {@code Future} handle returned from the proxy will be an actual asynchronous
 * {@code Future} that can be used to track the result of the asynchronous method
 * execution. However, since the target method needs to implement the same signature,
 * it will have to return a temporary {@code Future} handle that just passes a value
 * through: e.g. {@link AsyncResult}, EJB 3.1's {@link jakarta.ejb.AsyncResult},
 * or {@link java.util.concurrent.CompletableFuture#completedFuture(Object)}.
 *
 * @author Juergen Hoeller
 * @author Chris Beams
 * @see AnnotationAsyncExecutionInterceptor
 * @see AsyncAnnotationAdvisor
 * @since 4.0
 */
@Target({ ElementType.TYPE, ElementType.METHOD })
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Async {

  /**
   * A qualifier value for the specified asynchronous operation(s).
   * <p>May be used to determine the target executor to be used when executing
   * the asynchronous operation(s), matching the qualifier value (or the bean
   * name) of a specific {@link java.util.concurrent.Executor Executor} or
   * {@link cn.taketoday.core.task.TaskExecutor TaskExecutor}
   * bean definition.
   * <p>When specified on a class-level {@code @Async} annotation, indicates that the
   * given executor should be used for all methods within the class. Method-level use
   * of {@code Async#value} always overrides any value set at the class level.
   */
  String value() default "";

}
