/*
 *    Copyright 2009-2021 the original author or authors.
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

// Modifications Copyright 2017 - 2026 the TODAY authors.

package infra.jdbc.type;

import org.jspecify.annotations.Nullable;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;

import infra.util.ClassUtils;

/**
 * @author Clinton Begin
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
public class UnknownTypeHandler extends BasicTypeHandler<Object> {

  private boolean useColumnLabel = true;

  private final TypeHandlerManager registry;

  /**
   * The constructor that pass the type handler registry.
   *
   * @param typeHandlerManager a type handler registry
   */
  public UnknownTypeHandler(TypeHandlerManager typeHandlerManager) {
    this.registry = typeHandlerManager;
  }

  @Override
  @SuppressWarnings({ "rawtypes", "unchecked" })
  public void setNonNullParameter(PreparedStatement ps, int i, Object arg) throws SQLException {
    TypeHandler handler = resolveTypeHandler(arg);
    handler.setParameter(ps, i, arg);
  }

  @Nullable
  @Override
  public Object getResult(ResultSet rs, String columnName) throws SQLException {
    TypeHandler<?> handler = resolveTypeHandler(rs, columnName);
    return handler.getResult(rs, columnName);
  }

  @Nullable
  @Override
  public Object getResult(ResultSet rs, int columnIndex) throws SQLException {
    TypeHandler<?> handler = resolveTypeHandler(rs.getMetaData(), columnIndex);
    if (handler == null || handler instanceof UnknownTypeHandler) {
      handler = ObjectTypeHandler.sharedInstance;
    }
    return handler.getResult(rs, columnIndex);
  }

  @Nullable
  @Override
  public Object getResult(CallableStatement cs, int columnIndex) throws SQLException {
    return cs.getObject(columnIndex);
  }

  @SuppressWarnings("NullAway")
  protected TypeHandler<?> resolveTypeHandler(@Nullable Object parameter) {
    if (parameter == null) {
      return ObjectTypeHandler.sharedInstance;
    }
    TypeHandler<?> handler = registry.getTypeHandler(parameter.getClass());
    // check if handler is null (issue #270)
    if (handler == null || handler instanceof UnknownTypeHandler) {
      handler = ObjectTypeHandler.sharedInstance;
    }
    return handler;
  }

  private TypeHandler<?> resolveTypeHandler(final ResultSet rs, final String column) {
    try {
      ResultSetMetaData rsmd = rs.getMetaData();
      int count = rsmd.getColumnCount();
      boolean useColumnLabel = isUseColumnLabel();
      TypeHandler<?> handler = null;
      for (int columnIdx = 1; columnIdx <= count; columnIdx++) {
        String name = useColumnLabel ? rsmd.getColumnLabel(columnIdx) : rsmd.getColumnName(columnIdx);
        if (column.equals(name)) {
          handler = resolveTypeHandler(rsmd, columnIdx);
          break;
        }
      }
      if (handler == null || handler instanceof UnknownTypeHandler) {
        handler = ObjectTypeHandler.sharedInstance;
      }
      return handler;
    }
    catch (SQLException e) {
      throw new TypeException("Error determining JDBC type for column %s.  Cause: %s".formatted(column, e), e);
    }
  }

  @Nullable
  protected TypeHandler<?> resolveTypeHandler(ResultSetMetaData rsmd, Integer columnIndex) {
    Class<?> javaType = safeGetClassForColumn(rsmd, columnIndex);
    if (javaType != null) {
      return registry.getTypeHandler(javaType);
    }
    return null;
  }

  @Nullable
  private Class<?> safeGetClassForColumn(ResultSetMetaData rsmd, Integer columnIndex) {
    try {
      return ClassUtils.load(rsmd.getColumnClassName(columnIndex));
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
