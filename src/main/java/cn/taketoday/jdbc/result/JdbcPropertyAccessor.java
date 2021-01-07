package cn.taketoday.jdbc.result;

import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * @author TODAY
 * @date 2021/1/7 22:49
 */
public interface JdbcPropertyAccessor {

  Object get(Object obj);

  void set(Object obj, ResultSet resultSet, int columnIndex) throws SQLException;

}
