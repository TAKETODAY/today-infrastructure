package cn.taketoday.jdbc.datasource;

import org.junit.jupiter.api.Test;

import java.sql.SQLException;

import javax.sql.DataSource;

import cn.taketoday.jdbc.CannotGetJdbcConnectionException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 1.0 2022/7/13 16:18
 */
class DataSourceUtilsTests {

  @Test
  void testConnectionNotAcquiredExceptionIsPropagated() throws SQLException {
    DataSource dataSource = mock(DataSource.class);
    when(dataSource.getConnection()).thenReturn(null);
    assertThatThrownBy(() -> DataSourceUtils.getConnection(dataSource))
            .isInstanceOf(CannotGetJdbcConnectionException.class)
            .hasMessageStartingWith("Failed to obtain JDBC Connection")
            .hasCauseInstanceOf(IllegalStateException.class);
  }

  @Test
  void testConnectionSQLExceptionIsPropagated() throws SQLException {
    DataSource dataSource = mock(DataSource.class);
    when(dataSource.getConnection()).thenThrow(new SQLException("my dummy exception"));
    assertThatThrownBy(() -> DataSourceUtils.getConnection(dataSource))
            .isInstanceOf(CannotGetJdbcConnectionException.class)
            .hasMessageStartingWith("Failed to obtain JDBC Connection")
            .satisfies(e -> {
              assertThat(e).isInstanceOf(SQLException.class);
            })
            .hasMessage("my dummy exception");
  }

}
