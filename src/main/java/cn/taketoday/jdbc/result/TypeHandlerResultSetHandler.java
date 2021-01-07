package cn.taketoday.jdbc.result;

import java.sql.ResultSet;
import java.sql.SQLException;

import cn.taketoday.jdbc.ResultSetHandler;
import cn.taketoday.jdbc.type.TypeHandler;

/**
 * @author TODAY
 * @date 2021/1/7 22:52
 */
public class TypeHandlerResultSetHandler<T> implements ResultSetHandler<T> {
  final TypeHandler<T> typeHandler;

  public TypeHandlerResultSetHandler(TypeHandler<T> typeHandler) {
    this.typeHandler = typeHandler;
  }

  @Override
  public T handle(ResultSet resultSet) throws SQLException {
    return typeHandler.getResult(resultSet, 1);
  }
}
