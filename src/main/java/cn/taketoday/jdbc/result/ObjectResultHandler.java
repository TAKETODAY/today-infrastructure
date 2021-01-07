package cn.taketoday.jdbc.result;

import java.sql.ResultSet;
import java.sql.SQLException;

import cn.taketoday.jdbc.ResultSetHandler;
import cn.taketoday.jdbc.reflection.BeanMetadata;

/**
 * @author TODAY
 * @date 2021/1/2 18:28
 */
public class ObjectResultHandler<T> implements ResultSetHandler<T> {

  final int columnCount;
  final BeanMetadata metadata;
  final JdbcPropertyAccessor[] propertyAccessors;

  public ObjectResultHandler(final BeanMetadata metadata, final JdbcPropertyAccessor[] accessors, int columnCount) {
    this.metadata = metadata;
    this.propertyAccessors = accessors;
    this.columnCount = columnCount;
  }

  @Override
  @SuppressWarnings("unchecked")
  public T handle(final ResultSet resultSet) throws SQLException {
    // otherwise we want executeAndFetch with object mapping
    Object pojo = metadata.newInstance();
    final JdbcPropertyAccessor[] accessors = propertyAccessors;
    for (int colIdx = 1; colIdx <= columnCount; colIdx++) {
      final JdbcPropertyAccessor setter = accessors[colIdx - 1];
      if (setter != null) {
        setter.set(pojo, resultSet, colIdx);
      }
    }
    return (T) pojo;
  }

}
