/*
 * Copyright 2017 - 2026 the TODAY authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package infra.jdbc;

import org.jspecify.annotations.Nullable;

import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.Map;

import infra.beans.BeanProperty;
import infra.beans.BeanUtils;
import infra.jdbc.core.ResultSetExtractor;
import infra.jdbc.support.JdbcUtils;
import infra.jdbc.type.TypeHandler;
import infra.util.ConcurrentReferenceHashMap;
import infra.util.MapCache;

@SuppressWarnings("rawtypes")
public class DefaultResultSetHandlerFactory<T extends @Nullable Object> implements ResultSetHandlerFactory<T> {

  static final MapCache<HandlerKey, ResultSetExtractor, ResultSetMetaData> CACHE
          = new MapCache<>(new ConcurrentReferenceHashMap<>()) {

    @Override
    protected ResultSetExtractor createValue(HandlerKey key, ResultSetMetaData param) {
      try {
        return key.factory.createHandler(param);
      }
      catch (SQLException e) {
        throw new PersistenceException(null, e);
      }
    }
  };

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
  @SuppressWarnings({ "unchecked", "NullAway" })
  public ResultSetExtractor<T> getResultSetHandler(ResultSetMetaData meta) throws SQLException {
    int columnCount = meta.getColumnCount();
    StringBuilder builder = new StringBuilder(columnCount * 10);
    for (int i = 1; i <= columnCount; i++) {
      builder.append(JdbcUtils.lookupColumnName(meta, i))
              .append('\n');
    }
    return CACHE.get(new HandlerKey(builder.toString(), this), meta);
  }

  @SuppressWarnings({ "unchecked", "NullAway" })
  private ResultSetExtractor<T> createHandler(ResultSetMetaData meta) throws SQLException {
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
        throw new PersistenceException("Could not map %s to any property.".formatted(colName));
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

  static final class HandlerKey {
    public final String stringKey;
    public final DefaultResultSetHandlerFactory factory;

    HandlerKey(String stringKey, DefaultResultSetHandlerFactory f) {
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

}
