/*
 * Copyright 2017 - 2024 the original author or authors.
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
 * along with this program. If not, see [https://www.gnu.org/licenses/]
 */

package cn.taketoday.jdbc;

import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.Map;

import cn.taketoday.beans.BeanProperty;
import cn.taketoday.beans.BeanUtils;
import cn.taketoday.jdbc.support.JdbcUtils;
import cn.taketoday.jdbc.type.TypeHandler;
import cn.taketoday.lang.Nullable;
import cn.taketoday.util.ConcurrentReferenceHashMap;
import cn.taketoday.util.MapCache;

@SuppressWarnings("rawtypes")
public class DefaultResultSetHandlerFactory<T> implements ResultSetHandlerFactory<T> {

  private final JdbcBeanMetadata metadata;
  private final RepositoryManager repositoryManager;

  @Nullable
  private final Map<String, String> columnMappings;

  public DefaultResultSetHandlerFactory(JdbcBeanMetadata pojoMetadata,
          RepositoryManager operations, @Nullable Map<String, String> columnMappings) {
    this.metadata = pojoMetadata;
    this.repositoryManager = operations;
    this.columnMappings = columnMappings;
  }

  @Override
  @SuppressWarnings("unchecked")
  public ResultSetHandler<T> getResultSetHandler(ResultSetMetaData meta) throws SQLException {
    int columnCount = meta.getColumnCount();
    StringBuilder builder = new StringBuilder(columnCount * 10);
    for (int i = 1; i <= columnCount; i++) {
      builder.append(JdbcUtils.lookupColumnName(meta, i))
              .append('\n');
    }
    return CACHE.get(new HandlerKey(builder.toString(), this), meta);
  }

  @SuppressWarnings("unchecked")
  private ResultSetHandler<T> createHandler(ResultSetMetaData meta) throws SQLException {
    // cache key is ResultSetMetadata + Bean type
    int columnCount = meta.getColumnCount();

    /*
     * Fallback to executeScalar if converter exists, we're selecting 1 column, and
     * no property setter exists for the column.
     */
    boolean singleScalarColumn = BeanUtils.isSimpleValueType(metadata.getObjectType()) && columnCount == 1;
    if (singleScalarColumn) {
      TypeHandler typeHandler = repositoryManager.getTypeHandler(metadata.getObjectType());
      return new TypeHandlerResultSetHandler<>(typeHandler);
    }

    var accessors = new ObjectPropertySetter[columnCount];
    for (int i = 1; i <= columnCount; i++) {
      String colName = JdbcUtils.lookupColumnName(meta, i);
      var accessor = getAccessor(colName, metadata);
      // If more than 1 column is fetched (we cannot fall back to executeScalar),
      // and the setter doesn't exist, throw exception.
      if (accessor == null && columnCount > 1 && metadata.throwOnMappingFailure) {
        throw new PersistenceException("Could not map " + colName + " to any property.");
      }
      accessors[i - 1] = accessor;
    }

    return new ObjectResultHandler<>(metadata, accessors, columnCount);
  }

  @Nullable
  private ObjectPropertySetter getAccessor(String colName, JdbcBeanMetadata metadata) {
    int index = colName.indexOf('.');
    if (index <= 0) {
      // Simple path - column
      BeanProperty beanProperty = metadata.getBeanProperty(colName, columnMappings);
      if (beanProperty == null) {
        return null;
      }
      return new ObjectPropertySetter(null, beanProperty, repositoryManager);
    }

    PropertyPath propertyPath = new PropertyPath(metadata.getObjectType(), colName);
    // find property-path
    BeanProperty beanProperty = propertyPath.getNestedBeanProperty();
    if (beanProperty == null) {
      return null;
    }

    // if colName is property-path style just using property-path set
    return new ObjectPropertySetter(propertyPath, beanProperty, repositoryManager);
  }

  private static final class HandlerKey {
    public final String stringKey;
    public final DefaultResultSetHandlerFactory factory;

    private HandlerKey(String stringKey, DefaultResultSetHandlerFactory f) {
      this.stringKey = stringKey;
      this.factory = f;
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) {
        return true;
      }
      if (o instanceof HandlerKey key) {
        return stringKey.equals(key.stringKey)
                && factory.metadata.equals(key.factory.metadata);
      }
      return false;
    }

    @Override
    public int hashCode() {
      int result = factory.metadata.hashCode();
      result = 31 * result + stringKey.hashCode();
      return result;
    }

  }

  static final MapCache<HandlerKey, ResultSetHandler, ResultSetMetaData> CACHE
          = new MapCache<>(new ConcurrentReferenceHashMap<>()) {

    @Override
    protected ResultSetHandler createValue(HandlerKey key, ResultSetMetaData param) {
      try {
        return key.factory.createHandler(param);
      }
      catch (SQLException e) {
        throw new PersistenceException(e);
      }
    }
  };

}
