package cn.taketoday.jdbc.result;

import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * @author TODAY
 * @date 2021/1/2 18:27
 */
public interface ResultHandler<T> {

  T handle(ResultSet resultSet) throws SQLException;
}
