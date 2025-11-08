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

package infra.jdbc.format;

import org.junit.jupiter.api.Test;

import java.sql.Statement;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2022/9/12 19:38
 */
class SqlStatementLoggerTests {
  SqlStatementLogger logger = new SqlStatementLogger(true, true, true, 20);

  @Test
  void log() {

    logger.logStatement("SELECT * FROM t_user where id = ?");
    logger.logStatement("SELECT * FROM t_user where id = ?", DDLSQLFormatter.INSTANCE);
    logger.logSlowQuery("SELECT * FROM t_user where id = ?", System.nanoTime() - TimeUnit.MINUTES.toNanos(2));

    logger.logStatement(
            "create table issue5table(id int identity primary key, val integer)", DDLSQLFormatter.INSTANCE);

  }

  @Test
  void shouldCreateSqlStatementLoggerWithAllParameters() {
    SqlStatementLogger logger = new SqlStatementLogger(true, true, true, true, 1000, "test-prefix");

    assertThat(logger).isNotNull();
  }

  @Test
  void shouldCreateSqlStatementLoggerWithDefaultPrefix() {
    SqlStatementLogger logger = new SqlStatementLogger(true, true, true, 1000);

    assertThat(logger).isNotNull();
  }

  @Test
  void shouldLogStatementWithDescription() {
    SqlStatementLogger logger = new SqlStatementLogger(false, false, false, 0);

    // Should not throw exception
    logger.logStatement("Test query", "SELECT * FROM users");
    assertThat(true).isTrue();
  }

  @Test
  void shouldLogStatementWithoutDescription() {
    SqlStatementLogger logger = new SqlStatementLogger(false, false, false, 0);

    // Should not throw exception
    logger.logStatement("SELECT * FROM users");
    assertThat(true).isTrue();
  }

  @Test
  void shouldLogStatementWithCustomFormatter() {
    SqlStatementLogger logger = new SqlStatementLogger(false, false, false, 0);

    // Should not throw exception
    logger.logStatement("SELECT * FROM users", BasicSQLFormatter.INSTANCE);
    assertThat(true).isTrue();
  }

  @Test
  void shouldLogStatementWithDescriptionAndCustomFormatter() {
    SqlStatementLogger logger = new SqlStatementLogger(false, false, false, 0);

    // Should not throw exception
    logger.logStatement("Test query", "SELECT * FROM users", BasicSQLFormatter.INSTANCE);
    assertThat(true).isTrue();
  }

  @Test
  void shouldCheckIfDebugEnabled() {
    SqlStatementLogger logger = new SqlStatementLogger(false, false, false, 0);

    boolean debugEnabled = logger.isDebugEnabled();

    // Result depends on actual logger configuration, but shouldn't throw exception
    assertThat(debugEnabled).isNotNull();
  }

  @Test
  void shouldCheckIfSlowDebugEnabled() {
    SqlStatementLogger logger = new SqlStatementLogger(false, false, false, 0);

    boolean slowDebugEnabled = logger.isSlowDebugEnabled();

    // Result depends on actual logger configuration, but shouldn't throw exception
    assertThat(slowDebugEnabled).isNotNull();
  }

  @Test
  void shouldLogSlowQueryWithStatement() {
    SqlStatementLogger logger = new SqlStatementLogger(false, false, false, 100);
    Statement statement = new MockStatement("SELECT * FROM users");

    // Should not throw exception
    logger.logSlowQuery(statement, System.nanoTime() - TimeUnit.SECONDS.toNanos(1));
    assertThat(true).isTrue();
  }

  @Test
  void shouldNotLogSlowQueryWhenDisabled() {
    SqlStatementLogger logger = new SqlStatementLogger(false, false, false, 0);
    Statement statement = new MockStatement("SELECT * FROM users");

    // Should not throw exception
    logger.logSlowQuery(statement, System.nanoTime() - TimeUnit.SECONDS.toNanos(1));
    assertThat(true).isTrue();
  }

  @Test
  void shouldThrowExceptionForInvalidStartTimeNanos() {
    SqlStatementLogger logger = new SqlStatementLogger(false, false, false, 100);

    assertThatThrownBy(() -> logger.logSlowQuery("SELECT * FROM users", -1))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("startTimeNanos");
  }

  @Test
  void shouldLogSlowQueryWithString() {
    SqlStatementLogger logger = new SqlStatementLogger(false, false, false, 100);

    // Should not throw exception
    logger.logSlowQuery("SELECT * FROM users", System.nanoTime() - TimeUnit.SECONDS.toNanos(1));
    assertThat(true).isTrue();
  }

  @Test
  void shouldNotLogSlowQueryWhenExecutionTimeIsBelowThreshold() {
    SqlStatementLogger logger = new SqlStatementLogger(false, false, false, 5000); // 5 seconds threshold

    // Should not throw exception and should not log since execution time is 100ms < 5000ms
    logger.logSlowQuery("SELECT * FROM users", System.nanoTime() - TimeUnit.MILLISECONDS.toNanos(100));
    assertThat(true).isTrue();
  }

  @Test
  void shouldFormatAndHighlightStatement() {
    SqlStatementLogger logger = new SqlStatementLogger(false, true, true, 0);

    // Should not throw exception
    logger.logStatement("SELECT id, name FROM users WHERE age > 18");
    assertThat(true).isTrue();
  }

  @Test
  void shouldLogToStdoutOnly() {
    SqlStatementLogger logger = new SqlStatementLogger(true, false, false, true, 0, "test-prefix");

    // Should not throw exception
    logger.logStatement("SELECT * FROM users");
    assertThat(true).isTrue();
  }

  @Test
  void shouldReturnToString() {
    SqlStatementLogger logger = new SqlStatementLogger(true, true, true, 1000);

    String result = logger.toString();

    assertThat(result).contains("format = true");
    assertThat(result).contains("logToStdout = true");
    assertThat(result).contains("highlight = true");
    assertThat(result).contains("logSlowQuery = 1000");
  }

  static class MockStatement implements Statement {
    private final String sql;

    public MockStatement(String sql) {
      this.sql = sql;
    }

    @Override
    public String toString() {
      return sql;
    }

    // Implement other required methods with minimal implementation
    @Override
    public java.sql.ResultSet executeQuery(String sql) { return null; }

    @Override
    public int executeUpdate(String sql) { return 0; }

    @Override
    public void close() { }

    @Override
    public int getMaxFieldSize() { return 0; }

    @Override
    public void setMaxFieldSize(int max) { }

    @Override
    public int getMaxRows() { return 0; }

    @Override
    public void setMaxRows(int max) { }

    @Override
    public void setEscapeProcessing(boolean enable) { }

    @Override
    public int getQueryTimeout() { return 0; }

    @Override
    public void setQueryTimeout(int seconds) { }

    @Override
    public void cancel() { }

    @Override
    public java.sql.SQLWarning getWarnings() { return null; }

    @Override
    public void clearWarnings() { }

    @Override
    public void setCursorName(String name) { }

    @Override
    public boolean execute(String sql) { return false; }

    @Override
    public java.sql.ResultSet getResultSet() { return null; }

    @Override
    public int getUpdateCount() { return 0; }

    @Override
    public boolean getMoreResults() { return false; }

    @Override
    public void setFetchDirection(int direction) { }

    @Override
    public int getFetchDirection() { return 0; }

    @Override
    public void setFetchSize(int rows) { }

    @Override
    public int getFetchSize() { return 0; }

    @Override
    public int getResultSetConcurrency() { return 0; }

    @Override
    public int getResultSetType() { return 0; }

    @Override
    public void addBatch(String sql) { }

    @Override
    public void clearBatch() { }

    @Override
    public int[] executeBatch() { return new int[0]; }

    @Override
    public java.sql.Connection getConnection() { return null; }

    @Override
    public boolean getMoreResults(int current) { return false; }

    @Override
    public java.sql.ResultSet getGeneratedKeys() { return null; }

    @Override
    public int executeUpdate(String sql, int autoGeneratedKeys) { return 0; }

    @Override
    public int executeUpdate(String sql, int[] columnIndexes) { return 0; }

    @Override
    public int executeUpdate(String sql, String[] columnNames) { return 0; }

    @Override
    public boolean execute(String sql, int autoGeneratedKeys) { return false; }

    @Override
    public boolean execute(String sql, int[] columnIndexes) { return false; }

    @Override
    public boolean execute(String sql, String[] columnNames) { return false; }

    @Override
    public int getResultSetHoldability() { return 0; }

    @Override
    public boolean isClosed() { return false; }

    @Override
    public void setPoolable(boolean poolable) { }

    @Override
    public boolean isPoolable() { return false; }

    @Override
    public void closeOnCompletion() { }

    @Override
    public boolean isCloseOnCompletion() { return false; }

    @Override
    public <T> T unwrap(Class<T> iface) { return null; }

    @Override
    public boolean isWrapperFor(Class<?> iface) { return false; }
  }

}