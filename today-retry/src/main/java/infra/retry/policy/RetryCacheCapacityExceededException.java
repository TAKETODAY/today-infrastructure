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
package infra.retry.policy;

import infra.retry.RetryException;

/**
 * Exception that indicates that a cache limit was exceeded. This is often a sign of badly
 * or inconsistently implemented hashCode, equals in failed items. Items can then fail
 * repeatedly and appear different to the cache, so they get added over and over again
 * until a limit is reached and this exception is thrown. Consult the documentation of the
 * {@link RetryContextCache} in use to determine how to increase the limit if appropriate.
 *
 * @author Dave Syer
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
public class RetryCacheCapacityExceededException extends RetryException {

  /**
   * Constructs a new instance with a message.
   *
   * @param message the message sent when creating the exception
   */
  public RetryCacheCapacityExceededException(String message) {
    super(message);
  }

  /**
   * Constructs a new instance with a message and nested exception.
   *
   * @param msg the exception message.
   * @param nested the nested exception
   */
  public RetryCacheCapacityExceededException(String msg, Throwable nested) {
    super(msg, nested);
  }

}
