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
package cn.taketoday.util;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.UndeclaredThrowableException;

/**
 * @author TODAY <br>
 * 2018-11-13 21:25
 */
public abstract class ExceptionUtils {

  /**
   * Unwrap
   *
   * @param ex target {@link Throwable}
   * @return unwrapped {@link Throwable}
   */
  public static Throwable unwrapThrowable(Throwable ex) {
    Throwable unwrapped = ex;
    while (true) {
      if (unwrapped instanceof InvocationTargetException) {
        unwrapped = ((InvocationTargetException) unwrapped).getTargetException();
      }
      else if (unwrapped instanceof UndeclaredThrowableException) {
        unwrapped = ((UndeclaredThrowableException) unwrapped).getUndeclaredThrowable();
      }
      else {
        return unwrapped;
      }
    }
  }

  /**
   * Build a message for the given base message and root cause.
   *
   * @param message the base message
   * @param cause the root cause
   * @return the full exception message
   * @since 3.0
   */
  public static String buildMessage(String message, Throwable cause) {
    if (cause == null) {
      return message;
    }
    StringBuilder sb = new StringBuilder(64);
    if (message != null) {
      sb.append(message).append("; ");
    }
    sb.append("Nested exception is ");
    String nested = cause.getMessage();
    if (nested == null) {
      sb.append(cause);
    }
    else {
      sb.append(buildMessage(cause.getMessage(), cause.getCause()));
    }
    return sb.toString();
  }

  /**
   * Retrieve the innermost cause of the given exception, if any.
   *
   * @param original the original exception to introspect
   * @return the innermost exception, or {@code null} if none
   * @since 3.0
   */
  public static Throwable getRootCause(Throwable original) {
    if (original == null) {
      return null;
    }
    Throwable rootCause = null;
    Throwable cause = original.getCause();
    while (cause != null && cause != rootCause) {
      rootCause = cause;
      cause = cause.getCause();
    }
    return rootCause;
  }

  /**
   * Retrieve the most specific cause of the given exception, that is,
   * either the innermost cause (root cause) or the exception itself.
   * <p>Differs from {@link #getRootCause} in that it falls back
   * to the original exception if there is no root cause.
   *
   * @param original the original exception to introspect
   * @return the most specific cause (never {@code null})
   * @since 3.0
   */
  public static Throwable getMostSpecificCause(Throwable original) {
    Throwable rootCause = getRootCause(original);
    return (rootCause != null ? rootCause : original);
  }

  /**
   * Throws any throwable 'sneakily' - you don't need to catch it, nor declare that you throw it onwards.
   * The exception is still thrown - javac will just stop whining about it.
   * <p>
   * Example usage:
   * <pre>
   *   public void run() {
   *     throw sneakyThrow(new IOException("You don't need to catch me!"));
   *   }
   * </pre>
   * <p>
   * NB: The exception is not wrapped, ignored, swallowed, or redefined. The JVM actually does not know or care
   * about the concept of a 'checked exception'. All this method does is hide the act of throwing a checked exception
   * from the java compiler.
   * <p>
   * Note that this method has a return type of {@code RuntimeException}; it is advised you always call this
   * method as argument to the {@code throw} statement to avoid compiler errors regarding no return
   * statement and similar problems. This method won't of course return an actual {@code RuntimeException} -
   * it never returns, it always throws the provided exception.
   *
   * @param t The throwable to throw without requiring you to catch its type.
   * @return A dummy RuntimeException; this method never returns normally, it <em>always</em> throws an exception!
   * @since 4.0
   */
  public static RuntimeException sneakyThrow(Throwable t) {
    if (t == null)
      throw new NullPointerException("t");
    return sneakyThrow0(t);
  }

  @SuppressWarnings("unchecked")
  private static <T extends Throwable> T sneakyThrow0(Throwable t) throws T {
    throw (T) t;
  }

}
