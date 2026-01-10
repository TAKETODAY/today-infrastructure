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

package infra.transaction;

import org.jspecify.annotations.Nullable;

import infra.lang.Assert;

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

  public TransactionSystemException(@Nullable String msg) {
    super(msg);
  }

  public TransactionSystemException(@Nullable String msg, @Nullable Throwable cause) {
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
  @Nullable
  public Throwable getOriginalException() {
    return (this.applicationException != null ? this.applicationException : getCause());
  }
}
