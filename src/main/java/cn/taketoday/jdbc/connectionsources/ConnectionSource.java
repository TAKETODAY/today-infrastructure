package cn.taketoday.jdbc.connectionsources;

import java.sql.Connection;
import java.sql.SQLException;

import cn.taketoday.jdbc.JdbcConnection;

/**
 * An abstraction layer for providing jdbc connection to use from
 * {@link JdbcConnection} Created by nickl on 09.01.17.
 */
public interface ConnectionSource {

  Connection getConnection() throws SQLException;
}
