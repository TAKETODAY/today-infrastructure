/*
 * Copyright 2017 - 2025 the original author or authors.
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

package infra.util;

import org.jspecify.annotations.Nullable;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.UndeclaredThrowableException;

import infra.core.NestedException;
import infra.util.function.ThrowingRunnable;
import infra.util.function.ThrowingSupplier;

/**
 * Utility methods for Exception
 *
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 2018-11-13 21:25
 */
public abstract class ExceptionUtils {

  /**
   * Unwrap first level {@link InvocationTargetException} and
   * {@link UndeclaredThrowableException}
   *
   * @param ex target {@link Throwable}
   * @return unwrapped {@link Throwable}
   * @see InvocationTargetException
   * @see UndeclaredThrowableException
   */
  public static Throwable unwrapIfNecessary(Throwable ex) {
    Throwable unwrapped = ex;
    while (true) {
      if (unwrapped instanceof InvocationTargetException
              || unwrapped instanceof UndeclaredThrowableException) {
        unwrapped = unwrapped.getCause();
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
  @Nullable
  public static String getNestedMessage(@Nullable Throwable cause, @Nullable String message) {
    if (cause == null) {
      return message;
    }
    StringBuilder sb = new StringBuilder(64);
    if (message != null) {
      sb.append(message).append("; ");
    }
    sb.append("nested exception is ").append(cause);
    return sb.toString();
  }

  /**
   * Retrieve the innermost cause of the given exception, if any.
   *
   * @param original the original exception to introspect
   * @return the innermost exception, or {@code null} if none
   * @since 3.0
   */
  @Nullable
  public static Throwable getRootCause(@Nullable Throwable original) {
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
    return rootCause != null ? rootCause : original;
  }

  /**
   * Retrieve the most specific cause of the given exception,
   *
   * @param original the original exception to introspect
   * @return the most specific cause, Maybe {@code null} if not found
   * @since 4.0
   */
  @Nullable
  @SuppressWarnings("unchecked")
  public static <T extends Throwable> T getMostSpecificCause(@Nullable Throwable original, @Nullable Class<T> exType) {
    if (exType == null || original == null) {
      return null;
    }
    if (exType.isInstance(original)) {
      return (T) original;
    }
    Throwable cause = original.getCause();
    if (cause == original) {
      return null;
    }

    while (cause != null) {
      if (exType.isInstance(cause)) {
        return (T) cause;
      }
      if (cause.getCause() == cause) {
        break;
      }
      cause = cause.getCause();
    }
    return null;
  }

  /**
   * Check whether this exception contains an exception of the given type:
   * either it is of the given class itself or it contains a nested cause
   * of the given type.
   *
   * @param exType the exception type to look for
   * @return whether there is a nested exception of the specified type
   */
  public static boolean contains(Throwable ex, @Nullable Class<?> exType) {
    if (exType == null) {
      return false;
    }
    if (exType.isInstance(ex)) {
      return true;
    }
    Throwable cause = ex.getCause();
    if (cause == ex) {
      return false;
    }
    if (cause instanceof NestedException nested) {
      return nested.contains(exType);
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
  public static RuntimeException sneakyThrow(@Nullable Throwable t) {
    if (t == null)
      throw new NullPointerException("t");
    return sneakyThrow0(t);
  }

  @SuppressWarnings("unchecked")
  private static <T extends Throwable> T sneakyThrow0(Throwable t) throws T {
    throw (T) t;
  }

  /**
   * @since 4.0
   */
  public static void sneakyThrow(ThrowingRunnable action) {
    try {
      action.run();
    }
    catch (Throwable e) {
      throw sneakyThrow(e);
    }
  }

  /**
   * @since 4.0
   */
  public static <T> T sneakyThrow(ThrowingSupplier<T> action) {
    try {
      return action.getWithException();
    }
    catch (Throwable e) {
      throw sneakyThrow(e);
    }
  }

  /**
   * Gets the stack trace from a Throwable as a String.
   *
   * @param cause the {@link Throwable} to be examined
   * @return the stack trace as generated by {@link
   * Throwable#printStackTrace(java.io.PrintWriter)} method.
   */
  public static String stackTraceToString(Throwable cause) {
    ByteArrayOutputStream out = new ByteArrayOutputStream();
    PrintStream pout = new PrintStream(out);
    cause.printStackTrace(pout);
    pout.flush();
    try {
      return out.toString();
    }
    finally {
      try {
        out.close();
      }
      catch (IOException ignore) {
        // ignore as should never happen
      }
    }
  }

}
