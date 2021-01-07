/**
 * Copyright 2009-2019 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package cn.taketoday.jdbc.type;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

import cn.taketoday.context.utils.ClassUtils;

/**
 * @author Clinton Begin
 * @author TODAY
 */
public class UnknownTypeHandler extends BaseTypeHandler<Object> {

  private boolean useColumnLabel = true;
  private final TypeHandlerRegistry registry;

  /**
   * The constructor that pass the type handler registry.
   *
   * @param typeHandlerRegistry
   *         a type handler registry
   */
  public UnknownTypeHandler(TypeHandlerRegistry typeHandlerRegistry) {
    this.registry = typeHandlerRegistry;
  }

  @Override
  public void setNonNullParameter(PreparedStatement ps, int i, Object parameter) throws SQLException {
    TypeHandler handler = resolveTypeHandler(parameter);
    handler.setParameter(ps, i, parameter);
  }

  @Override
  public Object getResult(ResultSet rs, String columnName) throws SQLException {
    TypeHandler<?> handler = resolveTypeHandler(rs, columnName);
    return handler.getResult(rs, columnName);
  }

  @Override
  public Object getResult(ResultSet rs, int columnIndex) throws SQLException {
    TypeHandler<?> handler = resolveTypeHandler(rs.getMetaData(), columnIndex);
    if (handler == null || handler instanceof UnknownTypeHandler) {
      handler = ObjectTypeHandler.getSharedInstance();
    }
    return handler.getResult(rs, columnIndex);
  }

  @Override
  public Object getResult(CallableStatement cs, int columnIndex) throws SQLException {
    return cs.getObject(columnIndex);
  }

  protected TypeHandler<?> resolveTypeHandler(Object parameter) {
    if (parameter == null) {
      return ObjectTypeHandler.getSharedInstance();
    }
    TypeHandler<?> handler = registry.getTypeHandler(parameter.getClass());
    // check if handler is null (issue #270)
    if (handler == null || handler instanceof UnknownTypeHandler) {
      handler = ObjectTypeHandler.getSharedInstance();
    }
    return handler;
  }

  private TypeHandler<?> resolveTypeHandler(final ResultSet rs, final String column) {
    try {
      Map<String, Integer> columnIndexLookup = new HashMap<>();
      ResultSetMetaData rsmd = rs.getMetaData();
      int count = rsmd.getColumnCount();
      boolean useColumnLabel = isUseColumnLabel();
      for (int i = 1; i <= count; i++) {
        String name = useColumnLabel ? rsmd.getColumnLabel(i) : rsmd.getColumnName(i);
        columnIndexLookup.put(name, i);
      }
      Integer columnIndex = columnIndexLookup.get(column);
      TypeHandler<?> handler = null;
      if (columnIndex != null) {
        handler = resolveTypeHandler(rsmd, columnIndex);
      }
      if (handler == null || handler instanceof UnknownTypeHandler) {
        handler = ObjectTypeHandler.getSharedInstance();
      }
      return handler;
    }
    catch (SQLException e) {
      throw new TypeException("Error determining JDBC type for column " + column + ".  Cause: " + e, e);
    }
  }

  protected TypeHandler<?> resolveTypeHandler(ResultSetMetaData rsmd, Integer columnIndex) {
    Class<?> javaType = safeGetClassForColumn(rsmd, columnIndex);
    if (javaType != null) {
      return registry.getTypeHandler(javaType);
    }
    return null;
  }

  private Class<?> safeGetClassForColumn(ResultSetMetaData rsmd, Integer columnIndex) {
    try {
      return ClassUtils.loadClass(rsmd.getColumnClassName(columnIndex));
    }
    catch (Exception e) {
      return null;
    }
  }

  public boolean isUseColumnLabel() {
    return useColumnLabel;
  }

  public void setUseColumnLabel(boolean useColumnLabel) {
    this.useColumnLabel = useColumnLabel;
  }
}
