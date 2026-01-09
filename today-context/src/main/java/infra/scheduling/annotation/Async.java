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

package infra.scheduling.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import infra.aot.hint.annotation.Reflective;
import infra.context.annotation.Configuration;
import infra.core.task.TaskExecutor;

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
 * more specific {@link java.util.concurrent.CompletableFuture} type which allows
 * for richer interaction with the asynchronous task and for immediate composition
 * with further processing steps.
 *
 * <p>A {@code Future} handle returned from the proxy will be an actual asynchronous
 * {@code (Completable)Future} that can be used to track the result of the
 * asynchronous method execution. However, since the target method needs to implement
 * the same signature, it will have to return a temporary {@code Future} handle that
 * just passes a value after computation in the execution thread: typically through
 * {@link java.util.concurrent.CompletableFuture#completedFuture(Object)}. The
 * provided value will be exposed to the caller through the actual asynchronous
 * {@code Future} handle at runtime.
 *
 * @author Juergen Hoeller
 * @author Chris Beams
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
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
   * {@link TaskExecutor TaskExecutor}
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
