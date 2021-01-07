package cn.taketoday.jdbc.result;

import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;

import cn.taketoday.context.reflect.PropertyAccessor;
import cn.taketoday.jdbc.reflection.BeanMetadata;

/**
 * @author TODAY
 * @date 2021/1/2 18:28
 */
public class ObjectResultHandler implements ResultHandler<Object> {

  final BeanMetadata metadata;
  final PropertyAccessor[] propertyAccessors;
  /*final */int columnCount;

  public ObjectResultHandler(final BeanMetadata metadata, final PropertyAccessor[] propertyAccessors) {
    this.metadata = metadata;
    this.propertyAccessors = propertyAccessors;
  }

  public ObjectResultHandler(final BeanMetadata metadata, final ResultSetMetaData meta,
                             PropertyAccessor[] propertyAccessors) {
    this.metadata = metadata;
    this.propertyAccessors = propertyAccessors;
  }

  @Override
  public Object handle(final ResultSet resultSet) throws SQLException {

    // otherwise we want executeAndFetch with object mapping
    Object ret = metadata.newInstance();
    final PropertyAccessor[] accessors = propertyAccessors;
    for (int colIdx = 1; colIdx <= columnCount; colIdx++) {
      PropertyAccessor setter = accessors[colIdx - 1];
      if (setter != null) {
        setter.set(ret, resultSet.getObject(colIdx));
      }
    }

    return ret;
  }

}
