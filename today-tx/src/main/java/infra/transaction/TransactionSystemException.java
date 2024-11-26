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
package infra.transaction;

import infra.lang.Assert;
import infra.lang.Nullable;

/**
 * Exception thrown when a general transaction system error is encountered, like
 * on commit or rollback.
 *
 * @author Juergen Hoeller
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 */
public class TransactionSystemException extends TransactionException {

  @Nullable
  private Throwable applicationException;

  public TransactionSystemException(String msg) {
    super(msg);
  }

  public TransactionSystemException(String msg, Throwable cause) {
    super(msg, cause);
  }

  /**
   * Set an application exception that was thrown before this transaction
   * exception, preserving the original exception despite the overriding
   * TransactionSystemException.
   *
   * @param ex the application exception
   * @throws IllegalStateException if this TransactionSystemException already holds an application
   * exception
   */
  public void initApplicationException(Throwable ex) {
    Assert.notNull(ex, "Application exception is required");
    if (this.applicationException != null) {
      throw new IllegalStateException("Already holding an application exception: " + this.applicationException);
    }
    this.applicationException = ex;
  }

  /**
   * Return the application exception that was thrown before this transaction
   * exception, if any.
   *
   * @return the application exception, or {@code null} if none set
   */
  @Nullable
  public final Throwable getApplicationException() {
    return this.applicationException;
  }

  /**
   * Return the exception that was the first to be thrown within the failed
   * transaction: i.e. the application exception, if any, or the
   * TransactionSystemException's own cause.
   *
   * @return the original exception, or {@code null} if there was none
   */
  public Throwable getOriginalException() {
    return (this.applicationException != null ? this.applicationException : getCause());
  }
}
