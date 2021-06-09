package cn.taketoday.jdbc.support;

import java.sql.Connection;
import java.sql.SQLException;

import javax.sql.DataSource;

/**
 * The default implementation of {@link ConnectionSource}, Simply delegates all
 * calls to specified {@link DataSource }
 */
public final class DataSourceConnectionSource implements ConnectionSource {
  private final DataSource dataSource;

  /**
   * Creates a ConnectionSource that gets connection from specified
   * {@link DataSource }
   *
   * @param dataSource
   *         a DataSource to get connections from
   */
  public DataSourceConnectionSource(DataSource dataSource) {
    this.dataSource = dataSource;
  }

  @Override
  public Connection getConnection() throws SQLException {
    return dataSource.getConnection();
  }

  public DataSource getDataSource() {
    return dataSource;
  }

  // static

  public static DataSourceConnectionSource of(DataSource source) {
    return new DataSourceConnectionSource(source);
  }
}
