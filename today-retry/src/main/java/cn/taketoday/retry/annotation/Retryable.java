/*
 * Copyright 2017 - 2023 the original author or authors.
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

package cn.taketoday.retry.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation for a method invocation that is retryable.
 *
 * @author Dave Syer
 * @author Artem Bilan
 * @author Gary Russell
 * @author Maksim Kita
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.METHOD, ElementType.TYPE })
public @interface Retryable {

  /**
   * Name of method in this class to use for recover. Method had to be marked with
   * {@link Recover} annotation.
   *
   * @return the name of recover method
   */
  String recover() default "";

  /**
   * Retry interceptor bean name to be applied for retryable method. Is mutually
   * exclusive with other attributes.
   *
   * @return the retry interceptor bean name
   */
  String interceptor() default "";

  /**
   * Exception types that are retryable. Defaults to empty (and if exclude is also empty
   * all exceptions are retried).
   *
   * @return exception types to retry
   */
  Class<? extends Throwable>[] value() default {};

  /**
   * Exception types that are retryable. Defaults to empty (and, if noRetryFor is also
   * empty, all exceptions are retried).
   *
   * @return exception types to retry
   */
  Class<? extends Throwable>[] retryFor() default {};

  /**
   * Exception types that are not retryable. Defaults to empty (and, if retryFor is also
   * empty, all exceptions are retried). If retryFor is empty but noRetryFor is not, all
   * other exceptions are retried
   *
   * @return exception types not to retry
   */
  Class<? extends Throwable>[] noRetryFor() default {};

  /**
   * Exception types that are not recoverable; these exceptions are thrown to the caller
   * without calling any recoverer (immediately if also in {@link #noRetryFor()}).
   * Defaults to empty.
   *
   * @return exception types not to retry
   */
  Class<? extends Throwable>[] notRecoverable() default {};

  /**
   * A unique label for statistics reporting. If not provided the caller may choose to
   * ignore it, or provide a default.
   *
   * @return the label for the statistics
   */
  String label() default "";

  /**
   * Flag to say that the retry is stateful: i.e. exceptions are re-thrown, but the
   * retry policy is applied with the same policy to subsequent invocations with the
   * same arguments. If false then retryable exceptions are not re-thrown.
   *
   * @return true if retry is stateful, default false
   */
  boolean stateful() default false;

  /**
   * @return the maximum number of attempts (including the first failure), defaults to 3
   */
  int maxAttempts() default 3;

  /**
   * @return an expression evaluated to the maximum number of attempts (including the
   * first failure), defaults to 3 Overrides {@link #maxAttempts()}. Use {@code #{...}}
   * for one-time evaluation during initialization, omit the delimiters for evaluation
   * at runtime.
   */
  String maxAttemptsExpression() default "";

  /**
   * Specify the backoff properties for retrying this operation. The default is a simple
   * {@link Backoff} specification with no properties - see its documentation for
   * defaults.
   *
   * @return a backoff specification
   */
  Backoff backoff() default @Backoff();

  /**
   * Specify an expression to be evaluated after the
   * {@code SimpleRetryPolicy.canRetry()} returns true - can be used to conditionally
   * suppress the retry. Only invoked after an exception is thrown. The root object for
   * the evaluation is the last {@code Throwable}. Other beans in the context can be
   * referenced. For example: <pre class=code>
   *  {@code "message.contains('you can retry this')"}.
   * </pre> and <pre class=code>
   *  {@code "@someBean.shouldRetry(#root)"}.
   * </pre>
   *
   * @return the expression.
   */
  String exceptionExpression() default "";

  /**
   * Bean names of retry listeners to use instead of default ones defined in Spring
   * context. If this attribute is set to an empty string {@code ""}, it will
   * effectively exclude all retry listeners, including with the default listener beans,
   * from being used.
   *
   * @return retry listeners bean names
   */
  String[] listeners() default {};

}
