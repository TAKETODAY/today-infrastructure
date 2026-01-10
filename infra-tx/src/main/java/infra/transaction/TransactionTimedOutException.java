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
 * Exception to be thrown when a transaction has timed out.
 *
 * <p>
 * Thrown by local transaction strategies if the deadline for a
 * transaction has been reached when an operation is attempted, according to the
 * timeout specified for the given transaction.
 *
 * <p>
 * Beyond such checks before each transactional operation, local
 * transaction strategies will also pass appropriate timeout values to resource
 * operations (for example to JDBC Statements, letting the JDBC driver respect
 * the timeout). Such operations will usually throw native resource exceptions
 * (for example, JDBC SQLExceptions) if their operation timeout has been
 * exceeded, to be converted to DataAccessException in the respective
 * DAO (which might use JdbcTemplate, for example).
 *
 * <p>
 * In a JTA environment, it is up to the JTA transaction coordinator to apply
 * transaction timeouts. Usually, the corresponding JTA-aware connection pool
 * will perform timeout checks and throw corresponding native resource
 * exceptions (for example, JDBC SQLExceptions).
 *
 * @author Juergen Hoeller
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see java.sql.Statement#setQueryTimeout
 * @see java.sql.SQLException
 * @since 2019-11-09 17:07
 */
public class TransactionTimedOutException extends TransactionException {

  public TransactionTimedOutException(String msg) {
    super(msg);
  }

  public TransactionTimedOutException(String msg, Throwable cause) {
    super(msg, cause);
  }

}
