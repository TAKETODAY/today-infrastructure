package cn.taketoday.jdbc;

import java.sql.ResultSetMetaData;
import java.sql.SQLException;

/**
 * User: dimzon Date: 4/7/14 Time: 12:02 AM
 */
public interface ResultSetHandlerFactory<T> {

  ResultSetHandler<T> newResultSetHandler(ResultSetMetaData resultSetMetaData) throws SQLException;
}
