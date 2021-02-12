package cn.taketoday.jdbc.result;

import java.sql.ResultSet;
import java.sql.SQLException;

import cn.taketoday.context.factory.BeanProperty;
import cn.taketoday.jdbc.type.TypeHandler;

/**
 * @author TODAY
 * @date 2021/1/7 22:50
 */
public class TypeHandlerPropertyAccessor implements JdbcPropertyAccessor {

  final TypeHandler<?> typeHandler;
  final BeanProperty beanProperty;

  public TypeHandlerPropertyAccessor(TypeHandler<?> typeHandler, BeanProperty beanProperty) {
    this.typeHandler = typeHandler;
    this.beanProperty = beanProperty;
  }

  @Override
  public Object get(Object obj) {
    return beanProperty.getValue(obj);
  }

  @Override
  public void set(Object obj, ResultSet resultSet, int columnIndex) throws SQLException {
    final Object result = getResult(resultSet, columnIndex);
    beanProperty.setValue(obj, result);
  }

  /**
   * Get result from {@link ResultSet}.
   * <p>
   * Obtain from {@link TypeHandler}, if it fails, use the default acquisition method
   * </p>
   *
   * @param resultSet
   *         Target result set
   *
   * @return data object
   *
   * @throws SQLException
   *         If {@link ResultSet#getObject(int)} failed
   */
  Object getResult(ResultSet resultSet, int columnIndex) throws SQLException {
    try {
      return typeHandler.getResult(resultSet, columnIndex);
    }
    catch (SQLException e) {
      // maybe data conversion error
      return resultSet.getObject(columnIndex);
    }
  }
}
