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

package infra.dao;

import org.jspecify.annotations.Nullable;

/**
 * Exception thrown on various data access concurrency failures.
 *
 * <p>This exception provides subclasses for specific types of failure,
 * in particular optimistic locking versus pessimistic locking.
 *
 * @author Thomas Risberg
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see OptimisticLockingFailureException
 * @see PessimisticLockingFailureException
 * @see CannotAcquireLockException
 * @see DeadlockLoserDataAccessException
 * @since 4.0
 */
public class ConcurrencyFailureException extends TransientDataAccessException {

  /**
   * Constructor for ConcurrencyFailureException.
   *
   * @param msg the detail message
   */
  public ConcurrencyFailureException(String msg) {
    super(msg);
  }

  /**
   * Constructor for ConcurrencyFailureException.
   *
   * @param msg the detail message
   * @param cause the root cause from the data access API in use
   */
  public ConcurrencyFailureException(String msg, @Nullable Throwable cause) {
    super(msg, cause);
  }

}
