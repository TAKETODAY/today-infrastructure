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

package cn.taketoday.transaction;

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
