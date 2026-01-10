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

/**
 * Exception thrown when a transaction can't be created using an underlying
 * transaction API such as JTA.
 *
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 2019-11-09 15:48
 */
public class CannotCreateTransactionException extends TransactionException {

  /**
   * Constructor for CannotCreateTransactionException.
   *
   * @param msg the detail message
   */
  public CannotCreateTransactionException(String msg) {
    super(msg);
  }

  /**
   * Constructor for CannotCreateTransactionException.
   *
   * @param msg the detail message
   * @param cause the root cause from the transaction API in use
   */
  public CannotCreateTransactionException(String msg, Throwable cause) {
    super(msg, cause);
  }

}
