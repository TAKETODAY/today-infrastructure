package cn.taketoday.jdbc.connectionsources;

import java.sql.Connection;

import cn.taketoday.jdbc.JdbcConnection;

/**
 * Predefined implementations of {@link ConnectionSource} Created by nickl on
 * 09.01.17.
 */
public class ConnectionSources {

  private ConnectionSources() {}

  /**
   * A ConnectionSource that will wrap externally managed connection with proxy
   * that will omit {@link Connection#close()} or {@link Connection#commit()}
   * calls. This is useful to make {@link JdbcConnection} work with
   * externally managed transactions
   *
   * @param connection
   *         connection to wrap
   *
   * @return a connection wrapper that represent a nested connection
   */
  public static ConnectionSource join(final Connection connection) {
    return () -> new NestedConnection(connection);
  }

}
