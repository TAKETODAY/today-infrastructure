/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2021 All Rights Reserved.
 *
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see [http://www.gnu.org/licenses/]
 */

package cn.taketoday.jdbc.result;

import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;

import cn.taketoday.beans.BeanProperty;
import cn.taketoday.jdbc.PersistenceException;
import cn.taketoday.jdbc.support.JdbcUtils;
import cn.taketoday.jdbc.type.TypeHandler;
import cn.taketoday.jdbc.type.TypeHandlerRegistry;
import cn.taketoday.util.ClassUtils;
import cn.taketoday.util.ConcurrentReferenceHashMap;
import cn.taketoday.util.MapCache;

@SuppressWarnings("rawtypes")
public class DefaultResultSetHandlerFactory<T> implements ResultSetHandlerFactory<T> {

  private final JdbcBeanMetadata metadata;
  private final TypeHandlerRegistry registry;

  public DefaultResultSetHandlerFactory(TypeHandlerRegistry registry, JdbcBeanMetadata pojoMetadata) {
    this.metadata = pojoMetadata;
    this.registry = registry;
  }

  @Override
  @SuppressWarnings("unchecked")
  public ResultSetHandler<T> newResultSetHandler(ResultSetMetaData meta) throws SQLException {
    int columnCount = meta.getColumnCount();
    StringBuilder builder = new StringBuilder(columnCount * 10);
    for (int i = 1; i <= columnCount; i++) {
      builder.append(JdbcUtils.lookupColumnName(meta, i))
              .append('\n');
    }
    return CACHE.get(new Key(builder.toString(), this), meta);
  }

  @SuppressWarnings("unchecked")
  private ResultSetHandler<T> newResultSetHandler0(ResultSetMetaData meta) throws SQLException {
    // cache key is ResultSetMetadata + Bean type
    int columnCount = meta.getColumnCount();

    /*
     * Fallback to executeScalar if converter exists, we're selecting 1 column, and
     * no property setter exists for the column.
     */
    boolean useExecuteScalar = ClassUtils.isSimpleType(metadata.getType()) && columnCount == 1;

    if (useExecuteScalar) {
      TypeHandler typeHandler = registry.getTypeHandler(metadata.getType());
      return new TypeHandlerResultSetHandler<>(typeHandler);
    }
    JdbcPropertyAccessor[] accessors = new JdbcPropertyAccessor[columnCount];
    for (int i = 1; i <= columnCount; i++) {
      String colName = JdbcUtils.lookupColumnName(meta, i);
      JdbcPropertyAccessor accessor = getAccessor(colName, metadata);
      // If more than 1 column is fetched (we cannot fall back to executeScalar),
      // and the setter doesn't exist, throw exception.
      if (accessor == null && columnCount > 1 && metadata.isThrowOnMappingFailure()) {
        throw new PersistenceException("Could not map " + colName + " to any property.");
      }
      accessors[i - 1] = accessor;
    }

    return new ObjectResultHandler<>(metadata, accessors, columnCount);
  }

  private JdbcPropertyAccessor getAccessor(String propertyName, JdbcBeanMetadata metadata) {
    int index = propertyName.indexOf('.');

    if (index <= 0) {
      // Simple path - fast way
      BeanProperty beanProperty = metadata.getBeanProperty(propertyName);
      // behavior change: do not throw if POJO contains less properties
      if (beanProperty == null) {
        return null;
      }
      TypeHandler<?> typeHandler = registry.getTypeHandler(beanProperty.getType());
      return new TypeHandlerPropertyAccessor(typeHandler, beanProperty);
    }

    // dot path - long way
    // i'm too lazy now to rewrite this case so I just call old unoptimized code...
    TypeHandler<?> typeHandler = registry.getTypeHandler(metadata.getType());
    class PropertyPathPropertyAccessor extends JdbcPropertyAccessor {
      @Override
      public Object get(Object obj) {
        return metadata.getProperty(obj, propertyName);
      }

      @Override
      public void set(Object obj, ResultSet resultSet, int columnIndex) throws SQLException {
        Object result = typeHandler.getResult(resultSet, columnIndex);
        metadata.setProperty(obj, propertyName, result);
      }
    }

    return new PropertyPathPropertyAccessor();
  }

  private static class Key {
    public final String stringKey;
    public final DefaultResultSetHandlerFactory factory;

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
              && factory.metadata.equals(key.factory.metadata);
    }

    @Override
    public int hashCode() {
      int result = factory.metadata.hashCode();
      result = 31 * result + stringKey.hashCode();
      return result;
    }

  }

  static final MapCache<Key, ResultSetHandler, ResultSetMetaData> CACHE
          = new MapCache<>(new ConcurrentReferenceHashMap<>()) {

    @Override
    protected ResultSetHandler createValue(Key key, ResultSetMetaData param) {
      try {
        return key.factory.newResultSetHandler0(param);
      }
      catch (SQLException e) {
        throw new PersistenceException(e);
      }
    }
  };

}
