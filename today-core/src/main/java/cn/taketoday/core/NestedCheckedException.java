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

import cn.taketoday.util.ExceptionUtils;

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
 * @author TODAY 2021/2/2 11:13
 * @see #getMessage
 * @see NestedRuntimeException
 * @since 3.0
 */
public abstract class NestedCheckedException extends Exception {
  @Serial
  private static final long serialVersionUID = 1L;

  static {
    // Eagerly load the NestedExceptionUtils class to avoid classloader deadlock
    // issues on OSGi when calling getMessage(). Reported by Don Brown; SPR-5607.
    ExceptionUtils.class.getName();
  }

  public NestedCheckedException() { }

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
  public String getNestedMessage() {
    return ExceptionUtils.buildMessage(super.getMessage(), getCause());
  }

  /**
   * Retrieve the innermost cause of this exception, if any.
   *
   * @return the innermost exception, or {@code null} if none
   */
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
  public boolean contains(Class<?> exType) {
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
    if (cause instanceof NestedCheckedException) {
      return ((NestedCheckedException) cause).contains(exType);
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
