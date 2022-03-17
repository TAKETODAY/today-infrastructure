package cn.taketoday.jdbc.result;

import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * @author TODAY 2021/1/7 22:49
 */
public abstract class JdbcPropertyAccessor {

  public abstract Object get(Object obj);

  public abstract void set(
          Object obj, ResultSet resultSet, int columnIndex) throws SQLException;

}
