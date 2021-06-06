/*
 * Original Author -> 杨海健 (taketoday@foxmail.com) https://taketoday.cn
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

import cn.taketoday.context.factory.BeanProperty;
import cn.taketoday.context.utils.ClassUtils;
import cn.taketoday.context.utils.Mappings;
import cn.taketoday.jdbc.PersistenceException;
import cn.taketoday.jdbc.reflection.JdbcBeanMetadata;
import cn.taketoday.jdbc.reflection.Pojo;
import cn.taketoday.jdbc.type.TypeHandler;
import cn.taketoday.jdbc.type.TypeHandlerRegistry;
import cn.taketoday.jdbc.utils.JdbcUtils;

public class DefaultResultSetHandlerFactory<T> implements ResultSetHandlerFactory<T> {

  private final JdbcBeanMetadata metadata;
  final TypeHandlerRegistry registry;

  public DefaultResultSetHandlerFactory(JdbcBeanMetadata pojoMetadata, TypeHandlerRegistry registry) {
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
    return CACHE.get(new Key(builder.toString(), this), meta);
  }

  @SuppressWarnings("unchecked")
  private ResultSetHandler<T> newResultSetHandler0(final ResultSetMetaData meta) throws SQLException {
    // cache key is ResultSetMetadata + Bean type
    final int columnCount = meta.getColumnCount();

    /**
     * Fallback to executeScalar if converter exists, we're selecting 1 column, and
     * no property setter exists for the column.
     */
    final JdbcBeanMetadata metadata = this.metadata;
    final boolean useExecuteScalar = ClassUtils.isSimpleType(metadata.getType()) && columnCount == 1;

    if (useExecuteScalar) {
      final TypeHandler typeHandler = registry.getTypeHandler(metadata.getType());
      return new TypeHandlerResultSetHandler<>(typeHandler);
    }
    final boolean throwOnMappingFailure = metadata.isThrowOnMappingFailure();
    final JdbcPropertyAccessor[] accessors = new JdbcPropertyAccessor[columnCount];
    for (int i = 1; i <= columnCount; i++) {
      final String colName = JdbcUtils.getColumnName(meta, i);
      final JdbcPropertyAccessor accessor = getAccessor(colName, metadata);
      // If more than 1 column is fetched (we cannot fall back to executeScalar),
      // and the setter doesn't exist, throw exception.
      if (throwOnMappingFailure && accessor == null && columnCount > 1) {
        throw new PersistenceException("Could not map " + colName + " to any property.");
      }
      accessors[i - 1] = accessor;
    }

    return new ObjectResultHandler<>(metadata, accessors, columnCount);
  }

  private JdbcPropertyAccessor getAccessor(final String propertyPath, final JdbcBeanMetadata metadata) {
    int index = propertyPath.indexOf('.');

    if (index <= 0) {
      // Simple path - fast way
      final BeanProperty beanProperty = metadata.getBeanProperty(propertyPath);
      // behavior change: do not throw if POJO contains less properties
      if (beanProperty == null) {
        return null;
      }
      final TypeHandler<?> typeHandler = registry.getTypeHandler(beanProperty.getType());
      return new TypeHandlerPropertyAccessor(typeHandler, beanProperty);
    }

    // dot path - long way
    // i'm too lazy now to rewrite this case so I just call old unoptimized code...
    // TODO
    final TypeHandler<?> typeHandler = registry.getTypeHandler(metadata.getType());

    class PropertyPathPropertyAccessor extends JdbcPropertyAccessor {

      @Override
      public Object get(final Object obj) {
        Pojo pojo = new Pojo(metadata, obj);
        return pojo.getProperty(propertyPath);
//        return BeanPropertyAccessor.getProperty(obj, metadata, propertyPath);
      }

      @Override
      public void set(final Object obj, final ResultSet resultSet, final int columnIndex) throws SQLException {
        final Object result = typeHandler.getResult(resultSet, columnIndex);
        Pojo pojo = new Pojo(metadata, obj);
        pojo.setProperty(propertyPath, result);
//        BeanPropertyAccessor.setProperty(obj, metadata, propertyPath, result);
      }
    }

    return new PropertyPathPropertyAccessor();
  }

  private static class Key {
    final String stringKey;
    final DefaultResultSetHandlerFactory factory;

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

  static final Mappings<ResultSetHandler, ResultSetMetaData> CACHE
          = new Mappings<ResultSetHandler, ResultSetMetaData>() {

    @Override
    protected ResultSetHandler createValue(Object key, ResultSetMetaData param) {
      try {
        return ((Key) key).factory.newResultSetHandler0(param);
      }
      catch (SQLException e) {
        throw new RuntimeException(e);
      }
    }
  };

}
