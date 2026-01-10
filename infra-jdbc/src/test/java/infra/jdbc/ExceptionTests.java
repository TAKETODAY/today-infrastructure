/*
 * Copyright 2017 - 2026 the TODAY authors.
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

package infra.jdbc;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.sql.SQLException;
import java.sql.SQLWarning;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 5.0 2025/11/7 15:46
 */
class ExceptionTests {

  @Nested
  class JdbcUpdateAffectedIncorrectNumberOfRowsExceptionTests {
    @Test
    void shouldCreateExceptionWithValidParameters() {
      String sql = "UPDATE users SET name = 'John' WHERE id = 1";
      int expected = 1;
      int actual = 0;

      JdbcUpdateAffectedIncorrectNumberOfRowsException exception =
              new JdbcUpdateAffectedIncorrectNumberOfRowsException(sql, expected, actual);

      assertThat(exception).isNotNull();
      assertThat(exception.getMessage()).contains(sql).contains(String.valueOf(expected)).contains(String.valueOf(actual));
    }

    @Test
    void shouldReturnExpectedRowsAffected() {
      String sql = "DELETE FROM users WHERE age < 18";
      int expected = 5;
      int actual = 3;

      JdbcUpdateAffectedIncorrectNumberOfRowsException exception =
              new JdbcUpdateAffectedIncorrectNumberOfRowsException(sql, expected, actual);

      assertThat(exception.getExpectedRowsAffected()).isEqualTo(expected);
    }

    @Test
    void shouldReturnActualRowsAffected() {
      String sql = "INSERT INTO users (name, age) VALUES ('John', 25)";
      int expected = 1;
      int actual = 0;

      JdbcUpdateAffectedIncorrectNumberOfRowsException exception =
              new JdbcUpdateAffectedIncorrectNumberOfRowsException(sql, expected, actual);

      assertThat(exception.getActualRowsAffected()).isEqualTo(actual);
    }

    @Test
    void shouldReturnWasDataUpdatedWhenActualGreaterThanZero() {
      String sql = "UPDATE users SET status = 'active'";
      int expected = 10;
      int actual = 5;

      JdbcUpdateAffectedIncorrectNumberOfRowsException exception =
              new JdbcUpdateAffectedIncorrectNumberOfRowsException(sql, expected, actual);

      assertThat(exception.wasDataUpdated()).isTrue();
    }

    @Test
    void shouldReturnWasDataUpdatedWhenActualIsZero() {
      String sql = "DELETE FROM users WHERE id = 999";
      int expected = 1;
      int actual = 0;

      JdbcUpdateAffectedIncorrectNumberOfRowsException exception =
              new JdbcUpdateAffectedIncorrectNumberOfRowsException(sql, expected, actual);

      assertThat(exception.wasDataUpdated()).isFalse();
    }

    @Test
    void shouldHandleNegativeRowsAffected() {
      String sql = "UPDATE users SET name = 'test'";
      int expected = -1;
      int actual = -2;

      JdbcUpdateAffectedIncorrectNumberOfRowsException exception =
              new JdbcUpdateAffectedIncorrectNumberOfRowsException(sql, expected, actual);

      assertThat(exception.getExpectedRowsAffected()).isEqualTo(expected);
      assertThat(exception.getActualRowsAffected()).isEqualTo(actual);
      assertThat(exception.wasDataUpdated()).isFalse();
    }

    @Test
    void shouldHandleLargeNumbers() {
      String sql = "INSERT INTO logs (message) VALUES ('test')";
      int expected = Integer.MAX_VALUE;
      int actual = Integer.MAX_VALUE - 1;

      JdbcUpdateAffectedIncorrectNumberOfRowsException exception =
              new JdbcUpdateAffectedIncorrectNumberOfRowsException(sql, expected, actual);

      assertThat(exception.getExpectedRowsAffected()).isEqualTo(expected);
      assertThat(exception.getActualRowsAffected()).isEqualTo(actual);
      assertThat(exception.wasDataUpdated()).isTrue();
    }

    @Test
    void shouldIncludeAllInformationInMessage() {
      String sql = "SELECT * FROM users";
      int expected = 1;
      int actual = 0;

      JdbcUpdateAffectedIncorrectNumberOfRowsException exception =
              new JdbcUpdateAffectedIncorrectNumberOfRowsException(sql, expected, actual);

      assertThat(exception.getMessage()).contains("SQL update").contains(sql).contains("affected").contains(String.valueOf(actual)).contains(String.valueOf(expected));
    }

  }

  @Nested
  class UncategorizedSQLExceptionTests {
    @Test
    void shouldCreateExceptionWithValidParameters() {
      String task = "Executing query";
      String sql = "SELECT * FROM users WHERE id = ?";
      SQLException sqlException = new SQLException("Test SQL exception", "42000", 1064);

      UncategorizedSQLException exception = new UncategorizedSQLException(task, sql, sqlException);

      assertThat(exception).isNotNull();
      assertThat(exception.getMessage()).contains(task).contains(sql).contains("42000").contains("1064");
      assertThat((Object) exception.getSQLException()).isEqualTo(sqlException);
      assertThat(exception.getSql()).isEqualTo(sql);
    }

    @Test
    void shouldReturnSQLException() {
      String task = "Update operation";
      String sql = "UPDATE users SET name = ?";
      SQLException sqlException = new SQLException("Update failed", "23000", 1452);

      UncategorizedSQLException exception = new UncategorizedSQLException(task, sql, sqlException);

      assertThat((Object) exception.getSQLException()).isEqualTo(sqlException);
      assertThat(exception.getSQLException().getMessage()).isEqualTo("Update failed");
      assertThat(exception.getSQLException().getSQLState()).isEqualTo("23000");
      assertThat(exception.getSQLException().getErrorCode()).isEqualTo(1452);
    }

    @Test
    void shouldReturnSql() {
      String task = "Delete operation";
      String sql = "DELETE FROM users WHERE age < ?";
      SQLException sqlException = new SQLException("Delete error");

      UncategorizedSQLException exception = new UncategorizedSQLException(task, sql, sqlException);

      assertThat(exception.getSql()).isEqualTo(sql);
    }

    @Test
    void shouldHandleExceptionWithNoSqlStateAndErrorCode() {
      String task = "Batch operation";
      String sql = "INSERT INTO test VALUES (?)";
      SQLException sqlException = new SQLException("Batch failed");

      UncategorizedSQLException exception = new UncategorizedSQLException(task, sql, sqlException);

      assertThat(exception.getMessage()).contains(task).contains(sql).contains("null").contains("0");
      assertThat((Object) exception.getSQLException()).isEqualTo(sqlException);
    }

    @Test
    void shouldPreserveCause() {
      String task = "Query execution";
      String sql = "SELECT * FROM nonexistent_table";
      SQLException sqlException = new SQLException("Table doesn't exist", "42S02", 1146);

      UncategorizedSQLException exception = new UncategorizedSQLException(task, sql, sqlException);

      assertThat(exception.getCause()).isEqualTo(sqlException);
      assertThat((Object) exception.getSQLException()).isSameAs(sqlException);
    }

  }

  @Nested
  class SQLWarningExceptionTests {
    @Test
    void shouldCreateExceptionWithValidParameters() {
      String message = "SQL Warning occurred during query execution";
      SQLWarning sqlWarning = new SQLWarning("Test warning", "01000", 100);

      SQLWarningException exception = new SQLWarningException(message, sqlWarning);

      assertThat(exception).isNotNull();
      assertThat(exception.getMessage()).isEqualTo(message);
      assertThat((Object) exception.getSQLWarning()).isEqualTo(sqlWarning);
    }

    @Test
    void shouldReturnSQLWarning() {
      String message = "Warning during database operation";
      SQLWarning sqlWarning = new SQLWarning("Data truncation", "01004", 101);

      SQLWarningException exception = new SQLWarningException(message, sqlWarning);

      assertThat((Object) exception.getSQLWarning()).isEqualTo(sqlWarning);
      assertThat(exception.getSQLWarning().getMessage()).isEqualTo("Data truncation");
      assertThat(exception.getSQLWarning().getSQLState()).isEqualTo("01004");
      assertThat(exception.getSQLWarning().getErrorCode()).isEqualTo(101);
    }

    @Test
    void shouldPreserveCause() {
      String message = "Operation completed with warnings";
      SQLWarning sqlWarning = new SQLWarning("Connection unstable", "01002", 200);

      SQLWarningException exception = new SQLWarningException(message, sqlWarning);

      assertThat(exception.getCause()).isEqualTo(sqlWarning);
      assertThat((Object) exception.getSQLWarning()).isSameAs(sqlWarning);
    }

    @Test
    void shouldHandleNullMessage() {
      SQLWarning sqlWarning = new SQLWarning("Warning message", "01000", 300);

      SQLWarningException exception = new SQLWarningException(null, sqlWarning);

      assertThat(exception).isNotNull();
      assertThat(exception.getMessage()).isNull();
      assertThat((Object) exception.getSQLWarning()).isEqualTo(sqlWarning);
    }

    @Test
    void shouldHandleMultipleWarnings() {
      String message = "Multiple warnings detected";
      SQLWarning sqlWarning = new SQLWarning("First warning");
      SQLWarning nextWarning = new SQLWarning("Second warning");
      sqlWarning.setNextWarning(nextWarning);

      SQLWarningException exception = new SQLWarningException(message, sqlWarning);

      assertThat((Object) exception.getSQLWarning()).isEqualTo(sqlWarning);
      assertThat((Object) exception.getSQLWarning().getNextWarning()).isEqualTo(nextWarning);
    }

  }

  @Nested
  class LobRetrievalFailureExceptionTests {
    @Test
    void shouldCreateExceptionWithMessageOnly() {
      String message = "Failed to retrieve LOB content";

      LobRetrievalFailureException exception = new LobRetrievalFailureException(message);

      assertThat(exception).isNotNull();
      assertThat(exception.getMessage()).isEqualTo(message);
      assertThat(exception.getCause()).isNull();
    }

    @Test
    void shouldCreateExceptionWithMessageAndIOException() {
      String message = "Error reading LOB stream";
      IOException ioException = new IOException("Stream closed unexpectedly");

      LobRetrievalFailureException exception = new LobRetrievalFailureException(message, ioException);

      assertThat(exception).isNotNull();
      assertThat(exception.getMessage()).isEqualTo(message);
      assertThat(exception.getCause()).isEqualTo(ioException);
      assertThat(exception.getCause().getMessage()).isEqualTo("Stream closed unexpectedly");
    }

    @Test
    void shouldHandleNullMessage() {
      IOException ioException = new IOException("IO error");

      LobRetrievalFailureException exception = new LobRetrievalFailureException(null, ioException);

      assertThat(exception).isNotNull();
      assertThat(exception.getMessage()).isNull();
      assertThat(exception.getCause()).isEqualTo(ioException);
    }

    @Test
    void shouldHandleNullIOException() {
      String message = "LOB retrieval failed";

      LobRetrievalFailureException exception = new LobRetrievalFailureException(message, null);

      assertThat(exception).isNotNull();
      assertThat(exception.getMessage()).isEqualTo(message);
      assertThat(exception.getCause()).isNull();
    }

    @Test
    void shouldHandleBothNullParameters() {
      LobRetrievalFailureException exception = new LobRetrievalFailureException(null, null);

      assertThat(exception).isNotNull();
      assertThat(exception.getMessage()).isNull();
      assertThat(exception.getCause()).isNull();
    }

  }

  @Nested
  class IncorrectResultSetColumnCountExceptionTests {
    @Test
    void shouldCreateExceptionWithExpectedAndActualCounts() {
      int expectedCount = 1;
      int actualCount = 3;

      IncorrectResultSetColumnCountException exception = new IncorrectResultSetColumnCountException(expectedCount, actualCount);

      assertThat(exception).isNotNull();
      assertThat(exception.getMessage()).contains("Incorrect column count").contains(String.valueOf(expectedCount)).contains(String.valueOf(actualCount));
      assertThat(exception.getExpectedCount()).isEqualTo(expectedCount);
      assertThat(exception.getActualCount()).isEqualTo(actualCount);
    }

    @Test
    void shouldCreateExceptionWithCustomMessage() {
      String message = "Custom error message for column count mismatch";
      int expectedCount = 2;
      int actualCount = 0;

      IncorrectResultSetColumnCountException exception = new IncorrectResultSetColumnCountException(message, expectedCount, actualCount);

      assertThat(exception).isNotNull();
      assertThat(exception.getMessage()).isEqualTo(message);
      assertThat(exception.getExpectedCount()).isEqualTo(expectedCount);
      assertThat(exception.getActualCount()).isEqualTo(actualCount);
    }

    @Test
    void shouldReturnExpectedCount() {
      int expectedCount = 5;
      int actualCount = 2;

      IncorrectResultSetColumnCountException exception = new IncorrectResultSetColumnCountException(expectedCount, actualCount);

      assertThat(exception.getExpectedCount()).isEqualTo(expectedCount);
    }

    @Test
    void shouldReturnActualCount() {
      int expectedCount = 3;
      int actualCount = 7;

      IncorrectResultSetColumnCountException exception = new IncorrectResultSetColumnCountException(expectedCount, actualCount);

      assertThat(exception.getActualCount()).isEqualTo(actualCount);
    }

    @Test
    void shouldHandleZeroCounts() {
      int expectedCount = 0;
      int actualCount = 0;

      IncorrectResultSetColumnCountException exception = new IncorrectResultSetColumnCountException(expectedCount, actualCount);

      assertThat(exception.getExpectedCount()).isEqualTo(expectedCount);
      assertThat(exception.getActualCount()).isEqualTo(actualCount);
      assertThat(exception.getMessage()).contains("Incorrect column count").contains("0").contains("0");
    }

    @Test
    void shouldHandleNegativeCounts() {
      int expectedCount = -1;
      int actualCount = -5;

      IncorrectResultSetColumnCountException exception = new IncorrectResultSetColumnCountException(expectedCount, actualCount);

      assertThat(exception.getExpectedCount()).isEqualTo(expectedCount);
      assertThat(exception.getActualCount()).isEqualTo(actualCount);
    }

    @Test
    void shouldHandleLargeCounts() {
      int expectedCount = Integer.MAX_VALUE;
      int actualCount = Integer.MIN_VALUE;

      IncorrectResultSetColumnCountException exception = new IncorrectResultSetColumnCountException(expectedCount, actualCount);

      assertThat(exception.getExpectedCount()).isEqualTo(expectedCount);
      assertThat(exception.getActualCount()).isEqualTo(actualCount);
    }

  }

  @Nested
  class ArrayParameterBindFailedExceptionTests {
    @Test
    void shouldCreateExceptionWithMessageOnly() {
      String message = "Failed to bind array parameter";

      ArrayParameterBindFailedException exception = new ArrayParameterBindFailedException(message);

      assertThat(exception).isNotNull();
      assertThat(exception.getMessage()).isEqualTo(message);
      assertThat(exception.getCause()).isNull();
    }

    @Test
    void shouldCreateExceptionWithMessageAndCause() {
      String message = "Error binding array parameter";
      Throwable cause = new RuntimeException("Array conversion failed");

      ArrayParameterBindFailedException exception = new ArrayParameterBindFailedException(message, cause);

      assertThat(exception).isNotNull();
      assertThat(exception.getMessage()).isEqualTo(message);
      assertThat(exception.getCause()).isEqualTo(cause);
      assertThat(exception.getCause().getMessage()).isEqualTo("Array conversion failed");
    }

    @Test
    void shouldHandleNullMessage() {
      Throwable cause = new Exception("Root cause");

      ArrayParameterBindFailedException exception = new ArrayParameterBindFailedException(null, cause);

      assertThat(exception).isNotNull();
      assertThat(exception.getMessage()).isNull();
      assertThat(exception.getCause()).isEqualTo(cause);
    }

    @Test
    void shouldHandleNullCause() {
      String message = "Array binding error";

      ArrayParameterBindFailedException exception = new ArrayParameterBindFailedException(message, null);

      assertThat(exception).isNotNull();
      assertThat(exception.getMessage()).isEqualTo(message);
      assertThat(exception.getCause()).isNull();
    }

    @Test
    void shouldHandleBothNullParameters() {
      ArrayParameterBindFailedException exception = new ArrayParameterBindFailedException(null, null);

      assertThat(exception).isNotNull();
      assertThat(exception.getMessage()).isNull();
      assertThat(exception.getCause()).isNull();
    }

    @Test
    void shouldInheritFromParameterBindFailedException() {
      String message = "Array parameter binding failed";
      ArrayParameterBindFailedException exception = new ArrayParameterBindFailedException(message);

      assertThat(exception).isInstanceOf(ParameterBindFailedException.class);
    }

  }

  @Nested
  class ParameterBindFailedExceptionTests {
    @Test
    void shouldCreateExceptionWithMessageOnly() {
      String message = "Failed to bind parameter";

      ParameterBindFailedException exception = new ParameterBindFailedException(message);

      assertThat(exception).isNotNull();
      assertThat(exception.getMessage()).isEqualTo(message);
      assertThat(exception.getCause()).isNull();
    }

    @Test
    void shouldCreateExceptionWithMessageAndCause() {
      String message = "Error binding parameter";
      Throwable cause = new RuntimeException("Conversion failed");

      ParameterBindFailedException exception = new ParameterBindFailedException(message, cause);

      assertThat(exception).isNotNull();
      assertThat(exception.getMessage()).isEqualTo(message);
      assertThat(exception.getCause()).isEqualTo(cause);
      assertThat(exception.getCause().getMessage()).isEqualTo("Conversion failed");
    }

    @Test
    void shouldHandleNullMessage() {
      Throwable cause = new Exception("Root cause");

      ParameterBindFailedException exception = new ParameterBindFailedException(null, cause);

      assertThat(exception).isNotNull();
      assertThat(exception.getMessage()).isNull();
      assertThat(exception.getCause()).isEqualTo(cause);
    }

    @Test
    void shouldHandleNullCause() {
      String message = "Parameter binding error";

      ParameterBindFailedException exception = new ParameterBindFailedException(message, null);

      assertThat(exception).isNotNull();
      assertThat(exception.getMessage()).isEqualTo(message);
      assertThat(exception.getCause()).isNull();
    }

    @Test
    void shouldHandleBothNullParameters() {
      ParameterBindFailedException exception = new ParameterBindFailedException(null, null);

      assertThat(exception).isNotNull();
      assertThat(exception.getMessage()).isNull();
      assertThat(exception.getCause()).isNull();
    }

    @Test
    void shouldInheritFromPersistenceException() {
      String message = "Parameter binding failed";
      ParameterBindFailedException exception = new ParameterBindFailedException(message);

      assertThat(exception).isInstanceOf(PersistenceException.class);
    }

  }

  @Nested
  class CannotGetJdbcConnectionExceptionTests {
    @Test
    void shouldCreateExceptionWithMessageOnly() {
      String message = "Failed to get JDBC connection";

      CannotGetJdbcConnectionException exception = new CannotGetJdbcConnectionException(message);

      assertThat(exception).isNotNull();
      assertThat(exception.getMessage()).isEqualTo(message);
      assertThat(exception.getCause()).isNull();
    }

    @Test
    void shouldCreateExceptionWithMessageAndCause() {
      String message = "Cannot establish database connection";
      Throwable cause = new SQLException("Connection timeout");

      CannotGetJdbcConnectionException exception = new CannotGetJdbcConnectionException(message, cause);

      assertThat(exception).isNotNull();
      assertThat(exception.getMessage()).isEqualTo(message);
      assertThat(exception.getCause()).isEqualTo(cause);
      assertThat(exception.getCause().getMessage()).isEqualTo("Connection timeout");
    }

    @Test
    void shouldHandleNullMessage() {
      Throwable cause = new RuntimeException("Root cause");

      CannotGetJdbcConnectionException exception = new CannotGetJdbcConnectionException(null, cause);

      assertThat(exception).isNotNull();
      assertThat(exception.getMessage()).isNull();
      assertThat(exception.getCause()).isEqualTo(cause);
    }

    @Test
    void shouldHandleNullCause() {
      String message = "Database connection failed";

      CannotGetJdbcConnectionException exception = new CannotGetJdbcConnectionException(message, null);

      assertThat(exception).isNotNull();
      assertThat(exception.getMessage()).isEqualTo(message);
      assertThat(exception.getCause()).isNull();
    }

    @Test
    void shouldHandleBothNullParameters() {
      CannotGetJdbcConnectionException exception = new CannotGetJdbcConnectionException(null, null);

      assertThat(exception).isNotNull();
      assertThat(exception.getMessage()).isNull();
      assertThat(exception.getCause()).isNull();
    }

    @Test
    void shouldInheritFromNestedRuntimeException() {
      String message = "Connection error";
      CannotGetJdbcConnectionException exception = new CannotGetJdbcConnectionException(message);

      assertThat(exception).isInstanceOf(infra.core.NestedRuntimeException.class);
    }

  }

}
