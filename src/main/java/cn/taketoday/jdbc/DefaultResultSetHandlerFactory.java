package cn.taketoday.jdbc;

import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;

import cn.taketoday.jdbc.reflection.BeanMetadata;
import cn.taketoday.jdbc.reflection.BeanProperty;
import cn.taketoday.jdbc.reflection.Pojo;
import cn.taketoday.jdbc.result.JdbcPropertyAccessor;
import cn.taketoday.jdbc.result.ObjectResultHandler;
import cn.taketoday.jdbc.result.TypeHandlerPropertyAccessor;
import cn.taketoday.jdbc.result.TypeHandlerResultSetHandler;
import cn.taketoday.jdbc.type.SimpleTypeRegistry;
import cn.taketoday.jdbc.type.TypeHandler;
import cn.taketoday.jdbc.type.TypeHandlerRegistry;
import cn.taketoday.jdbc.utils.AbstractCache;
import cn.taketoday.jdbc.utils.JdbcUtils;

public class DefaultResultSetHandlerFactory<T> implements ResultSetHandlerFactory<T> {

  private final BeanMetadata metadata;
  final TypeHandlerRegistry registry;

  public DefaultResultSetHandlerFactory(BeanMetadata pojoMetadata, TypeHandlerRegistry registry) {
    this.metadata = pojoMetadata;
    this.registry = registry;
  }

  @Override
  @SuppressWarnings("unchecked")
  public ResultSetHandler<T> newResultSetHandler(final ResultSetMetaData meta) throws SQLException {
    final StringBuilder builder = new StringBuilder();
    final int columnCount = meta.getColumnCount();
    for (int i = 1; i <= columnCount; i++) {
      builder.append(JdbcUtils.getColumnName(meta, i))
              .append('\n');
    }
    return (ResultSetHandler<T>) CACHE.get(new Key(builder.toString(), this), meta);
  }

  @SuppressWarnings("unchecked")
  private ResultSetHandler<T> newResultSetHandler0(final ResultSetMetaData meta) throws SQLException {
    // cache key is ResultSetMetadata + Bean type
    final int columnCount = meta.getColumnCount();

    /**
     * Fallback to executeScalar if converter exists, we're selecting 1 column, and
     * no property setter exists for the column.
     */
    final boolean useExecuteScalar = SimpleTypeRegistry.isSimpleType(metadata.getType()) && columnCount == 1;

    if (useExecuteScalar) {
      final TypeHandler typeHandler = registry.getTypeHandler(metadata.getType());
      return new TypeHandlerResultSetHandler<>(typeHandler);
    }

    final JdbcPropertyAccessor[] propertyAccessors = new JdbcPropertyAccessor[columnCount];

    for (int i = 1; i <= columnCount; i++) {
      final String colName = JdbcUtils.getColumnName(meta, i);

      propertyAccessors[i - 1] = getPropertyAccessor(colName, metadata);

      // If more than 1 column is fetched (we cannot fall back to executeScalar),
      // and the setter doesn't exist, throw exception.
      if (this.metadata.throwOnMappingFailure && propertyAccessors[i - 1] == null && columnCount > 1) {
        throw new PersistenceException("Could not map " + colName + " to any property.");
      }
    }

    return new ObjectResultHandler<>(metadata, propertyAccessors, columnCount);
  }

  private JdbcPropertyAccessor getPropertyAccessor(final String propertyPath, final BeanMetadata metadata) {
    int index = propertyPath.indexOf('.');

    if (index <= 0) {
      // Simple path - fast way
      final BeanProperty beanProperty = metadata.getBeanProperty(propertyPath);
      // behavior change: do not throw if POJO contains less properties
      if (beanProperty == null) {
        return null;
      }
      final TypeHandler<?> typeHandler = registry.getTypeHandler(beanProperty.getType());
      return new TypeHandlerPropertyAccessor(typeHandler, beanProperty.getPropertyAccessor());
    }

    // dot path - long way
    // i'm too lazy now to rewrite this case so I just call old unoptimized code...
    // TODO
    final TypeHandler<?> typeHandler = registry.getTypeHandler(metadata.getType());

    class PropertyPathPropertyAccessor implements JdbcPropertyAccessor {

      @Override
      public Object get(final Object obj) {
        Pojo pojo = new Pojo(metadata, obj);
        return pojo.getProperty(propertyPath);
      }

      @Override
      public void set(final Object obj, final ResultSet resultSet, final int columnIndex) throws SQLException {
        Pojo pojo = new Pojo(metadata, obj);
        final Object result = typeHandler.getResult(resultSet, columnIndex);
        pojo.setProperty(propertyPath, result);
      }
    }

    return new PropertyPathPropertyAccessor();
  }

  private static class Key {
    final String stringKey;
    final DefaultResultSetHandlerFactory factory;

    DefaultResultSetHandlerFactory factory() {
      return factory;
    }

    private BeanMetadata getMetadata() {
      return factory.metadata;
    }

    private Key(String stringKey, DefaultResultSetHandlerFactory f) {
      this.stringKey = stringKey;
      this.factory = f;
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) {
        return true;
      }
      if (o == null || getClass() != o.getClass()) {
        return false;
      }

      Key key = (Key) o;

      return stringKey.equals(key.stringKey)
              && factory.metadata.equals(key.getMetadata());
    }

    @Override
    public int hashCode() {
      int result = factory.metadata.hashCode();
      result = 31 * result + stringKey.hashCode();
      return result;
    }

  }

  static final AbstractCache<Key, ResultSetHandler, ResultSetMetaData> CACHE
          = new AbstractCache<Key, ResultSetHandler, ResultSetMetaData>() {
    @Override
    protected ResultSetHandler evaluate(Key key, ResultSetMetaData param) {
      try {
        return key.factory().newResultSetHandler0(param);
      }
      catch (SQLException e) {
        throw new RuntimeException(e);
      }
    }
  };

}
