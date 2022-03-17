/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2022 All Rights Reserved.
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

package cn.taketoday.test.util;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * {@code ExceptionCollector} is a test utility for executing code blocks,
 * collecting exceptions, and generating a single {@link AssertionError}
 * containing any exceptions encountered as {@linkplain Throwable#getSuppressed()
 * suppressed exceptions}.
 *
 * <p>This utility is intended to support <em>soft assertion</em> use cases
 * similar to the {@code SoftAssertions} support in AssertJ and the
 * {@code assertAll()} support in JUnit Jupiter.
 *
 * @author Sam Brannen
 * @since 4.0
 */
public class ExceptionCollector {

  private final List<Throwable> exceptions = new ArrayList<>();

  /**
   * Execute the supplied {@link Executable} and track any exception thrown.
   *
   * @param executable the {@code Executable} to execute
   * @see #getExceptions()
   * @see #assertEmpty()
   */
  public void execute(Executable executable) {
    try {
      executable.execute();
    }
    catch (Throwable ex) {
      this.exceptions.add(ex);
    }
  }

  /**
   * Get the list of exceptions encountered in {@link #execute(Executable)}.
   *
   * @return an unmodifiable copy of the list of exceptions, potentially empty
   * @see #assertEmpty()
   */
  public List<Throwable> getExceptions() {
    return Collections.unmodifiableList(this.exceptions);
  }

  /**
   * Assert that this {@code ExceptionCollector} does not contain any
   * {@linkplain #getExceptions() exceptions}.
   * <p>If this collector is empty, this method is effectively a no-op.
   * <p>If this collector contains a single {@link Error} or {@link Exception},
   * this method rethrows the error or exception.
   * <p>If this collector contains a single {@link Throwable}, this method throws
   * an {@link AssertionError} with the error message of the {@code Throwable}
   * and with the {@code Throwable} as the {@linkplain Throwable#getCause() cause}.
   * <p>If this collector contains multiple exceptions, this method throws an
   * {@code AssertionError} whose message is "Multiple Exceptions (#):"
   * followed by a new line with the error message of each exception separated
   * by a new line, with {@code #} replaced with the number of exceptions present.
   * In addition, each exception will be added to the {@code AssertionError} as
   * a {@link Throwable#addSuppressed(Throwable) suppressed exception}.
   *
   * @see #execute(Executable)
   * @see #getExceptions()
   */
  public void assertEmpty() throws Exception {
    if (this.exceptions.isEmpty()) {
      return;
    }

    if (this.exceptions.size() == 1) {
      Throwable exception = this.exceptions.get(0);
      if (exception instanceof Error error) {
        throw error;
      }
      if (exception instanceof Exception ex) {
        throw ex;
      }
      AssertionError assertionError = new AssertionError(exception.getMessage(), exception);
      throw assertionError;
    }

    StringBuilder message = new StringBuilder();
    message.append("Multiple Exceptions (").append(this.exceptions.size()).append("):");
    for (Throwable exception : this.exceptions) {
      message.append('\n');
      message.append(exception.getMessage());
    }
    AssertionError assertionError = new AssertionError(message);
    this.exceptions.forEach(assertionError::addSuppressed);
    throw assertionError;
  }

  /**
   * {@code Executable} is a functional interface that can be used to implement
   * any generic block of code that potentially throws a {@link Throwable}.
   *
   * <p>The {@code Executable} interface is similar to {@link Runnable},
   * except that an {@code Executable} can throw any kind of exception.
   */
  @FunctionalInterface
  public interface Executable {

    void execute() throws Throwable;

  }

}
