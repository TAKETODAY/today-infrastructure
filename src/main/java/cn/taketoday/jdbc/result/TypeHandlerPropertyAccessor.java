package cn.taketoday.jdbc.result;

import java.sql.ResultSet;
import java.sql.SQLException;

import cn.taketoday.context.reflect.PropertyAccessor;
import cn.taketoday.jdbc.type.TypeHandler;

/**
 * @author TODAY
 * @date 2021/1/7 22:50
 */
public class TypeHandlerPropertyAccessor implements JdbcPropertyAccessor {

  final TypeHandler<?> typeHandler;
  final PropertyAccessor accessor;

  public TypeHandlerPropertyAccessor(TypeHandler<?> typeHandler, PropertyAccessor accessor) {
    this.typeHandler = typeHandler;
    this.accessor = accessor;
  }

  @Override
  public Object get(Object obj) {
    return accessor.get(obj);
  }

  @Override
  public void set(Object obj, ResultSet resultSet, int columnIndex) throws SQLException {
    final Object result = typeHandler.getResult(resultSet, columnIndex);
    accessor.set(obj, result);
  }
}
