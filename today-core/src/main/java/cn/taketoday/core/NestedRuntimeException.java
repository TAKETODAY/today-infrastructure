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

package cn.taketoday.core;

import java.io.Serial;

import cn.taketoday.lang.Nullable;
import cn.taketoday.util.ExceptionUtils;

/**
 * Handy class for wrapping runtime {@code Exceptions} with a root cause.
 * This class is {@code abstract} to force the programmer to extend the class.
 *
 * <p>The similarity between this class and the {@link NestedCheckedException}
 * class is unavoidable, as Java forces these two classes to have different
 * superclasses (ah, the inflexibility of concrete inheritance!).
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @author TODAY 2021/2/2 11:12
 * @see #getMessage
 * @see NestedCheckedException
 * @since 3.0
 */
public abstract class NestedRuntimeException extends RuntimeException {
  @Serial
  private static final long serialVersionUID = 1L;

  static {
    // Eagerly load the ExceptionUtils class to avoid classloader deadlock
    ExceptionUtils.class.getName();
  }

  public NestedRuntimeException() { }

  /**
   * Construct a {@code NestedRuntimeException} with the specified detail message.
   *
   * @param msg the detail message
   */
  public NestedRuntimeException(@Nullable String msg) {
    super(msg);
  }

  /**
   * Construct a {@code NestedRuntimeException} with the specified nested exception.
   *
   * @param cause the nested exception
   */
  public NestedRuntimeException(@Nullable Throwable cause) {
    super(cause);
  }

  /**
   * Construct a {@code NestedRuntimeException} with the specified detail message
   * and nested exception.
   *
   * @param msg the detail message
   * @param cause the nested exception
   */
  public NestedRuntimeException(@Nullable String msg, @Nullable Throwable cause) {
    super(msg, cause);
  }

  /**
   * Return the detail message, including the message from the nested exception
   * if there is one.
   *
   * @since 4.0
   */
  public String getNestedMessage() {
    return ExceptionUtils.buildMessage(getMessage(), getCause());
  }

  /**
   * Retrieve the innermost cause of this exception, if any.
   *
   * @return the innermost exception, or {@code null} if none
   */
  @Nullable
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
  public Throwable getMostSpecificCause() {
    Throwable rootCause = getRootCause();
    return (rootCause != null ? rootCause : this);
  }

  /**
   * Check whether this exception contains an exception of the given type:
   * either it is of the given class itself or it contains a nested cause
   * of the given type.
   *
   * @param exType the exception type to look for
   * @return whether there is a nested exception of the specified type
   */
  public boolean contains(@Nullable Class<?> exType) {
    if (exType == null) {
      return false;
    }
    if (exType.isInstance(this)) {
      return true;
    }
    Throwable cause = getCause();
    if (cause == this) {
      return false;
    }
    if (cause instanceof NestedRuntimeException) {
      return ((NestedRuntimeException) cause).contains(exType);
    }
    else {
      while (cause != null) {
        if (exType.isInstance(cause)) {
          return true;
        }
        if (cause.getCause() == cause) {
          break;
        }
        cause = cause.getCause();
      }
      return false;
    }
  }

}
