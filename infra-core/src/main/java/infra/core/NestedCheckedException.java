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

package infra.core;

import org.jspecify.annotations.Nullable;

import java.io.Serial;

import infra.util.ExceptionUtils;

/**
 * Handy class for wrapping checked {@code Exceptions} with a root cause.
 * This class is {@code abstract} to force the programmer to extend the class.
 *
 * <p>The similarity between this class and the {@link NestedRuntimeException}
 * class is unavoidable, as Java forces these two classes to have different
 * superclasses (ah, the inflexibility of concrete inheritance!).
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see #getMessage
 * @see NestedRuntimeException
 * @since 3.0 2021/2/2 11:13
 */
public abstract class NestedCheckedException extends Exception implements NestedException {

  @Serial
  private static final long serialVersionUID = 1L;

  static {
    // Eagerly load the NestedExceptionUtils class to avoid classloader deadlock
    // issues on OSGi when calling getMessage(). Reported by Don Brown; SPR-5607.
    ExceptionUtils.class.getName();
  }

  public NestedCheckedException() {
  }

  /**
   * Construct a {@code NestedCheckedException} with the specified detail message.
   *
   * @param msg the detail message
   */
  public NestedCheckedException(String msg) {
    super(msg);
  }

  /**
   * Construct a {@code NestedCheckedException} with the specified nested exception.
   *
   * @param cause the nested exception
   */
  public NestedCheckedException(Throwable cause) {
    super(cause);
  }

  /**
   * Construct a {@code NestedCheckedException} with the specified detail message
   * and nested exception.
   *
   * @param msg the detail message
   * @param cause the nested exception
   */
  public NestedCheckedException(String msg, Throwable cause) {
    super(msg, cause);
  }

  /**
   * Return the detail message, including the message from the nested exception
   * if there is one.
   *
   * @since 4.0
   */
  @Nullable
  @Override
  public String getNestedMessage() {
    return ExceptionUtils.getNestedMessage(getCause(), getMessage());
  }

  /**
   * Retrieve the innermost cause of this exception, if any.
   *
   * @return the innermost exception, or {@code null} if none
   */
  @Nullable
  @Override
  public Throwable getRootCause() {
    return ExceptionUtils.getRootCause(this);
  }

  /**
   * Retrieve the most specific cause of this exception, that is,
   * either the innermost cause (root cause) or this exception itself.
   * <p>Differs from {@link #getRootCause()} in that it falls back
   * to the present exception if there is no root cause.
   *
   * @return the most specific cause (never {@code null})
   */
  @Override
  public Throwable getMostSpecificCause() {
    return ExceptionUtils.getMostSpecificCause(this);
  }

  /**
   * Check whether this exception contains an exception of the given type:
   * either it is of the given class itself or it contains a nested cause
   * of the given type.
   *
   * @param exType the exception type to look for
   * @return whether there is a nested exception of the specified type
   */
  @Override
  public boolean contains(@Nullable Class<?> exType) {
    return ExceptionUtils.contains(this, exType);
  }

}
