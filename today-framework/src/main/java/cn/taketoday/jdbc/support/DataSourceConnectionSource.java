package cn.taketoday.jdbc.support;

import java.sql.Connection;
import java.sql.SQLException;

import javax.sql.DataSource;

import cn.taketoday.lang.Assert;

/**
 * The default implementation of {@link ConnectionSource}, Simply delegates all
 * calls to specified {@link DataSource}
 */
public final class DataSourceConnectionSource implements ConnectionSource {
  private final DataSource dataSource;

  /**
   * Creates a ConnectionSource that gets connection from specified
   * {@link DataSource}
   *
   * @param dataSource a DataSource to get connections from
   */
  DataSourceConnectionSource(DataSource dataSource) {
    Assert.notNull(dataSource, "DataSource must not be null");
    this.dataSource = dataSource;
  }

  /**
   * <p>Attempts to establish a connection with the data source that
   * this {@code DataSource} object represents.
   *
   * @return a connection to the data source
   * @throws SQLException if a database access error occurs
   * @throws java.sql.SQLTimeoutException when the driver has determined that the
   * timeout value specified by the {@code setLoginTimeout} method
   * has been exceeded and has at least tried to cancel the
   * current database connection attempt
   */
  @Override
  public Connection getConnection() throws SQLException {
    return dataSource.getConnection();
  }

  public DataSource getDataSource() {
    return dataSource;
  }

  @Override
  public String toString() {
    return "Connections from dataSource: " + dataSource;
  }

}
