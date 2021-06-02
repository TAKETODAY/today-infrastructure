package cn.taketoday.jdbc;

import java.sql.Connection;
import java.sql.SQLException;

/**
 * @author TODAY 2021/6/2 21:23
 */
@FunctionalInterface
public interface ConnectionCallback<T> {

  T doInConnection(final Connection con) throws SQLException;
}
